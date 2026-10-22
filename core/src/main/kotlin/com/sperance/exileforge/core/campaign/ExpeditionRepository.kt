package com.sperance.exileforge.core.campaign

import com.sperance.exileforge.core.quests.QuestTurnIn
import com.sperance.exileforge.core.quests.Quests
import com.sperance.exileforge.rules.content.MapCode
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.rules.run.MapMechanic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Зона, в которую вот-вот войдут: карта из сундука для неё (null - без карты), зелье и скарабеи к карте. */
data class MapLaunch(val mapCode: MapCode, val picked: String? = null, val potion: String? = null, val scarabs: List<String> = emptyList())

/** Вещь, принесённая походом, и миг, когда она легла. */
data class LootEntry(val item: ItemInstance, val at: Long)

/** Окно атласа: узел под курсором; окно закрыто, пока состояния нет. */
data class AtlasWindow(val selected: String = "")

/** Заход, открытый на сервере без похода на экране: приложение закрылось посреди него - его предлагают продолжить или покинуть. */
data class UnfinishedRun(val runId: String, val zone: MapCode)

/**
 * Поход на экране: карточка зоны перед входом, добыча похода для листа снаряжения, окно атласа и счётчики
 * журнала - события, которых сервер ещё не взял, и отвергнутые, для значка. [saleMarks] - id вещей добычи, помеченных
 * к продаже посреди захода: в итогах карты они уже отмечены, хранятся в журнале захода на устройстве и переживают перезапуск. [forced] - механики, что тестировщик ставит на новые заходы
 * насильно: выбор держится между зонами. Заход на экране: [quests] - доска заданий, снятая на входе (лист «Задания»
 * карты считает от неё и счёта захода - перечитанная доска уже несёт его события), [turnedIn] - задания, сданные после карты,
 * [storyReady] - после карты шаг сюжета выполнен и ждёт ручной сдачи,
 * [atlasPoints] - ключи очков Атласа, что принесли ответы захода или испытания, [potion] - зелье, выпитое на входе. [wearable] - id вещей,
 * что в заходе на экране можно надеть, по счёту сервера на входе и ответам журнала: набор, в котором герой вошёл, и находки захода.
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
    val quests: Quests? = null,
    val turnedIn: List<QuestTurnIn> = emptyList(),
    val storyReady: Boolean = false,
    val atlasPoints: List<String> = emptyList(),
    val potion: String? = null,
    val wearable: Set<String> = emptySet(),
)

/** Единственный источник правды о походе на экране; сам бег похода - у `ExpeditionActions`. */
class ExpeditionRepository {
    private val mutable = MutableStateFlow(Expedition())
    val state: StateFlow<Expedition> = mutable

    fun update(transform: (Expedition) -> Expedition) = mutable.update(transform)

    fun clear() = update { Expedition() }
}
