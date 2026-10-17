package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.NodeSpot
import com.sperance.exileforge.core.display.itemIcon
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.icons.spriteVector

/**
 * The campaign's scene, drawn by Compose itself: the map in pseudo-isometry while walking — its ground,
 * rock and air in the biome's [MapStyle] — with
 * the hero and the monsters as round tokens of their portraits, and while fighting
 * the cave of the owner's mockup VI behind the fight's cards ([FightStage]). Everything is a shape — rule 17, no picture is ever loaded
 * — and nothing here is text: names, bars and numbers are the overlay's, in the app's dictionary.
 *
 * The run's clock is [rememberRunClock]: the scene only reads it, so the world is stepped once a frame and drawn by the
 * same frame, never seen half-moved. Reading [clock] in the draw block is what redraws it: only the drawing is repeated
 * each frame, never the composition.
 *
 * Слои земли и скалы берутся у окна и возвращаются ему, когда сцена уходит.
 */
@Composable fun ExpeditionScene(run: ExpeditionRun, clock: FloatState, classCode: String?, modifier: Modifier = Modifier) {
    val painter = remember { ScenePainter() }
    painter.layers.graphics = LocalGraphicsContext.current
    DisposableEffect(painter) { onDispose { painter.layers.release() } }
    painter.motion = LocalMotion.current
    painter.materials = run.world.features.filterIsInstance<NodeSpot>().map { it.node.job.output }.distinct().mapNotNull { code ->
        key(code) { itemIcon(code)?.let(::spriteVector)?.let { code to MaterialMark(rememberVectorPainter(it), materialTier(run.index, code)) } }
    }.toMap()
    Canvas(modifier) { painter.draw(this, run, clock.floatValue, classCode) }
}

/**
 * Часы захода: каждый кадр [ExpeditionRun.update] один раз; значение - секунды на экране. Живут отдельно от сцены:
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
