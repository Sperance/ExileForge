package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Окраска метки: слова [ink], подложка [fill], рамка [frame] (null - без рамки). Обычно - от одного тона ([TagLook.tone]); своя
 * окраска - для мест со своей подложкой (капсулы Разлома, тёмная плашка добычи).
 */
@Immutable
data class TagTone(val ink: Color, val fill: Color, val frame: Color?)

/** Вид метки одного тона: окантованная (тихая подложка и рамка), залитая (подложка гуще, слова жирнее, без рамки), рамка без подложки. */
enum class TagLook(private val fill: Float, private val frame: Float?) {
    Outlined(.1f, .4f),
    Filled(.15f, null),
    Framed(0f, .35f),
    ;

    /** Окраска вида тоном [tint]. */
    fun tone(tint: Color): TagTone = TagTone(tint, tint.copy(alpha = fill), frame?.let { tint.copy(alpha = it) })

    /** Залитая метка пишет слова жирно: без рамки её край - лишь подложка. */
    internal val bold: Boolean get() = this == Filled
}

/** Размер метки: мелкая - подпись строки, средняя - метка рядом с обычным текстом. Поля - [padding], слова - [style]. */
enum class TagSize(internal val padding: PaddingValues) {
    Small(PaddingValues(horizontal = 6.dp, vertical = 2.dp)),
    Medium(PaddingValues(horizontal = 8.dp, vertical = 3.dp)),
    ;

    /** Начертание слов метки этого размера. */
    val style: TextStyle
        @Composable @ReadOnlyComposable get() = when (this) {
            Small -> MaterialTheme.typography.labelSmall
            Medium -> MaterialTheme.typography.labelMedium
        }
}

/**
 * Метка-плашка: слова [text] на подложке своего тона ([tone]), одна строка, длинное - многоточием. Не кнопка, пока нет [onClick]; с ним -
 * нажимается всей зоной касания 44 dp, рисуется только плашка, [onClickLabel] - действие для чтения с экрана. [leading] - знак перед
 * словами (значок, глиф) цветом метки. Начертание - [style] (по умолчанию - размера [size]), жирность - [bold].
 */
@Composable fun ToneTag(
    text: String,
    tone: TagTone,
    modifier: Modifier = Modifier,
    size: TagSize = TagSize.Small,
    style: TextStyle = size.style,
    bold: Boolean = false,
    leading: (@Composable RowScope.() -> Unit)? = null,
    onClickLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    if (onClick == null) {
        TagPlate(text, tone, modifier, size, style, bold, leading)
        return
    }
    Box(modifier.touchOverhang().clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick), contentAlignment = Alignment.Center) {
        TagPlate(text, tone, Modifier, size, style, bold, leading)
    }
}

/** Сама плашка метки: подложка, рамка и слова; зона касания - у [ToneTag]. */
@Composable private fun TagPlate(text: String, tone: TagTone, modifier: Modifier, size: TagSize, style: TextStyle, bold: Boolean, leading: (@Composable RowScope.() -> Unit)?) {
    Row(
        modifier.clip(TagShape).background(tone.fill, TagShape).then(tone.frame?.let { Modifier.border(1.dp, it, TagShape) } ?: Modifier).padding(size.padding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
    ) {
        leading?.invoke(this)
        Text(text, color = tone.ink, style = style, fontWeight = if (bold) FontWeight.Bold else null, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

/** Метка вида [look] одного тона [tint] - обычный случай [ToneTag]: залитая пишет жирно. */
@Composable fun ToneTag(
    text: String,
    tint: Color,
    modifier: Modifier = Modifier,
    look: TagLook = TagLook.Outlined,
    size: TagSize = TagSize.Small,
    leading: (@Composable RowScope.() -> Unit)? = null,
    onClickLabel: String? = null,
    onClick: (() -> Unit)? = null,
) {
    ToneTag(text, look.tone(tint), modifier, size, bold = look.bold, leading = leading, onClickLabel = onClickLabel, onClick = onClick)
}

/** Скругление всех меток. */
private val TagShape = RoundedCornerShape(6.dp)
