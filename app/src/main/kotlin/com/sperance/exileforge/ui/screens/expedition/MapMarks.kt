package com.sperance.exileforge.ui.screens.expedition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DoorFront
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sperance.exileforge.core.campaign.run.BlightView
import com.sperance.exileforge.core.campaign.run.ExpeditionRun
import com.sperance.exileforge.core.campaign.run.RunHud
import com.sperance.exileforge.core.campaign.run.arenaGate
import com.sperance.exileforge.core.campaign.run.seal
import com.sperance.exileforge.core.display.number
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.ui.components.LocalSettingsEdit
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.TipCallout
import com.sperance.exileforge.ui.components.touchZone
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.screens.expedition.scene.BlightTint
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.LossText
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Quiet
import com.sperance.exileforge.ui.theme.Rune
import com.sperance.exileforge.ui.theme.rarityColor
import kotlinx.coroutines.delay

/**
 * Метки состояния карты под её именем: страж (с «×N» после его гибели: живая, пока шаг ускорен, серая - ускорение выключено;
 * касание переключает ускорение), печать стража, ворота арены,
 * очаг Скверны и журнал, что сервер давно не берёт или отклонил. Метка - короткое слово или счёт «3/5» цветом своего
 * смысла; полный текст - в подсказке по касанию ([TipCallout]), а касание метки журнала ещё и отправляет его сейчас ([onRetry]).
 * Чего нет - метки нет.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MapMarks(run: ExpeditionRun, hud: RunHud, zone: Boolean, onRetry: () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GuardianMark(run, hud, zone)
        run.seal?.takeIf { !it.open }?.let { seal ->
            Mark(count(seal.killed, seal.need), SealTint, Icons.Outlined.Lock, { Tip(ui("expedition.seal_progress", seal.killed, seal.need), tint = SealTint) })
        }
        run.arenaGate?.takeIf { !it.open }?.let { gate ->
            Mark(count(gate.cleared, gate.need), Caution, Icons.Outlined.DoorFront, { Tip(ui("expedition.gate_progress", gate.cleared, gate.need), tint = Caution) })
        }
        BlightMark(hud.blight)
        JournalMarks(hud, onRetry)
    }
}

/**
 * Метка стража: жив, отдыхает или повержен; шаг после его гибели - «×N» внутри неё, слова - в подсказке. «×N» серая, пока ускорение
 * выключено настройкой; касание самой «×N» переключает настройку ([LocalSettingsEdit]), касание метки открывает подсказку о нынешнем шаге.
 */
@Composable private fun GuardianMark(run: ExpeditionRun, hud: RunHud, zone: Boolean) {
    val guardian = GuardianLine.of(hud, zone)
    val factor = run.index.campaign.expedition.bossSlainSpeed
    val edit = LocalSettingsEdit.current
    val swift = when {
        !hud.swift -> ""
        hud.hastened -> ui("run.swift", number(factor))
        else -> ui("run.swift_off")
    }
    val (tint, icon) = when (guardian.state) {
        GuardianState.ALIVE -> LossText to ForgeGlyphs.Skull
        GuardianState.RESTING -> guardian.color to Icons.Outlined.Schedule
        GuardianState.SLAIN -> guardian.color to Icons.Outlined.Check
    }
    Mark(
        guardian.mark,
        tint,
        icon,
        { Tip(guardian.text, swift, tint = guardian.color) },
        extra = ui("expedition.mark.swift", number(factor)).takeIf { hud.swift },
        extraTint = if (hud.hastened) Rune else Muted,
        onExtra = edit?.let { change -> { change { copy(swiftAfterBoss = !swiftAfterBoss) } } }.takeIf { hud.swift },
    )
}

/** Метка очага Скверны: «2/3» или Матерь; монстры вокруг или сундук зачищенного - в подсказке. Очаг не найден или иссяк - нет. */
@Composable private fun BlightMark(view: BlightView?) {
    val blight = view?.takeIf { it.seen && !it.over } ?: return
    val mother = blight.point == blight.points
    val title = if (mother) ui("map.legend_mother") else ui("feature.blight.point", blight.point + 1, blight.limit)
    Mark(if (mother) title else count(blight.point + 1, blight.limit), BlightTint.hot, Icons.Outlined.WaterDrop, {
        val chest = blight.chest
        when {
            chest != null -> Tip(title, tint = BlightTint.hot, facts = listOf(ui("feature.blight.chest") to ui("feature.blight.quality.${chest.name}")), valueTint = rarityColor(chest.name))
            blight.raised -> Tip(title, ui("feature.blight.left", blight.left), tint = BlightTint.hot)
            else -> Tip(title, tint = BlightTint.hot)
        }
    })
}

/**
 * Журнал захода, тихо: число событий, что сервер не берёт дольше [PENDING_GRACE] (касание - отправить сейчас), и число отклонённых.
 * Пока всё учтено - ничего: пачка в полёте - не новость.
 */
@Composable private fun JournalMarks(hud: RunHud, onRetry: () -> Unit) {
    var overdue by remember { mutableStateOf(false) }
    LaunchedEffect(hud.pending > 0, hud.applied) {
        overdue = false
        if (hud.pending > 0) {
            delay(PENDING_GRACE)
            overdue = true
        }
    }
    if (hud.pending > 0 && overdue) {
        Mark("${hud.pending}", Quiet, Icons.Outlined.Sync, { Tip(ui("expedition.pending", hud.pending), tint = Quiet) }, onOpen = onRetry)
    }
    if (hud.rejected > 0) {
        Mark("${hud.rejected}", LossText, Icons.Outlined.ErrorOutline, { Tip(ui("expedition.rejected", hud.rejected), tint = LossText) })
    }
}

/**
 * Метка над картой: значок и короткое [label] цветом [tint] в окантовке того же цвета, [extra] - вставка справа («×2») тоном [extraTint].
 * Касание раскрывает подсказку [tip] (читается в миг открытия) и зовёт [onOpen]; для чтения с экрана - полный текст подсказки.
 * [onExtra] - своё касание вставки: подсказку не открывает.
 */
@Composable private fun Mark(
    label: String,
    tint: Color,
    icon: ImageVector,
    tip: () -> Tip,
    extra: String? = null,
    extraTint: Color = Rune,
    onOpen: (() -> Unit)? = null,
    onExtra: (() -> Unit)? = null,
) {
    var open by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(7.dp)
    Row(
        Modifier
            .clickable {
                open = true
                onOpen?.invoke()
            }
            .semantics { contentDescription = tip().let { listOfNotNull(it.title, it.body).filter(String::isNotBlank).joinToString(". ") } }
            .touchZone()
            .height(MARK_HEIGHT).background(MarkWell, shape).background(tint.copy(alpha = .13f), shape).border(1.dp, tint.copy(alpha = .62f), shape)
            .padding(start = 6.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Text(label, color = tint, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"), maxLines = 1)
        extra?.let {
            Text(
                it,
                color = extraTint,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .then(if (onExtra != null) Modifier.clickable(onClick = onExtra).touchZone() else Modifier)
                    .background(extraTint.copy(alpha = .14f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
        if (open) TipCallout(tip()) { open = false }
    }
}

/** Счёт метки «3/5». */
private fun count(done: Int, need: Int): String = ui("expedition.mark.count", done, need)

/** Как долго старейшее неотправленное событие журнала ждёт, прежде чем заход скажет о нём. */
internal const val PENDING_GRACE = 10_000L

/** Высота метки. */
private val MARK_HEIGHT = 26.dp

/** Тёмная подложка метки под её тоном. */
private val MarkWell = Color(0xBF0A0D12)

/** Печать стража: фиолетовый счёт павших редких. */
private val SealTint = Color(0xFFC9A0FF)
