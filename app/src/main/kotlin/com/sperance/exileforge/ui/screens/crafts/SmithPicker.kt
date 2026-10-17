package com.sperance.exileforge.ui.screens.crafts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.choiceTitle
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.CoreAttribute
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SmithCategory
import com.sperance.exileforge.rules.content.SmithChoice
import com.sperance.exileforge.ui.icons.BagIcon
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.SlotIcon
import com.sperance.exileforge.ui.theme.*

/**
 * Выбор кузнеца «Сетка 3×3»: все варианты разом - строки «Оружие / Броня» × столбцы атрибутов (над столбцом - самоцвет
 * атрибута и его имя), нижний ряд - «Украшения» и «Случайно»; одно касание - один выбор. В ячейке - облик базы и сколько
 * самоцвета выбора в сумке ([have]); нет самоцвета - ячейка тусклая, число красным. Под сеткой - что даст выбранное.
 */
@Composable
internal fun SmithPicker(choice: SmithChoice, have: (String) -> Long, onChoice: (SmithChoice) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(GAP)) {
            Spacer(Modifier.width(HEAD))
            CoreAttribute.entries.forEach { attribute -> AttributeHead(attribute, Modifier.weight(1f)) }
        }
        listOf(SmithCategory.WEAPON, SmithCategory.ARMOUR).forEach { category ->
            Row(horizontalArrangement = Arrangement.spacedBy(GAP), verticalAlignment = Alignment.CenterVertically) {
                CategoryHead(category)
                CoreAttribute.entries.forEach { attribute ->
                    val option = SmithChoice.of(category, attribute)
                    val gems = option.gems.sumOf(have)
                    Cell(option == choice, gems == 0L, Modifier.weight(1f), onClick = { onChoice(option) }) {
                        SlotIcon(category.face, Parchment, Modifier.size(26.dp))
                        GemCount(option.gems.first(), gems)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(GAP), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.width(HEAD))
            val jewellery = SmithChoice.JEWELLERY
            Cell(jewellery == choice, jewellery.gems.any { have(it) == 0L }, Modifier.weight(2f), wide = true, onClick = { onChoice(jewellery) }) {
                SlotIcon(SmithCategory.JEWELLERY.face, Parchment, Modifier.size(26.dp))
                Column {
                    Text(ui("merchant.group.JEWELLERY"), color = Parchment, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(gemList(jewellery.gems, have), style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
            Cell(SmithChoice.RANDOM == choice, scarce = false, Modifier.weight(1f), onClick = { onChoice(SmithChoice.RANDOM) }) {
                Icon(ForgeGlyphs.Anvil, null, tint = Parchment, modifier = Modifier.size(26.dp))
                Text(ui("crafts.mode.random"), color = Parchment, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
            }
        }
        ChoiceHint(choice)
    }
}

/** Заголовок столбца: самоцвет атрибута и его имя в цвете атрибута. */
@Composable
private fun AttributeHead(attribute: CoreAttribute, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        BagIcon(attribute.gem, Modifier.size(18.dp), tint = attribute.tint)
        Text(attributeTitle(attribute), color = attribute.tint, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Заголовок строки: облик группы и её имя. */
@Composable
private fun CategoryHead(category: SmithCategory) {
    Column(Modifier.width(HEAD), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        SlotIcon(category.face, Muted, Modifier.size(16.dp))
        Text(categoryTitle(category), color = Muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Число самоцвета [gem] в сумке у ячейки: нет ни одного - красным. */
@Composable
private fun GemCount(gem: String, count: Long) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        BagIcon(gem, Modifier.size(11.dp), tint = Muted)
        Text(count.toString(), color = if (count == 0L) LifeRed else Muted, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * Ячейка сетки: выбранная - в золотой рамке с зелёным светом сверху и точкой в углу; [scarce] - самоцвета нет, ячейка тусклая.
 * [wide] - содержимое в строку (украшения), иначе столбцом.
 */
@Composable
private fun Cell(selected: Boolean, scarce: Boolean, modifier: Modifier, wide: Boolean = false, onClick: () -> Unit, content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val ground = if (selected) Modifier.background(Brush.radialGradient(listOf(Gold.copy(alpha = .22f), Abyss)), shape) else Modifier.background(Abyss, shape)
    Box(
        modifier.height(CELL).clip(shape).then(ground).border(if (selected) 1.5.dp else 1.dp, if (selected) Gold else Bronze, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onClick).alpha(if (scarce && !selected) .55f else 1f),
    ) {
        if (wide) {
            Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { content() }
        } else {
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically)) { content() }
        }
        if (selected) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(8.dp).background(Gold, CircleShape))
    }
}

/** Самоцветы украшений строкой «Рубин · Топаз · Сапфир 0»: каждый, которого нет, - красным с нулём. */
private fun gemList(gems: List<String>, have: (String) -> Long) = buildAnnotatedString {
    gems.forEachIndexed { i, gem ->
        if (i > 0) withStyle(SpanStyle(color = Muted)) { append(" · ") }
        val count = have(gem)
        if (count == 0L) {
            withStyle(SpanStyle(color = LifeRed, fontWeight = FontWeight.Bold)) { append(itemTitle(gem) + " 0") }
        } else {
            withStyle(SpanStyle(color = Muted)) { append(itemTitle(gem)) }
        }
    }
}

/** Что даст выбранное: имя выбора жирным и его объяснение; у случайной ковки - что она без самоцветов и примесей. */
@Composable
private fun ChoiceHint(choice: SmithChoice) {
    val text = buildAnnotatedString {
        if (choice == SmithChoice.RANDOM) {
            append(ui("crafts.random_hint"))
            return@buildAnnotatedString
        }
        withStyle(SpanStyle(color = Parchment, fontWeight = FontWeight.SemiBold)) { append(choiceTitle(choice.name)) }
        append(" — ")
        append(choice.attribute?.let { ui("crafts.smith_hint", attributeTitle(it)) } ?: ui("crafts.smith_hint_jewellery"))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Outlined.Info, null, tint = Muted, modifier = Modifier.size(14.dp).padding(top = 1.dp))
        Text(text, color = Muted, style = MaterialTheme.typography.labelSmall)
    }
}

/** Облик группы в сетке: оружие - одноручное, броня - нагрудник, украшения - кольцо. */
private val SmithCategory.face: Slot
    get() = when (this) {
        SmithCategory.WEAPON -> Slot.WEAPON_1H
        SmithCategory.ARMOUR -> Slot.BODY
        SmithCategory.JEWELLERY -> Slot.RING
    }

/** Самоцвет атрибута - тот, что тратит выбор его столбца. */
private val CoreAttribute.gem: String get() = SmithChoice.of(SmithCategory.WEAPON, this).gems.first()

/** Цвет атрибута: сила - кровь, ловкость - топаз, интеллект - мана. */
private val CoreAttribute.tint: Color
    get() = when (this) {
        CoreAttribute.STRENGTH -> LifeRed
        CoreAttribute.DEXTERITY -> Caution
        CoreAttribute.INTELLIGENCE -> ManaBlue
    }

private fun categoryTitle(category: SmithCategory): String = ui("merchant.group.${category.name}")

private fun attributeTitle(attribute: CoreAttribute): String = when (attribute) {
    CoreAttribute.STRENGTH -> ui("req.strength")
    CoreAttribute.DEXTERITY -> ui("req.dexterity")
    CoreAttribute.INTELLIGENCE -> ui("req.intelligence")
}

/** Ширина заголовка строки сетки. */
private val HEAD: Dp = 52.dp

/** Высота ячейки сетки. */
private val CELL: Dp = 58.dp

/** Зазор сетки. */
private val GAP: Dp = 6.dp
