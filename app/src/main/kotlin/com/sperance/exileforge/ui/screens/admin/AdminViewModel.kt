package com.sperance.exileforge.ui.screens.admin

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.admin.Admin
import com.sperance.exileforge.core.admin.AdminRepository
import com.sperance.exileforge.core.admin.RedemptionActions
import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.ui.nav.Navigator
import kotlinx.coroutines.flow.StateFlow

/** Коды наград администратора: список, создание и удаление; команды - у сервиса кодов наград. */
class AdminViewModel(
    slice: GameSlice,
    admins: AdminRepository,
    private val navigator: Navigator,
    private val redemptions: RedemptionActions,
) : ViewModel() {
    val game: StateFlow<GameUi> = slice.ui
    val admin: StateFlow<Admin> = admins.state

    /** С кодов наград - назад: звено «Аккаунт» цепочки. */
    fun back() = navigator.back()

    fun loadRedemptions() = redemptions.load()
    fun createRedemption(code: RedemptionCode) = redemptions.create(code)
    fun deleteRedemption(id: String) = redemptions.delete(id)
}
