package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.DraughtRate
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.FoeTotem
import com.sperance.exileforge.core.campaign.combat.HeroPools
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.combat.RiftBoonLines
import com.sperance.exileforge.core.campaign.combat.RiftCombat
import com.sperance.exileforge.core.campaign.combat.pools
import com.sperance.exileforge.core.campaign.combat.surrender
import com.sperance.exileforge.core.campaign.combat.withShades
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.campaign.run.RunHud
import com.sperance.exileforge.core.campaign.run.RunPhase
import com.sperance.exileforge.core.character.StatLine
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.FeatureStat
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.content.Op
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.RiftNodeKind
import com.sperance.exileforge.rules.content.RiftRule
import com.sperance.exileforge.rules.content.RiftRules
import com.sperance.exileforge.rules.content.RiftStyle
import com.sperance.exileforge.rules.content.RiftTouch
import com.sperance.exileforge.rules.content.RiftTrial
import com.sperance.exileforge.rules.content.SkillType
import com.sperance.exileforge.rules.content.TraitLine
import com.sperance.exileforge.rules.content.TrialRules
import com.sperance.exileforge.rules.rift.RiftAct
import com.sperance.exileforge.rules.rift.RiftEngine
import com.sperance.exileforge.rules.rift.RiftFight
import com.sperance.exileforge.rules.rift.RiftFoe
import com.sperance.exileforge.rules.rift.RiftRun
import com.sperance.exileforge.rules.roll.Dice
import com.sperance.exileforge.rules.roll.MonsterEffect
import com.sperance.exileforge.rules.roll.MonsterRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Бой узла Разлома, как его рисует экран (3.96.0): карточки арены, волна из скольких, уровень врагов, секунды боя, вскрытое
 * условие Чемпиона и выполнено ли оно пока; [phase] - идёт, пал, окончен победой.
 */
data class RiftFightHud(
    val phase: TrialPhase,
    val run: RunHud,
    val fight: FightHud?,
    val kind: RiftNodeKind,
    val wave: Int,
    val waves: Int,
    val level: Int,
    val seconds: Double,
    val trial: RiftTrial? = null,
    val met: Boolean = false,
)

/**
 * Бой узла Разлома (3.96.0, сервер 1.83.0): волны узла [fight] одна за другой, каждая - этап боя с переносом; герой - со
 * своим снаряжением, дарами и проклятиями забега [run] и мутаторами недели, здоровье и заряды фляг - из забега. Стражи - со
 * своими механиками ([RiftCombat]). Конец боя - [RiftAct.Fight] в [onEnd]: его шлёт на сервер владелец арены.
 */
class RiftArena(
    val index: ContentIndex,
    private val rules: RiftRules,
    private val engine: RiftEngine,
    private val run: RiftRun,
    private val fight: RiftFight,
    context: RunContext,
    private val gear: HeroGear,
    pet: Pet?,
    private val onEnd: (RiftAct.Fight) -> Unit,
) {
    val combat = index.campaign.combat
    private val node = engine.plan.node(fight.node)!!
    private val zone = TrialRules.arena(index.campaign, fight.level)
    private val arenaRun = Run(index, zone, Streams.mix(engine.plan.seed, run.id.hashCode().toLong(), node.id.toLong()), context)
    private val spawns = Spawns(index, arenaRun)
    private val roller = MonsterRoller(index)
    private val touch: RiftTouch = engine.touch(run)
    private val altar = run.hero.altar

    /** Испытание чемпиона «В одиночку» и «Оскудение» - питомец не встаёт. */
    private val pet: Pet? = pet.takeIf { node.trial != RiftTrial.NO_PET && (touch.rules[RiftRule.BARREN] ?: 0.0) <= 0 }
    private val allies = PetAllies(index, combat)
    private val commands = ConcurrentLinkedQueue<RunCommand>()

    private val effects: Map<String, Double> = MapEffects.sum(AtlasEffects.map(emptyMap(), context.atlas), touch.effects + altarEffects())
    private val build = HeroBuild(gear, effects, combat, heroLines())
    private val hero: Combatant get() = build.body
    private val kit: Loadout get() = gear.kit
    val stance: HeroStance get() = gear.stance
    private var pools = startPools()

    private var phase = TrialPhase.FIGHT
    private val waves: List<List<RolledMonster>> = fight.waves.mapIndexed { w, wave -> wave.mapIndexedNotNull { i, foe -> rolled(foe, arenaRun.streams.of("riftFoe", w * 64 + i)) } }.filter { it.isNotEmpty() }
    private var wave = 0
    private var battle: Battle? = null
    private var monsters: List<RolledMonster> = emptyList()
    private var carry: StageCarry? = null
    private var started = false
    private var paused = false
    private var interlude: Double? = TrialArena.BREAK
    private var speed = 1
    private var elapsed = 0.0
    private var hurt = false
    private var drunk = 0
    private var lowest = 1.0
    private var switches = 0

    private val state = MutableStateFlow(snapshot())
    val hud: StateFlow<RiftFightHud> = state.asStateFlow()

    init {
        stand()
    }

    fun send(command: RunCommand) {
        commands.add(command)
    }

    fun update(dt: Double) {
        while (true) apply(commands.poll() ?: break)
        if (phase == TrialPhase.FIGHT) play(dt)
        state.value = snapshot()
    }

    private fun apply(command: RunCommand) {
        val fight = battle
        when (command) {
            RunCommand.Speed -> speed = if (speed >= 4) 1 else speed * 2

            RunCommand.Begin -> if (fight != null && phase == TrialPhase.FIGHT) {
                started = true
                paused = false
                interlude = null
            }

            RunCommand.Pause -> if (fight != null && started && fight.outcome == null) paused = !paused

            RunCommand.Surrender -> if (fight != null && fight.outcome == null) {
                fight.surrender()
                paused = false
            }

            is RunCommand.Focus -> fight?.focus(command.index)

            is RunCommand.Cast -> fight?.useSkill(command.slot)

            is RunCommand.Drink -> if (started) fight?.useFlask(command.slot)

            else -> Unit
        }
    }

    /** Строки листа героя: дары (ступень учтена правилами), строки проклятий и мутаторов, урон алтарной уникалки за проклятия. */
    private fun heroLines(): List<StatLine> {
        val curses = run.curses.count { it.altar }
        val spite = (altar[FeatureStat.ALTAR_CURSE_DAMAGE.name] ?: 0.0) * curses
        return (engine.boonLines(run) + touch.hero).map(::line) +
            listOfNotNull(spite.takeIf { it > 0 }?.let { StatLine(CoreStat.DAMAGE.code, Op.MORE, it) })
    }

    /**
     * Проклятия алтаря под алтарными уникалками: слабее на «оберег», сильнее на «силу сделки». Строки карты проклятий уже в
     * [touch]; здесь - поправка к их доле от алтаря.
     */
    private fun altarEffects(): Map<String, Double> {
        val shift = ((altar[FeatureStat.ALTAR_PACT_POWER.name] ?: 0.0) - (altar[FeatureStat.ALTAR_CURSE_WARD.name] ?: 0.0)) / 100
        if (shift == 0.0) return emptyMap()
        val altarTouch = RiftTouch.sum(run.curses.filter { it.altar }.mapNotNull { rules.cursesByCode[it.code]?.touch })
        return altarTouch.effects.mapValues { (_, v) -> v * shift }
    }

    private fun line(line: TraitLine) = StatLine(line.stat, line.op, line.value)

    /** Здоровье и фляги из забега; «Сухость» и мутаторы меняют заряды фляг в бою. */
    private fun startPools(): HeroPools {
        val dry = touch.rules[RiftRule.FLASK_CHARGES] ?: 0.0
        val charges = kit.flasks.mapIndexed { i, flask ->
            val max = flask?.maxCharges ?: 0.0
            (run.flasks.getOrNull(i) ?: max).coerceAtMost(max - dry).coerceIn(0.0, max)
        }
        val mana = hero.maxMana * (1 - kit.reserved(hero) / 100)
        return HeroPools(hero.maxLife * run.life.coerceIn(0.0, 1.0), mana, charges, kit.flasks.map { 0.0 }, kit.flasks.map { DraughtRate() })
    }

    /** Враг узла: страж - со своими строками босса, Вождь - редкий со свойствами сверх своих, прочие - по редкости. */
    private fun rolled(foe: RiftFoe, dice: Dice): RolledMonster? {
        val monster = index.monster(foe.monster) ?: return null
        val buffs = MapEffects.buffs(effects) + growth(fight.power) + touch.foes.map { MonsterEffect(it.stat, it.op, it.value) }
        if (foe.rarity == MonsterRarity.UNIQUE) return spawns.skilled(spawns.buffed(roller.boss(roller.guardian(monster.code, fight.level), dice), buffs), dice)
        val rule = index.campaign.rarity(foe.rarity)
        val traits = foe.traits.mapNotNull(index.campaign.traits.byCode::get)
        val mods = roller.draw(roller.pool(zone.tables, fight.level), fight.level, rule, dice.between(rule.modifiers), dice)
        val built = roller.build(monster, fight.level, rule, mods, dice, roller.traitEffects(traits, foe.rarity))
        return spawns.skilled(spawns.buffed(built.copy(traits = (built.traits + traits.map { it.code }).distinct()), buffs), dice)
    }

    /** Рост узла: на столько процентов больше здоровья и урона у каждого врага. */
    private fun growth(power: Double): List<MonsterEffect> = if (power <= 0) emptyList() else (listOf(CoreStat.HEALTH.code) + DamageType.entries.map { it.attack }).map { MonsterEffect(it, Op.MORE, power) }

    /** Механики стража акта и правила забега для боя. */
    private fun riftCombat(): RiftCombat {
        val act = fight.guardian
        val guardians = rules.guardians
        return RiftCombat(
            warden = guardians.warden.takeIf { act == 0 },
            devourer = guardians.devourer.takeIf { act == 1 },
            echo = run.echo,
            boons = run.boons.mapNotNull { held -> rules.boonsByCode[held.code]?.let { RiftBoonLines(held.code, engine.boonLines(run.copy(boons = listOf(held))).map(::line)) } },
            lord = guardians.lord.takeIf { act != null && act >= 2 },
            forbidden = run.forbidden,
            ambush = touch.rules[RiftRule.AMBUSH] ?: 0.0,
            idol = guardians.warden.takeIf { act == 0 }?.let { index.campaign.totems.byCode[it.idols] }?.let { FoeTotem(it) },
        )
    }

    /** Следующая волна встаёт; первая - после передышки, следующие - сразу. */
    private fun stand() {
        monsters = waves.getOrNull(wave) ?: return
        val phases = PhaseFoes(index, combat)
        val stream = wave.toLong()
        val dice = Dice(Streams.mix(arenaRun.seed, RETINUE_STREAM, stream))
        var foes: List<Foe> = phases.withRetinue(monsters.map { phases.foe(it, fight.level) }, dice)
        val devourer = rules.guardians.devourer.takeIf { fight.guardian == 1 }
        if (devourer != null) {
            index.monster(devourer.shade)?.let { shade ->
                val rolledShade = roller.build(shade, fight.level, index.campaign.rarity(MonsterRarity.NORMAL), emptyList(), dice)
                foes = phases.withShades(foes, phases.foe(rolledShade, fight.level), devourer.shades)
            }
        }
        battle = Battle(
            hero, foes, combat, pools.life, Random(Streams.mix(arenaRun.seed, FIGHT_STREAM, stream)), gear.stance,
            kit = kit, model = build, pools = pools, percent = gear.percent, ally = allies.of(hero.stats, pet), stage = carry,
            rift = riftCombat().takeIf { fight.guardian != null || it.ambush > 0 },
        )
        started = wave > 0
        paused = false
        interlude = if (wave == 0) TrialArena.BREAK else null
    }

    private fun play(dt: Double) {
        val fight = battle ?: return
        if (!started) {
            interlude?.let { left ->
                if (left > dt) {
                    interlude = left - dt
                } else {
                    interlude = null
                    started = true
                }
            }
        }
        if (!started || paused) return
        val before = fight.time
        fight.advance(dt * speed)
        elapsed += fight.time - before
        if (fight.heroLife < hero.maxLife - LIFE_EPSILON) hurt = true
        lowest = minOf(lowest, fight.heroLife / hero.maxLife)
        val outcome = fight.outcome ?: return
        if (fight.time < fight.duration + index.campaign.expedition.aftermath) return
        drunk += fight.trial.flasks
        switches += fight.trial.focusChanges
        lowest = minOf(lowest, fight.trial.lowestLife)
        pools = fight.pools()
        if (outcome != Outcome.WIN) return end(won = false)
        carry = fight.carry()
        wave++
        if (wave >= waves.size) end(won = true) else stand()
    }

    /** Условие Чемпиона выполнено по ходу боя. */
    private fun met(): Boolean = when (node.trial) {
        null -> false
        RiftTrial.NO_FLASKS -> drunk == 0
        RiftTrial.FAST -> elapsed <= rules.champion.seconds
        RiftTrial.HALF_LIFE -> lowest >= rules.champion.life / 100
        RiftTrial.NO_PET -> true
        RiftTrial.ONE_FOCUS -> switches == 0
    }

    private fun end(won: Boolean) {
        if (phase != TrialPhase.FIGHT) return
        phase = if (won) TrialPhase.DONE else TrialPhase.DEAD
        battle = null
        val life = if (won) (pools.life / hero.maxLife).coerceIn(0.0, 1.0) else 0.0
        onEnd(RiftAct.Fight(won, life, pools.charges, hurt, won && met()))
    }

    private fun snapshot(): RiftFightHud {
        val fight = battle
        val manaCap = hero.maxMana * (1 - kit.reserved(hero) / 100)
        val runHud = RunHud(
            RunPhase.FIGHT, MapCode.NONE, (fight?.heroLife ?: pools.life).roundToInt(), hero.maxLife.roundToInt(),
            (fight?.heroFighter?.shield ?: hero.maxShield).roundToInt(), hero.maxShield.roundToInt(),
            alive = 0, total = 0, heroMana = (fight?.heroMana ?: pools.mana).roundToInt(), heroMaxMana = manaCap.roundToInt(),
            heroReserved = (hero.maxMana - manaCap).roundToInt(),
        )
        val leader = monsters.maxByOrNull { it.rarity.ordinal }
        val fightHud = if (fight != null && leader != null) {
            fight.hud(monsters, leader, speed, started, paused, hero.taunt, this.fight.level, stage = wave + 1, stages = waves.size, interlude = interlude)
        } else {
            null
        }
        // Условие Чемпиона вскрывается, когда бой начался
        val trial = node.trial?.takeIf { started || wave > 0 || phase != TrialPhase.FIGHT }
        return RiftFightHud(phase, runHud, fightHud, node.kind, wave + 1, waves.size, this.fight.level, elapsed, trial, trial != null && met())
    }

    private companion object {
        private val FIGHT_STREAM = "rift".hashCode().toLong()
        private val RETINUE_STREAM = "riftRetinue".hashCode().toLong()

        /** Потеря здоровья меньше этого - не потеря (округления регенерации). */
        const val LIFE_EPSILON = 0.5
    }
}

/** Стиль героя для веса даров Разлома (3.96.0): чары или атаки по умениям, ведущий урон - по листу. */
object RiftStyles {
    fun of(gear: HeroGear): List<RiftStyle> {
        val types = gear.kit.actives.mapNotNull { it?.skill?.type }
        val cast = if (SkillType.SPELL in types) RiftStyle.SPELL else RiftStyle.ATTACK
        val stats = gear.stats
        fun sum(vararg codes: CoreStat) = codes.sumOf { stats[it.code] ?: 0.0 }
        val damage = mapOf(
            RiftStyle.PHYSICAL to sum(CoreStat.ATTACK_PHYSICAL),
            RiftStyle.ELEMENTAL to sum(CoreStat.ATTACK_FIRE, CoreStat.ATTACK_COLD, CoreStat.ATTACK_LIGHTNING),
            RiftStyle.CHAOS to sum(CoreStat.ATTACK_CHAOS),
        )
        val lead = damage.maxByOrNull { it.value }?.takeIf { it.value > 0 }?.key
        return listOfNotNull(cast, lead)
    }
}
