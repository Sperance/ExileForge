package com.sperance.exileforge.core.town

import com.sperance.exileforge.core.crafts.CraftsActions
import com.sperance.exileforge.core.guild.GuildActions
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.market.MarketActions
import com.sperance.exileforge.core.network.GameApi
import com.sperance.exileforge.core.questboard.QuestActions
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.core.session.Reads
import com.sperance.exileforge.core.session.ServerConnection
import com.sperance.exileforge.core.world.ContentLoader

/**
 * Вход в Город одним запросом (`POST hero/town`): доска заданий, ремёсла с досчётом, гильдия после захода и свой рынок - одним ответом,
 * снимок героя едет с ним. Ответ ложится в те же хранилища, что ответы `quests/refresh`, `crafts/settle`, `guild/visit` и
 * `auctionlot/mine`, и считается их свежим чтением: экран здания, открытый следом, сервер не спрашивает
 * ([com.sperance.exileforge.core.session.READ_FRESH_MS]). Рынка в ответе нет, пока он герою закрыт по уровню, - экран рынка спросит
 * его сам и получит причину замка. Прежние маршруты остаются за своими экранами.
 */
class TownActions(
    private val heroes: HeroRepository,
    private val connection: ServerConnection,
    private val commands: CommandRunner,
    private val content: ContentLoader,
    private val quests: QuestActions,
    private val crafts: CraftsActions,
    private val guild: GuildActions,
    private val market: MarketActions,
) {
    private val api: GameApi get() = connection.api

    /** Город чтением под своим ключом: возврат в игру и связь на вкладке Города. */
    fun enter(silent: Boolean = false) {
        val id = heroes.heroId
        if (id.isBlank()) return
        commands.read(Reads.TOWN, silent = silent, tag = id) { visit() }
    }

    /** Сам запрос: прогрев зовёт его и ждёт ответа, не через очередь чтений. Контент прежде - доску и ремёсла рисуют правила. */
    suspend fun visit() {
        val id = heroes.heroId
        check(id.isNotBlank()) { ui("auction.choose_character") }
        content.ensure()
        val town = api.hero.town(id)
        if (!heroes.onScreen(id)) return
        quests.land(id, town.quests)
        crafts.land(id, town.crafts)
        guild.land(id, town.guild)
        town.auction?.let { market.land(id, it) }
        commands.settled(if (town.auction != null) BROUGHT else BROUGHT - Reads.LOTS, id)
    }

    private companion object {
        /** Чтения, чей ответ Город принёс вместо них. */
        val BROUGHT = setOf(Reads.QUESTS, Reads.CRAFTS, Reads.GUILD, Reads.LOTS)
    }
}
