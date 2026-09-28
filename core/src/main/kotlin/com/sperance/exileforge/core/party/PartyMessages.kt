package com.sperance.exileforge.core.party

import com.sperance.exileforge.core.campaign.AgentMode
import com.sperance.exileforge.core.campaign.FightHud
import com.sperance.exileforge.core.campaign.FlaskView
import com.sperance.exileforge.core.campaign.RunPhase
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Pet
import com.sperance.exileforge.rules.party.PartyWire
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.run.RunEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * What the heroes of a party tell each other over the lobby's socket (3.25.0): the server only passes it on.
 * The host walks and fights for everyone and sends each guest the world and their own side of the fight;
 * a guest sends their hero's card and what they do in a fight.
 */
@Serializable
sealed interface PartyMessage {
    /** Host → guest, a few times a second: the world as it stands, and the guest's own side of it. */
    @Serializable @SerialName("mirror") data class Mirror(val world: WorldMirror, val hud: MateHud) : PartyMessage
    /** Host → guest, as a fight opens and now and then through it: the pack, whole, which the mirrors leave out. */
    @Serializable @SerialName("setup") data class Setup(val fight: Int, val leader: RolledMonster, val monsters: List<RolledMonster>) : PartyMessage
    /** Host → guest: an event of the host's journal; the guest writes the same one into their own. */
    @Serializable @SerialName("event") data class Event(val event: RunEvent) : PartyMessage
    /** Host → guest: the host wants the cards again — it came back, or lost them. */
    @Serializable @SerialName("hello") data object Hello : PartyMessage
    /** Guest → host: the hero as a fight takes them. */
    @Serializable @SerialName("card") data class Card(val card: MateCard) : PartyMessage
    /** Guest → host: what the guest does. */
    @Serializable @SerialName("act") data class Act(val act: MateAct) : PartyMessage

    companion object {
        fun encode(message: PartyMessage): JsonElement = PartyWire.json.encodeToJsonElement(serializer(), message)
        fun decode(payload: JsonElement): PartyMessage? = runCatching { PartyWire.json.decodeFromJsonElement(serializer(), payload) }.getOrNull()
    }
}

/** What a guest does: a skill or a flask of theirs, a foe singled out, ready for the fight, or events missed. */
@Serializable
sealed interface MateAct {
    @Serializable @SerialName("cast") data class Cast(val slot: Int) : MateAct
    @Serializable @SerialName("drink") data class Drink(val slot: Int) : MateAct
    @Serializable @SerialName("focus") data class Focus(val index: Int) : MateAct
    @Serializable @SerialName("ready") data object Ready : MateAct
    /** The events of the host's journal from number [from] on, sent again: the guest's journal missed some. */
    @Serializable @SerialName("resend") data class Resend(val from: Int) : MateAct
}

/**
 * A guest's hero as the host's fight takes them: the finished sheet, the skills and their slots, the belt,
 * the weapon's template (for the stance) and the combat pet. Sent on joining and after every change of gear.
 */
@Serializable
data class MateCard(
    val heroId: String,
    val name: String,
    val heroClass: String,
    val level: Int,
    val stats: Map<String, Double>,
    val skills: HeroSkills,
    val flasks: List<ItemInstance?> = emptyList(),
    val weapon: String? = null,
    val pet: Pet? = null,
)

/** One monster on the map as the host sees it. */
@Serializable
data class AgentMirror(val id: Int, val x: Float, val y: Float, val mode: AgentMode, val fallen: List<Int> = emptyList())

/**
 * The host's world as a guest draws it: where the host stands and looks, every monster, what was opened,
 * drunk, freed or spent; the phase of the run and how far the host's journal has gone.
 */
@Serializable
data class WorldMirror(
    val phase: RunPhase,
    val vaal: Boolean,
    val x: Double, val y: Double, val facingX: Double, val facingY: Double, val moving: Boolean,
    /** The monsters still standing; one not here has fallen. Crystals and cracks follow the journal's events, not the mirror. */
    val agents: List<AgentMirror>,
    val chests: List<Int> = emptyList(),
    val fountains: List<Int> = emptyList(),
    val portal: Boolean = false,
    val party: List<MateView> = emptyList(),
    /** The host's journal: the number its first event here bore, and the number the next one will. */
    val journalBase: Int = 0,
    val journal: Int = 0,
)

/** A guest's own side, as the host keeps it: their pools and belt between fights, and the fight as they see it. */
@Serializable
data class MateHud(
    val life: Int, val maxLife: Int, val shield: Int, val maxShield: Int, val mana: Int, val maxMana: Int,
    val flasks: List<FlaskView?> = emptyList(),
    val down: Boolean = false,
    val fight: FightHud? = null,
    val fightId: Int = 0,
)

/** A hero of the party as the bar over the map shows them. */
@Serializable
data class MateView(val heroId: String, val name: String, val heroClass: String, val life: Int, val maxLife: Int, val alive: Boolean = true, val host: Boolean = false)

/** The fight's roll call (3.25.0): how many are ready of how many, the seconds before it begins anyway, whether this hero is. */
@Serializable
data class ReadyView(val ready: Int, val total: Int, val left: Int, val mine: Boolean)
