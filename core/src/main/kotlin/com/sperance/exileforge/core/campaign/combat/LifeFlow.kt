package com.sperance.exileforge.core.campaign.combat

/**
 * Итог здоровья героя за секунду боя: регенерация [regen] против дегена [degen] (его доля максимума - [degenShare]
 * в процентах) и отложенного урона [delayed], всё - здоровья в секунду. В бою недуги сюда не входят: их урон пишется своими
 * строками. На карте ([ailments]) - входят: урон недугов в секунду, что несёт герой.
 */
data class LifeFlow(
    val regen: Double = 0.0,
    val degen: Double = 0.0,
    val delayed: Double = 0.0,
    val degenShare: Double = 0.0,
    val ailments: Double = 0.0,
    /** Из [regen] - отдых вне боя `offCombatRegen`: на карте разбор называет его отдельно от регенерации листа. */
    val rest: Double = 0.0,
    /** Из [ailments] - что принимает энергощит: урон недугов вне боя идёт сперва в щит, яд и хаос - мимо него. */
    val shielded: Double = 0.0,
    /**
     * Ранящий недуг, что глушит восстановление на карте: сильнейший по урону в секунду из кровотечения, яда и
     * поджога; пока он на герое, ни регенерации, ни отдыха нет. В бою и без ранящего недуга - null.
     */
    val stalledBy: Ailment? = null,
    /** Сколько здоровья в секунду дали бы регенерация и отдых, не будь [stalledBy] - для разбора; иначе 0. */
    val stalled: Double = 0.0,
) {
    /** Сколько здоровья в секунду прибывает (плюс) или уходит (минус): урон недугов, что принял щит, здоровья не трогает. */
    val net: Double get() = regen - degen - delayed - ailments + shielded

    /**
     * Здоровье [life] из [max] на карте и правда меняется: метка «±N/с» на полосе жизни - только тогда. Ранящий недуг
     * пишется всегда (он и глушит восстановление); прибыль на полном здоровье и убыль на дне дегена - покой.
     */
    fun moves(life: Double, max: Double): Boolean = stalledBy != null || when {
        net >= MOVING -> life < max
        net <= -MOVING -> life > FLOOR
        else -> false
    }

    /** Течёт хоть что-то: заметная регенерация, деген или отложенный урон - секунда стоит строки журнала. */
    val flowing: Boolean get() = regen >= 1 || degen > 0 || delayed > 0

    companion object {
        /** Ничего не течёт: до первой секунды боя и у героя без регенерации и дегена. */
        val NONE = LifeFlow()

        /** Ниже этого здоровья деген на карте не опускает (урон недугов - опускает и убивает). */
        const val FLOOR = 1.0

        /** Меньше этого в секунду - покой: число «±0/с» не пишется. */
        private const val MOVING = .5
    }
}

/**
 * Счётчик секунды для [LifeFlow]: регенерация, деген и отложенный урон героя копятся каждый шаг, раз в секунду
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
