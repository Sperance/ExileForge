package com.sperance.exileforge.ui.screens.tree

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.character.GearVerdict
import com.sperance.exileforge.core.character.MightShift
import com.sperance.exileforge.core.display.Comparison
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.requirementReason
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.sheet.figures
import com.sperance.exileforge.core.sheet.might
import com.sperance.exileforge.core.sheet.socketing
import com.sperance.exileforge.core.sheet.view
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.TreeNode
import com.sperance.exileforge.rules.roll.ItemInstance
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ComparisonDigest
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.Hold
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.ItemRow
import com.sperance.exileforge.ui.components.LineFold
import com.sperance.exileforge.ui.components.MightShiftBadge
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.SheetBody
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Seam
import com.sperance.exileforge.ui.theme.Vital
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Самоцвет-кандидат в гнездо: копия [item] и её вид [view]; [free] - правила пустят её сюда (уникальный самоцвет - один на
 * героя). [might] - Мощь героя сейчас и с этим самоцветом в гнезде, [comparison] - сводка героя «было → станет» при той же замене;
 * у самоцвета, что не встанет, сравнения нет.
 */
@Immutable
internal class JewelOffer(val item: ItemInstance, val view: ItemView, val free: Boolean, val might: MightShift?, val comparison: Comparison) {
    /** Замена ослабит героя: Мощь с ней ниже нынешней сверх шума. */
    val worse: Boolean get() = might?.shift == GearVerdict.Shift.DOWN
}

/**
 * Кандидаты в гнездо: самоцветы тайника - не надетые и не вставленные. Каждый мерится листом героя с ним в этом гнезде - тот же
 * расчёт правил, что даёт сводку и Мощь ([socketing]): гнездо, условия и вторая копия уникального учтены листом, а не здесь.
 */
internal object JewelOffers {
    /** Копия [item] - самоцвет в тайнике, который можно предложить гнезду. */
    fun candidate(index: ContentIndex, item: ItemInstance): Boolean = !item.equipped && !item.socketed && index.template(item.template)?.slot == Slot.JEWEL

    /**
     * Кандидаты в гнездо [socket] героя [hero], каким его захватил вызов: сперва те, что встанут, по Мощи героя с ними - сильнейший
     * первым, ослабляющие героя - последними; уникальный, что уже стоит в другом гнезде, - в самом конце. Считать вне главного потока.
     */
    fun of(hero: HeroView, index: ContentIndex, socket: String): List<JewelOffer> {
        val now = hero.might?.value
        return hero.items.filter { candidate(index, it) }.mapNotNull { item ->
            val view = ItemView.of(item, index) ?: return@mapNotNull null
            if (!hero.jewelFree(index, item, socket)) return@mapNotNull JewelOffer(item, view, false, null, Comparison(emptyList()))
            val outcome = hero.socketing(index, item, socket)
            val might = now?.let { before -> outcome.might?.let { MightShift(before, it.value) } }
            JewelOffer(item, view, true, might, Comparison.of(outcome.delta, index, sheet = hero.figures.values, level = hero.level))
        }.sortedWith(compareByDescending<JewelOffer> { it.free }.thenByDescending { it.might?.after ?: 0.0 })
    }
}

/**
 * Гнездо самоцвета в панели узла: что в нём стоит и что можно поставить.
 *
 * Самоцвет - обычная вещь, кандидаты - самоцветы тайника ([JewelOffers]). Встанет ли - решает сервер: гнездо должно быть взято,
 * уникальный - не второй такой же; лист предлагает, отказ объясняет.
 *
 * Пустое гнездо - пунктирная лунка с подсказкой: само гнездо бонусов не даёт, имя - тема, а не бонус; ниже - сколько самоцветов
 * свободно и сдвиг Мощи лучшего, «Вставить самоцвет». Занятое - самоцвет строкой тайника с тремя строками и «ещё N», справа его
 * статус: работает, бездействует (вторая копия уникального - причина правил) или ждёт, пока гнездо не взято. «Вытащить» -
 * удержанием, «Заменить» и «Вставить» открывают лист выбора ([JewelPicker]); прежний самоцвет уходит в тайник.
 *
 * Сравнение кандидатов считается вне главного потока один раз на входы расчёта - вещи, дерево, уровень, класс, умения, законы и дар
 * героя, - контент и гнездо, и только когда оно на экране; сменились входы - прежний список гаснет до нового.
 */
@Composable internal fun SocketContents(
    game: GameUi,
    index: ContentIndex,
    node: TreeNode,
    allocated: Boolean,
    enabled: Boolean,
    onSocket: (String, String) -> Unit,
    onUnsocket: (String) -> Unit,
) {
    val hero = game.hero ?: return
    val inside = hero.jewels[node.code]
    val inspect = rememberInspect()
    val free = remember(hero, index) { hero.items.count { JewelOffers.candidate(index, it) } }
    var picking by remember(node.code) { mutableStateOf(false) }
    val measured = allocated && free > 0 && (inside == null || picking)
    val offers by produceState<List<JewelOffer>?>(null, hero.items, hero.tree, hero.level, hero.heroClass, hero.skills, hero.info.laws, hero.info.fate, index, node.code, measured) {
        value = null
        if (measured) value = withContext(Dispatchers.Default) { JewelOffers.of(hero, index, node.code) }
    }

    val held = inside?.let(game::view)
    if (inside == null) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EmptySocket()
            MutedText(ui("tree.socket_hint"), Modifier.weight(1f))
        }
        when {
            !allocated -> MutedText(ui("tree.socket_first"))

            free == 0 -> MutedText(ui("tree.no_jewels"))

            else -> {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val best = offers?.firstOrNull { it.free }?.might?.takeIf { it.shift == GearVerdict.Shift.UP }
                    MutedText(ui(if (best != null) "tree.jewels_free_best" else "tree.jewels_free", free))
                    best?.let { MightShiftBadge(it) }
                }
                ForgeButton(onClick = { picking = true }, modifier = Modifier.fillMaxWidth(), state = ButtonState.of(enabled)) { Text(ui("tree.jewel_in")) }
            }
        }
    } else if (held != null) {
        val idle = hero.inactive[inside.id]?.joinToString("\n") { requirementReason(it, game.lang) }
        ItemRow(
            held,
            note = when {
                !allocated -> ui("tree.socket_locked")
                idle != null -> idle
                else -> ui("tree.socket_working")
            },
            noteColor = when {
                !allocated -> Muted
                idle != null -> LifeRed
                else -> Vital
            },
            worn = true,
            lines = LineFold.SHORT,
        ) { inspect(Inspect.Copy(inside)) }
        if (!allocated) MutedText(ui("tree.socket_waiting"))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ForgeButton(
                onClick = { onUnsocket(inside.id) },
                modifier = Modifier.weight(1f),
                kind = ButtonKind.Secondary,
                state = ButtonState.of(enabled),
                hold = Hold(),
            ) { Text(ui("tree.jewel_out")) }
            if (allocated && free > 0) {
                ForgeButton(onClick = { picking = true }, modifier = Modifier.weight(1f), kind = ButtonKind.Secondary, state = ButtonState.of(enabled)) { Text(ui("tree.jewel_replace")) }
            }
        }
    }

    if (picking) {
        JewelPicker(held, offers, enabled, onDismiss = { picking = false }) { id ->
            picking = false
            onSocket(id, node.code)
        }
    }
}

/** Пустая лунка гнезда: пунктирный круг размера самоцвета строки. */
@Composable private fun EmptySocket() {
    Canvas(Modifier.size(44.dp)) {
        val width = 1.5.dp.toPx()
        drawCircle(Seam, radius = size.minDimension / 2 - width, style = Stroke(width, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx()))))
    }
}

/**
 * Лист выбора самоцвета в гнездо: у каждого кандидата сдвиг Мощи героя и до трёх строк сводки «было → станет» при замене именно в
 * этом гнезде; сильнейший сверху, ослабляющий героя - с красной отметкой в конце. Касание выделяет (сразу выделен лучший),
 * удержание открывает карточку вещи, подтверждение - кнопка «Поставить: …» внизу. [inside] - самоцвет, что уйдёт в тайник;
 * [offers] - null, пока сравнение считается.
 */
@Composable private fun JewelPicker(inside: ItemView?, offers: List<JewelOffer>?, enabled: Boolean, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val inspect = rememberInspect()
    var picked by remember { mutableStateOf<String?>(null) }
    val choice = offers?.let { all -> all.firstOrNull { it.free && it.item.id == picked } ?: all.firstOrNull { it.free } }
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(spacing = 8.dp),
        header = {
            Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(ui(if (inside == null) "tree.jewel_in" else "tree.jewel_swap"), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                MutedText(listOfNotNull(inside?.let { ui("tree.jewel_swap_out", it.title) }, ui("tree.jewel_by_might")).joinToString(" · "))
                MutedText(ui("tree.jewel_hold_hint"))
            }
        },
        footer = {
            ForgeButton(onClick = { choice?.let { onPick(it.item.id) } }, modifier = Modifier.weight(1f), state = ButtonState.of(enabled && choice != null)) {
                PutLabel(choice?.let { ui("tree.jewel_put", it.view.title) } ?: ui("tree.jewel_swap"))
            }
        },
    ) {
        if (offers == null) MutedText(ui("common.loading"))
        offers?.forEach { offer -> OfferRow(offer, selected = offer === choice, onSelect = { picked = offer.item.id }, onInspect = { inspect(Inspect.Copy(offer.item)) }) }
    }
}

/** Подпись кнопки «Поставить: …» в одну строку: длинное имя обрезается. */
@Composable private fun RowScope.PutLabel(label: String) {
    Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
}

/**
 * Кандидат листа выбора: самоцвет строкой тайника со свёрнутыми строками, напротив имени - сдвиг Мощи; под ним - до трёх строк
 * сравнения. Ослабляющий героя - красной отметкой, уникальный из другого гнезда - причиной и без выбора.
 */
@Composable private fun OfferRow(offer: JewelOffer, selected: Boolean, onSelect: () -> Unit, onInspect: () -> Unit) {
    ItemRow(
        offer.view,
        note = when {
            !offer.free -> ui("tree.jewel_unique_taken")
            offer.worse -> ui("tree.jewel_worse")
            else -> null
        },
        noteColor = LifeRed,
        selected = selected,
        trailing = if (offer.might != null) {
            { MightShiftBadge(offer.might) }
        } else {
            null
        },
        lines = LineFold.FOLDED,
        footer = { ComparisonDigest(offer.comparison) },
        onLongClick = onInspect,
    ) { if (offer.free) onSelect() }
}
