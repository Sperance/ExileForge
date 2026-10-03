package com.sperance.exileforge.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sperance.exileforge.core.campaign.LogKind
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.Building
import com.sperance.exileforge.presentation.state.Buzz
import com.sperance.exileforge.presentation.state.StashSort
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Оболочка приложения (3.80.30): вкладки и здания по прежнему номеру, «Настройки» поверх экрана, строка в тосты.
 * Пока над `ForgeRuntime` ради проверки административных вкладок; уедет вместе с ним.
 */
class ShellViewModel(private val runtime: ForgeRuntime) : ViewModel() {
    /** Вкладка по прежнему номеру; закрытую уровнем героя навигатор не откроет и скажет, с какого. */
    fun tab(tab: Int) = runtime.tab(tab)

    /** Здание Города (3.22.0) или площадь для null. */
    fun building(building: Building?) {
        runtime.commands.dismissMessage()
        runtime.navigator.tab(Route.ofBuilding(building))
    }

    /** «Настройки» (3.77.0) поверх открытой вкладки; закрытие возвращает на неё. */
    fun openSettings() {
        runtime.commands.dismissMessage()
        runtime.navigator.open(Route.Settings)
    }

    fun closeSettings() = runtime.navigator.back()

    /** Проба связи сразу (3.30.0): нажата иконка «не в сети». */
    fun retryLink() = runtime.connectionViewModel.wake(now = true)

    /** Строка в тосты с экранов (3.76.0: место, открытое уровнем). */
    fun announce(text: String) = runtime.toast(text)

    fun dismissMessage() = runtime.commands.dismissMessage()
    fun dismissNotice() = runtime.notices.dismiss()
    fun buzz(kind: Buzz) = runtime.buzz(kind)
    fun clearLogs() = runtime.journal.clear()

    /** Настройки устройства, пока живущие в общем состоянии: порядок сундука и «скрыть надетое» (3.30.0, 3.69.0), фильтр журнала боя. */
    fun stashSort(sort: StashSort) {
        runtime.mutable.update { it.copy(stashSort = sort) }
        viewModelScope.launch { runtime.store.saveStashSort(sort.name) }
    }

    fun stashHideWorn(hide: Boolean) {
        runtime.mutable.update { it.copy(stashHideWorn = hide) }
        viewModelScope.launch { runtime.store.saveStashHideWorn(hide) }
    }

    fun logFilter(kinds: Set<LogKind>) {
        runtime.mutable.update { it.copy(logFilter = kinds) }
        viewModelScope.launch { runtime.store.saveLogFilter(LogKind.write(kinds)) }
    }
}
