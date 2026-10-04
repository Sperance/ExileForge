package com.sperance.exileforge.presentation.admin

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.admin.Admin
import com.sperance.exileforge.core.admin.AdminRepository
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.presentation.ForgeRuntime
import com.sperance.exileforge.presentation.state.AppMode
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.Rarity
import kotlinx.coroutines.flow.StateFlow

/**
 * Инструменты администратора (3.80.40): режим, выдачи герою и коды наград - для отладочных экранов. Сами команды пока
 * у фич `ForgeRuntime`; экраны уже не получают общую модель.
 */
class AdminViewModel(private val runtime: ForgeRuntime, slice: GameSlice, admins: AdminRepository) : ViewModel() {
    val game: StateFlow<GameUi> = slice.ui
    val admin: StateFlow<Admin> = admins.state

    fun tab(tab: Int) = runtime.tab(tab)
    fun mode(mode: AppMode) = runtime.sessionViewModel.mode(mode)

    /** Случайная вещь: редкость и слот с панели, пусто - любые. */
    fun grantRandom(rarity: String, slot: String) = runtime.hero.grantRandom(rarity, slot)

    /** Названный шаблон, брошенный сервером на [rarity] или своей. */
    fun grant(template: String, rarity: Rarity? = null) = runtime.hero.grant(template, rarity)
    fun grantItem(code: String, amount: Long) = runtime.hero.grantItem(code, amount)
    fun addExperience(amount: Double) = runtime.hero.addExperience(amount)
    fun loadRedemptions() = runtime.redemptionViewModel.load()
    fun createRedemption(code: RedemptionCode) = runtime.redemptionViewModel.create(code)
    fun deleteRedemption(id: String) = runtime.redemptionViewModel.delete(id)
}
