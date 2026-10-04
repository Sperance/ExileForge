package com.sperance.exileforge.ui.screens.skills

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.Flask
import com.sperance.exileforge.core.campaign.FlaskKind
import com.sperance.exileforge.core.campaign.Loadout
import com.sperance.exileforge.core.campaign.combat.Combatant
import com.sperance.exileforge.core.campaign.draught
import com.sperance.exileforge.core.display.ItemView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.fineNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.statTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.HeroView
import com.sperance.exileforge.presentation.skills.GrimoireViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.presentation.state.view
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SkillKind
import com.sperance.exileforge.rules.content.SkillRules
import com.sperance.exileforge.rules.content.Slot
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.Caption
import com.sperance.exileforge.ui.theme.*
import org.koin.compose.viewmodel.koinViewModel

/** Пояс флаконов и обмен книг (3.80.19). */
/** The belt as three flasks side by side, each filled to its charges, the chosen one lit; an empty place is a dashed outline. */
@Composable internal fun Belt(hero: HeroView, index: ContentIndex, skills: HeroSkills, body: Combatant, selected: Int, onSelect: (Int) -> Unit) {
    val flasks = worn(hero, index, skills)
    Row(
        Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color(0xFF3A2A1A), Color(0xFF24190F))), RoundedCornerShape(10.dp))
            .border(1.dp, Bronze, RoundedCornerShape(10.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        flasks.forEachIndexed { at, worn ->
            val flask = worn?.flask
            val on = at == selected
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).background(if (on) Gold.copy(alpha = .12f) else Color.Transparent)
                    .border(1.dp, if (on) GoldBright else Color.Transparent, RoundedCornerShape(8.dp)).clickable { onSelect(at) }.padding(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FlaskBottle(flask?.kind, 1f, flask != null, Modifier.size(34.dp, 56.dp))
                Text(
                    if (flask != null) "${flask.maxCharges.toInt()} · ${flask.perUse(body).toInt()}" else ui("skills.belt_empty"),
                    color = if (flask != null) GoldBright else Muted,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(flask?.condition?.let(::conditionTitle) ?: "", color = Rune, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** A flask worn on the belt: the copy over the content on screen, and the fight's reading of it. */
internal data class WornFlask(val view: ItemView, val flask: Flask)

/** The belt's three places: each worn flask with the fight's reading of it; null where a place is empty or its template is unknown. */
internal fun worn(hero: HeroView, index: ContentIndex, skills: HeroSkills): List<WornFlask?> = Slot.FLASKS.mapIndexed { i, slot ->
    hero.equipped[slot]?.let { item ->
        index.template(item.template)?.let { WornFlask(ItemView(item, it, index), Flask.of(item, it, index, skills.flasks.getOrNull(i))) }
    }
}

/** A flask drawn as a bottle whose liquid stands at [fill], in the colour of what it brings. */
@Composable fun FlaskBottle(kind: FlaskKind?, fill: Float, present: Boolean, modifier: Modifier) {
    val tint = kind?.let(::flaskTint) ?: Muted
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val neck = Path().apply {
            moveTo(w * .38f, 0f)
            lineTo(w * .62f, 0f)
            lineTo(w * .62f, h * .28f)
            lineTo(w * .95f, h * .62f)
            lineTo(w * .88f, h)
            lineTo(w * .12f, h)
            lineTo(w * .05f, h * .62f)
            lineTo(w * .38f, h * .28f)
            close()
        }
        if (present) {
            clipPath(neck) {
                val top = h * (1 - fill.coerceIn(0f, 1f) * .7f)
                drawRect(tint.copy(alpha = .8f), topLeft = Offset(0f, top), size = Size(w, h - top))
            }
        }
        drawPath(
            neck,
            if (present) GoldBright.copy(alpha = .8f) else Muted.copy(alpha = .6f),
            style = Stroke(width = 2.dp.toPx(), pathEffect = if (present) null else PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
        )
    }
}

/**
 * The chosen place of the belt: the flask's name, what a draught of it gives with the hero's sheet —
 * life or mana and how fast, how long it lasts, what it costs of its charges — when it is drunk by
 * itself, and its card.
 */
@Composable internal fun BeltDetail(hero: HeroView, index: ContentIndex, skills: HeroSkills, body: Combatant, at: Int, onCondition: () -> Unit) {
    val (view, flask) = worn(hero, index, skills).getOrNull(at) ?: run {
        InfoCard(ui("skills.belt_place", at + 1), ui("skills.belt_empty_hint"))
        return
    }
    val draught = flask.draught(body, body.maxLife, body.maxMana)
    ForgePanel(accent = flaskTint(flask.kind)) {
        Text(view.title, color = rarityColor(view.rarity.name), style = MaterialTheme.typography.titleMedium)
        val gives = when (flask.kind) {
            FlaskKind.LIFE -> ui("skills.flask_life", number(draught.life + draught.lifeRate * draught.duration), fineNumber(draught.duration))
            FlaskKind.MANA -> ui("skills.flask_mana", number(draught.mana + draught.manaRate * draught.duration), fineNumber(draught.duration))
            FlaskKind.UTILITY -> ui("skills.flask_utility", fineNumber(draught.duration))
        }
        Text(gives, color = Parchment, style = MaterialTheme.typography.bodyMedium)
        Text(
            ui("skills.flask_charges", number(flask.perUse(body)), number(flask.maxCharges)) +
                (if (view.quality > 0) " · " + ui("skills.flask_quality", view.quality) else ""),
            color = Muted,
            style = MaterialTheme.typography.labelMedium,
        )
        if (draught.lines.isNotEmpty()) SkillLines(draught.lines.map { SkillText.statLine(it.stat, it.op, it.value) })
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(ui("skills.drunk_when"), color = Muted, style = MaterialTheme.typography.labelMedium)
            AssistChip(onClick = onCondition, label = { Text(conditionTitle(flask.condition)) })
        }
    }
    ItemCard(view, enabled = false, detailed = true)
}

/**
 * Three books for one (2.78.0): any three of the bag — a book counted as often as it lies there — and the
 * rules' gold for a book of the class's own, opened by the hero's level.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Exchange(game: GameUi, vm: GrimoireViewModel, index: ContentIndex, hero: HeroView, pages: List<SkillDefinition>, onDismiss: () -> Unit) {
    val rule = index.skills.rules.exchange
    val owned = books(index).mapNotNull { book -> (game.bagAmount(book.code) ?: 0L).takeIf { it > 0 }?.let { book.code.removePrefix(SkillRules.BOOK_PREFIX) to it } }
    var chosen by remember { mutableStateOf(listOf<String>()) }
    var target by remember { mutableStateOf<String?>(null) }
    val price = rule.goldPerLevel * hero.level
    val money = hero.money
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(ui("skills.exchange_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            MutedText(ui("skills.exchange_rule", rule.books, number(price.toDouble())))
            Caption(ui("skills.exchange_give", chosen.size, rule.books))
            owned.forEach { (code, amount) ->
                val taken = chosen.count { it == code }
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (taken > 0) PanelRaised else Color.Transparent)
                        .clickable { chosen = if (taken < amount && chosen.size < rule.books) chosen + code else chosen - code }.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    index.skills.byCode[code]?.let { SkillGlyph(it.icon, Modifier.size(22.dp), if (taken > 0) GoldBright else Muted) }
                    Text(SkillText.title(code), color = if (taken > 0) GoldBright else Parchment, modifier = Modifier.weight(1f))
                    Text(if (taken > 0) "$taken / $amount" else "× $amount", color = Gold)
                }
            }
            Caption(ui("skills.exchange_take"))
            pages.filter { it.unlock <= hero.level }.forEach { skill ->
                val on = target == skill.code
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(if (on) PanelRaised else Color.Transparent)
                        .clickable { target = skill.code }.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = on, onClick = { target = skill.code }, colors = RadioButtonDefaults.colors(selectedColor = Gold))
                    Text(SkillText.title(skill.code), color = if (on) GoldBright else Parchment)
                }
            }
            val ready = chosen.size == rule.books && target != null && money >= price && !game.busy
            if (money < price) Text(ui("skills.exchange_gold", number(price.toDouble())), color = LifeRed, style = MaterialTheme.typography.bodySmall)
            ForgeButton(
                enabled = ready,
                onClick = {
                    target?.let { vm.exchangeBooks(chosen, it) }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright),
            ) { Text(ui("skills.exchange")) }
        }
    }
}

internal val PREPARATION: String = CoreStat.SKILL_PREPARATION.code
