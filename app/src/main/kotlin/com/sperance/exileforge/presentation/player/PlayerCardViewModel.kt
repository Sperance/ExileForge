package com.sperance.exileforge.presentation.player

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.feedback.FeedbackRepository
import com.sperance.exileforge.core.guild.GuildRepository
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.model.hero.PlayerCard
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.presentation.guild.GuildActions
import com.sperance.exileforge.presentation.market.MarketActions
import com.sperance.exileforge.presentation.nav.Navigator
import com.sperance.exileforge.presentation.nav.Route
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Открытая карточка игрока (4.5.1): чья ([target], null - закрыта) и ответ сервера ([card], null - ещё читается). */
data class PlayerCardUi(val target: String? = null, val card: PlayerCard? = null)

/**
 * Карточка игрока (4.5.1): одна на приложение, открывается нажатием на имя или строку игрока где угодно (`LocalPlayerCard`).
 * Профиль и права на действия - у сервера (`GET hero/card`): клиент не дублирует права гильдии и модерации, а лишь прячет
 * недоступные кнопки. Действия карточки ведут в свои экраны: письмо администратора, гильдия, витрина лотов продавца.
 */
class PlayerCardViewModel(
    private val connection: ServerConnection,
    private val heroes: HeroRepository,
    private val commands: CommandRunner,
    private val guilds: GuildRepository,
    private val guildActions: GuildActions,
    private val market: MarketActions,
    private val feedback: FeedbackRepository,
    private val navigator: Navigator,
    slice: GameSlice,
) : ViewModel() {
    val game: StateFlow<GameUi> = slice.ui

    private val mutable = MutableStateFlow(PlayerCardUi())
    val state: StateFlow<PlayerCardUi> = mutable.asStateFlow()

    /** Карточка героя [target] глазами своего героя; без своего героя (меню героев) смотреть не от кого. */
    fun open(target: String) {
        val viewer = heroes.heroId.takeIf { it.isNotBlank() } ?: return
        if (target.isBlank()) return
        mutable.value = PlayerCardUi(target)
        commands.read(Reads.PLAYER_CARD, restart = true) {
            try {
                val card = connection.api.hero.card(viewer, target)
                mutable.update { if (it.target == target) it.copy(card = card) else it }
            } catch (e: com.sperance.exileforge.core.network.ApiFailure) {
                // Отказ (героя нет, другой сервер) печатает исполнитель; пустой лист не остаётся висеть
                mutable.update { if (it.target == target) PlayerCardUi() else it }
                throw e
            }
        }
    }

    fun close() {
        mutable.value = PlayerCardUi()
    }

    /** «Лоты игрока»: витрина аукциона, суженная до этого продавца. */
    fun sellerLots(card: PlayerCard) = leave {
        market.sellerLots(card.heroId, card.name)
        navigator.tab(Route.Auction)
    }

    /** «В гильдию»: приглашение в свою гильдию по имени - право уже проверил сервер карточки. */
    fun invite(card: PlayerCard) {
        guildActions.invite(card.name)
        close()
    }

    /** «Написать» (только администратору): форма письма с логином владельца героя. */
    fun write(card: PlayerCard) {
        val login = card.rights.moderation?.login?.takeIf { it.isNotBlank() } ?: return
        leave {
            feedback.update { it.copy(mailTo = login) }
            navigator.open(Route.Settings(MAIL_PAGE))
        }
    }

    /**
     * Можно ли открыть гильдию карточки: своя - её зал, у героя без гильдии (или ещё не прочитанной) - поиск по знаку; чужая
     * гильдия при своей не открывается - экрана чужой гильдии нет.
     */
    fun guildOpens(card: PlayerCard): Boolean {
        val guild = card.guild ?: return false
        val mine = guilds.state.value.mine ?: return true
        return mine.guild == null || mine.guild?.id == guild.id
    }

    fun openGuild(card: PlayerCard) {
        val guild = card.guild ?: return
        if (!guildOpens(card)) return
        leave {
            if (guilds.state.value.mine?.guild?.id != guild.id) {
                guildActions.query(guild.tag)
                guildActions.search()
            }
            navigator.tab(Route.Guild)
        }
    }

    /** Модерация героя карточки: окно модерации с его досье (досье открывает модель модерации). */
    fun moderate() = leave { navigator.open(Route.Settings(MODERATION_PAGE)) }

    private inline fun leave(block: () -> Unit) {
        close()
        block()
    }

    private companion object {
        /** Страница настроек с письмом администратора (`SettingsPage.MAIL`). */
        const val MAIL_PAGE = "MAIL"

        /** Страница настроек окна модерации (`SettingsPage.MODERATION`). */
        const val MODERATION_PAGE = "MODERATION"
    }
}
