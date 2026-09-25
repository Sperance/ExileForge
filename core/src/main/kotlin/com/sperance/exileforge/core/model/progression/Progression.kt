package com.sperance.exileforge.core.model.progression

import com.sperance.exileforge.core.i18n.LocaleKey
import com.sperance.exileforge.core.i18n.locOr
import com.sperance.exileforge.core.model.modifier.Modifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** One stat of a reference table: a class's base, or its growth per level. */
@Serializable data class StatValue(val stat: String, val value: Double)

/**
 * A character class (collection `CharacterClass`).
 *
 * It is the base every percentage is counted from and the way into the skill tree. A character
 * references it rather than storing a copy, so a rebalance reaches everyone: what a player chose
 * is snapshotted, what the world decides is not.
 *
 * [params] are the class's permanent modifiers — above all the attribute conversions. They are
 * fixed by the reference table, so they carry no tier.
 */
@Serializable data class CharacterClass(
    @SerialName("_id") val id: String = "",
    val code: String = "",
    val startNodeCode: String = "",
    val baseStats: List<StatValue> = emptyList(),
    val perLevelStats: List<StatValue> = emptyList(),
    val params: List<Modifier> = emptyList(),
) {
    val title: String get() = locOr(LocaleKey.className(code), code)
    val details: String get() = locOr(LocaleKey.classDescription(code), "")
}

/**
 * One step of the progression table (collection `ExperienceLevel`).
 *
 * The same table decides when a character levels up and how many skill points that level hands
 * over. Both are the server's: the client reads the table to show what is coming, never to work
 * out a level itself.
 */
@Serializable data class ExperienceLevel(
    @SerialName("_id") val id: String = "",
    val level: Int = 1,
    val experience: Double = 0.0,
)
