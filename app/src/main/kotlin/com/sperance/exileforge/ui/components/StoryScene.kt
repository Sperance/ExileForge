package com.sperance.exileforge.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.i18n.loc
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.presentation.settings.SettingsViewModel
import com.sperance.exileforge.rules.text.LocaleKey
import org.koin.compose.viewmodel.koinViewModel
import kotlinx.coroutines.delay

/**
 * Сюжетный экран (4.6.6, макет «Пролог и продолжение», вариант A «Пепел и свиток»): палитра Предначертания, жар снизу и угли,
 * заголовок Cinzel, абзацы [paragraphs] печатаются по буквам один за другим. Касание дописывает абзац, касание по дописанному -
 * следующий, после последнего - [onDone]. «Пропустить» видно, если [skippable], и не раньше [skipAfterMs]. Без анимаций
 * (`LocalMotion`) текст появляется сразу. Один экран на пролог и продолжения шагов сюжета.
 */
@Composable fun StoryScene(
    kicker: String,
    title: String,
    paragraphs: List<String>,
    skippable: Boolean,
    onDone: () -> Unit,
    skipAfterMs: Long = 0,
) {
    val moving = LocalMotion.current
    var shown by remember(paragraphs) { mutableIntStateOf(1) }
    var typed by remember(paragraphs) { mutableIntStateOf(if (moving) 0 else Int.MAX_VALUE) }
    var skipReady by remember { mutableStateOf(skipAfterMs <= 0) }
    val current = paragraphs.getOrNull(shown - 1).orEmpty()
    LaunchedEffect(skipAfterMs) {
        if (skipAfterMs > 0) {
            delay(skipAfterMs)
            skipReady = true
        }
    }
    LaunchedEffect(shown, moving) {
        if (!moving) return@LaunchedEffect
        typed = 0
        while (typed < current.length) {
            delay(TYPE_MS)
            typed++
        }
    }
    fun advance() {
        when {
            typed < current.length -> typed = Int.MAX_VALUE
            shown < paragraphs.size -> {
                shown++
                if (!moving) typed = Int.MAX_VALUE
            }
            else -> onDone()
        }
    }
    BackHandler(enabled = skippable && skipReady, onBack = onDone)
    Box(
        Modifier.fillMaxSize().background(FateInk.Ground.last()).ashGround()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = ::advance),
    ) {
        EmberField(Modifier.fillMaxSize())
        AltarGlow(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(160.dp))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 28.dp, vertical = 72.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(kicker.uppercase(), color = FateInk.Rim, style = relicName(13), letterSpacing = 3.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text(title, style = relicName(26).copy(color = FateInk.Gold), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp))
            paragraphs.take(shown).forEachIndexed { i, text ->
                val visible = if (i == shown - 1) text.take(typed.coerceAtMost(text.length)) else text
                Text(visible, color = FateInk.Vellum, fontFamily = FontFamily.Serif, fontStyle = FontStyle.Italic, fontSize = 18.sp, lineHeight = 27.sp)
            }
        }
        if (skippable && skipReady) {
            val shape = RoundedCornerShape(50)
            Text(
                ui("story.skip"),
                color = FateInk.Text,
                style = relicName(12),
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(16.dp)
                    .border(1.dp, FateInk.Rim, shape).clickable(onClick = onDone).padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
        Text(
            ui(if (typed >= current.length && shown >= paragraphs.size) "story.tap_done" else "story.tap_next"),
            color = FateInk.AshInk,
            style = relicName(11),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp).fillMaxWidth(),
        )
    }
}

/** Абзацы текста словаря: пустые строки между ними. */
fun storyParagraphs(text: String): List<String> = text.split("\n\n").map(String::trim).filter(String::isNotBlank)

/** Сколько мс на букву печати. */
private const val TYPE_MS = 22L

/**
 * Пролог перед [content] (4.6.6): новый аккаунт видит его до Предначертания ([always] = false - только пока пролог не показан на
 * устройстве), создание героя - каждый раз ([always] = true). Пропустить можно сразу, если пролог уже видели, иначе - через
 * [FIRST_SKIP_MS]. Показанный пролог отмечается на устройстве.
 */
@Composable fun PrologueGate(always: Boolean, content: @Composable () -> Unit) {
    val settings = koinViewModel<SettingsViewModel>()
    val seen = settings.settings.collectAsStateWithLifecycle().value.prologueSeen
    var passed by rememberSaveable { mutableStateOf(false) }
    val text = loc(LocaleKey.PROLOGUE)
    if (passed || (!always && seen) || text.isBlank()) return content()
    StoryScene(
        kicker = ui("story.prologue"),
        title = loc(LocaleKey.PROLOGUE_TITLE),
        paragraphs = storyParagraphs(text),
        skippable = true,
        skipAfterMs = if (seen) 0 else FIRST_SKIP_MS,
        onDone = {
            passed = true
            if (!seen) settings.change { copy(prologueSeen = true) }
        },
    )
}

/** Первый показ пролога на устройстве: «Пропустить» появляется через столько мс. */
private const val FIRST_SKIP_MS = 3_000L
