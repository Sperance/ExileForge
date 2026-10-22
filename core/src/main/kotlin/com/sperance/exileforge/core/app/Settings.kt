package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.hero.BagFilter
import com.sperance.exileforge.rules.content.FightRules
import com.sperance.exileforge.rules.content.MapCode
import kotlinx.serialization.Serializable

/**
 * The player's settings («Настройки», variant A of three mockups), kept on the device as one document: how the
 * screen, the fight, the text and the motion behave and what the phone buzzes for. Every field has the default a first
 * start gets, so a document from an older version reads whole.
 */
@Serializable
data class GameSettings(
    val keepScreen: KeepScreen = KeepScreen.EXPEDITION,
    /** Скорость, с которой начинается каждый бой - одна из скоростей правил `fight.speeds`; кнопка боя меняет её дальше по кругу. */
    val fightSpeed: Int = 1,
    val damageNumbers: DamageNumbers = DamageNumbers.ALL,
    /** The hero's life share, in per cent, under which a running fight pauses by itself; 0 never. */
    val autoPause: Int = 0,
    /** Ускорение шага героя по карте после гибели босса (`expedition.bossSlainSpeed`); выключено - шаг обычный, метка «×N» серая. */
    val swiftAfterBoss: Boolean = true,
    val textSize: TextSize = TextSize.M,
    /** Pulses, glows and marching roads; off for a weak phone or a low battery. */
    val animations: Boolean = true,
    /** Упрощённые эффекты боя: без тряски и вспышек - для чувствительных глаз и слабого телефона. */
    val simpleEffects: Boolean = false,
    val buzzDanger: Boolean = true,
    val buzzButtons: Boolean = true,
    /** Где стояла камера карты «Похода» у каждого героя: по id героя; вход на вкладку возвращает её туда же. */
    val worldViews: Map<String, WorldView> = emptyMap(),
    /**
     * Последняя зона, в которую входил каждый герой на этом устройстве, по id героя: удобство устройства, как [worldViews], - сервер
     * последней зоны не хранит (только открытый заход); к ней ведут кнопка «К герою» и первая камера карты «Похода».
     */
    val lastZones: Map<String, MapCode> = emptyMap(),
    /** Недавние зоны входа каждого героя на этом устройстве, новые первыми (не больше [GameSettings.RECENT_ZONES]): лист поиска зон их предлагает. */
    val recentZones: Map<String, List<MapCode>> = emptyMap(),
    /** Пролог уже показан на этом устройстве: дальше перед созданием героя его можно пропустить сразу. */
    val prologueSeen: Boolean = false,
    /** Подсказка боя «Коснитесь врага, чтобы бить его первым» уже показана на этом устройстве: крестик или первое касание врага. */
    val focusTipSeen: Boolean = false,
    /** Таблица титулов «Хроники» свёрнута заголовком. */
    val titlesFolded: Boolean = false,
    /** Свойства уникальных и мифических лотов аукциона раскрыты сразу (A+C); на сервер не уходит. */
    val legendsOpen: Boolean = false,
    /** Чипы требований в карточке предмета раскрыты: свёртку «Можно / Нельзя надеть» игрок раскрыл, и она помнится на устройстве. */
    val requirementsOpen: Boolean = false,
    /** Панель механик тестировщика в карточке зоны раскрыта; по умолчанию свёрнута заголовком. */
    val testerMechanicsOpen: Boolean = false,
    /** Отбор и порядок сумки героя ([BagFilter]): полки, «Можно применить», порядок внутри полки; поиск не помнится. */
    val bag: BagFilter = BagFilter(),
    /** Ключ строки, что окно загрузки показало последней ([com.sperance.exileforge.core.i18n.LaunchLines]): следующая будет другой. */
    val lastLaunchLine: String = "",
) {
    companion object {
        val PAUSES = listOf(0, 30, 50)

        /** Сколько недавних зон входа помнит устройство на героя ([recentZones]). */
        const val RECENT_ZONES = 5
    }
}

/** Камера карты «Похода»: точка мира в середине экрана ([x], [y]) и приближение [scale]. */
@Serializable
data class WorldView(val x: Int, val y: Int, val scale: Float)

/** When the screen stays lit: only while a run or a trial is on screen, always, or as the phone decides. */
enum class KeepScreen { EXPEDITION, ALWAYS, NEVER }

/** Which numbers rise off the fighters' cards. */
enum class DamageNumbers { ALL, CRITS, OFF }

/** The game's text against the phone's own size. */
enum class TextSize(val scale: Float) { S(.9f), M(1f), L(1.15f) }

/** Скорость начала любого боя (поход, испытания, Разлом): [GameSettings.fightSpeed] среди скоростей [rules]; незнакомая - первая из них. */
fun GameSettings.startSpeed(rules: FightRules): Int = fightSpeed.takeIf { it in rules.speeds } ?: rules.speeds.first()

/**
 * Настройки устройства, с которыми начинается заход (`ExpeditionRun.start`, и зона Ваал тоже): [speed] - скорость боёв на входе,
 * дальше её меняет кнопка скорости; [swiftAllowed] - ускорение после босса включено ([GameSettings.swiftAfterBoss]), дальше его
 * ведёт команда `RunCommand.SwiftAllowed`.
 */
data class RunPrefs(val speed: Int, val swiftAllowed: Boolean) {
    companion object {
        /** Умолчания первого запуска: скорость 1, ускорение включено. */
        val DEFAULT = RunPrefs(speed = 1, swiftAllowed = true)

        /** Из настроек [settings] под скорости боя [rules]. */
        fun of(settings: GameSettings, rules: FightRules): RunPrefs = RunPrefs(settings.startSpeed(rules), settings.swiftAfterBoss)
    }
}
