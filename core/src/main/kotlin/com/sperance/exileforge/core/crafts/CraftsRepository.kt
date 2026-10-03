package com.sperance.exileforge.core.crafts

import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.rules.roll.WorkGains
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Ремёсла, как сервер ответил последним, и часы устройства в тот миг ([readAt]); [totals] - итог сеанса,
 * [last] - последний цикл, [pending] - циклы, брошенные здесь раньше, чем их сосчитал сервер.
 */
data class Crafts(
    val state: CraftsState? = null,
    val readAt: Long = 0,
    val totals: WorkGains = WorkGains(),
    val last: WorkGains? = null,
    val pending: WorkGains = WorkGains(),
) {
    /** Сдвиг часов сервера против устройства на миг ответа. */
    val offset: Long get() = state?.let { it.now - readAt } ?: 0L
}

/** Единственный источник правды о ремёслах (3.80.14). */
class CraftsRepository {
    private val mutable = MutableStateFlow(Crafts())
    val state: StateFlow<Crafts> = mutable

    fun update(transform: (Crafts) -> Crafts) = mutable.update(transform)

    fun clear() = update { Crafts() }
}
