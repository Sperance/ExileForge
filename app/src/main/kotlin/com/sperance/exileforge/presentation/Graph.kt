package com.sperance.exileforge.presentation

import com.sperance.exileforge.core.admin.AdminRepository
import com.sperance.exileforge.core.campaign.ExpeditionRepository
import com.sperance.exileforge.core.crafts.CraftsRepository
import com.sperance.exileforge.core.feedback.FeedbackRepository
import com.sperance.exileforge.core.guild.GuildRepository
import com.sperance.exileforge.core.hero.HeroRepository
import com.sperance.exileforge.core.i18n.LanguageRepository
import com.sperance.exileforge.core.market.MarketRepository
import com.sperance.exileforge.core.network.LinkRepository
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
import com.sperance.exileforge.presentation.state.AppModes

/** Репозитории :core одним узлом (3.80.15): их делят сервисы приложения (`AppService`). */
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
    val languages: LanguageRepository,
    val links: LinkRepository,
    val admins: AdminRepository,
    val modes: AppModes,
)

/** Действия игры одним узлом (3.80.15): их зовут и экраны через свои модели, и сервисы приложения - из прогрева, входа и связи. */
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
