package com.sperance.exileforge.ui.screens.session

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.DeletionPreview
import com.sperance.exileforge.core.model.hero.GuildDeparture
import com.sperance.exileforge.core.model.hero.GuildOutcome
import com.sperance.exileforge.core.model.hero.HeroSummary
import com.sperance.exileforge.ui.components.ConfirmSheet
import com.sperance.exileforge.ui.components.LedgerLine
import com.sperance.exileforge.ui.components.Tone
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.LifeRed

/** Что делают с героем в меню (4.5.1): в корзину на срок или стереть навсегда - оба подтверждаются его именем. */
enum class HeroDeletion { MARK, ERASE }

/**
 * Лист удаления героя [hero] (4.5.1): последствия из [preview] (лоты вернутся письмом, гильдия - выход, главенство наследнику
 * или роспуск, срок корзины) и подтверждение вводом имени. [preview] null - последствия ещё читаются, кнопка погашена. Стирание
 * ([HeroDeletion.ERASE]) последствий не перечисляет: герой уже в корзине, всё случилось при удалении.
 */
@Composable fun HeroDeletionSheet(hero: HeroSummary, mode: HeroDeletion, preview: DeletionPreview?, busy: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var typed by rememberSaveable(hero.id, mode) { mutableStateOf("") }
    val matches = typed.trim().equals(hero.name.trim(), ignoreCase = true)
    val reading = mode == HeroDeletion.MARK && preview == null
    ConfirmSheet(
        title = ui(if (mode == HeroDeletion.MARK) "chars.release_q" else "chars.erase_q"),
        subtitle = hero.name,
        danger = true,
        icon = { Icon(ForgeGlyphs.Exile, null, tint = LifeRed, modifier = Modifier.size(40.dp)) },
        ledger = if (mode == HeroDeletion.MARK) preview?.let(::consequences).orEmpty() else emptyList(),
        note = when {
            reading -> ui("common.loading")
            mode == HeroDeletion.MARK -> ui("chars.release_text", preview?.purgeDays ?: 0)
            else -> ui("chars.erase_text")
        },
        confirm = ui(if (mode == HeroDeletion.MARK) "chars.release_do" else "chars.erase_do"),
        blocked = busy || reading || !matches,
        options = {
            OutlinedTextField(
                typed,
                { typed = it.take(hero.name.length + NAME_SLACK) },
                label = { Text(ui("chars.release_type_name", hero.name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        onDismiss = onDismiss,
    ) { onConfirm(typed.trim()) }
}

/** Последствия удаления строками листа: срок корзины, лоты, гильдия. */
private fun consequences(preview: DeletionPreview): List<LedgerLine> = listOfNotNull(
    LedgerLine(ui("chars.release_term"), ui("chars.release_term_value", preview.purgeDays)),
    preview.lots.takeIf { it > 0 }?.let { LedgerLine(ui("chars.release_lots"), ui("chars.release_lots_value", it), Tone.SPEND) },
    preview.guild?.let { LedgerLine(ui("chars.release_guild"), departureText(it), if (it.outcome == GuildOutcome.DISBAND) Tone.SPEND else Tone.PLAIN) },
)

/** Что станет с гильдией: герой выйдет, главенство перейдёт наследнику или гильдия распустится. */
private fun departureText(departure: GuildDeparture): String {
    val guild = "[${departure.tag}] ${departure.name}"
    return when (departure.outcome) {
        GuildOutcome.LEAVE -> ui("chars.release_guild_leave", guild)
        GuildOutcome.TRANSFER -> ui("chars.release_guild_transfer", guild, departure.heirName.orEmpty())
        GuildOutcome.DISBAND -> ui("chars.release_guild_disband", guild)
    }
}

/** Запас длины поля имени: лишние пробелы по краям не обрезают ввод на полуслове. */
private const val NAME_SLACK = 4
