package com.sperance.exileforge.core.campaign.combat

/**
 * Итог здоровья героя за секунду боя (4.9.3): регенерация [regen] против дегена [degen] (его доля максимума - [degenShare]
 * в процентах) и отложенного урона [delayed], всё - здоровья в секунду. В бою недуги сюда не входят: их урон пишется своими
 * строками. На карте ([ailments]) - входят: урон недугов в секунду, что несёт герой.
 */
data class LifeFlow(
    val regen: Double = 0.0,
    val degen: Double = 0.0,
    val delayed: Double = 0.0,
    val degenShare: Double = 0.0,
    val ailments: Double = 0.0,
    /** Из [regen] - отдых вне боя `offCombatRegen` (4.12.x, UI-X2): на карте разбор называет его отдельно от регенерации листа. */
    val rest: Double = 0.0,
) {
    /** Сколько здоровья в секунду прибывает (плюс) или уходит (минус). */
    val net: Double get() = regen - degen - delayed - ailments

    /** Течёт хоть что-то: заметная регенерация, деген или отложенный урон - секунда стоит строки журнала. */
    val flowing: Boolean get() = regen >= 1 || degen > 0 || delayed > 0

    companion object {
        /** Ничего не течёт: до первой секунды боя и у героя без регенерации и дегена. */
        val NONE = LifeFlow()
    }
}

/**
 * Счётчик секунды для [LifeFlow] (4.9.3): регенерация, деген и отложенный урон героя копятся каждый шаг, раз в секунду
 * складываются в одну строку журнала. Кость не тянет - запись боя не меняет.
 */
internal class LifeMeter {
    private var regen = 0.0
    private var degen = 0.0
    private var delayed = 0.0
    private var from = 0.0

    fun regen(amount: Double) {
        regen += amount
    }

    fun degen(amount: Double) {
        degen += amount
    }

    fun delayed(amount: Double) {
        delayed += amount
    }

    /**
     * Итог секунды к [time] с долей дегена [degenShare] (в долях максимума): поток в секунду, и счёт начинается заново;
     * null - секунда ещё не прошла.
     */
    fun due(time: Double, degenShare: Double): LifeFlow? {
        val span = time - from
        if (span < 1.0) return null
        val flow = LifeFlow(regen / span, degen / span, delayed / span, degenShare * 100)
        regen = 0.0
        degen = 0.0
        delayed = 0.0
        from = time
        return flow
    }
}
