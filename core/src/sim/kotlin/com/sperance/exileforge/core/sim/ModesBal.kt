package com.sperance.exileforge.core.sim

import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.TrialArena
import com.sperance.exileforge.core.campaign.TrialPhase
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.run.RunCommand
import com.sperance.exileforge.core.character.Sheets
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Region
import com.sperance.exileforge.rules.content.TrialKind
import com.sperance.exileforge.rules.content.TrialRun
import com.sperance.exileforge.rules.run.RunContext

/**
 * Режимы MODES-BAL (0.14.0, отчёт): испытания - настоящей ареной клиента ([TrialArena]) без экрана: босс-раш региона на высшей
 * скорости боя с часами стены, как их мерит сервер (бой, итог боя и [TAP] секунд игрока на «В бой» между боссами), и башня с
 * первого этажа до гибели героя (строка угрозы десятка - первая, самая слабая).
 */
class ModesBal(private val index: ContentIndex) {
    private val trials = checkNotNull(index.campaign.trials)

    /** Итог раша: боссов пало из [bosses], полная зачистка, секунды стены и уложился ли в лимит бонуса. */
    data class Rush(val killed: Int, val bosses: Int, val wall: Double, val fast: Boolean)

    /** Итог башни: последний пройденный этаж и уровень его врагов, секунды боёв. */
    data class Tower(val floor: Int, val level: Int, val seconds: Double)

    fun rush(hero: Hero, region: Region, tier: Int, seed: Long): Rush {
        var now = 0L
        val arena = arena(hero, TrialRun("rush-$seed", TrialKind.RUSH, seed, hero.level, 0L, region = region.code, tier = tier)) { now }
        repeat(index.rules.fight.speeds.size - 1) { arena.send(RunCommand.Speed) }
        var seen = -1
        var guard = 0
        while (arena.hud.value.phase == TrialPhase.FIGHT && guard++ < LIMIT) {
            val hud = arena.hud.value
            if (hud.cleared != seen) {
                seen = hud.cleared
                now += (TAP * 1000).toLong()
            }
            arena.send(RunCommand.Begin)
            arena.update(DT)
            now += (DT * 1000).toLong()
        }
        val hud = arena.hud.value
        val wall = now / 1000.0
        val full = hud.phase == TrialPhase.DONE && hud.cleared >= hud.steps
        return Rush(hud.cleared, hud.steps, wall, full && wall <= trials.rush.seconds * hud.steps)
    }

    fun tower(hero: Hero, seed: Long, maxFloor: Int): Tower {
        var now = 0L
        val arena = arena(hero, TrialRun("tower-$seed", TrialKind.TOWER, seed, hero.level, 0L)) { now }
        repeat(index.rules.fight.speeds.size - 1) { arena.send(RunCommand.Speed) }
        var guard = 0
        var level = 0
        while (arena.hud.value.phase == TrialPhase.FIGHT && arena.hud.value.cleared < maxFloor && guard++ < LIMIT * 20) {
            val hud = arena.hud.value
            if (hud.choice != null) arena.send(RunCommand.PickLine(0))
            arena.send(RunCommand.Begin)
            if (hud.fight != null) level = hud.level
            arena.update(DT)
            now += (DT * 1000).toLong()
        }
        val hud = arena.hud.value
        return Tower(hud.cleared, level, hud.elapsed)
    }

    private fun arena(hero: Hero, trial: TrialRun, clock: () -> Long): TrialArena {
        val b = hero.build
        val sheet = Sheets.calculate(index, hero.level, hero.heroClass.code, b.tree, b.items)
        val loadout = Loadout.of(b.skills, index, hero.heroClass.code, b.flasks, sheet.stats, index.powers, index.rules.charges)
        val gear = HeroGear(sheet.stats, hero.level, sheet.model, HeroStance.of(hero.heroClass.code), loadout, index.stats.percent)
        return TrialArena(index, trial, RunContext(hero.heroClass.code, hero.level), gear, emptyList(), onEvent = {}, clock = clock)
    }

    private companion object {
        /** Кадр часов, реальные секунды. */
        const val DT = 0.05

        /** Секунды игрока между боями: касание «В бой». */
        const val TAP = 2.0

        /** Потолок кадров на испытание. */
        const val LIMIT = 200_000
    }
}
