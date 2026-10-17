package com.sperance.exileforge.core.character

import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.rules.roll.RolledMonster
import com.sperance.exileforge.rules.sheet.Might
import com.sperance.exileforge.rules.sheet.MightExtras
import com.sperance.exileforge.rules.sheet.MightReading

/**
 * Мощь на клиенте: одна точка над функцией правил [Might] - клиент её не дублирует и не «подправляет». Герой меряется
 * своим телом боя: на листе героя - боевой лист без карты (`CombatSheet.might`, [Might.hero] - та же функция, что у сервера), в бою -
 * лист под эффектами карты (`FightHud.heroBody`) с уроном умений из боевого листа. Не прогноз боя.
 */
object Mights {
    /** Мощь героя в бою: тело [body] под эффектами карты и что сверх листа [extras] (умения, питомец, резерв маны, фляги - `CombatSheet.mightExtras`). */
    fun hero(index: ContentIndex, body: Combatant, extras: MightExtras): MightReading = Might.hero(index, body.stats, body.level, extras)

    /**
     * Мощь монстра [monster] по его листу; уровень 0 у монстра - уровень зоны [level]. У босса ([boss]) - null: его силу
     * меряет доля побед эталонов, а не Мощь, и клиент её нигде не показывает.
     */
    fun of(index: ContentIndex, monster: RolledMonster, level: Int): MightReading? = if (monster.boss) null else Might.of(index, monster.stats, monster.level.takeIf { it > 0 } ?: level)
}

/**
 * Босс - всякий враг, кого ставит `MonsterRoller.boss` правил (стражи зон и карт, финалы, стражи Ваал, вожди, страж Разлома,
 * Матерь Скверны): у всех редкость босса. Одна точка признака для клиента.
 */
val RolledMonster.boss: Boolean get() = rarity == MonsterRarity.UNIQUE

/** Мощь героя до и после смены вещи: «было → стало» в итоге героя карточки вещи. */
data class MightShift(val before: Double, val after: Double) {
    /** Доля изменения (на 5% больше); без прежней Мощи - ноль. */
    val change: Double get() = if (before > 0) after / before - 1 else 0.0

    /** Куда сдвинулась Мощь: меньше полупроцента - шум ([GearVerdict.Shift.EVEN]). */
    val shift: GearVerdict.Shift get() = GearVerdict.Shift.of(change)

    /**
     * Сдвиг стоит показа - одно правило на все места (значок списков, строка карточки): Мощь посчитана и сдвиг не
     * шум. Нет - ни «⚜ =», ни пустой строки «было → стало».
     */
    val shown: Boolean get() = before > 0 && shift != GearVerdict.Shift.EVEN
}

/**
 * Где враг стоит против героя по Мощи: только цвет числа на карточке врага - серый, золото, красный. Не прогноз боя и не
 * баланс игры: пороги ниже - константы отображения, ни на какой подсчёт они не влияют.
 */
enum class MightStanding {
    /** Слабее героя вдвое и больше. */
    WEAKER,

    /** Сопоставим с героем. */
    MATCHED,

    /** Заметно сильнее героя. */
    STRONGER,
    ;

    companion object {
        /** Враг слабее, когда его Мощь не больше этой доли Мощи героя («слабее вдвое»). */
        private const val WEAKER_SHARE = 0.5

        /** Враг сильнее, когда его Мощь больше этой доли Мощи героя: меньший перевес - ещё «сопоставим». */
        private const val STRONGER_SHARE = 1.2

        /** Стойка врага с Мощью [foe] против героя с Мощью [hero]; без Мощи героя - сопоставим. */
        fun of(foe: Double, hero: Double): MightStanding = when {
            hero <= 0 -> MATCHED
            foe <= hero * WEAKER_SHARE -> WEAKER
            foe > hero * STRONGER_SHARE -> STRONGER
            else -> MATCHED
        }
    }
}
