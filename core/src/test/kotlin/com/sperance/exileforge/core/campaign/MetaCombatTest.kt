package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.TestContent
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.character.SheetModel
import com.sperance.exileforge.core.sim.MobKit
import com.sperance.exileforge.rules.content.ActiveSlot
import com.sperance.exileforge.rules.content.ChargeKind
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.content.StatGroup
import com.sperance.exileforge.rules.content.StatSection
import com.sperance.exileforge.rules.content.StatUnit
import com.sperance.exileforge.rules.content.TakenNode
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.GuardianFloor
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import com.sperance.exileforge.rules.sheet.SheetCalculator
import com.sperance.exileforge.rules.sheet.StatOperation
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Метаморфные сторожа боя (0.14.0, META-TESTS): каждая характеристика героя - пара «стат × операция», как её кладёт хоть один
 * носитель контента (вещь, модификатор, дерево, умение, сила), - и каждая боевая сила уникалки меняют настоящий бой клиента:
 * бой «с» и «без» неё в двух эталонных схватках (атакующий против стаи и заклинатель против сильной стаи) расходится хоть одним
 * событием. Не меняет - характеристика небоевая по разделу `OUTSIDE`, сила небоевая по форме (добыча, объекты карты, слоты, Осквернение),
 * или она в списке [INERT] с причиной. Новая мёртвая характеристика ломает сборку.
 */
class MetaCombatTest {
    private val index: ContentIndex = TestContent.index
    private val rules = index.campaign.combat

    @Test
    fun everyHeroStatMovesTheFight() {
        val pairs = contentLines().filterKeys { (stat, _) -> index.stats[stat]?.let { it.group == StatGroup.HERO && it.section != StatSection.OUTSIDE } == true }
        assertTrue(pairs.size >= 150, "the check proves nothing: ${pairs.size} hero lines")
        val verdicts = pairs.entries.parallelStream().map { (pair, found) ->
            val (stat, op) = pair
            val value = found.value.takeIf { it > 0 } ?: probe(stat)
            Triple("$stat:$op", moves(listOf(StatOperation(stat, op, if (op == Op.MORE && index.stats[stat]?.unit == StatUnit.FLAG) 1.0 else value))), found.carriers)
        }.toList()
        check(verdicts, "характеристики")
    }

    @Test
    fun everyCombatPowerMovesTheFight() {
        val values = powerValues()
        val combat = index.powers.powers.filter { (it.on != null || it.sheet.isNotEmpty()) && it.world == null && it.slots == null && it.features.isEmpty() }
        assertTrue(combat.size >= 400, "the check proves nothing: ${combat.size} combat powers")
        val verdicts = combat.parallelStream().map { power ->
            Triple(power.stat, moves(listOf(StatOperation(power.stat, Op.ADD, values[power.stat] ?: 100.0))), setOf("powers"))
        }.toList()
        check(verdicts, "силы")
    }

    private fun check(verdicts: List<Triple<String, Boolean, Set<String>>>, what: String) {
        val dead = verdicts.filter { !it.second && it.first !in INERT }.map { "${it.first} (${it.third.joinToString(",")})" }
        assertTrue(dead.isEmpty(), "$what не меняют бой (${dead.size}): $dead")
    }

    /** Меняет ли [extra] хоть одну из эталонных схваток. */
    private fun moves(extra: List<StatOperation>): Boolean = SCENARIOS.any { scenario -> fight(scenario, emptyList()) != fight(scenario, extra) }

    private data class Scenario(val heroClass: String, val style: SkillType, val foesPower: Double, val cleared: Boolean, val seed: Int)

    private val prints = java.util.concurrent.ConcurrentHashMap<Scenario, Int>()

    /** Отпечаток схватки: события боя и итог пулов. */
    private fun fight(scenario: Scenario, extra: List<StatOperation>): Int {
        if (extra.isEmpty()) prints[scenario]?.let { return it }
        val heroClass = index.heroClass(scenario.heroClass)!!
        val calc = SheetCalculator(index)
        val res = calc.hero(LEVEL, heroClass, listOf(TakenNode(heroClass.startNode)), emptyList())
        val model = SheetModel(res.base, res.operations + BASE.map { (stat, value) -> StatOperation(stat, Op.ADD, value) } + extra, index)
        val stats = model.plain
        val skills = skills(scenario.heroClass, scenario.style)
        val kit = Loadout.of(skills, index, scenario.heroClass, kitFlasks, stats, index.powers, index.rules.charges)
        val gear = HeroGear(stats, LEVEL, model, HeroStance.of(scenario.heroClass), kit, index.stats.percent)
        val build = HeroBuild(gear, emptyMap(), rules)
        val battle = Battle(
            build.body, foes(scenario), rules, index.rules.fight, build.body.maxLife, Random(scenario.seed), gear.stance,
            kit = kit, model = build, percent = gear.percent, cleared = scenario.cleared,
        )
        while (battle.outcome == null && battle.time < SECONDS) battle.advance(0.1)
        val print = battle.events.joinToString(";") { "${it.actor}${it.action}${it.kind}${it.skill}${it.foe}${"%.3f".format(it.damage)}${"%.3f".format(it.healed)}" }
            .plus("|${"%.3f".format(battle.heroLife)}|${"%.3f".format(battle.heroMana)}|${battle.outcome}").hashCode()
        if (extra.isEmpty()) prints[scenario] = print
        return print
    }

    private val kitFlasks by lazy { MobKit(index).flasks(LEVEL, Rarity.RARE) }

    /** Умения схватки: удар стиля, трата зарядов и её накопитель, проклятие, клич и метка; пассивные - два бонуса класса и аура. */
    private fun skills(heroClass: String, style: SkillType): HeroSkills {
        val all = index.skills.ofClass(heroClass).filter { it.unlock <= LEVEL }
        val hit = all.filter { it.type == style && it.hit != null && !it.common && it.charges == null }.maxBy { it.unlock }
        val spender = all.firstOrNull { it.charges?.consume == true }
        val spent = spender?.charges?.kind
        val gainer = spent?.let { kind -> all.firstOrNull { g -> g.charges?.gain != null && (kind == ChargeKind.ALL || g.charges?.kind == kind) } }
        val support = listOf(SkillType.CURSE, SkillType.WARCRY).mapNotNull { type -> all.firstOrNull { it.type == type && it.mark == null } } + listOfNotNull(all.firstOrNull { it.mark != null })
        val actives = (listOfNotNull(hit, spender, gainer) + support).distinct()
        val passives = all.filter { it.type.kind == SkillKind.PASSIVE && it.type != SkillType.AURA }.take(2) + listOfNotNull(all.firstOrNull { it.type == SkillType.AURA })
        return HeroSkills(
            learned = (actives + passives).associate { it.code to SKILL_LEVEL },
            active = actives.map { ActiveSlot(it.code, it.condition) },
            passive = passives.map { it.code },
        )
    }

    /** Стая зоны уровня [LEVEL]: три обычных и редкий настоящими бросками, с умениями; удары ×[Scenario.foesPower]. */
    private fun foes(scenario: Scenario): List<Foe> {
        val zone = index.campaign.zones.filter { it.monsters.isNotEmpty() }.minBy { abs(it.level - FOE_LEVEL) }
        val run = Run(index, zone, 77L, RunContext(scenario.heroClass, FOE_LEVEL))
        val spawns = Spawns(index, run)
        val phases = PhaseFoes(index, rules)
        return listOf(MonsterRarity.NORMAL, MonsterRarity.NORMAL, MonsterRarity.NORMAL, MonsterRarity.RARE).mapIndexed { n, rarity ->
            val dice = Dice(Streams.mix(77L, n.toLong(), rarity.ordinal.toLong()))
            val m = spawns.skilled(run.monsters.roll(zone, zone.monsters[n % zone.monsters.size], run.pool, dice, 0, index.campaign.rarity(rarity), run.areaLevel), dice)
            phases.foe(m.copy(stats = m.stats.mapValues { (stat, v) -> if (stat in GuardianFloor.STRIKES) v * scenario.foesPower else v }), run.areaLevel)
        }
    }

    private class Found(var value: Double, val carriers: MutableSet<String>)

    /** Пары «стат × операция» всех носителей контента и наибольшее по модулю число, с каким носитель их кладёт. */
    private fun contentLines(): Map<Pair<String, Op>, Found> {
        val out = HashMap<Pair<String, Op>, Found>()
        CARRIERS.forEach { file ->
            fun walk(e: JsonElement) {
                when (e) {
                    is JsonObject -> {
                        val stat = (e["stat"] as? JsonPrimitive)?.content
                        if (stat != null && index.stats[stat] != null) {
                            val op = (e["op"] as? JsonPrimitive)?.content?.let { runCatching { Op.valueOf(it) }.getOrNull() } ?: Op.ADD
                            val found = out.getOrPut(stat to op) { Found(0.0, HashSet()) }
                            found.carriers += file.removeSuffix(".json")
                            found.value = maxOf(found.value, numbers(e).maxOfOrNull { abs(it) } ?: 0.0)
                        }
                        e.values.forEach(::walk)
                    }

                    is JsonArray -> e.forEach(::walk)

                    else -> Unit
                }
            }
            walk(TestContent.json(file))
        }
        return out
    }

    /** Числа строки: `value`, `values`, `range` (вложенные массивы - тоже). */
    private fun numbers(e: JsonObject): List<Double> = listOf("value", "values", "range").mapNotNull { e[it] }.flatMap { flat(it) }

    private fun flat(e: JsonElement): List<Double> = when (e) {
        is JsonPrimitive -> listOfNotNull(e.doubleOrNull)
        is JsonArray -> e.flatMap { flat(it) }
        else -> emptyList()
    }

    /** Число силы: наибольшее, с каким её кладёт строка вещи. */
    private fun powerValues(): Map<String, Double> {
        val out = HashMap<String, Double>()
        index.templates.values.forEach { t -> t.lines.flatten().forEach { line -> if (line.stat.startsWith("POWER_")) out.merge(line.stat, line.range.maxOf { abs(it) }, ::maxOf) } }
        return out
    }

    /** Число пробы без числа в контенте: флаг - единица, остальное - заметная величина. */
    private fun probe(stat: String): Double = if (index.stats[stat]?.unit == StatUnit.FLAG) 1.0 else 30.0

    private companion object {
        const val LEVEL = 60
        const val SKILL_LEVEL = 12
        const val SECONDS = 40.0

        /** Уровень стаи: ниже героя - схватка длится десятки секунд, и герой успевает и бить, и терять здоровье. */
        const val FOE_LEVEL = 40

        /** Носители строк героя в контенте. */
        val CARRIERS = listOf("modifiers.json", "equipment.json", "items.json", "tree.json", "skills.json", "powers.json", "classes.json", "fates.json", "atlas.json")

        /** Лист эталона поверх класса: урон, защиты, щит, недуги, крит - чтобы условия большинства строк наступали в бою. */
        val BASE = mapOf(
            "STOCK_ATTACK_PHYSICAL" to 120.0, "STOCK_ATTACK_FIRE" to 30.0, "STOCK_ATTACK_COLD" to 30.0, "STOCK_ATTACK_LIGHTNING" to 30.0, "STOCK_ATTACK_CHAOS" to 20.0,
            "STOCK_ATTACK_SPEED" to 1.4, "STOCK_HEALTH" to 3000.0, "STOCK_MANA" to 300.0, "STOCK_ENERGY_SHIELD" to 300.0, "STOCK_ARMOR" to 800.0,
            "STOCK_EVASION" to 600.0, "STOCK_BLOCK_CHANCE" to 15.0, "STOCK_ALL_ELEMENTAL_RESISTANCES" to 30.0, "STOCK_CRITICAL_CHANCE" to 20.0, "STOCK_SPELL_CRITICAL_CHANCE" to 20.0,
            "STOCK_IGNITE_CHANCE" to 20.0, "STOCK_SHOCK_CHANCE" to 20.0, "STOCK_POISON_CHANCE" to 20.0, "STOCK_BLEED_CHANCE" to 20.0, "STOCK_FREEZE_CHANCE" to 10.0,
            "STOCK_HEALTH_REGEN" to 10.0, "STOCK_MANA_REGEN_FLAT" to 8.0, "STOCK_STRENGTH" to 150.0, "STOCK_AGILITY" to 150.0, "STOCK_INTELLECT" to 150.0,
        )

        /**
         * Схватки: атакующий и заклинатель, каждый против обычной стаи (бой перед ней выигран - ложатся силы «после победы») и против
         * стаи, что бьёт сильнее (герой теряет здоровье, пьёт фляги и может пасть); строка живая, если меняет хоть одну.
         */
        val SCENARIOS = listOf(
            Scenario("MARAUDER", SkillType.ATTACK, 1.0, cleared = true, seed = 7),
            Scenario("MARAUDER", SkillType.ATTACK, 1.8, cleared = false, seed = 13),
            Scenario("WITCH", SkillType.SPELL, 0.8, cleared = true, seed = 17),
            Scenario("WITCH", SkillType.SPELL, 1.3, cleared = false, seed = 11),
            // 0.14.1, оборона классов строкой: Мародёр получает на 30% меньше урона - ещё две стаи, где он теряет здоровье (лечение от
            // урона живое) и где горят поджоги
            Scenario("MARAUDER", SkillType.ATTACK, 1.75, cleared = false, seed = 13),
            Scenario("MARAUDER", SkillType.ATTACK, 1.8, cleared = false, seed = 7),
        )

        /** Известные строки и силы, что эталонная схватка не меняет, - с причиной. */
        val INERT: Set<String> = buildSet {
            // Характеристики - нужен источник вне схватки: Предначертание, руны, чужие умения
            addAll(listOf("STOCK_ACTIVE_RUNE_SOCKETS:ADD", "STOCK_BORROW_SKILLS:ADD", "STOCK_FATE_EFFECT:ADD", "STOCK_LAST_RUNE_EFFECT:ADD"))
            addAll(listOf("STOCK_RUNE_EFFECT:ADD"))
            // Характеристики - условие строки не наступает в эталонных схватках (заряды, оглушение, блок чар и пр.)
            addAll(listOf("STOCK_ASCENDED_DAMAGE:ADD", "STOCK_ATTACK_MAGICAL:ADD", "STOCK_ATTACK_MAGICAL:MORE", "STOCK_BUFF_DURATION:ADD"))
            addAll(listOf("STOCK_DAMAGE_PER_FRENZY:ADD", "STOCK_ENERGY_REGEN:INCREASED", "STOCK_LEECH_ALL:INCREASED", "STOCK_LEECH_PHYSICAL:MORE"))
            addAll(listOf("STOCK_LIFE_DAMAGE_DELAY:ADD", "STOCK_LOW_LIFE_SPEED:ADD", "STOCK_MANA_ON_BLOCK:ADD", "STOCK_POISON_DAMAGE:INCREASED"))
            addAll(listOf("STOCK_SKILL_HEALING:INCREASED", "STOCK_SPELL_BLOCK:ADD", "STOCK_STUN_THRESHOLD:INCREASED", "STOCK_STUN_THRESHOLD:MORE"))
            addAll(listOf("STOCK_TAUNT:ADD"))
            // Характеристики - недуги, которых нет у стаи эталона, и защита от них
            addAll(listOf("STOCK_AVOID_FREEZE:ADD", "STOCK_AVOID_PARCH:ADD", "STOCK_AVOID_WOUND:ADD", "STOCK_ELECTROCUTE_BUILDUP:ADD"))
            addAll(listOf("STOCK_FREEZE_CHANCE:ADD", "STOCK_FREEZE_DURATION:ADD", "STOCK_FREEZE_DURATION_ON_SELF:ADD", "STOCK_IMMUNE_CURSE:ADD"))
            addAll(listOf("STOCK_IMMUNE_PARCH:ADD", "STOCK_IMMUNE_WOUND:ADD", "STOCK_PARCH_DURATION:ADD", "STOCK_PARCH_EFFECT:ADD"))
            addAll(listOf("STOCK_PARCH_TAKEN:ADD", "STOCK_RECOVERY_SUPPRESSION:ADD", "STOCK_SPELL_SUPPRESSION:ADD", "STOCK_SUPPRESSION_AVOID:ADD"))
            addAll(listOf("STOCK_WOUND_DURATION:ADD", "STOCK_WOUND_EFFECT:ADD", "STOCK_WOUND_TAKEN:ADD"))
            // Характеристики - небоевые: добыча и объекты карты
            addAll(listOf("STOCK_BLIGHT_REDUCTION:ADD", "STOCK_FATE_ASH_LOOT:ADD", "STOCK_VEILED:ADD", "STOCK_VEILED_LOOT:ADD"))
            // Характеристики - пределы: в схватке значение не доходит до потолка
            addAll(listOf("STOCK_BLOCK_MAX:ADD", "STOCK_EVASION_MAX:ADD", "STOCK_MAX_ENDURANCE_CHARGES:ADD"))
            addAll(listOf("STOCK_MAX_FRENZY_CHARGES:ADD", "STOCK_MAX_POWER_CHARGES:ADD", "STOCK_RESIST_MAX_ALL:ADD", "STOCK_RESIST_MAX_CHAOS:ADD"))
            addAll(listOf("STOCK_RESIST_MAX_COLD:ADD", "STOCK_RESIST_MAX_FIRE:ADD", "STOCK_RESIST_MAX_LIGHTNING:ADD", "STOCK_SPELL_BLOCK_MAX:ADD"))
            // Силы - условие силы не наступает в эталонных схватках (аудит 19_STATS: stats-pw1/pw2, inject.tsv)
            addAll(listOf("POWER_ABSCESS_WARD", "POWER_ALTAR_VAAL_SIGHT", "POWER_ANCESTRAL_BRAND", "POWER_ANCESTRAL_VIGOUR"))
            addAll(listOf("POWER_ASCENDED_FURY", "POWER_AURORA_ECLIPSE", "POWER_BARON_LEGION", "POWER_BILE_TOUCH"))
            addAll(listOf("POWER_BLIND_DEVOTION", "POWER_BLOODBATH", "POWER_BLOODED_TAKINGS", "POWER_CALM_CENTER"))
            addAll(listOf("POWER_CARESS_DEVOTION", "POWER_CAUTERY", "POWER_CHARGED_EDGE", "POWER_CHARGES"))
            addAll(listOf("POWER_DEADWOOD_CROWN", "POWER_DEVOTED_SWIFTNESS", "POWER_DROUGHT_MAW", "POWER_ELDER_MIRROR"))
            addAll(listOf("POWER_ENTHRONED_SHADOW", "POWER_ETERNITY_SEAM", "POWER_FATE_THREAD", "POWER_FEED_ON_THE_FALLEN"))
            addAll(listOf("POWER_FEVER_BLOOM", "POWER_FROZEN_WOUND", "POWER_GALE_SHATTER", "POWER_GILDED_GREED"))
            addAll(listOf("POWER_GLACIER_SHARD", "POWER_GOLDEN_PURSE", "POWER_HIVE_VOLLEY", "POWER_HOLLOW_LIGHTNESS"))
            addAll(listOf("POWER_HOLLOW_VEINS", "POWER_HUSH_STILLNESS", "POWER_IDOLBREAKER", "POWER_IDOLBREAKER_SPLINTERS"))
            addAll(listOf("POWER_IDOL_ASH", "POWER_IDOL_ASH_EMBERS", "POWER_LAMP_HOARD", "POWER_LICHS_FOCUS"))
            addAll(listOf("POWER_LINEAGE_FURY", "POWER_LINEAGE_SIGNET", "POWER_LINGERING_ROT", "POWER_LONE_QUARRY"))
            addAll(listOf("POWER_LOYAL_BANNER", "POWER_MAGNATE_TAX", "POWER_MOTHERS_CORD", "POWER_PLAGUE_TRAIL_HOARD"))
            addAll(listOf("POWER_PROTOTYPE_BULWARK", "POWER_PROTOTYPE_PLATING", "POWER_QUARRY_SOLUTION", "POWER_RIFT_STEP"))
            addAll(listOf("POWER_ROOT_DRINK", "POWER_RUNE_CARVER", "POWER_RUNE_RESONANCE", "POWER_SACRIFICIAL_PACT"))
            addAll(listOf("POWER_SCHOLARS_INSIGHT", "POWER_SHAMANBANE", "POWER_SHAMANBANE_WARD", "POWER_SPECTRAL_LEECH"))
            addAll(listOf("POWER_SUNS_BLESSING", "POWER_SUTURE_THIEF", "POWER_TAILWIND", "POWER_THICKET_GRACE"))
            addAll(listOf("POWER_TRUE_SWING", "POWER_UMBILICUS_DROUGHT", "POWER_UNDERTOW", "POWER_VEIN_BURST"))
            addAll(listOf("POWER_WARDEN_BASTION", "POWER_ZENITH_GLARE"))
            // Силы «чистым» (0.14.2): обычные монстры +25% здоровья - стая эталона держит недуг на герое с первой секунды
            addAll(listOf("POWER_STILL_POSTURE"))
        }
    }
}
