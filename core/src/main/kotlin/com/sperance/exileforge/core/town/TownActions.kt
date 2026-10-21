package com.sperance.exileforge.core.town

import com.sperance.exileforge.core.crafts.CraftsActions
import com.sperance.exileforge.core.guild.GuildActions
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.hero.HeroSync
import com.sperance.exileforge.core.market.MarketActions
import com.sperance.exileforge.core.network.ApiFailure
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.questboard.QuestActions
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.session.freshness
import com.sperance.exileforge.core.world.ContentLoader
import kotlinx.coroutines.CancellationException
import java.io.IOException

/**
 * Вход в Город одним запросом (`POST hero/town`): доска заданий, ремёсла с досчётом, гильдия после захода и свой рынок - одним ответом,
 * снимок героя едет с ним. Ответ ложится в те же хранилища, что ответы `quests/refresh`, `crafts/settle`, `guild/visit` и
 * `auctionlot/mine`, и считается их свежим чтением: экран здания, открытый следом, сервер не спрашивает
 * ([com.sperance.exileforge.core.session.READ_FRESH_MS]). Рынка в ответе нет, пока он герою закрыт по уровню, - экран рынка спросит
 * его сам и получит причину замка. Прежние маршруты остаются за своими экранами.
 */
class TownActions(
    private val heroes: HeroRepository,
    private val heroSync: HeroSync,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val content: ContentLoader,
    private val quests: QuestActions,
    private val crafts: CraftsActions,
    private val guild: GuildActions,
    private val market: MarketActions,
) {
    private val api: GameApi get() = connection.api

    /**
     * Город чтением под своим ключом: возврат в игру и связь на вкладке Города. С [fresh] ответ моложе срока свежести чтения не
     * спрашивается снова. Пока чтение в полёте, чтения зданий ([BROUGHT]) не идут вторым запросом. false - чтение пропущено.
     */
    fun enter(silent: Boolean = false, fresh: Boolean = false): Boolean {
        val id = heroes.heroId.takeIf { it.isNotBlank() } ?: return false
        return commands.read(Reads.TOWN, silent = silent, tag = id, maxAgeMs = freshness(fresh), brings = BROUGHT) { visit() }
    }

    /**
     * Сам запрос: прогрев зовёт его и ждёт ответа, не через очередь чтений. Контент прежде - доску и ремёсла рисуют правила. Отказ
     * сервера или связи не оставляет героя без чтения: он перечитывается своим запросом, а отказ идёт дальше.
     */
    suspend fun visit() {
        val id = heroes.heroId.takeIf { it.isNotBlank() } ?: return
        content.ensure()
        val town = try {
            api.hero.town(id)
        } catch (e: ApiFailure) {
            heroInstead(id)
            throw e
        } catch (e: IOException) {
            heroInstead(id)
            throw e
        }
        if (!heroes.onScreen(id)) return
        quests.land(id, town.quests)
        crafts.land(id, town.crafts)
        guild.land(id, town.guild)
        town.auction?.let { market.land(id, it) }
        commands.settled(if (town.auction != null) BROUGHT else BROUGHT - Reads.LOTS, id)
    }

    /** Герой [id] своим чтением, когда Город не ответил; его собственный отказ Город не заслоняет. */
    private suspend fun heroInstead(id: String) {
        if (!heroes.onScreen(id)) return
        try {
            heroSync.readHero()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
        }
    }

    private companion object {
        /** Чтения, чей ответ Город принёс вместо них. */
        val BROUGHT = setOf(Reads.QUESTS, Reads.CRAFTS, Reads.GUILD, Reads.LOTS)
    }
}
