package com.sperance.exileforge.core.campaign

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Симуляция, что шагает кадрами экрана: заход карты, испытание, бой Разлома. [awake] - нужен ли ей кадр сейчас: бой или заход идут,
 * ждёт команда или новость сервера; на паузе, под листом, в лобби до боя и в итогах она спит, и часы экрана кадров не берут.
 */
interface Simulation {
    val awake: StateFlow<Boolean>

    /** Шаг на [dt] секунд экрана. */
    fun update(dt: Double)
}

/**
 * Будильник симуляции: ввод извне ([poke] - команда, ответ сервера) будит её до ближайшего шага; шаг берёт отметку ввода ([took]) и
 * решает, нужен ли следующий кадр ([rest]). Ввод между отметкой и решением не теряется - симуляция остаётся бодрой.
 */
class SimWake {
    private val mutable = MutableStateFlow(true)
    val awake: StateFlow<Boolean> = mutable.asStateFlow()

    @Volatile private var poked = true

    fun poke() {
        poked = true
        mutable.value = true
    }

    /** Был ли ввод извне с прошлого шага; отметка снимается. */
    fun took(): Boolean = poked.also { poked = false }

    /** Шаг сделан: кадр нужен, пока [active] - бой или заход идут - или с отметки пришёл новый ввод. */
    fun rest(active: Boolean) {
        mutable.value = active || poked
    }
}
