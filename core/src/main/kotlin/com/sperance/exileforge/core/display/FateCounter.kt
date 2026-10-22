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

/**
 * Что копит дар - по рычагу [lever]: дар с этим рычагом показывает счёт, дар без счётного рычага - ничего. Числа - функциями
 * правил [FateEffects], тех же, что у сервера. Новый счётный рычаг - новый элемент здесь.
 */
enum class FateCounter(val lever: FateLever) {
    /** Трофеи боссов: всего (цена обмена - [FateEffects.trophyPrice]; без неё счёта нет). */
    TROPHIES(FateLever.TROPHIES) {
        override fun tally(source: FateSource): String? {
            source.effects.trophyPrice() ?: return null
            return ui("fate.trophies_total", groupedNumber(source.info.trophies.values.filter { it > 0 }.sum().toLong()))
        }
    },

    /** Щедрость судьбы: боёв до сферы ([FateEffects.bountyStep]) по счёту сервера `fateFights`. */
    BOUNTY(FateLever.FIGHT_BOUNTY) {
        override fun tally(source: FateSource): String? {
            val step = source.effects.bountyStep(source.info.fateFights) ?: return null
            return ui("fate.bounty_next", step.left)
        }
    },

    /** Разгон: побед в счёте ([FateEffects.streakWins]). */
    STREAK(FateLever.WIN_STREAK) {
        override fun tally(source: FateSource): String = ui("fate.streak_count", groupedNumber(source.effects.streakWins(source.won).toLong()))
    },
    ;

    /** Счёт этого рычага у [source] подписью ленты дара; null - рычагу нечего показать. */
    internal abstract fun tally(source: FateSource): String?

    companion object {
        /** Счётный рычаг дара с рычагами [effects]; дар ничего не копит - null. */
        fun of(effects: FateEffects): FateCounter? = entries.firstOrNull { effects.has(it.lever) }

        /** Счёт дара героя [source] подписью ленты дара на листе героя; дар без счётного рычага или без счёта - null. */
        fun of(source: FateSource): String? = of(source.effects)?.tally(source)
    }
}
