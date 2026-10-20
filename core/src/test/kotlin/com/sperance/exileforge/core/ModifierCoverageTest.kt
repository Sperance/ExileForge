package com.sperance.exileforge.core

import com.sperance.exileforge.rules.content.AtlasStat
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.Effect
import com.sperance.exileforge.rules.content.FlaskStat
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.ModifierDef
import com.sperance.exileforge.rules.content.StatGroup
import com.sperance.exileforge.rules.sheet.MightStats
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Every line a modifier or a unique can roll is counted: its stat is registered and is one the engine reads — by name
 * in the registries of the rules or as a power (no name prefixes: a new dead stat with a familiar name is caught too). Текст строки на каждом
 * языке проверяет один тест - `LocalizationTest` сервера.
 */
class ModifierCoverageTest {
    private val index = TestContent.index

    private val effects: List<Effect> by lazy { index.families.values.flatMap { it.effects } }

    /**
     * Что читает движок: реестры правил, а не префиксы имён - характеристики ядра, карт, Атласа и фляг, силы,
     * повышаемые пределы, перечень боевых характеристик героя Мощи ([MightStats]: каждую он относит к учтённым, сведённым или
     * читаемым боем вне Мощи), ауры врагов (группа `AURA` - их все читает бой), механики фляг - реестр читателей `FlaskStat`
     * метка скрытой строки (её ищет сфера раскрытия) и строки места (`room`: их читают места аффиксов вещи - `ContentIndex.limits`).
     */
    private val read: Set<String> by lazy {
        buildSet {
            CoreStat.entries.mapTo(this) { it.code }
            MapStat.entries.mapTo(this) { it.code }
            AtlasStat.entries.mapTo(this) { it.code }
            FlaskStat.entries.mapTo(this) { it.code }
            addAll(index.powers.byStat.keys)
            index.campaign.combat.ceilings.all.mapTo(this) { it.raise }
            addAll(MightStats.COUNTED + MightStats.FOLDED + MightStats.OUTSIDE)
            index.stats.ofGroup(StatGroup.AURA).mapTo(this) { it.code }
            index.families.values.filter { ModifierDef.VEILED in it.tags }.flatMapTo(this) { def -> def.effects.map { it.stat } }
            index.families.values.filter { it.room }.flatMapTo(this) { def -> def.effects.map { it.stat } }
        }
    }

    @Test
    fun everyRolledStatIsRegistered() {
        val missing = effects.map { it.stat }.distinct().filter { it !in index.stats }
        assertTrue(missing.isEmpty(), "not in stats.json: $missing")
    }

    @Test
    fun everyRolledStatIsReadByTheEngine() {
        val dead = effects.map { it.stat }.distinct().filterNot { it in read }
        assertTrue(dead.isEmpty(), "stats nothing reads: $dead")
    }
}
