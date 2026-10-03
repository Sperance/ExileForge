package com.sperance.exileforge.presentation

import com.sperance.exileforge.core.campaign.ExpeditionRepository
import com.sperance.exileforge.core.crafts.CraftsRepository
import com.sperance.exileforge.core.feedback.FeedbackRepository
import com.sperance.exileforge.core.guild.GuildRepository
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.market.MarketRepository
import com.sperance.exileforge.core.quests.QuestRepository
import com.sperance.exileforge.core.session.SessionRepository
import com.sperance.exileforge.core.world.WorldRepository
import com.sperance.exileforge.presentation.crafts.CraftsActions
import com.sperance.exileforge.presentation.expedition.ExpeditionActions
import com.sperance.exileforge.presentation.expedition.TrialActions
import com.sperance.exileforge.presentation.guild.GuildActions
import com.sperance.exileforge.presentation.hero.HeroActions
import com.sperance.exileforge.presentation.hero.HeroSync
import com.sperance.exileforge.presentation.market.MarketActions
import com.sperance.exileforge.presentation.quests.QuestActions

/** Репозитории :core одним узлом (3.80.15): переходный держатель, пока `ForgeRuntime` отражает их в общее состояние. */
class Repositories(
    val sessions: SessionRepository,
    val world: WorldRepository,
    val heroes: HeroRepository,
    val boards: QuestRepository,
    val markets: MarketRepository,
    val guilds: GuildRepository,
    val feedbacks: FeedbackRepository,
    val crafts: CraftsRepository,
    val expeditions: ExpeditionRepository,
)

/** Действия игры одним узлом (3.80.15): их зовут и экраны через свои модели, и `ForgeRuntime` из прогрева и похода. */
class Actions(
    val quests: QuestActions,
    val market: MarketActions,
    val guild: GuildActions,
    val crafts: CraftsActions,
    val hero: HeroActions,
    val heroSync: HeroSync,
    val expedition: ExpeditionActions,
    val trial: TrialActions,
)
