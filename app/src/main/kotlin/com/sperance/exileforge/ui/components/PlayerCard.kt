package com.sperance.exileforge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sperance.exileforge.core.display.classTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.hero.CardModeration
import com.sperance.exileforge.core.model.hero.PlayerCard
import com.sperance.exileforge.presentation.admin.ModerationViewModel
import com.sperance.exileforge.presentation.player.PlayerCardViewModel
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.ui.screens.hero.titleName
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Caution
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.PanelRaised
import com.sperance.exileforge.ui.theme.Parchment
import org.koin.compose.viewmodel.koinViewModel

/**
 * Действие места под карточкой игрока (4.5.1): [label] на кнопке, [run] выполняется, и карточка закрывается. Так состав
 * гильдии оставляет за картой игрока прежний лист званий и прав.
 */
data class PlayerAction(val label: String, val run: () -> Unit)

/** Как открыть карточку игрока по id его героя (4.5.1) с действием места; null - хозяина нет, нажатие ничего не открывает. */
val LocalPlayerCard = staticCompositionLocalOf<((String, PlayerAction?) -> Unit)?> { null }

private val NO_PLAYER: (String, PlayerAction?) -> Unit = { _, _ -> }

/** Открыватель карточки игрока из [LocalPlayerCard]; без хозяина - пустой. */
@Composable fun rememberPlayerCard(): (String, PlayerAction?) -> Unit = LocalPlayerCard.current ?: NO_PLAYER

/**
 * Нажатие открывает карточку игрока [heroId] (4.5.1) - имя или строка игрока где угодно: состав гильдии, Зал славы, продавец
 * лота, автор отчёта. Пустой id ([heroId] null или пусто) нажатия не даёт.
 */
fun Modifier.opensPlayer(heroId: String?): Modifier = if (heroId.isNullOrBlank()) {
    this
} else {
    composed {
        val open = rememberPlayerCard()
        clickable { open(heroId, null) }
    }
}

/**
 * Хозяин карточки игрока (4.5.1, утверждён макет «Свиток»): всё внутри открывает её через [LocalPlayerCard]; лист один, поверх.
 * Карточку читает [PlayerCardViewModel]; модерация уводит в досье окна модерации - там прежние бан и удаление.
 */
@Composable fun PlayerCardHost(game: GameUi, content: @Composable () -> Unit) {
    val vm = koinViewModel<PlayerCardViewModel>()
    val state by vm.state.collectAsStateWithLifecycle()
    var extra by remember { mutableStateOf<PlayerAction?>(null) }
    val opener: (String, PlayerAction?) -> Unit = remember(vm) {
        { heroId, action ->
            extra = action
            vm.open(heroId)
        }
    }
    CompositionLocalProvider(LocalPlayerCard provides opener) { content() }
    if (state.target == null) return
    ForgeSheet(onDismissRequest = vm::close) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val card = state.card
            if (card == null) {
                MutedText(ui("common.loading"))
                return@Column
            }
            Scroll(game, vm, card)
            // Предначертание аккаунта (4.6.3, «Лента и свиток»): лента во всю ширину под строками; не выбрано - ленты нет
            card.fate?.let { fate -> FateRibbon(fate, ui("fate.ribbon.caption"), Modifier.fillMaxWidth()) }
            CardActions(
                game,
                card,
                onWrite = { vm.write(card) },
                onInvite = { vm.invite(card) },
                onLots = { vm.sellerLots(card) },
            )
            extra?.let { action ->
                ForgeOutlinedButton(onClick = {
                    vm.close()
                    action.run()
                }, enabled = !game.busy, modifier = Modifier.fillMaxWidth()) { Text(action.label) }
            }
            card.rights.moderation?.takeIf { it.any }?.let { rights ->
                val moderation = koinViewModel<ModerationViewModel>()
                ModerationRow(game, rights) {
                    vm.close()
                    moderation.open(card.heroId, rights.userId)
                    vm.moderate()
                }
            }
        }
    }
}

/** «Свиток»: слева корешок - уровень, знак класса и класс; справа титул над именем и справочные строки. */
@Composable private fun Scroll(game: GameUi, vm: PlayerCardViewModel, card: PlayerCard) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Spine(game, card, Modifier.width(96.dp).fillMaxHeight())
        Column(Modifier.weight(1f)) {
            card.title.takeIf { it.isNotBlank() }?.let {
                Text(titleName(it).uppercase(), color = Caution, style = MaterialTheme.typography.labelSmall, letterSpacing = 1.sp)
            }
            Text(card.name, style = relicName(19).copy(color = GoldBright), maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            card.guild?.let { guild ->
                val opens = vm.guildOpens(card)
                ReferenceRow(ui("player.guild"), ui("player.guild_value", guild.tag, ui("guild.role.${guild.role.name}")), Parchment, onClick = if (opens) ({ vm.openGuild(card) }) else null)
            }
            leagueLevel(game, card.league)?.let { ReferenceRow(ui("player.league"), ui("rift.league", it), Parchment) }
            ReferenceRow(ui("player.online"), presenceText(card.online, card.lastSeenAt), Presence.of(card.online, card.lastSeenAt).color.takeIf { card.online } ?: Parchment)
        }
    }
}

/** Корешок свитка: уровень серифом, знак класса, имя класса. */
@Composable private fun Spine(game: GameUi, card: PlayerCard, modifier: Modifier) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.background(Brush.verticalGradient(listOf(PanelRaised, Panel)), shape).border(1.dp, Bronze, shape).padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(ui("player.level"), color = Muted, style = MaterialTheme.typography.labelSmall)
        Text(card.level.toString(), style = relicName(22).copy(color = GoldBright))
        ClassPortrait(card.heroClass.takeIf { it.isNotBlank() }, game.world.portraits, Modifier.size(44.dp), round = true)
        Text(classTitle(card.heroClass), color = Muted, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** Справочная строка свитка: подпись слева, значение справа; со стрелкой - нажатие ведёт дальше. */
@Composable private fun ReferenceRow(label: String, value: String, color: Color, onClick: (() -> Unit)? = null) {
    Column(Modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
            Text(value + if (onClick != null) " ›" else "", color = color, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        }
        HorizontalDivider(color = Bronze.copy(alpha = .6f), thickness = 1.dp)
    }
}

/** Кнопки карточки: «Написать» и «В гильдию» - по правам сервера, «Лоты игрока» - всегда, с их числом. */
@Composable private fun ColumnScope.CardActions(game: GameUi, card: PlayerCard, onWrite: () -> Unit, onInvite: () -> Unit, onLots: () -> Unit) {
    val rights = card.rights
    val canWrite = rights.mail && !rights.moderation?.login.isNullOrBlank()
    if (canWrite || rights.invite) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (canWrite) ForgeButton(onClick = onWrite, enabled = !game.busy, modifier = Modifier.weight(1f)) { Text(ui("player.write")) }
            if (rights.invite) ForgeOutlinedButton(onClick = onInvite, enabled = !game.busy, modifier = Modifier.weight(1f)) { Text(ui("player.invite")) }
        }
    }
    ForgeOutlinedButton(onClick = onLots, enabled = !game.busy, modifier = Modifier.fillMaxWidth()) { Text(ui("player.lots", card.lots)) }
}

/** Строка модерации (только модератору и администратору): прежние бан и удаление - в досье окна модерации. */
@Composable private fun ModerationRow(game: GameUi, rights: CardModeration, onDossier: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        HorizontalDivider(color = Bronze, thickness = 1.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (rights.banHero || rights.banAccount || rights.banDevice) {
                ForgeTextButton(onClick = onDossier, enabled = !game.busy) { Text(ui("moderation.ban"), color = LifeRed) }
            }
            if (rights.delete) ForgeTextButton(onClick = onDossier, enabled = !game.busy) { Text(ui("moderation.delete"), color = LifeRed) }
            Spacer(Modifier.weight(1f))
            ForgeTextButton(onClick = onDossier, enabled = !game.busy) { Text(ui("moderation.dossier"), color = Gold) }
        }
    }
}

/** Нижняя граница лиги Разлома [league] уровнем; таблицы лиг нет - null. */
private fun leagueLevel(game: GameUi, league: Int): Int? = game.index?.campaign?.trials?.rift?.leagues?.getOrNull(league)
