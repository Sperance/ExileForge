package com.sperance.exileforge.ui.screens.hero

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.display.contestTitle
import com.sperance.exileforge.core.display.dayText
import com.sperance.exileforge.core.display.groupedNumber
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.display.stampText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ClassesFile
import com.sperance.exileforge.rules.content.ContestHonour
import com.sperance.exileforge.ui.components.ClassPortrait
import com.sperance.exileforge.ui.components.GoldPrice
import com.sperance.exileforge.ui.components.RoleMark
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.Tipped
import com.sperance.exileforge.ui.components.honourRing
import com.sperance.exileforge.ui.components.medalOf
import com.sperance.exileforge.ui.theme.*

/** Строка героя над текущим состоянием игры; до прочтения героя её нет. */
@Composable fun HeroLine(game: GameUi, modifier: Modifier = Modifier) {
    rememberHeroHeader(game)?.let { HeroLine(it, modifier) }
}

/**
 * Герой одной строкой: портрет, «имя · ур.», класс мелко, [trailing] справа и под ними полоса
 * опыта; касание имени и уровня - подсказка опыта до следующего уровня ([heroLevelTip]). Это и шапка игры на вкладке «Герой» ([com.sperance.exileforge.ui.ForgeBanner]):
 * [onPortrait] - портрет открывает аккаунт, как сигил, а справа - связь, конверт почты и меню. Золото
 * - справа под полосой опыта, в строке её подписи: длинное имя сокращается многоточием и не выталкивает ни значки, ни число золота,
 * длинная подпись опыта - тоже.
 *
 * Класс - база, от которой считается каждый процент; опыт - полоса во всю ширину, шапка игры не сворачивается. Титул состязаний
 * ([HeroHeaderState.honour]) встаёт на место класса цветом медали места со сроком «до 24 окт.», касание - подсказка со сроком; класс тогда
 * - в подсказке имени. Рамка портрета - медаль титула.
 */
@Composable fun HeroLine(
    hero: HeroHeaderState,
    modifier: Modifier = Modifier,
    onPortrait: (() -> Unit)? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val xp = experienceOf(hero.classes, hero.level, hero.experience)
    Column(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            val ring = hero.honour?.let { Modifier.border(2.dp, honourRing(it), CircleShape) } ?: Modifier
            ClassPortrait(hero.heroClass, hero.portraits, Modifier.size(38.dp).then(ring).then(onPortrait?.let { Modifier.clickable(onClickLabel = ui("nav.account"), onClick = it) } ?: Modifier), round = true)
            Tipped({ heroLevelTip(hero) }, Modifier.weight(1f)) {
                Column(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(hero.name, color = GoldBright, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                        RoleMark(hero.role, 14.dp)
                        Text("· " + ui("hero.line_level", hero.level), color = Parchment, style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                    }
                    val honour = hero.honour
                    if (honour == null) {
                        Text(classLine(hero), color = Muted, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    } else {
                        HonourLine(honour)
                    }
                }
            }
            trailing()
        }
        if (hero.classes != null) {
            Box(Modifier.fillMaxWidth().height(4.dp).background(PanelRaised, RoundedCornerShape(2.dp)).semantics { contentDescription = xp.label }) {
                Box(Modifier.fillMaxWidth(xp.fraction).fillMaxHeight().background(Brush.horizontalGradient(listOf(Gold, GoldBright)), RoundedCornerShape(2.dp)))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(xp.label, color = Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            GoldPrice(hero.money)
        }
    }
}

/** Класс героя словами; неизвестен - так и сказано. */
private fun classLine(hero: HeroHeaderState): String = hero.heroClass.takeIf { it.isNotBlank() }?.let(::classTitle) ?: ui("hero.unknown_class")

/** Титул состязаний на месте класса: «Охотник дня · до 24 окт.» цветом медали места; касание - подсказка со сроком титула и рамки. */
@Composable private fun HonourLine(honour: ContestHonour) {
    val title = contestTitle(honour)
    Tipped({ Tip(title, ui("contest.honour_tip", stampText(honour.until)), medalOf(honour.place)) }) {
        Text(
            ui("contest.honour_until", title, dayText(honour.until)),
            color = medalOf(honour.place),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Опыт внутри уровня: доля полосы и подпись; [progress] - набрано и нужно в уровне, null - масштаба нет или уровень последний. */
private class Experience(val fraction: Float, val label: String, val progress: LevelProgress? = null)

/** Опыт в уровне: набрано [gained] из [needed]; [left] - сколько осталось до следующего уровня. */
private class LevelProgress(val gained: Long, val needed: Long) {
    val left: Long get() = needed - gained
}

/**
 * Подсказка имени и уровня героя [hero]: уровень, опыт в уровне «набрано / нужно» и сколько осталось до следующего уровня; на последнем
 * уровне - что он последний. Без таблицы опыта - только уровень.
 */
internal fun heroLevelTip(hero: HeroHeaderState): Tip {
    val progress = experienceOf(hero.classes, hero.level, hero.experience).progress
    val steps = when {
        progress != null -> listOf(
            ui("hero.level_tip.xp") to "${groupedNumber(progress.gained)} / ${groupedNumber(progress.needed)}",
            ui("hero.level_tip.left", hero.level + 1) to groupedNumber(progress.left),
        )

        hero.classes != null -> listOf(ui("hero.level_tip.xp") to ui("hero.xp_last"))

        else -> emptyList()
    }
    val heroClass = listOfNotNull((ui("hero.level_tip.class") to classLine(hero)).takeIf { hero.honour != null })
    return Tip(hero.name, tint = GoldBright, facts = heroClass + listOf(ui("hero.level_tip.level") to "${hero.level}") + steps)
}

/** Доля полосы опыта в уровне [level] при опыте [experience] (0..1); без таблицы опыта [classes] или на последнем уровне - 1. */
internal fun experienceShare(classes: ClassesFile?, level: Int, experience: Double): Float = experienceOf(classes, level, experience).fraction

/**
 * Сколько пройдено в этом уровне. Таблица опыта классов говорит, сколько стоит следующий уровень, - клиент читает её, чтобы
 * показать, что впереди, а не чтобы считать уровень: уровень решает сервер. На последнем уровне полоса полна и говорит это;
 * без контента масштаба нет, и печатается только итог. Подпись - «1 240 / 2 950 · 42%».
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
    return Experience(fraction, label, next?.let { LevelProgress(inLevel.toLong(), span.toLong()) })
}
