package com.sperance.exileforge.core.hero

import com.sperance.exileforge.core.model.hero.HeroView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** Герой на экране: его id, как его зовут все маршруты героя, и сам герой, каким сервер ответил последним. */
data class HeroHolding(val heroId: String = "", val hero: HeroView? = null)

/**
 * Единственный источник правды о герое на экране (3.80.10). Снимки, слияние частей и расчёт листа пока
 * в `HeroViewModel`; сюда ложится готовый [HeroView], а кошелёк и сумку правят команды, что их меняют.
 */
class HeroRepository {
    private val mutable = MutableStateFlow(HeroHolding())
    val state: StateFlow<HeroHolding> = mutable

    /** Id героя на экране без пробелов; пусто - герой не выбран. */
    val heroId: String get() = mutable.value.heroId.trim()

    /** Ответ про другого героя не рисуется. */
    fun onScreen(id: String): Boolean = id == heroId

    /** Новый герой входит в игру: прежний забыт. */
    fun select(heroId: String) = mutable.update { HeroHolding(heroId = heroId) }

    /** Сколько раз герой был нарисован заново; команда сравнивает до и после, чтобы знать, принёс ли ответ снимок. */
    var version: Long = 0
        private set

    fun set(hero: HeroView) {
        version++
        mutable.update { it.copy(hero = hero) }
    }

    fun patch(transform: (HeroView) -> HeroView) = mutable.update { h -> h.copy(hero = h.hero?.let(transform)) }

    /** Кошелёк, каким его назвал ответ сервера на команду с деньгами. */
    fun money(money: Long) = patch { it.copy(info = it.info.copy(money = money)) }

    fun clear() = mutable.update { HeroHolding() }
}
