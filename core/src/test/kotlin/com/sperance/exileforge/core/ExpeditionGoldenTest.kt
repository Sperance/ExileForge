package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.Flask
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.RunJournal
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.run.AutoPlan
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.campaign.run.RunPhase
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.ActiveSlot
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.ChestWindow
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.ItemFactory
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.rules.run.RunEvent
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Golden-тест забега: несколько автозабегов (класс, зона, уровень, seed) на фиксированном шаге против
 * записанного журнала. Распил `ExpeditionRun`/`ExpeditionWorld` не должен сдвинуть ни одно событие;
 * осознанная смена правил или контента перезаписывает снимки: `GOLDEN_UPDATE=1 ./gradlew :core:test`.
 */
class ExpeditionGoldenTest {
    private val index get() = TestContent.index
    private val golden = listOf(File("src/test/resources/golden"), File("core/src/test/resources/golden")).first { it.parentFile.isDirectory }

    private data class Case(val heroClass: Int, val zone: Int, val level: Int, val seed: Long)

    private val cases = listOf(Case(0, 0, 12, 3L), Case(1, 1, 20, 11L), Case(2, 2, 30, 2024L))

    @Test
    fun a_fixed_autorun_plays_the_recorded_journal() {
        val update = System.getenv("GOLDEN_UPDATE") == "1"
        golden.mkdirs()
        cases.forEach { case ->
            val name = "run-${case.heroClass}-${case.zone}-${case.level}-${case.seed}.txt"
            val actual = play(case)
            val file = File(golden, name)
            if (update || !file.exists()) {
                file.writeText(actual)
            } else {
                assertEquals(file.readText(), actual, "$name diverges; GOLDEN_UPDATE=1 rewrites it when the rules changed on purpose")
            }
        }
        assertTrue(golden.listFiles().orEmpty().count { it.name.startsWith("run-") } >= cases.size)
    }

    /** A whole autorun on fixed dice and a fixed step: the journal line by line, then the run's figures. */
    private fun play(case: Case): String {
        val heroClass = index.classes.classes[case.heroClass % index.classes.classes.size]
        val zone = index.campaign.zones[case.zone % index.campaign.zones.size]
        val sheet = Sheets.calculate(index, case.level, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        // The class's first active skill, as a fresh hero wields it.
        val skill = index.skills.ofClass(heroClass.code).firstOrNull { it.type.kind == SkillKind.ACTIVE && it.unlock <= case.level }
        val skills = HeroSkills(learned = skill?.let { mapOf(it.code to case.level.coerceAtMost(20)) }.orEmpty(), active = listOf(skill?.let { ActiveSlot(it.code, SlotCondition.READY) }))
        // Two plain life flasks on the belt: a naked hero would fall before the boss.
        val flasks = List(2) { n ->
            val template = index.template(FLASK) ?: return@List null
            Flask.of(ItemFactory(index).create("flask-$n", template, Rarity.COMMON, Dice(case.seed + n)), template, index, SlotCondition.READY)
        }
        val gear = HeroGear(sheet.stats, case.level, sheet.model, HeroStance.of(heroClass.code), Loadout.of(skills, index.skills, heroClass.code, flasks, index.powers, index.rules.charges), index.stats.percent)
        val run = Run(index, zone, case.seed, RunContext(heroClass.code, case.level))
        val journal = RunJournal("run-${case.seed}", "hero", zone.code)
        val campaign = CampaignState(chests = mapOf(zone.code to ChestWindow(Long.MAX_VALUE, 2)))
        val expedition = ExpeditionRun.start(index, zone, run, journal, gear, campaign, 0L, 0.0, case.level, { 0L }, auto = AutoPlan(abyss = false))
        var guard = 0
        while (guard++ < 200_000) {
            expedition.update(STEP)
            val hud = expedition.hud.value
            when (hud.phase) {
                RunPhase.DEAD, RunPhase.CLEARED, RunPhase.LEFT -> break

                // The portal's gate waits for the player: a golden run never enters the Vaal zone.
                RunPhase.GATE -> expedition.send(RunCommand.ShutGate(false))

                RunPhase.MAP -> if (hud.auto == null) expedition.send(RunCommand.Leave)

                else -> Unit
            }
        }
        val hud = expedition.hud.value
        val share = expedition.share()
        return buildString {
            appendLine("hero=${heroClass.code} zone=${zone.code} level=${case.level} seed=${case.seed} skill=${skill?.code}")
            appendLine("phase=${hud.phase} life=${expedition.heroLife.f()} seconds=${share.seconds.f()} kills=${share.kills} bosses=${share.bosses} deaths=${share.deaths} events=${journal.size}")
            journal.all.forEach { e ->
                val fight = (e as? RunEvent.Fight)?.fight?.let { t -> "fight=${t.hits}/${t.crits}/${t.misses}/${t.blocked}/${t.evaded}/${t.ailments} taken=${t.taken} healed=${t.healed} max=${t.maxHit} dealt=${t.dealt.toSortedMap()}" }.orEmpty()
                appendLine((e.flat() + fight).joinToString(" ").trimEnd())
            }
        }
    }

    /** The event as the journal wrote it before the sealed events (server 1.75.0): the golden files stay the same. */
    private fun RunEvent.flat(): List<Any> {
        val index = when (this) {
            is RunEvent.Chest -> index
            is RunEvent.Crystal -> index
            is RunEvent.CrystalVaal -> index
            is RunEvent.AbyssOpen -> index
            else -> 0
        }
        val vaal = (this as? RunEvent.Kill)?.vaal ?: (this as? RunEvent.Fall)?.vaal ?: false
        val claim = this as? RunEvent.AbyssClaim
        return listOf(n, kind, (this as? RunEvent.Kill)?.i ?: 0, (this as? RunEvent.Kill)?.m ?: 0, index, claim?.depth ?: 0, claim?.fallen ?: false, vaal)
    }

    private fun Double.f() = "%.4f".format(java.util.Locale.ROOT, this)

    private companion object {
        const val STEP = 0.1
        const val FLASK = "FLASK_MEDIUM_LIFE"
    }
}
