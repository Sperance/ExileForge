package com.sperance.exileforge.ui.screens.skills

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.SkillGrowthView
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.ContentIndex
import com.sperance.exileforge.rules.content.HeroSkills
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.InspectAction
import com.sperance.exileforge.ui.components.InspectButton
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.NeonSkillIcon
import com.sperance.exileforge.ui.components.skillKindLine
import com.sperance.exileforge.ui.screens.expedition.Caption
import com.sperance.exileforge.ui.theme.*

/**
 * Что открыть в карточке умения героя (0.14.4, #25): умение [code]; [level] - какой уровень показать (null - изученный героем,
 * не изучено - первый); [condition] - условие ячейки, где оно стоит; [action] - действие места главной кнопкой карточки (взять
 * книгу главы), как у карточки предмета.
 */
data class SkillPeek(val code: String, val level: Int? = null, val condition: SlotCondition? = null, val action: InspectAction? = null)

/** Как открыть карточку умения отсюда; null - хозяина нет, касание ничего не открывает. */
val LocalSkillInspector = staticCompositionLocalOf<((SkillPeek) -> Unit)?> { null }

private val NO_PEEK: (SkillPeek) -> Unit = {}

/** Открыватель карточки умения из [LocalSkillInspector]; без хозяина - пустой. */
@Composable fun rememberSkillInspect(): (SkillPeek) -> Unit = LocalSkillInspector.current ?: NO_PEEK

/**
 * Хозяин карточек умений (0.14.4, #25, макет «Карточка умения», вариант A): любое умение героя где угодно - выбор книги главы,
 * выбор класса, экран уровня, ячейки и выбор Гримуара, обмен книг, книга в сумке - открывает здесь одну карточку ([SkillCard]);
 * лист один, поверх. Где касание уже выбирает (ячейка, обмен), карточку открывает долгое нажатие.
 */
@Composable fun SkillInspectorHost(game: GameUi, content: @Composable () -> Unit) {
    var open by remember { mutableStateOf<SkillPeek?>(null) }
    val opener: (SkillPeek) -> Unit = remember { { open = it } }
    CompositionLocalProvider(LocalSkillInspector provides opener) { content() }
    val peek = open ?: return
    val index = game.index ?: return
    val close = { open = null }
    ForgeSheet(onDismissRequest = close) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SkillCard(game, index, peek.code, peek.level, peek.condition)
            peek.action?.let { InspectButton(it, close) }
        }
    }
}

/**
 * Карточка умения [code] глазами героя игры: значок с тиром, имя, вид и тир, уровень; что оно делает на уровне [level] (null -
 * изученном, неизученное - на первом) с ценой и темпом по листу героя, затем - на следующем уровне. Без героя (выбор класса) -
 * первый уровень по пустому листу. Её читают хозяин карточек и лист умения в бою.
 */
@Composable internal fun SkillCard(game: GameUi, index: ContentIndex, code: String, level: Int?, condition: SlotCondition? = null) {
    val skill = index.skills.byCode[code] ?: return MutedText(ui("common.loading"))
    val hero = game.hero
    val skills = hero?.skills ?: NO_SKILLS
    val stats = hero?.stats.orEmpty()
    val heroLevel = hero?.level ?: 1
    val view = remember(skill, skills, heroLevel, stats) { SkillGrowthView.of(index, skill, skills, heroLevel, stats) }
    val forged = remember(skill, skills, stats) { index.skillForge.effective(skill, skills, stats) }
    val shown = (level ?: view.level).coerceAtLeast(1)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NeonSkillIcon(skill, view.tier, 48.dp, dim = view.level == 0)
        Column(Modifier.weight(1f)) {
            Text(SkillText.title(skill.code), color = GoldBright, style = MaterialTheme.typography.titleLarge)
            Text(skillKindLine(skill) + " · " + tierLine(view), color = Muted, style = MaterialTheme.typography.labelMedium)
        }
        Text(if (view.level > 0) ui("skills.level_short", shown) else "—", color = Gold, style = MaterialTheme.typography.titleLarge)
    }
    SkillFacts(index, forged, shown, stats, heroLevel, condition = condition)
    if (shown < view.cap) {
        Caption(ui("skills.at_level", shown + 1))
        SkillFacts(index, forged, shown + 1, stats, heroLevel, ModBlue.copy(alpha = .75f))
    }
}

/** Умения героя, которого ещё нет (выбор класса): ничего не изучено. */
private val NO_SKILLS = HeroSkills()
