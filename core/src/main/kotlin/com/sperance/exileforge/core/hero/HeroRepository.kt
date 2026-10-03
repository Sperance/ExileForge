package com.sperance.exileforge.core.hero

import com.sperance.exileforge.core.model.hero.ChestOpening
import com.sperance.exileforge.core.model.hero.HeroView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/**
 * Герой на экране: его id, как его зовут все маршруты героя; сам герой, каким сервер ответил последним; чей он
 * ([owner]); когда прочитан целиком ([readAt], 0 - «никогда или заведомо устарел») и когда виден ([seenAt]);
 * предмет под кузницей ([selectedEquipment]) и её последняя фраза; вскрытый сундук, пока его не закрыли.
 */
data class HeroHolding(
    val heroId: String = "",
    val hero: HeroView? = null,
    val owner: String = "",
    val readAt: Long = 0,
    val seenAt: Long = 0,
    val selectedEquipment: String = "",
    val forgeLine: String = "",
    val chest: ChestOpening? = null,
) {
    /** Чтение ещё свежее: герой есть и прочитан не раньше [within] миллисекунд назад. */
    fun fresh(now: Long, within: Long): Boolean = hero != null && now - readAt < within
}

/**
 * Единственный источник правды о герое на экране (3.80.10). Снимки сливает и рисует `HeroSync`; сюда ложится
 * готовый [HeroView], а кошелёк и сумку правят команды, что их меняют.
 */
class HeroRepository {
    private val mutable = MutableStateFlow(HeroHolding())
    val state: StateFlow<HeroHolding> = mutable

    /** Id героя на экране без пробелов; пусто - герой не выбран. */
    val heroId: String get() = mutable.value.heroId.trim()

    /** Ответ про другого героя не рисуется. */
    fun onScreen(id: String): Boolean = id == heroId

    /** Сколько раз герой был нарисован заново; команда сравнивает до и после, чтобы знать, принёс ли ответ снимок. */
    var version: Long = 0
        private set

    /** Новый герой входит в игру: прежний забыт. */
    fun select(heroId: String) = mutable.update { HeroHolding(heroId = heroId) }

    /** Герой нарисован заново в [now]: предмет под кузницей остаётся, если он ещё в сундуке, иначе - первый. */
    fun set(hero: HeroView, now: Long) {
        version++
        mutable.update { h ->
            h.copy(
                hero = hero,
                owner = hero.info.userId,
                readAt = now,
                seenAt = now,
                selectedEquipment = h.selectedEquipment.takeIf { chosen -> hero.items.any { it.id == chosen } } ?: hero.items.firstOrNull()?.id.orEmpty(),
            )
        }
    }

    /** Сервер ответил 304: герой тот же, чтение свежее. */
    fun seen(now: Long) = mutable.update { it.copy(readAt = now, seenAt = now) }

    /** Чтение заведомо устарело: ближайший `ensure` перечитает. */
    fun stale() = mutable.update { it.copy(readAt = 0) }

    fun selectEquipment(itemId: String) = mutable.update { it.copy(selectedEquipment = itemId, forgeLine = if (itemId == it.selectedEquipment) it.forgeLine else "") }

    fun forgeLine(text: String) = mutable.update { it.copy(forgeLine = text) }

    fun chest(opening: ChestOpening?) = mutable.update { it.copy(chest = opening) }

    fun patch(transform: (HeroView) -> HeroView) = mutable.update { h -> h.copy(hero = h.hero?.let(transform)) }

    /** Кошелёк, каким его назвал ответ сервера на команду с деньгами. */
    fun money(money: Long) = patch { it.copy(info = it.info.copy(money = money)) }

    fun clear() = mutable.update { HeroHolding() }
}
