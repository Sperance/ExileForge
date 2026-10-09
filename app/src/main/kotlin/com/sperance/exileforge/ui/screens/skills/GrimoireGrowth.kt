package com.sperance.exileforge.ui.screens.skills

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.RuneText
import com.sperance.exileforge.core.display.SkillGrowthView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.jobTitle
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.professionTitle
import com.sperance.exileforge.core.display.roman
import com.sperance.exileforge.core.i18n.refusalText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.skills.GrimoireViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.CoreStat
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.Item
import com.sperance.exileforge.rules.content.RuneDefinition
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.ui.components.Engraved
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeOutlinedButton
import com.sperance.exileforge.ui.components.ForgePanel
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.Inspect
import com.sperance.exileforge.ui.components.InspectAction
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.NeonHue
import com.sperance.exileforge.ui.components.rememberInspect
import com.sperance.exileforge.ui.screens.hero.StackIcon
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Blood
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.ModBlue
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Vital

/**
 * Полоса опыта умения (4.4.0): доля к следующему уровню и подпись - сколько из скольких, полная полоса в ожидании требования
 * следующего уровня или потолок тира (и что тир III ведёт дальше). [compact] - тонкая полоса страницы без подписи.
 */
@Composable internal fun XpBar(index: ContentIndex, skill: SkillDefinition, view: SkillGrowthView, compact: Boolean = false) {
    val tint = NeonHue.of(skill).color
    val shape = RoundedCornerShape(4.dp)
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(Modifier.fillMaxWidth().height(if (compact) 3.dp else 7.dp).clip(shape).background(Abyss).border(1.dp, Bronze, shape)) {
            Box(Modifier.fillMaxWidth(view.share).height(if (compact) 3.dp else 7.dp).background(Brush.horizontalGradient(listOf(tint.copy(alpha = .45f), tint))))
        }
        if (!compact) Text(xpCaption(index, skill, view), color = if (view.waiting) LifeRed else Muted, style = MaterialTheme.typography.labelSmall)
    }
}

/** Подпись полосы опыта: потолок, ожидание требования или опыт к следующему уровню. */
internal fun xpCaption(index: ContentIndex, skill: SkillDefinition, view: SkillGrowthView): String = when {
    view.capped && view.cap < view.boostedCap -> ui("skills.xp_cap_tier", view.cap, roman(view.boostedTier), view.boostedCap)
    view.capped -> ui("skills.xp_cap", view.cap)
    view.waiting -> ui("skills.xp_waiting", view.level + 1, needLine(index, skill, view.level + 1))
    else -> ui("skills.xp_progress", number(view.xp), number(view.toNext ?: 0.0), view.level + 1)
}

/**
 * Книга умения (4.4.0): неизученное - «Изучить», изученное - «Прочитать книгу: +N опыта»; на потолке и в ожидании требования
 * книга не читается - кнопка погашена, причина под ней. Без книг - откуда их взять.
 */
@Composable internal fun BookButton(
    game: GameUi,
    index: ContentIndex,
    skill: SkillDefinition,
    view: SkillGrowthView,
    heroLevel: Int,
    stats: Map<String, Double>,
    onRead: () -> Unit,
) {
    val books = bookCount(game, skill.code)
    if (books <= 0) {
        MutedText(ui(if (heroLevel < skill.unlock && view.level == 0) "skills.no_book_opens" else "skills.no_book", skill.unlock))
        return
    }
    val reason = when {
        view.level == 0 -> index.skillRules.unmet(skill, 1, heroLevel, stats).takeIf { it.isNotEmpty() }?.let { ui("skills.requires", 1, needLine(index, skill, 1)) }
        view.capped -> ui("skills.book_capped")
        view.waiting -> ui("skills.book_waiting", view.level + 1)
        else -> null
    }
    ForgeButton(
        enabled = reason == null && !game.busy,
        onClick = onRead,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Blood, contentColor = GoldBright),
    ) {
        Text(if (view.level == 0) ui("skills.learn", books) else ui("skills.read_xp", number(view.bookXp), books))
    }
    reason?.let { Text(it, color = LifeRed, style = MaterialTheme.typography.bodySmall) }
}

/**
 * Гнёзда рун (4.4.0): открытые - руна или пустое гнездо, закрытые - римская цифра тира, что их откроет. Касание пустого
 * открывает выбор руны, касание руны - её карточку с «Вынуть»: снятая руна разрушается.
 */
@Composable internal fun RuneSockets(game: GameUi, vm: GrimoireViewModel, skill: SkillDefinition, view: SkillGrowthView, onPick: (Int) -> Unit) {
    val inspect = rememberInspect()
    val tint = NeonHue.of(skill).color
    Engraved(ui("runes.sockets", view.sockets, view.maxSockets))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        (0 until view.maxSockets).forEach { socket ->
            val rune = view.runes.getOrNull(socket)
            val open = socket < view.sockets
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(if (rune != null) PanelRaised else Abyss)
                    .drawBehind { socketRing(tint.copy(alpha = if (open) .9f else .35f), dashed = rune == null) }
                    .clickable(enabled = open && !game.busy) {
                        if (rune == null) {
                            onPick(socket)
                        } else {
                            inspect(Inspect.Stack(rune.code, InspectAction(ui("runes.take_out"), enabled = !game.busy) { vm.socketRune(skill.code, socket, null) }))
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                when {
                    rune != null -> StackIcon(game, rune.code, 30)
                    open -> Text("+", color = tint, style = MaterialTheme.typography.titleMedium)
                    else -> Text(view.opensAt.getOrNull(socket)?.let(::roman).orEmpty(), color = Muted, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
    view.runes.filterNotNull().forEach { rune ->
        Text(ui("runes.line", itemTitle(rune.code), RuneText.effect(rune, runePower(game))), color = ModBlue, style = MaterialTheme.typography.bodySmall)
    }
}

/** Кольцо гнезда: сплошное у руны, пунктирное у пустого и закрытого. */
private fun DrawScope.socketRing(color: Color, dashed: Boolean) {
    val width = 1.5.dp.toPx()
    drawCircle(
        color,
        radius = size.minDimension / 2 - width,
        center = Offset(size.width / 2, size.height / 2),
        style = Stroke(width, pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 3.dp.toPx())) else null),
    )
}

/** Сила рун героя: `1 + STOCK_RUNE_EFFECT / 100`. */
internal fun runePower(game: GameUi): Double = 1 + (game.hero?.stats?.get(CoreStat.RUNE_EFFECT.code) ?: 0.0) / 100

/**
 * Выбор руны в гнездо [socket] (4.4.0): стопки сумки категории руны, что встают в вид умения. Касание - карточка руны с
 * «Вставить»; правила отказа ([com.sperance.exileforge.rules.content.SkillGrowth.socketRefusal]) гасят кнопку и называют причину.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RunePicker(
    game: GameUi,
    vm: GrimoireViewModel,
    index: ContentIndex,
    skill: SkillDefinition,
    skills: HeroSkills,
    socket: Int,
    stats: Map<String, Double>,
    onDismiss: () -> Unit,
) {
    val inspect = rememberInspect()
    val growth = index.skillGrowth
    val runes = index.itemsByCategory[Item.RUNE].orEmpty().mapNotNull { item -> growth.rune(item.code.value) }
        .filter { skill.type in it.types && (game.bagAmount(it.code) ?: 0L) > 0 }
    ForgeSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(ui("runes.pick", socket + 1, SkillText.title(skill.code)), color = GoldBright, style = MaterialTheme.typography.titleMedium)
            MutedText(ui("runes.pick_hint"))
            if (runes.isEmpty()) MutedText(ui("runes.none"))
            runes.forEach { rune ->
                val refusal = growth.socketRefusal(skills, skill, socket, rune, stats)
                RuneRow(game, rune, refusal == null) {
                    val action = InspectAction(ui("runes.insert"), enabled = refusal == null && !game.busy, reason = refusal?.let(::refusalText)) {
                        onDismiss()
                        vm.socketRune(skill.code, socket, rune.code)
                    }
                    inspect(Inspect.Stack(rune.code, action))
                }
            }
        }
    }
}

/** Строка руны в выборе: значок, имя, семейство и действие, сколько в сумке; не встающая - приглушена. */
@Composable private fun RuneRow(game: GameUi, rune: RuneDefinition, fits: Boolean, onOpen: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(PanelRaised).clickable(onClick = onOpen).padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StackIcon(game, rune.code, 34)
        Column(Modifier.weight(1f)) {
            Text(itemTitle(rune.code), color = if (fits) GoldBright else Muted, style = MaterialTheme.typography.bodyMedium)
            Text(RuneText.family(rune) + " · " + RuneText.effect(rune, runePower(game)), color = if (fits) ModBlue else Muted, style = MaterialTheme.typography.labelSmall)
        }
        Text(ui("runes.owned", game.bagAmount(rune.code) ?: 0L), color = Gold, style = MaterialTheme.typography.labelMedium)
    }
}

/**
 * Ритуал тира (4.4.0): следующий тир, его порог уровня и вся цена - книги умения, примесь стихии и вход работы чар, - сколько
 * этого в сумке; отказ правил, если ритуал пока не начать, и кнопка к работе Зачарования. Высший тир - одна строка.
 */
@Composable internal fun RitualCard(game: GameUi, vm: GrimoireViewModel, view: SkillGrowthView) {
    ForgePanel {
        val ritual = view.ritual
        if (ritual == null) {
            Engraved(ui("ritual.title_top"))
            MutedText(ui("ritual.top", roman(view.owned)))
            return@ForgePanel
        }
        Engraved(ui("ritual.title", roman(ritual.rule.tier)))
        Text(
            ui("ritual.gives", ritual.rule.level, ritual.rule.sockets, number(ritual.rule.boost)) + if (ritual.rule.ascended) " " + ui("ritual.ascended") else "",
            color = Parchment,
            style = MaterialTheme.typography.bodySmall,
        )
        Text(ui("ritual.where", jobTitle(ritual.job.code), professionTitle(ritual.profession), ritual.job.level), color = Muted, style = MaterialTheme.typography.labelSmall)
        ritual.cost.forEach { input ->
            val have = game.bagAmount(input.item) ?: 0L
            Text(ui("ritual.cost", itemTitle(input.item), have, input.amount), color = if (have >= input.amount) Vital else LifeRed, style = MaterialTheme.typography.labelMedium)
        }
        ritual.refusal?.let { Text(refusalText(it), color = LifeRed, style = MaterialTheme.typography.bodySmall) }
        ForgeOutlinedButton(enabled = !game.busy, onClick = { vm.openCrafts() }, modifier = Modifier.fillMaxWidth()) { Text(ui("ritual.to_job", professionTitle(ritual.profession))) }
    }
}

/** Подпись тира под именем умения: «Тир II». */
internal fun tierLine(view: SkillGrowthView): String = ui("skills.tier", roman(view.tier))
