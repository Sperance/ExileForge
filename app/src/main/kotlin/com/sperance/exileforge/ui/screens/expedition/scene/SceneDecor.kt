package com.sperance.exileforge.ui.screens.expedition.scene

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * What lies on the ground, by biome (since 2.32.0): the seed says which of three kinds grows on
 * a tile, the biome what that kind is — a shell on the shore, a skull in the crypt, embers in the ash.
 */
internal fun ScenePainter.decor(map: ExpeditionMap, x: Int, y: Int, palette: Palette, biome: String, light: Float) {
    val kind = map.decorAt(x, y)
    if (kind == 0) return
    val cx = isoX(x + .5, y + .5)
    val cy = isoY(x + .5, y + .5)
    val u = unit / 6
    val base = shade(palette.decor, x, y, .15f, light = light)
    val glowing = palette.accent.copy(alpha = (.35f + .2f * sin(time * 2f + x)) * light)
    pen.color = base
    fun stone() = pen.ellipse(cx - u * 1.6f, cy - u * .6f, u * 3.2f, u * 1.6f)
    fun tuft() = (-1..1).forEach { i -> pen.triangle(cx + i * u - u * .4f, cy, cx + i * u + u * .4f, cy, cx + i * u * 1.4f, cy + u * 2.6f) }
    fun shard(color: Color = glowing) {
        pen.triangle(cx - u * .7f, cy, cx + u * .7f, cy, cx, cy + u * 3.4f)
        pen.color = color
        pen.circle(cx, cy + u * 1.2f, u * 1.4f)
    }
    fun puddle() {
        pen.color = tone(palette.accent, .5f * light, alpha = .55f)
        pen.ellipse(cx - u * 2.4f, cy - u * .9f, u * 4.8f, u * 1.8f)
        pen.color = palette.accent.copy(alpha = .25f * light)
        pen.ellipse(cx - u * .8f, cy - u * .2f, u * 1.2f, u * .5f)
    }
    fun bones() {
        pen.line(cx - u * 1.8f, cy - u * .4f, cx + u * 1.8f, cy + u * .4f, u * .5f)
        pen.circle(cx - u * 1.8f, cy - u * .4f, u * .45f)
        pen.circle(cx + u * 1.8f, cy + u * .4f, u * .45f)
        pen.circle(cx + u * .2f, cy + u * 1.2f, u * .9f)
    }
    fun flame() {
        pen.color = tone(palette.wallSide, light)
        pen.rect(cx - u * .3f, cy, u * .6f, u * 2.2f)
        pen.color = Palettes.torch.copy(alpha = (.7f + .3f * sin(time * 11f + y)) * light.coerceAtLeast(.6f))
        pen.circle(cx, cy + u * 2.6f, u * .7f)
        pen.color = Palettes.torch.copy(alpha = .15f)
        pen.circle(cx, cy + u * 2.6f, u * 2.2f)
    }
    fun column() {
        pen.rect(cx - u * .8f, cy, u * 1.6f, u * 3.4f)
        pen.color = tone(palette.decor, 1.25f * light)
        pen.ellipse(cx - u * .8f, cy + u * 3.1f, u * 1.6f, u * .6f)
    }
    fun mushroom() {
        pen.rect(cx - u * .2f, cy, u * .4f, u * 1.4f)
        pen.color = glowing
        pen.ellipse(cx - u * .9f, cy + u * 1.2f, u * 1.8f, u * .9f)
    }
    fun embers() {
        pen.color = Palettes.torch.copy(alpha = (.35f + .25f * sin(time * 3f + x + y)) * light)
        pen.ellipse(cx - u * 2f, cy - u * .6f, u * 4f, u * 1.2f)
    }
    when (biome) {
        "SHORE" -> when (kind) {
            1 -> stone()

            2 -> puddle()

            else -> {
                pen.color = tone(Color(0xFFE6D2B4), light)
                pen.arc(cx, cy, u * 1.2f, 0f, 180f)
            }
        }

        "CAVE" -> when (kind) {
            1 -> stone()

            2 -> {
                pen.triangle(cx - u, cy, cx + u, cy, cx, cy + u * 4f)
            }

            else -> shard()
        }

        "MIRE" -> when (kind) {
            1 -> puddle()
            2 -> tuft()
            else -> mushroom()
        }

        "FOREST" -> when (kind) {
            1 -> tuft()

            2 -> mushroom()

            else -> {
                pen.ellipse(cx - u * 1.4f, cy - u * .5f, u * 2.8f, u * 1.4f)
                pen.color = tone(palette.decor, .7f * light)
                pen.ellipse(cx - u * .8f, cy + u * .2f, u * 1.6f, u * .6f)
            }
        }

        "RUINS" -> when (kind) {
            1 -> stone()
            2 -> column()
            else -> bones()
        }

        "CRYPT" -> when (kind) {
            1 -> bones()

            2 -> flame()

            else -> {
                pen.rect(cx - u, cy, u * 2f, u * 2.4f)
                pen.circle(cx, cy + u * 2.4f, u)
            }
        }

        "MINES" -> when (kind) {
            1 -> stone()
            2 -> shard()
            else -> flame()
        }

        "ASH" -> when (kind) {
            1 -> stone()

            2 -> embers()

            else -> {
                pen.rect(cx - u * .5f, cy, u, u * 2.6f)
                pen.color = Palettes.torch.copy(alpha = .5f * light)
                pen.circle(cx, cy + u * .3f, u * .5f)
            }
        }

        "FROST" -> when (kind) {
            1 -> {
                pen.color = tone(Color(0xFFE8F2FA), light)
                pen.ellipse(cx - u * 2f, cy - u * .6f, u * 4f, u * 1.6f)
            }

            2 -> shard()

            else -> stone()
        }

        "VAAL" -> when (kind) {
            1 -> bones()
            2 -> flame()
            else -> puddle()
        }

        // The lands past the Drowned Temple (2.77.0): bleached bones in the sand, pillars of red
        // rock, the jungle's tufts, amber in the hive, lava pools, braziers, void crystals, rot.
        "DESERT" -> when (kind) {
            1 -> stone()
            2 -> bones()
            else -> tuft()
        }

        "CANYON" -> when (kind) {
            1 -> stone()
            2 -> column()
            else -> bones()
        }

        "JUNGLE" -> when (kind) {
            1 -> tuft()
            2 -> mushroom()
            else -> puddle()
        }

        "HIVE" -> when (kind) {
            1 -> shard()
            2 -> mushroom()
            else -> bones()
        }

        "VOLCANO" -> when (kind) {
            1 -> stone()
            2 -> embers()
            else -> shard()
        }

        "CITADEL" -> when (kind) {
            1 -> column()
            2 -> flame()
            else -> stone()
        }

        "ABYSS" -> when (kind) {
            1 -> shard()
            2 -> bones()
            else -> puddle()
        }

        "BLIGHT" -> when (kind) {
            1 -> mushroom()
            2 -> puddle()
            else -> tuft()
        }

        // The lands 71–100 (3.42.0): sky pillars and beacons, glass shards, storm-split rock, drowned columns,
        // coral growths, the vaults' brass, golden braziers, fallen stars and what oblivion leaves.
        "SKYREACH" -> when (kind) {
            1 -> column()
            2 -> flame()
            else -> shard()
        }

        "GLASSWASTE" -> when (kind) {
            1 -> shard()
            2 -> stone()
            else -> bones()
        }

        "STORMPEAK" -> when (kind) {
            1 -> stone()
            2 -> shard()
            else -> embers()
        }

        "SUNKEN" -> when (kind) {
            1 -> column()
            2 -> puddle()
            else -> bones()
        }

        "CORAL" -> when (kind) {
            1 -> mushroom()
            2 -> tuft()
            else -> shard()
        }

        "TIDEVAULT" -> when (kind) {
            1 -> column()
            2 -> puddle()
            else -> flame()
        }

        "GODHALL" -> when (kind) {
            1 -> column()
            2 -> flame()
            else -> stone()
        }

        "ASTRAL" -> when (kind) {
            1 -> shard()
            2 -> flame()
            else -> puddle()
        }

        "OBLIVION" -> when (kind) {
            1 -> bones()
            2 -> shard()
            else -> stone()
        }

        "TEMPLE" -> when (kind) {
            1 -> column()

            2 -> flame()

            else -> {
                pen.color = glowing
                pen.circle(cx, cy, u * 1.3f)
                pen.color = tone(palette.floor, light)
                pen.circle(cx, cy, u * .8f)
            }
        }

        else -> when (kind) {
            1 -> stone()
            2 -> tuft()
            else -> shard()
        }
    }
}
