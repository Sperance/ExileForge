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

/**
 * The expedition drawn as an isometric floor with the camera tied to the hero.
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
        val camera = IsoCamera(world.focus, Vec2(size.width, size.height), tile)
        val kick = Offset(sin(now * .09f) * world.shake * 7f, cos(now * .117f) * world.shake * 5f)
        // Painter's order: whatever stands further down the floor hides what is behind it.
        val standing = ArrayList<Pair<Float, DrawScope.() -> Unit>>(48)
        fun stand(at: Vec2, draw: DrawScope.() -> Unit) { standing += camera.depth(at) to draw }
        world.field.props.forEach { piece -> if(piece.blocking) stand(piece.position) { scenery(camera, piece, now) } }
        world.loot.forEach { pile -> stand(pile.position) { dropPile(camera, pile, cache, glyphs, now) } }
        world.mobs.forEach { mob ->
            if(!mob.gone) stand(mob.position) {
                body(camera, mob, cache.art(mob.icon), if(mob.boss) glyphs.boss else glyphs.mob,
                    elementTint(mob.element), measurer, labels.name)
            }
        }
        world.hero?.takeIf { !it.gone }?.let { exile ->
            stand(exile.position) { body(camera, exile, cache.art(exile.icon), glyphs.hero, Gold, measurer, labels.name) }
        }
        standing.sortBy { it.first }
        drawRect(voidBrush())
        withTransform({ translate(kick.x, kick.y) }) {
            battleground(camera, world.field, now)
            world.corpses.forEach { remains ->
                figure(cache.art(remains.icon), if(remains.boss) glyphs.boss else glyphs.mob, Muted,
                    camera.toScreen(remains.position).offset, tile.x * .48f, remains.fade * .45f)
            }
            standing.forEach { it.second(this) }
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

/**
 * The ground: a ledge of rock, the lit floor of the zone, its tile lattice and the wall around it.
 *
 * One polygon and a few dozen lines rather than a diamond per tile — a floor is mostly texture, and a
 * thousand paths a frame would cost more than the whole fight does.
 */
private fun DrawScope.battleground(camera: IsoCamera, field: Battlefield, now: Long) {
    ledge(camera, field, -1.6f, Ink)
    ledge(camera, field, -.7f, Color(0xFF101720))
    val plate = ledge(camera, field, 0f, null)
    val middle = camera.toScreen(field.centre).offset
    drawPath(plate, Brush.radialGradient(listOf(PanelRaised, Panel, Abyss), middle, size.maxDimension * .72f))
    for(step in 0..field.width.toInt()) tileLine(camera, Vec2(step.toFloat(), 0f), Vec2(step.toFloat(), field.height))
    for(step in 0..field.height.toInt()) tileLine(camera, Vec2(0f, step.toFloat()), Vec2(field.width, step.toFloat()))
    drawPath(plate, Bronze.copy(alpha = .55f), style = Stroke(3f))
    field.props.filter { !it.blocking }.forEach { bones(camera, it) }
    // A slow pulse over the stone, so a still frame never looks like a frozen one.
    drawPath(plate, Gold.copy(alpha = .03f + .02f * sin(now * .0011f)))
}

/** The field outline, optionally grown by [inset] tiles to read as the rock the floor is cut into. */
private fun DrawScope.ledge(camera: IsoCamera, field: Battlefield, inset: Float, fill: Color?): Path {
    val corners = listOf(Vec2(inset, inset), Vec2(field.width - inset, inset),
        Vec2(field.width - inset, field.height - inset), Vec2(inset, field.height - inset))
        .map { camera.toScreen(it).offset }
    val path = Path().apply {
        moveTo(corners[0].x, corners[0].y)
        corners.drop(1).forEach { lineTo(it.x, it.y) }
        close()
    }
    fill?.let { drawPath(path, it) }
    return path
}

private fun DrawScope.tileLine(camera: IsoCamera, from: Vec2, to: Vec2) {
    drawLine(Bronze.copy(alpha = .13f), camera.toScreen(from).offset, camera.toScreen(to).offset, 1f)
}

/** Stones, pillars and braziers: the cover a swipe has to walk around. */
private fun DrawScope.scenery(camera: IsoCamera, piece: Prop, now: Long) {
    val base = camera.toScreen(piece.position).offset
    val span = camera.tile.x * piece.radius
    when(piece.kind) {
        PropKind.ROCK -> {
            shadowPool(base, span * 1.6f, .7f)
            val top = base - Offset(0f, span * .62f)
            drawPath(Path().apply {
                moveTo(base.x - span, base.y); lineTo(top.x - span * .5f, top.y)
                lineTo(top.x + span * .4f, top.y - span * .2f); lineTo(base.x + span, base.y); close()
            }, Brush.verticalGradient(listOf(PanelRaised, Color(0xFF12171E)), top.y - span, base.y))
            drawLine(Bronze.copy(alpha = .28f), Offset(top.x - span * .5f, top.y), Offset(top.x + span * .4f, top.y - span * .2f), 1.4f)
        }
        PropKind.PILLAR -> {
            shadowPool(base, span * 1.7f, .8f)
            val height = span * 3.4f
            drawRect(Brush.horizontalGradient(listOf(Color(0xFF151A22), PanelRaised, Color(0xFF0E131A)),
                startX = base.x - span * .52f, endX = base.x + span * .52f),
                topLeft = Offset(base.x - span * .52f, base.y - height), size = Size(span * 1.04f, height))
            drawRect(Bronze.copy(alpha = .40f), Offset(base.x - span * .66f, base.y - height - span * .22f),
                Size(span * 1.32f, span * .22f))
        }
        PropKind.BRAZIER -> {
            shadowPool(base, span * 1.4f, .6f)
            val flame = span * (1.1f + .18f * sin(now * .006f + piece.position.x))
            drawCircle(Ember.copy(alpha = .16f), flame * 1.8f, base - Offset(0f, span))
            drawCircle(Ember, flame * .42f, base - Offset(0f, span * 1.1f))
            drawCircle(GoldBright.copy(alpha = .8f), flame * .2f, base - Offset(0f, span * 1.25f))
            drawRect(Color(0xFF181D25), Offset(base.x - span * .5f, base.y - span * .5f), Size(span, span * .5f))
        }
        PropKind.BONES -> Unit
    }
}

/** Bones lie flat on the floor: scenery of a place that has eaten other exiles. */
private fun DrawScope.bones(camera: IsoCamera, piece: Prop) {
    val base = camera.toScreen(piece.position).offset
    val span = camera.tile.x * piece.radius
    drawLine(Muted.copy(alpha = .22f), base - Offset(span, span * .3f), base + Offset(span, span * .3f), 2f)
    drawLine(Muted.copy(alpha = .16f), base - Offset(span * .5f, -span * .4f), base + Offset(span * .6f, -span * .3f), 1.6f)
}

/**
 * One body: its pose offset, hit flash, picture, a life bar and its name.
 *
 * The bar is [WorldActor.lifeRatio] — the server's life over the server's maximum — never a number
 * this screen kept for itself.
 */
private fun DrawScope.body(camera: IsoCamera, actor: WorldActor, art: ServerArt?, glyph: Painter,
    accent: Color, measurer: TextMeasurer, style: TextStyle) {
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
    val stride = if(actor.walking) abs(sin(actor.bob * 9f)) * 3.5f else sin(actor.bob * 2.1f) * 2.2f
    val feet = camera.toScreen(actor.position + actor.heading * lunge).offset
    val span = camera.tile.x * (if(actor.boss) .92f else .62f) * grow
    val centre = feet - Offset(0f, span * .48f + stride)
    shadowPool(feet, span * 1.25f, alpha)
    when(actor.pose) {
        WorldPose.GUARD -> ward(centre, span, progress)
        WorldPose.CAST -> sigilRing(centre, span, accent, progress)
        WorldPose.QUAFF -> drawCircle(Vital.copy(alpha = .22f * (1f - progress)), span * .64f, centre)
        else -> Unit
    }
    if(actor.shield > 0.0) drawCircle(ShieldCyan.copy(alpha = .28f * alpha), span * .58f, centre, style = Stroke(1.6f))
    // The picture is mirrored when the body faces west, so a walk reads as a walk and not a moonwalk.
    figure(art, glyph, accent, centre, span, alpha, flip = actor.heading.x - actor.heading.y < 0f)
    if(actor.flash > 0f) drawCircle(Color.White.copy(alpha = .34f * actor.flash * alpha), span * .44f, centre)
    val barTop = centre.y - span * .68f
    lifeBar(Offset(centre.x - span * .46f, barTop), span * .92f, actor.lifeRatio,
        if(actor.side == WorldSide.HERO) Gold else accent, alpha)
    label(measurer, actor.name, Offset(centre.x, barTop - 11f), style,
        (if(actor.boss) Blood else Parchment).copy(alpha = alpha * if(actor.ambient) .55f else 1f))
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

/** The heavy strike: a rune ring that opens as the mana the server spent leaves the hero. */
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

/** A reward on the floor: a beam in its rarity colour that the hero collects by walking over it. */
private fun DrawScope.dropPile(camera: IsoCamera, pile: GroundLoot, cache: ArtCache, glyphs: WorldGlyphs, now: Long) {
    val base = camera.toScreen(pile.position).offset
    val tint = lootTint(pile)
    val pulse = .70f + .30f * sin(now * .004f + pile.position.x)
    val height = camera.tile.y * 2.6f
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, tint.copy(alpha = .34f * pulse)),
        startY = base.y - height, endY = base.y),
        topLeft = Offset(base.x - camera.tile.x * .07f, base.y - height), size = Size(camera.tile.x * .14f, height))
    drawOval(tint.copy(alpha = .26f * pulse),
        topLeft = Offset(base.x - camera.tile.x * .3f, base.y - camera.tile.y * .18f),
        size = Size(camera.tile.x * .6f, camera.tile.y * .36f))
    figure(cache.art(pile.icon), lootGlyph(pile.kind, glyphs), tint, base - Offset(0f, camera.tile.y * .5f),
        camera.tile.x * .34f, 1f)
}

/** A claimed drop flying to the hero's pack. */
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
    val centre = camera.toScreen(number.origin).offset - Offset(0f, camera.tile.y * .9f + number.progress * size.height * .13f)
    label(measurer, number.text, centre, style.copy(fontSize = style.fontSize * scale),
        tintOf(number.tint).copy(alpha = alpha), shadowed = true)
}

/** The torch the exile carries: the dark closes in everywhere the hero is not. */
private fun DrawScope.torchlight(camera: IsoCamera, hero: WorldActor?) {
    val light = camera.toScreen(hero?.position ?: camera.focus).offset
    drawRect(Brush.radialGradient(listOf(Color.Transparent, Ink.copy(alpha = .34f), Ink.copy(alpha = .88f)),
        center = light, radius = size.minDimension * .92f))
}

/** Victory, defeat, a retreat, or the name of whatever just walked in. */
private fun DrawScope.callBanner(banner: Banner, measurer: TextMeasurer, style: TextStyle) {
    val alpha = when {
        banner.progress < .12f -> banner.progress / .12f
        banner.progress > .72f -> (1f - banner.progress) / .28f
        else -> 1f
    }.coerceIn(0f, 1f)
    val accent = if(banner.boss) Blood else Gold
    val middle = size.height * .17f
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
