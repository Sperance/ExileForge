# Версии и миграции

История подъёмов `API_REVISION`, `RULES_VERSION` и форм коллекций сервера - то, чего нет в коде. В каждом разделе - 10 последних подъёмов, одна строка на подъём: номер - версия пакета - что сломалось, что снесено или что делает миграция. Строка пишется в коммите подъёма, старшая удаляется (она в git).

## API_REVISION (сервер `Constants.kt`, клиент `core/model/sync/Sync.kt`)

- 84 - 0.14.0 - сняты урон крита, `combat.borrowedMana`, `MonsterSkill.mana`, `loot.uniqueReach`; новые `combat.cast`, общие умения, метки, `POST hero/quests/book`.
- 85 - 0.14.1 - отчёты в GitHub Issues (`BugReport.issue*` вместо `asana*`, `bugreport/known`, `admin/feedback/issue`), без снимков экрана, `abyss.leaderPower`.
- 86 - 0.14.2 - вид отчёта `CRASH`, `BugReportRequest.traces|trail|heroSheet`; без предложений: сняты `SUGGESTION`, `IssueState.DONE`, `Suggestion` → `KnownReport`.
- 87 - 0.14.4 - сняты `bugreport/similar`, `feedback.similar`, `IssueState.IN_PROGRESS`; новые `feedback.exportHoldMinutes`, `SkillDefinition.sigil`.
- 88 - 0.15.0 - `StoryBookTaken`, мана умений по уровню умения (сняты `ManaPrice`, `KitSkill.price`), `StatDef.gain`, `rules.album`, `POST admin/fate?code=`, `X-Client-Version`.
- 89 - 0.16.0 - тайник в `HeroItem`, `ItemInstance.og`, снят автопробег (`auto*`), `atlas.json` с `layout`, `guild/log` - `{entries, more}`, `Mail.link`, `POST admin/feedback/note`.
- 90 - 0.16.2 - стек падения с версией сборки, отчёты модерации без системных данных, место на доске Мощи, раздел `CHARGES`, `{r:…}` по пути правил.
- 91 - 0.16.3 - `RunStart.chestStock`, `EventReward.chestStock`, `ActiveMap.sources`, `AuctionLot.returned`, `WorkGains/WorkTally.missed`, `FountainRule.flasks`.
- 92 - 0.16.5 - `Received.overflowSold|autoSold` (`Sale`) вместо `sold/gold/shards`, `ProfessionView.layers` вместо `bonus`, `POST admin/fate?user=&reason=`, `GET admin/feedback/detail`.
- 93 - 0.16.6 - снят `GET admin/feedback/detail` (`ReportDetail`); `BugReportRequest|OwnReport|FullReport.topic` (улучшение администратора), `CrashTrace.count` на сервере (склейка падений отчёта).

## RULES_VERSION (`rules/.../content/Content.kt`)

- 85 - 0.14.0 - вещь не падает ниже требования, один множитель крита, силы уникалок не усиливаются.
- 86 - 0.14.0 - общие умения, каталог умений вдвое, вершины открытия 60/70/80/90, метка с подрывом.
- 87 - 0.14.1 - оборона классов строкой, своя кривая вождей Бездны, мягче бои Разлома.
- 88 - 0.14.2 - фонтан лечит больше, классы без строки получаемого урона, здоровье обычных монстров выше.
- 89 - 0.15.0 - мана умений по уровню умения, новые формулы и пределы боя, новые дерево и Атлас, яд и хаос, подгонка под цели баланса.
- 90 - 0.16.0 - щит по виду урона, резерв здоровья аурами, восстановление стражей под `bosses.sustainShare`, чистая зона карты.
- 91 - 0.16.2 - риск карты без опыта, доля стай Ваал-зоны, отдых вне боя, формы карт «Лабиринт» и «Арены».
- 92 - 0.16.3 - реген маны не ниже нуля, заряды фляг у фонтана, сундуки на старте захода, требования по семейству основы.
- 93 - 0.16.4 - темп боя вдвое: здоровье монстров ниже, удары выше, ярость и тотемы раньше.
- 94 - 0.16.6 - «Несгибаемый бастион» дороже (скорость атаки −12%), вечный бафф не вытесняется временным того же вида - действует сильнейший.

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
