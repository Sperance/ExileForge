package com.sperance.exileforge.core

import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.rules.content.AtlasStat
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.Effect
import com.sperance.exileforge.rules.content.MapStat
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every line a modifier or a unique can roll (3.81.0) is counted: its stat is registered and is one the engine reads — by name
 * in the registries, as a power, or as one of the families the fight builds its codes for at run time. Текст строки на каждом
 * языке проверяет один тест - `LocalizationTest` сервера (4.12.x, TEST-05).
 */
class ModifierCoverageTest {
    private val index = TestContent.index

    private val effects: List<Effect> by lazy { index.families.values.flatMap { it.effects } }

    /**
     * Stats the fight reads by a code it assembles — one per element, ailment, charge or buff —, a flask's own lines (read off
     * the flask, not the sheet), the foes' auras, and the veil's mark that the unveiling orb looks for.
     */
    private val families = listOf(
        "FLASK_", "AURA_", "STOCK_VEILED",
        "STOCK_RESIST_", "STOCK_ATTACK_", "STOCK_PENETRATE_", "STOCK_SPELL_ADD_", "STOCK_DAMAGE_VS_", "STOCK_AVOID_", "STOCK_LEECH_",
        "STOCK_CRITICAL_", "STOCK_IMMUNE_", "STOCK_DAMAGE_PER_", "STOCK_PHYSICAL_AS_EXTRA_", "STOCK_PHYSICAL_TAKEN_AS_",
    )

    private fun read(stat: String): Boolean = stat in CoreStat.entries.map { it.code } ||
        stat in MapStat.entries.map { it.code } ||
        stat in AtlasStat.entries.map { it.code } ||
        index.powers.byStat.containsKey(stat) ||
        families.any { stat.startsWith(it) } ||
        stat.endsWith("_ALWAYS") || stat.endsWith("_DURATION") || stat.endsWith("_DURATION_ON_SELF") ||
        index.campaign.combat.ceilings.all.any { it.raise == stat } ||
        DamageType.entries.any { stat.endsWith("_${it.name}") }

    @Test
    fun everyRolledStatIsRegistered() {
        val missing = effects.map { it.stat }.distinct().filter { it !in index.stats }
        assertTrue(missing.isEmpty(), "not in stats.json: $missing")
    }

    @Test
    fun everyRolledStatIsReadByTheEngine() {
        val dead = effects.map { it.stat }.distinct().filterNot(::read)
        assertTrue(dead.isEmpty(), "stats nothing reads: $dead")
    }
}
