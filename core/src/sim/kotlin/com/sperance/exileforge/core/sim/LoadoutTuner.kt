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
 * Набор активных умений эталона по урону настоящего боя клиента: поиск начинается с набора героя по правилу и улучшает его
 * по одному ходу - ход меняет одно умение набора (или занимает свободный слот) на умение или пару «накопитель зарядов + трата»,
 * пока ход растит урон больше чем на [GAIN]. Каждый ход сначала мерится коротким боем ([SHORT] с), лучшие [FINALISTS] - полным
 * ([SECONDS] с); урон набора считается один раз на набор (порядок умений - порядок изученного). Урон - среднее геометрическое урона
 * в секунду по редкому манекену и по стае из трёх обычных ([Damage]); поддержка (кличи, проклятия, метки) входит в набор, если её
 * благо в бою перевешивает ещё одно бьющее умение.
 */
class LoadoutTuner(private val index: ContentIndex) {
    private val skillRules = SkillRules(index.skills)
    private val targetLife = targetLifePowers(index)

    /** Урон набора в бою: по одной цели и по стае из трёх, в секунду. */
    data class Damage(val single: Double, val pack: Double, val bySkill: Map<String, Double> = emptyMap(), val byType: Map<String, Double> = emptyMap()) {
        val score: Double get() = sqrt(single.coerceAtLeast(1.0) * pack.coerceAtLeast(1.0))
    }

    /**
     * Лучший набор активных умений для [hero]: изученное не меняется, пассивные - как были; поиск - от его набора по правилу.
     *
     * Умение, что урона не прибавляет, слот не занимает: после поиска набор теряет каждое умение, без которого урон не меньше.
     */
    fun tune(hero: Hero): Hero {
        val slots = skillRules.activeSlots(hero.level)
        val pool = hero.build.skills.learned.keys.mapNotNull { index.skills.byCode[it] }.filter { it.type.kind == SkillKind.ACTIVE }
        if (pool.size <= 1 || slots <= 0) return hero
        val rank = pool.withIndex().associate { it.value.code to it.index }
        val full = HashMap<List<String>, Double>()
        val short = HashMap<List<String>, Double>()
        fun canonical(set: Collection<SkillDefinition>) = set.distinct().sortedBy { rank.getValue(it.code) }
        fun key(set: List<SkillDefinition>) = set.map { it.code }
        fun score(set: List<SkillDefinition>, seconds: Double, memo: HashMap<List<String>, Double>) = memo.getOrPut(key(set)) { damage(with(hero, set), seconds).score }
        var chosen = canonical(hero.build.skills.active.filterNotNull().mapNotNull { slot -> pool.firstOrNull { it.code == slot.skill } }.take(slots))
        var best = if (chosen.isEmpty()) 0.0 else score(chosen, SECONDS, full)
        while (true) {
            val left = pool.filter { it !in chosen }
            val options = left.map { listOf(it) } + pairs(left)
            val moves = (listOf<SkillDefinition?>(null) + chosen).flatMap { out -> options.map { option -> canonical(chosen.filter { it != out } + option) } }
                .filter { it.size <= slots }.distinctBy(::key)
            val finalists = moves.sortedWith(compareByDescending<List<SkillDefinition>> { score(it, SHORT, short) }.thenBy { key(it).joinToString() }).take(FINALISTS)
            val (pick, value) = finalists.map { it to score(it, SECONDS, full) }.maxWithOrNull(compareBy<Pair<List<SkillDefinition>, Double>> { it.second }.thenByDescending { key(it.first).joinToString() }) ?: break
            if (chosen.isNotEmpty() && value <= best * (1 + GAIN)) break
            chosen = pick
            best = value
        }
        chosen.toList().forEach { skill ->
            val without = chosen - skill
            val value = if (without.isEmpty()) 0.0 else score(without, SECONDS, full)
            if (value >= best) {
                chosen = without
                best = value
            }
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
     * Урон [hero] манекенами зоны его уровня за [seconds] с боя (по умолчанию [SECONDS]): редкий (×1000 здоровья) и три обычных (×100).
     *
     * Урон «от здоровья цели» растёт со здоровьем манекена: два манекена (×[LOW] и ×[HIGH]), урон набора - плоская часть решения системы из
     * двух. Тики недугов - своей долей по недугу, чтобы отчёт видел урон яда отдельно.
     */
    fun damage(hero: Hero, seconds: Double = SECONDS): Damage {
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
            while (battle.time < seconds && battle.outcome == null) battle.advance(0.25)
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

        /** Короткий бой отбора ходов, с. */
        const val SHORT = SECONDS / 5

        /** Ходов, что после короткого боя мерятся полным. */
        const val FINALISTS = 3
        const val LOW = 100.0
        const val HIGH = 1000.0

        /** Меньше этой доли прироста - умение набору не нужно. */
        const val GAIN = 0.01
    }
}
