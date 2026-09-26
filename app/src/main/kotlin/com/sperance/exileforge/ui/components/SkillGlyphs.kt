package com.sperance.exileforge.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.campaign.FlaskKind
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.skillIcon
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.skills.SkillDefinition
import com.sperance.exileforge.core.model.skills.SlotCondition
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.spriteVector
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.ManaBlue
import com.sperance.exileforge.ui.theme.Rune

/** A skill's drawing (2.78.0): the server's sprite it names, the grimoire's own mark without one. */
@Composable fun SkillGlyph(icon: String, modifier: Modifier = Modifier, tint: Color = Gold) =
    Icon(skillIcon(icon)?.let(::spriteVector) ?: ForgeGlyphs.Grimoire, null, tint = tint, modifier = modifier)

/** A flask's colour by what it brings: life red, mana blue, the rest a rune's. */
fun flaskTint(kind: FlaskKind): Color = when (kind) {
    FlaskKind.LIFE -> LifeRed
    FlaskKind.MANA -> ManaBlue
    FlaskKind.UTILITY -> Rune
}

/** When a slot fires or a flask is drunk by itself, in a few words. */
fun conditionTitle(condition: SlotCondition): String = ui("skills.condition.${condition.name}")

/** What a skill is, as a page of the grimoire heads it: its type and, for a blow of an element, the element. */
fun skillKindLine(skill: SkillDefinition): String {
    val element = skill.hit?.spell?.element ?: skill.hit?.element?.takeIf { skill.hit?.convert != null } ?: skill.dot?.element
    return listOfNotNull(ui("skills.type.${skill.type.name}"), element?.let(SkillText::element)).joinToString(" · ")
}
