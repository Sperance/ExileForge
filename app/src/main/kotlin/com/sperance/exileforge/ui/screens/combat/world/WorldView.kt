package com.sperance.exileforge.ui.screens.combat.world

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
import com.sperance.exileforge.core.model.combat.world.*
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.icons.LocalForgeIcons
import com.sperance.exileforge.ui.icons.ServerArt
import com.sperance.exileforge.ui.icons.drawServerArt
import com.sperance.exileforge.ui.icons.serverArt
import com.sperance.exileforge.ui.theme.*
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

/** Where the thumb went down and where it is now; the floating stick is drawn between the two. */
internal data class StickState(val origin: Offset, val thumb: Offset)

/**
 * Parsed server art kept alive for a whole zone run.
 *
 * The view redraws every frame and asks for the same handful of ids each time, so an icon is parsed
 * once here instead of inside the draw pass.
 */
private class ArtCache(private val icons: IconSet) {
    private val parsed = HashMap<String, ServerArt?>()
    fun art(id: String): ServerArt? {
        if(!parsed.containsKey(id)) parsed[id] = serverArt(icons, id, framed = false)
        return parsed[id]
    }
}

/** Bundled silhouettes the view falls back to when the server set is missing or not loaded yet. */
private class WorldGlyphs(val mob: Painter, val boss: Painter, val hero: Painter, val gear: Painter,
    val orb: Painter, val mark: Painter, val coin: Painter)

/** Text styles measured on the canvas, captured once because there is no layout pass in a draw scope. */
private class WorldLabels(val name: TextStyle, val number: TextStyle, val banner: TextStyle)

/** One path reused for every block face, so a wall of thirty tiles allocates nothing per frame. */
private class Scratch { val path = Path() }

/**
 * The expedition drawn as an isometric map with the camera tied to the exile.
 *
 * Everything on it comes out of [WorldSimulation], which only ever holds numbers the server sent: the
 * bars are the server's life, the floating numbers are the life it took off, and the piles on the floor
 * are the rewards it granted. [clock] is read inside the draw pass so a simulation step repaints the
 * canvas without recomposing the screen around it.
 */
@Composable internal fun WorldView(world: WorldSimulation, clock: () -> Long, tile: Vec2,
    stick: () -> StickState?, modifier: Modifier = Modifier) {
    val icons = LocalForgeIcons.current
    val cache = remember(icons.version) { ArtCache(icons) }
    val scratch = remember { Scratch() }
    val measurer = rememberTextMeasurer()
    val glyphs = WorldGlyphs(rememberVectorPainter(ForgeGlyphs.Skull), rememberVectorPainter(ForgeGlyphs.Sigil),
        rememberVectorPainter(ForgeGlyphs.Exile), rememberVectorPainter(ForgeGlyphs.Gem),
        rememberVectorPainter(ForgeGlyphs.Orb), rememberVectorPainter(ForgeGlyphs.Constellation),
        rememberVectorPainter(ForgeGlyphs.Stash))
    val labels = WorldLabels(MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp),
        MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
        MaterialTheme.typography.headlineSmall)
    Canvas(modifier) {
        val now = clock()
        val map = world.map
        val camera = IsoCamera(world.focus, Vec2(size.width, size.height), tile)
        val kick = Offset(sin(now * .09f) * world.shake * 7f, cos(now * .117f) * world.shake * 5f)
        drawRect(voidBrush())
        withTransform({ translate(kick.x, kick.y) }) {
            val window = window(camera, map)
            ground(camera, map, window, now)
            world.corpses.forEach { remains ->
                figure(cache.art(remains.icon), if(remains.boss) glyphs.boss else glyphs.mob, Muted,
                    camera.toScreen(remains.position).offset, tile.x * .5f, remains.fade * .45f)
            }
            altar(camera, map, world.rules.boss, now)
            // Terrain and bodies share one painter's order, walked diagonal by diagonal: everything on
            // the same band is drawn together, so a body behind a wall is hidden by it.
            for(band in window.left + window.top..window.right + window.bottom) {
                for(tx in window.left..window.right) {
                    val ty = band - tx
                    if(ty < window.top || ty > window.bottom) continue
                    val terrain = map.terrain(tx, ty)
                    if(terrain == Terrain.WALL || terrain == Terrain.PILLAR)
                        block(camera, scratch, tx, ty, map, terrain)
                }
                map.decor.forEach { if(camera.depth(it.position).toBand() == band) scenery(camera, it, now) }
                world.loot.forEach { if(camera.depth(it.position).toBand() == band) dropPile(camera, it, cache, glyphs, now) }
                world.mobs.forEach {
                    if(!it.gone && camera.depth(it.position).toBand() == band)
                        body(camera, it, cache.art(it.icon), if(it.boss) glyphs.boss else glyphs.mob,
                            elementTint(it.element), measurer, labels.name, now)
                }
                world.hero?.takeIf { !it.gone && camera.depth(it.position).toBand() == band }?.let {
                    body(camera, it, cache.art(it.icon), glyphs.hero, Gold, measurer, labels.name, now)
                }
            }
            world.sparks.forEach { sparkle(camera, it) }
            world.motes.forEach { mote(camera, it, cache, glyphs) }
            world.numbers.forEach { floater(camera, it, measurer, labels.number) }
        }
        torchlight(camera, world.hero)
        world.banner?.let { callBanner(it, measurer, labels.banner) }
        stick()?.let { joystick(it) }
    }
}

private val Vec2.offset: Offset get() = Offset(x, y)
private fun Float.toBand() = floor(this).toInt()

/** The rectangle of tiles the screen can possibly show, so nothing off-camera is ever drawn. */
private class Window(val left: Int, val top: Int, val right: Int, val bottom: Int)

private fun DrawScope.window(camera: IsoCamera, map: Battlefield): Window {
    val corners = listOf(Vec2(0f, 0f), Vec2(size.width, 0f), Vec2(0f, size.height), Vec2(size.width, size.height))
        .map { camera.toWorld(it) }
    // A tall body is drawn from a tile below the one it stands on, so the window is grown by two.
    return Window(
        (corners.minOf { it.x } - 2f).toBand().coerceIn(0, map.width - 1),
        (corners.minOf { it.y } - 2f).toBand().coerceIn(0, map.height - 1),
        (corners.maxOf { it.x } + 2f).toBand().coerceIn(0, map.width - 1),
        (corners.maxOf { it.y } + 2f).toBand().coerceIn(0, map.height - 1))
}

/**
 * The floor: one lit plate for the whole zone, its tile lattice, then the tiles that are not plain.
 *
 * A diamond per tile would be nine hundred paths a frame; the ground is mostly texture, so only water
 * and shingle are actually drawn and the rest is one polygon.
 */
private fun DrawScope.ground(camera: IsoCamera, map: Battlefield, window: Window, now: Long) {
    val plate = Path()
    val corners = listOf(Vec2(0f, 0f), Vec2(map.width.toFloat(), 0f),
        Vec2(map.width.toFloat(), map.height.toFloat()), Vec2(0f, map.height.toFloat()))
        .map { camera.toScreen(it).offset }
    plate.moveTo(corners[0].x, corners[0].y)
    corners.drop(1).forEach { plate.lineTo(it.x, it.y) }
    plate.close()
    drawPath(plate, Brush.radialGradient(listOf(PanelRaised, Panel, Abyss),
        camera.toScreen(map.centre).offset, size.maxDimension * .8f))
    for(step in 0..map.width) tileLine(camera, Vec2(step.toFloat(), 0f), Vec2(step.toFloat(), map.height.toFloat()))
    for(step in 0..map.height) tileLine(camera, Vec2(0f, step.toFloat()), Vec2(map.width.toFloat(), step.toFloat()))
    val diamond = Path()
    for(ty in window.top..window.bottom) for(tx in window.left..window.right) when(map.terrain(tx, ty)) {
        Terrain.RUBBLE -> tileFace(camera, diamond, tx, ty, 0f, Color(0xFF1A1F27))
        Terrain.WATER -> {
            tileFace(camera, diamond, tx, ty, 0f, Color(0xFF10242F))
            tileFace(camera, diamond, tx, ty, 0f, ShieldCyan.copy(alpha = .10f + .05f * sin(now * .0016f + tx + ty)))
        }
        else -> Unit
    }
    drawPath(plate, Bronze.copy(alpha = .45f), style = Stroke(3f))
}

private fun DrawScope.tileLine(camera: IsoCamera, from: Vec2, to: Vec2) {
    drawLine(Bronze.copy(alpha = .10f), camera.toScreen(from).offset, camera.toScreen(to).offset, 1f)
}

/** The diamond of one tile, optionally lifted, drawn into a path that is reused every time. */
private fun DrawScope.tileFace(camera: IsoCamera, path: Path, tx: Int, ty: Int, lift: Float, colour: Color) {
    val north = camera.toScreen(Vec2(tx.toFloat(), ty.toFloat())).offset
    val east = camera.toScreen(Vec2(tx + 1f, ty.toFloat())).offset
    val south = camera.toScreen(Vec2(tx + 1f, ty + 1f)).offset
    val west = camera.toScreen(Vec2(tx.toFloat(), ty + 1f)).offset
    path.reset()
    path.moveTo(north.x, north.y - lift); path.lineTo(east.x, east.y - lift)
    path.lineTo(south.x, south.y - lift); path.lineTo(west.x, west.y - lift); path.close()
    drawPath(path, colour)
}

/** A wall or a column as a solid block; faces buried in the next block are not drawn at all. */
private fun DrawScope.block(camera: IsoCamera, scratch: Scratch, tx: Int, ty: Int, map: Battlefield, terrain: Terrain) {
    val tall = terrain == Terrain.PILLAR
    val lift = camera.tile.y * if(tall) 2.6f else 1.5f
    val east = camera.toScreen(Vec2(tx + 1f, ty.toFloat())).offset
    val south = camera.toScreen(Vec2(tx + 1f, ty + 1f)).offset
    val west = camera.toScreen(Vec2(tx.toFloat(), ty + 1f)).offset
    if(!solid(map, tx + 1, ty)) face(scratch.path, south, east, lift, Color(0xFF232A34))
    if(!solid(map, tx, ty + 1)) face(scratch.path, west, south, lift, Color(0xFF151A22))
    tileFace(camera, scratch.path, tx, ty, lift, if(tall) Color(0xFF39424F) else Color(0xFF2E3644))
    if(tall) drawLine(Bronze.copy(alpha = .30f), west - Offset(0f, lift), south - Offset(0f, lift), 1.4f)
}

private fun solid(map: Battlefield, x: Int, y: Int) =
    map.terrain(x, y).let { it == Terrain.WALL || it == Terrain.PILLAR }

/** One upright side of a block, between two ground corners. */
private fun DrawScope.face(path: Path, from: Offset, to: Offset, lift: Float, colour: Color) {
    path.reset()
    path.moveTo(from.x, from.y); path.lineTo(to.x, to.y)
    path.lineTo(to.x, to.y - lift); path.lineTo(from.x, from.y - lift); path.close()
    drawPath(path, colour)
}

/** The summoning circle: dead stone until the zone's kill count opens it, then it burns. */
private fun DrawScope.altar(camera: IsoCamera, map: Battlefield, armed: Boolean, now: Long) {
    val centre = camera.toScreen(map.altar).offset
    val span = camera.tile.x * .9f
    val pulse = if(armed) .55f + .45f * sin(now * .004f) else .16f
    val tint = if(armed) GoldBright else Bronze
    drawOval(tint.copy(alpha = .16f * pulse),
        topLeft = Offset(centre.x - span / 2f, centre.y - span / 4f), size = Size(span, span / 2f))
    drawOval(tint.copy(alpha = .75f * pulse), topLeft = Offset(centre.x - span / 2f, centre.y - span / 4f),
        size = Size(span, span / 2f), style = Stroke(2f))
    drawOval(tint.copy(alpha = .50f * pulse), topLeft = Offset(centre.x - span / 3f, centre.y - span / 6f),
        size = Size(span / 1.5f, span / 3f), style = Stroke(1.4f))
    if(armed) drawCircle(GoldBright.copy(alpha = .22f * pulse), span * .30f, centre - Offset(0f, span * .22f))
}

/** Braziers, bones and driftwood: what a place looks like once something has lived and died in it. */
private fun DrawScope.scenery(camera: IsoCamera, piece: Decor, now: Long) {
    val base = camera.toScreen(piece.position).offset
    val span = camera.tile.x * .3f
    when(piece.kind) {
        DecorKind.BRAZIER -> {
            val flame = span * (1.1f + .2f * sin(now * .006f + piece.position.x))
            drawCircle(Ember.copy(alpha = .14f), flame * 2.2f, base - Offset(0f, span))
            drawRect(Color(0xFF181D25), Offset(base.x - span * .5f, base.y - span * .8f), Size(span, span * .8f))
            drawCircle(Ember, flame * .5f, base - Offset(0f, span * 1.2f))
            drawCircle(GoldBright.copy(alpha = .85f), flame * .24f, base - Offset(0f, span * 1.45f))
        }
        DecorKind.BONES -> {
            drawLine(Muted.copy(alpha = .30f), base - Offset(span, span * .3f), base + Offset(span, span * .3f), 2f)
            drawLine(Muted.copy(alpha = .22f), base - Offset(span * .5f, -span * .4f), base + Offset(span * .6f, -span * .3f), 1.6f)
        }
        DecorKind.DRIFTWOOD -> {
            drawLine(Bronze.copy(alpha = .55f), base - Offset(span * 1.2f, 0f), base + Offset(span * 1.2f, span * .3f), 3f)
            drawLine(Bronze.copy(alpha = .35f), base - Offset(span * .4f, span * .3f), base + Offset(span * .8f, -span * .1f), 2f)
        }
    }
}

/**
 * One body: the ring it stands in, its pose offset, hit flash, picture, a life bar and its name.
 *
 * The bar is [WorldActor.lifeRatio] — the server's life over the server's maximum — never a number
 * this screen kept for itself.
 */
private fun DrawScope.body(camera: IsoCamera, actor: WorldActor, art: ServerArt?, glyph: Painter,
    accent: Color, measurer: TextMeasurer, style: TextStyle, now: Long) {
    val progress = actor.poseProgress
    val lunge = when(actor.pose) {
        WorldPose.WINDUP -> -.16f * progress
        WorldPose.STRIKE -> .42f * sin(progress * PI.toFloat())
        WorldPose.RECOIL -> -.24f * (1f - progress)
        else -> 0f
    }
    val alpha = when(actor.pose) {
        WorldPose.SPAWN -> progress
        WorldPose.DEATH -> 1f - progress
        else -> 1f
    }
    val grow = if(actor.pose == WorldPose.SPAWN) .62f + .38f * progress else 1f
    val stride = if(actor.walking) abs(sin(actor.bob * 9f)) * 4f else sin(actor.bob * 2.1f) * 2.4f
    val feet = camera.toScreen(actor.position + actor.heading * lunge).offset
    val span = camera.tile.x * (if(actor.boss) 1.05f else .78f) * grow
    val centre = feet - Offset(0f, span * .48f + stride)
    // A ring on the ground says who is in this fight: the exile, and the mob the server put opposite.
    val ring = when {
        actor.side == WorldSide.HERO -> Gold
        actor.engaged -> LifeRed
        else -> Color.Transparent
    }
    shadowPool(feet, span * 1.15f, alpha)
    if(ring != Color.Transparent) footRing(feet, camera.tile.x * .62f, ring, alpha, now)
    when(actor.pose) {
        WorldPose.GUARD -> ward(centre, span, progress)
        WorldPose.CAST -> sigilRing(centre, span, accent, progress)
        WorldPose.QUAFF -> drawCircle(Vital.copy(alpha = .22f * (1f - progress)), span * .64f, centre)
        else -> Unit
    }
    if(actor.shield > 0.0) drawCircle(ShieldCyan.copy(alpha = .28f * alpha), span * .58f, centre, style = Stroke(1.6f))
    // The picture is mirrored when the body faces west, so a walk reads as a walk and not a moonwalk.
    figure(art, glyph, accent, centre, span, alpha, flip = actor.heading.x - actor.heading.y < 0f)
    if(actor.flash > 0f) drawCircle(Color.White.copy(alpha = .40f * actor.flash * alpha), span * .44f, centre)
    val barTop = centre.y - span * .62f
    lifeBar(Offset(centre.x - span * .40f, barTop), span * .80f, actor.lifeRatio,
        if(actor.side == WorldSide.HERO) Gold else accent, alpha)
    if(actor.side == WorldSide.HERO || actor.engaged || actor.boss)
        label(measurer, actor.name, Offset(centre.x, barTop - 11f), style,
            (if(actor.boss) Blood else Parchment).copy(alpha = alpha), shadowed = true)
}

/** The lit ellipse a fighter stands in, so the eye finds them on a floor full of stone. */
private fun DrawScope.footRing(feet: Offset, span: Float, tint: Color, alpha: Float, now: Long) {
    val pulse = .65f + .35f * sin(now * .005f)
    drawOval(tint.copy(alpha = .40f * alpha * pulse),
        topLeft = Offset(feet.x - span / 2f, feet.y - span / 4f), size = Size(span, span / 2f), style = Stroke(2.2f))
    drawOval(tint.copy(alpha = .10f * alpha),
        topLeft = Offset(feet.x - span / 2f, feet.y - span / 4f), size = Size(span, span / 2f))
}

/**
 * Grounding ellipse: without it the bodies float over the floor.
 *
 * Two flat ovals rather than a gradient: a brush is laid out in canvas space, not inside the shape it
 * fills, so a soft blob would have to be positioned by hand for every body on the floor.
 */
private fun DrawScope.shadowPool(feet: Offset, span: Float, alpha: Float) {
    drawOval(Color.Black.copy(alpha = .30f * alpha),
        topLeft = Offset(feet.x - span * .60f, feet.y - span * .18f), size = Size(span * 1.20f, span * .36f))
    drawOval(Color.Black.copy(alpha = .42f * alpha),
        topLeft = Offset(feet.x - span * .40f, feet.y - span * .12f), size = Size(span * .80f, span * .24f))
}

private fun DrawScope.lifeBar(topLeft: Offset, width: Float, ratio: Float, accent: Color, alpha: Float) {
    drawRect(Abyss.copy(alpha = .86f * alpha), topLeft, Size(width, 5f))
    if(ratio > 0f) drawRect(LifeRed.copy(alpha = alpha), topLeft, Size(width * ratio, 5f))
    drawRect(accent.copy(alpha = .60f * alpha), topLeft, Size(width, 5f), style = Stroke(1f))
}

/** The guard stance: a bronze ring braced against whatever is coming. */
private fun DrawScope.ward(centre: Offset, span: Float, progress: Float) {
    drawCircle(Bronze.copy(alpha = .70f * (1f - progress * .4f)), span * .62f, centre, style = Stroke(3f))
}

/** The heavy strike: a rune ring that opens as the mana the server spent leaves the exile. */
private fun DrawScope.sigilRing(centre: Offset, span: Float, accent: Color, progress: Float) {
    drawCircle(accent.copy(alpha = .40f * (1f - progress)), span * (.42f + .36f * progress), centre, style = Stroke(2f))
    drawCircle(ManaBlue.copy(alpha = .26f * (1f - progress)), span * (.24f + .24f * progress), centre, style = Stroke(1.4f))
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

private fun DrawScope.sparkle(camera: IsoCamera, spark: Spark) {
    val reach = spark.speed * spark.progress
    val head = camera.toScreen(spark.origin + Vec2(cos(spark.angle), sin(spark.angle)) * reach).offset
    drawCircle(tintOf(spark.tint).copy(alpha = 1f - spark.progress), 2.8f * (1f - spark.progress) + .6f, head)
}

/** A reward on the floor: a beam in its rarity colour that the exile collects by walking over it. */
private fun DrawScope.dropPile(camera: IsoCamera, pile: GroundLoot, cache: ArtCache, glyphs: WorldGlyphs, now: Long) {
    val base = camera.toScreen(pile.position).offset
    val tint = lootTint(pile)
    val pulse = .70f + .30f * sin(now * .004f + pile.position.x)
    val height = camera.tile.y * 3f
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, tint.copy(alpha = .38f * pulse)),
        startY = base.y - height, endY = base.y),
        topLeft = Offset(base.x - camera.tile.x * .08f, base.y - height), size = Size(camera.tile.x * .16f, height))
    drawOval(tint.copy(alpha = .30f * pulse),
        topLeft = Offset(base.x - camera.tile.x * .3f, base.y - camera.tile.y * .18f),
        size = Size(camera.tile.x * .6f, camera.tile.y * .36f))
    figure(cache.art(pile.icon), lootGlyph(pile.kind, glyphs), tint, base - Offset(0f, camera.tile.y * .6f),
        camera.tile.x * .38f, 1f)
}

/** A claimed drop flying to the exile's pack. */
private fun DrawScope.mote(camera: IsoCamera, mote: LootMote, cache: ArtCache, glyphs: WorldGlyphs) {
    val alpha = 1f - mote.progress * mote.progress
    val centre = camera.toScreen(mote.position).offset - Offset(0f, camera.tile.y * (.5f + mote.progress))
    val tint = lootTint(mote.loot)
    drawCircle(tint.copy(alpha = .22f * alpha), camera.tile.x * .22f, centre)
    figure(cache.art(mote.loot.icon), lootGlyph(mote.loot.kind, glyphs), tint, centre, camera.tile.x * .3f, alpha)
}

private fun lootGlyph(kind: LootKind, glyphs: WorldGlyphs) = when(kind) {
    LootKind.EQUIPMENT -> glyphs.gear
    LootKind.EXPERIENCE -> glyphs.mark
    LootKind.GOLD -> glyphs.coin
    LootKind.CURRENCY -> glyphs.orb
}

/** Equipment wears its rolled rarity; the rest of the rewards keep their own colours. */
internal fun lootTint(pile: GroundLoot) = when {
    pile.kind == LootKind.EQUIPMENT && pile.rarity.isNotBlank() -> rarityColor(pile.rarity)
    pile.kind == LootKind.EQUIPMENT -> Gold
    pile.kind == LootKind.CURRENCY -> Rune
    pile.kind == LootKind.EXPERIENCE -> GoldBright
    else -> Bronze
}

private fun DrawScope.floater(camera: IsoCamera, number: DamageNumber, measurer: TextMeasurer, style: TextStyle) {
    val alpha = (1f - number.progress * number.progress).coerceIn(0f, 1f)
    val scale = if(number.crit) 1.35f + .25f * (1f - number.progress) else 1f
    val centre = camera.toScreen(number.origin).offset - Offset(0f, camera.tile.y * 1.1f + number.progress * size.height * .13f)
    label(measurer, number.text, centre, style.copy(fontSize = style.fontSize * scale),
        tintOf(number.tint).copy(alpha = alpha), shadowed = true)
}

/** The torch the exile carries: the dark closes in, but never so far that the fight is lost in it. */
private fun DrawScope.torchlight(camera: IsoCamera, hero: WorldActor?) {
    val light = camera.toScreen(hero?.position ?: camera.focus).offset
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Ink.copy(alpha = .16f), Ink.copy(alpha = .62f)),
        center = light, radius = size.minDimension * 1.05f))
}

/** Victory, defeat, a retreat, or the name of whatever just walked in. */
private fun DrawScope.callBanner(banner: Banner, measurer: TextMeasurer, style: TextStyle) {
    val alpha = when {
        banner.progress < .12f -> banner.progress / .12f
        banner.progress > .72f -> (1f - banner.progress) / .28f
        else -> 1f
    }.coerceIn(0f, 1f)
    val accent = if(banner.boss) Blood else Gold
    val middle = size.height * .22f
    drawRect(Brush.horizontalGradient(listOf(Color.Transparent, Ink.copy(alpha = .78f * alpha), Color.Transparent)),
        topLeft = Offset(0f, middle - 20f), size = Size(size.width, 40f))
    label(measurer, banner.text.uppercase(), Offset(size.width / 2f, middle), style, accent.copy(alpha = alpha))
    drawLine(Brush.horizontalGradient(listOf(Color.Transparent, accent.copy(alpha = .60f * alpha), Color.Transparent)),
        Offset(0f, middle + 18f), Offset(size.width, middle + 18f), 1.2f)
}

/** The floating stick: it appears under the thumb that put it there and nowhere else. */
private fun DrawScope.joystick(stick: StickState) {
    val radius = size.minDimension * .12f
    val reach = stick.thumb - stick.origin
    val knob = stick.origin + (if(reach.getDistance() > radius) reach / reach.getDistance() * radius else reach)
    drawCircle(Ink.copy(alpha = .42f), radius, stick.origin)
    drawCircle(Gold.copy(alpha = .38f), radius, stick.origin, style = Stroke(1.6f))
    drawCircle(Gold.copy(alpha = .26f), radius * .44f, knob)
    drawCircle(GoldBright.copy(alpha = .85f), radius * .44f, knob, style = Stroke(2f))
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
private fun tintOf(tint: WorldTint) = when(tint) {
    WorldTint.HERO_DAMAGE -> GoldBright
    WorldTint.MOB_DAMAGE -> LifeRed
    WorldTint.HEAL -> Vital
    WorldTint.MANA -> ManaBlue
    WorldTint.SHIELD -> ShieldCyan
    WorldTint.MISS -> Muted
    WorldTint.GUARD -> Rune
    WorldTint.LOOT -> Gold
}

/** The mob's damage type, in the theme's own colours; an unknown element stays bone white. */
internal fun elementTint(element: String) = when(element.lowercase()) {
    "fire" -> Ember
    "cold" -> ShieldCyan
    "lightning" -> Gold
    "chaos" -> rarityColor("EPIC")
    else -> Parchment
}
