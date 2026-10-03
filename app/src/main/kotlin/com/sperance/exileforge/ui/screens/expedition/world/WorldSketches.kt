package com.sperance.exileforge.ui.screens.expedition.world

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.sperance.exileforge.core.campaign.WorldMap
import com.sperance.exileforge.rules.content.CampaignFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

internal class Glow(val centre: Offset, val radius: Float, val color: Color)
internal class Sketch(val path: Path, val color: Color, val fill: Boolean, val width: Float = 1f)
internal class Grain(val points: List<Offset>, val color: Color)

/** What a biome's land is sketched with, how many a zone gets and how much room each wants. */
internal enum class SketchKind(val count: Int, val room: Float) {
    CRYSTAL_HILL(6, 52f),
    SNOW_HILL(6, 52f),
    CINDER_HILL(6, 52f),
    TREE(10, 30f),
    REED(10, 28f),
    COLUMN(7, 26f),
    TOMB(7, 26f),
    ROCK(7, 28f),
    DUNE(8, 40f),
    MESA(5, 56f),
    MOUND(7, 30f),
    TOWER(6, 30f),
    CHASM(5, 44f),
    SPORE(9, 28f),
    ISLE(6, 40f),
    SPIRE(8, 30f),
    STORM(5, 52f),
    ARCH(6, 34f),
    CORAL(9, 28f),
    SHRINE(5, 40f),
    STAR(8, 30f),
}

/** The sketches gathered into one path per ink, so the whole land is drawn in a handful of calls. */
internal class Sketches {
    private val spots = mutableListOf<Offset>()
    private val hillBody = Path()
    private val hillEdge = Path()
    private val hillHatch = Path()
    private val snow = Path()
    private val embers = Path()
    private val crystals = Path()
    private val canopies = Path()
    private val canopyEdge = Path()
    private val trunks = Path()
    private val reeds = Path()
    private val stones = Path()
    private val stoneEdge = Path()
    private val dunes = Path()
    private val voids = Path()
    private val voidEdge = Path()
    private val caps = Path()
    private val glints = Path()
    private val clouds = Path()
    private val bolts = Path()
    private val coral = Path()

    fun crowded(p: Offset, room: Float) = spots.any { hypot(it.x - p.x, it.y - p.y) < room }

    fun add(kind: SketchKind, p: Offset, size: Float) {
        spots += p
        when (kind) {
            SketchKind.CRYSTAL_HILL, SketchKind.SNOW_HILL, SketchKind.CINDER_HILL -> hill(kind, p, 15 * size)

            SketchKind.TREE -> {
                trunks.moveTo(p.x, p.y)
                trunks.lineTo(p.x, p.y + 6)
                canopies.addOval(Rect(Offset(p.x, p.y - 2), 6f))
                canopyEdge.addOval(Rect(Offset(p.x, p.y - 2), 6f))
            }

            SketchKind.REED -> {
                reeds.moveTo(p.x - 5, p.y)
                reeds.lineTo(p.x + 5, p.y)
                reeds.moveTo(p.x, p.y)
                reeds.lineTo(p.x, p.y - 6)
                reeds.moveTo(p.x, p.y)
                reeds.lineTo(p.x - 3, p.y - 5)
                reeds.moveTo(p.x, p.y)
                reeds.lineTo(p.x + 3, p.y - 5)
            }

            SketchKind.COLUMN -> {
                val h = 7 + 5 * size
                stones.addRect(Rect(p.x, p.y - h, p.x + 4, p.y))
                stones.addRect(Rect(p.x + 7, p.y - h * .55f, p.x + 11, p.y))
            }

            SketchKind.TOMB -> {
                val arch = Path().apply {
                    moveTo(p.x - 5, p.y)
                    lineTo(p.x - 5, p.y - 6)
                    quadraticTo(p.x, p.y - 13, p.x + 5, p.y - 6)
                    lineTo(p.x + 5, p.y)
                    close()
                }
                stones.addPath(arch)
                stoneEdge.addPath(arch)
                stoneEdge.moveTo(p.x, p.y - 8)
                stoneEdge.lineTo(p.x, p.y - 3)
                stoneEdge.moveTo(p.x - 2, p.y - 6)
                stoneEdge.lineTo(p.x + 2, p.y - 6)
            }

            SketchKind.ROCK -> {
                val rock = Rect(p.x - 6 * size, p.y - 4 * size, p.x + 6 * size, p.y)
                stones.addOval(rock)
                stoneEdge.addOval(rock)
            }

            SketchKind.DUNE -> {
                val s = 9 * size
                dunes.moveTo(p.x - s * 1.3f, p.y)
                dunes.quadraticTo(p.x - s * .4f, p.y - s * .8f, p.x + s * .4f, p.y)
                dunes.moveTo(p.x - s * .2f, p.y - 1)
                dunes.quadraticTo(p.x + s * .6f, p.y - s * .6f, p.x + s * 1.3f, p.y - 1)
            }

            SketchKind.MESA -> {
                val s = 13 * size
                hillBody.moveTo(p.x - s, p.y)
                hillBody.lineTo(p.x - s * .55f, p.y - s * .7f)
                hillBody.lineTo(p.x + s * .55f, p.y - s * .7f)
                hillBody.lineTo(p.x + s, p.y)
                hillBody.close()
                hillEdge.moveTo(p.x - s, p.y)
                hillEdge.lineTo(p.x - s * .55f, p.y - s * .7f)
                hillEdge.lineTo(p.x + s * .55f, p.y - s * .7f)
                hillEdge.lineTo(p.x + s, p.y)
                // The strata of the red rock, two bands across the face.
                for (t in listOf(.35f, .65f)) {
                    hillHatch.moveTo(p.x - s * (1 - .45f * t), p.y - s * .7f * t)
                    hillHatch.lineTo(p.x + s * (1 - .45f * t), p.y - s * .7f * t)
                }
            }

            SketchKind.MOUND -> {
                val s = 8 * size
                val dome = Path().apply {
                    moveTo(p.x - s, p.y)
                    cubicTo(p.x - s, p.y - s * 1.4f, p.x + s, p.y - s * 1.4f, p.x + s, p.y)
                    close()
                }
                stones.addPath(dome)
                stoneEdge.addPath(dome)
                glints.addOval(Rect(Offset(p.x - s * .35f, p.y - s * .35f), s * .16f))
                glints.addOval(Rect(Offset(p.x + s * .3f, p.y - s * .6f), s * .13f))
            }

            SketchKind.TOWER -> {
                val h = 12 + 6 * size
                val tower = Path().apply {
                    moveTo(p.x - 4, p.y)
                    lineTo(p.x - 4, p.y - h)
                    lineTo(p.x - 5.5f, p.y - h)
                    lineTo(p.x - 5.5f, p.y - h - 3)
                    lineTo(p.x - 2.5f, p.y - h - 3)
                    lineTo(p.x - 2.5f, p.y - h - 1)
                    lineTo(p.x + 2.5f, p.y - h - 1)
                    lineTo(p.x + 2.5f, p.y - h - 3)
                    lineTo(p.x + 5.5f, p.y - h - 3)
                    lineTo(p.x + 5.5f, p.y - h)
                    lineTo(p.x + 4, p.y - h)
                    lineTo(p.x + 4, p.y)
                    close()
                }
                stones.addPath(tower)
                stoneEdge.addPath(tower)
                glints.addRect(Rect(p.x - 1, p.y - h * .7f, p.x + 1, p.y - h * .55f))
            }

            SketchKind.CHASM -> {
                val s = 12 * size
                val crack = Path().apply {
                    moveTo(p.x - s, p.y)
                    lineTo(p.x - s * .4f, p.y - s * .22f)
                    lineTo(p.x, p.y - s * .08f)
                    lineTo(p.x + s * .5f, p.y - s * .26f)
                    lineTo(p.x + s, p.y - s * .05f)
                    lineTo(p.x + s * .45f, p.y + s * .12f)
                    lineTo(p.x - s * .1f, p.y + s * .05f)
                    lineTo(p.x - s * .5f, p.y + s * .14f)
                    close()
                }
                voids.addPath(crack)
                voidEdge.addPath(crack)
            }

            SketchKind.ISLE -> {
                val s = 9 * size
                val rock = Path().apply {
                    moveTo(p.x - s, p.y - s * .4f)
                    lineTo(p.x + s, p.y - s * .4f)
                    lineTo(p.x + s * .3f, p.y + s * .5f)
                    lineTo(p.x - s * .2f, p.y + s * .3f)
                    close()
                }
                stones.addPath(rock)
                stoneEdge.addPath(rock)
                clouds.addOval(Rect(p.x - s * 1.3f, p.y - s * .2f, p.x - s * .2f, p.y + s * .35f))
                clouds.addOval(Rect(p.x + s * .1f, p.y - s * .1f, p.x + s * 1.4f, p.y + s * .45f))
            }

            SketchKind.SPIRE -> {
                val h = 10 + 7 * size
                crystals.moveTo(p.x - 3, p.y)
                crystals.lineTo(p.x, p.y - h)
                crystals.lineTo(p.x + 3, p.y)
                crystals.close()
                crystals.moveTo(p.x + 4, p.y)
                crystals.lineTo(p.x + 6, p.y - h * .55f)
                crystals.lineTo(p.x + 8, p.y)
                crystals.close()
                stoneEdge.moveTo(p.x, p.y - h)
                stoneEdge.lineTo(p.x, p.y)
            }

            SketchKind.STORM -> {
                hill(SketchKind.SNOW_HILL, p, 13 * size)
                val c = Offset(p.x + 4, p.y - 22 * size)
                clouds.addOval(Rect(c.x - 11, c.y - 4, c.x + 11, c.y + 4))
                bolts.moveTo(c.x, c.y + 3)
                bolts.lineTo(c.x - 3, c.y + 9)
                bolts.lineTo(c.x + 1, c.y + 9)
                bolts.lineTo(c.x - 2, c.y + 15)
            }

            SketchKind.ARCH -> {
                val arch = Path().apply {
                    moveTo(p.x - 7, p.y)
                    lineTo(p.x - 7, p.y - 8)
                    quadraticTo(p.x, p.y - 16, p.x + 7, p.y - 8)
                    lineTo(p.x + 7, p.y)
                    lineTo(p.x + 4, p.y)
                    lineTo(p.x + 4, p.y - 7)
                    quadraticTo(p.x, p.y - 12, p.x - 4, p.y - 7)
                    lineTo(p.x - 4, p.y)
                    close()
                }
                stones.addPath(arch)
                stoneEdge.addPath(arch)
                dunes.moveTo(p.x - 11, p.y - 2)
                dunes.quadraticTo(p.x - 5, p.y - 5, p.x, p.y - 2)
                dunes.quadraticTo(p.x + 5, p.y + 1, p.x + 11, p.y - 2)
            }

            SketchKind.CORAL -> {
                val s = 6 * size
                coral.moveTo(p.x, p.y)
                coral.lineTo(p.x, p.y - s * 1.6f)
                coral.moveTo(p.x, p.y - s * .6f)
                coral.lineTo(p.x - s * .8f, p.y - s * 1.3f)
                coral.lineTo(p.x - s * .8f, p.y - s * 1.8f)
                coral.moveTo(p.x, p.y - s * .9f)
                coral.lineTo(p.x + s * .7f, p.y - s * 1.5f)
                coral.lineTo(p.x + s * .9f, p.y - s * 2f)
            }

            SketchKind.SHRINE -> {
                val w = 8 + 3 * size
                val shrine = Path().apply {
                    moveTo(p.x - w, p.y)
                    lineTo(p.x - w, p.y - 6)
                    lineTo(p.x - w - 2, p.y - 6)
                    lineTo(p.x, p.y - 13 - 2 * size)
                    lineTo(p.x + w + 2, p.y - 6)
                    lineTo(p.x + w, p.y - 6)
                    lineTo(p.x + w, p.y)
                    close()
                }
                stones.addPath(shrine)
                stoneEdge.addPath(shrine)
                glints.addOval(Rect(Offset(p.x, p.y - 9), 1.8f))
            }

            SketchKind.STAR -> {
                val r = 4 + 3 * size
                glints.moveTo(p.x, p.y - r * 2)
                glints.lineTo(p.x + r * .3f, p.y - r * 1.3f)
                glints.lineTo(p.x + r, p.y - r)
                glints.lineTo(p.x + r * .3f, p.y - r * .7f)
                glints.lineTo(p.x, p.y)
                glints.lineTo(p.x - r * .3f, p.y - r * .7f)
                glints.lineTo(p.x - r, p.y - r)
                glints.lineTo(p.x - r * .3f, p.y - r * 1.3f)
                glints.close()
            }

            SketchKind.SPORE -> {
                val s = 5 * size
                trunks.moveTo(p.x, p.y)
                trunks.lineTo(p.x, p.y - s)
                val cap = Path().apply {
                    moveTo(p.x - s * 1.1f, p.y - s)
                    quadraticTo(p.x, p.y - s * 2.3f, p.x + s * 1.1f, p.y - s)
                    close()
                }
                caps.addPath(cap)
                canopyEdge.addPath(cap)
            }
        }
    }

    /** A hill as the old charts drew them: a dark cone, an inked edge, hatching on the shaded side, and its cap. */
    private fun hill(kind: SketchKind, p: Offset, s: Float) {
        val peak = Offset(p.x - s * .12f, p.y - s * 1.05f)
        val left = p.x - s
        val right = p.x + s * .9f
        hillBody.moveTo(left, p.y)
        hillBody.lineTo(peak.x, peak.y)
        hillBody.lineTo(right, p.y)
        hillBody.close()
        hillEdge.moveTo(left, p.y)
        hillEdge.lineTo(peak.x, peak.y)
        hillEdge.lineTo(right, p.y)
        for (i in 1..4) {
            val t = i / 5f
            val hx = peak.x + (right - peak.x) * t
            val hy = peak.y + (p.y - peak.y) * t
            hillHatch.moveTo(hx, hy)
            hillHatch.lineTo(hx - s * .22f, p.y)
        }
        val cap = Path().apply {
            moveTo(peak.x, peak.y)
            lineTo(peak.x - s * .28f, peak.y + s * .34f)
            lineTo(peak.x - s * .05f, peak.y + s * .26f)
            lineTo(peak.x + s * .12f, peak.y + s * .38f)
            lineTo(peak.x + s * .3f, peak.y + s * .3f)
            close()
        }
        when (kind) {
            SketchKind.CINDER_HILL -> embers.addPath(cap)

            SketchKind.CRYSTAL_HILL -> {
                snow.addPath(cap)
                val c = Offset(p.x + s * .9f, p.y - 3)
                crystals.moveTo(c.x, c.y - 9)
                crystals.lineTo(c.x + 3.5f, c.y)
                crystals.lineTo(c.x, c.y + 3)
                crystals.lineTo(c.x - 3.5f, c.y)
                crystals.close()
            }

            else -> snow.addPath(cap)
        }
    }

    fun build(): List<Sketch> {
        val gold = Color(0xFFC8AA6E)
        return listOf(
            Sketch(hillBody, Color(0xEB10141A), fill = true),
            Sketch(hillEdge, gold.copy(alpha = .55f), fill = false, width = 1.2f),
            Sketch(hillHatch, gold.copy(alpha = .25f), fill = false),
            Sketch(snow, Color(0xFFCFE3F2).copy(alpha = .55f), fill = true),
            Sketch(embers, Color(0xFFD9642E).copy(alpha = .7f), fill = true),
            Sketch(crystals, Color(0xFF8EC5FF).copy(alpha = .5f), fill = true),
            Sketch(trunks, gold.copy(alpha = .35f), fill = false),
            Sketch(canopies, Color(0xF2223420), fill = true),
            Sketch(caps, Color(0xE66A3A5A), fill = true),
            Sketch(canopyEdge, Color(0xFFA0B478).copy(alpha = .45f), fill = false),
            Sketch(reeds, Color(0xFF8CA578).copy(alpha = .5f), fill = false),
            Sketch(stones, Color(0xFF3A3326).copy(alpha = .9f), fill = true),
            Sketch(stoneEdge, gold.copy(alpha = .35f), fill = false),
            Sketch(glints, Color(0xFFF0B83A).copy(alpha = .75f), fill = true),
            Sketch(dunes, Color(0xFFD8B070).copy(alpha = .45f), fill = false, width = 1.2f),
            Sketch(voids, Color(0xF207050C), fill = true),
            Sketch(voidEdge, Color(0xFFA070FF).copy(alpha = .45f), fill = false),
            Sketch(clouds, Color(0xFFDDE8F2).copy(alpha = .35f), fill = true),
            Sketch(bolts, Color(0xFFF2D03A).copy(alpha = .8f), fill = false, width = 1.4f),
            Sketch(coral, Color(0xFFE86A7A).copy(alpha = .7f), fill = false, width = 1.6f),
        )
    }
}
