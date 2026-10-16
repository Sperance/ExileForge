package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.rules.content.BrewStat
import com.sperance.exileforge.rules.content.BuffKind
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.MapStat
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.roll.GuardianFloor
import com.sperance.exileforge.rules.roll.MonsterEffect
import com.sperance.exileforge.rules.sheet.Shift
import com.sperance.exileforge.rules.sheet.SourceKind
import com.sperance.exileforge.rules.sheet.StatSource
import kotlin.math.abs
import kotlin.math.max

/** What a line of a map is, as the launch window marks it. */
enum class MapLineKind {
    HARM,
    CONTENT,
    REWARD,
    ;

    companion object {
        /** Строки, что дают без спроса. */
        private val rewards: Set<String> = listOf(
            MapStat.QUANTITY, MapStat.RARITY, MapStat.GOLD, MapStat.CHESTS, MapStat.HERO_HASTE,
            MapStat.HERO_ATTACK_SPEED, MapStat.HERO_LIFE, MapStat.HERO_LEECH, MapStat.CRYSTALS, MapStat.BOOKS, MapStat.ABYSS_CRACKS,
            MapStat.ABYSS_DEPTH, MapStat.ABYSS_HOARD, MapStat.ABYSS_UNIQUE, MapStat.ABYSS_ORBS, MapStat.ABYSS_RARE,
        ).mapTo(HashSet()) { it.code }

        /** What a map's stat does, for its line's mark: a harm pays by the rules' risk, a reward pays itself, the rest is content. */
        fun of(index: ContentIndex, stat: String): MapLineKind = when {
            stat in rewards -> REWARD
            stat in index.campaign.maps.risk -> HARM
            else -> CONTENT
        }

        /** How many percent one rolled value of [stat] pays, by the rules' weight; zero for what is not a risk. */
        fun risk(index: ContentIndex, stat: String, value: Double): Double = value * (index.campaign.maps.risk[stat] ?: 0.0)
    }
}

/**
 * What a map does to a run. The fight is the client's, so the map's summed effects — the run context's
 * `active.effects` — are applied here: monster buffs fold by the modifier formula over the rolled stats, the
 * hero's debuffs take their share of the sheet. What the map pays is rolled by the rules from the same context.
 */
object MapEffects {
    /** Удары монстра (STAT-MAP-03): все виды его урона, и чары, - одна точка правил. */
    private val damage = GuardianFloor.STRIKES
    private val resists = DamageType.entries.mapNotNull { it.resist }

    /** The hero's critical chances a map lowers together: the attacks' and the spells' own (server 1.56.0). */
    private val criticalChances = listOf(CoreStat.CRITICAL_CHANCE.code, CoreStat.SPELL_CRITICAL_CHANCE.code)

    /** What a map adds to every monster on it, as effects folded by the same formula as a modifier. */
    fun buffs(effects: Map<String, Double>): List<MonsterEffect> = buildList {
        effects[MapStat.MONSTER_LIFE.code]?.let { add(MonsterEffect(CoreStat.HEALTH.code, Op.INCREASED, it)) }
        effects[MapStat.MONSTER_DAMAGE.code]?.let { v -> damage.forEach { add(MonsterEffect(it, Op.INCREASED, v)) } }
        effects[MapStat.MONSTER_SPEED.code]?.let { v -> listOf(CoreStat.ATTACK_SPEED.code, CoreStat.CAST_SPEED.code).forEach { add(MonsterEffect(it, Op.INCREASED, v)) } }
        effects[MapStat.MONSTER_RESIST.code]?.let { v -> resists.forEach { add(MonsterEffect(it, Op.ADD, v)) } }
        effects[MapStat.MONSTER_PENETRATION.code]?.let { add(MonsterEffect(CoreStat.PENETRATE_ELEMENTAL.code, Op.ADD, it)) }
        effects[MapStat.MONSTER_REFLECT.code]?.let { add(MonsterEffect(CoreStat.REFLECT.code, Op.ADD, it)) }
        effects[MapStat.MONSTER_CRITICAL.code]?.let { add(MonsterEffect(CoreStat.CRITICAL_CHANCE.code, Op.ADD, it)) }
        effects[MapStat.MONSTER_AILMENTS.code]?.let { v -> Ailment.entries.filter { it.general }.forEach { add(MonsterEffect("STOCK_${it.word}_CHANCE", Op.ADD, v)) } }
        // Изранение и иссушение (4.11.0) - своя строка карты, не общая «шанс наложить состояние»
        effects[MapStat.MONSTER_WITHER.code]?.let { v -> listOf(CoreStat.WOUND_CHANCE.code, CoreStat.PARCH_CHANCE.code).forEach { add(MonsterEffect(it, Op.ADD, v)) } }
        effects[MapStat.MONSTER_ARMOUR.code]?.let { v -> listOf(CoreStat.ARMOR.code, CoreStat.EVASION.code).forEach { add(MonsterEffect(it, Op.INCREASED, v)) } }
        effects[MapStat.MONSTER_LEECH.code]?.let { add(MonsterEffect(CoreStat.LEECH_ALL.code, Op.ADD, it)) }
        effects[MapStat.MONSTER_STUN.code]?.let { add(MonsterEffect(CoreStat.AVOID_STUN.code, Op.ADD, it)) }
        effects[MapStat.MONSTER_CAST.code]?.let { add(MonsterEffect(CoreStat.COOLDOWN_RECOVERY.code, Op.ADD, it)) }
        // 3.35.0 (server 1.34.0): buffs the pack wears from the start, and a share of its physical damage added as the elements.
        effects[MapStat.MONSTER_ONSLAUGHT.code]?.takeIf { it > 0 }?.let { add(MonsterEffect(BuffKind.ONSLAUGHT.always, Op.ADD, 1.0)) }
        effects[MapStat.MONSTER_FORTIFY.code]?.takeIf { it > 0 }?.let { add(MonsterEffect(BuffKind.FORTIFY.always, Op.ADD, 1.0)) }
        effects[MapStat.MONSTER_EXTRA_ELEMENTAL.code]?.let { v -> DamageType.ELEMENTS.forEach { add(MonsterEffect("STOCK_PHYSICAL_AS_EXTRA_${it.name}", Op.ADD, v / DamageType.ELEMENTS.size)) } }
        // Стойкость к подавлению восстановления (4.2.0)
        effects[MapStat.MONSTER_UNSUPPRESSED.code]?.let { add(MonsterEffect(CoreStat.SUPPRESSION_AVOID.code, Op.ADD, it)) }
    }

    /** A crystal's guardian beyond its rarity: so many percent more life and damage when a Vaal orb made it [stronger], and by the atlas's power of guardians. */
    fun guardianBuffs(stronger: Boolean, strongerBy: Double, effects: Map<String, Double>): List<MonsterEffect> {
        val power = (if (stronger) strongerBy else 0.0) + (effects[AtlasEffects.GUARDIAN_POWER] ?: 0.0)
        return if (power <= 0) emptyList() else (listOf(CoreStat.HEALTH.code) + damage).map { MonsterEffect(it, Op.MORE, power) }
    }

    /** What a map does to its boss alone: so many percent more life and damage. */
    fun bossBuffs(effects: Map<String, Double>): List<MonsterEffect> = effects[MapStat.BOSS_POWER.code]?.takeIf { it > 0 }?.let { v -> (listOf(CoreStat.HEALTH.code) + damage).map { MonsterEffect(it, Op.MORE, v) } }.orEmpty()

    fun hero(stats: Map<String, Double>, effects: Map<String, Double>): Map<String, Double> {
        if (effects.isEmpty()) return stats
        val sheet = stats.toMutableMap()
        effects[MapStat.HERO_RESIST.code]?.let { v -> resists.forEach { sheet[it] = (stats[it] ?: 0.0) - v } }
        fun add(stat: String, v: Double) {
            sheet[stat] = (sheet[stat] ?: 0.0) + v
        }
        // «Меньше регенерации» карты (4.12.x, STAT-MAP-07) - общая строка листа `STOCK_LIFE_REGEN_LESS`: её читает `Recovery` - дно,
        // вампиризм и регенерация одной долей
        effects[MapStat.HERO_REGEN.code]?.let { add(CoreStat.LIFE_REGEN_LESS.code, it) }
        fun scale(stat: String, share: Double) {
            sheet[stat] = (sheet[stat] ?: 0.0) * max(0.0, 1 + share / 100)
        }
        // Дары Атласа - прибавки к листу до «меньше» карты (4.8.0): штраф карты урезает и их, а не только родной реген маны
        AtlasEffects.hero.forEach { (atlas, stat) -> effects[atlas]?.let { add(stat, it) } }
        effects[MapStat.HERO_RECOVERY.code]?.let { add(CoreStat.RECOVERY_RATE.code, -it) }
        effects[MapStat.HERO_MAX_RESIST.code]?.let { v ->
            add(CoreStat.RESIST_MAX_ALL.code, -v)
            add(CoreStat.RESIST_MAX_CHAOS.code, -v)
        }
        effects[MapStat.HERO_DEFENCES.code]?.let { v -> listOf(CoreStat.ARMOR.code, CoreStat.EVASION.code, CoreStat.ENERGY_SHIELD.code).forEach { scale(it, -v) } }
        effects[MapStat.HERO_BLOCK.code]?.let { add(CoreStat.BLOCK_CHANCE.code, -it) }
        effects[MapStat.HERO_CRIT.code]?.let { v -> criticalChances.filter { it in sheet }.forEach { scale(it, -v) } }
        effects[MapStat.HERO_HASTE.code]?.let { add(CoreStat.MOVEMENT_SPEED.code, it) }
        effects[MapStat.HERO_ATTACK_SPEED.code]?.let { scale(CoreStat.ATTACK_SPEED.code, it) }
        effects[MapStat.HERO_LIFE.code]?.let { scale(CoreStat.HEALTH.code, it) }
        effects[MapStat.HERO_LEECH.code]?.let { add(CoreStat.LEECH_ALL.code, it) }
        effects[MapStat.HERO_DEGEN.code]?.let { add(CoreStat.LIFE_DEGEN_PERCENT.code, it) }
        effects[MapStat.HERO_MANA_REGEN.code]?.let { v -> sheet[CoreStat.MANA_REGEN.code] = (100 + (sheet[CoreStat.MANA_REGEN.code] ?: 0.0)) * max(0.0, 1 - v / 100) - 100 }
        // Угроза высокого тира (4.8.0): флаг листа - реген маны обнуляет общий расчёт боя и прогулки
        effects[MapStat.HERO_NO_MANA_REGEN.code]?.let { add(CoreStat.NO_MANA_REGEN.code, it) }
        effects[MapStat.SKILL_COST.code]?.let { add(CoreStat.SKILL_COST.code, -it) }
        effects[MapStat.FLASK_CHARGES.code]?.let { add(CoreStat.FLASK_CHARGES_GAINED.code, -it) }
        // 3.35.0 (server 1.34.0): the map's curses and prohibitions.
        effects[MapStat.HERO_ENFEEBLE.code]?.let { v -> sheet[CoreStat.DAMAGE.code] = (100 + (sheet[CoreStat.DAMAGE.code] ?: 0.0)) * max(0.0, 1 - v / 100) - 100 }
        effects[MapStat.HERO_VULNERABILITY.code]?.let { add(CoreStat.PHYSICAL_TAKEN.code, it) }
        effects[MapStat.HERO_FLASK_EFFECT.code]?.let { add(CoreStat.FLASK_EFFECT.code, -it) }
        effects[MapStat.HERO_BUFF_DURATION.code]?.let { add(BuffKind.DURATION, -it) }
        // The alchemist's potion of the run (3.79.0, server 1.74.0): resistances added, damage and life «more».
        effects[BrewStat.RESIST]?.let { add(CoreStat.ALL_ELEMENTAL_RESISTANCES.code, it) }
        effects[BrewStat.DAMAGE]?.let { v -> sheet[CoreStat.DAMAGE.code] = (100 + (sheet[CoreStat.DAMAGE.code] ?: 0.0)) * (1 + v / 100) - 100 }
        effects[BrewStat.LIFE]?.let { scale(CoreStat.HEALTH.code, it) }
        // Дар алтаря сделки (3.90.0, сервер 1.81.3): «больше» урона героя до конца карты.
        effects[MapStat.HERO_DAMAGE.code]?.let { v -> sheet[CoreStat.DAMAGE.code] = (100 + (sheet[CoreStat.DAMAGE.code] ?: 0.0)) * (1 + v / 100) - 100 }
        return sheet
    }

    /**
     * What each of the map's and the atlas's effects moved on the hero's sheet, by stat: every effect
     * laid alone, and whatever they do only together left on the stat's last shift, so the shifts add up to [hero].
     * 4.10.x: строка делится по источникам [origins] - доля древа гильдии и выпитого зелья кладётся своим сдвигом.
     */
    fun heroShifts(stats: Map<String, Double>, effects: Map<String, Double>, origins: EffectOrigins = EffectOrigins.NONE): Map<String, List<Shift>> {
        if (effects.isEmpty()) return emptyMap()
        val shifts = LinkedHashMap<String, MutableList<Shift>>()
        effects.forEach { (code, value) ->
            origins.parts(code, value).forEach { (source, share) ->
                hero(stats, mapOf(code to share)).forEach { (stat, moved) ->
                    val delta = moved - (stats[stat] ?: 0.0)
                    if (abs(delta) >= MOVED) shifts.getOrPut(stat) { mutableListOf() } += Shift(source, delta)
                }
            }
        }
        hero(stats, effects).forEach { (stat, joint) ->
            val own = shifts[stat] ?: return@forEach
            val rest = joint - (stats[stat] ?: 0.0) - own.sumOf { it.delta }
            if (abs(rest) >= MOVED) own[own.lastIndex] = own.last().let { it.copy(delta = it.delta + rest) }
        }
        return shifts
    }

    private const val MOVED = 0.05

    /**
     * Строки карты, что меняют героя (4.9.3), для полки «на весь бой»: каждая строка карты (не дар Атласа) со сдвигами листа
     * героя, что она дала по [heroShifts]; [index] метит вред. Порядок - карты. 4.10.x: доли древа гильдии и зелья ([origins]) -
     * свои плитки со своим источником, а не строка карты.
     */
    fun heroMods(index: ContentIndex, stats: Map<String, Double>, effects: Map<String, Double>, origins: EffectOrigins = EffectOrigins.NONE): List<HeroMapMod> {
        val moved = LinkedHashMap<StatSource, MutableList<Pair<String, Double>>>()
        heroShifts(stats, effects, origins).forEach { (stat, shifts) ->
            shifts.filter { it.source.kind in SHELVED }.forEach { moved.getOrPut(it.source) { mutableListOf() } += stat to it.delta }
        }
        return moved.map { (source, shifts) -> HeroMapMod(source, shifts, source.kind == SourceKind.MAP && MapLineKind.of(index, source.ref) == MapLineKind.HARM) }
    }

    /** Источники сдвигов, что встают плиткой на полку героя: строки карты, древо гильдии и зелье (дары Атласа - нет). */
    private val SHELVED = setOf(SourceKind.MAP, SourceKind.GUILD, SourceKind.POTION)

    /** Two maps of effects summed per stat: a Vaal zone's lines over the map's. */
    fun sum(a: Map<String, Double>, b: Map<String, Double>): Map<String, Double> = (a.keys + b.keys).associateWith { (a[it] ?: 0.0) + (b[it] ?: 0.0) }
}

/**
 * Строка карты на герое (4.9.3): её источник [source] (4.10.x: строка карты, древо гильдии или зелье - код в `ref`), что она сдвинула
 * в листе героя ([shifts]: стат - сдвиг) и вред ли она ([harm]).
 */
data class HeroMapMod(val source: StatSource, val shifts: List<Pair<String, Double>>, val harm: Boolean)
