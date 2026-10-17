package com.sperance.exileforge.ui.screens.admin

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.admin.Admin
import com.sperance.exileforge.core.admin.AdminRepository
import com.sperance.exileforge.core.admin.RedemptionActions
import com.sperance.exileforge.core.app.AppMode
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.SessionActions
import com.sperance.exileforge.core.hero.HeroActions
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.nav.Navigator
import com.sperance.exileforge.ui.nav.Route
import kotlinx.coroutines.flow.StateFlow

/**
 * Инструменты администратора: режим, выдачи герою и коды наград - для отладочных экранов. Команды - у сервисов
 * сессии, героя и кодов наград.
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
