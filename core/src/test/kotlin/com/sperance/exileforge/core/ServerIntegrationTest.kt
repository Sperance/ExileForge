package com.sperance.exileforge.core

import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.LotStatus
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.model.sync.HeroSnapshot
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.network.SanctionRequest
import com.sperance.exileforge.core.network.SanctionTarget
import com.sperance.exileforge.rules.content.ContentLoader
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.reward.RedemptionReward
import com.sperance.exileforge.rules.roll.ItemBuckets
import com.sperance.exileforge.rules.run.MapMechanic
import com.sperance.exileforge.rules.run.StartOptions
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The live contract, opt-in: CI launches the pinned server (`backend/` submodule) with MongoDB and
 * points `EF_LIVE_URL` here. No transport mocks - a mock agrees with itself by construction.
 *
 * One walk through what the client does: the manifest and its chunks make the same index the
 * server holds; a hero is one snapshot in parts; an item is granted, worn, rerolled and sold; a run
 * is issued as a seed, rolled here with `rules` and paid by the server's replay of the journal; a
 * lot is listed and taken back; a code is redeemed; a player is refused what is not theirs.
 */
class ServerIntegrationTest {

    /**
     * Манифест называет ревизию клиента и каждый чанк с отпечатком; маршруты - по сессии. Каждый POST героя приносит сдвинутые части. Без
     * Предначертания герой не создаётся. Выдачи - администратором любому герою; выданная вещь метки не несёт. Заход - семя сервера, награды
     * катит только он; насильная механика и страж сразу со входа - только тестировщику со свободным прогрессом (`CP_028`, иначе отказ темпа); Ваал-зона живёт только в своём заходе; новое семя не чаще `run.newSeedSeconds`; закрытый заход отвергает журнал
     * `CP_018`. Аукцион забирает вещь и возвращает при снятии; плата за место - золотом; герою-тестировщику аукцион закрыт (`GR_001`); лот
     * героя под баном не продаётся, без места в тайнике - `CH_020`. Код погашается один раз на аккаунт и устройство. Чужое и
     * только-администраторское - отказ.
     */
    @Test fun realServerClientContract(): Unit = runBlocking {
        val url = System.getenv("EF_LIVE_URL")
        assumeTrue("Enabled only by the isolated client/server job", !url.isNullOrBlank())
        val api = GameApi(requireNotNull(url))

        val manifest = api.manifest(fresh = true)
        manifest.requireWorkbench()
        assertEquals(API_REVISION, manifest.revision)
        val chunks = manifest.content.chunks.mapValues { (file, _) -> api.files.contentChunk(file) }
        val index = ContentLoader.load { chunks.getValue(it) }
        assertEquals(manifest.content.hash, index.hash, "the chunks do not make the manifest's content")
        assertEquals(TestContent.index.hash, index.hash, "the served content is not the pinned submodule's")

        val locales = manifest.locale
        assertTrue(locales.languages.map { it.code }.containsAll(listOf("ru", "en")), "languages: ${locales.languages}")
        serverLocale = api.files.localeBundle(assertNotNull(locales.language("ru")))
        assertTrue(serverLocale.contains("system.success"))

        assertTrue(manifest.routes.none { it.path.startsWith("/api/v1/admin/") }, "an anonymous manifest named the admin routes")
        val admin = api.login("admin", requireNotNull(System.getenv("EF_ADMIN_PASSWORD")))
        assertEquals("ADMIN", admin.role)
        assertEquals(admin.id, assertNotNull(api.currentUser()).id)
        api.requireRoutes()
        assertTrue(api.manifest(fresh = true).routes.any { it.path.startsWith("/api/v1/admin/moderation") }, "the administrator's manifest lacks moderation")

        var delivered: HeroSnapshot? = null
        api.heroSync({ HeroParts(it).header() }) { _, snapshot -> delivered = snapshot }
        val heroClass = index.classes.classes.first().code
        api.fated()
        val created = api.hero.create(admin.id, "Contract", "", heroClass)
        val heroId = created.id
        try {
            api.hero.grantItem(heroId, Orb.CHAOS_ORB.name, 1)
            assertTrue(api.moderation.setTester(heroId, true))
            val parts = HeroParts(heroId).merge(assertNotNull(api.hero.view(heroId, HeroParts(heroId))))
            assertTrue(parts.complete, "the first view did not bring every part")
            assertEquals(heroClass, parts.hero.heroClass)
            assertEquals(null, api.hero.view(heroId, parts), "an unchanged hero should answer 304")
            delivered = null
            val town = api.hero.town(heroId)
            assertNull(town.guild.guild, "a new hero entered the town in a guild")
            assertNotNull(delivered, "the town visit brought no hero snapshot")
            val sheet = Sheets.calculate(index, parts.hero.level, heroClass, parts.tree, parts.items)
            assertTrue((sheet.stats["STOCK_HEALTH"] ?: 0.0) > 0.0)

            val ring = index.templates.values.first { it.slot == Slot.RING && !it.unique && !it.demanding }
            val rare = api.hero.grantEquipment(heroId, ring.code, Rarity.RARE)
            assertEquals(Rarity.RARE, rare.rarity)
            val floor = index.rarityLimits(Rarity.RARE, Slot.RING).floor
            assertTrue(rare.rolls.count { index.modifier(it.code)?.affix == true } >= floor, "a rare below its floor: ${rare.rolls}")
            assertEquals(Slot.RING, api.hero.equip(heroId, rare.id).slot)
            assertNotNull(delivered, "a POST with the hero brought no snapshot")
            assertTrue(ItemBuckets.names[ItemBuckets.of(rare.id)] in assertNotNull(delivered).parts, "equipping did not deliver the ring's bucket")

            api.hero.grantItem(heroId, Orb.CHAOS_ORB.name, 2)
            val rerolled = assertNotNull(api.hero.applyOrb(heroId, rare.id, Orb.CHAOS_ORB.name).item)
            assertEquals(rare.id, rerolled.id)
            assertTrue(rerolled.rolls.count { index.modifier(it.code)?.affix == true } >= floor)
            assertTrue(api.hero.sell(heroId, api.hero.unequip(heroId, rare.id).id).gold > 0, "a granted item sold for nothing")

            val zoneCode = index.world.unlocked(emptyList()).first()
            val corrupted = index.zone(zoneCode)!!.corrupted.value.isNotBlank()
            if (corrupted) assertEquals("CP_028", (runCatching { api.campaign.start(heroId, zoneCode, StartOptions(forced = setOf(MapMechanic.VAAL))) }.exceptionOrNull() as? ApiFailure)?.code)
            api.hero.freeProgress(heroId, true)
            if (corrupted) {
                val vaal = api.campaign.start(heroId, zoneCode, StartOptions(forced = setOf(MapMechanic.VAAL)))
                assertEquals(1, assertNotNull(api.campaign.events(heroId, vaal.id, listOf(RunEvent.VaalOpen(0)))).applied)
                assertEquals(zoneCode, HeroParts(heroId).merge(assertNotNull(api.hero.view(heroId, HeroParts(heroId)))).campaign.vaalZone?.mapCode)
            }
            var entered = runCatching { api.campaign.start(heroId, zoneCode) }
            while ((entered.exceptionOrNull() as? ApiFailure)?.code == "CP_021") {
                delay(2_000)
                entered = runCatching { api.campaign.start(heroId, zoneCode) }
            }
            val start = entered.getOrThrow()
            assertEquals(null, start.context.vaal, "the Vaal zone of a replaced run stayed open")
            assertEquals(start.id, api.campaign.start(heroId, zoneCode).id, "entering the zone again did not go on with its run")
            val run = Run(index, index.zone(zoneCode)!!, start.seed, start.context)
            assertEquals(run.count, start.count, "the client and the server count the zone differently")
            val events = listOf(RunEvent.Kill(0, i = 0, m = 0), RunEvent.Boss(1), RunEvent.Leave(2))
            val report = assertNotNull(api.campaign.events(heroId, start.id, events))
            assertEquals(3, report.applied)
            assertEquals(emptyList(), report.rejected)
            assertTrue(!report.open, "the run stayed open after LEAVE")
            assertTrue(report.rewards.map { it.n }.containsAll(listOf(0, 1)), "the kill and the boss did not come back with their rewards")
            assertTrue(report.rewards.first().reward.experience > 0, "a kill paid no experience")
            assertTrue(zoneCode in report.progress.cleared, "a slain boss did not clear the zone")
            assertEquals("CP_018", assertFailsWith<ApiFailure> { api.campaign.events(heroId, start.id, events) }.code)

            suspend fun bag(code: String) = HeroParts(heroId).merge(assertNotNull(api.hero.view(heroId, HeroParts(heroId)))).bag[code] ?: 0L
            val starterOrb = Orb.ORB_OF_TRANSMUTATION.name
            val auctionLevel = index.rules.unlocks["AUCTION"] ?: 1
            api.hero.grant(heroId, "level", "level" to auctionLevel.toString())
            api.hero.grant(heroId, "gold", "amount" to LISTING_GOLD.toString())
            assertEquals("GR_001", assertFailsWith<ApiFailure> { api.auction.sellItem(heroId, starterOrb, 1, Orb.CHAOS_ORB.name, 1) }.code)
            assertEquals(false, api.moderation.setTester(heroId, false))
            val held = bag(starterOrb)
            val lot = assertNotNull(api.auction.sellItem(heroId, starterOrb, 1, Orb.CHAOS_ORB.name, 1).lot)
            assertEquals(LotStatus.ACTIVE, lot.status)
            assertTrue(api.auction.search(heroId, AuctionFilter(), listOf("")).items.any { it.id == lot.id })
            assertEquals(LotStatus.CANCELLED, api.auction.cancel(heroId, lot.id).lot?.status)
            assertEquals(held, bag(starterOrb))

            val trader = GameApi(url)
            val traderHero = trader.hero.create(trader.loginByDevice(null, "contract-trader").id.also { trader.fated() }, "Trader", "", heroClass).id
            api.moderation.setTester(traderHero, true)
            api.hero.grant(traderHero, "level", "level" to auctionLevel.toString())
            api.hero.grant(traderHero, "gold", "amount" to LISTING_GOLD.toString())
            val grantedRing = api.hero.grantEquipment(traderHero, ring.code, Rarity.RARE)
            assertEquals("GR_001", assertFailsWith<ApiFailure> { trader.auction.sellEquipment(traderHero, grantedRing.id, Orb.CHAOS_ORB.name, 1) }.code)
            api.moderation.setTester(traderHero, false)
            api.moderation.setTester(heroId, true)
            api.hero.grantItem(heroId, Orb.CHAOS_ORB.name, 2)
            api.moderation.setTester(heroId, false)
            val starter = HeroParts(traderHero).merge(assertNotNull(trader.hero.view(traderHero, HeroParts(traderHero)))).items.first { it.id != grantedRing.id }
            if (starter.equipped) trader.hero.unequip(traderHero, starter.id)
            val open = assertNotNull(trader.auction.sellEquipment(traderHero, starter.id, Orb.CHAOS_ORB.name, 1).lot).id
            val banned = assertNotNull(trader.auction.sellItem(traderHero, Orb.ORB_OF_TRANSMUTATION.name, 1, Orb.CHAOS_ORB.name, 1).lot).id
            val ban = api.moderation.ban(SanctionRequest(SanctionTarget.HERO, heroId = traderHero, hours = 1, comment = "contract"))
            assertEquals("AU_004", assertFailsWith<ApiFailure> { api.auction.buy(heroId, banned) }.code)
            api.moderation.lift(ban.id)
            val stash = index.rules.stash
            api.moderation.setTester(heroId, true)
            repeat((stash.baseSlots + stash.overflowSlots) / TESTER_BATCH + 1) { api.hero.grant(heroId, "rares", "count" to TESTER_BATCH.toString()) }
            api.moderation.setTester(heroId, false)
            assertEquals("CH_020", assertFailsWith<ApiFailure> { api.auction.buy(heroId, open) }.code)

            val code = api.promo.create(RedemptionCode(code = "CONTRACT-${System.currentTimeMillis()}", treasure = listOf(RedemptionReward.Gold(100)), limit = 1))
            suspend fun traderMoney() = HeroParts(traderHero).merge(assertNotNull(trader.hero.view(traderHero, HeroParts(traderHero)))).hero.money
            val before = traderMoney()
            assertEquals("system.success", trader.hero.redeem(traderHero, code.code))
            assertEquals(before + 100, traderMoney())
            assertFailsWith<ApiFailure> { trader.hero.redeem(traderHero, code.code) }
            api.promo.delete(code.id)

            val player = GameApi(url)
            player.login(requireNotNull(System.getenv("EF_PLAYER_LOGIN")), requireNotNull(System.getenv("EF_PLAYER_PASSWORD")))
            assertEquals(403, assertFailsWith<ApiFailure> { player.hero.view(heroId, HeroParts(heroId)) }.status)
            player.fated()
            val own = player.hero.create(assertNotNull(player.currentUser()).id, "Player", "", heroClass)
            try {
                assertEquals(403, assertFailsWith<ApiFailure> { player.hero.grantItem(own.id, Orb.CHAOS_ORB.name, 1) }.status)
            } finally {
                player.hero.eraseDeletion(own.id, own.name)
            }
        } finally {
            api.hero.delete(heroId)
        }
    }
}

/** Золото на плату за места лотов контракта: с запасом на несколько выставлений. */
private const val LISTING_GOLD = 100_000L

/** Сколько вещей выдаёт тестировщику один запрос `grant/rares` (`TESTER_MAX_BATCH` сервера по умолчанию). */
private const val TESTER_BATCH = 50

/** Предначертание аккаунта выбрано: невыбранное - первый из трёх предложенных, выбранное остаётся. */
private suspend fun GameApi.fated() {
    val view = fate.view()
    if (view.chosen == null) assertEquals(view.offers.first().code, assertNotNull(fate.choose(view.offers.first().code).chosen).code)
}
