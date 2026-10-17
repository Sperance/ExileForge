package com.sperance.exileforge.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.session.Reach
import com.sperance.exileforge.core.session.StartGate
import com.sperance.exileforge.core.session.StartInput
import com.sperance.exileforge.core.session.StartVerdict
import com.sperance.exileforge.presentation.app.ReachState
import com.sperance.exileforge.presentation.app.StartWindow
import com.sperance.exileforge.presentation.app.WarmStep
import com.sperance.exileforge.presentation.app.Warmup
import com.sperance.exileforge.presentation.features.UpdateState
import com.sperance.exileforge.presentation.features.UpdateViewModel
import com.sperance.exileforge.ui.theme.*
import com.sperance.exileforge.update.UpdateInstaller
import kotlinx.coroutines.delay

/** The update model of the activity, for the screens that show the version and check by hand. */
val LocalUpdates = staticCompositionLocalOf<UpdateViewModel?> { null }

/** Что из сохранённого на устройстве уже прочитано: мир, словарь, иконки. */
data class StartStages(val contentReady: Boolean, val dictionaryReady: Boolean, val iconsReady: Boolean) {
    val deviceReady: Boolean get() = contentReady && dictionaryReady && iconsReady
}

/**
 * Окно запуска, обновления и прогрева - одно: заставка во весь экран,
 * внизу - что идёт сейчас и общая полоса, шаги - по «Подробнее». Что показать, решает [StartGate] по модели (связь, провод сервера,
 * проверка сборки, мир устройства, прогрев героя [warmup]); обязательное обновление и несовпавший провод сервера держат окно, пока
 * не разрешатся, - игра пишет, чего ждёт. Окно появляется не раньше [SHOW_DELAY_MS] (быстрый запуск его не мигает), а показавшись,
 * держится не меньше [MIN_SHOWN_MS] и уходит одним переходом: «отпущен» - флаг модели на весь процесс ([ServerReach.release]),
 * а не экрана. Ни назад, ни касание мимо его не закрывают. [busy] - идёт поход, испытание или Разлом: найденное обновление ждёт
 * его конца, а пока о нём говорит плашка сверху ([UpdateHint]), не мешая бою.
 *
 * Окно не привязано к жизненному циклу: обязано видеть каждое изменение. Возврат в приложение - сборка спрашивается сразу (сверку ресурсов
 * модель откладывает до конца похода); возврат из настроек или установщика - разрешение спрашивается снова. Появление с задержкой и не
 * короче [MIN_SHOWN_MS]. Сборка, найденная в походе или испытании, закроет игру по их концу, а пока - плашка сверху.
 */
@Composable fun UpdateGate(
    updates: UpdateViewModel,
    busy: Boolean,
    stages: StartStages,
    window: StartWindow,
    warmup: Warmup?,
    actions: StartActions,
    diagnostics: () -> String,
) {
    val s by updates.state.collectAsState()
    val reach = window.reach
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LifecycleEventEffect(Lifecycle.Event.ON_START) { updates.resumed() }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { updates.foreground() }
    val warming = warmup?.takeIf { !it.finished }
    val verdict = StartGate.verdict(
        StartInput(reach.reach, window.fit, s.mandatory, s.checking, stages.deviceReady, warming != null, window.elapsedMs, window.released, busy),
    )
    LaunchedEffect(verdict) { if (verdict == StartVerdict.RELEASE) actions.release() }
    var visible by remember { mutableStateOf(false) }
    var shownAt by remember { mutableLongStateOf(0L) }
    var face by remember { mutableStateOf(verdict.takeIf { it.shown } ?: StartVerdict.HOLD) }
    if (verdict.shown) face = verdict
    LaunchedEffect(verdict.shown) {
        if (verdict.shown) {
            if (!visible) {
                delay(SHOW_DELAY_MS)
                shownAt = System.currentTimeMillis()
                visible = true
            }
        } else if (visible) {
            delay((MIN_SHOWN_MS - (System.currentTimeMillis() - shownAt)).coerceAtLeast(0))
            visible = false
        }
    }
    var waited by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(CONTINUE_AFTER_MS)
        waited = true
    }
    when {
        visible -> Splash {
            when (face) {
                StartVerdict.UPDATE -> UpdateBody(s, updates)

                StartVerdict.AWAIT_GAME -> Awaiting(ui("launch.await_game.title"), ui("launch.await_game.text"), null, updates::checkNow)

                StartVerdict.AWAIT_SERVER -> Awaiting(ui("launch.await_server.title"), ui("launch.await_server.text"), reach.retryAt, actions.checkFit)

                StartVerdict.WARMING -> warming?.let { Warming(it) }

                else -> {
                    Starting(s, stages, reach, unreachable = face == StartVerdict.UNREACHABLE)
                    if (face == StartVerdict.UNREACHABLE) ForgeButton(onClick = actions.retry, modifier = Modifier.fillMaxWidth()) { Text(ui("start.retry")) }
                    if (face == StartVerdict.UNREACHABLE || waited) {
                        ForgeOutlinedButton(onClick = actions.release, modifier = Modifier.fillMaxWidth()) { Text(ui("start.offline")) }
                        CopyLog {
                            "--- window\nlifecycle=${lifecycle.currentState} verdict=$verdict face=$face visible=$visible busy=$busy\nseen $window\nseen update checking=${s.checking} " +
                                "update=${s.update?.info?.versionName} failure=${s.failure}\nlive update checking=${updates.state.value.checking}\n$stages\nwarmup=$warmup\n" + diagnostics()
                        }
                    }
                }
            }
        }

        s.askSources && window.released -> Locked { SourcesBody() }

        busy -> s.update?.let { UpdateHint(it.info.versionName) }
    }
}

/** Что окно запуска умеет сделать по кнопкам: повторить связь, отпустить старт («Играть без связи»), перечитать провод сервера. */
class StartActions(val retry: () -> Unit, val release: () -> Unit, val checkFit: () -> Unit)

/** Запуск: что идёт сейчас строкой, общая полоса по шагам (связь, версия, мир устройства) и шаги - по «Подробнее». */
@Composable private fun ColumnScope.Starting(s: UpdateState, stages: StartStages, reach: ReachState, unreachable: Boolean) {
    val steps = listOf(reach.reach == Reach.ANSWERED, !s.checking, stages.deviceReady)
    val now = when {
        unreachable -> ui("start.unreachable")
        reach.reach != Reach.ANSWERED -> ui("launch.step.server")
        s.progress != null -> ui("start.state.downloading", (s.progress * 100).toInt())
        s.checking -> ui("launch.step.version")
        !stages.deviceReady -> ui("launch.step.device")
        else -> ui("launch.step.ready")
    }
    Status(now, steps.count { it }.toFloat() / steps.size, if (unreachable) Ember else Gold) { StartRows(s, stages, reach) }
}

/** Прогрев героя: шаг, что идёт, полоса и все шаги - по «Подробнее». */
@Composable private fun ColumnScope.Warming(warmup: Warmup) {
    val current = WarmStep.entries.firstOrNull { it !in warmup.done }
    Status(ui("launch.step.world", current?.let { ui("warmup.step.${it.name}") } ?: ui("launch.step.ready")), warmup.progress, Gold) {
        WarmStep.entries.forEach { step -> StartRow(ui("warmup.step.${step.name}"), if (step in warmup.done) RowState.DONE else RowState.RUN, "") }
    }
}

/** Провода не совпали: что ждём и почему, когда следующая проверка ([nextAt], null - без отсчёта) и «Проверить сейчас». */
@Composable private fun ColumnScope.Awaiting(title: String, text: String, nextAt: Long?, onCheck: () -> Unit) {
    Text(title, color = Caution, style = MaterialTheme.typography.titleMedium)
    MutedText(text)
    LinearProgressIndicator(Modifier.fillMaxWidth(), color = Caution, trackColor = PanelRaised)
    nextAt?.let { at ->
        val now by ticking()
        MutedText(ui("launch.next_check", ((at - now + 999) / 1000).coerceAtLeast(0)), style = MaterialTheme.typography.labelSmall)
    }
    ForgeOutlinedButton(onClick = onCheck, modifier = Modifier.fillMaxWidth()) { Text(ui("launch.check_now")) }
}

/** Строка «что идёт», полоса доли [progress] и шаги [details] - по «Подробнее». */
@Composable private fun ColumnScope.Status(now: String, progress: Float, tint: Color, details: @Composable ColumnScope.() -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    Text(now, color = Parchment, style = MaterialTheme.typography.bodyMedium)
    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = tint, trackColor = PanelRaised)
    Text(
        ui(if (open) "launch.less" else "launch.more", (progress * 100).toInt()),
        color = Rune,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.align(Alignment.End).clickable(role = Role.Button) { open = !open }.padding(4.dp),
    )
    if (open) Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = details)
}

/** Часы окна: пересчёт отсчётов раз в [TICK_MS]. */
@Composable private fun ticking(): State<Long> = produceState(System.currentTimeMillis()) {
    while (true) {
        delay(TICK_MS)
        value = System.currentTimeMillis()
    }
}

/** Заставка во весь экран: знак игры по центру, версия под ним, внизу - [content] окна; ни назад, ни касание мимо не закрывают. */
@Composable private fun Splash(content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        ChordOverWindow()
        Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(PanelRaised, Ink), radius = SPLASH_GLOW)).systemBarsPadding()) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(BRAND, color = GoldBright, style = MaterialTheme.typography.headlineMedium, fontFamily = FontFamily.Serif, letterSpacing = 3.sp)
                VersionLabel()
            }
            Column(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().widthIn(max = 480.dp).heightIn(max = SPLASH_PANEL).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        }
    }
}

/** Знак игры на заставке - бренд, не строка словаря. */
private const val BRAND = "EXILE FORGE"

/** Окно запуска появляется не раньше этого: быстрый запуск обходится без него. */
private const val SHOW_DELAY_MS = 600L

/** Показавшееся окно держится не меньше этого: без мигания. */
private const val MIN_SHOWN_MS = 800L

/** Размах свечения заставки. */
private const val SPLASH_GLOW = 1200f

/** Наибольшая высота нижней части заставки: длинные заметки обновления прокручиваются. */
private val SPLASH_PANEL = 560.dp

/** Плашка сверху над боем: найдено обновление, оно встанет по концу похода или испытания; касания идут мимо неё. */
@Composable private fun UpdateHint(version: String) {
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 56.dp), contentAlignment = Alignment.TopCenter) {
        Text(
            ui("update.after_fight", version),
            color = GoldBright,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.background(PanelRaised.copy(alpha = .92f), MaterialTheme.shapes.small).padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/**
 * Строки окна запуска: что проверяется или качается сейчас, по пункту на строку; связь - только когда её нет.
 *
 * Связь - не пункт для игрока: строка появляется только с ошибкой и отсчётом до повтора.
 */
@Composable private fun StartRows(s: UpdateState, stages: StartStages, reach: ReachState) {
    val now by ticking()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (reach.reach == Reach.UNREACHABLE) {
            StartRow(ui("start.row.server"), RowState.FAIL, ui("start.state.retry_in", ((reach.retryAt - now + 999) / 1000).coerceAtLeast(0)))
        }
        when {
            s.progress != null -> StartRow(ui("start.row.version"), RowState.RUN, ui("start.state.downloading", (s.progress * 100).toInt()))
            s.checking -> StartRow(ui("start.row.version"), RowState.RUN, ui("start.state.checking"))
            s.update != null -> StartRow(ui("start.row.version"), RowState.DONE, ui("start.state.found", s.update.info.versionName))
            s.failure != null -> StartRow(ui("start.row.version"), RowState.FAIL, ui("start.state.unchecked"))
            else -> StartRow(ui("start.row.version"), RowState.DONE, ui("start.state.latest", BuildConfig.VERSION_NAME))
        }
        StartRow(ui("start.row.device"), if (stages.deviceReady) RowState.DONE else RowState.RUN, ui(if (stages.deviceReady) "start.state.ready" else "start.state.reading"))
    }
}

private enum class RowState { RUN, DONE, FAIL }

/** Пункт: значок состояния, что это и коротко - где оно. */
@Composable private fun StartRow(title: String, state: RowState, status: String) {
    val color: Color = when (state) {
        RowState.RUN -> Gold
        RowState.DONE -> Vital
        RowState.FAIL -> Ember
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            when (state) {
                RowState.RUN -> CircularProgressIndicator(Modifier.size(16.dp), color = color, strokeWidth = 2.dp)
                RowState.DONE -> Text("✓", color = color, style = MaterialTheme.typography.titleSmall)
                RowState.FAIL -> Text("✕", color = color, style = MaterialTheme.typography.titleSmall)
            }
        }
        Text(title, color = Parchment, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(status, color = color, style = MaterialTheme.typography.labelLarge)
    }
}

/** Журнал запуска в буфер обмена: метка «скопировано» держится пару секунд. */
@Composable private fun CopyLog(diagnostics: () -> String) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_MS)
            copied = false
        }
    }
    TextButton(onClick = {
        clipboard.setText(AnnotatedString(diagnostics()))
        copied = true
    }, modifier = Modifier.fillMaxWidth()) { Text(ui(if (copied) "start.copied" else "start.copy_log"), color = Gold) }
}

private const val COPIED_MS = 2_000L

/** Как часто окно пересчитывает отсчёт до повтора. */
private const val TICK_MS = 500L

/** Сколько окно ждёт, прежде чем предложить войти, не дожидаясь. */
private const val CONTINUE_AFTER_MS = 15_000L

/** «Установка неизвестных приложений» не разрешена: только путь в настройки; вернувшись, игра спрашивает снова. */
@Composable private fun ColumnScope.SourcesBody() {
    val context = LocalContext.current
    Text(ui("update.sources_title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
    MutedText(ui("update.sources_note"))
    ForgeButton(onClick = { context.startActivity(UpdateInstaller.permissionScreen(context)) }, modifier = Modifier.fillMaxWidth()) {
        Text(ui("update.open_settings"))
    }
}

/** A window nothing outside closes: no back, no tap beside it. */
@Composable private fun Locked(content: @Composable ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)) {
        ChordOverWindow()
        Column(
            Modifier.fillMaxWidth().padding(20.dp).background(PanelRaised, DialogShape).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

/**
 * The build to take: from what to what, what is new, and the download into the installer.
 *
 * Обход установщика: страница релиза на GitHub, APK руками.
 */
@Composable private fun ColumnScope.UpdateBody(s: UpdateState, updates: UpdateViewModel) {
    val update = s.update ?: return
    val context = LocalContext.current
    Text(ui("update.title"), color = GoldBright, style = MaterialTheme.typography.titleMedium)
    if (update.info.size > 0) MutedText(ui("update.size", "%.1f".format(update.info.size / 1_048_576.0)))
    Text(ui("update.notes"), color = Parchment, style = MaterialTheme.typography.labelLarge)
    Column(Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(update.info.versionName, color = GoldBright, style = MaterialTheme.typography.labelLarge)
        if (update.info.notes.isNotBlank()) MutedText(update.info.notes)
    }
    val progress = s.progress
    when {
        progress != null -> {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised)
            MutedText(ui("update.downloading", (progress * 100).toInt()))
        }

        s.installing -> {
            LinearProgressIndicator(Modifier.fillMaxWidth(), color = Gold, trackColor = PanelRaised)
            MutedText(ui("update.installing"))
        }

        s.needsPermission -> {
            InfoCard(ui("update.permission_title"), ui("update.permission_note"))
            ForgeButton(onClick = {
                context.startActivity(UpdateInstaller.permissionScreen(context))
                updates.permissionAsked()
            }, modifier = Modifier.fillMaxWidth()) {
                Text(ui("update.open_settings"))
            }
        }

        else -> {
            s.error?.let { InfoCard(ui("update.failed_title"), it, failure = true) }
            ForgeButton(onClick = updates::install, modifier = Modifier.fillMaxWidth()) { Text(ui(if (s.error != null) "update.retry" else "update.install")) }
            if (s.error != null) {
                ForgeOutlinedButton(onClick = {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.pageUrl)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }, modifier = Modifier.fillMaxWidth()) { Text(ui("update.browser")) }
            }
        }
    }
}

/** This build's version, dim: on the way in and in the account. */
@Composable fun VersionLabel(modifier: Modifier = Modifier) {
    Text(ui("app.version", BuildConfig.VERSION_NAME), modifier = modifier, color = Muted.copy(alpha = .55f), style = MaterialTheme.typography.labelSmall)
}

/** Версия в аккаунте: без проверки вручную - она идёт сама; «Обновить» - только когда есть новая. */
@Composable fun UpdateCard() {
    val updates = LocalUpdates.current ?: return
    val s by updates.state.collectAsStateWithLifecycle()
    InfoCard(ui("update.card_title"), ui("update.version", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE) + updateNote(s)?.let { "\n$it" }.orEmpty())
    if (s.update != null) {
        ForgeOutlinedButton(enabled = !s.installing, onClick = updates::install, modifier = Modifier.fillMaxWidth()) { Text(ui("update.install")) }
    }
}

/** Что сказать о версии: есть новая, идёт проверка, отказ проверки или «последняя». */
fun updateNote(s: UpdateState): String? = when {
    s.update != null -> ui("update.found", s.update.info.versionName)
    s.checking -> ui("update.checking")
    s.failure != null -> s.failure
    else -> ui("update.up_to_date")
}
