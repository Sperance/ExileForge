package com.sperance.exileforge.core.display

import com.sperance.exileforge.core.app.Building
import com.sperance.exileforge.core.i18n.plural
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroInfo
import com.sperance.exileforge.rules.content.FateEffect
import com.sperance.exileforge.rules.content.FateEffects
import com.sperance.exileforge.rules.content.FateLever

/**
 * Из чего считается счёт дара: герой [info] (счета, что хранит сервер), рычаги дара [effects] и выигранные бои Разгона [won].
 * В заходе рычаги - замороженные захода (с переносом прошлых заходов [FateEffects.carry]), [won] - бои самого захода; вне
 * захода - рычаги героя без переноса, а [won] - перенос, что сервер отдал героем (`fateCarry`): итог Разгона в обоих случаях
 * `won + carry`, прибавка - [FateEffects.streak].
 */
class FateSource(val info: HeroInfo, val effects: FateEffects, val won: Int) {
    companion object {
        /** Вне захода: рычаги героя [effects] и перенос Разгона сервера. */
        fun idle(info: HeroInfo, effects: FateEffects) = FateSource(info, effects, info.fateCarry)
    }
}

/**
 * Счёт дара, как его показывают чип шапки героя и подпись ленты: коротко для чипа [short] (у порогового счёта - сколько осталось),
 * полная подпись [caption], доля пути до порога [progress] (null - счёт без порога) и строки подсказки «подпись - число» [facts].
 */
data class FateTally(
    val counter: FateCounter,
    val short: String,
    val caption: String,
    val progress: Float? = null,
    val facts: List<Pair<String, String>> = emptyList(),
)

/**
 * Что копит дар - по рычагу [lever]: дар с этим рычагом показывает счёт, дар без счётного рычага - ничего. [visit] - здание,
 * где счёт тратят (кнопка в подсказке чипа), null - тратить негде. Новый счётный рычаг - новый элемент здесь.
 */
enum class FateCounter(val lever: FateLever, val visit: Building? = null) {
    /** Трофеи боссов: всего и у скольких боссов их хватает на обмен (цена - [FateEffect.value] на силе дара). */
    TROPHIES(FateLever.TROPHIES, Building.MERCHANT) {
        override fun tally(effect: FateEffect, source: FateSource): FateTally {
            val counts = source.info.trophies.values.filter { it > 0 }
            val total = counts.sum().toLong()
            val ready = counts.count { it >= effect.value.toInt() }
            return FateTally(
                this,
                groupedNumber(total),
                ui("fate.trophies_total", groupedNumber(total)),
                facts = listOf(ui("fate.trophies_ready") to ui("fate.trophies_ready_value", ready, plural("fate.bosses", ready))),
            )
        }
    },

    /** Щедрость судьбы: боёв до сферы - порог [FateEffect.every] на силе дара и строках надетого, счёт сервера `fateFights`. */
    BOUNTY(FateLever.FIGHT_BOUNTY) {
        override fun tally(effect: FateEffect, source: FateSource): FateTally? {
            val every = effect.every.takeIf { it > 0 } ?: return null
            val done = (source.info.fateFights % every).toInt()
            val left = every - done
            return FateTally(
                this,
                "$left " + plural("fate.fights", left),
                ui("fate.bounty_next", left),
                progress = done.toFloat() / every,
                facts = listOf(ui("fate.bounty_every") to every.toString()),
            )
        }
    },

    /** Разгон: побед в счёте (заход и перенос прошлых) и прибавка урона и добычи на них, до потолка рычага. */
    STREAK(FateLever.WIN_STREAK) {
        override fun tally(effect: FateEffect, source: FateSource): FateTally {
            val count = (source.won + source.effects.carry).toLong()
            return FateTally(
                this,
                groupedNumber(count),
                ui("fate.streak_count", groupedNumber(count)),
                facts = listOf(ui("fate.streak_loot") to "+" + number(source.effects.streak(source.won)) + "%"),
            )
        }
    },
    ;

    /** Счёт этого рычага с полями [effect] на силе дара; null - рычагу нечего показать. */
    internal abstract fun tally(effect: FateEffect, source: FateSource): FateTally?

    companion object {
        /** Счёт дара героя [source]; дар без счётного рычага - null. */
        fun of(source: FateSource): FateTally? = entries.firstNotNullOfOrNull { counter -> source.effects.of(counter.lever)?.let { counter.tally(it, source) } }
    }
}
