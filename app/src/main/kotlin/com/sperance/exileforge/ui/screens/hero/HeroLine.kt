package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.groupedNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.ClassesFile
import com.sperance.exileforge.ui.components.ClassPortrait
import com.sperance.exileforge.ui.components.GoldPrice
import com.sperance.exileforge.ui.components.RoleMark
import com.sperance.exileforge.ui.theme.*

/** Строка героя над текущим состоянием игры; до прочтения героя её нет. */
@Composable fun HeroLine(game: GameUi, modifier: Modifier = Modifier) {
    rememberHeroHeader(game)?.let { HeroLine(it, modifier) }
}

/**
 * Герой одной строкой (3.90.3, макет «Тайник» В): портрет, «имя · ур.», класс мелко, [trailing] справа (по умолчанию -
 * золото) и под ними полоса опыта с подписью «42% · 1 240 / 2 950» в той же строке (3.94.0), без нажатия. С 3.90.5 это и шапка игры на вкладке «Герой» ([com.sperance.exileforge.ui.ForgeBanner]):
 * [onPortrait] - портрет открывает аккаунт, как сигил, а справа - золото, ремесло, связь, конверт почты (4.3.0) и меню.
 */
@Composable fun HeroLine(
    hero: HeroHeaderState,
    modifier: Modifier = Modifier,
    onPortrait: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = { GoldPrice(hero.money) },
) {
    val xp = experienceOf(hero.classes, hero.level, hero.experience)
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ClassPortrait(hero.heroClass, hero.portraits, Modifier.size(38.dp).then(onPortrait?.let { Modifier.clickable(onClickLabel = ui("nav.account"), onClick = it) } ?: Modifier), round = true)
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(hero.name, color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    RoleMark(hero.role, 14.dp)
                    Text("· " + ui("hero.line_level", hero.level), color = Parchment, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                }
                // Класс - база, от которой считается каждый процент; его решает сервер.
                Text(
                    hero.heroClass.takeIf { it.isNotBlank() }?.let(::classTitle) ?: ui("hero.unknown_class"),
                    color = Muted,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            trailing()
        }
        // Опыт (3.94.0): полоса и подпись одной строкой, всегда на виду - шапка игры не сворачивается
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (hero.classes != null) {
                Box(Modifier.weight(1f).height(4.dp).background(PanelRaised, RoundedCornerShape(2.dp)).semantics { contentDescription = xp.label }) {
                    Box(Modifier.fillMaxWidth(xp.fraction).fillMaxHeight().background(Brush.horizontalGradient(listOf(Gold, GoldBright)), RoundedCornerShape(2.dp)))
                }
            }
            Text(xp.label, color = Muted, fontSize = 10.sp, maxLines = 1, softWrap = false)
        }
    }
}

/** Опыт внутри уровня: доля полосы и подпись. */
private class Experience(val fraction: Float, val label: String)

/**
 * Сколько пройдено в этом уровне. Таблица опыта классов говорит, сколько стоит следующий уровень, - клиент читает её, чтобы
 * показать, что впереди, а не чтобы считать уровень: уровень решает сервер. На последнем уровне полоса полна и говорит это;
 * без контента масштаба нет, и печатается только итог. Подпись (3.90.0) - «1 240 / 2 950 · 42%».
 */
private fun experienceOf(classes: ClassesFile?, level: Int, experience: Double): Experience {
    val floor = classes?.threshold(level) ?: 0.0
    val next = classes?.nextThreshold(level)
    val span = next?.let { it - floor } ?: 0.0
    val inLevel = (experience - floor).coerceIn(0.0, span)
    val fraction = if (span > 0.0) (inLevel / span).toFloat() else 1f
    val label = when {
        classes == null -> ui("hero.xp_total", number(experience))
        next == null -> ui("hero.xp_last")
        else -> ui("hero.xp_compact", Math.round(fraction * 100), groupedNumber(inLevel.toLong()), groupedNumber(span.toLong()))
    }
    return Experience(fraction, label)
}
