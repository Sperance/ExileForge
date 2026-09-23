package com.sperance.exileforge.ui.screens.expedition.gdx

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.abs
import kotlin.math.sin

/**
 * The silhouettes: the hero and every monster form, drawn from shapes — no picture is ever loaded.
 *
 * Each figure stands with its feet at `(x, y)` and is [size] tall, faces [facing] (1 right, -1 left),
 * and can be washed toward white by [flash] (a hit) and faded by [alpha] (a death). [time] animates
 * the idle bob, a bat's wings and a wraith's hover.
 */
class Figures(private val shapes: ShapeRenderer) {
    private val scratch = Color()

    private fun paint(base: Color, flash: Float, alpha: Float, dim: Float = 1f) {
        scratch.set(base.r * dim, base.g * dim, base.b * dim, 1f).lerp(Color.WHITE, flash.coerceIn(0f, 1f))
        scratch.a = alpha
        shapes.color = scratch
    }

    fun shadow(x: Float, y: Float, width: Float, alpha: Float = .45f) {
        shapes.setColor(0f, 0f, 0f, alpha)
        shapes.ellipse(x - width / 2, y - width / 6, width, width / 3)
    }

    /** The rarity ring under a magic or rare monster's feet. */
    fun ring(x: Float, y: Float, width: Float, color: Color, time: Float) {
        val pulse = .55f + .25f * sin(time * 4f)
        shapes.setColor(color.r, color.g, color.b, pulse)
        shapes.ellipse(x - width / 2, y - width / 5, width, width / 2.5f)
        shapes.setColor(0f, 0f, 0f, .6f)
        shapes.ellipse(x - width * .4f, y - width * .15f, width * .8f, width * .3f)
    }

    fun hero(x: Float, y: Float, size: Float, facing: Float, time: Float, moving: Boolean, flash: Float = 0f, alpha: Float = 1f) {
        val bob = if (moving) abs(sin(time * 10f)) * size * .04f else sin(time * 2f) * size * .01f
        val u = size / 10f
        shadow(x, y, u * 5.5f)
        // Cape behind, then legs, body, head and the blade in the leading hand.
        paint(Palettes.heroCape, flash, alpha, .9f)
        shapes.triangle(x - facing * u * .6f, y + u * 7f + bob, x - facing * u * 3.2f, y + u * .8f, x + facing * u * .6f, y + u * 1.2f)
        paint(Palettes.hero, flash, alpha, .55f)
        val stride = if (moving) sin(time * 10f) * u * .9f else 0f
        shapes.rect(x - u * 1.2f + stride, y, u * .9f, u * 3.2f)
        shapes.rect(x + u * .3f - stride, y, u * .9f, u * 3.2f)
        paint(Palettes.hero, flash, alpha)
        shapes.ellipse(x - u * 1.8f, y + u * 2.8f + bob, u * 3.6f, u * 4.2f)
        paint(Palettes.hero, flash, alpha, 1.15f)
        shapes.circle(x, y + u * 8f + bob, u * 1.3f, 16)
        paint(Palettes.steel, flash, alpha)
        shapes.rectLine(x + facing * u * 1.6f, y + u * 4.5f + bob, x + facing * u * 4.4f, y + u * 8.6f + bob, u * .5f)
    }

    fun monster(form: String, x: Float, y: Float, size: Float, facing: Float, time: Float, flash: Float = 0f, alpha: Float = 1f) {
        val body = Palettes.body(form)
        val u = size / 10f
        val bob = sin(time * 3f + x * .01f) * u * .2f
        when (form) {
            "CRAB" -> {
                shadow(x, y, u * 8f)
                paint(body, flash, alpha, .6f)
                for (i in -2..2) if (i != 0) shapes.rectLine(x + i * u * 1.1f, y + u * 1.5f, x + i * u * 2.2f, y, u * .35f)
                paint(body, flash, alpha)
                shapes.ellipse(x - u * 3f, y + u * 1f + bob, u * 6f, u * 3f)
                shapes.circle(x + facing * u * 3.6f, y + u * 3.2f + bob, u * 1.3f, 12)
                shapes.circle(x - facing * u * 3.2f, y + u * 3f + bob, u * 1f, 12)
                eyes(x + facing * u * .6f, y + u * 4.2f + bob, u * .9f, flash, alpha)
            }
            "BAT" -> {
                val hover = u * 4f + sin(time * 6f) * u * .6f
                shadow(x, y, u * 4f, .3f)
                val flap = sin(time * 14f) * u * 2.2f
                paint(body, flash, alpha, .8f)
                shapes.triangle(x, y + hover + u, x - u * 5f, y + hover + u * 2f + flap, x - u * 1.5f, y + hover - u * .5f)
                shapes.triangle(x, y + hover + u, x + u * 5f, y + hover + u * 2f + flap, x + u * 1.5f, y + hover - u * .5f)
                paint(body, flash, alpha)
                shapes.circle(x, y + hover + u, u * 1.4f, 12)
                eyes(x, y + hover + u * 1.4f, u * .7f, flash, alpha)
            }
            "SERPENT" -> {
                shadow(x, y, u * 6f)
                paint(body, flash, alpha)
                for (i in 0..4) {
                    val t = i / 4f
                    shapes.circle(x - facing * u * (2.5f - t * 3f) + sin(time * 3f + i) * u * .4f, y + u * (1f + t * 6f) + bob, u * (1.6f - t * .5f), 12)
                }
                paint(body, flash, alpha, 1.2f)
                shapes.ellipse(x + facing * u * .2f - u * 1.4f, y + u * 7f + bob, u * 2.8f, u * 1.8f)
                eyes(x + facing * u * .8f, y + u * 8.1f + bob, u * .8f, flash, alpha)
            }
            "SLUG" -> {
                shadow(x, y, u * 8f)
                paint(body, flash, alpha)
                shapes.ellipse(x - u * 4f, y, u * 8f, u * 3f + bob)
                paint(body, flash, alpha, 1.25f)
                shapes.rectLine(x + facing * u * 2.5f, y + u * 2.5f, x + facing * u * 3.2f, y + u * 5f, u * .35f)
                shapes.rectLine(x + facing * u * 1.5f, y + u * 2.5f, x + facing * u * 1.8f, y + u * 5.2f, u * .35f)
                eyes(x + facing * u * 2.5f, y + u * 5.1f, u * .7f, flash, alpha)
            }
            "BEAST" -> {
                shadow(x, y, u * 8f)
                val gait = sin(time * 8f) * u * .5f
                paint(body, flash, alpha, .7f)
                for (leg in listOf(-2.4f, -1.2f, 1.2f, 2.4f)) shapes.rect(x + leg * u - u * .35f + gait * (if (leg > 0) 1 else -1), y, u * .7f, u * 2.4f)
                paint(body, flash, alpha)
                shapes.ellipse(x - u * 3.4f, y + u * 2f + bob, u * 6.8f, u * 3f)
                shapes.circle(x + facing * u * 3.6f, y + u * 4.2f + bob, u * 1.5f, 12)
                shapes.triangle(x + facing * u * 3f, y + u * 5.2f + bob, x + facing * u * 3.8f, y + u * 6.6f + bob, x + facing * u * 4.2f, y + u * 5f + bob)
                shapes.rectLine(x - facing * u * 3.2f, y + u * 3.6f + bob, x - facing * u * 5f, y + u * 4.8f + bob, u * .4f)
                eyes(x + facing * u * 4.1f, y + u * 4.5f + bob, u * .6f, flash, alpha)
            }
            "SPIDER" -> {
                shadow(x, y, u * 8f)
                paint(body, flash, alpha, .8f)
                for (i in 0..3) {
                    val spread = u * (1.2f + i * .9f)
                    val step = sin(time * 10f + i) * u * .4f
                    shapes.rectLine(x, y + u * 2f, x - spread, y + step, u * .3f)
                    shapes.rectLine(x, y + u * 2f, x + spread, y - step, u * .3f)
                }
                paint(body, flash, alpha)
                shapes.ellipse(x - facing * u * 3.2f - u * 1.6f, y + u * 1.5f + bob, u * 3.6f, u * 3f)
                shapes.circle(x + facing * u * .8f, y + u * 2.4f + bob, u * 1.3f, 12)
                eyes(x + facing * u * 1.2f, y + u * 2.8f + bob, u * .6f, flash, alpha)
            }
            "GOLEM" -> {
                shadow(x, y, u * 7f)
                paint(body, flash, alpha, .75f)
                shapes.rect(x - u * 2f, y, u * 1.5f, u * 3f)
                shapes.rect(x + u * .5f, y, u * 1.5f, u * 3f)
                paint(body, flash, alpha)
                shapes.rect(x - u * 3f, y + u * 3f + bob, u * 6f, u * 4.2f)
                shapes.rect(x - u * 4.2f, y + u * 3.6f + bob, u * 1.4f, u * 3.2f)
                shapes.rect(x + u * 2.8f, y + u * 3.6f + bob, u * 1.4f, u * 3.2f)
                paint(body, flash, alpha, 1.15f)
                shapes.rect(x - u * 1.2f, y + u * 7.2f + bob, u * 2.4f, u * 1.8f)
                eyes(x + facing * u * .3f, y + u * 8.2f + bob, u * .9f, flash, alpha)
            }
            "WRAITH" -> {
                val hover = u * 1.2f + sin(time * 2.5f) * u * .5f
                shadow(x, y, u * 4f, .25f)
                paint(body, flash, alpha * .75f, .8f)
                shapes.triangle(x - u * 2.4f, y + u * 6f + hover, x + u * 2.4f, y + u * 6f + hover, x + sin(time * 4f) * u, y + hover)
                paint(body, flash, alpha * .85f)
                shapes.ellipse(x - u * 2.4f, y + u * 4.6f + hover, u * 4.8f, u * 3f)
                shapes.circle(x, y + u * 8.2f + hover, u * 1.4f, 12)
                eyes(x + facing * u * .3f, y + u * 8.3f + hover, u * .8f, flash, alpha)
            }
            else -> {
                // Humanoids, the undead and brutes share a body; the undead are thinner, brutes wider.
                val width = when (form) { "UNDEAD" -> .75f; "BRUTE" -> 1.45f; else -> 1f }
                val height = if (form == "BRUTE") 1.15f else 1f
                shadow(x, y, u * 5f * width)
                paint(body, flash, alpha, .7f)
                shapes.rect(x - u * 1.2f * width, y, u * .9f * width, u * 3.2f * height)
                shapes.rect(x + u * .3f * width, y, u * .9f * width, u * 3.2f * height)
                paint(body, flash, alpha)
                shapes.ellipse(x - u * 1.8f * width, y + u * 2.8f * height + bob, u * 3.6f * width, u * 4.2f * height)
                shapes.rectLine(x + facing * u * 1.4f * width, y + u * 6f * height + bob, x + facing * u * 3f * width, y + u * 3.6f * height + bob, u * .6f * width)
                paint(body, flash, alpha, 1.1f)
                shapes.circle(x, y + u * 8f * height + bob, u * 1.3f * width.coerceAtMost(1.1f), 14)
                if (form == "UNDEAD") {
                    paint(body, flash, alpha, .45f)
                    for (i in 0..2) shapes.rectLine(x - u, y + u * (4f + i) + bob, x + u, y + u * (4f + i) + bob, u * .2f)
                }
                eyes(x + facing * u * .4f, y + u * 8.2f * height + bob, u * .7f, flash, alpha)
            }
        }
    }

    private fun eyes(x: Float, y: Float, gap: Float, flash: Float, alpha: Float) {
        paint(Palettes.blood, flash, alpha, 1.4f)
        shapes.circle(x - gap / 2, y, gap * .28f, 8)
        shapes.circle(x + gap / 2, y, gap * .28f, 8)
    }
}
