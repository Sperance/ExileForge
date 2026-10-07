package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.core.atlas.AtlasEffects
import com.sperance.exileforge.core.campaign.ZoneShare
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

/** What the overlay asks of the run; applied at the start of the next step. */
sealed interface RunCommand {
    data object Continue : RunCommand
    data object Speed : RunCommand
    data object Leave : RunCommand

    /** The fight begins: until then the pack is laid open to be studied. Mid-fight it ends a [Pause]. */
    data object Begin : RunCommand

    /** «В бой» с экрана-вызова стража (3.92.0). */
    data object Accept : RunCommand

    /** Тестировщик (3.92.0): заход начинается сразу боем со стражем, без пути к нему. */
    data object ToBoss : RunCommand
    data object Pause : RunCommand

    /** A window over the map stops the world while it is open: `true` takes a hold, `false` gives one back. */
    data class Hold(val on: Boolean) : RunCommand
    data class Focus(val index: Int) : RunCommand
    data object DismissChest : RunCommand

    /** The level-up screen read (3.81.0): the levels it told of are not told again. */
    data object LevelSeen : RunCommand

    /** Stepping back from the gate undecided: the portal stays, and opens again when walked onto. */
    data object StepBack : RunCommand

    /** The gate is decided: [entered] the zone, or refused it — a refused zone closes for good. */
    data class ShutGate(val entered: Boolean) : RunCommand

    /** Back from the Vaal zone with [life] left, and the mana and flasks it left. */
    data class Returned(val life: Double, val pools: HeroPools? = null, val zone: ZoneShare? = null) : RunCommand

    data class Cast(val slot: Int) : RunCommand
    data class Drink(val slot: Int) : RunCommand

    /** Takes on the guardian of the crystal the hero stands at. */
    data object Release : RunCommand

    /** A Vaal orb on the crystal the hero stands at: spent from the bag, the outcome told by the server's answer. */
    data object VaalCrystal : RunCommand

    /** Steps away from a crystal or a crack undecided. */
    data object StepOff : RunCommand

    /** A fountain tapped on the map (3.70.0): offered as if the hero stood at it. */
    data class OfferFountain(val id: Int) : RunCommand

    /** The fountain offered is drunk: life and mana back by its share, the flasks full. */
    data object TakeFountain : RunCommand

    /** Выбор на листе объекта карты (3.90.0): пара алтаря или вещь торговца по номеру. */
    data class Choose(val choice: Int) : RunCommand

    /** «Собрать» у узла ремесла (3.90.0): сбор идёт его секунды, забег стоит. */
    data object Gather : RunCommand

    /** At a crack of the Abyss: opens it, or goes a depth deeper from the sheet between depths. */
    data object Descend : RunCommand

    /** Takes the hoard of the depths cleared and leaves the Abyss. */
    data object TakeHoard : RunCommand

    /** Stops the autorun: the run goes on by hand from where it stands. */
    data object StopAuto : RunCommand

    /**
     * The server answered the journal up to [applied] (server 1.30.0): what each accepted event brought, by its
     * number, the numbers it [rejected], the experience a fall in the batch [lost], and the crystals Vaal orbs
     * changed, by event number (server 1.30.2).
     */
    data class Settled(
        val applied: Int,
        val rewards: Map<Int, Reward> = emptyMap(),
        val rejected: List<Int> = emptyList(),
        val lost: Double? = null,
        val crystals: Map<Int, Crystal> = emptyMap(),
        /** First wins over a guardian (server 1.76.0): the event and the hero's place among all who won it. */
        val ranks: Map<Int, Long> = emptyMap(),
        /** Добыча начатых боёв (server 1.80.0): по номеру события ENGAGE - что даст убийство каждого члена. */
        val pending: Map<Int, Map<Int, Reward>> = emptyMap(),
    ) : RunCommand

    /** The hero's campaign as the server holds it now: the Vaal zone a portal opened and a crystal a Vaal orb changed are read from it. */
    data class Campaign(val state: CampaignState) : RunCommand
}
