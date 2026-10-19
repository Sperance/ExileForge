package com.sperance.exileforge.core.player

import com.sperance.exileforge.core.app.GameSlice
import com.sperance.exileforge.core.app.GameUi
import com.sperance.exileforge.core.app.Landing
import com.sperance.exileforge.core.app.Screens
import com.sperance.exileforge.core.feedback.FeedbackRepository
import com.sperance.exileforge.core.feedback.LetterActions
import com.sperance.exileforge.core.feedback.LetterDraft
import com.sperance.exileforge.core.guild.GuildActions
import com.sperance.exileforge.core.guild.GuildRepository
import com.sperance.exileforge.core.hall.HallFocus
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.market.MarketActions
import com.sperance.exileforge.core.model.feedback.MailAddressee
import com.sperance.exileforge.core.model.hero.PlayerCard
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.rules.content.HallBoard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Открытая карточка игрока: чья ([target], null - закрыта) и ответ сервера ([card], null - ещё читается). */
data class PlayerCardUi(val target: String? = null, val card: PlayerCard? = null)

/**
 * Карточка игрока: одна на приложение, открывается нажатием на имя или строку игрока где угодно (`LocalPlayerCard`).
 * Профиль и права на действия - у сервера (`GET hero/card`): клиент не дублирует права гильдии и модерации, а лишь прячет
 * недоступные кнопки. Действия карточки ведут в свои экраны: письмо герою и администратора, гильдия, витрина лотов
 * продавца; игнор писем героя меняет карточку на месте.
 */
class PlayerCardModel(
    private val connection: ServerConnection,
    private val heroes: HeroRepository,
    private val commands: CommandRunner,
    private val guilds: GuildRepository,
    private val guildActions: GuildActions,
    private val market: MarketActions,
    private val feedback: FeedbackRepository,
    private val letters: LetterActions,
    private val screens: Screens,
    private val hall: HallFocus,
    slice: GameSlice,
) {
    val game: StateFlow<GameUi> = slice.ui

    private val mutable = MutableStateFlow(PlayerCardUi())
    val state: StateFlow<PlayerCardUi> = mutable.asStateFlow()

    /**
     * Карточка героя [target] глазами своего героя; без своего героя (меню героев) смотреть не от кого.
     *
     * Отказ (героя нет, другой сервер) печатает исполнитель; пустой лист не остаётся висеть.
     */
    fun open(target: String) {
        val viewer = heroes.heroId.takeIf { it.isNotBlank() } ?: return
        if (target.isBlank()) return
        mutable.value = PlayerCardUi(target)
        commands.read(Reads.PLAYER_CARD, restart = true) {
            try {
                val card = connection.api.hero.card(viewer, target)
                mutable.update { if (it.target == target) it.copy(card = card) else it }
            } catch (e: com.sperance.exileforge.core.network.ApiFailure) {
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
        screens.tab(Landing.AUCTION)
    }

    /** «Место по Мощи ›»: доска славы «Мощь», прокрученная к герою карточки. */
    fun openMightBoard(card: PlayerCard) = leave { hall.open(HallBoard.MIGHT, card.heroId) }

    /** «В гильдию»: приглашение в свою гильдию по имени - право уже проверил сервер карточки. */
    fun invite(card: PlayerCard) {
        guildActions.invite(card.name)
        close()
    }

    /** «Написать»: окно письма герою карточки; адресат - плашкой, не вводом. */
    fun write(card: PlayerCard) = leave { letters.compose(LetterDraft.to(card.heroId, card.name, card.heroClass, card.level)) }

    /** Письмо администратора аккаунту героя (только администратору): форма письма адресована владельцу по id аккаунта. */
    fun writeAdmin(card: PlayerCard) {
        val owner = card.rights.moderation?.takeIf { it.userId.isNotBlank() } ?: return
        leave {
            feedback.update { it.copy(mailTo = MailAddressee.of(owner.userId, owner.login, card.name)) }
            screens.open(Landing.MAIL)
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
            screens.tab(Landing.GUILD)
        }
    }

    /** Модерация героя карточки: окно модерации с его досье (досье открывает модель модерации). */
    fun moderate() = leave { screens.open(Landing.MODERATION) }

    private inline fun leave(block: () -> Unit) {
        close()
        block()
    }
}
