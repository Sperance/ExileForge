package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Abyss
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Vital

/**
 * Кнопка ряда поверх холста ([CanvasToolbar]): значок [icon] с обязательным именем [label] для чтения с экрана и действием [onClick].
 * [enabled] - доступна ли (погашенная - штриховкой и приглушённым значком), [tint] - свой цвет значка (иначе - светлый ряда),
 * [badge] - счётчик в углу, [state] - состояние для чтения с экрана, [marks] - знаки поверх кнопки (кольцо прогресса, точка),
 * [anchored] - всплывающее у кнопки (меню «⋮»).
 */
@Immutable
class CanvasTool(
    val icon: ImageVector,
    val label: String,
    val enabled: Boolean,
    val onClick: () -> Unit,
    val tint: Color? = null,
    val badge: Int? = null,
    val state: String? = null,
    val marks: (DrawScope.() -> Unit)? = null,
    val anchored: (@Composable () -> Unit)? = null,
)

/**
 * Ряд кнопок поверх холста (карта мира, Атлас, древо умений, мини-карта, меню забега): значки 44×44 dp в одну строку на тёмной
 * капсуле с окантовкой цветом мира [accent], между соседями - тонкий шов. Ряд короче, когда кнопок меньше: место держит тот, кто
 * ставит капсулу (выравнивание по правому краю - последняя кнопка не сдвигается).
 */
@Composable fun CanvasToolbar(tools: List<CanvasTool>, modifier: Modifier = Modifier, accent: Color = Gold) {
    if (tools.isEmpty()) return
    Row(
        modifier.background(Abyss.copy(alpha = CAPSULE_ALPHA), CapsuleShape).border(1.dp, accent.copy(alpha = RIM_ALPHA), CapsuleShape)
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tools.forEachIndexed { i, tool ->
            if (i > 0) Box(Modifier.width(1.dp).height(SEAM_HEIGHT).background(accent.copy(alpha = SEAM_ALPHA)))
            key(tool.label) { ToolButton(tool) }
        }
    }
}

/** Пара масштаба для [CanvasToolbar]: «−» ([onZoomOut], пока [canZoomOut]) и «+» ([onZoomIn], пока [canZoomIn]) - в этом порядке. */
fun zoomTools(canZoomOut: Boolean, canZoomIn: Boolean, onZoomOut: () -> Unit, onZoomIn: () -> Unit): List<CanvasTool> = listOf(
    CanvasTool(ForgeGlyphs.Minus, ui("expedition.zoom_out"), canZoomOut, onZoomOut),
    CanvasTool(ForgeGlyphs.Plus, ui("expedition.zoom_in"), canZoomIn, onZoomIn),
)

/** Кнопка капсулы: значок без фона; погашенная - штриховка под значком, а не только цвет; знаки [CanvasTool.marks] - поверх. */
@Composable private fun ToolButton(tool: CanvasTool) {
    Box {
        ForgeIconButton(
            tool.icon,
            tool.label,
            onClick = tool.onClick,
            modifier = Modifier.offHatch(!tool.enabled).toolMarks(tool.marks).semantics { tool.state?.let { stateDescription = it } },
            state = ButtonState.of(tool.enabled),
            accent = tool.tint ?: GoldBright,
            badge = tool.badge,
            badgeColor = Vital,
        )
        tool.anchored?.invoke()
    }
}

/** Штриховка погашенной кнопки: косые линии в круге под значком. */
private fun Modifier.offHatch(on: Boolean): Modifier = if (!on) this else clipToBounds().drawWithCache {
    val r = HATCH_RADIUS.toPx()
    val centre = Offset(size.width / 2, size.height / 2)
    val disc = Path().apply { addOval(Rect(centre, r)) }
    val step = HATCH_STEP.toPx()
    val width = 1.dp.toPx()
    onDrawBehind {
        clipPath(disc) {
            var x = centre.x - 2 * r
            while (x < centre.x + 2 * r) {
                drawLine(HATCH_INK, Offset(x, centre.y + r), Offset(x + 2 * r, centre.y - r), width)
                x += step
            }
        }
    }
}

/** Знаки [marks] поверх кнопки. */
private fun Modifier.toolMarks(marks: (DrawScope.() -> Unit)?): Modifier = if (marks == null) this else drawWithContent {
    drawContent()
    marks()
}

private val CapsuleShape = RoundedCornerShape(50)
private val SEAM_HEIGHT = 22.dp
private val HATCH_RADIUS = 11.dp
private val HATCH_STEP = 4.dp
private val HATCH_INK = Muted.copy(alpha = .35f)
private const val CAPSULE_ALPHA = .86f
private const val RIM_ALPHA = .4f
private const val SEAM_ALPHA = .28f
