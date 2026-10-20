package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.campaign.run.GearLock
import com.sperance.exileforge.core.campaign.run.GearPreview
import com.sperance.exileforge.core.character.GearVerdict
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
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.math.roundToInt

/** Метка строки лута в походе: [text] напротив имени и рамка строки её цвета [color]. */
internal class LootMark(val text: String, val color: Color)

/**
 * Метка строки лута по уже посчитанному вердикту [verdict] вещи [item]: фляга, что герой может надеть, - «Новая фляга · 0 зар.» (только с
 * [flasks] - пока набор в походе ещё меняется), прочая вещь лучше надетого ([GearVerdict.better]) - «▲ Лучше надетого»; иначе null.
 */
internal fun lootMark(game: GameUi, item: ItemInstance, verdict: GearVerdict?, flasks: Boolean): LootMark? {
    verdict ?: return null
    val flask = game.index?.template(item.template)?.slot?.isFlask == true
    return when {
        flask -> LootMark(ui("expedition.new_flask"), Boon).takeIf { flasks }
        verdict.better -> LootMark(ui("expedition.better_worn"), Vital)
        else -> null
    }
}

/** Вердикт вещи [item] строки лута и её метка ([lootMark]): сравнение - с копией героя, когда она уже у него. */
@Composable internal fun rememberLootVerdict(game: GameUi, item: ItemInstance, flasks: Boolean): Pair<GearVerdict?, LootMark?> {
    val verdict = rememberGearVerdict(game, game.hero?.item(item.id) ?: item)
    return verdict to lootMark(game, item, verdict, flasks)
}

/**
 * Срез игры [game] с героем, как его одел поход по карте ([ExpeditionViewModel.runHero]): вердикты, метки «Лучше надетого», требования и
 * карточки в походе сравнивают с набором похода, а не с набором на сервере; вне похода - сам [game].
 */
@Composable internal fun rememberRunGame(game: GameUi, vm: ExpeditionViewModel): GameUi {
    val dressed by vm.runHero.collectAsStateWithLifecycle()
    return remember(game, dressed) { dressed?.takeIf { it !== game.hero && it.id == game.hero?.id }?.let { game.copy(holding = game.holding.copy(hero = it)) } ?: game }
}

/** Смена набора в походе на экране: [lock] - почему сейчас нельзя (null - можно), [wearable] - id вещей, что в заходе можно надеть. */
internal class RunWearing(val lock: GearLock?, val wearable: Set<String>)

/** Смена набора в походе по карте на экране; null - похода нет (испытание, Разлом, убежище). */
@Composable internal fun rememberRunWearing(vm: ExpeditionViewModel): RunWearing? {
    val run by vm.run.collectAsStateWithLifecycle()
    val wearable = vm.state.collectAsStateWithLifecycle().value.wearable
    val shown = run ?: return null
    val hud = shown.hud.collectAsStateWithLifecycle()
    val lock by remember(hud) { derivedStateOf { hud.value.gearLock } }
    return RunWearing(lock, wearable)
}

/**
 * «Надеть» вещи [item] в походе по карте - на место [place] (null - решают правила; место вкладок карточки главнее): погашено с причиной,
 * если вещь не из набора входа и не находка захода ([RunWearing.wearable]), набор сейчас не сменить ([GearLock]: в бою кнопка - «Надеть после боя») или это вторая копия уникалки;
 * не по требованиям - погашено, требования говорит карточка. Над кнопкой - что изменится ([RunWearNotes]). [then] - перед командой
 * (место закрывает свой лист).
 */
internal fun runWearAction(game: GameUi, vm: ExpeditionViewModel, item: ItemInstance, place: Slot?, wearing: RunWearing, then: () -> Unit = {}): InspectAction {
    val twin = game.index?.let { game.hero?.uniqueWorn(it, item, place) } == true
    val reason = when {
        item.id !in wearing.wearable -> ui("expedition.wear_stash")
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
 * Что изменит надетая в походе вещь [itemId] на месте [place]: действует со следующего боя; здоровье сохранит число под новым
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
        flask.replaced?.let { id -> game.hero?.item(id)?.let { game.view(it) } }?.let { old -> MutedText(ui("expedition.wear_flask_replace", flask.slot + 1, old.title, flask.lost)) }
    }
}
