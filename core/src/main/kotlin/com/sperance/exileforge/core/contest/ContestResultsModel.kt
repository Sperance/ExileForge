package com.sperance.exileforge.core.contest

import com.sperance.exileforge.core.app.GameSettings
import com.sperance.exileforge.core.app.PreferencesRepository
import com.sperance.exileforge.core.feedback.FeedbackRepository
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.model.feedback.Mail
import com.sperance.exileforge.core.model.hero.ServerClock
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.rules.content.ContestHonour
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Итоги состязаний к показу при входе героя: призы из писем, что этот герой ещё не видел ([shown]), новые первыми - видны последние
 * [SHOWN] ([visible]), прочие - числом [more] («ещё N - в почте»).
 */
data class ContestNews(val prizes: List<ContestPrizeMail> = emptyList()) {
    val visible: List<ContestPrizeMail> get() = prizes.take(SHOWN)

    val more: Int get() = (prizes.size - SHOWN).coerceAtLeast(0)

    val empty: Boolean get() = prizes.isEmpty()

    companion object {
        /** Сколько грамот лист показывает разом. */
        const val SHOWN = 3

        /** Итоги ящика [mail]: письма о призах, ещё непрочитанные и не показанные на устройстве ([shown] - их id). */
        fun of(mail: List<Mail>, shown: Collection<String>): ContestNews = ContestNews(
            mail.asSequence().filter { !it.read && it.id !in shown }.mapNotNull(ContestPrizeMail::of).sortedByDescending { it.createdAt }.toList(),
        )
    }
}

/** Итоги героя [heroId]: его лучший действующий титул состязаний [honour] (null - нет) - для шапки Героя. */
data class ContestResults(val heroId: String = "", val honour: ContestHonour? = null)

/**
 * Итоги состязаний героя в игре: при входе героя ([check]) тихо читаются его ящик (в общую почту, [FeedbackRepository]) и свой титул
 * (карточка игрока своего героя); лист итогов показывает непоказанные призы ([news]) один раз на приз - [seen] запоминает их на
 * устройстве. Ответ, пришедший к уже сменённому герою, не ложится.
 */
class ContestResultsModel(
    private val connection: ServerConnection,
    private val heroes: HeroRepository,
    private val mail: FeedbackRepository,
    private val prefs: PreferencesRepository,
    private val commands: CommandRunner,
    private val scope: CoroutineScope,
) {
    private val mutable = MutableStateFlow(ContestResults())
    val results: StateFlow<ContestResults> = mutable.asStateFlow()

    /** Настройки устройства: в них - показанные итоги. */
    val settings: StateFlow<GameSettings> = prefs.settings

    /** Ящик героя в игре. */
    val inbox = mail.state

    /** Прочитать ящик и свой титул героя в игре: при входе героя, не чаще [FRESH_MS] на героя. */
    fun check() {
        val heroId = heroes.heroId.takeIf { it.isNotBlank() } ?: return
        mutable.update { if (it.heroId == heroId) it else ContestResults(heroId) }
        commands.read(Reads.CONTEST_RESULTS, silent = true, tag = heroId, maxAgeMs = FRESH_MS) {
            val letters = connection.api.mail.inbox(heroId)
            val honour = ContestHonour.best(connection.api.hero.card(heroId, heroId).honours.filter { it.until > ServerClock.now() })
            if (!heroes.onScreen(heroId)) return@read
            mail.update { it.withMail(letters) }
            mutable.update { if (it.heroId == heroId) it.copy(honour = honour) else it }
        }
    }

    /** Итоги ящика [letters] к показу герою [heroId]: только его письма. */
    fun news(heroId: String, letters: List<Mail>, settings: GameSettings): ContestNews = ContestNews.of(letters.filter { it.heroId == heroId }, settings.shownPrizes[heroId].orEmpty())

    /** Итоги писем [mailIds] показаны герою в игре: при следующем входе не встанут. */
    fun seen(mailIds: Collection<String>) {
        val heroId = heroes.heroId
        scope.launch { prefs.keepShownPrizes(heroId, mailIds) }
    }

    private companion object {
        /** Сколько итоги героя считаются свежими: возврат из боя на экран Города ящик не перечитывает. */
        const val FRESH_MS = 10 * 60 * 1000L
    }
}
