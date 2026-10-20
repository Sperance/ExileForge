# Версии и миграции

История подъёмов `API_REVISION`, `RULES_VERSION` и форм коллекций сервера - то, чего нет в коде. В каждом разделе - 10 последних подъёмов, одна строка на подъём: номер - версия пакета - что сломалось, что снесено или что делает миграция. Строка пишется в коммите подъёма, старшая удаляется (она в git).

## API_REVISION (сервер `Constants.kt`, клиент `core/model/sync/Sync.kt`)

- 86 - 0.14.2 - вид отчёта `CRASH`, `BugReportRequest.traces|trail|heroSheet`; без предложений: сняты `SUGGESTION`, `IssueState.DONE`, `Suggestion` → `KnownReport`.
- 87 - 0.14.4 - сняты `bugreport/similar`, `feedback.similar`, `IssueState.IN_PROGRESS`; новые `feedback.exportHoldMinutes`, `SkillDefinition.sigil`.
- 88 - 0.15.0 - `StoryBookTaken`, мана умений по уровню умения (сняты `ManaPrice`, `KitSkill.price`), `StatDef.gain`, `rules.album`, `POST admin/fate?code=`, `X-Client-Version`.
- 89 - 0.16.0 - тайник в `HeroItem`, `ItemInstance.og`, снят автопробег (`auto*`), `atlas.json` с `layout`, `guild/log` - `{entries, more}`, `Mail.link`, `POST admin/feedback/note`.
- 90 - 0.16.2 - стек падения с версией сборки, отчёты модерации без системных данных, место на доске Мощи, раздел `CHARGES`, `{r:…}` по пути правил.
- 91 - 0.16.3 - `RunStart.chestStock`, `EventReward.chestStock`, `ActiveMap.sources`, `AuctionLot.returned`, `WorkGains/WorkTally.missed`, `FountainRule.flasks`.
- 92 - 0.16.5 - `Received.overflowSold|autoSold` (`Sale`) вместо `sold/gold/shards`, `ProfessionView.layers` вместо `bonus`, `POST admin/fate?user=&reason=`, `GET admin/feedback/detail`.
- 93 - 0.16.6 - снят `GET admin/feedback/detail` (`ReportDetail`); `BugReportRequest|OwnReport|FullReport.topic` (улучшение администратора), `CrashTrace.count` на сервере (склейка падений отчёта).
- 94 - 0.16.8 - `MailRequest.userId`, `POST guild/stash/tab/name` (`GuildStashTab.name`), `WorkView.stoppedAt` и `POST hero/crafts/resume`, `Received.deferred` и `POST hero/campaign/settle` (`CampaignState.pendingSale`), `TrapKind.shape` вместо `reach/rearm`, `CombatRules.tempo`.
- 95 - 0.16.9 - событие журнала `REGEAR` и `RunStart.wearable` (надевание в походе), `RushTier.unique`, `Item.bound`, `GuildRules.entryLevelMax`, снят `POST hero/trials/key`, `hero/sell?unequip`, отказы `CH_043`/`CH_044`.

## RULES_VERSION (`rules/.../content/Content.kt`)

- 88 - 0.14.2 - фонтан лечит больше, классы без строки получаемого урона, здоровье обычных монстров выше.
- 89 - 0.15.0 - мана умений по уровню умения, новые формулы и пределы боя, новые дерево и Атлас, яд и хаос, подгонка под цели баланса.
- 90 - 0.16.0 - щит по виду урона, резерв здоровья аурами, восстановление стражей под `bosses.sustainShare`, чистая зона карты.
- 91 - 0.16.2 - риск карты без опыта, доля стай Ваал-зоны, отдых вне боя, формы карт «Лабиринт» и «Арены».
- 92 - 0.16.3 - реген маны не ниже нуля, заряды фляг у фонтана, сундуки на старте захода, требования по семейству основы.
- 93 - 0.16.4 - темп боя вдвое: здоровье монстров ниже, удары выше, ярость и тотемы раньше.
- 94 - 0.16.6 - «Несгибаемый бастион» дороже (скорость атаки −12%), вечный бафф не вытесняется временным того же вида - действует сильнейший; у карты ниже порога тиров свой уровень (район падения, сундук или герой у картографа со сдвигом `maps.levels`), уровень района такой карты - не ниже его; монстры карт, зон кампании и арен сильнее от уровня героя над районом сверх льготы в 6 уровней (`heroScale`), опыт за них - тем же множителем здоровья.
- 95 - 0.16.7 - фазы 44 боссам актов 2–9 и вождям Бездны, новые виды Пламя/Стужа/Кровопускатель/Сотрясатель/Тень, подгонка силы стражей.
- 96 - 0.16.8 - темп боя ×0.55 (`combat.tempo`), здоровье редкостей ×1.6/×3.5/×10 и времена ×1.82, недуги ×1.8, скорости боя [1,2], ловушки - формы, ритм, лёд/молния/жернов/маятник/пульс, без наград, мод «Больше ловушек»; эссенций вдвое меньше, карт с монстров 0.028, остановка работы ремесла через 12 ч.
- 97 - 0.16.9 - узлы дерева «регенерация щита» - доля щита в секунду, награда раша срезана под 1,2-1,5× захода, уникалка раша - шансом по ступеням, порядок бросков сундука раша.

## Формы коллекций и миграции (`config/CollectionSchemas.kt`)

- `task.renamedModifiers` 1 - 0.15.0 - строки копий вещей с прежними кодами семейств - под новыми кодами, вещи целы.
- `task.spareUniques` 1 - 0.15.0 - надетая вторая копия уникалки снята в тайник.
- `task.atlasReset` 1 - 0.15.0 - узлы Атласа сняты бесплатно, очки босса пересчитаны, герою письмо.
- `HeroItem` 1 - 0.16.0 - новая коллекция тайника: документ на копию вещи `heroId:itemId`, в герое - надетое и вставленное.
- `task.heroItems` 1 - 0.16.0 - тайник и переполнение героев перенесены в `HeroItem`, id и номера копий целы.
- `task.atlasRebuild` 1 - 0.16.0 - узлы нового дерева Атласа сняты бесплатно, очки целы, герою письмо.
- `task.looseJewels` 1 - 0.16.2 - самоцветы из невзятых гнёзд дерева - в тайник.
- `task.itemLevelFloor` 1 - 0.16.2 - уровень предмета старых копий поднят до требования шаблона.
- части `BugReport.traces` 1, `Hero`/`HeroRun.mapExperience` 1 - 0.16.2 - поля сняты, документы целы.
- `HeroRun` 2 - 0.16.3 - снесены незаконченные заходы (запас сундуков).
