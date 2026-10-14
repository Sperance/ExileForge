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
    /** Упрощённые эффекты боя (3.93.0): без тряски и вспышек - для чувствительных глаз и слабого телефона. */
    val simpleEffects: Boolean = false,
    val buzzDanger: Boolean = true,
    val buzzButtons: Boolean = false,
    /** Карточка сюжета над картой «Похода» (4.4.x): свёрнута ли и при каком шаге это решено. */
    val storyFold: StoryFold = StoryFold(),
    /** Где стояла камера карты «Похода» у каждого героя (4.6.5): по id героя; вход на вкладку возвращает её туда же. */
    val worldViews: Map<String, WorldView> = emptyMap(),
    /** Пролог (4.7.0) уже показан на этом устройстве: дальше перед созданием героя его можно пропустить сразу. */
    val prologueSeen: Boolean = false,
    /** Продолжения шагов сюжета, что уже показаны (4.7.0): `герой:шаг`; повтор - из «Всего сюжета». */
    val storySeen: Set<String> = emptySet(),
    /** Таблица титулов «Хроники» свёрнута заголовком (4.8.0, макет «Титулы», вариант A). */
    val titlesFolded: Boolean = false,
    /** Свойства уникальных и мифических лотов аукциона раскрыты сразу (4.8.0, макет «Аукцион», A+C); на сервер не уходит. */
    val legendsOpen: Boolean = false,
) {
    companion object {
        val PAUSES = listOf(0, 30, 50)
    }
}

/** Камера карты «Похода» (4.6.5): точка мира в середине экрана ([x], [y]) и приближение [scale]. */
@Serializable
data class WorldView(val x: Int, val y: Int, val scale: Float)

/**
 * Свёрнутость карточки сюжета (4.4.x) и шаг, при котором игрок её видел: [quest] - id задания главы, [ready] - ждала ли награда.
 * Новый шаг сюжета или награда, что стала ждать, раскрывают свёрнутую карточку сами.
 */
@Serializable
data class StoryFold(val collapsed: Boolean = false, val quest: String = "", val ready: Boolean = false) {
    /** Карточка увидела шаг [quest] с наградой [ready]: новый шаг или награда, что стала ждать, - раскрыть. */
    fun seen(quest: String, ready: Boolean): StoryFold = when {
        quest != this.quest || (ready && !this.ready) -> StoryFold(collapsed = false, quest = quest, ready = ready)
        else -> copy(ready = ready)
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

/** Скорости боя из правил (`fight.speeds`, 4.7.0); контента ещё нет - умолчания правил. */
fun fightSpeeds(index: com.sperance.exileforge.rules.content.ContentIndex?): List<Int> = index?.rules?.fight?.speeds.orEmpty()

/** Сколько нажатий кнопки скорости боя дают скорость [speed] из настроек: 1 → 2 → 4. */
fun speedPresses(index: com.sperance.exileforge.rules.content.ContentIndex?, speed: Int): Int = fightSpeeds(index).indexOf(speed).coerceAtLeast(0)
