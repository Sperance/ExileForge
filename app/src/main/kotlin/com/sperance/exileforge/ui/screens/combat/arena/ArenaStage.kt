package com.sperance.exileforge.ui.screens.combat.arena

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.display.icons.IconSet
import com.sperance.exileforge.core.model.combat.arena.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.icons.ServerArt
import com.sperance.exileforge.ui.icons.drawServerArt
import com.sperance.exileforge.ui.icons.serverArt
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Parsed server art kept alive for a whole zone run.
 *
 * The stage redraws every frame and asks for the same handful of ids each time, so an icon is parsed
 * once here instead of inside the draw pass.
 */
private class ArtCache(private val icons: IconSet) {
    private val parsed = HashMap<String, ServerArt?>()
    fun art(id: String): ServerArt? {
        if(!parsed.containsKey(id)) parsed[id] = serverArt(icons, id, framed = false)
        return parsed[id]
    }
}

/** Bundled silhouettes the stage falls back to when the server set is missing or not loaded yet. */
private class ArenaGlyphs(val mob: Painter, val boss: Painter, val hero: Painter, val gear: Painter,
    val orb: Painter, val mark: Painter)

/** Text styles measured on the canvas, captured once because there is no layout pass in a draw scope. */
private class ArenaLabels(val name: TextStyle, val number: TextStyle, val banner: TextStyle)

/**
 * The battle drawn as a 2D stage.
 *
 * Everything on it comes out of [ArenaSimulation], which only ever holds numbers the server sent: the
 * bars are the server's life, the floating numbers are the life it took off, and the motes are the
 * rewards it granted. [clock] is read inside the draw pass so a simulation step repaints the canvas
 * without recomposing the screen around it.
 */
@Composable fun ArenaStage(arena: ArenaSimulation, clock: () -> Long, zoneId: String, modifier: Modifier = Modifier) {
    val icons = LocalForgeIcons.current
    val cache = remember(icons.version) { ArtCache(icons) }
    val measurer = rememberTextMeasurer()
    val glyphs = ArenaGlyphs(rememberVectorPainter(ForgeGlyphs.Skull), rememberVectorPainter(ForgeGlyphs.Sigil),
        rememberVectorPainter(ForgeGlyphs.Exile), rememberVectorPainter(ForgeGlyphs.Gem),
        rememberVectorPainter(ForgeGlyphs.Orb), rememberVectorPainter(ForgeGlyphs.Constellation))
    val labels = ArenaLabels(MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        MaterialTheme.typography.headlineSmall)
    Canvas(modifier) {
        val now = clock()
        val kick = Offset(sin(now * .09f) * arena.shake * 7f, cos(now * .117f) * arena.shake * 5f)
        withTransform({ translate(kick.x, kick.y) }) {
            cavern(zoneId)
            arena.corpses.forEach { remains ->
                figure(cache.art(remains.icon), if(remains.boss) glyphs.boss else glyphs.mob, Muted,
                    at(remains.position), span(remains.position) * .74f, remains.fade * .5f, flip = true)
            }
            arena.mobs.forEach { if(it.queued) waitingMob(it, cache, glyphs, measurer, labels.name) }
            arena.mobs.forEach {
                if(!it.queued && !it.gone) fighter(it, cache.art(it.icon),
                    if(it.boss) glyphs.boss else glyphs.mob, elementTint(it.element), measurer, labels.name)
            }
            arena.hero?.let { fighter(it, cache.art(it.icon), glyphs.hero, Gold, measurer, labels.name) }
            arena.sparks.forEach { sparkle(it) }
            arena.loot.forEach { lootMote(it, cache.art(it.icon), glyphs) }
            arena.numbers.forEach { damageNumber(it, measurer, labels.number) }
        }
        arena.banner?.let { callBanner(it, measurer, labels.banner) }
    }
}

/** Arena units to pixels, plus the depth scale that makes the front of the stage read as nearer. */
private fun DrawScope.at(point: Vec2) = Offset(point.x * size.width, point.y * size.height)
private fun DrawScope.span(point: Vec2) = size.height * .30f * (.80f + .36f * point.y)

/** Cave backdrop: vault glow, two ridges seeded by the zone, and the floor the fight stands on. */
private fun DrawScope.cavern(zoneId: String) {
    drawRect(Brush.verticalGradient(listOf(Ink, Abyss, Panel), 0f, size.height))
    val vault = Offset(size.width * .5f, -size.height * .10f)
    drawCircle(Brush.radialGradient(listOf(Gold.copy(alpha = .09f), Color.Transparent), vault, size.width * .7f),
        radius = size.width * .7f, center = vault)
    ridge(zoneId.hashCode(), .58f, Panel)
    ridge(zoneId.hashCode() * 31, .66f, PanelRaised)
    val floor = size.height * .72f
    drawRect(Brush.verticalGradient(listOf(PanelRaised, Ink), floor, size.height),
        topLeft = Offset(0f, floor), size = Size(size.width, size.height - floor))
    drawLine(Brush.horizontalGradient(listOf(Color.Transparent, Bronze.copy(alpha = .55f), Color.Transparent)),
        Offset(0f, floor), Offset(size.width, floor), 1.4f)
}

/** Jagged silhouette walked from a seed, so a zone always gets the same skyline. */
private fun DrawScope.ridge(seed: Int, top: Float, colour: Color) {
    val steps = 9
    val base = abs(seed)
    val path = Path().apply {
        moveTo(0f, size.height)
        for(step in 0..steps) {
            val jitter = (base / (step + 1) % 100) / 100f
            lineTo(size.width * step / steps, size.height * (top - jitter * .16f))
        }
        lineTo(size.width, size.height); close()
    }
    drawPath(path, colour)
}

/** A pack member waiting its turn: the server fights one mob, the rest are the zone standing by. */
private fun DrawScope.waitingMob(mob: ArenaActor, cache: ArtCache, glyphs: ArenaGlyphs,
    measurer: TextMeasurer, style: TextStyle) {
    val centre = at(mob.position) + Offset(0f, sin(mob.bob * 1.6f) * 3f)
    val side = span(mob.position) * .58f
    figure(cache.art(mob.icon), if(mob.boss) glyphs.boss else glyphs.mob, Muted, centre, side, .34f, flip = true)
    label(measurer, mob.name, centre + Offset(0f, side * .62f), style, Muted.copy(alpha = .70f))
}

/**
 * One fighter: pose offset, hit flash, its picture, a life bar and its name.
 *
 * The bar is [ArenaActor.lifeRatio] — the server's life over the server's maximum — never a number
 * this screen kept for itself.
 */
private fun DrawScope.fighter(actor: ArenaActor, art: ServerArt?, glyph: Painter, accent: Color,
    measurer: TextMeasurer, style: TextStyle) {
    val progress = actor.poseProgress
    val lunge = actor.facing * when(actor.pose) {
        ArenaPose.WINDUP -> -.020f * progress
        ArenaPose.STRIKE -> .055f * sin(progress * PI.toFloat())
        ArenaPose.RECOIL -> -.030f * (1f - progress)
        else -> 0f
    }
    val alpha = when(actor.pose) {
        ArenaPose.SPAWN -> progress
        ArenaPose.DEATH -> 1f - progress
        else -> 1f
    }
    val grow = if(actor.pose == ArenaPose.SPAWN) .62f + .38f * progress else 1f
    val sway = if(actor.pose == ArenaPose.IDLE) sin(actor.bob * 2.1f) * 2.6f else 0f
    val centre = at(actor.position + Vec2(lunge, 0f)) + Offset(0f, sway)
    val side = span(actor.position) * grow
    shadowPool(centre, side, alpha)
    when(actor.pose) {
        ArenaPose.GUARD -> ward(centre, side, actor.facing, progress)
        ArenaPose.CAST -> sigilRing(centre, side, accent, progress)
        ArenaPose.QUAFF -> drawCircle(Vital.copy(alpha = .22f * (1f - progress)), side * .64f, centre)
        else -> Unit
    }
    if(actor.shield > 0.0) drawCircle(ShieldCyan.copy(alpha = .30f * alpha), side * .56f, centre, style = Stroke(1.6f))
    figure(art, glyph, accent, centre, side, alpha, flip = actor.side == ArenaSide.MOB)
    if(actor.flash > 0f) drawCircle(Color.White.copy(alpha = .34f * actor.flash * alpha), side * .42f, centre)
    val barTop = centre.y - side * .66f
    lifeBar(Offset(centre.x - side * .44f, barTop), side * .88f, actor.lifeRatio,
        if(actor.side == ArenaSide.HERO) Gold else accent, alpha)
    label(measurer, actor.name, Offset(centre.x, barTop - 12f), style,
        (if(actor.boss) Blood else Parchment).copy(alpha = alpha))
}

/** Grounding ellipse: without it the fighters float over the floor. */
private fun DrawScope.shadowPool(centre: Offset, side: Float, alpha: Float) {
    drawOval(Brush.radialGradient(listOf(Color.Black.copy(alpha = .55f * alpha), Color.Transparent)),
        topLeft = Offset(centre.x - side * .34f, centre.y + side * .34f), size = Size(side * .68f, side * .18f))
}

private fun DrawScope.lifeBar(topLeft: Offset, width: Float, ratio: Float, accent: Color, alpha: Float) {
    drawRect(Abyss.copy(alpha = .86f * alpha), topLeft, Size(width, 5f))
    if(ratio > 0f) drawRect(LifeRed.copy(alpha = alpha), topLeft, Size(width * ratio, 5f))
    drawRect(accent.copy(alpha = .60f * alpha), topLeft, Size(width, 5f), style = Stroke(1f))
}

/** The guard stance: a bronze arc on the side the blow is coming from. */
private fun DrawScope.ward(centre: Offset, side: Float, facing: Float, progress: Float) {
    val fade = 1f - progress * .4f
    drawArc(Bronze.copy(alpha = .70f * fade), if(facing > 0f) -70f else 110f, 140f, false,
        topLeft = Offset(centre.x - side * .52f, centre.y - side * .52f),
        size = Size(side * 1.04f, side * 1.04f), style = Stroke(3f))
}

/** The heavy strike: a rune ring that opens as the mana the server spent leaves the hero. */
private fun DrawScope.sigilRing(centre: Offset, side: Float, accent: Color, progress: Float) {
    drawCircle(accent.copy(alpha = .40f * (1f - progress)), side * (.40f + .34f * progress), centre, style = Stroke(2f))
    drawCircle(ManaBlue.copy(alpha = .26f * (1f - progress)), side * (.22f + .22f * progress), centre, style = Stroke(1.4f))
}

/** The server's drawing when the set is loaded, the bundled glyph when it is not. */
private fun DrawScope.figure(art: ServerArt?, glyph: Painter, tint: Color, centre: Offset, side: Float,
    alpha: Float, flip: Boolean = false) {
    if(side <= 0f || alpha <= 0f) return
    withTransform({ if(flip) scale(-1f, 1f, centre) }) {
        if(art != null) drawServerArt(art, centre, side, alpha)
        else withTransform({ translate(centre.x - side / 2f, centre.y - side / 2f) }) {
            with(glyph) { draw(Size(side, side), alpha = alpha, colorFilter = ColorFilter.tint(tint)) }
        }
    }
}

private fun DrawScope.sparkle(spark: ArenaSpark) {
    val reach = spark.speed * spark.progress
    val head = at(spark.origin) + Offset(cos(spark.angle) * reach * size.width, sin(spark.angle) * reach * size.height)
    drawCircle(tintOf(spark.tint).copy(alpha = 1f - spark.progress), 2.6f * (1f - spark.progress) + .6f, head)
}

/** A reward drifting to the hero. Its picture is the server's when the set knows the item. */
private fun DrawScope.lootMote(mote: ArenaLootMote, art: ServerArt?, glyphs: ArenaGlyphs) {
    val alpha = 1f - mote.progress * mote.progress
    val centre = at(mote.position) + Offset(0f, sin(mote.progress * 8f) * 4f)
    val side = size.height * .11f
    val tint = when(mote.kind) {
        LootKind.EQUIPMENT -> Gold
        LootKind.CURRENCY -> Rune
        LootKind.EXPERIENCE -> GoldBright
        LootKind.GOLD -> Bronze
    }
    drawCircle(tint.copy(alpha = .22f * alpha), side * .74f, centre)
    figure(art, when(mote.kind) {
        LootKind.EQUIPMENT -> glyphs.gear
        LootKind.EXPERIENCE -> glyphs.mark
        else -> glyphs.orb
    }, tint, centre, side, alpha)
}

private fun DrawScope.damageNumber(number: ArenaNumber, measurer: TextMeasurer, style: TextStyle) {
    val alpha = (1f - number.progress * number.progress).coerceIn(0f, 1f)
    val scale = if(number.crit) 1.35f + .25f * (1f - number.progress) else 1f
    val centre = at(number.origin) - Offset(0f, number.progress * size.height * .16f)
    label(measurer, number.text, centre, style.copy(fontSize = style.fontSize * scale),
        tintOf(number.tint).copy(alpha = alpha), shadowed = true)
}

/** Victory, defeat, a retreat, or the name of whatever just walked in. */
private fun DrawScope.callBanner(banner: ArenaBanner, measurer: TextMeasurer, style: TextStyle) {
    val alpha = when {
        banner.progress < .12f -> banner.progress / .12f
        banner.progress > .72f -> (1f - banner.progress) / .28f
        else -> 1f
    }.coerceIn(0f, 1f)
    val accent = if(banner.boss) Blood else Gold
    val middle = size.height * .18f
    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Ink.copy(alpha = .78f * alpha), Color.Transparent)),
        topLeft = Offset(0f, middle - 20f), size = Size(size.width, 40f))
    label(measurer, banner.text.uppercase(), Offset(size.width / 2f, middle), style, accent.copy(alpha = alpha))
    drawLine(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = .60f * alpha), Color.Transparent)),
        Offset(0f, middle + 18f), Offset(size.width, middle + 18f), 1.2f)
}

/** Text centred on a point; the canvas measures it because there is no layout pass out here. */
private fun DrawScope.label(measurer: TextMeasurer, text: String, centre: Offset, style: TextStyle,
    colour: Color, shadowed: Boolean = false) {
    if(text.isBlank() || colour.alpha <= .02f) return
    val layout = measurer.measure(text, style)
    val topLeft = Offset(centre.x - layout.size.width / 2f, centre.y - layout.size.height / 2f)
    if(shadowed) drawText(layout, Color.Black.copy(alpha = colour.alpha * .65f), topLeft + Offset(1.4f, 1.4f))
    drawText(layout, colour, topLeft)
}

/** Damage families as the stash palette draws them. */
private fun tintOf(tint: ArenaTint) = when(tint) {
    ArenaTint.HERO_DAMAGE -> GoldBright
    ArenaTint.MOB_DAMAGE -> LifeRed
    ArenaTint.HEAL -> Vital
    ArenaTint.MANA -> ManaBlue
    ArenaTint.SHIELD -> ShieldCyan
    ArenaTint.MISS -> Muted
    ArenaTint.GUARD -> Rune
}

/** The mob's damage type, in the theme's own colours; an unknown element stays bone white. */
internal fun elementTint(element: String) = when(element.lowercase()) {
    "fire" -> Ember
    "cold" -> ShieldCyan
    "lightning" -> Gold
    "chaos" -> rarityColor("EPIC")
    else -> Parchment
}
