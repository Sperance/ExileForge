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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.FeatureView
import com.sperance.exileforge.core.campaign.run.LevelUp
import com.sperance.exileforge.core.campaign.run.RoomSpot
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.hero.HeroHolding
import com.sperance.exileforge.core.i18n.LocaleBundle
import com.sperance.exileforge.core.i18n.serverLocale
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.core.model.command.UserProfile
import com.sperance.exileforge.core.model.hero.HeroInfo
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.core.world.World
import com.sperance.exileforge.data.settings.DEFAULT_SERVER
import com.sperance.exileforge.presentation.admin.AdminViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.ActiveSlot
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Line
import com.sperance.exileforge.rules.content.ModifierCode
import com.sperance.exileforge.rules.content.Quest
import com.sperance.exileforge.rules.content.QuestCondition
import com.sperance.exileforge.rules.content.QuestKind
import com.sperance.exileforge.rules.content.QuestReward
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.run.AltarPact
import com.sperance.exileforge.rules.run.MapFeature
import com.sperance.exileforge.rules.run.MapMechanic
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.screens.admin.AdminPanels
import com.sperance.exileforge.ui.screens.expedition.AltarSheet
import com.sperance.exileforge.ui.screens.expedition.FeatureSheet
import com.sperance.exileforge.ui.screens.expedition.LevelUpScreen
import com.sperance.exileforge.ui.screens.expedition.NodeSheet
import com.sperance.exileforge.ui.screens.expedition.scene.ExpeditionScene
import com.sperance.exileforge.ui.screens.quests.BookChoice
import com.sperance.exileforge.ui.screens.quests.QuestRow
import com.sperance.exileforge.ui.screens.quests.StoryCodex
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
 * Снимки ключевых экранов (0.14.0, UI-SHOTS): каждый экран рисуется над контентом закреплённого сервера по-русски, без анимаций,
 * и ложится в `design/shots/<имя>.jpg` - CI выгружает их артефактом `exile-forge-shots`; что обрезано или наехало, видно там.
 * Экраны: задания и контракты (макет B), гримуар 4+3, лист алтаря, книга главы, окно уровня, Кодекс, узел Атласа с исключением,
 * карта со стенами и дверью комнаты, панель боя на 4 слота, окно сбора, админка.
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

    @Test fun grimoire() = shot("grimoire") {
        val level = 50
        val own = index.skills.ofClass(heroClass.code).filter { it.unlock <= level }
        val actives = own.filter { it.kind == SkillKind.ACTIVE }.take(4)
        val passives = own.filter { it.kind == SkillKind.PASSIVE }.take(3)
        val skills = HeroSkills(
            learned = (actives + passives).associate { it.code to 5 },
            active = actives.map { ActiveSlot(it.code, SlotCondition.READY) },
            passive = passives.map { it.code },
        )
        val stats = Sheets.calculate(index, level, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList()).stats
        Slots(index, skills, level, stats) {}
        (actives + passives).forEach { Page(game(level), index, it, skills, level, stats, null) {} }
    }

    @Test fun altar() = shot("altar", scroll = false) {
        val offers = index.campaign.features?.altar?.offers.orEmpty().take(2)
        val pacts = offers.mapNotNull { offer -> line(offer.boon)?.let { boon -> line(offer.curse)?.let { curse -> AltarPact(boon, curse, offer.code, offer.target) } } }
        Box(Modifier.height(640.dp)) { AltarSheet(game(40), MapFeature.Altar(0, pacts), {}, {}) }
    }

    @Test fun storyBook() = shot("book") {
        BookChoice(index, 1, heroClass.code, HeroSkills(), false) { _, _ -> }
    }

    @Test fun levelUp() = shot("levelup", scroll = false) {
        Box(Modifier.height(720.dp)) { LevelUpScreen(LevelUp(9, 10), heroClass, index, false) {} }
    }

    @Test fun codex() {
        // Заголовок «Кодекс» гравирован прописными - раскрываем по подписи «развернуть»
        shot("codex", open = ui("common.expand")) { StoryCodex(index.quests.story, 3) }
    }

    @Test fun atlasExclusion() = shot("atlas") {
        // Ключевые «только Зодчий / только Пустота» (ASANA-N09): второй погашен с причиной
        val node = index.atlas.nodes.first { it.excludes.isNotEmpty() }
        val taken = setOf(index.atlasGraph.start) + node.excludes + index.atlasGraph.neighbours(node.code)
        NodeSheet(index, node, taken, available = 3, price = 100, enabled = true, onTake = {}, onRefund = {}, modifier = Modifier)
    }

    @Test fun mapWithDoor() = shot("map", scroll = false) {
        val zone = index.campaign.zones.firstOrNull { it.code.value == "C2_DEEP_ADITS" } ?: index.campaign.zones.first()
        val level = 12
        val sheet = Sheets.calculate(index, level, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        val gear = HeroGear(sheet.stats, level, sheet.model, HeroStance.of(heroClass.code), Loadout.of(HeroSkills(), index, heroClass.code, emptyList(), sheet.stats, index.powers, index.rules.charges), index.stats.percent)
        val run = Run(index, zone, 7L, RunContext(heroClass.code, level, forced = setOf(MapMechanic.VAULT)))
        val expedition = ExpeditionRun.start(index, zone, run, RunJournal("run-7", "hero", zone.code.value), gear, CampaignState(), System.currentTimeMillis(), 0.0, level, { 0L })
        // Герой - у двери комнаты, если она встала: дверь и рычаг видны на снимке
        expedition.world.features.filterIsInstance<RoomSpot>().firstOrNull()?.let { room ->
            expedition.world.heroX = room.entrance.x + .5
            expedition.world.heroY = room.entrance.y + 1.5
        }
        expedition.update(0.0)
        ExpeditionScene(expedition, mutableFloatStateOf(0f), heroClass.code, Modifier.fillMaxWidth().height(640.dp))
    }

    /** Окно сбора (ASANA-12, шаблон листа объекта карты): узел ремесла на карте, полоса сбора на 40%. */
    @Test fun gathering() = shot("gathering", scroll = false) {
        val level = 20
        val zone = index.campaign.zones.first { it.level >= level }
        val run = Run(index, zone, 7L, RunContext(heroClass.code, level, forced = setOf(MapMechanic.NODES)))
        val node = run.features.all.filterIsInstance<MapFeature.Node>().first()
        Box(Modifier.height(640.dp)) { FeatureSheet(game(level), FeatureView(node, emptyList(), gathering = .4f)) {} }
    }

    /** Админка с ролью администратора (ADMIN-UI): промокоды, выдачи, режим. */
    @Test fun admin() = shot("admin", scroll = false) {
        val vm: AdminViewModel = GlobalContext.get().get()
        val plain = game(40)
        val game = plain.copy(session = plain.session.copy(profile = UserProfile("owner", role = "ADMIN")))
        Box(Modifier.height(1400.dp)) { AdminPanels(game, vm) }
    }

    /** Строка модификатора [code] серединой первого тира. */
    private fun line(code: String): Line? = index.modifier(ModifierCode(code))?.let { def -> Line(def.code, def.tiers.firstOrNull()?.values.orEmpty().map { it.average() }) }

    private fun game(level: Int): GameUi {
        val info = HeroInfo("hero", "owner", "Изгнанник", heroClass = heroClass.code, level = level)
        val hero = HeroView(info, emptyList(), sheet = Sheets.calculate(index, level, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList()))
        return GameUi(
            activity = Activity(busy = false),
            session = Session(DEFAULT_SERVER, profile = UserProfile("owner"), signedIn = true),
            world = World(content = index, contentHash = index.hash),
            holding = HeroHolding(heroId = "hero", hero = hero, owner = "owner"),
        )
    }

    /** Экран [content] шириной 360 dp без анимаций; [open] - текст, по которому нажать перед снимком (свёрнутое раскрыть). */
    private fun shot(name: String, scroll: Boolean = true, open: String? = null, content: @Composable ColumnScope.() -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalMotion provides false) {
                ForgeTheme {
                    Column(
                        Modifier.width(360.dp).background(Ink).then(if (scroll) Modifier.verticalScroll(rememberScrollState()) else Modifier).padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        content = content,
                    )
                }
            }
        }
        open?.let { compose.onNodeWithText(it).performClick() }
        compose.waitForIdle()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val directory = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "design/shots").apply { mkdirs() }
        File(directory, "$name.jpg").outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, SHOT_QUALITY, it) }
    }

    private companion object {
        const val SHOT_QUALITY = 75
    }
}
