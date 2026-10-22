package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.campaign.LogKind
import com.sperance.exileforge.core.filter.ItemFilter
import com.sperance.exileforge.core.filter.ItemFilters
import com.sperance.exileforge.core.filter.ItemShelf
import com.sperance.exileforge.core.i18n.LaunchLines
import com.sperance.exileforge.rules.content.FightRules
import com.sperance.exileforge.rules.content.MapCode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Настройки игрока на устройстве как единственный источник правды: экраны читают потоки
 * отсюда, а не копию в общем состоянии. Запись идёт в [SettingsStore], поток обновляется сам.
 */
class PreferencesRepository(private val store: SettingsStore, scope: CoroutineScope) {
    val settings: StateFlow<GameSettings> = store.gameSettings.stateIn(scope, SharingStarted.Eagerly, GameSettings())

    /** Запомненные фильтры списков предметов. */
    val itemFilters: StateFlow<ItemFilters> = store.itemFilters.stateIn(scope, SharingStarted.Eagerly, ItemFilters())

    /** Какие строки журнала боя видны (из общего состояния сюда). */
    val logFilter: StateFlow<Set<LogKind>> = store.logFilter.map { LogKind.parse(it) }.stateIn(scope, SharingStarted.Eagerly, LogKind.DEFAULT)

    /** Правка настроек от записанного на устройстве, атомарно в хранилище: на старте поток ещё может держать умолчания, а параллельные правки не затирают друг друга. */
    suspend fun editSettings(edit: GameSettings.() -> GameSettings) = store.editGameSettings { it.edit() }

    /** Скорость начала любого боя (поход, испытания, Разлом) по нынешним настройкам ([GameSettings.startSpeed]). */
    fun fightSpeed(rules: FightRules): Int = settings.value.startSpeed(rules)

    /** С чем начинается заход по нынешним настройкам ([RunPrefs.of]). */
    fun runPrefs(rules: FightRules): RunPrefs = RunPrefs.of(settings.value, rules)

    suspend fun saveItemFilter(shelf: ItemShelf, value: ItemFilter) = store.saveItemFilters(itemFilters.value.with(shelf, value))

    suspend fun saveLogFilter(value: Set<LogKind>) = store.saveLogFilter(LogKind.write(value))

    /** Где стояла камера карты у героя [heroId]; не стояла - null, карта откроется на фронтире. */
    fun worldView(heroId: String): WorldView? = settings.value.worldViews[heroId]

    /** Запомнить камеру карты героя [heroId] на устройстве: только настройки, сервер не знает; без героя - ничего. */
    suspend fun keepWorldView(heroId: String, view: WorldView) {
        if (heroId.isBlank()) return
        editSettings { copy(worldViews = worldViews + (heroId to view)) }
    }

    /** Последняя зона, в которую входил герой [heroId] на этом устройстве; не входил - null. */
    fun lastZone(heroId: String): MapCode? = settings.value.lastZones[heroId]

    /** Запомнить зону [zone], в которую вошёл герой [heroId], и поставить её первой в недавние: только настройки устройства; без героя - ничего. */
    suspend fun keepLastZone(heroId: String, zone: MapCode) {
        if (heroId.isBlank()) return
        editSettings {
            val recent = (listOf(zone) + recentZones[heroId].orEmpty().filter { it != zone }).take(GameSettings.RECENT_ZONES)
            copy(lastZones = lastZones + (heroId to zone), recentZones = recentZones + (heroId to recent))
        }
    }

    /** Забыть камеру карты, последнюю и недавние зоны стёртого героя [heroId]: его записи на устройстве больше никому не нужны. */
    suspend fun forgetHero(heroId: String) = editSettings { copy(worldViews = worldViews - heroId, lastZones = lastZones - heroId, recentZones = recentZones - heroId) }

    /**
     * Строка окна загрузки на этот показ из [keys]: не та, что в прошлый ([GameSettings.lastLaunchLine], прочитано с устройства), выбор
     * помнится; строк нет - null.
     */
    suspend fun launchLine(keys: List<String>): String? {
        var picked: String? = null
        editSettings {
            val next = LaunchLines.next(keys, lastLaunchLine)
            picked = next
            next?.let { copy(lastLaunchLine = it) } ?: this
        }
        return picked
    }
}
