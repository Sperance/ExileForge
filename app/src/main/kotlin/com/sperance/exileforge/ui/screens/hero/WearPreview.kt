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
import com.sperance.exileforge.core.market.wearable
import com.sperance.exileforge.core.sheet.WearBasis
import com.sperance.exileforge.core.sheet.WearSums
import com.sperance.exileforge.core.sheet.might
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.wearBasis
import com.sperance.exileforge.core.sheet.wearPlaces
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.LostBlock
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.PillTabs
import com.sperance.exileforge.ui.components.RelicLook
import com.sperance.exileforge.ui.components.StatComparison
import com.sperance.exileforge.ui.components.StatComparisonPending
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
 * строка на каждую сдвинутую характеристику, Мощь «было → стало» - в заголовке сравнения (только когда сдвиг стоит
 * показа, `MightShift.shown`). Недоступная вещь говорит вместо этого красным, чего ей не хватает.
 * Над итогом - вкладки мест ([WearChoice]): с чем из надетого сравнивать. У фляги итог - «ИТОГ ФЛЯГИ», у
 * инструмента - «ИТОГ РЕМЕСЛА» против инструмента его профессии. Все итоги - сравнение «было → стало»
 * ([StatComparison]), свёрнутое целиком под заголовком, в нём же - «Теряешь» ([WearSums.lost]): строки всего, что снимет надевание,
 * чьи характеристики у героя с этой вещью уходят в ноль.
 *
 * Фляга и инструмент листа героя не меняют: вместо итога героя - их вердикт против надетого в гнезде. Мощь - из того же листа
 * боя, что сдвиги.
 */
@Composable private fun WearTotals(game: GameUi, item: ItemInstance, choice: WearChoice, look: RelicLook) {
    val unmet = game.unmetFor(item)
    val selected = choice.selected
    val place = choice.target
    val sums = rememberWearSums(game, item, place, selected?.removed.orEmpty(), unmet.isEmpty())
    val lost = sums?.lost.orEmpty()
    if (unmet.isEmpty()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (choice.choosable && selected != null) {
                PillTabs(choice.places.map { placeTitle(it.slot) }, choice.places.indexOf(selected), { choice.pick(choice.places[it].slot) }, segmented = true)
                MutedText(if (selected.free) ui("wear.place_free") else ui("wear.instead", selected.removed.joinToString { equipmentTitle(it.template) }), style = MaterialTheme.typography.labelSmall)
            }
            val verdict = selected?.verdict
            val delta = sums?.comparison
            if (sums == null) {
                StatComparisonPending(ui(pendingTitle(game.index?.template(item.template)?.slot)), look = look)
            } else if (delta != null) {
                val might = game.hero?.might?.let { before -> sums.might?.let { MightShift(before.value, it.value) } }
                StatComparison(ui("relic.totals"), delta, look = look, lost = lost, might = might)
            } else if (verdict is FlaskVerdict) {
                StatComparison(ui("relic.flask_totals"), remember(verdict) { Comparison.of(verdict) }, look = look, lost = lost, might = verdict.might)
            } else if (verdict is ToolVerdict) {
                if (verdict.delta.isEmpty()) {
                    MutedText(ui("wear.tool_same"), style = MaterialTheme.typography.labelSmall)
                    LostBlock(lost, look.muted)
                } else {
                    StatComparison(ui("relic.tool_totals"), remember(verdict) { Comparison.of(verdict.delta, game.index) }, look = look, lost = lost)
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

/**
 * Итог «Если надеть» вещи [item] на место [place], снимающей [removed] ([WearSums]), сложенный вне главного потока; null - пока
 * считается или не нужен ([needed] - вещь по требованиям). Пересчёт - только с новой вещью, контентом, местом или основой примерки
 * героя ([WearBasis]): тот же герой новой ссылкой его не запускает.
 */
@Composable private fun rememberWearSums(game: GameUi, item: ItemInstance, place: Slot?, removed: List<ItemInstance>, needed: Boolean): WearSums? {
    val index = game.index
    val basis = remember(game.hero) { game.hero?.wearBasis }
    val apart = index?.template(item.template)?.slot?.let { it.isFlask || it.isTool } == true
    val sums by produceState<WearSums?>(null, item, index, place, removed, basis, needed) {
        value = null
        if (!needed || index == null || basis == null) return@produceState
        value = withContext(Dispatchers.Default) { if (apart) WearSums.apart(index, item, removed) else WearSums.worn(index, basis, item, place, removed) }
    }
    return sums
}

/** Ключ заголовка сравнения вещи слота [slot], пока оно считается: у фляги и инструмента - свой, как у готового итога. */
private fun pendingTitle(slot: Slot?): String = when {
    slot?.isFlask == true -> "relic.flask_totals"
    slot?.isTool == true -> "relic.tool_totals"
    else -> "relic.totals"
}

/** Имя места на теле для вкладки: своё у мест пары («Левая рука»), иначе имя слота. */
internal fun placeTitle(slot: Slot): String = uiOr(uiLanguage, "wear.place.${slot.name}", slotTitle(slot))

/** Итог героя для карточки [item] с выбором места [choice], или null, когда вещь не надевается: карта, самоцвет, уже надетая. */
fun wearTotals(game: GameUi, item: ItemInstance, choice: WearChoice): (@Composable (RelicLook) -> Unit)? = if (!game.wearable(item) || item.equipped || item.socketed) null else ({ look -> WearTotals(game, item, choice, look) })
