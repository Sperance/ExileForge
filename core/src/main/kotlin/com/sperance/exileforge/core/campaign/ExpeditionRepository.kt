package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.run.MapMechanic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Зона, в которую вот-вот войдут: карта из сундука для неё (null - без карты), зелье (3.79.0) и скарабеи к карте. */
data class MapLaunch(val mapCode: MapCode, val picked: String? = null, val potion: String? = null, val scarabs: List<String> = emptyList())

/** Вещь, принесённая походом, и миг, когда она легла. */
data class LootEntry(val item: ItemInstance, val at: Long)

/** Окно атласа: узел под курсором; окно закрыто, пока состояния нет. */
data class AtlasWindow(val selected: String = "")

/** Заход, открытый на сервере без похода на экране (3.89.0): приложение закрылось посреди него - его предлагают продолжить или покинуть. */
data class UnfinishedRun(val runId: String, val zone: MapCode)

/**
 * Поход на экране (3.80.20): карточка зоны перед входом, добыча похода для листа снаряжения, окно атласа и счётчики
 * журнала - события, которых сервер ещё не взял, и отвергнутые, для значка. [saleMarks] (3.91.0) - id вещей добычи, помеченных
 * к продаже посреди захода: в итогах карты они уже отмечены. [forced] (4.0.1) - механики, что тестировщик ставит на новые заходы
 * насильно: выбор держится между зонами.
 */
data class Expedition(
    val launch: MapLaunch? = null,
    val runLoot: List<LootEntry> = emptyList(),
    val atlas: AtlasWindow? = null,
    val pending: Int = 0,
    val rejected: Int = 0,
    val unfinished: UnfinishedRun? = null,
    val saleMarks: Set<String> = emptySet(),
    val forced: Set<MapMechanic> = emptySet(),
)

/** Единственный источник правды о походе на экране (3.80.20); сам бег похода - у `ExpeditionActions`. */
class ExpeditionRepository {
    private val mutable = MutableStateFlow(Expedition())
    val state: StateFlow<Expedition> = mutable

    fun update(transform: (Expedition) -> Expedition) = mutable.update(transform)

    fun clear() = update { Expedition() }
}
