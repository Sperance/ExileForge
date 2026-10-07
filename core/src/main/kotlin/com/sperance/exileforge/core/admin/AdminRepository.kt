package com.sperance.exileforge.core.admin

import com.sperance.exileforge.core.model.command.RedemptionCode
import com.sperance.exileforge.core.network.TesterAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Инструменты администратора: коды наград. Тестовые учётки - в окне модерации (3.94.0). */
data class Admin(
    val redemptions: List<RedemptionCode> = emptyList(),
)

/** Единственный источник данных администратора (3.80.32); выход из аккаунта их забывает. */
class AdminRepository {
    private val mutable = MutableStateFlow(Admin())
    val state: StateFlow<Admin> = mutable.asStateFlow()

    fun update(transform: (Admin) -> Admin) = mutable.update(transform)

    fun clear() {
        mutable.value = Admin()
    }
}
