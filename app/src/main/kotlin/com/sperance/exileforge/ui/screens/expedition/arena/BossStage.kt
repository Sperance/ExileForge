package com.sperance.exileforge.ui.screens.expedition.arena

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.campaign.run.BossHud
import com.sperance.exileforge.core.campaign.run.FightHud
import com.sperance.exileforge.core.display.monsterTitle
import com.sperance.exileforge.ui.components.LocalMotion
import com.sperance.exileforge.ui.components.LocalSettings
import kotlin.math.sin

/** Как движутся частицы биома: поднимаются (искры, споры), падают (снег, пепел, капли) или плывут вбок (песок, туман). */
private enum class Drift { RISE, FALL, SIDE }

/** Небо биома для боя с боссом: верх и низ фона, цвет частиц и их ход. */
private data class Sky(val top: Color, val bottom: Color, val mote: Color, val drift: Drift)

private val SKIES = mapOf(
    "SHORE" to Sky(Color(0xFF16313A), Color(0xFF070D10), Color(0xFF9FD8E8), Drift.SIDE),
    "MIRE" to Sky(Color(0xFF1F2E1C), Color(0xFF080B07), Color(0xFF9FE8A0), Drift.RISE),
    "FOREST" to Sky(Color(0xFF1C2E1E), Color(0xFF070B08), Color(0xFFCFE89F), Drift.RISE),
    "JUNGLE" to Sky(Color(0xFF173220), Color(0xFF060C07), Color(0xFF9FF0B0), Drift.RISE),
    "CAVE" to Sky(Color(0xFF24392F), Color(0xFF070908), Color(0xFF9FE8C0), Drift.FALL),
    "MINES" to Sky(Color(0xFF2E2A22), Color(0xFF0A0907), Color(0xFFE8C88A), Drift.FALL),
    "CRYPT" to Sky(Color(0xFF241E2E), Color(0xFF09070C), Color(0xFFC8B0E8), Drift.RISE),
    "RUINS" to Sky(Color(0xFF2E2A24), Color(0xFF0B0A08), Color(0xFFE0D0B0), Drift.SIDE),
    "TEMPLE" to Sky(Color(0xFF2E2818), Color(0xFF0B0906), Color(0xFFF5D58A), Drift.RISE),
    "DESERT" to Sky(Color(0xFF3A2C18), Color(0xFF0E0A06), Color(0xFFF0D090), Drift.SIDE),
    "CANYON" to Sky(Color(0xFF3A2218), Color(0xFF0E0806), Color(0xFFE8A070), Drift.SIDE),
    "FROST" to Sky(Color(0xFF1C2A3A), Color(0xFF070A0E), Color(0xFFFFFFFF), Drift.FALL),
    "STORMPEAK" to Sky(Color(0xFF1E2238), Color(0xFF07080E), Color(0xFFF2E04A), Drift.FALL),
    "ASH" to Sky(Color(0xFF2E2220), Color(0xFF0B0807), Color(0xFFB8A8A0), Drift.FALL),
    "VOLCANO" to Sky(Color(0xFF3A1A10), Color(0xFF0E0605), Color(0xFFFF8A3A), Drift.RISE),
    "BLIGHT" to Sky(Color(0xFF26301A), Color(0xFF090B06), Color(0xFFC8E86A), Drift.RISE),
    "HIVE" to Sky(Color(0xFF332A14), Color(0xFF0C0A05), Color(0xFFF2C14A), Drift.SIDE),
    "CORAL" to Sky(Color(0xFF14303A), Color(0xFF060C0F), Color(0xFFFFB0C8), Drift.RISE),
    "SUNKEN" to Sky(Color(0xFF102A38), Color(0xFF050B0F), Color(0xFF9FE0F0), Drift.RISE),
    "TIDEVAULT" to Sky(Color(0xFF12283A), Color(0xFF050A0F), Color(0xFFB0E8F8), Drift.RISE),
    "CITADEL" to Sky(Color(0xFF2A2630), Color(0xFF0A090C), Color(0xFFE0D8F0), Drift.SIDE),
    "GLASSWASTE" to Sky(Color(0xFF2A3036), Color(0xFF0A0C0E), Color(0xFFE8F6FF), Drift.SIDE),
    "SKYREACH" to Sky(Color(0xFF223048), Color(0xFF080B12), Color(0xFFFFFFFF), Drift.SIDE),
    "ASTRAL" to Sky(Color(0xFF1E1838), Color(0xFF07060E), Color(0xFFF5E6A0), Drift.RISE),
    "GODHALL" to Sky(Color(0xFF302818), Color(0xFF0C0A06), Color(0xFFFFE8A0), Drift.RISE),
    "ABYSS" to Sky(Color(0xFF1E1230), Color(0xFF07040C), Color(0xFFC88AF0), Drift.RISE),
    "OBLIVION" to Sky(Color(0xFF181820), Color(0xFF050507), Color(0xFFA0A0B8), Drift.FALL),
)
private val DEFAULT_SKY = Sky(Color(0xFF24392F), Color(0xFF070908), Color(0xFF9FE8C0), Drift.RISE)

/**
 * Фон боя с боссом «Кино»: небо биома зоны, медленные частицы его погоды и свечение фазы снизу; к третьей фазе
 * свет уходит в её цвет. Без анимаций частицы стоят; частицы разгораются по воротам эффектов [fx] - после входа стража.
 */
@Composable internal fun BossBackdrop(biome: String, boss: BossHud, frame: FightFrame, fx: ArenaFxGate) {
    val sky = SKIES[biome] ?: DEFAULT_SKY
    val phase = phaseNumber(boss)
    val tint by animateFloatAsState((phase - 1).coerceIn(0, 2) / 2f, tween(1600), label = "phaseTint")
    val motion = LocalMotion.current
    Canvas(Modifier.fillMaxSize()) {
        val t = if (motion) frame.time else 0f
        drawRect(Brush.verticalGradient(listOf(sky.top, sky.bottom)))
        drawRect(Brush.radialGradient(listOf(phaseAura(phase).copy(alpha = .10f + .12f * tint), Color.Transparent), Offset(size.width / 2, size.height * .32f), size.width * .9f))
        repeat(34) { k ->
            val seed = (k * 37 % 101) / 101f
            val speed = .015f + .03f * ((k * 53 % 89) / 89f)
            val phaseOf = (t * speed + seed) % 1f
            val (x, y) = when (sky.drift) {
                Drift.RISE -> size.width * ((seed * 7.3f) % 1f) + sin(t * .7f + k) * 10.dp.toPx() to size.height * (1 - phaseOf)
                Drift.FALL -> size.width * ((seed * 5.1f) % 1f) + sin(t * .5f + k) * 14.dp.toPx() to size.height * phaseOf
                Drift.SIDE -> size.width * phaseOf to size.height * ((seed * 3.7f) % 1f) + sin(t + k) * 6.dp.toPx()
            }
            val fade = sin(phaseOf * Math.PI).toFloat() * fx.effects
            drawCircle(sky.mote.copy(alpha = .45f * fade), (1f + 1.6f * seed).dp.toPx(), Offset(x, y))
        }
    }
}

/**
 * Кино поверх боя: сцена входа стража [intro] в границах поля [bounds]; каждая новая фаза - виньетка её цвета по краям (держится до
 * конца фазы) и короткая светлая вспышка; победа - золотая вспышка. Вспышек нет без анимаций и при упрощённых эффектах.
 *
 * Вспышка фазы - по счёту пройденных порогов: окно журнала её не теряет. Обе вспышки принадлежат бою ([FightHud.leader]) и его
 * боссу: следующий бой, даже на ×2/×3 до конца гашения прошлой, начинает без них, а при живом боссе вспышки победы нет.
 */
@Composable internal fun BossCinema(fight: FightHud, boss: BossHud, intro: BossIntro?, bounds: IntroBounds) {
    val simple = LocalSettings.current.simpleEffects || !LocalMotion.current
    val foe = fight.foes.firstOrNull { it.index == boss.index } ?: return
    val phase = phaseNumber(boss)
    val vignette by animateFloatAsState(((phase - 1).coerceIn(0, 2)) * .18f, tween(1200), label = "vignette")
    val bout = fight.leader
    val flash = remember(bout, boss.index) { Animatable(0f) }
    var seen by remember(bout, boss.index) { mutableIntStateOf(phase) }
    LaunchedEffect(bout, boss.index, phase) {
        if (phase <= seen) return@LaunchedEffect
        seen = phase
        if (!simple) {
            flash.snapTo(.16f)
            flash.animateTo(0f, tween(420, easing = LinearEasing))
        }
    }
    val win = remember(bout, boss.index) { Animatable(0f) }
    LaunchedEffect(bout, boss.index, foe.alive) {
        when {
            foe.alive -> win.snapTo(0f)
            !simple -> {
                win.snapTo(.18f)
                win.animateTo(0f, tween(900))
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        if (vignette > 0f) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, phaseAura(phase).copy(alpha = vignette)), center, size.maxDimension * .72f))
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            flash.value.takeIf { it > 0f }?.let { drawRect(Color.White.copy(alpha = it)) }
            win.value.takeIf { it > 0f }?.let { drawRect(FrameGoldBright.copy(alpha = it)) }
        }
        intro?.Stage(monsterTitle(foe.monster.code), bounds)
    }
}
