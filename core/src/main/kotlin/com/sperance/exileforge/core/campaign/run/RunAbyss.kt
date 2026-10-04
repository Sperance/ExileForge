package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.MapStats
import com.sperance.exileforge.core.campaign.StageCarry
import com.sperance.exileforge.core.campaign.combat.Action
import com.sperance.exileforge.core.campaign.combat.Ailment
import com.sperance.exileforge.core.campaign.combat.Ally
import com.sperance.exileforge.core.campaign.combat.Battle
import com.sperance.exileforge.core.campaign.combat.Buildup
import com.sperance.exileforge.core.campaign.combat.CombatEvent
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.combat.DamageType
import com.sperance.exileforge.core.campaign.combat.DraughtRate
import com.sperance.exileforge.core.campaign.combat.EffectView
import com.sperance.exileforge.core.campaign.combat.FlaskView
import com.sperance.exileforge.core.campaign.combat.Foe
import com.sperance.exileforge.core.campaign.combat.HeroPools
import com.sperance.exileforge.core.campaign.combat.HeroStance
import com.sperance.exileforge.core.campaign.combat.HitKind
import com.sperance.exileforge.core.campaign.combat.Outcome
import com.sperance.exileforge.core.campaign.combat.Side
import com.sperance.exileforge.core.campaign.combat.SkillView
import com.sperance.exileforge.core.campaign.combat.flaskViews
import com.sperance.exileforge.core.campaign.combat.pools
import com.sperance.exileforge.core.campaign.combat.traitsIn
import com.sperance.exileforge.core.model.campaign.CampaignState
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.EssenceBook
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.content.Zone
import com.sperance.exileforge.rules.roll.AbyssRifts
import com.sperance.exileforge.rules.roll.Crystal
import com.sperance.exileforge.rules.roll.LootRoller
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.roll.Streams
import com.sperance.exileforge.rules.roll.VaalZone
import com.sperance.exileforge.rules.run.Reward
import com.sperance.exileforge.rules.run.Run
import com.sperance.exileforge.rules.run.RunEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

// ==================== The Abyss ====================

/** A descent under way: its crack, how many depths it leads down, how many are cleared, and the fights of the wave left. */
internal class Descent(val spot: AbyssSpot, val depth: Int) {
    var cleared = 0
    var level = 0
    var fights: List<List<RolledMonster>> = emptyList()

    /** The claim of the hoard, by its event: what it holds is the server's answer. */
    var claim: Int? = null
    var fallen = false

    /** The last fight of the descent won (3.32.0): the next wave or depth is its next stage. */
    var carry: StageCarry? = null
}

/** «Спуститься»: the crack is opened — its event recorded — and the first wave rises; between depths, the next. */
internal fun ExpeditionRun.descend() {
    val spot = rift ?: return
    val rule = abyssRule ?: return
    val current = descent
    if (current == null) {
        val place = world.standingCracks.indexOf(spot)
        record { RunEvent.AbyssOpen(it, place) } ?: return
        spot.opened = true
        val depth = AbyssRifts(index).depth(rule, spot.depth, mapEffects[MapStats.ABYSS_DEPTH] ?: 0.0)
        Descent(spot, depth).also {
            descent = it
            wave(it, 1)
        }
    } else if (current.claim == null && current.cleared < current.depth) {
        wave(current, current.cleared + 1)
    }
}

internal fun ExpeditionRun.wave(current: Descent, depth: Int) {
    val rule = abyssRule ?: return
    current.level = zone.level + (rule.waves.getOrNull(depth - 1)?.level ?: 0)
    current.fights = waves.wave(rule, depth, current.spot.id, zone, mapEffects, run.context.extraRareMods)
    nextFight(current)
}

internal fun ExpeditionRun.nextFight(current: Descent) {
    val group = current.fights.firstOrNull() ?: return
    current.fights = current.fights.drop(1)
    engage(MonsterAgent(ExpeditionRun.ABYSS_AGENT - current.spot.id, group, current.spot.cell.x + 0.5, current.spot.cell.y + 0.5), current.level, abyssal = true, carry = current.carry)
}

/** The descent is over: the hoard of the depths cleared — whole, or what a fall leaves of it. */
internal fun ExpeditionRun.take(current: Descent, fallen: Boolean) {
    current.fallen = fallen
    // A fall in the Abyss burns the whole hoard (server 1.2.0); the hoard is still counted, as the server counts it
    current.claim = rewarding(record { RunEvent.AbyssClaim(it, current.cleared, fallen) })?.n
}
