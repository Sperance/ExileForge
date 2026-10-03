package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.MapEffects
import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.traitsIn
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import java.io.File
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Golden-тест боя: несколько фиксированных боёв (класс, зона, seed) против записанного лога.
 * Распил `Combat.kt` не должен изменить ни одного удара; изменение контента или правил боя
 * (новый `RULES_VERSION`) перезаписывает снимки: `GOLDEN_UPDATE=1 ./gradlew :core:test`.
 */
class CombatGoldenTest {
    private val index get() = TestContent.index
    private val golden = listOf(File("src/test/resources/golden"), File("core/src/test/resources/golden")).first { it.parentFile.isDirectory }

    private data class Case(val heroClass: Int, val zone: Int, val level: Int, val seed: Long)

    private val cases = listOf(Case(0, 0, 1, 1L), Case(1, 0, 3, 7L), Case(2, 1, 5, 42L), Case(0, 2, 12, 1234L))

    @Test
    fun a_fixed_fight_plays_the_recorded_log() {
        val update = System.getenv("GOLDEN_UPDATE") == "1"
        golden.mkdirs()
        cases.forEach { case ->
            val name = "battle-${case.heroClass}-${case.zone}-${case.level}-${case.seed}.txt"
            val actual = play(case)
            val file = File(golden, name)
            if (update || !file.exists()) {
                file.writeText(actual)
            } else {
                assertEquals(file.readText(), actual, "$name diverges; GOLDEN_UPDATE=1 rewrites it when the rules changed on purpose")
            }
        }
        assertTrue(golden.listFiles().orEmpty().size >= cases.size)
    }

    /** A whole fight on fixed dice, one line per event, numbers to four places. */
    private fun play(case: Case): String {
        val heroClass = index.classes.classes[case.heroClass % index.classes.classes.size]
        val zone = index.campaign.zones[case.zone % index.campaign.zones.size]
        val rules = index.campaign.combat
        val sheet = Sheets.calculate(index, case.level, heroClass.code, listOf(TakenNode(heroClass.startNode)), emptyList())
        val hero = Combatant(sheet.stats, case.level, rules)
        val run = Run(index, zone, case.seed, RunContext(heroClass.code, case.level))
        val pack = Spawns(index, run).packs(false, MapEffects.buffs(emptyMap())).first()
        val foes = pack.map { monster ->
            val level = monster.level.takeIf { it > 0 } ?: zone.level
            Foe(Combatant(monster.stats, level, rules), monster.rarity, monster.skills.mapNotNull(index.skills.monsterByCode::get), monster, level, monster.traitsIn(index), index.campaign.traits.power(monster.rarity))
        }
        val battle = Battle(hero, foes, rules, hero.maxLife, Random(case.seed))
        var guard = 0
        while (battle.outcome == null && guard++ < 100_000) battle.advance(0.1)
        val log = battle.log()
        return buildString {
            appendLine("hero=${heroClass.code} zone=${zone.code} level=${case.level} seed=${case.seed}")
            appendLine("outcome=${log.outcome} life=${log.heroLife.f()} duration=${log.duration.f()} events=${log.events.size}")
            log.events.forEach { e ->
                appendLine(
                    listOf(e.time.f(), e.actor, e.action, e.kind, e.damage.f(), e.type, e.healed.f(), e.stunned, e.inflicted.joinToString("+"), e.ailment, e.heroLife.f(), e.heroShield.f(), e.monsterLife.f(), e.monsterShield.f(), e.foe, e.skill, e.onSelf, e.heroMana.f(), e.pet).joinToString(" "),
                )
            }
        }
    }

    private fun Double.f() = "%.4f".format(java.util.Locale.ROOT, this)
}
