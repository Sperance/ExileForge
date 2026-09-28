package com.sperance.exileforge.core.party

import com.sperance.exileforge.core.campaign.Ally
import com.sperance.exileforge.core.campaign.DraughtRate
import com.sperance.exileforge.core.campaign.Flask
import com.sperance.exileforge.core.campaign.HeroBuild
import com.sperance.exileforge.core.campaign.HeroGear
import com.sperance.exileforge.core.campaign.HeroPools
import com.sperance.exileforge.core.campaign.HeroStance
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.Combatant
import com.sperance.exileforge.rules.content.CombatRules
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.PartyRule
import com.sperance.exileforge.rules.content.PetRole
import com.sperance.exileforge.rules.roll.Menagerie
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.run.RunEvent

/**
 * A run's place in a party (3.25.0), shared by the map's run and the Vaal zone's behind it: alone there is none.
 * [size] is how many heroes the run was entered with — the server scaled its tokens and loot by it.
 */
sealed class RunParty(val size: Int, val rule: PartyRule)

/**
 * The host's side: the guests as the host's fights take them, and the way to tell them what they see.
 * [tell] sends to one guest by hero id, or to all with null; it is called from the run's frame and must not block.
 */
class PartyLead(size: Int, rule: PartyRule, val self: MateView, val tell: (String?, PartyMessage) -> Unit) : RunParty(size, rule) {
    /** The guests, in the order they joined: their seats in a fight follow it. */
    internal val mates = LinkedHashMap<String, Mate>()
    /** The events of the host's journal so far: a guest that missed some asks for them again. */
    internal val told = mutableListOf<RunEvent>()

    /** What the socket brought for the guests, taken in by the run's frame: the mates are the frame's alone. */
    private val inbox = java.util.concurrent.ConcurrentLinkedQueue<Any>()

    /** A guest's card came: their hero is built again for the map's effects when the run next takes it. */
    fun card(card: MateCard) { inbox += card }

    /** A guest left the party: they fight no more, and nothing more is told to them. */
    fun gone(heroId: String) { inbox += Gone(heroId) }

    private class Gone(val heroId: String)

    /** The frame takes in what came: new cards, guests gone. */
    internal fun drain() {
        while (true) when (val next = inbox.poll() ?: return) {
            is MateCard -> mates.getOrPut(next.heroId) { Mate(next.heroId) }.also { it.card = next; it.dirty = true }
            is Gone -> mates.remove(next.heroId)
        }
    }
}

/** A guest's side: the host's pack as it was last set up, and how far the host's journal has been followed. */
class PartyFollow(size: Int, rule: PartyRule, val act: (MateAct) -> Unit) : RunParty(size, rule) {
    /** The last setup and mirror the host sent: written by the socket, read by the run's frame. */
    @Volatile var setup: PartyMessage.Setup? = null
    @Volatile var mirror: PartyMessage.Mirror? = null
    /** The number of the host's next event this guest expects; null until the first mirror or event says where it starts. */
    internal var next: Int? = null
    /** Seconds before a gap in the journal is asked for again. */
    internal var wait = 0.0

    /** The host's fight as this guest sees it, the pack filled back in from the setup. */
    internal fun fight(): com.sperance.exileforge.core.campaign.FightHud? {
        val hud = mirror?.hud ?: return null
        val fight = hud.fight ?: return null
        val pack = setup?.takeIf { it.fight == hud.fightId } ?: return null
        return fight.copy(leader = pack.leader, foes = fight.foes.map { foe -> pack.monsters.getOrNull(foe.index)?.let { foe.copy(monster = it) } ?: foe })
    }
}

/**
 * A guest as the host's fights take them: their card made into a hero at the map's effects, and the pools they
 * carry from fight to fight — kept by the host, since the host fights for them.
 */
class Mate internal constructor(val heroId: String) {
    var card: MateCard? = null
        internal set
    internal var dirty = false
    internal var build: HeroBuild? = null
    internal var ally: Ally? = null
    internal var life = 0.0
    internal var mana = 0.0
    internal var charges: List<Double> = emptyList()
    internal var flaskLeft: List<Double> = emptyList()
    internal var rates: List<DraughtRate> = emptyList()

    val name: String get() = card?.name.orEmpty()
    internal val pools: HeroPools get() = HeroPools(life, mana, charges, flaskLeft, rates)
    internal fun manaCap(): Double = build?.let { it.body.maxMana * (1 - it.gear.kit.reserved(it.body) / 100) } ?: 0.0

    /** The card made into a hero at [effects]: a hero seen for the first time comes in whole, one rebuilt keeps the share of life they had. */
    internal fun rebuild(index: ContentIndex, effects: Map<String, Double>, rules: CombatRules) {
        val card = card ?: return
        val before = build
        val next = HeroBuild(gear(index, card), effects, rules)
        build = next
        ally = card.pet?.let { pet ->
            val pets = Menagerie(index)
            pets.species(pet.species)?.let { kind ->
                Ally(pet.species, Combatant(pets.sheet(pet), pet.level, rules), kind.role == PetRole.TANK,
                    if (kind.role == PetRole.SUPPORT) pets.supportHeal(pet) else 0.0, index.pets.drawFire)
            }
        }
        val flasks = next.gear.kit.flasks
        if (before == null) {
            life = next.body.maxLife
            mana = manaCap()
            charges = flasks.map { it?.maxCharges ?: 0.0 }
        } else {
            life = if (before.body.maxLife > 0) life / before.body.maxLife * next.body.maxLife else next.body.maxLife
            mana = mana.coerceIn(0.0, manaCap())
            charges = flasks.mapIndexed { i, flask -> flask?.let { (charges.getOrNull(i) ?: it.maxCharges).coerceIn(0.0, it.maxCharges) } ?: 0.0 }
        }
        flaskLeft = flasks.indices.map { flaskLeft.getOrNull(it) ?: 0.0 }
        rates = flasks.indices.map { rates.getOrNull(it) ?: DraughtRate() }
        dirty = false
    }

    private fun gear(index: ContentIndex, card: MateCard): HeroGear {
        val conditions = card.skills.flasks
        val flasks = card.flasks.mapIndexed { i, item -> item?.let { index.template(it.template)?.let { template -> Flask.of(item, template, index, conditions.getOrNull(i)) } } }
        val weapon = card.weapon?.let(index::template)?.weaponType
        return HeroGear(card.stats, card.level, null, HeroStance.of(card.heroClass, weapon),
            Loadout.of(card.skills, index.skills, card.heroClass, flasks, index.powers), index.stats.percent)
    }
}

/** A monster as a mirror carries it: what the cards need to be told apart — the pack itself comes whole with the setup. */
internal fun RolledMonster.stub(): RolledMonster = RolledMonster(code, form, rarity, emptyList(), emptyMap(), behaviour, range)
