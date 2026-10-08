package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.character.FlaskVerdict
import com.sperance.exileforge.core.character.WearPlace
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.unmetFor
import com.sperance.exileforge.presentation.state.wearDelta
import com.sperance.exileforge.presentation.state.wearPlaces
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.FlaskTotals
import com.sperance.exileforge.ui.components.GearVerdictSummary
import com.sperance.exileforge.ui.components.HeroTotals
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.PillTabs
import com.sperance.exileforge.ui.components.RelicLook
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Whether an item goes on the body at all: a map, a tool and a jewel are placed elsewhere. */
fun wearable(game: GameUi, item: ItemInstance): Boolean = game.index?.template(item.template)?.slot?.let { !it.isJewelLike && !it.isTool } == true

/**
 * Выбор места для «Если надеть» (3.90.3): у вещи слота с несколькими местами (кольца - две руки, фляги) игрок выбирает,
 * с чем сравнивать, а «Надеть» надевает туда же. Пока игрок не выбрал - лучшее место ([WearPlace.best]).
 */
@Stable
class WearChoice internal constructor(val places: List<WearPlace>, private val picked: MutableState<Slot?>) {
    /** Место сравнения: выбранное игроком или лучшее. */
    val selected: WearPlace? get() = places.firstOrNull { it.slot == picked.value } ?: WearPlace.best(places)

    /** Мест больше одного - есть из чего выбирать. */
    val choosable: Boolean get() = places.size > 1

    /** Куда просить сервер надеть вещь: выбранное место, когда мест несколько; null - решает слот шаблона. */
    val target: Slot? get() = selected?.slot?.takeIf { choosable }

    fun pick(slot: Slot) {
        picked.value = slot
    }
}

/** Места [item] с вердиктами, сложенные вне главного потока, и выбор игрока среди них; новая вещь - выбор заново. */
@Composable fun rememberWearChoice(game: GameUi, item: ItemInstance): WearChoice {
    val places by produceState(emptyList<WearPlace>(), item, game.hero, game.index) { value = withContext(Dispatchers.Default) { game.wearPlaces(item) } }
    val picked = remember(item.id) { mutableStateOf<Slot?>(null) }
    return remember(places, picked) { WearChoice(places, picked) }
}

/**
 * «Если надеть» (2.46.0) в итоге героя карточки (3.88.6): что станет с листом с этой вещью, сложенное здесь формулой правил, -
 * строка на каждую сдвинутую характеристику, над ними - урон и защита двумя числами (3.89.0). Недоступная вещь говорит вместо этого красным, чего ей не хватает.
 * С 3.90.3 над итогом - вкладки мест ([WearChoice]): с чем из надетого сравнивать. У фляги (4.2.0) итог - «ИТОГ ФЛЯГИ» ([FlaskTotals]).
 */
@Composable private fun WearTotals(game: GameUi, item: ItemInstance, choice: WearChoice, look: RelicLook) {
    val unmet = game.unmetFor(item.template)
    val selected = choice.selected
    val place = choice.target
    // Фляга листа героя не меняет (4.2.0): вместо пустого итога героя - строки её листа против фляги в выбранном гнезде.
    val flask = game.index?.template(item.template)?.slot?.isFlask == true
    val delta = remember(item, game.hero, game.index, place) { if (flask) emptyList() else game.wearDelta(item, place) }
    if (unmet.isEmpty()) {
        // Итог в двух числах (3.89.0) над построчным «Если надеть».
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (choice.choosable && selected != null) {
                PillTabs(choice.places.map { placeTitle(it.slot) }, choice.places.indexOf(selected), { choice.pick(choice.places[it].slot) }, segmented = true)
                MutedText(selected.replaced?.let { ui("wear.instead", equipmentTitle(it.template)) } ?: ui("wear.place_free"), style = MaterialTheme.typography.labelSmall)
            }
            val verdict = selected?.verdict
            verdict?.let { GearVerdictSummary(it) }
            if (!flask) {
                HeroTotals(delta, look)
            } else if (verdict is FlaskVerdict) {
                FlaskTotals(verdict, look)
            }
        }
        return
    }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(ui("wear.blocked"), color = LifeRed, style = MaterialTheme.typography.labelLarge)
        unmet.forEach { Text(requirementReason(it, game.lang), color = LifeRed, style = MaterialTheme.typography.bodySmall) }
    }
}

/** Имя места на теле для вкладки: своё у мест пары («Левая рука»), иначе имя слота. */
private fun placeTitle(slot: Slot): String = uiOr(uiLanguage, "wear.place.${slot.name}", slotTitle(slot))

/** Итог героя для карточки [item] с выбором места [choice], или null, когда вещь не надевается: карта, самоцвет, уже надетая. */
fun wearTotals(game: GameUi, item: ItemInstance, choice: WearChoice): (@Composable (RelicLook) -> Unit)? = if (!wearable(game, item) || item.equipped || item.socketed) null else ({ look -> WearTotals(game, item, choice, look) })
