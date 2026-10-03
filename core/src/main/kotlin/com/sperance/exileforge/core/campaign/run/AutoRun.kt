package com.sperance.exileforge.core.campaign.run

import com.sperance.exileforge.rules.roll.Streams

/** What an autorun takes on besides the monsters and the guardian, chosen before it starts (3.2.0). */
data class AutoPlan(val chests: Boolean = true, val crystals: Boolean = true, val abyss: Boolean = true)

/** Where an autorun stands, for the overlay: the wave under way of how many. */
data class AutoHud(val wave: Int, val waves: Int)

/** One step of an autorun, taken when the run stands on its map between fights. */
sealed interface AutoStep {
    /** A new wave begins: only the count moves. */
    data class Wave(val number: Int) : AutoStep
    data class Fight(val agent: MonsterAgent) : AutoStep
    data class OpenChest(val chest: Chest) : AutoStep
    data class Guardian(val spot: CrystalSpot) : AutoStep

    /** A crack of the Abyss: the run stops, and the player decides depth by depth. */
    data class Rift(val spot: AbyssSpot) : AutoStep

    /** The Vaal portal: the run stops at its gate, and the player decides. */
    data object Portal : AutoStep
    data object Boss : AutoStep
    data object Exit : AutoStep
}

/**
 * The autorun of a zone (3.2.0): the zone's own packs — the very tokens its seed stood on the map, so
 * the journal and the loot are the ones a walk would bring — fought as 8 to 15 waves on the arena,
 * one pack after another; between waves what the plan takes: a chest opened, a crystal's guardian
 * released, a crack of the Abyss and the Vaal portal stopped at for the player's word. The guardian
 * last, if it stands, and the way out. Nothing here rolls loot or talks to the server: each step is
 * what the run already does when the hero walks into it.
 */
class AutoPilot(private val steps: ArrayDeque<AutoStep>, val waves: Int) {
    var wave = 0
        private set

    /** Seconds before the next step: a beat between fights, so a wave reads as one. */
    var rest = 0.0

    fun next(): AutoStep? = steps.removeFirstOrNull()?.also { if (it is AutoStep.Wave) wave = it.number }

    companion object {
        const val MIN_WAVES = 8
        const val MAX_WAVES = 15

        /** The beat between two steps, in seconds of the run's pace. */
        const val BEAT = .6

        /** The steps of an autorun over [world], its number of waves drawn from the run's [seed]. */
        fun of(world: ExpeditionWorld, plan: AutoPlan, seed: Long, bossStands: Boolean): AutoPilot {
            val packs = world.agents.filter { it !== world.boss && it.alive }.sortedBy { it.id }
            val waves = Streams(seed).of("autoWaves").between(MIN_WAVES, MAX_WAVES).coerceIn(1, packs.size.coerceAtLeast(1))
            val chests = if (plan.chests) ArrayDeque(world.chests.filterNot { it.opened }) else ArrayDeque()
            val crystals = if (plan.crystals) ArrayDeque(world.standingCrystals) else ArrayDeque()
            val cracks = if (plan.abyss) ArrayDeque(world.standingCracks) else ArrayDeque()
            val steps = ArrayDeque<AutoStep>()
            val size = packs.size / waves
            val extra = packs.size % waves
            var from = 0
            (1..waves).forEach { number ->
                val take = size + if (number <= extra) 1 else 0
                steps += AutoStep.Wave(number)
                packs.subList(from, (from + take).coerceAtMost(packs.size)).forEach { steps += AutoStep.Fight(it) }
                from += take
                // What lies on the map comes between the waves, spread over the run rather than heaped at its end
                chests.removeFirstOrNull()?.let { steps += AutoStep.OpenChest(it) }
                if (number % 2 == 0) crystals.removeFirstOrNull()?.let { steps += AutoStep.Guardian(it) }
                if (number % 3 == 0) cracks.removeFirstOrNull()?.let { steps += AutoStep.Rift(it) }
                if (number == (waves + 1) / 2 && world.portal != null) steps += AutoStep.Portal
            }
            chests.forEach { steps += AutoStep.OpenChest(it) }
            crystals.forEach { steps += AutoStep.Guardian(it) }
            cracks.forEach { steps += AutoStep.Rift(it) }
            if (bossStands) steps += AutoStep.Boss
            steps += AutoStep.Exit
            return AutoPilot(steps, waves)
        }
    }
}
