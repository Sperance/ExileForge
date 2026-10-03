package com.sperance.exileforge.presentation.state

import kotlinx.serialization.Serializable

/**
 * The player's settings (3.77.0, «Настройки», variant A of three mockups), kept on the device as one document: how the
 * screen, the fight, the text and the motion behave and what the phone buzzes for. Every field has the default a first
 * start gets, so a document from an older version reads whole.
 */
@Serializable
data class GameSettings(
    val keepScreen: KeepScreen = KeepScreen.EXPEDITION,
    /** The speed every run and trial begins at: 1, 2 or 4, the steps of the fight's own button. */
    val fightSpeed: Int = 1,
    val damageNumbers: DamageNumbers = DamageNumbers.ALL,
    /** The hero's life share, in per cent, under which a running fight pauses by itself; 0 never. */
    val autoPause: Int = 0,
    val textSize: TextSize = TextSize.M,
    /** Pulses, glows and marching roads; off for a weak phone or a low battery. */
    val animations: Boolean = true,
    val buzzDanger: Boolean = true,
    val buzzButtons: Boolean = false,
) {
    companion object {
        val SPEEDS = listOf(1, 2, 4)
        val PAUSES = listOf(0, 30, 50)
    }
}

/** When the screen stays lit: only while a run or a trial is on screen, always, or as the phone decides. */
enum class KeepScreen { EXPEDITION, ALWAYS, NEVER }

/** Which numbers rise off the fighters' cards. */
enum class DamageNumbers { ALL, CRITS, OFF }

/** The game's text against the phone's own size. */
enum class TextSize(val scale: Float) { S(.9f), M(1f), L(1.15f) }

/** Вибрации - из :core (3.80.14). */
typealias Buzz = com.sperance.exileforge.core.session.Buzz
