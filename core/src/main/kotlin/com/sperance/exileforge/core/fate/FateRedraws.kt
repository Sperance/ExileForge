package com.sperance.exileforge.core.fate

import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.phrase
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.fate.FateView
import com.sperance.exileforge.core.model.hero.BagUse
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Notices
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Раздача «Колоды Рока»: стопка [item], что её открыла, раздача сервера [view] (нынешний дар и три новых; null - читается) и [accepted] -
 * код принятого дара: выбор принят, экран уходит.
 */
data class FateDeal(val item: String, val view: FateView? = null, val accepted: String? = null)

/**
 * Смены своего Предначертания: «Колода Рока» из сумки - новая раздача из трёх даров без нынешнего (`GET fate/deck`), выбор - стопкой
 * (`hero/bag/use`), - и смена себе любым даром модератором и тестировщиком (`POST fate/self`). Новый дар ложится в профиль сессии, герои и их
 * список перечитываются: счётчики прежнего дара у героев с нуля.
 */
class FateRedraws(
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val sessions: SessionRepository,
    private val heroes: HeroRepository,
    private val notices: Notices,
) {
    private val mutable = MutableStateFlow<FateDeal?>(null)
    val deal: StateFlow<FateDeal?> = mutable.asStateFlow()

    private val api get() = connection.api

    /** Раздача колодой [item]: экран открыт сразу, карты - когда придут. */
    fun open(item: String) {
        mutable.value = FateDeal(item)
        commands.read(Reads.FATE_DEAL, restart = true) {
            val view = api.fate.deck()
            mutable.update { it?.takeIf { deal -> deal.item == item }?.copy(view = view) }
        }
    }

    /** Выбор дара [code] из раздачи: колода тратится той же командой, раздача помечается принятой. */
    fun choose(code: String) {
        val deal = mutable.value ?: return
        val heroId = heroes.heroId
        val started = commands.task(writing = true, touches = TOUCHED) {
            val view = api.hero.useBag(heroId, BagUse(deal.item, fate = code)).fate ?: return@task
            accept(view)
            mutable.update { it?.copy(accepted = view.chosen?.code ?: code) }
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    fun close() {
        mutable.value = null
    }

    /** Свой дар [code] - модератору и аккаунту с героем-тестировщиком (окно тестирования). */
    fun redrawOwn(code: String) {
        val started = commands.task(writing = true, touches = TOUCHED) {
            val view = api.fate.redrawOwn(code)
            accept(view)
            notices.toast(ui("moderation.fate.changed", view.chosen?.title.orEmpty()))
        }
        if (!started) commands.refuse(phrase("runtime.busy_retry"))
    }

    /** Новый дар [view] - в профиль сессии, как после выбора. */
    private fun accept(view: FateView) = sessions.update { it.copy(fate = view, profile = it.profile?.let { p -> p.copy(fate = view.chosen?.code ?: p.fate) }) }

    private companion object {
        /** Смена дара меняет героев аккаунта и их список. */
        val TOUCHED = setOf(Reads.HERO, Reads.CHARACTERS)
    }
}
