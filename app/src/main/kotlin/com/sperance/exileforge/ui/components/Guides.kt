package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.data.settings.GuideStore
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** A screen that explains itself on the first visit (3.14.0): its title and lines are `guide.<name>.title/text` in the dictionary. */
enum class Guide(val icon: ImageVector) {
    HERO(ForgeGlyphs.Helm),
    EXPEDITION(ForgeGlyphs.Swords),
    CRAFTS(ForgeGlyphs.Anvil),
    PROGRESS(ForgeGlyphs.Sigil),
    AUCTION(ForgeGlyphs.Orb),
    TREE(ForgeGlyphs.Constellation),
    GRIMOIRE(ForgeGlyphs.Grimoire),
    PETS(ForgeGlyphs.Exile),
    FORGE(ForgeGlyphs.Anvil),
    MERCHANT(ForgeGlyphs.Coins),
    FIGHT(ForgeGlyphs.Swords),
    ATLAS(ForgeGlyphs.Atlas),
    ABYSS(ForgeGlyphs.Rift),
    MAP_LAUNCH(ForgeGlyphs.Scroll),
    CITY(ForgeGlyphs.Keep),
    GUILD(ForgeGlyphs.Banner),
    QUESTS(ForgeGlyphs.Scroll),
    ;

    private val key get() = "guide.${name.lowercase()}"
    val title: String get() = ui("$key.title")
    val text: String get() = ui("$key.text")

    /** The one line a first visit shows (3.79.0); the whole text waits behind «?» and «Подробнее». */
    val line: String get() = ui("$key.line")
}

/**
 * The guides of one app session: screens ask to be explained on their first visit, the desk shows one at a time, in the
 * order they asked, and only those the device has not read — as one line (3.79.0), [hint], not a sheet over the screen.
 * «Подробнее» on the line and the «?» of a screen open the whole guide, [open], at any time.
 */
@Stable
class GuideDesk(private val store: GuideStore, private val scope: CoroutineScope) {
    private var read: Set<String>? = null
    private val waiting = ArrayDeque<Guide>()

    var open: Guide? by mutableStateOf(null)
        private set
    var hint: Guide? by mutableStateOf(null)
        private set

    init {
        scope.launch {
            store.read.collect {
                read = it
                next()
            }
        }
    }

    fun visit(guide: Guide) {
        if (guide == open || guide in waiting) return
        waiting += guide
        next()
    }

    fun show(guide: Guide) {
        open = guide
    }

    /** The line goes, read; [more] opens its whole guide. */
    fun dismissHint(more: Boolean = false) {
        val guide = hint ?: return
        markRead(guide)
        hint = null
        if (more) open = guide else next()
    }

    fun close() {
        open?.let(::markRead)
        open = null
        next()
    }

    private fun markRead(guide: Guide) {
        read = read.orEmpty() + guide.name
        scope.launch { store.markRead(guide.name) }
    }

    fun reset() {
        scope.launch { store.reset() }
    }

    private fun next() {
        val known = read ?: return
        if (open != null || hint != null) return
        while (waiting.isNotEmpty()) {
            val guide = waiting.removeFirst()
            if (guide.name !in known) {
                hint = guide
                return
            }
        }
    }
}

/** The app's desk; none in previews and UI tests, so a screen drawn alone never opens a sheet over itself. */
val LocalGuideDesk = staticCompositionLocalOf<GuideDesk?> { null }

/** Asks for [guide] once, when the screen first appears. */
@Composable fun FirstVisit(guide: Guide) {
    val desk = LocalGuideDesk.current ?: return
    LaunchedEffect(guide) { desk.visit(guide) }
}

/** The «?» that opens [guide] again. */
@Composable fun GuideButton(guide: Guide, modifier: Modifier = Modifier) {
    val desk = LocalGuideDesk.current ?: return
    IconButton(onClick = { desk.show(guide) }, modifier = modifier.size(36.dp)) {
        Icon(Icons.AutoMirrored.Outlined.HelpOutline, ui("guide.open"), tint = Muted)
    }
}

/** Whatever guide the desk holds, as a sheet from below: the screen stays in sight above it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideHost(desk: GuideDesk) {
    desk.hint?.let { GuideLine(it, desk) }
    val guide = desk.open ?: return
    ForgeSheet(onDismissRequest = desk::close) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(guide.icon, null, tint = Gold, modifier = Modifier.size(32.dp))
                Text(guide.title, color = GoldBright, style = MaterialTheme.typography.titleLarge)
            }
            guide.text.lines().filter(String::isNotBlank).forEach { Text(it, color = Parchment, style = MaterialTheme.typography.bodyMedium) }
            ForgeButton(onClick = desk::close, modifier = Modifier.fillMaxWidth()) { Text(ui("guide.got_it")) }
        }
    }
}

/** A first visit's one line (3.79.0), floating above the bottom bar: «Подробнее» opens the guide, a tap elsewhere on it closes. */
@Composable private fun GuideLine(guide: Guide, desk: GuideDesk) {
    LaunchedEffect(guide) {
        kotlinx.coroutines.delay(HINT_MS)
        desk.dismissHint()
    }
    Box(Modifier.fillMaxSize().navigationBarsPadding().padding(start = 12.dp, end = 12.dp, bottom = 92.dp), contentAlignment = Alignment.BottomCenter) {
        val shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
        Row(
            Modifier.fillMaxWidth().background(PanelRaised, shape).border(1.dp, Gold.copy(alpha = .35f), shape)
                .clickable(onClickLabel = ui("common.close")) { desk.dismissHint() }.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(guide.icon, null, tint = Gold, modifier = Modifier.size(20.dp))
            Text(guide.line, color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            TextButton(onClick = { desk.dismissHint(more = true) }) { Text(ui("guide.more"), color = GoldBright) }
        }
    }
}

/** How long a first visit's line stays before it leaves by itself. */
private const val HINT_MS = 7_000L
