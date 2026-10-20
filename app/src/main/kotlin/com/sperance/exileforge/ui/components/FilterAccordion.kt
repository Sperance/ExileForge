package com.sperance.exileforge.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.rarityTitle
import com.sperance.exileforge.core.filter.LevelRange
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.theme.*

/** Строка шторки-аккордеона: имя группы, её текущий выбор словами и что раскрывается под ней по нажатию. */
class AccordionGroup(val key: String, val title: String, val value: String, val content: @Composable () -> Unit)

/** Переключатель под группами шторки: «Только сделанное мной», «Скрыть надетое» и подобные. */
class FilterSwitch(val title: String, val checked: Boolean, val onChange: (Boolean) -> Unit)

/**
 * Шторка фильтров «Аккордеон» - одна на все списки приложения: под именем - [header] (вилка уровня, [LevelRangeHeader]), каждая
 * группа - строка с текущим выбором, что раскрывается по нажатию (открыта одна), ниже переключатели, внизу «Сбросить» и
 * «Показать N» ([show]). Поиска в шторке нет - он в строке поиска экрана ([SearchBar]). Что выбрано - решает список: шторка лишь
 * рисует [groups] и [switches].
 *
 * Контролы шторки плотные: видимая высота фишек 28dp, строк и полей 40dp, зона касания - не ниже [FilterTouch].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterAccordionSheet(
    groups: List<AccordionGroup>,
    switches: List<FilterSwitch>,
    show: String,
    resettable: Boolean,
    onReset: () -> Unit,
    onShow: () -> Unit,
    onDismiss: () -> Unit,
    header: (@Composable () -> Unit)? = null,
) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(spacing = 8.dp),
        footer = {
            ForgeButton(onClick = onReset, state = ButtonState.of(resettable), modifier = Modifier.weight(1f), kind = ButtonKind.Secondary) { Text(ui("filter.reset")) }
            ForgeButton(onClick = onShow, modifier = Modifier.weight(1f)) { Text(show, maxLines = 1) }
        },
    ) {
        Text(ui("filter.title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
        header?.invoke()
        Column(Modifier.fillMaxWidth().depthPanel(RoundedCornerShape(12.dp))) {
            groups.forEachIndexed { at, group ->
                if (at > 0) HorizontalDivider(color = PanelRaised)
                AccordionRow(group, group.key == open) { open = if (open == group.key) null else group.key }
            }
        }
        switches.forEach { switch ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = FilterTouch).clip(RoundedCornerShape(10.dp))
                    .toggleable(switch.checked, role = Role.Switch, onValueChange = switch.onChange).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(switch.title, color = Parchment, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Switch(checked = switch.checked, onCheckedChange = null, modifier = Modifier.scale(SwitchScale))
            }
        }
    }
}

/** Строка группы: имя слева, выбор золотом справа и стрелка; раскрытая - с содержимым под ней. */
@Composable private fun AccordionRow(group: AccordionGroup, open: Boolean, onToggle: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = FilterTouch).clickable(role = Role.Button, onClick = onToggle).padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(group.title, color = Parchment, style = MaterialTheme.typography.bodyMedium)
            Text(
                group.value,
                color = GoldBright,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
            Icon(if (open) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, null, tint = Muted, modifier = Modifier.size(18.dp))
        }
        AnimatedVisibility(open, enter = expandVertically(), exit = shrinkVertically()) {
            Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp)) { group.content() }
        }
    }
}

/** Выбор одного из [options] фишками: выбранная - золотом. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: (T) -> Boolean, label: (T) -> String, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option -> ChoicePill(label(option), selected(option)) { onPick(option) } }
    }
}

/** Редкости цветными плитками: плитка в цвете своей редкости ([rarityColor]), выбранная - залита им. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RarityTiles(rarities: List<Rarity>, selected: (Rarity) -> Boolean, onToggle: (Rarity) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        rarities.forEach { rarity ->
            val color = rarityColor(rarity.name)
            val on = selected(rarity)
            val tile = RoundedCornerShape(10.dp)
            Text(
                rarityTitle(rarity),
                color = if (on) Ink else color,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.filterTap(tile, Role.Checkbox) { onToggle(rarity) }.background(if (on) color else color.copy(alpha = .10f), tile)
                    .border(1.dp, color.copy(alpha = if (on) 1f else .55f), tile).padding(horizontal = 10.dp, vertical = 5.dp),
            )
        }
    }
}

/**
 * Вилка уровня предмета в шапке шторки фильтра: имя и поля «от – до» одной строкой, под ними фишки «Любое» и «Около моего (±5)»
 * с уровнем героя [heroLevel]. Поля и фишки правят одну вилку [range].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LevelRangeHeader(range: LevelRange, heroLevel: Int, onChange: (LevelRange) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().background(Gold.copy(alpha = .07f), shape).border(1.dp, Gold.copy(alpha = .35f), shape).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(ui("auction.item_level"), color = GoldBright, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, modifier = Modifier.weight(1f))
            LevelField(range.min, ui("filter.level_from")) { onChange(range.copy(min = it)) }
            Text("–", color = Muted, style = MaterialTheme.typography.bodyMedium)
            LevelField(range.max, ui("filter.level_to")) { onChange(range.copy(max = it)) }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ChoicePill(ui("filter.any"), range.isAny) { onChange(LevelRange()) }
            val near = LevelRange.near(heroLevel)
            ChoicePill(ui("filter.level_near", LevelRange.NEAR), range == near, note = heroLevel.toString()) { onChange(near) }
        }
    }
}

/** Край вилки уровня - плотное поле 40dp с подписью [hint] («от», «до») внутри; ввод - только цифры ([LevelRange.bound]). */
@Composable private fun LevelField(value: Int?, hint: String, onValue: (Int?) -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    val style = MaterialTheme.typography.labelLarge.copy(color = GoldBright, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    BasicTextField(
        value?.toString().orEmpty(),
        { onValue(LevelRange.bound(it)) },
        Modifier.width(72.dp).height(FilterTouch).semantics { contentDescription = hint },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        textStyle = style,
        cursorBrush = SolidColor(Gold),
        decorationBox = { field ->
            Row(
                Modifier.fillMaxSize().background(Abyss, shape).border(1.dp, Seam, shape).padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(hint, color = Quiet, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { field() }
            }
        },
    )
}

/** Фишка выбора: выбранная - золотом; [note] - мелкая приписка после подписи (уровень героя у «Около моего»). */
@Composable private fun ChoicePill(label: String, on: Boolean, note: String? = null, onPick: () -> Unit) {
    val pill = RoundedCornerShape(14.dp)
    val ink = if (on) Ink else Parchment
    Row(
        Modifier.filterTap(pill, Role.RadioButton, onPick).background(if (on) Gold else PanelRaised, pill).heightIn(min = 28.dp).padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, color = ink, style = MaterialTheme.typography.labelMedium, fontWeight = if (on) FontWeight.SemiBold else null, maxLines = 1)
        note?.let { Text(it, color = ink.copy(alpha = .8f), style = MaterialTheme.typography.labelSmall, maxLines = 1) }
    }
}

/**
 * Касание плотного контрола фильтра: видимая форма [shape] мала, зона касания шире неё по вертикали на [FilterTapPad] с каждой
 * стороны (фишка 28dp - касание 40dp); рябь - по форме.
 */
@Composable private fun Modifier.filterTap(shape: Shape, role: Role, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    return clickable(source, indication = null, role = role, onClick = onClick).padding(vertical = FilterTapPad).clip(shape).indication(source, LocalIndication.current)
}

/** Наименьшая зона касания контрола шторки фильтра. */
private val FilterTouch = 40.dp

/** Поле касания вокруг фишки фильтра по вертикали. */
private val FilterTapPad = 6.dp

/** Масштаб переключателя шторки: строка переключателя - 40dp, касание - вся строка. */
private const val SwitchScale = .8f

/** Цвет значка фильтра: золото, когда список сужен. */
internal fun filterTint(tweaks: Int): Color = if (tweaks > 0) GoldBright else Muted
