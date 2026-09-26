package com.sperance.exileforge.ui.icons

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.model.currency.CurrencyItem
import com.sperance.exileforge.core.model.currency.CurrencyOrb
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.Dp
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.sin

/**
 * The orbs as stained glass (2.69.0, the owner's mockup C «Витраж»): three flat tones of the orb's
 * own hue in a heavy dark lead line, its emblem in a gold-rimmed medallion, and rays round the
 * precious ones. Every orb is a colour and a sign, so two orbs are told apart at 20 dp by either.
 *
 * Each is an [ImageVector] built once and kept: a bag of twenty stacks draws the same few again.
 * It is painted as an [Image], never an `Icon` — an icon's tint would flatten the glass to one colour.
 */
@Composable fun OrbGlyph(orb: CurrencyOrb?, modifier: Modifier = Modifier, description: String? = null) =
    Image(rememberVectorPainter(orbVector(orb)), description, modifier)

/**
 * A picker's art for orb options keyed by their `items` id (2.69.1): the orb's glass, or nothing for
 * a key that is not an orb — «любая сфера», an empty choice — which then keeps the list's own spacing.
 */
fun orbArt(orbs: List<CurrencyItem>): @Composable (String, Dp) -> Unit = { key, size ->
    orbs.firstOrNull { it.id == key }?.let { OrbGlyph(it.orb, Modifier.size(size)) } ?: Spacer(Modifier.size(size))
}

/** One orb's look: its glass, its sign, and whether it is precious enough for rays. */
private class OrbArt(val hue: Long, val emblem: Emblem, val rays: Boolean = false)

private val art = mapOf(
    CurrencyOrb.ORB_OF_TRANSMUTATION to OrbArt(0xFF7FB8E8, Emblem.ARROW_UP),
    CurrencyOrb.ORB_OF_AUGMENTATION to OrbArt(0xFF6FA0E0, Emblem.PLUS),
    CurrencyOrb.ORB_OF_ALTERATION to OrbArt(0xFF8A9CFF, Emblem.SWAP),
    CurrencyOrb.ORB_OF_ALCHEMY to OrbArt(0xFFE8C060, Emblem.FLASK),
    CurrencyOrb.REGAL_ORB to OrbArt(0xFFE0B040, Emblem.CROWN),
    CurrencyOrb.CHAOS_ORB to OrbArt(0xFFD4A030, Emblem.SPIRAL),
    CurrencyOrb.EXALTED_ORB to OrbArt(0xFFF0E0B0, Emblem.STAR, rays = true),
    CurrencyOrb.DIVINE_ORB to OrbArt(0xFFFFE8A0, Emblem.SUN, rays = true),
    CurrencyOrb.ORB_OF_ANNULMENT to OrbArt(0xFFC8D0E0, Emblem.MINUS),
    CurrencyOrb.ORB_OF_SCOURING to OrbArt(0xFFB8C8C8, Emblem.WAVE),
    CurrencyOrb.BLESSED_ORB to OrbArt(0xFFA8E0F0, Emblem.DROP),
    CurrencyOrb.VAAL_ORB to OrbArt(0xFFE04030, Emblem.EYE, rays = true),
    CurrencyOrb.ORB_OF_CHANCE to OrbArt(0xFF90D070, Emblem.CLOVER),
    CurrencyOrb.MIRROR_OF_KALANDRA to OrbArt(0xFFE8F4FF, Emblem.MIRROR, rays = true),
    CurrencyOrb.FRACTURING_ORB to OrbArt(0xFFC0A070, Emblem.CRACK),
    CurrencyOrb.SHAPERS_ORB to OrbArt(0xFF7FC8F0, Emblem.HEX, rays = true),
    CurrencyOrb.ELDER_ORB to OrbArt(0xFFB080E0, Emblem.TENTACLE, rays = true),
    CurrencyOrb.ABYSS_ORB to OrbArt(0xFF8A4FE8, Emblem.RIFT, rays = true),
    CurrencyOrb.ORB_OF_REGRET to OrbArt(0xFF9AA8B8, Emblem.MOON),
    CurrencyOrb.EMPOWERING_ORB to OrbArt(0xFFF08040, Emblem.FLAME),
    CurrencyOrb.MERCY_ORB to OrbArt(0xFF80E0C0, Emblem.HEART),
    CurrencyOrb.PERIL_ORB to OrbArt(0xFFE05060, Emblem.SKULL),
    CurrencyOrb.HORDE_ORB to OrbArt(0xFFC09060, Emblem.DOTS),
    CurrencyOrb.MAGUS_ORB to OrbArt(0xFF8888FF, Emblem.RUNE),
    CurrencyOrb.ELITE_ORB to OrbArt(0xFFFFFF77, Emblem.GEM),
    CurrencyOrb.BOUNTY_ORB to OrbArt(0xFFE8CF94, Emblem.COIN),
    CurrencyOrb.TREASURE_ORB to OrbArt(0xFFD8B060, Emblem.GEM),
    CurrencyOrb.GILDED_ORB to OrbArt(0xFFF0D060, Emblem.COIN, rays = true),
    CurrencyOrb.WARDEN_ORB to OrbArt(0xFFB05050, Emblem.CROWN),
    CurrencyOrb.HELMET_SCROLL to OrbArt(0xFFC8B8E8, Emblem.RUNE),
    CurrencyOrb.GLOVES_SCROLL to OrbArt(0xFFB8C8E8, Emblem.RUNE),
    CurrencyOrb.BOOTS_SCROLL to OrbArt(0xFFB8E0C8, Emblem.RUNE),
    CurrencyOrb.WEAPON_SCROLL to OrbArt(0xFFE8C0B0, Emblem.RUNE),
    CurrencyOrb.ESSENCE_ORB to OrbArt(0xFFB07FE0, Emblem.CRYSTAL),
    CurrencyOrb.SCRIBE_ORB to OrbArt(0xFFD8C8A0, Emblem.BOOK),
    CurrencyOrb.GLASSBLOWERS_BAUBLE to OrbArt(0xFF9FD8E8, Emblem.FLASK),
)

/** An orb the client has no art for — one the server added later — is plain gold glass with a gem. */
private val unknown = OrbArt(0xFFC8AA6E, Emblem.GEM)

/** The signs, as path data in a 100-unit box centred on 50,50; [filled] ones are shapes, the rest strokes. */
private enum class Emblem(val d: String, val filled: Boolean) {
    ARROW_UP("M50 26 L66 46 H56 V72 H44 V46 H34 Z", true),
    PLUS("M44 30 H56 V44 H70 V56 H56 V70 H44 V56 H30 V44 H44 Z", true),
    SWAP("M30 42 H62 V34 L74 46 L62 58 V50 H30 Z M70 58 H38 V66 L26 54 L38 42", false),
    FLASK("M44 28 H56 V44 L70 70 Q72 74 67 74 H33 Q28 74 30 70 L44 44 Z", true),
    CROWN("M28 66 L32 38 L42 52 L50 32 L58 52 L68 38 L72 66 Z", true),
    SPIRAL("M50 50 m0 -4 a4 4 0 1 1 -4 4 a8 8 0 1 1 8 8 a12 12 0 1 1 -12 -12 a16 16 0 1 1 16 16 a20 20 0 1 1 -20 -20", false),
    STAR("M50 26 L56 44 L75 44 L60 55 L66 74 L50 62 L34 74 L40 55 L25 44 L44 44 Z", true),
    SUN("M50 38 a12 12 0 1 1 -0.1 0 Z M50 22 V30 M50 70 V78 M22 50 H30 M70 50 H78 M30 30 L36 36 M64 64 L70 70 M30 70 L36 64 M64 36 L70 30", false),
    MINUS("M30 44 H70 V56 H30 Z", true),
    WAVE("M26 44 Q34 34 42 44 T58 44 T74 44 M26 58 Q34 48 42 58 T58 58 T74 58", false),
    DROP("M50 26 Q66 48 66 58 A16 16 0 0 1 34 58 Q34 48 50 26 Z", true),
    EYE("M24 50 Q50 26 76 50 Q50 74 24 50 Z M50 42 a8 8 0 1 1 -0.1 0 Z", false),
    CLOVER("M50 48 a9 9 0 1 1 0 -1 M50 48 a9 9 0 1 0 0 1 M48 50 a9 9 0 1 1 -1 0 M52 50 a9 9 0 1 0 1 0 M50 56 V76", false),
    MIRROR("M50 24 A16 22 0 1 1 49.9 24 Z M44 34 L40 48 M52 30 L46 50", false),
    CRACK("M50 22 L44 40 L56 48 L42 62 L52 78", false),
    RIFT("M50 22 L58 40 L53 50 L62 62 L50 78 L41 60 L47 50 L39 36 Z", true),
    HEX("M50 26 L71 38 V62 L50 74 L29 62 V38 Z M50 38 L61 44 V56 L50 62 L39 56 V44 Z", true),
    TENTACLE("M36 74 Q30 50 44 40 Q56 32 50 24 M50 74 Q50 54 60 46 Q70 38 64 28 M64 74 Q70 58 76 52", false),
    MOON("M58 26 A24 24 0 1 0 58 74 A18 18 0 1 1 58 26 Z", true),
    FLAME("M50 24 Q66 42 60 56 Q70 52 66 64 A16 16 0 0 1 34 62 Q30 50 42 44 Q42 54 48 54 Q40 38 50 24 Z", true),
    HEART("M50 72 L30 52 A11 11 0 0 1 50 36 A11 11 0 0 1 70 52 Z", true),
    SKULL("M34 48 A16 16 0 1 1 66 48 V58 H60 V66 H40 V58 H34 Z M42 46 a4 4 0 1 1 -0.1 0 Z M58 46 a4 4 0 1 1 -0.1 0 Z", true),
    DOTS("M40 40 a6 6 0 1 1 -0.1 0 Z M60 40 a6 6 0 1 1 -0.1 0 Z M50 58 a6 6 0 1 1 -0.1 0 Z M34 60 a4 4 0 1 1 -0.1 0 Z M66 60 a4 4 0 1 1 -0.1 0 Z", true),
    RUNE("M50 24 V76 M50 36 L64 26 M50 50 L36 40 M50 50 L64 62", false),
    GEM("M36 36 H64 L74 48 L50 76 L26 48 Z M26 48 H74 M42 36 L38 48 L50 76 L62 48 L58 36", true),
    COIN("M50 30 a20 20 0 1 1 -0.1 0 Z M50 38 V62 M44 42 H55 Q60 42 60 47 Q60 50 50 50 Q40 50 40 55 Q40 58 45 58 H56", false),
    CRYSTAL("M50 22 L64 40 L58 76 H42 L36 40 Z M36 40 H64 M50 22 L46 40 L50 76", true),
    BOOK("M28 32 Q40 28 50 34 Q60 28 72 32 V70 Q60 66 50 72 Q40 66 28 70 Z M50 34 V72", false),
}

private val lead = SolidColor(Color(0xFF111111))
private val built = ConcurrentHashMap<CurrencyOrb, ImageVector>()
private val fallback by lazy { build(unknown) }

private fun orbVector(orb: CurrencyOrb?): ImageVector =
    orb?.let { built.getOrPut(it) { build(art[it] ?: unknown) } } ?: fallback

private fun nodes(d: String) = PathParser().parsePathString(d).toNodes()

private fun build(orb: OrbArt): ImageVector {
    val hue = Color(orb.hue)
    fun tone(f: Float) = Color((hue.red * f).coerceAtMost(1f), (hue.green * f).coerceAtMost(1f), (hue.blue * f).coerceAtMost(1f))
    return ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 100f, viewportHeight = 100f).apply {
        if (orb.rays) repeat(12) { k ->
            val a = k * Math.PI / 6
            fun at(r: Double, t: Double) = "${50 + r * cos(t)} ${50 + r * sin(t)}"
            addPath(nodes("M${at(36.0, a - .12)} L${at(48.0, a)} L${at(36.0, a + .12)} Z"), fill = SolidColor(Color(0xFFF0E2C0)), stroke = lead, strokeLineWidth = 2f)
        }
        val disc = "M14 50 A36 36 0 1 0 86 50 A36 36 0 1 0 14 50 Z"
        addPath(nodes(disc), fill = SolidColor(tone(.55f)))
        addPath(nodes("M50 14 A36 36 0 0 0 20 70 L50 50 Z"), fill = SolidColor(hue))
        addPath(nodes("M50 14 A36 36 0 0 1 80 70 L50 50 Z"), fill = SolidColor(tone(1.3f)))
        addPath(nodes("M50 14 V50 M20 70 L50 50 L80 70"), stroke = lead, strokeLineWidth = 3f)
        addPath(nodes(disc), stroke = lead, strokeLineWidth = 5f)
        addPath(nodes("M31 50 A19 19 0 1 0 69 50 A19 19 0 1 0 31 50 Z"), fill = SolidColor(Color(0xFF15171A)), stroke = SolidColor(Color(0xFFC8AA6E)), strokeLineWidth = 3f)
        group(scaleX = .34f, scaleY = .34f, pivotX = 50f, pivotY = 50f) {
            val sign = orb.emblem
            if (sign.filled) addPath(nodes(sign.d), fill = SolidColor(hue))
            else addPath(nodes(sign.d), stroke = SolidColor(hue), strokeLineWidth = 10f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
        }
    }.build()
}
