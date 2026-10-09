package com.sperance.exileforge.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sperance.exileforge.core.display.SkillText
import com.sperance.exileforge.core.display.skillIcon
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.SkillDefinition
import com.sperance.exileforge.rules.content.SlotCondition
import com.sperance.exileforge.rules.sheet.FlaskKind
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.NeonSprite
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.ManaBlue
import com.sperance.exileforge.ui.theme.Rune

/** A skill's drawing (2.78.0): the server's sprite it names - с 4.4.0 неоновым контуром, - the grimoire's own mark without one. */
@Composable fun SkillGlyph(icon: String, modifier: Modifier = Modifier, tint: Color = Gold) {
    if (!NeonSprite(skillIcon(icon), tint, modifier)) Icon(ForgeGlyphs.Grimoire, null, tint = tint, modifier = modifier)
}

/** A flask's colour by what it brings: life red, mana blue, the rest a rune's. */
fun flaskTint(kind: FlaskKind): Color = when (kind) {
    FlaskKind.LIFE -> LifeRed
    FlaskKind.MANA -> ManaBlue
    FlaskKind.UTILITY -> Rune
}

/** When a slot fires or a flask is drunk by itself, in a few words. */
fun conditionTitle(condition: SlotCondition): String = ui("skills.condition.${condition.name}")

/** What a skill is, as a page of the grimoire heads it: its type and, for a blow of an element, the element. */
fun skillKindLine(skill: SkillDefinition): String = listOfNotNull(ui("skills.type.${skill.type.name}"), skill.element?.let(SkillText::element)).joinToString(" · ")
