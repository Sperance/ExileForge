package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.run.GearLock
import com.sperance.exileforge.core.campaign.run.GearPreview
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.unmetFor
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.InspectAction
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.rememberGearVerdict
import com.sperance.exileforge.ui.theme.Boon
import com.sperance.exileforge.ui.theme.Vital
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/** Метка строки лута в походе: [text] напротив имени и рамка строки её цвета [color]. */
internal class LootMark(val text: String, val color: Color)

/**
 * Метка строки лута: фляга, что герой может надеть, - «Новая фляга · 0 зар.» (только с [flasks] - пока набор в походе ещё меняется),
 * прочая вещь лучше надетого ([com.sperance.exileforge.core.character.GearVerdict.better]) - «▲ Лучше надетого»; иначе null. Вердикт - с
 * копией героя, когда она уже у него.
 */
@Composable internal fun rememberLootMark(game: GameUi, item: ItemInstance, flasks: Boolean): LootMark? {
    val verdict = rememberGearVerdict(game, game.hero?.item(item.id) ?: item) ?: return null
    val flask = game.index?.template(item.template)?.slot?.isFlask == true
    return when {
        flask -> LootMark(ui("expedition.new_flask"), Boon).takeIf { flasks }
        verdict.better -> LootMark(ui("expedition.better_worn"), Vital)
        else -> null
    }
}

/** Смена набора в походе на экране: [lock] - почему сейчас нельзя (null - можно), [found] - id находок захода, их и надевают. */
internal class RunWearing(val lock: GearLock?, val found: Set<String>)

/** Смена набора в походе по карте на экране; null - похода нет (испытание, Разлом, убежище). */
@Composable internal fun rememberRunWearing(vm: ExpeditionViewModel): RunWearing? {
    val run by vm.run.collectAsStateWithLifecycle()
    val found = vm.state.collectAsStateWithLifecycle().value.found
    val shown = run ?: return null
    val lock by remember(shown) { shown.hud.map { it.gearLock }.distinctUntilChanged() }.collectAsStateWithLifecycle(shown.hud.value.gearLock)
    return RunWearing(lock, found)
}

/**
 * «Надеть» вещи [item] в походе по карте - на место [place] (null - решают правила; место вкладок карточки главнее): погашено с причиной,
 * если вещь не из этого захода, набор сейчас не сменить ([GearLock]: в бою кнопка - «Надеть после боя») или это вторая копия уникалки;
 * не по требованиям - погашено, требования говорит карточка. Над кнопкой - что изменится ([RunWearNotes]). [then] - перед командой
 * (место закрывает свой лист).
 */
internal fun runWearAction(game: GameUi, vm: ExpeditionViewModel, item: ItemInstance, place: Slot?, wearing: RunWearing, then: () -> Unit = {}): InspectAction {
    val twin = game.index?.let { game.hero?.uniqueWorn(it, item, place) } == true
    val reason = when {
        item.id !in wearing.found -> ui("expedition.wear_stash")
        wearing.lock != null -> ui(wearing.lock.key)
        twin -> ui("hero.unique_worn")
        else -> null
    }
    val can = reason == null && game.unmetFor(item.template).isEmpty() && !game.busy && (game.ownsCharacter || game.isAdmin)
    val notes: (@Composable ColumnScope.() -> Unit)? = if (reason == null) ({ RunWearNotes(game, vm, item.id, place) }) else null
    return InspectAction(ui(if (wearing.lock == GearLock.FIGHT) "expedition.wear_after_fight" else "hero.equip"), enabled = can, reason = reason, notes = notes) {
        then()
        vm.wear(item.id, wearInto ?: place)
    }
}

/**
 * Что изменит надетая в походе находка [itemId] на месте [place]: действует со следующего боя; здоровье сохранит число под новым
 * максимумом; добыча следующих боёв и неоткрытых сундуков - по новому набору (сдвиги прибавок); новая фляга встаёт пустой, а снятая
 * уносит свои заряды. Считается вне главного потока; до ответа - только первая строка.
 */
@Composable internal fun ColumnScope.RunWearNotes(game: GameUi, vm: ExpeditionViewModel, itemId: String, place: Slot?) {
    val preview by produceState<GearPreview?>(null, itemId, place, game.hero) { value = withContext(Dispatchers.Default) { vm.wearPreview(itemId, place) } }
    Text(ui("expedition.wear_found"), color = Vital, style = MaterialTheme.typography.labelMedium)
    MutedText(ui("expedition.wear_next_fight"))
    val shown = preview ?: return
    val before = shown.maxLife.roundToInt()
    val after = shown.newMaxLife.roundToInt()
    if (after != before) {
        val life = shown.life.roundToInt()
        MutedText(ui("expedition.wear_life", number(life.toDouble()), number(before.toDouble()), number(min(life, after).toDouble()), number(after.toDouble())))
    }
    if (shown.loot.isNotEmpty()) {
        val shifts = shown.loot.joinToString(", ") { ui("expedition.wear_loot_line", ui("expedition.wear_loot.${it.kind.name.lowercase()}"), it.before.roundToInt(), it.after.roundToInt()) }
        Text(ui("expedition.wear_loot", shifts), color = Boon, style = MaterialTheme.typography.bodySmall)
    }
    shown.flask?.let { flask ->
        Text(ui("expedition.wear_flask", flask.maxCharges), color = Boon, style = MaterialTheme.typography.bodySmall)
        flask.replaced?.let { id -> game.hero?.item(id)?.let(game::view) }?.let { old -> MutedText(ui("expedition.wear_flask_replace", flask.slot + 1, old.title, flask.lost)) }
    }
}
