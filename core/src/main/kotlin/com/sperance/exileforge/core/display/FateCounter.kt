package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroInfo
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.FateLever

/**
 * Из чего считается счёт дара: герой [info] (счета, что хранит сервер), рычаги дара [effects] и выигранные в идущем заходе бои
 * Разгона [won]. В заходе рычаги - замороженные захода, вне захода - рычаги героя с переносом сервера (`fateCarry`) и [won] = 0:
 * перенос в обоих случаях несёт [FateEffects.carry].
 */
class FateSource(val info: HeroInfo, val effects: FateEffects, val won: Int = 0)

/** Счёт дара [counter], как его показывает подпись ленты дара на листе героя: [caption]. */
data class FateTally(val counter: FateCounter, val caption: String)

/**
 * Что копит дар - по рычагу [lever]: дар с этим рычагом показывает счёт, дар без счётного рычага - ничего. Числа - функциями
 * правил [FateEffects], тех же, что у сервера. Новый счётный рычаг - новый элемент здесь.
 */
enum class FateCounter(val lever: FateLever) {
    /** Трофеи боссов: всего (цена обмена - [FateEffects.trophyPrice]; без неё счёта нет). */
    TROPHIES(FateLever.TROPHIES) {
        override fun tally(source: FateSource): FateTally? {
            source.effects.trophyPrice() ?: return null
            val total = source.info.trophies.values.filter { it > 0 }.sum().toLong()
            return FateTally(this, ui("fate.trophies_total", groupedNumber(total)))
        }
    },

    /** Щедрость судьбы: боёв до сферы ([FateEffects.bountyStep]) по счёту сервера `fateFights`. */
    BOUNTY(FateLever.FIGHT_BOUNTY) {
        override fun tally(source: FateSource): FateTally? {
            val step = source.effects.bountyStep(source.info.fateFights) ?: return null
            return FateTally(this, ui("fate.bounty_next", step.left))
        }
    },

    /** Разгон: побед в счёте ([FateEffects.streakWins]). */
    STREAK(FateLever.WIN_STREAK) {
        override fun tally(source: FateSource): FateTally = FateTally(this, ui("fate.streak_count", groupedNumber(source.effects.streakWins(source.won).toLong())))
    },
    ;

    /** Счёт этого рычага у [source]; null - рычагу нечего показать. */
    internal abstract fun tally(source: FateSource): FateTally?

    companion object {
        /** Счёт дара героя [source]; дар без счётного рычага - null. */
        fun of(source: FateSource): FateTally? = entries.firstNotNullOfOrNull { counter -> counter.takeIf { source.effects.has(it.lever) }?.tally(source) }
    }
}
