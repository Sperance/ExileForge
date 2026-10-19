package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.i18n.plural
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
 * Счёт дара, как его показывают чип шапки героя и подпись ленты: коротко для чипа [short] (у порогового счёта - сколько осталось),
 * полная подпись [caption] (лента и чтение с экрана), доля пути до порога [progress] (null - счёт без порога) и строки подсказки
 * «подпись - число» [facts], первая - сам счёт.
 */
data class FateTally(
    val counter: FateCounter,
    val short: String,
    val caption: String,
    val progress: Float? = null,
    val facts: List<Pair<String, String>> = emptyList(),
)

/**
 * Что копит дар - по рычагу [lever]: дар с этим рычагом показывает счёт, дар без счётного рычага - ничего. Числа - функциями
 * правил [FateEffects], тех же, что у сервера. Новый счётный рычаг - новый элемент здесь.
 */
enum class FateCounter(val lever: FateLever) {
    /** Трофеи боссов: всего и у скольких боссов их хватает на обмен (цена - [FateEffects.trophyPrice]). */
    TROPHIES(FateLever.TROPHIES) {
        override fun tally(source: FateSource): FateTally? {
            val price = source.effects.trophyPrice() ?: return null
            val counts = source.info.trophies.values.filter { it > 0 }
            val total = groupedNumber(counts.sum().toLong())
            val ready = counts.count { it >= price }
            return FateTally(
                this,
                total,
                ui("fate.trophies_total", total),
                facts = listOf(ui("fate.trophies_all") to total, ui("fate.trophies_ready") to ui("fate.trophies_ready_value", ready, plural("fate.bosses", ready))),
            )
        }
    },

    /** Щедрость судьбы: боёв до сферы ([FateEffects.bountyStep]) по счёту сервера `fateFights`. */
    BOUNTY(FateLever.FIGHT_BOUNTY) {
        override fun tally(source: FateSource): FateTally? {
            val step = source.effects.bountyStep(source.info.fateFights) ?: return null
            return FateTally(
                this,
                ui("fate.fights_left", step.left, plural("fate.fights", step.left)),
                ui("fate.bounty_next", step.left),
                progress = step.done.toFloat() / step.every,
                facts = listOf(ui("fate.bounty_left") to step.left.toString(), ui("fate.bounty_every") to step.every.toString()),
            )
        }
    },

    /** Разгон: побед в счёте ([FateEffects.streakWins]) и прибавка урона и добычи на них ([FateEffects.streak]). */
    STREAK(FateLever.WIN_STREAK) {
        override fun tally(source: FateSource): FateTally {
            val wins = groupedNumber(source.effects.streakWins(source.won).toLong())
            return FateTally(
                this,
                wins,
                ui("fate.streak_count", wins),
                facts = listOf(ui("fate.streak_wins") to wins, ui("fate.streak_loot") to ui("fate.streak_loot_value", number(source.effects.streak(source.won)))),
            )
        }
    },
    ;

    /** Счёт этого рычага у [source]; null - рычагу нечего показать. */
    internal abstract fun tally(source: FateSource): FateTally?

    companion object {
        /** Счёт дара героя [source]; дар без счётного рычага - null. */
        fun of(source: FateSource): FateTally? = entries.firstNotNullOfOrNull { counter -> counter.takeIf { source.effects.has(it.lever) }?.tally(source) }
    }
}
