package com.sperance.exileforge.core.model.town

import com.sperance.exileforge.core.model.auction.AuctionMine
import com.sperance.exileforge.core.model.crafts.CraftsState
import com.sperance.exileforge.core.model.guild.GuildMine
import com.sperance.exileforge.rules.content.QuestBoard
import kotlinx.serialization.Serializable

/**
 * Город одним ответом (`POST hero/town`): доска заданий с выдачей (как `hero/quests/refresh`), ремёсла с досчётом (как
 * `hero/crafts/settle`), гильдия после захода (как `guild/visit`) и свой рынок (как `auctionlot/mine`); [auction] null - рынок герою
 * ещё закрыт по уровню. Снимок героя едет в конверте, как у любой команды.
 */
@Serializable data class TownVisit(val quests: QuestBoard, val crafts: CraftsState, val guild: GuildMine, val auction: AuctionMine? = null)
