package com.sperance.exileforge.core.app

import kotlinx.serialization.Serializable

/**
 * The player's settings («Настройки», variant A of three mockups), kept on the device as one document: how the
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
    /** Упрощённые эффекты боя: без тряски и вспышек - для чувствительных глаз и слабого телефона. */
    val simpleEffects: Boolean = false,
    val buzzDanger: Boolean = true,
    val buzzButtons: Boolean = false,
    /** Карточка сюжета над картой «Похода»: свёрнута ли и при каком шаге это решено. */
    val storyFold: StoryFold = StoryFold(),
    /** Где стояла камера карты «Похода» у каждого героя: по id героя; вход на вкладку возвращает её туда же. */
    val worldViews: Map<String, WorldView> = emptyMap(),
    /** Последняя зона, в которую входил каждый герой на этом устройстве: код зоны по id героя; к ней ведёт кнопка «К герою» карты «Похода». */
    val lastZones: Map<String, String> = emptyMap(),
    /** Пролог уже показан на этом устройстве: дальше перед созданием героя его можно пропустить сразу. */
    val prologueSeen: Boolean = false,
    /** Продолжения шагов сюжета, что уже показаны: `герой:шаг`; повтор - из «Всего сюжета». */
    val storySeen: Set<String> = emptySet(),
    /** Таблица титулов «Хроники» свёрнута заголовком. */
    val titlesFolded: Boolean = false,
    /** Свойства уникальных и мифических лотов аукциона раскрыты сразу (A+C); на сервер не уходит. */
    val legendsOpen: Boolean = false,
    /** Чипы требований в карточке предмета раскрыты: свёртку «Можно / Нельзя надеть» игрок раскрыл, и она помнится на устройстве. */
    val requirementsOpen: Boolean = false,
    /** Ключ строки, что окно загрузки показало последней ([com.sperance.exileforge.core.i18n.LaunchLines]): следующая будет другой. */
    val lastLaunchLine: String = "",
) {
    companion object {
        val PAUSES = listOf(0, 30, 50)
    }
}

/** Камера карты «Похода»: точка мира в середине экрана ([x], [y]) и приближение [scale]. */
@Serializable
data class WorldView(val x: Int, val y: Int, val scale: Float)

/**
 * Свёрнутость карточки сюжета и шаг, при котором игрок её видел: [quest] - id задания главы, [ready] - ждала ли награда.
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

/** Вибрации - из :core. */
typealias Buzz = com.sperance.exileforge.core.session.Buzz

/** Скорости боя из правил (`fight.speeds`); контента ещё нет - умолчания правил. */
fun fightSpeeds(index: com.sperance.exileforge.rules.content.ContentIndex?): List<Int> = index?.rules?.fight?.speeds.orEmpty()

/** Сколько нажатий кнопки скорости боя дают скорость [speed] из настроек: 1 → 2 → 4. */
fun speedPresses(index: com.sperance.exileforge.rules.content.ContentIndex?, speed: Int): Int = fightSpeeds(index).indexOf(speed).coerceAtLeast(0)
