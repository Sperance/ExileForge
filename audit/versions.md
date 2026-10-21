# Версии и миграции

История подъёмов `API_REVISION`, `RULES_VERSION` и форм коллекций сервера - то, чего нет в коде. В каждом разделе - 10 последних подъёмов, одна строка на подъём: номер - версия пакета - что сломалось, что снесено или что делает миграция. Строка пишется в коммите подъёма, старшая удаляется (она в git).

## API_REVISION (сервер `Constants.kt`, клиент `core/model/sync/Sync.kt`)

- 91 - 0.16.3 - `RunStart.chestStock`, `EventReward.chestStock`, `ActiveMap.sources`, `AuctionLot.returned`, `WorkGains/WorkTally.missed`, `FountainRule.flasks`.
- 92 - 0.16.5 - `Received.overflowSold|autoSold` (`Sale`) вместо `sold/gold/shards`, `ProfessionView.layers` вместо `bonus`, `POST admin/fate?user=&reason=`, `GET admin/feedback/detail`.
- 93 - 0.16.6 - снят `GET admin/feedback/detail` (`ReportDetail`); `BugReportRequest|OwnReport|FullReport.topic` (улучшение администратора), `CrashTrace.count` на сервере (склейка падений отчёта).
- 94 - 0.16.8 - `MailRequest.userId`, `POST guild/stash/tab/name` (`GuildStashTab.name`), `WorkView.stoppedAt` и `POST hero/crafts/resume`, `Received.deferred` и `POST hero/campaign/settle` (`CampaignState.pendingSale`), `TrapKind.shape` вместо `reach/rearm`, `CombatRules.tempo`.
- 95 - 0.16.9 - событие журнала `REGEAR` и `RunStart.wearable` (надевание в походе), `RushTier.unique`, `Item.bound`, `GuildRules.entryLevelMax`, снят `POST hero/trials/key`, `hero/sell?unequip`, отказы `CH_043`/`CH_044`.
- 96 - 0.16.10 - `Job.additives` - код группы, `CraftingRules.additives` - группы с эффектами `AdditiveEffect` (`MODIFIER`/`YIELD`), `CraftsState.additives`; `hero/orb?count` (пачка сферы качества), снят `hero/merchant/buyOrb` и `rules.merchant.orbs`; поля контента `room` (семейство) и `cost` (вариант верстака); `Job.Chart.band` вместо `region`, счётчик `MAP_RUNS`; почта без частотных лимитов и уровня (из `rules.mail` сняты `perDay`/`perRecipient`/`guildPerDay`/`cooldownSeconds`/`windowHours`, из `unlocks` - `MAIL`; `mail/quota` без `to` и полей счёта; `MailGate` - `OPEN`/`MUTED`).
- 97 - 0.16.11 - `HeroView.freeProgress` (замки прогресса - только тестировщику со свободным прогрессом) и `POST hero/tester/freeProgress?on=`, `RunReport|TrialReport.refusals` (только причины темпа), `run.plausibility.rushBossSeconds`, `TrialRun.nextBoss|wall`; Разлом: `RiftRun.honor|keys|burned` вместо `life`, `RiftAct.Fight.taken` вместо `life`/`hurt`, `RiftAct.Chalice|Open`, `RiftOffer.Coffers`, `RiftOffer.Forge.chalice`, `RiftNode.key`, `RiftResult.honor|honorScore|burned`, `RiftReport.coffer`, `rift.honor|keys`, `RiftScore.honor` вместо `flawless`, снят `RiftNodes.restHeal`.
- 98 - 0.16.11 - `POST guild/stash/deposit?tab=` - пакетный вклад: тело - посылки `GuildDepositParcel` (копия `itemId` или стопка `code`+`amount`), ответ `GuildDeposited` (`stash`, `placed`, `refused` по `DepositBar`); правило `GuildDeposit`, событие журнала `STASH_IN_MANY`; торги рынка: `AuctionLot.mode|bidding|standing` (`LotMode`, `BidState`), `POST auctionlot/bid|buyout|accept`, `sell/*?mode=BID&minPrice=`, `search?mode=`, `AuctionMine.bids`, `AuctionSlots.bids|bidLimit`, `rules.auction.bidStep|bidStepMin|bidTailHours|bidsMax`, `slots` 100, письма `mail.auction_outbid|bid_returned|sold|won`, отказы `AU_018`-`AU_024`.
- 99 - 0.17.0 - часть снимка `campaign` разделена на `campaign` и `run` (`RunState?`, null - заход закрыт), новый `POST hero/town` (`TownVisit`: доска заданий, ремёсла, гильдия, свой рынок одним ответом).
- 100 - 0.17.1 - статусы отчёта `BugStatus.DISCUSSION`/`WITHDRAWN`, `POST admin/feedback/comment` (комментарий модерации, письмо `mail.feedback_comment`), `POST bugreport/close` (отказ `BUG_011`) вместо `POST bugreport/delete` (удаление - `POST admin/feedback/delete`), `OwnReport.closable` вместо `deletable`, `AdminReport.discussable|queue`, `FullReport.awaitsModeration`; `GET admin/feedback?kind&queue&q&sort&page` - `FeedbackQueuePage` (`Page` + `QueueCounts` у первой страницы), действия модерации с отчётом отвечают `FeedbackMove` (отчёт и счёт очередей под `kind`/`q`); сняты голоса (`GET bugreport/known`, `POST bugreport/vote`, `KnownReport`, `Vote`, `BUG_003`, `likes`/`dislikes`, порядок `RATING`); одна страница `Page` (items, page, total?, more) у очереди отчётов, списков, корзины и журнала модерации, поиска и журнала гильдии; страница списков - `rules.pageSize` (до `PAGE_SIZE_MAX`) вместо `moderation.pageSize`, без параметра `size` у `guild/search`, `guild/log`, `auctionlot/search`; `rules.inputs.reason`.

## RULES_VERSION (`rules/.../content/Content.kt`)

- 90 - 0.16.0 - щит по виду урона, резерв здоровья аурами, восстановление стражей под `bosses.sustainShare`, чистая зона карты.
- 91 - 0.16.2 - риск карты без опыта, доля стай Ваал-зоны, отдых вне боя, формы карт «Лабиринт» и «Арены».
- 92 - 0.16.3 - реген маны не ниже нуля, заряды фляг у фонтана, сундуки на старте захода, требования по семейству основы.
- 93 - 0.16.4 - темп боя вдвое: здоровье монстров ниже, удары выше, ярость и тотемы раньше.
- 94 - 0.16.6 - «Несгибаемый бастион» дороже (скорость атаки −12%), вечный бафф не вытесняется временным того же вида - действует сильнейший; у карты ниже порога тиров свой уровень (район падения, сундук или герой у картографа со сдвигом `maps.levels`), уровень района такой карты - не ниже его; монстры карт, зон кампании и арен сильнее от уровня героя над районом сверх льготы в 6 уровней (`heroScale`), опыт за них - тем же множителем здоровья.
- 95 - 0.16.7 - фазы 44 боссам актов 2–9 и вождям Бездны, новые виды Пламя/Стужа/Кровопускатель/Сотрясатель/Тень, подгонка силы стражей.
- 96 - 0.16.8 - темп боя ×0.55 (`combat.tempo`), здоровье редкостей ×1.6/×3.5/×10 и времена ×1.82, недуги ×1.8, скорости боя [1,2], ловушки - формы, ритм, лёд/молния/жернов/маятник/пульс, без наград, мод «Больше ловушек»; эссенций вдвое меньше, карт с монстров 0.028, остановка работы ремесла через 12 ч.
- 97 - 0.16.9 - узлы дерева «регенерация щита» - доля щита в секунду, награда раша срезана под 1,2-1,5× захода, уникалка раша - шансом по ступеням, порядок бросков сундука раша.
- 98 - 0.16.10 - природного шанса яда у монстров нет (`combat.ailments` POISONED `chance` 0): яд - только шансом наложившего; работы «Каменоломня» и «Шлифовка», валежник с рощ, добавки любых работ (`AdditiveEffect`); пулы инструментов без 11 гибридов; рецепты верстака «+1 префикс/суффикс» (лимиты вещи от строк места); карта падает случайной зоны до героя+2, картограф - 6 диапазонов по 10 уровней; вес награды цели заданий (заходы и торговля 0,5), цель заходов - заход по карте с победой.
- 99 - 0.16.11 - Разлом: Честь вместо переносимого здоровья (бой с полным здоровьем, потеря - доля потерянного здоровья), ключи узлов из сида недели, сундуки Владыки (сундук на каждый ключ), очки остатка Чести вместо «без урона».

## Формы коллекций и миграции (`config/CollectionSchemas.kt`)

- `task.itemLevelFloor` 1 - 0.16.2 - уровень предмета старых копий поднят до требования шаблона.
- части `BugReport.traces` 1, `Hero`/`HeroRun.mapExperience` 1 - 0.16.2 - поля сняты, документы целы.
- `HeroRun` 2 - 0.16.3 - снесены незаконченные заходы (запас сундуков).
- часть `Hero.merchantOrbs` 1 - 0.16.10 - снято поле `merchant.orbs` (полка сфер торговца), документы целы.
- `task.retiredHybrids` 1 - 0.16.10 - гибриды инструментов на копиях (Hero, HeroItem, HeroRun, Mail, AuctionLot, Guild) - одиночным главным свойством с тем же числом (одиночный той же группы - большее число, занятая сторона - следующее свойство), дно редкости держится.
- `task.chartBands` 1 - 0.16.10 - идущая работа картографа `CHART_REGION_N` - в `CHART_BAND_*` своих уровней.
- `MailStamp`, `MailCounter` - 0.16.10 - сняты (`CollectionSchemas.RETIRED`): журнал и счётчик пределов почты не нужны, старт сносит их и их записи `Schema`.
- часть `Hero.riftRun` 1 - 0.16.11 - сняты незаконченные забеги Разлома (`campaign.trials.rift.run`: Честь и ключи вместо здоровья), документы целы.
- `HeroUnique` 1, `task.heroUniques` 1 - 0.17.0 - находки героев из карты `Hero.uniques` в коллекцию `HeroUnique` (документ на шаблон у героя), поле `uniques` снято, `uniquesFound` и номера копий целы.
- часть `BugReport.votes` 1 - 0.17.1 - сняты поля голосов `likes`, `dislikes`, `rating`, документы целы.
- `task.influenceSlot` 1 - 0.17.1 - строки влияния копий вещей (Hero, HeroItem, HeroRun, Mail, AuctionLot, Guild) - на место влияния сверх аффиксов: остаётся лучшая (высший тир, при равенстве - большая доля), прочие сняты, ниже дна редкости - долатано случайной обычной строкой; варианты выбора и раскрытия со строкой влияния сняты.
