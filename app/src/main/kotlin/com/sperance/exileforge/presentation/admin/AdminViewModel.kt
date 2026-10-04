package com.sperance.exileforge.presentation.admin

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.admin.Admin
import com.sperance.exileforge.core.admin.AdminRepository
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.presentation.app.RedemptionActions
import com.sperance.exileforge.presentation.app.SessionActions
import com.sperance.exileforge.presentation.hero.HeroActions
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.Rarity
import kotlinx.coroutines.flow.StateFlow

/**
 * Инструменты администратора (3.80.40): режим, выдачи герою и коды наград - для отладочных экранов. Команды - у сервисов
 * сессии, героя и кодов наград (3.80.44).
 */
class AdminViewModel(
    slice: GameSlice,
    admins: AdminRepository,
    private val navigator: Navigator,
    private val session: SessionActions,
    private val hero: HeroActions,
    private val redemptions: RedemptionActions,
) : ViewModel() {
    val game: StateFlow<GameUi> = slice.ui
    val admin: StateFlow<Admin> = admins.state

    /** Коды наград - экран над инструментами администратора. */
    fun openRedemptions() = navigator.open(Route.Redemption)
    fun mode(mode: AppMode) = session.mode(mode)

    /** Случайная вещь: редкость и слот с панели, пусто - любые. */
    fun grantRandom(rarity: String, slot: String) = hero.grantRandom(rarity, slot)

    /** Названный шаблон, брошенный сервером на [rarity] или своей. */
    fun grant(template: String, rarity: Rarity? = null) = hero.grant(template, rarity)
    fun grantItem(code: String, amount: Long) = hero.grantItem(code, amount)
    fun addExperience(amount: Double) = hero.addExperience(amount)
    fun loadRedemptions() = redemptions.load()
    fun createRedemption(code: RedemptionCode) = redemptions.create(code)
    fun deleteRedemption(id: String) = redemptions.delete(id)
}
