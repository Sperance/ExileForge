package com.sperance.exileforge.ui.screens.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.lineText
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.StoryBooks
import com.sperance.exileforge.rules.content.StoryChapter
import com.sperance.exileforge.rules.content.ThroneLaw
import com.sperance.exileforge.rules.text.LocaleKey
import com.sperance.exileforge.ui.components.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.skills.SkillPeek
import com.sperance.exileforge.ui.screens.skills.rememberSkillInspect
import com.sperance.exileforge.ui.theme.*

/**
 * Трон пройденной главы [chapter] ждёт выбора: эпилог главы со строкой правила и карточки законов как варианты ответа - радио у
 * имени, между ними медальон «ИЛИ»; в карточке обе стороны закона - что получаете и цена этого закона. Выбранная карточка светится,
 * прочие притушены; под ними одна кнопка «Взять «…» навсегда» (до выбора погашена «Выберите закон») и что будет утрачено; взятие -
 * через шторку с удержанием, [onTake] - закон навсегда, перевыбора нет.
 */
@Composable internal fun LawChoice(index: ContentIndex, chapter: StoryChapter, busy: Boolean, onTake: (String) -> Unit) {
    var picked by rememberSaveable(chapter.region) { mutableStateOf<String?>(null) }
    var asked by remember { mutableStateOf<ThroneLaw?>(null) }
    val chosen = chapter.laws.firstOrNull { it.code == picked }
    Epilogue(chapter)
    chapter.laws.forEachIndexed { i, law ->
        if (i > 0) OrMedallion()
        LawOption(index, law, selected = law == chosen, dim = chosen != null && law != chosen) { picked = law.code }
    }
    ForgeButton({ asked = chosen }, state = ButtonState.of(!busy && chosen != null), modifier = Modifier.fillMaxWidth()) {
        Text(chosen?.let { ui("story.law.take_named", loc(LocaleKey.lawName(it.code))) } ?: ui("story.law.pick"), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
    chosen?.let { law ->
        val lost = chapter.laws.filter { it != law }.joinToString(", ") { ui("story.law.quoted", loc(LocaleKey.lawName(it.code))) }
        Text(
            accented(ui("story.law.lost", lost), ui("story.law.lost_mark"), PAY),
            color = Muted,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    asked?.let { law ->
        ConfirmSheet(
            title = ui("story.law.confirm_title", loc(LocaleKey.lawName(law.code))),
            confirm = ui("story.law.confirm"),
            onDismiss = { asked = null },
            ledger = ledger(index, law),
            warning = ui("story.law.warning"),
            danger = true,
        ) {
            asked = null
            onTake(law.code)
        }
    }
}

/** Текст [text] с частью [part] цветом [color]; части в тексте нет - текст как есть. */
private fun accented(text: String, part: String, color: Color): AnnotatedString = buildAnnotatedString {
    append(text)
    text.indexOf(part).takeIf { part.isNotEmpty() && it >= 0 }?.let { at -> addStyle(SpanStyle(color = color), at, at + part.length) }
}

/** Эпилог главы над законами: «Закон трона», глава, текст до трёх строк («Читать полностью») и строка правила выбора. */
@Composable private fun Epilogue(chapter: StoryChapter) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Brush.verticalGradient(listOf(EPI_TOP, EPI_BOTTOM)), shape).border(1.dp, GoldBright.copy(alpha = .28f), shape)
            .drawBehind { drawRect(GoldBright, size = Size(3.dp.toPx(), size.height)) }.padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(ForgeGlyphs.Keep, null, tint = GoldBright, modifier = Modifier.size(16.dp))
            Engraved(ui("story.law.title"), accent = GoldBright, modifier = Modifier.weight(1f))
            Text(loc(LocaleKey.chapterTitle(chapter.region)), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        ExpandableText(loc(LocaleKey.chapterOutro(chapter.region)), color = Parchment, style = MaterialTheme.typography.bodyMedium, folded = OUTRO_LINES, more = ui("common.read_more"))
        HorizontalDivider(color = Bronze)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(ForgeGlyphs.Scales, null, tint = Caution, modifier = Modifier.size(16.dp))
            Text(ui("story.law.rule"), color = GoldBright, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Закон-вариант ответа: радио и имя; «Вы получаете» - строки героя, «Цена этого закона» - строки монстров в красной половине с
 * подписью, кому они. Выбранный - светится [Gold], притушенный [dim] - вполсилы.
 */
@Composable private fun LawOption(index: ContentIndex, law: ThroneLaw, selected: Boolean, dim: Boolean, onPick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().alpha(if (dim) DIM else 1f).glow(Gold, on = selected, radius = 12.dp, shape = shape).clip(shape)
            .then(if (selected) Modifier.background(Brush.verticalGradient(listOf(PICKED_TOP, PICKED_BOTTOM)), shape) else Modifier.depthPanel(shape, elevation = 0.dp))
            .border(1.dp, if (selected) Gold.copy(alpha = .85f) else Bronze, shape)
            .selectable(selected, role = Role.RadioButton, onClick = onPick).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(Modifier.padding(bottom = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Radio(selected)
            Text(loc(LocaleKey.lawName(law.code)), color = GoldBright, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        }
        LawHalf(ui("story.law.gain"), HALF_GAIN, null) {
            law.hero.forEach { LawLine(Icons.Outlined.Shield, lineText(index, it), Vital, MaterialTheme.typography.bodyMedium, FontWeight.SemiBold) }
        }
        LawHalf(ui("story.law.price"), HALF_PAY, PAY.copy(alpha = .18f)) {
            law.monsters.forEach { LawLine(ForgeGlyphs.Skull, lineText(index, it), PAY, MaterialTheme.typography.bodySmall, FontWeight.Normal) }
            MutedText(ui("story.law.who"), Modifier.padding(start = 24.dp), style = MaterialTheme.typography.labelSmall)
        }
    }
}

/** Половина карточки закона: подпись гравировкой и строки; [edge] - контур половины цены, null - без него. */
@Composable private fun LawHalf(title: String, fill: Color, edge: Color?, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().background(fill, shape).then(edge?.let { Modifier.border(1.dp, it, shape) } ?: Modifier).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Engraved(title, accent = Muted)
        content()
    }
}

/** Строка закона: знак стороны и текст строки модификатора. */
@Composable private fun LawLine(icon: ImageVector, text: String, tint: Color, style: TextStyle, weight: FontWeight) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(16.dp))
        Text(text, color = tint, style = style, fontWeight = weight)
    }
}

/** Радио выбора закона: кольцо, у выбранного - [Gold] с точкой. */
@Composable private fun Radio(on: Boolean) {
    Box(Modifier.size(22.dp).border(2.dp, if (on) Gold else Muted.copy(alpha = .4f), CircleShape), contentAlignment = Alignment.Center) {
        if (on) Box(Modifier.size(10.dp).glow(Gold, radius = 6.dp, shape = CircleShape).clip(CircleShape).background(Gold))
    }
}

/** Медальон «ИЛИ» между законами: круг с тусклыми линиями по сторонам. */
@Composable private fun OrMedallion() {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, OR_LINE))))
        Box(
            Modifier.size(44.dp).glow(Caution, radius = 8.dp, shape = CircleShape).clip(CircleShape).background(Brush.radialGradient(listOf(OR_CORE, OR_EDGE)))
                .border(1.dp, Caution.copy(alpha = .45f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text(ui("story.law.or"), color = Caution, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold) }
        Box(Modifier.weight(1f).height(1.dp).background(Brush.horizontalGradient(listOf(OR_LINE, Color.Transparent))))
    }
}

/** Строк эпилога до «Читать полностью». */
private const val OUTRO_LINES = 3

/** Притушенный невыбранный закон. */
private const val DIM = .72f

/** Стекло эпилога: сверху светлее. */
private val EPI_TOP = Color(0xFF1F2A2A)
private val EPI_BOTTOM = Color(0xFF121A1C)

/** Выбранный закон: зелёное стекло. */
private val PICKED_TOP = Color(0xFF24352F)
private val PICKED_BOTTOM = Color(0xFF121C19)

/** Половины карточки закона: тёмная - выгода, красноватая - цена. */
private val HALF_GAIN = Color(0x730A0E12)
private val HALF_PAY = Color(0x1F8A2E2E)

/** Цена закона - мягкий красный. */
private val PAY = Color(0xFFE48A83)

/** Медальон «ИЛИ»: сердцевина, кромка и линии по сторонам. */
private val OR_CORE = Color(0xFF2A2A1E)
private val OR_EDGE = Color(0xFF12140F)
private val OR_LINE = Color(0xFF3A4B57)

/** Взятые законы: имя, строки героя и цена для монстров - на экране Сюжета под главой. */
@Composable internal fun TakenLaws(index: ContentIndex, laws: List<ThroneLaw>) {
    if (laws.isEmpty()) return
    Engraved(ui("story.laws.title"))
    laws.forEach { LawCard(index, it) }
}

/** Карточка закона: имя, что он даёт герою и чем платят - монстры регионов дальше трона и карт Атласа; [action] - под ними. */
@Composable private fun LawCard(index: ContentIndex, law: ThroneLaw, action: (@Composable ColumnScope.() -> Unit)? = null) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).depthPanel(shape, accent = Gold).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(loc(LocaleKey.lawName(law.code)), color = GoldBright, style = MaterialTheme.typography.titleSmall)
        MutedText(ui("story.law.hero"))
        law.hero.forEach { Text("+ " + lineText(index, it), color = Vital, style = MaterialTheme.typography.bodyMedium) }
        MutedText(ui("story.law.cost"))
        law.monsters.forEach { Text("− " + lineText(index, it), color = LifeRed, style = MaterialTheme.typography.bodyMedium) }
        action?.invoke(this)
    }
}

/** Строки закона в шторке подтверждения: что получит герой и что получат монстры дальше. */
private fun ledger(index: ContentIndex, law: ThroneLaw): List<LedgerLine> = law.hero.map { LedgerLine(ui("story.law.ledger_hero"), lineText(index, it), Tone.GAIN) } +
    law.monsters.map { LedgerLine(ui("story.law.ledger_monsters"), lineText(index, it), Tone.SPEND) }

/**
 * Книга за пройденную главу [chapter]: варианты правил ([StoryBooks.options]) - умения своего класса или общие,
 * открытые к уровню главы; [onTake] - книга в сумку. Выбор один на главу.
 * строка варианта открывает карточку умения, а книгу берёт её главная кнопка: выбор - после того, как прочитано, что умение делает.
 */
@Composable internal fun BookChoice(index: ContentIndex, chapter: Int, heroClass: String, skills: HeroSkills, busy: Boolean, onTake: (Int, String) -> Unit) {
    val region = index.quests.story.getOrNull(chapter)?.region ?: return
    val inspect = rememberSkillInspect()
    ForgePanel(accent = GoldBright) {
        Engraved(ui("story.book.title"), accent = GoldBright)
        MutedText(ui("story.book.choose", loc(LocaleKey.chapterTitle(region))))
        StoryBooks(index).options(chapter, heroClass, skills).forEach { code ->
            val skill = index.skills.byCode[code] ?: return@forEach
            val take = InspectAction(ui("story.book.take"), enabled = !busy) { onTake(chapter, code) }
            Row(
                Modifier.fillMaxWidth().padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).clickable(role = Role.Button) { inspect(SkillPeek(code, action = take)) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NeonSkillIcon(skill, HeroSkills.FIRST_TIER, 36.dp)
                Column(Modifier.weight(1f)) {
                    Text(SkillText.title(code), color = Parchment, style = MaterialTheme.typography.bodyMedium)
                    MutedText(
                        listOfNotNull(
                            ui("skills.type.${skill.type.name}"),
                            ui("story.book.common").takeIf { skill.common },
                            ui("story.book.known", skills.level(code)).takeIf { skills.level(code) > 0 },
                        ).joinToString(" · "),
                    )
                }
                Icon(Icons.Outlined.Info, ui("story.book.about"), tint = Rune, modifier = Modifier.size(20.dp))
            }
        }
    }
}
