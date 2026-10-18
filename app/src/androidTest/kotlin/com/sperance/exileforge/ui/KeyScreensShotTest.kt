package com.sperance.exileforge.ui

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.app.AccountUi
import com.sperance.exileforge.core.app.DEFAULT_SERVER
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.CombatSheet
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.combat.FlaskView
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.combat.SkillView
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.FeatureView
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.campaign.run.LevelUp
import com.sperance.exileforge.core.campaign.run.ObjectAction
import com.sperance.exileforge.core.campaign.run.RoomSpot
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.crafts.Crafts
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.jobTitle
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.hero.HeroHolding
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.core.model.crafts.JobView
import com.sperance.exileforge.core.model.crafts.ProfessionView
import com.sperance.exileforge.core.model.crafts.WorkView
import com.sperance.exileforge.core.model.feedback.AdminReport
import com.sperance.exileforge.core.model.feedback.FeedbackKind
import com.sperance.exileforge.core.model.feedback.FullReport
import com.sperance.exileforge.core.model.feedback.IssueNote
import com.sperance.exileforge.core.model.feedback.IssueView
import com.sperance.exileforge.core.model.feedback.NoteAuthor
import com.sperance.exileforge.core.model.feedback.ReportStatus
import com.sperance.exileforge.core.model.hero.HeroInfo
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.model.trade.GoldBill
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.core.sheet.itemSeals
import com.sperance.exileforge.core.update.AppBuild
import com.sperance.exileforge.core.update.AvailableUpdate
import com.sperance.exileforge.core.update.UpdateState
import com.sperance.exileforge.core.world.World
import com.sperance.exileforge.rules.content.ActiveSlot
import com.sperance.exileforge.rules.content.HeroClass
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Influence
import com.sperance.exileforge.rules.content.Job
import com.sperance.exileforge.rules.content.JobExtra
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.ModifierDef
import com.sperance.exileforge.rules.content.Quest
import com.sperance.exileforge.rules.content.QuestCondition
import com.sperance.exileforge.rules.content.QuestKind
import com.sperance.exileforge.rules.content.QuestReward
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.content.Source
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.MonsterRoller
import com.sperance.exileforge.rules.roll.Roll
import com.sperance.exileforge.rules.roll.WorkTally
import com.sperance.exileforge.rules.run.AltarPact
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.rules.run.MapMechanic
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.rules.sheet.FlaskKind
import com.sperance.exileforge.ui.components.FeedbackViewModel
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.InspectAction
import com.sperance.exileforge.ui.components.InspectActions
import com.sperance.exileforge.ui.components.InspectChoice
import com.sperance.exileforge.ui.components.ItemCard
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.LocalWallClock
import com.sperance.exileforge.ui.components.StillClock
import com.sperance.exileforge.ui.components.UpdateDialog
import com.sperance.exileforge.ui.components.rememberItemFilter
import com.sperance.exileforge.ui.screens.admin.AdminPanels
import com.sperance.exileforge.ui.screens.admin.AdminViewModel
import com.sperance.exileforge.ui.screens.craft.TargetRail
import com.sperance.exileforge.ui.screens.crafts.CraftsViewModel
import com.sperance.exileforge.ui.screens.crafts.WorkPlaque
import com.sperance.exileforge.ui.screens.expedition.AltarSheet
import com.sperance.exileforge.ui.screens.expedition.FeatureSheet
import com.sperance.exileforge.ui.screens.expedition.LevelUpScreen
import com.sperance.exileforge.ui.screens.expedition.NodeSheet
import com.sperance.exileforge.ui.screens.expedition.ObjectActionPanel
import com.sperance.exileforge.ui.screens.expedition.arena.ActionBar
import com.sperance.exileforge.ui.screens.expedition.arena.FightFrame
import com.sperance.exileforge.ui.screens.expedition.scene.ExpeditionScene
import com.sperance.exileforge.ui.screens.hero.StatSheet
import com.sperance.exileforge.ui.screens.quests.BookChoice
import com.sperance.exileforge.ui.screens.quests.QuestRow
import com.sperance.exileforge.ui.screens.quests.StoryCodex
import com.sperance.exileforge.ui.screens.server.AdminTicket
import com.sperance.exileforge.ui.screens.server.ReportLine
import com.sperance.exileforge.ui.screens.skills.Page
import com.sperance.exileforge.ui.screens.skills.Slots
import com.sperance.exileforge.ui.theme.ForgeTheme
import com.sperance.exileforge.ui.theme.Ink
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.core.context.GlobalContext
import java.io.File

/**
 * Снимки ключевых экранов: каждый экран рисуется над контентом закреплённого сервера по-русски, без анимаций,
 * и ложится в `design/shots/<имя>.jpg` - CI выгружает их артефактом `exile-forge-shots`; что обрезано или наехало, видно там.
 * Экраны: задания и контракты, гримуар 4+3, лист алтаря, книга главы, окно уровня, Кодекс, узел Атласа с исключением,
 * карта со стенами и дверью комнаты, панель боя на 4 слота (стоящим снимком боя), окно сбора, админка, карточка редкой вещи с плашками
 * особых строк, карточка захваченной карты, полка кузницы с карточкой вещи и её действиями, окно обновления, модерация отчётов,
 * плашка идущей работы.
 */
class KeyScreensShotTest {
    @get:Rule val compose = createComposeRule()

    private val index get() = TestWorld.index
    private val heroClass get() = index.classes.classes.first()

    @Before fun dictionary() {
        serverLocale = TestWorld.russian
    }

    @After fun forget() {
        serverLocale = LocaleBundle()
    }

    @Test fun quests() = shot("quests") {
        val goals = index.quests.goals
        val orb = "ORB_OF_TRANSMUTATION"
        listOf(
            Quest("q1", QuestKind.DAILY, goals[0].code, goals[0].counter, Rarity.MAGIC, 40, progress = 40, reward = QuestReward(1200, 3400.0, mapOf(orb to 2))),
            Quest("q2", QuestKind.DAILY, goals[1 % goals.size].code, goals[1 % goals.size].counter, Rarity.RARE, 3, progress = 1, place = index.campaign.zones.first().code.value, conditions = listOf(QuestCondition.entries.first()), reward = QuestReward(2500, 8000.0)),
            Quest("q3", QuestKind.CONTRACT, goals[2 % goals.size].code, goals[2 % goals.size].counter, Rarity.UNIQUE, 12, progress = 5, expiresAt = System.currentTimeMillis() + 38 * 60_000, reward = QuestReward(9000, 24000.0, mapOf(orb to 1))),
            Quest("q4", QuestKind.WEEKLY, goals[3 % goals.size].code, goals[3 % goals.size].counter, Rarity.COMMON, 6, progress = 6, claimed = true, reward = QuestReward(600, 1500.0)),
        ).forEach { QuestRow(it) }
    }

    /** Гримуар 4+3: класс с самыми длинными именами активных умений - имена в слотах видны целиком, без «Тяжёлы…». */
    @Test fun grimoire() = shot("grimoire") {
        val level = 50
        val (owner, actives) = longestActives(level)
        val passives = index.skills.ofClass(owner.code).filter { it.unlock <= level && it.kind == SkillKind.PASSIVE }.take(3)
        val skills = HeroSkills(
            learned = (actives + passives).associate { it.code to 5 },
            active = actives.map { ActiveSlot(it.code, SlotCondition.READY) },
            passive = passives.map { it.code },
        )
        val stats = Sheets.calculate(index, level, owner.code, listOf(TakenNode(owner.startNode)), emptyList()).stats
        Slots(index, skills, level, stats) {}
        (actives + passives).forEach { Page(game(level), index, it, skills, level, stats, null) {} }
    }

    @Test fun altar() = shot("altar", scroll = false) {
        val offers = index.campaign.features?.altar?.offers.orEmpty().take(2)
        val pacts = offers.mapNotNull { offer -> line(offer.boon)?.let { boon -> line(offer.curse)?.let { curse -> AltarPact(boon, curse, offer.code, offer.target) } } }
        Box(Modifier.height(ALTAR_HEIGHT)) { AltarSheet(game(40), MapFeature.Altar(0, pacts), {}, {}) }
    }

    @Test fun storyBook() = shot("book") {
        BookChoice(index, 1, heroClass.code, HeroSkills(), false) { _, _ -> }
    }

    @Test fun levelUp() = shot("levelup", scroll = false) {
        Box(Modifier.height(720.dp)) { LevelUpScreen(LevelUp(9, 10), heroClass, index, false) {} }
    }

    /** Заголовок «Кодекс» гравирован прописными - раскрывается по подписи «развернуть». */
    @Test fun codex() {
        shot("codex", open = ui("common.expand")) { StoryCodex(index.quests.story, 3) }
    }

    /** Ключевые «только Зодчий / только Пустота»: второй погашен с причиной. */
    @Test fun atlasExclusion() = shot("atlas") {
        val node = index.atlas.nodes.first { it.excludes.isNotEmpty() }
        val taken = setOf(index.atlasGraph.start) + node.excludes + index.atlasGraph.neighbours(node.code)
        NodeSheet(index, node, taken, available = 3, refund = GoldBill(100, 500), enabled = true, onTake = {}, onRefund = {}, modifier = Modifier)
    }

    /** Герой - у двери комнаты, если она встала: дверь и рычаг видны на снимке. */
    @Test fun mapWithDoor() = shot("map", scroll = false) {
        val zone = index.campaign.zones.firstOrNull { it.code.value == "C2_DEEP_ADITS" } ?: index.campaign.zones.first()
        val level = 12
        val sheet = Sheets.calculate(index, level, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        val gear = HeroGear(sheet.stats, level, sheet.model, HeroStance.of(heroClass.code), Loadout.of(HeroSkills(), index, heroClass.code, emptyList(), sheet.stats, index.powers, index.rules.charges), index.stats.percent)
        val run = Run(index, zone, 7L, RunContext(heroClass.code, level, forced = setOf(MapMechanic.VAULT)))
        val expedition = ExpeditionRun.start(index, zone, run, RunJournal("run-7", "hero", zone.code.value), gear, CampaignState(), System.currentTimeMillis(), 0.0, level, { 0L })
        expedition.world.features.filterIsInstance<RoomSpot>().firstOrNull()?.let { room ->
            expedition.world.heroX = room.entrance.x + .5
            expedition.world.heroY = room.entrance.y + 1.5
        }
        expedition.update(0.0)
        ExpeditionScene(expedition, mutableFloatStateOf(0f), heroClass.code, Modifier.fillMaxWidth().height(640.dp))
    }

    /**
     * Панель боя на 4 слота: снимок боя собран руками и стоит - живой арены нет, часы кадра стоят, тестовые часы
     * Compose не идут сами (живой бой раша не давал эмулятору покоя). Слоты - готов, откат, нет маны, применяется; фляги - полная,
     * пьётся, пустая.
     */
    @Test fun fightBar() = shot("fight", scroll = false, frozen = true) {
        val level = 50
        val (owner, actives) = longestActives(level)
        val zone = index.campaign.zones.first { it.monsters.isNotEmpty() }
        val leader = MonsterRoller(index).roll(zone, zone.monsters.first(), emptyList(), Dice(7L))
        val states = listOf(SlotState(1f, true), SlotState(.35f, true, 4.2), SlotState(1f, false), SlotState(1f, true, casting = .6f))
        val views = actives.zip(states).mapIndexed { slot, (skill, state) ->
            SkillView(slot, skill.code, skill.icon, 5, 12 + 4 * slot, state.ready, state.affordable, SlotCondition.READY, state.seconds, casting = state.casting)
        }
        val flasks = listOf(
            FlaskView(0, "life", FlaskKind.LIFE, 4, 4, 1, 0f, SlotCondition.READY),
            FlaskView(1, "mana", FlaskKind.MANA, 2, 4, 1, .5f, SlotCondition.READY),
            FlaskView(2, "utility", FlaskKind.UTILITY, 0, 3, 1, 0f, SlotCondition.READY),
        )
        val fight = FightHud(leader, emptyList(), 420, 0, emptyList(), 1, null, heroMana = 60, heroMaxMana = 120, skills = views, flasks = flasks)
        ActionBar(game(level, owner.code), fight, FightFrame(mutableFloatStateOf(0f), null), {}) {}
    }

    /** Сбор на карте: лист узла ремесла и нижняя панель действия у объекта с полосой сбора на 40%. */
    @Test fun gathering() = shot("gathering", scroll = false) {
        val level = 20
        val zone = index.campaign.zones.first { it.level >= level }
        val run = Run(index, zone, 7L, RunContext(heroClass.code, level, forced = setOf(MapMechanic.NODES)))
        val node = run.features.all.filterIsInstance<MapFeature.Node>().first()
        Box(Modifier.height(640.dp)) {
            FeatureSheet(game(level), FeatureView(node, emptyList())) {}
            ObjectActionPanel(ObjectAction(node, .4f), { null }, { .4f })
        }
    }

    /** Админка с ролью администратора: промокоды, выдачи, режим. */
    @Test fun admin() = shot("admin", scroll = false) {
        val vm: AdminViewModel = GlobalContext.get().get()
        val plain = game(40)
        val game = plain.copy(session = plain.session.copy(profile = UserProfile("owner", role = "ADMIN")))
        Box(Modifier.height(1400.dp)) { AdminPanels(game, vm) }
    }

    /** Редкий амулет со строкой каждого особого источника: плашки особого блока, основные строки и счётчик сторон под ними. */
    @Test fun itemSeals() = shot("item_seals") {
        ItemCard(view(sealedAmulet()), enabled = false, detailed = true, action = false)
    }

    /** Захваченная карта с качеством и алхимией: плашки захвата, качества и алхимии над строками карты. */
    @Test fun mapSeals() = shot("map_seals") {
        val game = game(70)
        val map = capturedMap()
        ItemCard(view(map), enabled = false, detailed = true, action = false, seals = game.itemSeals(map))
    }

    /** Полка кузницы с отдельной кнопкой надетого и карточка вещи из выбора цели: «В кузницу» | «Надеть» и «⋯». */
    @Test fun forgeCard() = shot("forge_card") {
        val item = sealedAmulet()
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { TargetRail(rememberItemFilter(ItemShelf.FORGE)) {} }
        ItemCard(view(item), enabled = false, detailed = true, action = false)
        val menu = listOf(InspectAction(ui("item.sell_merchant"), figure = "1 340") {}, InspectAction(ui("item.list_auction")) {})
        InspectActions(Inspect.Copy(item, InspectAction(ui("bag.to_forge")) {}, InspectAction(ui("hero.equip")) {}, menu), InspectChoice(null)) {}
    }

    /** Сводка героя: раздел «Заряды» с действующим максимумом. */
    @Test fun heroSummary() = shot("hero_summary") {
        val level = 70
        val sheet = Sheets.calculate(index, level, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        val combat = CombatSheet.of(index, sheet, level, heroClass.code, HeroSkills(), null, emptyList())
        combat?.let { StatSheet(game(level), it.figures) }
    }

    /** Вид копии [item] над контентом закреплённого сервера. */
    private fun view(item: ItemInstance): ItemView = ItemView(item, index.template(item.template)!!, index)

    /** Описание модификатора, что катается, по условию [take]. */
    private fun def(take: (ModifierDef) -> Boolean): ModifierDef = index.definitions.first { it.rolls && take(it) }

    /** Ролл [def] его лучшим тиром серединой. */
    private fun roll(def: ModifierDef, fractured: Boolean = false): Roll = Roll(def.code, tier = 1, share = .5, fractured = fractured)

    /** Редкий порченый амулет: врождённая, порча, расколотая, эссенция, ручная работа, ремесло, влияние Зодчего, префикс и суффиксы. */
    private fun sealedAmulet(): ItemInstance {
        val template = index.templatesBySlot.getValue(Slot.AMULET).first { !it.unique }
        val plain = { source: Source -> def { it.source == source && !it.crafted && it.influence == null && MAP_TAG !in it.tags } }
        val rolls = listOf(
            roll(plain(Source.IMPLICIT)),
            roll(plain(Source.CORRUPTION)),
            roll(plain(Source.PREFIX), fractured = true),
            roll(plain(Source.ESSENCE)),
            roll(plain(Source.HANDCRAFTED)),
            roll(def { it.crafted && it.affix }),
            roll(def { it.influence == Influence.SHAPER }),
            roll(def { it.source == Source.PREFIX && !it.crafted && it.influence == null && MAP_TAG !in it.tags && it.family != plain(Source.PREFIX).family }),
            roll(plain(Source.SUFFIX)),
        )
        return ItemInstance("shot-amulet", template.code, Rarity.RARE, rolls, corrupted = true, influence = Influence.SHAPER, quality = 20, itemLevel = 84)
    }

    /** Редкая карта тира 7, захваченная Зодчим, качество 12%: алхимия и строки карты. */
    private fun capturedMap(): ItemInstance {
        val template = index.templatesBySlot.getValue(Slot.MAP).first()
        val mapLines = index.definitions.filter { it.rolls && it.affix && MAP_TAG in it.tags }.take(2)
        val rolls = listOf(roll(def { it.source == Source.ALCHEMY })) + mapLines.map { roll(it) }
        val zone = index.campaign.zones.last().code
        return ItemInstance("shot-map", template.code, Rarity.RARE, rolls, influence = Influence.SHAPER, quality = 12, itemLevel = 80, mapTier = 7, mapZone = zone)
    }

    /** Окно обязательного обновления: найдено (версии чипами, заметки с прокруткой, «Обновить», размер) и загрузка (тонкая полоса и «64% · 24 из 38 МБ»). */
    @Test fun update() = shot("update", scroll = false) {
        val notes = (1..9).joinToString("\n") { "• Строки предмета сгруппированы по источникам, пункт $it" }
        val update = AvailableUpdate(AppBuild(9999, "0.17.0", 1, 1, "app.apk", 39_845_888, "", notes), "", "https://github.com")
        Box(Modifier.weight(1f)) { UpdateDialog(update, UpdateState(checking = false, update = update), {}, {}) }
        Box(Modifier.weight(1f)) { UpdateDialog(update, UpdateState(checking = false, update = update, progress = .64f), {}, {}) }
    }

    /** Модерация: плотные строки и раскрытые тикеты - новый (Отклонить / В GitHub) и отправленный с ответом (Закрыть… / Issue #N ↗). */
    @Test fun moderation() = shot("moderation") {
        val vm = GlobalContext.get().get<FeedbackViewModel>().model
        val account = AccountUi(Session(DEFAULT_SERVER, profile = UserProfile("owner", role = "MODERATOR"), signedIn = true))
        fun report(id: String, text: String, status: ReportStatus, issue: IssueView? = null, kind: FeedbackKind = FeedbackKind.BUG) = AdminReport(
            FullReport(id, text, heroId = "hero", heroName = "Морвен", heroClass = heroClass.code, heroLevel = 25, status = status, kind = kind, createdAt = "2026-10-17T10:00:00", issue = issue),
            login = "sperance",
            moves = if (status == ReportStatus.CREATED) listOf(ReportStatus.SENT, ReportStatus.REJECTED) else emptyList(),
            deletable = true,
        )
        ReportLine(report("r0", "Убери на всех экранах названия экранов и слоганы под ними, они только занимают место", ReportStatus.SENT, IssueView(258))) {}
        AdminTicket(account, vm, report("r1", "В походе отображается «+0/с» и съезжает весь текст. Поправь глобально нормально", ReportStatus.CREATED)) {}
        val answered = IssueView(265, "https://github.com", notes = listOf(IssueNote(NoteAuthor.DEVELOPER, "Спасибо, поправим в следующей версии.")))
        AdminTicket(account, vm, report("r2", "В ремёслах текст текущих работ съехал, поправь и сделай красиво", ReportStatus.SENT, answered)) {}
        ReportLine(report("r3", "Жалоба на письмо: оскорбления", ReportStatus.CREATED, kind = FeedbackKind.COMPLAINT)) {}
    }

    /** Плашка идущей работы: длинное имя, часы цикла и три фишки выхода в час с «+N», строка уровня и итоги одним рядом. */
    @Test fun workPlaque() = shot("work") {
        val (profession, job) = index.professions.professions.flatMap { p -> p.jobs.filterIsInstance<Job.Item>().map { p to it } }.maxBy { (_, j) -> j.extra.size * 100 + jobTitle(j.code).length }
        val now = System.currentTimeMillis()
        val view = JobView(job, cycleMillis = 30_000, nothing = 10.0, extra = (job.extra + index.items.keys.take(3).map { JobExtra(it.value, 2.0) }).take(4), experience = 12.0)
        val held = ProfessionView(profession.code, level = 14, experience = 600.0, next = 1000.0, jobs = listOf(view))
        val work = WorkView(profession.code, job.code, settledAt = now - 12_000, cycleMillis = 30_000, startedAt = now - 65 * 60_000, totals = WorkTally(cycles = 130, nothing = 2, items = mapOf(job.output to 118), experience = 12_345.0, levels = 1))
        val vm: CraftsViewModel = GlobalContext.get().get()
        WorkPlaque(game(40), vm, Crafts(CraftsState(now = now, professions = listOf(held), work = work), readAt = now), 0L)
    }

    /** Состояние слота умения на снимке панели боя. */
    private data class SlotState(val ready: Float, val affordable: Boolean, val seconds: Double = 0.0, val casting: Float = 0f)

    /** Класс, чьи четыре открытых к [level] активных умения с самыми длинными именами длиннее всех, и эти умения. */
    private fun longestActives(level: Int): Pair<HeroClass, List<SkillDefinition>> = index.classes.classes.map { c ->
        c to index.skills.ofClass(c.code).filter { it.unlock <= level && it.kind == SkillKind.ACTIVE }.sortedByDescending { SkillText.title(it.code).length }.take(SLOTS)
    }.maxBy { (_, skills) -> skills.sumOf { SkillText.title(it.code).length } }

    /** Строка модификатора [code] серединой первого тира. */
    private fun line(code: String): Line? = index.modifier(ModifierCode(code))?.let { def -> Line(def.code, def.tiers.firstOrNull()?.values.orEmpty().map { it.average() }) }

    private fun game(level: Int, code: String = heroClass.code): GameUi {
        val owner = index.classes.classes.first { it.code == code }
        val info = HeroInfo("hero", "owner", "Изгнанник", heroClass = code, level = level)
        val hero = HeroView(info, emptyList(), sheet = Sheets.calculate(index, level, code, listOf(TakenNode(owner.startNode)), emptyList()))
        return GameUi(
            activity = Activity(busy = false),
            session = Session(DEFAULT_SERVER, profile = UserProfile("owner"), signedIn = true),
            world = World(content = index, contentHash = index.hash),
            holding = HeroHolding(heroId = "hero", hero = hero, owner = "owner"),
        )
    }

    /**
     * Экран [content] шириной 360 dp без анимаций; [open] - текст или описание касания, по которому нажать перед снимком (свёрнутое раскрыть); [frozen] -
     * тестовые часы Compose стоят: снимок не ждёт покоя того, что движется кадрами.
     */
    private fun shot(name: String, scroll: Boolean = true, open: String? = null, frozen: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
        if (frozen) compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalMotion provides false, LocalWallClock provides StillClock(System.currentTimeMillis())) {
                ForgeTheme {
                    Column(
                        Modifier.width(360.dp).background(Ink).then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = content,
                    )
                }
            }
        }
        open?.let { compose.onAllNodes(hasText(it) or hasContentDescription(it)).onFirst().performClick() }
        if (frozen) compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design/shots").apply { mkdirs() }
        File(directory, "$name.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, SHOT_QUALITY, it) }
    }

    private companion object {
        const val SHOT_QUALITY = 75

        /** Тег строк карты: на снимке вещи их нет, на снимке карты - только они. */
        const val MAP_TAG = "map"

        /** Активных слотов умений. */
        const val SLOTS = 4

        /** Высота листа алтаря: два варианта и «Пройти мимо» целиком (на 640 dp кнопка срезалась). */
        val ALTAR_HEIGHT = 780.dp
    }
}
