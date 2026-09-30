package com.sperance.exileforge.core

import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.model.auction.AuctionFilter
import com.sperance.exileforge.core.model.auction.LotStatus
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.core.model.command.RedemptionKind
import com.sperance.exileforge.core.model.command.RedemptionReward
import com.sperance.exileforge.core.model.sync.API_REVISION
import com.sperance.exileforge.core.model.sync.HeroParts
import com.sperance.exileforge.core.model.sync.HeroSnapshot
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.rules.content.ContentLoader
import com.sperance.exileforge.rules.content.Orb
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemBuckets
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import com.sperance.exileforge.rules.run.RunEventKind
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
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

    @Test fun realServerClientContract(): Unit = runBlocking {
        val url = System.getenv("EF_LIVE_URL")
        assumeTrue("Enabled only by the isolated client/server job", !url.isNullOrBlank())
        val api = GameApi(requireNotNull(url))

        // The manifest names the revision this client speaks and every chunk with its fingerprint.
        val manifest = api.manifest(fresh = true)
        manifest.requireWorkbench()
        assertEquals(API_REVISION, manifest.revision)
        val chunks = manifest.content.chunks.mapValues { (file, _) -> api.files.contentChunk(file) }
        val index = ContentLoader.load { chunks.getValue(it) }
        assertEquals(manifest.content.hash, index.hash, "the chunks do not make the manifest's content")
        assertEquals(TestContent.index.hash, index.hash, "the served content is not the pinned submodule's")

        val locales = api.files.localeManifest()
        assertTrue(locales.languages.map { it.code }.containsAll(listOf("ru", "en")), "languages: ${locales.languages}")
        serverLocale = api.files.localeBundle(assertNotNull(locales.language("ru")))
        assertTrue(serverLocale.contains("system.success"))

        val admin = api.login("admin", requireNotNull(System.getenv("EF_ADMIN_PASSWORD")))
        assertEquals("ADMIN", admin.role)
        assertEquals(admin.id, assertNotNull(api.currentUser()).id)

        // One hero, one document: every POST that names the hero brings the parts that changed.
        var delivered: HeroSnapshot? = null
        api.heroSync({ HeroParts(it).header() }) { _, snapshot -> delivered = snapshot }
        val heroClass = index.classes.classes.first().code
        val created = api.hero.create(admin.id, "Contract", "", heroClass)
        val heroId = created.id
        try {
            val parts = HeroParts(heroId).merge(assertNotNull(api.hero.view(heroId, HeroParts(heroId))))
            assertTrue(parts.complete, "the first view did not bring every part")
            assertEquals(heroClass, parts.hero.heroClass)
            assertEquals(null, api.hero.view(heroId, parts), "an unchanged hero should answer 304")
            val sheet = Sheets.calculate(index, parts.hero.level, heroClass, parts.tree, parts.items)
            assertTrue((sheet.stats["STOCK_HEALTH"] ?: 0.0) > 0.0)

            // Granted, worn, rerolled, sold - the server's answers are the client's items.
            val ring = index.templates.values.first { it.slot == Slot.RING && !it.unique && !it.demanding }
            val rare = api.hero.grantEquipment(heroId, ring.code, Rarity.RARE)
            assertEquals(Rarity.RARE, rare.rarity)
            val floor = index.limits(Rarity.RARE, Slot.RING).floor
            assertTrue(rare.rolls.count { index.modifier(it.code)?.affix == true } >= floor, "a rare below its floor: ${rare.rolls}")
            assertEquals(Slot.RING, api.hero.equip(heroId, rare.id).slot)
            assertNotNull(delivered, "a POST with the hero brought no snapshot")
            assertTrue(ItemBuckets.names[ItemBuckets.of(rare.id)] in assertNotNull(delivered).parts, "equipping did not deliver the ring's bucket")

            api.hero.grantItem(heroId, Orb.CHAOS_ORB.name, 2)
            val rerolled = assertNotNull(api.hero.applyOrb(heroId, rare.id, Orb.CHAOS_ORB.name).item)
            assertEquals(rare.id, rerolled.id)
            assertTrue(rerolled.rolls.count { index.modifier(it.code)?.affix == true } >= floor)
            assertTrue(api.hero.sell(heroId, api.hero.unequip(heroId, rare.id).id).gold > 0)

            // The run: a seed from the server, the same zone here, the journal paid on replay — every reward rolled by the server alone.
            val progress = api.campaign.progress(heroId)
            val zoneCode = progress.unlocked.first()
            val start = api.campaign.start(heroId, zoneCode)
            assertEquals(start.id, api.campaign.start(heroId, zoneCode).id, "entering the zone again did not go on with its run")
            val run = Run(index, index.zone(zoneCode)!!, start.seed, start.context)
            assertEquals(run.count, start.count, "the client and the server count the zone differently")
            val events = listOf(RunEvent(0, RunEventKind.KILL, i = 0, m = 0), RunEvent(1, RunEventKind.BOSS), RunEvent(2, RunEventKind.LEAVE))
            val report = api.campaign.events(heroId, events)
            assertEquals(3, report.applied)
            assertEquals(emptyList(), report.rejected)
            assertTrue(!report.open, "the run stayed open after LEAVE")
            assertTrue(report.rewards.map { it.n }.containsAll(listOf(0, 1)), "the kill and the boss did not come back with their rewards")
            assertTrue(report.rewards.first().reward.experience > 0, "a kill paid no experience")
            assertTrue(zoneCode in report.progress.cleared, "a slain boss did not clear the zone")
            // The run is closed: a journal sent again is refused with CP_018, which the client reads as "settled, drop it".
            assertEquals("CP_018", assertFailsWith<ApiFailure> { api.campaign.events(heroId, events) }.code)

            // The auction takes the item out of the hero and gives it back on cancel: the starter set and the run's loot
            // hold chaos orbs too, so the count is compared with itself before the lot, not with a fixed figure.
            suspend fun chaos() = HeroParts(heroId).merge(assertNotNull(api.hero.view(heroId, HeroParts(heroId)))).bag[Orb.CHAOS_ORB.name] ?: 0L
            val held = chaos()
            val lot = api.auction.sellItem(heroId, Orb.CHAOS_ORB.name, 1, Orb.CHAOS_ORB.name, 1)
            assertEquals(LotStatus.ACTIVE, lot.status)
            assertTrue(api.auction.search(heroId, AuctionFilter(), listOf("")).items.any { it.id == lot.id })
            assertEquals(LotStatus.CANCELLED, api.auction.cancel(heroId, lot.id).status)
            assertEquals(held, chaos())

            // A code is a treasure once.
            val code = api.promo.create(RedemptionCode(code = "CONTRACT-${System.currentTimeMillis()}", treasure = listOf(RedemptionReward(RedemptionKind.GOLD, "", 100.0))))
            val before = HeroParts(heroId).merge(assertNotNull(api.hero.view(heroId, HeroParts(heroId)))).hero.money
            assertEquals("system.success", api.hero.redeem(heroId, code.code))
            assertEquals(before + 100, HeroParts(heroId).merge(assertNotNull(api.hero.view(heroId, HeroParts(heroId)))).hero.money)
            assertFailsWith<ApiFailure> { api.hero.redeem(heroId, code.code) }
            api.promo.delete(code.id)

            // A player is refused what is not theirs and what only an administrator may do.
            val player = GameApi(url)
            player.login(requireNotNull(System.getenv("EF_PLAYER_LOGIN")), requireNotNull(System.getenv("EF_PLAYER_PASSWORD")))
            assertEquals(403, assertFailsWith<ApiFailure> { player.hero.view(heroId, HeroParts(heroId)) }.status)
            val own = player.hero.create(assertNotNull(player.currentUser()).id, "Player", "", heroClass)
            try {
                assertEquals(403, assertFailsWith<ApiFailure> { player.hero.grantItem(own.id, Orb.CHAOS_ORB.name, 1) }.status)
            } finally { player.hero.delete(own.id) }
        } finally {
            api.hero.delete(heroId)
        }
    }
}
