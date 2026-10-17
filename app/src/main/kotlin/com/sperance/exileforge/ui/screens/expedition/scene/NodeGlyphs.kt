package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import com.sperance.exileforge.core.campaign.run.NodeArt
import com.sperance.exileforge.core.campaign.run.NodeStroke
import com.sperance.exileforge.core.campaign.run.PathOp
import com.sperance.exileforge.core.campaign.run.StrokeKind
import com.sperance.exileforge.rules.content.NodeLook

/**
 * Художник узлов сбора неоновым глифом: тело - тёмная заливка с цветным контуром, под контуром - широкий бледный ореол того же
 * цвета; свечение - светлая заливка, линия - цветной штрих с ореолом, дыра - тень. Рисунок облика - из реестра ядра ([NodeArt]),
 * собранный узел - его собранный рисунок. Контуры и кисти строятся раз на облик и состояние; свет клетки гасит яркость.
 */
internal class NodeGlyphs {
    private val drawn = HashMap<NodeLook, Pair<List<Glyph>, List<Glyph>>>()

    /** Часть рисунка, готовая к кадру: контур, цвета и штрихи. */
    private class Glyph(stroke: NodeStroke) {
        val kind = stroke.kind
        val path = Path().apply { stroke.path.forEach { trace(it) } }
        val color = Color(NodeArt.shade(stroke.color, kind.lift).toInt())
        val alpha = if (kind == StrokeKind.GLOW && stroke.alpha < FAINT) stroke.alpha * FAINT_DIM else stroke.alpha
        val line = Stroke(if (kind == StrokeKind.LINE) stroke.width else RIM, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val aura = Stroke(line.width + AURA, cap = StrokeCap.Round, join = StrokeJoin.Round)

        private fun Path.trace(op: PathOp) {
            when (op) {
                is PathOp.Move -> moveTo(op.x, op.y)
                is PathOp.Line -> lineTo(op.x, op.y)
                is PathOp.Quad -> quadraticTo(op.x1, op.y1, op.x, op.y)
                is PathOp.Cubic -> cubicTo(op.x1, op.y1, op.x2, op.y2, op.x, op.y)
                PathOp.Close -> close()
            }
        }
    }

    /** Узел облика [look] на точке пола ([cx], [cy]) пера сцены (y вверх) при полуширине клетки [unit] и свете [light]. */
    fun draw(scope: DrawScope, look: NodeLook, spent: Boolean, cx: Float, cy: Float, unit: Float, light: Float) {
        val (full, gathered) = drawn.getOrPut(look) { NodeArt.of(look).let { art -> art.full.map(::Glyph) to art.gathered.map(::Glyph) } }
        val k = unit / (NodeArt.TILE_WIDTH / 2)
        scope.withTransform({
            translate(cx - NodeArt.FLOOR_X * k, -cy - NodeArt.FLOOR_Y * k)
            scale(k, k, Offset.Zero)
        }) {
            drawOval(Color.Black.copy(alpha = SHADOW * light), SHADOW_TOP_LEFT, SHADOW_SIZE)
            (if (spent) gathered else full).forEach { glyph -> paint(glyph, light) }
        }
    }

    private fun DrawScope.paint(glyph: Glyph, light: Float) {
        val color = tone(glyph.color, light)
        when (glyph.kind) {
            StrokeKind.BODY -> {
                drawPath(glyph.path, color.copy(alpha = AURA_ALPHA * light), style = glyph.aura)
                drawPath(glyph.path, FILL.copy(alpha = FILL_ALPHA))
                drawPath(glyph.path, color, style = glyph.line)
            }

            StrokeKind.GLOW -> drawPath(glyph.path, color.copy(alpha = glyph.alpha * light))

            StrokeKind.LINE -> {
                drawPath(glyph.path, color.copy(alpha = AURA_ALPHA * light), style = glyph.aura)
                drawPath(glyph.path, color, style = glyph.line)
            }

            StrokeKind.HOLE -> drawPath(glyph.path, Color.Black.copy(alpha = HOLE * light))
        }
    }

    private companion object {
        /** Тёмная заливка тела глифа. */
        val FILL = Color(0xFF0B1116)
        const val FILL_ALPHA = .9f

        /** Контур тела и ширина ореола сверх штриха - в точках клетки рисунка. */
        const val RIM = 1f
        const val AURA = 2.6f
        const val AURA_ALPHA = .22f

        /** Бледное свечение (ореол облика) гаснет ещё, чтобы не застить рисунок. */
        const val FAINT = .5f
        const val FAINT_DIM = .8f
        const val HOLE = .75f
        const val SHADOW = .5f
        val SHADOW_TOP_LEFT = Offset(21f, 58.5f)
        val SHADOW_SIZE = Size(38f, 10f)

        /** Насколько светлее цвета части её контур, свечение и линия. */
        val StrokeKind.lift: Double
            get() = when (this) {
                StrokeKind.BODY -> .3
                StrokeKind.GLOW -> .15
                StrokeKind.LINE -> .25
                StrokeKind.HOLE -> 0.0
            }
    }
}
