package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.sperance.exileforge.core.display.PickList
import com.sperance.exileforge.core.display.PickOption
import com.sperance.exileforge.core.filter.LevelRange
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.theme.*

/** Строка шторки-аккордеона: имя группы, её текущий выбор словами и что раскрывается под ней по нажатию. */
class AccordionGroup(val key: String, val title: String, val value: String, val content: @Composable () -> Unit)

/** Переключатель под группами шторки: «Только сделанное мной», «Скрыть надетое» и подобные; [hint] - что он значит, мелко под именем. */
class FilterSwitch(val title: String, val checked: Boolean, val hint: String? = null, val onChange: (Boolean) -> Unit)

/**
 * Шторка фильтров «Аккордеон» - одна на все списки приложения: под именем - [header] (вилка уровня, [LevelRangeHeader]), каждая
 * группа - строка с текущим выбором, что раскрывается по нажатию (открыта одна), ниже переключатели, внизу «Сбросить» и
 * «Показать N» ([show]). Поиск списка - в строке поиска экрана ([SearchBar]); в шторке ищут только варианты длинной группы
 * ([SearchChoice]). Что выбрано - решает список: шторка лишь рисует [groups] и [switches].
 *
 * Контролы шторки плотные: фишки - [ChipSize.Dense] (28dp, касание 40dp), строки и поля 40dp, зона касания - не ниже [FilterTouch].
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
                Column(Modifier.weight(1f)) {
                    Text(switch.title, color = Parchment, style = MaterialTheme.typography.bodyMedium)
                    switch.hint?.let { Text(it, color = Muted, style = MaterialTheme.typography.labelSmall) }
                }
                Switch(checked = switch.checked, onCheckedChange = null, modifier = Modifier.scale(SWITCH_SCALE))
            }
        }
    }
}

/** Строка группы - свёртка ([foldToggle]): имя слева, выбор золотом справа и шеврон; раскрытая - с содержимым под ней. */
@Composable private fun AccordionRow(group: AccordionGroup, open: Boolean, onToggle: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = FilterTouch).foldToggle(open, group.title, onToggle).padding(horizontal = 12.dp, vertical = 6.dp),
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
            FoldChevron(open)
        }
        FoldBody(open) {
            Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp)) { group.content() }
        }
    }
}

/** Выбор одного из [options] плотными фишками выбора ([ForgeChip], [ChipSize.Dense]): выбранная - золотом. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(options: List<T>, selected: (T) -> Boolean, label: (T) -> String, onPick: (T) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option -> ForgeChip(label(option), selected(option), { onPick(option) }, mode = ChipMode.Choice, size = ChipSize.Dense) }
    }
}

/**
 * Выбор одного варианта группы шторки с общим поиском вариантов ([PickList.found]): строка поиска с подсказкой [hint] и фишки -
 * сначала [fixed] (не ищутся: «Любое», «Все дары»), затем найденные из [options]; выбран вариант с ключом [chosen]. Поиск в шторке
 * фильтра - только так, в группе с длинным списком вариантов («по моду», дар модерации).
 */
@Composable
fun <K> SearchChoice(options: List<PickOption<K>>, chosen: K?, hint: String, fixed: List<PickOption<K>> = emptyList(), onPick: (K?) -> Unit) {
    var query by remember { mutableStateOf("") }
    val list = remember(options) { PickList(options) }
    val shown = remember(list, fixed, query) { fixed + list.found(query) }
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SearchBar(query, { query = it }, hint)
        ChoiceChips(shown, { it.key == chosen }, { it.title }) { onPick(it.key) }
    }
}

/**
 * Вилка уровня предмета в шапке шторки фильтра: имя и поля «от – до» одной строкой, под ними фишки «Любое» и «Около моего (±5)»
 * с уровнем героя [heroLevel]. Поля и фишки правят одну вилку [range].
 */
@Composable
fun LevelRangeHeader(range: LevelRange, heroLevel: Int, onChange: (LevelRange) -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.fillMaxWidth().background(Gold.copy(alpha = .07f), shape).border(1.dp, Gold.copy(alpha = .35f), shape).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(ui("filter.item_level"), color = GoldBright, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, modifier = Modifier.weight(1f))
            LevelField(range.min, ui("filter.level_from")) { onChange(range.copy(min = it)) }
            Text("–", color = Muted, style = MaterialTheme.typography.bodyMedium)
            LevelField(range.max, ui("filter.level_to")) { onChange(range.copy(max = it)) }
        }
        val near = LevelRange.near(heroLevel)
        ChoiceChips(listOf(LevelRange(), near), { it == range }, { if (it.isAny) ui("filter.any") else ui("filter.level_near", LevelRange.NEAR) + " · " + heroLevel }, onChange)
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

/** Наименьшая зона касания контрола шторки фильтра. */
private val FilterTouch = 40.dp

/** Масштаб переключателя шторки: строка переключателя - 40dp, касание - вся строка. */
private const val SWITCH_SCALE = .8f
