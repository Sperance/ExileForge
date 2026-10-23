package com.sperance.exileforge.ui.screens.contest

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.contest.ContestNews
import com.sperance.exileforge.core.contest.ContestPrizeMail
import com.sperance.exileforge.core.contest.ContestResultsModel
import com.sperance.exileforge.core.display.contestMetricLine
import com.sperance.exileforge.core.display.contestTitle
import com.sperance.exileforge.core.display.contestValue
import com.sperance.exileforge.core.display.groupedNumber
import com.sperance.exileforge.core.display.itemTitle
import com.sperance.exileforge.core.display.romanNumeral
import com.sperance.exileforge.core.display.stampText
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.rules.content.ContestHonour
import com.sperance.exileforge.ui.components.ButtonKind
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.SheetBody
import com.sperance.exileforge.ui.components.medalOf
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.components.rememberServerNow
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.Ink
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune
import org.koin.compose.koinInject

/**
 * Итоги состязаний при входе героя: вход героя тихо читает его ящик и свой титул ([ContestResultsModel.check]); непоказанные призы
 * ([ContestNews]) встают листом «Грамота» один раз - «Забрать» у грамоты открывает её письмо ([onOpenMail]), «Закрыть» (и закрытие
 * листа) запоминает их показанными на устройстве.
 */
@Composable fun ContestResultsHost(game: GameUi, onOpenMail: (String) -> Unit) {
    val model = koinInject<ContestResultsModel>()
    val inbox by model.inbox.collectAsStateWithLifecycle()
    val settings by model.settings.collectAsStateWithLifecycle()
    LaunchedEffect(game.heroId) { model.check() }
    val news = remember(game.heroId, inbox.mail, settings.shownPrizes) { model.news(game.heroId, inbox.mail, settings) }
    if (news.empty) return
    val podium = game.index?.rules?.contests?.podium ?: PODIUM
    val shown = remember(news) { news.prizes.map { it.mailId } }
    ContestResultsSheet(
        news,
        podium,
        onClaim = { mailId ->
            model.seen(shown)
            onOpenMail(mailId)
        },
        onClose = { model.seen(shown) },
    )
}

/**
 * Лист «Итоги состязания»: несколько призов - строкой, сколько их и что каждый в своём письме; грамоты одна под другой (последние
 * [ContestNews.SHOWN]), под ними «Ещё N - в почте»; один приз - «Позже» и «Забрать» в подвале, несколько - «Забрать» у каждой грамоты
 * и «Закрыть» в подвале.
 */
@Composable private fun ContestResultsSheet(news: ContestNews, podium: Int, onClaim: (String) -> Unit, onClose: () -> Unit) {
    val single = news.prizes.singleOrNull()
    ForgeSheet(
        onDismissRequest = onClose,
        body = SheetBody.Scroll(),
        footer = {
            if (single != null) {
                ForgeButton(onClose, Modifier.weight(1f), kind = ButtonKind.Secondary, size = ButtonSize.Wide) { Text(ui("contest.results.later")) }
                ForgeButton({ onClaim(single.mailId) }, Modifier.weight(1f), size = ButtonSize.Wide) { Text(ui("contest.results.claim")) }
            } else {
                ForgeButton(onClose, Modifier.fillMaxWidth(), kind = ButtonKind.Secondary, size = ButtonSize.Wide) { Text(ui("contest.results.close")) }
            }
        },
    ) {
        if (single == null) MutedText(ui("contest.results.many", news.prizes.size), Modifier.fillMaxWidth())
        news.visible.forEach { prize -> Certificate(prize, podium, onClaim = { onClaim(prize.mailId) }.takeIf { single == null }) }
        if (news.more > 0) MutedText(ui("contest.results.more", news.more), Modifier.fillMaxWidth())
    }
}

/**
 * Грамота приза [prize]: медаль с местом римской цифрой, «Итоги состязания дня: I место», мера и лига, место из числа героев и результат,
 * сферы награды строками и плашка титула с рамкой и сроком (местное время; срок прошёл - зачёркнут), у мест без титула - кто его
 * получает ([podium]). [onClaim] - своя «Забрать» (у одного приза её несёт подвал листа).
 */
@Composable private fun Certificate(prize: ContestPrizeMail, podium: Int, onClaim: (() -> Unit)?) {
    val medal = if (prize.place <= podium) medalOf(prize.place) else Bronze
    val place = romanNumeral(prize.place)
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxWidth().background(Panel, shape).border(1.dp, medal.copy(alpha = .6f), shape).padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(56.dp).background(Ink, CircleShape).border(2.dp, medal, CircleShape), contentAlignment = Alignment.Center) {
            Text(place, style = relicName(20).copy(color = medal))
        }
        Text(
            ui("contest.results.title", ui("contest.mail.kind.${prize.kind.name}"), place),
            style = relicName(17).copy(color = GoldBright),
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(ui("contest.results.line", contestMetricLine(prize.metric, prize.boss), leagueOf(prize)), color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        Text(ui("contest.results.standing", place, prize.size, contestValue(prize.metric, prize.value)), color = Parchment, style = MaterialTheme.typography.bodyMedium)
        Orbs(prize)
        val honour = prize.honour
        if (honour != null) HonourPlate(honour) else Text(ui("contest.results.no_title", podium), color = Muted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        onClaim?.let { claim -> ForgeButton(claim, Modifier.fillMaxWidth(), size = ButtonSize.Wide) { Text(ui("contest.results.claim")) } }
    }
}

/** Лига приза уровнями: «40–59», последняя - «60+». */
private fun leagueOf(prize: ContestPrizeMail): String = if (prize.leagueTo > 0) ui("contest.league_range", prize.leagueFrom, prize.leagueTo) else ui("contest.league_open", prize.leagueFrom)

/** Сферы награды: «Награда · сфер: 4» и строка на сферу «Сфера хаоса ×2». */
@Composable private fun Orbs(prize: ContestPrizeMail) {
    if (prize.orbs.isEmpty()) return
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(ui("contest.results.reward", groupedNumber(prize.orbCount)), color = Rune, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        prize.orbs.forEach { (code, count) ->
            Row(Modifier.fillMaxWidth()) {
                Text(itemTitle(code), color = Parchment, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text(ui("contest.results.count", groupedNumber(count)), color = GoldBright, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

/** Плашка титула: «Охотник дня» в рамке медали, ниже рамка и срок; срок прошёл - зачёркнуто и «срок прошёл …». */
@Composable private fun HonourPlate(honour: ContestHonour) {
    val now = rememberServerNow(MINUTE)
    val expired = honour.until <= now
    val medal = medalOf(honour.place)
    val shape = RoundedCornerShape(10.dp)
    Column(
        Modifier.fillMaxWidth().border(1.dp, medal, shape).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            ui("contest.results.honour", contestTitle(honour)),
            color = if (expired) Muted else medal,
            style = MaterialTheme.typography.titleSmall,
            textDecoration = if (expired) TextDecoration.LineThrough else null,
        )
        Text(
            if (expired) ui("contest.results.expired", stampText(honour.until)) else ui("contest.results.until", ui("contest.results.frame.${honour.place}"), stampText(honour.until)),
            color = Muted,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
        )
    }
}

/** Мест с титулом, пока правила не прочитаны. */
private const val PODIUM = 3

/** Такт подписи срока титула: минута. */
private const val MINUTE = 60_000L
