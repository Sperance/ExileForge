package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.update.ReleaseKind
import com.sperance.exileforge.core.update.ReleaseNotes
import com.sperance.exileforge.rules.content.Rarity
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.RelicLook
import com.sperance.exileforge.ui.components.nameStyle
import com.sperance.exileforge.ui.components.relicLook
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment

/**
 * Карточка релиза «Что нового» (4.0.1, вариант «Кованые рамы», утверждён владельцем): вес релиза задаёт орнамент, без
 * анимаций и без подписи веса. Фикс - простая плита, обновление (`X.Y.0`) - угольная плита в кованой рамке, крупное
 * (`X.0.0`) - двойная рама с филигранью и медальонами. Касание раскрывает заметки.
 */
@Composable fun ReleaseCard(release: ReleaseNotes, open: Boolean, onToggle: () -> Unit) {
    val ornament = Ornament.of(release.kind)
    Column(
        Modifier.fillMaxWidth()
            .background(ornament.ground, SHAPE)
            .drawWithContent {
                drawContent()
                ornament.frame(this)
            }
            .clickable(onClick = onToggle)
            .padding(horizontal = ornament.inset, vertical = ornament.inset - 2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ornament.dot?.let { Box(Modifier.size(7.dp).background(it, CircleShape)) }
            Text(ui("app.version", release.version), style = ornament.versionStyle(), modifier = Modifier.weight(1f))
        }
        release.published?.take(10)?.let { MutedText(it, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 2.dp)) }
        if (open) {
            ornament.Divider()
            release.body.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("## ") }.forEach { line ->
                Row(Modifier.padding(top = 5.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(Modifier.padding(top = 5.dp).size(9.dp)) { ornament.bullet(this) }
                    Text(line.removePrefix("- ").removePrefix("* "), color = ornament.text, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private val SHAPE = RoundedCornerShape(12.dp)

/** Орнамент веса релиза: подложка, рамка поверх содержимого, разделитель над заметками, значок строки и цвета текста. */
private sealed class Ornament {
    abstract val ground: Brush
    abstract val text: Color
    abstract val inset: Dp
    open val dot: Color? = null

    abstract fun frame(scope: DrawScope)
    abstract fun bullet(scope: DrawScope)

    @Composable abstract fun versionStyle(): TextStyle

    @Composable open fun Divider() = Unit

    /** Фикс: плита басальта в бронзовой нити, точка-значок. */
    object Plain : Ornament() {
        override val ground: Brush = Brush.verticalGradient(listOf(Panel, Panel))
        override val text = Parchment
        override val inset = 13.dp

        override fun frame(scope: DrawScope) = with(scope) {
            drawRoundRect(Bronze, cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(1.dp.toPx()))
        }

        override fun bullet(scope: DrawScope) = with(scope) { drawCircle(Muted, radius = 2.dp.toPx()) }

        @Composable override fun versionStyle() = MaterialTheme.typography.titleSmall.copy(color = Parchment, fontWeight = FontWeight.SemiBold)
    }

    /** Обновление: угольная плита уникальной вещи, кованые уголки с заклёпкой, черта внизу, ромб-разделитель. */
    object Forged : Ornament() {
        private val look: RelicLook = relicLook(Rarity.UNIQUE)
        override val ground: Brush = Brush.verticalGradient(listOf(look.top, look.bottom))
        override val text = Color(0xFFF3DCC6)
        override val inset = 15.dp
        override val dot: Color = look.gold

        override fun frame(scope: DrawScope) = with(scope) {
            val px = 1.dp.toPx()
            drawRoundRect(look.gold.copy(alpha = .45f), cornerRadius = CornerRadius(12.dp.toPx()), style = Stroke(px))
            corners { bracket(look.gold, look.accent) }
            drawLine(look.rarity, Offset(size.width * .15f, size.height - px / 2), Offset(size.width * .85f, size.height - px / 2), px)
        }

        override fun bullet(scope: DrawScope) = with(scope) { drawPath(diamond(center, size.minDimension * .45f), look.rarity) }

        @Composable override fun versionStyle() = look.nameStyle(16)

        @Composable override fun Divider() = RuleDivider(look.rarity.copy(alpha = .6f)) { drawPath(diamond(Offset.Zero, 4.dp.toPx()), look.gold) }

        /** Кованый уголок: скоба с закруглением и заклёпка на изгибе. */
        private fun DrawScope.bracket(gold: Color, rivet: Color) {
            val a = 5.dp.toPx()
            val b = 9.dp.toPx()
            val c = 22.dp.toPx()
            val path = Path().apply {
                moveTo(a, c)
                lineTo(a, b)
                quadraticTo(a, a, b, a)
                lineTo(c, a)
            }
            drawPath(path, gold, style = Stroke(1.6.dp.toPx()))
            drawCircle(rivet, radius = 1.8.dp.toPx(), center = Offset(a, a))
        }
    }

    /** Крупное: звёздная плита мифической вещи, двойная рама, филигрань по углам, медальоны сверху и снизу, звезда-разделитель. */
    object Filigree : Ornament() {
        private val look: RelicLook = relicLook(Rarity.MYTHICAL)
        override val ground: Brush = Brush.verticalGradient(listOf(look.top, look.bottom))
        override val text = Color(0xFFE9E2F5)
        override val inset = 18.dp
        override val dot: Color = look.gold

        override fun frame(scope: DrawScope) = with(scope) {
            // Мягкое сияние сверху - неподвижное
            drawRect(Brush.radialGradient(listOf(Color(0xFF3A2560), Color.Transparent), center = Offset(size.width / 2, 0f), radius = size.width * .7f))
            val radius = 12.dp.toPx()
            drawRoundRect(look.gold, cornerRadius = CornerRadius(radius), style = Stroke(1.4.dp.toPx()))
            val inner = 6.dp.toPx()
            drawRoundRect(
                look.gold.copy(alpha = .35f),
                topLeft = Offset(inner, inner),
                size = Size(size.width - inner * 2, size.height - inner * 2),
                cornerRadius = CornerRadius(radius - inner / 2),
                style = Stroke(1.dp.toPx()),
            )
            corners { scroll() }
            medallion(top = true)
            medallion(top = false)
        }

        override fun bullet(scope: DrawScope) = with(scope) { drawPath(star(center, size.minDimension / 2), look.gold) }

        @Composable override fun versionStyle() = look.nameStyle(19)

        @Composable override fun Divider() = RuleDivider(look.gold.copy(alpha = .7f)) {
            drawCircle(look.gold.copy(alpha = .5f), radius = 7.dp.toPx(), style = Stroke(1.dp.toPx()))
            drawPath(star(Offset.Zero, 4.5.dp.toPx()), look.name)
        }

        /** Филигрань угла: два завитка, сапфир на перекрестье и золотой треугольник в самом углу. */
        private fun DrawScope.scroll() {
            fun d(v: Float) = v.dp.toPx()
            val outer = Path().apply {
                moveTo(d(6f), d(34f))
                cubicTo(d(6f), d(18f), d(10f), d(12f), d(18f), d(10f))
                cubicTo(d(14f), d(16f), d(16f), d(22f), d(22f), d(22f))
                cubicTo(d(22f), d(16f), d(28f), d(10f), d(34f), d(6f))
            }
            drawPath(outer, look.gold, style = Stroke(d(1.4f)))
            val inner = Path().apply {
                moveTo(d(12f), d(28f))
                cubicTo(d(14f), d(22f), d(20f), d(18f), d(26f), d(16f))
            }
            drawPath(inner, look.name.copy(alpha = .6f), style = Stroke(d(1f)))
            drawCircle(look.accent, radius = d(2.4f), center = Offset(d(18f), d(18f)))
            val tip = Path().apply {
                moveTo(d(6f), d(6f))
                lineTo(d(14f), d(6f))
                lineTo(d(6f), d(14f))
                close()
            }
            drawPath(tip, look.gold)
        }

        /** Медальон на кромке: клин с сапфиром сверху, клин снизу. */
        private fun DrawScope.medallion(top: Boolean) {
            val w = 10.dp.toPx()
            val h = 9.dp.toPx()
            val y = if (top) 0f else size.height
            val tip = if (top) h else -h
            val cx = size.width / 2
            val wedge = Path().apply {
                moveTo(cx - w, y)
                lineTo(cx, y + tip)
                lineTo(cx + w, y)
                close()
            }
            drawPath(wedge, look.bottom)
            drawPath(wedge, look.gold, style = Stroke(1.4.dp.toPx()))
            if (top) drawCircle(look.accent, radius = 2.dp.toPx(), center = Offset(cx, y + tip * .4f))
        }
    }

    companion object {
        fun of(kind: ReleaseKind): Ornament = when (kind) {
            ReleaseKind.MAJOR -> Filigree
            ReleaseKind.MINOR -> Forged
            ReleaseKind.PATCH -> Plain
        }
    }
}

/** Рисунок [draw] уголка в каждом из четырёх углов: левый верхний как есть, прочие - отражением. */
private inline fun DrawScope.corners(crossinline draw: DrawScope.() -> Unit) {
    listOf(1f to 1f, -1f to 1f, 1f to -1f, -1f to -1f).forEach { (sx, sy) ->
        translate(if (sx < 0) size.width else 0f, if (sy < 0) size.height else 0f) {
            scale(sx, sy, pivot = Offset.Zero) { draw() }
        }
    }
}

/** Разделитель над заметками: две черты цвета [line] и знак [mark] между ними. */
@Composable private fun RuleDivider(line: Color, mark: DrawScope.() -> Unit) {
    Canvas(Modifier.fillMaxWidth().padding(top = 8.dp).height(14.dp)) {
        val y = size.height / 2
        val gap = 14.dp.toPx()
        drawLine(line, Offset(size.width * .06f, y), Offset(size.width / 2 - gap, y), 1.dp.toPx())
        drawLine(line, Offset(size.width / 2 + gap, y), Offset(size.width * .94f, y), 1.dp.toPx())
        translate(size.width / 2, y) { mark() }
    }
}

/** Ромб с полудиагональю [r] вокруг [c]. */
private fun diamond(c: Offset, r: Float): Path = Path().apply {
    moveTo(c.x, c.y - r)
    lineTo(c.x + r, c.y)
    lineTo(c.x, c.y + r)
    lineTo(c.x - r, c.y)
    close()
}

/** Четырёхлучевая звезда радиуса [r] вокруг [c]. */
private fun star(c: Offset, r: Float): Path {
    val k = r * .24f
    return Path().apply {
        moveTo(c.x, c.y - r)
        lineTo(c.x + k, c.y - k)
        lineTo(c.x + r, c.y)
        lineTo(c.x + k, c.y + k)
        lineTo(c.x, c.y + r)
        lineTo(c.x - k, c.y + k)
        lineTo(c.x - r, c.y)
        lineTo(c.x - k, c.y - k)
        close()
    }
}
