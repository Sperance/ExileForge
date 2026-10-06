package com.sperance.exileforge.core.crafts

import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.rules.roll.WorkGains
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Ремёсла, как сервер ответил последним, и часы устройства в тот миг ([readAt]); [totals] - итог сеанса,
 * [last] - что принёс последний пересчёт с циклами (3.89.1: его показывает всплывашка у полосы цикла).
 */
data class Crafts(
    val state: CraftsState? = null,
    val readAt: Long = 0,
    val totals: WorkGains = WorkGains(),
    val last: Harvest? = null,
) {
    /** Сдвиг часов сервера против устройства на миг ответа. */
    val offset: Long get() = state?.let { it.now - readAt } ?: 0L
}

/**
 * Сбор одного пересчёта (3.89.1): сумма циклов между двумя ответами сервера; [seq] растёт с каждым новым сбором,
 * чтобы экран показал всплывашку один раз на сбор, даже если два подряд принесли одно и то же.
 */
data class Harvest(val gains: WorkGains, val seq: Long)

/** Единственный источник правды о ремёслах (3.80.14). */
class CraftsRepository {
    private val mutable = MutableStateFlow(Crafts())
    val state: StateFlow<Crafts> = mutable

    fun update(transform: (Crafts) -> Crafts) = mutable.update(transform)

    fun clear() = update { Crafts() }
}
