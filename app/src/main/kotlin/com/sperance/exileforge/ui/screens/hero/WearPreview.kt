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
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.character.FlaskVerdict
import com.sperance.exileforge.core.character.MightShift
import com.sperance.exileforge.core.character.ToolVerdict
import com.sperance.exileforge.core.character.WearPlace
import com.sperance.exileforge.core.display.Comparison
import com.sperance.exileforge.core.display.equipmentTitle
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.display.slotTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.i18n.uiOr
import com.sperance.exileforge.core.sheet.figures
import com.sperance.exileforge.core.sheet.might
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.wearPlaces
import com.sperance.exileforge.core.sheet.wearing
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.MightComparison
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.PillTabs
import com.sperance.exileforge.ui.components.RelicLook
import com.sperance.exileforge.ui.components.StatComparison
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Whether an item goes on the hero at all: a map and a jewel are placed elsewhere; a tool - in its profession's slot. */
fun wearable(game: GameUi, item: ItemInstance): Boolean = game.index?.template(item.template)?.slot?.let { !it.isJewelLike } == true

/**
 * Выбор места для «Если надеть»: у вещи слота с несколькими местами (кольца - две руки, фляги) игрок выбирает,
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
 * «Если надеть» в итоге героя карточки: что станет с листом с этой вещью, сложенное здесь формулой правил, -
 * строка на каждую сдвинутую характеристику, под ними - одна строка Мощи «было → стало» (только когда сдвиг стоит
 * показа, `MightShift.shown`). Недоступная вещь говорит вместо этого красным, чего ей не хватает.
 * Над итогом - вкладки мест ([WearChoice]): с чем из надетого сравнивать. У фляги итог - «ИТОГ ФЛЯГИ», у
 * инструмента - «ИТОГ РЕМЕСЛА» против инструмента его профессии. Все итоги - сравнение «было → стало»
 * ([StatComparison]), свёрнутое до важнейших строк.
 *
 * Фляга и инструмент листа героя не меняют: вместо итога героя - их вердикт против надетого в гнезде. Мощь - одной строкой «было → стало»
 * из того же листа боя, что сдвиги.
 */
@Composable private fun WearTotals(game: GameUi, item: ItemInstance, choice: WearChoice, look: RelicLook) {
    val unmet = game.unmetFor(item.template)
    val selected = choice.selected
    val place = choice.target
    val apart = game.index?.template(item.template)?.slot?.let { it.isFlask || it.isTool } == true
    val outcome = remember(item, game.hero, game.index, place) { if (apart) null else game.wearing(item, place) }
    val delta = remember(outcome) { outcome?.let { Comparison.of(it.delta, game.index, sheet = game.hero?.figures?.values, level = game.hero?.level ?: 0) } }
    val might = game.hero?.might?.let { before -> outcome?.might?.let { MightShift(before.value, it.value) } }
    if (unmet.isEmpty()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (choice.choosable && selected != null) {
                PillTabs(choice.places.map { placeTitle(it.slot) }, choice.places.indexOf(selected), { choice.pick(choice.places[it].slot) }, segmented = true)
                MutedText(selected.replaced?.let { ui("wear.instead", equipmentTitle(it.template)) } ?: ui("wear.place_free"), style = MaterialTheme.typography.labelSmall)
            }
            val verdict = selected?.verdict
            if (delta != null) {
                StatComparison(ui("relic.totals"), delta, look = look)
                might?.let { MightComparison(it, look) }
            } else if (verdict is FlaskVerdict) {
                StatComparison(ui("relic.flask_totals"), remember(verdict) { Comparison.of(verdict) }, look = look)
                verdict.might?.let { MightComparison(it, look) }
            } else if (verdict is ToolVerdict) {
                if (verdict.delta.isEmpty()) {
                    MutedText(ui("wear.tool_same"), style = MaterialTheme.typography.labelSmall)
                } else {
                    StatComparison(ui("relic.tool_totals"), remember(verdict) { Comparison.of(verdict.delta, game.index) }, look = look)
                }
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
