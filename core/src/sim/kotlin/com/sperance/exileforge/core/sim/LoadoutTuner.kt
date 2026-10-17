package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.campaign.Spawns
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.rules.content.ActiveSlot
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SkillRules
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Набор активных умений эталона: слоты заполняются жадно по урону настоящего боя клиента - на каждом шаге
 * берётся умение (или пара «накопитель зарядов + трата», когда на неё есть два слота), что сильнее всего растит урон набора.
 * Урон - среднее геометрическое урона в секунду по редкому манекену и по стае из трёх обычных ([Damage]); поддержка (кличи,
 * проклятия, метки) входит в набор, если её благо в бою перевешивает ещё одно бьющее умение.
 */
class LoadoutTuner(private val index: ContentIndex) {
    private val skillRules = SkillRules(index.skills)
    private val targetLife = targetLifePowers(index)

    /** Урон набора в бою: по одной цели и по стае из трёх, в секунду. */
    data class Damage(val single: Double, val pack: Double, val bySkill: Map<String, Double> = emptyMap(), val byType: Map<String, Double> = emptyMap()) {
        val score: Double get() = sqrt(single.coerceAtLeast(1.0) * pack.coerceAtLeast(1.0))
    }

    /**
     * Лучший набор активных умений для [hero]: изученное не меняется, пассивные - как были.
     *
     * Умение, что урона не прибавляет, слот не занимает.
     */
    fun tune(hero: Hero): Hero {
        val skills = hero.build.skills
        val slots = skillRules.activeSlots(hero.level)
        val pool = skills.learned.keys.mapNotNull { index.skills.byCode[it] }.filter { it.type.kind == SkillKind.ACTIVE }
        if (pool.size <= 1 || slots <= 0) return hero
        val chosen = mutableListOf<SkillDefinition>()
        var best = 0.0
        while (chosen.size < slots) {
            val left = pool.filter { it !in chosen }
            val options = left.map { listOf(it) } + pairs(left).takeIf { slots - chosen.size >= 2 }.orEmpty()
            val scored = options.map { option -> option to damage(with(hero, chosen + option)).score }
            val (pick, score) = scored.maxByOrNull { it.second } ?: break
            if (chosen.isNotEmpty() && score <= best * (1 + GAIN)) break
            chosen += pick
            best = score
        }
        return with(hero, chosen)
    }

    /** Пары «накопитель + трата» из [left]: трата вида ALL берёт любой накопитель. */
    private fun pairs(left: List<SkillDefinition>): List<List<SkillDefinition>> {
        val gainers = left.filter { it.charges?.gain != null }
        return left.filter { it.charges?.consume == true }.flatMap { spender ->
            val kind = spender.charges!!.kind
            gainers.filter { kind == ChargeKind.ALL || it.charges!!.kind == kind }.map { listOf(it, spender) }
        }
    }

    private fun with(hero: Hero, actives: List<SkillDefinition>): Hero {
        val skills = hero.build.skills.copy(active = actives.map { ActiveSlot(it.code, it.condition) })
        return hero.copy(build = hero.build.copy(skills = skills))
    }

    /**
     * Урон [hero] манекенами зоны его уровня: редкий (×1000 здоровья, [SECONDS] с) и три обычных (×100).
     *
     * Урон «от здоровья цели» растёт со здоровьем манекена: два манекена (×[LOW] и ×[HIGH]), урон набора - плоская часть решения системы из
     * двух. Тики недугов - своей долей по недугу, чтобы отчёт видел урон яда отдельно.
     */
    fun damage(hero: Hero): Damage {
        val zone = zoneOf(hero.level)
        val run = Run(index, zone, SimKit.SEED + 3, RunContext(hero.heroClass.code, hero.level))
        val arena = Arena(index, hero, emptyMap())
        val spawns = Spawns(index, run)
        fun mob(rarity: MonsterRarity, s: Int) = Dice(Streams.mix(SimKit.SEED, zone.level.toLong() * 977 + s, rarity.ordinal.toLong())).let { dice ->
            spawns.skilled(run.monsters.roll(zone, zone.monsters[s % zone.monsters.size], run.pool, dice, 0, index.campaign.rarity(rarity), run.areaLevel), dice)
        }
        val parts = HashMap<String, Double>()
        val types = HashMap<String, Double>()

        fun dealt(foes: List<com.sperance.exileforge.rules.roll.RolledMonster>, big: Double, count: Boolean): Pair<Double, Double> {
            val battle = arena.battle(foes.map { arena.phases.foe(Prober.dummy(it, big), run.areaLevel) }, 7_300L, false)
            while (battle.time < SECONDS && battle.outcome == null) battle.advance(0.25)
            val hits = battle.events.filter { it.actor == Side.HERO && !it.onSelf && it.skill !in targetLife }
            if (count) hits.forEach { parts.merge(it.skill ?: it.ailment?.let { a -> "~${a.name}" } ?: "-", it.damage, Double::plus) }
            if (count) hits.forEach { types.merge(it.ailment?.name ?: it.type?.name ?: "-", it.damage, Double::plus) }
            val life = foes.sumOf { arena.phases.foe(Prober.dummy(it, big), run.areaLevel).body.let { b -> b.maxLife + b.maxShield } }
            return hits.sumOf { it.damage } / battle.time.coerceAtLeast(0.1) to life
        }
        fun flat(foes: List<com.sperance.exileforge.rules.roll.RolledMonster>): Double {
            val (r1, l1) = dealt(foes, LOW, true)
            val (r2, l2) = dealt(foes, HIGH, false)
            val share = ((r2 - r1) / (l2 - l1)).coerceAtLeast(0.0)
            return (r1 - share * l1).coerceAtLeast(0.0)
        }
        return Damage(flat(listOf(mob(MonsterRarity.RARE, 0))), flat((0 until 3).map { mob(MonsterRarity.NORMAL, it) }), parts, types)
    }

    private fun zoneOf(level: Int): Zone = index.campaign.zones.filter { it.monsters.isNotEmpty() }.minBy { abs(it.level - level) }

    private companion object {
        const val SECONDS = 20.0
        const val LOW = 100.0
        const val HIGH = 1000.0

        /** Меньше этой доли прироста - умение набору не нужно. */
        const val GAIN = 0.01
    }
}
