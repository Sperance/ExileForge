package com.sperance.exileforge.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.display.ItemVisualKind

/** Original vector emblems, bundled and available offline for every item category. */
@Composable fun ItemEmblem(kind: ItemVisualKind, tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.size(72.dp).clip(RoundedCornerShape(18.dp))
        .background(Brush.radialGradient(listOf(tint.copy(alpha = .22f), Color(0xFF111B23))))
        .semantics { contentDescription = "Иконка: ${kind.name}" }) {
        scale(size.width / 80f, size.height / 80f, pivot = Offset.Zero) {
            fun line(x: Float, y: Float, xx: Float, yy: Float, width: Float = 2.5f) = drawLine(tint, Offset(x,y), Offset(xx,yy), width, StrokeCap.Round)
            fun shape(vararg points: Pair<Float, Float>) {
                val p = Path().apply { moveTo(points[0].first, points[0].second); points.drop(1).forEach { lineTo(it.first,it.second) }; close() }
                drawPath(p, tint.copy(alpha = .15f)); drawPath(p, tint, style = Stroke(2.5f, join = StrokeJoin.Round))
            }
            drawCircle(tint.copy(alpha = .13f), 31f, Offset(40f,40f), style = Stroke(1f))
            when(kind) {
                ItemVisualKind.SWORD -> { shape(53f to 16f, 62f to 18f, 60f to 27f, 34f to 53f, 27f to 46f); line(24f,42f,38f,56f); line(29f,51f,19f,62f,5f) }
                ItemVisualKind.BOW -> { val p=Path().apply { moveTo(25f,14f); quadraticTo(73f,40f,25f,66f) }; drawPath(p,tint,style=Stroke(3f)); line(25f,14f,25f,66f,1.5f); line(17f,40f,63f,40f); line(57f,34f,63f,40f); line(57f,46f,63f,40f) }
                ItemVisualKind.STAFF -> { line(27f,64f,49f,24f,4f); shape(45f to 13f,58f to 15f,59f to 27f,47f to 33f,39f to 25f) }
                ItemVisualKind.HELMET -> { shape(20f to 58f,20f to 34f,30f to 20f,50f to 20f,60f to 34f,60f to 58f,47f to 52f,40f to 61f,33f to 52f); line(40f,23f,40f,48f); line(27f,39f,35f,42f); line(45f,42f,53f,39f) }
                ItemVisualKind.ARMOR -> { shape(18f to 26f,30f to 18f,35f to 28f,45f to 28f,50f to 18f,62f to 26f,55f to 39f,52f to 63f,28f to 63f,25f to 39f); line(40f,31f,40f,59f); line(29f,43f,51f,43f) }
                ItemVisualKind.GLOVES -> { shape(28f to 61f,21f to 40f,24f to 34f,31f to 41f,31f to 20f,37f to 20f,38f to 38f,41f to 17f,47f to 18f,46f to 38f,51f to 23f,57f to 25f,53f to 48f,49f to 61f); line(29f,54f,50f,54f) }
                ItemVisualKind.BOOTS -> { shape(29f to 17f,50f to 17f,47f to 47f,61f to 55f,61f to 63f,24f to 63f,24f to 52f); line(31f,26f,47f,26f); line(30f,34f,46f,34f); line(28f,55f,47f,55f) }
                ItemVisualKind.RING -> { drawCircle(tint,18f,Offset(40f,47f),style=Stroke(4f)); shape(40f to 13f,52f to 25f,40f to 38f,28f to 25f); line(33f,24f,46f,24f,1.5f) }
                ItemVisualKind.AMULET -> { val p=Path().apply { moveTo(22f,16f); quadraticTo(15f,44f,40f,42f); quadraticTo(65f,44f,58f,16f) };drawPath(p,tint,style=Stroke(2f));shape(40f to 36f,52f to 50f,40f to 67f,28f to 50f) }
                ItemVisualKind.BELT -> { shape(15f to 30f,65f to 30f,65f to 52f,15f to 52f);shape(30f to 26f,50f to 26f,50f to 56f,30f to 56f);line(40f,41f,57f,41f) }
                ItemVisualKind.SHIELD -> { shape(40f to 15f,61f to 24f,57f to 49f,40f to 66f,23f to 49f,19f to 24f);line(40f,22f,40f,56f);line(28f,34f,52f,34f) }
                ItemVisualKind.WINGS -> { shape(37f to 59f,16f to 34f,17f to 16f,35f to 33f);shape(43f to 59f,64f to 34f,63f to 16f,45f to 33f) }
                ItemVisualKind.CHARACTER -> { drawCircle(tint,11f,Offset(40f,28f),style=Stroke(3f)); val p=Path().apply { moveTo(20f,63f); cubicTo(20f,35f,60f,35f,60f,63f) };drawPath(p,tint,style=Stroke(3f)) }
                ItemVisualKind.CURRENCY -> { drawCircle(tint,22f,Offset(40f,40f),style=Stroke(3f));drawCircle(tint.copy(alpha=.5f),16f,Offset(40f,40f),style=Stroke(1f));shape(40f to 26f,49f to 40f,40f to 54f,31f to 40f) }
                ItemVisualKind.ITEM -> { shape(40f to 16f,59f to 30f,52f to 58f,28f to 58f,21f to 30f);line(21f,30f,59f,30f);line(40f,16f,32f,30f);line(32f,30f,40f,59f) }
            }
        }
    }
}
