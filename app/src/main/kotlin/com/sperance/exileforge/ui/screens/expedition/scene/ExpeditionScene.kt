package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.ExpeditionMap
import com.sperance.exileforge.core.campaign.Tile
import com.sperance.exileforge.core.campaign.run.AgentMode
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.rules.content.MonsterRarity
import com.sperance.exileforge.ui.icons.drawToken
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.sin

/**
 * The campaign's scene, drawn by Compose itself: the map in pseudo-isometry while walking — its ground,
 * rock and air in the biome's [MapStyle] since 2.64.0 — with
 * the hero and the monsters as round tokens of their portraits (since 2.31.0), and while fighting
 * the cave of the owner's mockup VI behind the fight's cards (2.57.0, [fightBackdrop]). Everything is a shape — rule 17, no picture is ever loaded
 * — and nothing here is text: names, bars and numbers are the overlay's, in the app's dictionary.
 *
 * The run's clock is [rememberRunClock]: the scene only reads it, so the world is stepped once a frame and drawn by the
 * same frame, never seen half-moved. Reading [clock] in the draw block is what redraws it: only the drawing is repeated
 * each frame, never the composition.
 */
@Composable fun ExpeditionScene(run: ExpeditionRun, clock: FloatState, classCode: String?, modifier: Modifier = Modifier) {
    val painter = remember { ScenePainter() }
    Canvas(modifier) { painter.draw(this, run, clock.floatValue, classCode) }
}

/**
 * Часы захода: каждый кадр [ExpeditionRun.update] один раз; значение - секунды на экране. Живут отдельно от сцены (3.95.1):
 * в ленте автопробега сцены нет, и заход, которого никто не шагал, стоял на «Волна 0» без отклика на «Стоп».
 */
@Composable fun rememberRunClock(run: ExpeditionRun): FloatState {
    val clock = remember(run) { mutableFloatStateOf(0f) }
    LaunchedEffect(run) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                if (last != 0L) {
                    val dt = ((now - last) / 1e9).coerceAtMost(.05)
                    run.update(dt)
                    clock.floatValue += dt.toFloat()
                }
                last = now
            }
        }
    }
    return clock
}

/** Half a tile's width on screen; the tile is twice as wide as it is tall. */
internal val SCENE_UNIT = 30.dp

/**
 * Where a point of the scene of [size] falls on the map, in tiles, with the tile's half-width [unit] in pixels —
 * the inverse of the painter's camera, which keeps the hero a little below the middle (3.70.0).
 */
internal fun sceneToWorld(run: ExpeditionRun, at: Offset, size: IntSize, unit: Float): Pair<Double, Double> {
    val across = (at.x - size.width / 2f) / unit
    val down = (at.y - size.height * .55f - unit) * 2 / unit
    return run.world.heroX + (across + down) / 2 to run.world.heroY + (down - across) / 2
}
