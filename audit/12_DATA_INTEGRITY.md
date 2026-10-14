# 12. Целостность данных: замки, транзакции, схемы (DATA)

Стенд и версии - см. `11_BACKEND_AND_API.md`. Вайп/схемы запускались только на своём mongod (`auditdb`, порт 27341).

## Сводка

| ID | Категория | P | Уверенность | Суть |
|---|---|---|---|---|
| DATA-01 | DATA | P2 | высокая (код) | Потеря коллекции `Schema` (восстановление без неё, ручная чистка) повторно применит все разрушительные `PARTS`/`PRUNES`/`TASKS`: обнуление находок, рекордов башни, перенумерация серийников |
| DATA-02 | DATA | P3 | высокая (код) | Вайп `Hero` сносит `Serial`, но не `Mail`: уникальные во вложениях писем сохраняют номера, счёт начинается заново - дубли номеров |
| DATA-03 | DATA | P3 | высокая (код) | Записи таблиц испытаний/Разлома (`TrialTables.record`) и статистика (`HeroStatsStore.bump`) пишутся после коммита героя, вне транзакции; сбой - рекорд потерян, повтор по ключу отвечает «исполнено» |
| DATA-04 | DATA | P3 | высокая (код) | Вайп `Hero` оставляет сирот: письма героев (`Mail.heroId`), санкции героев, отчёты |
| DATA-05 | DATA | P3 | средняя (код) | `BaseRepository.deleteById(id, session)` читает документ вне сессии транзакции |
| DATA-06 | DATA | P3 | высокая (код) | Серийный номер уникальной берётся из `Serial` до записи героя и вне её транзакции: неудачная запись (гонка версии) сжигает номер |
| DATA-07 | DATA | P3 | высокая (код) | Админское создание героя (`HeroService.create`, путь администратора): `Starter.grant` правит присланный объект внутри повторяемого тела транзакции - при повторе стартовый набор выдаётся дважды |
| DATA-08 | DATA | P3 | высокая (код) | `resetSchema` не атомарен: сносы, правки, заполнения и отметки версий - отдельными записями без транзакции |

## Замки и транзакции - карта (проверено кодом, ключевое - тестом)
- Замок героя ставится интерцептором (`installHeroLocks`) для любого запроса с пропущенным `heroId`; явные `HeroLocks.withLock` - при работе с другим героем: `acceptApplication` (заявитель), `LetterService.send`, `SelfDeletionService`, `RiftAwards.give`, фоновое истечение лотов, `GameServerService.move`, модерация.
- Без замка второго героя: `AuctionService.buy` (продавец), `GuildService.kick` (участник), `GuildChange.maintain/handOver` (герои состава), `FateService.choose`/`patchFate`, `GuildService.disband`/`patchGuild` (все герои гильдии; версия поднимается `$inc`). Порча данных исключена версией документа, но страдает вторая сторона (SEC-04: воспроизведено 22 из 30 гонок с отказом).
- Транзакции: многодокументные команды аукциона, гильдии, почты, промокода, создания героя - в `transactionExecute`; одиночная запись героя - `replace` по версии (без ключа - без транзакции, атомарна сама), с ключом или изменённым забегом - транзакция.
- Повторяемость тела (`RULES: тело transactionExecute повторяемо`): во всех просмотренных сервисах объекты меняются до транзакции, в теле - только запись; откат возвращает версию (`bumped/onRollback`). Исключение - DATA-07. Документы, читаемые внутри тела, читаются с `session` (SelfDeletion, Moderation, HeroService.create), кроме DATA-05.
- Несколько героев одной операцией: двойной выдачи нет (воспроизведено): письмо аккаунта двумя героями (20/20 - один успех), лот двумя покупателями (12/12 - один успех), один ключ на шести запросах (1 исполнение).

## [DATA-01] Повторное применение разрушительных правок при отсутствии `Schema`
- Файлы: `config/DatabaseSeeder.kt:100-131`, `config/CollectionSchemas.kt` (`PARTS`, `PRUNES`, `TASKS`).
- Ожидание (KDoc): «коллекция без записи только записывается - первый старт на старой базе ничего не сносит».
- Факт: для `ALL` так и есть, но для частей/срезов/заполнений/миграций отсутствие записи = версия 0 → применяются. На базе с данными без `Schema` снова выполнятся: `Hero.uniques = {}` (история находок), `towerBest = 0`, `quests.offers = []`, удаление записей башни, `task.globalSerials` (перенумерация серийников), `task.retiredItems`.
- Когда: восстановление бэкапа без `Schema`, ручная очистка, смена `MONGO_DB` на копию без неё. `MongoBackupManager` выгружает все коллекции (`listCollectionNames`) - обычный бэкап её содержит.
- Варианты: (1) если `Schema` пуста, а `Hero`/`User` не пусты - отказ старта; (2) при пустой `Schema` записывать текущие версии без применения.
- Статус: код, не воспроизводилось.

## [DATA-02] Серийники после вайпа героев
- `CollectionSchemas.ALL`: `Hero.wipes` содержит `Serial`, но не `Mail` (письма - аккаунта, сносятся только вайпом `User`). Возвраты лотов и письма администратора несут `instances` с `serial`. После вайпа `Hero` новые находки получат те же номера. Варианты: добавить `Mail` в `Hero.wipes` или fixup, снимающий `serial` во вложениях.

## [DATA-03] Побочные записи вне транзакции команды
- `campaign/TrialService.kt:155-159`, `RiftService.kt:169-170` - `tables.record` после `heroes.save`; `HeroRepository.save:396-400` → `HeroStatsStore.bump` (ошибка только в лог, `tally.clear()` до записи).
- При команде с ключом: метка исполнения уже закоммичена с героем; сбой записи таблицы → 5xx → повтор получает «исполнено» без отчёта, рекорд не ляжет.
- Варианты: писать запись таблицы в той же транзакции (`beforeCommit`), или идемпотентная досылка рекорда из героя на следующей команде.

## [DATA-04] Сироты при вайпе героев
- После вайпа `Hero` остаются `Mail` с `heroId` (письма героев, видимые «только адресату»), `Sanction` целей-героев, `ModerationEntry`, `BugReport`. Последствия - мусор в окне модерации и почте; на альфе, по RULES, допустимо - вопрос владельцу.

## [DATA-05] Чтение вне сессии внутри транзакции
- `base/repository/BaseRepository.kt:367` `deleteById(id, session) = delete(findById(id), session)` - `findById` без `session`. Используется, напр., `LetterService.send:90` (вытеснение письма). Удаление всё равно по версии, поэтому порчи нет, но чтение не видит записей своей транзакции. Вариант: `findById(id, session)`.

## [DATA-06] Сожжённые серийные номера
- `HeroRepository.settle → numberUniques` берёт `Serials.next` (отдельная запись) до записи героя. `BRY_002` или откат - номер потерян, в счёте пропуск. Влияет только на «№ копии»; P3.

## [DATA-07] Двойной стартовый набор у героя, созданного администратором
- `features/logic/hero/HeroService.kt:79-111`: у игрока объект героя создаётся в теле заново, у администратора используется присланный `source`, и `Starter.grant(hero, …)` выполняется на каждом повторе тела (`transactionExecute` повторяет при конфликте записи). Конфликт возможен на `User` (`users.update(owner)`). Вариант: копия `source` в начале тела.

## [DATA-08] Неатомарный автовайп
- `resetSchema`: `drop` → fixups → parts → prunes → fills → отметки версий, всё без транзакции (drop в транзакции невозможен). Повторный старт после обрыва повторит сносы (идемпотентно), но части без отметки применятся повторно (`$set`-правки идемпотентны, `pull` - тоже). Реального вреда в нынешнем наборе не видно; риск для будущих неидемпотентных `PartSchema`. Рекомендация: правило в RULES - части обязаны быть идемпотентны.

## CollectionSchemas - что снесётся
- Поднятие `User` → User, AuthSession, Hero, Mail, UserDevice, Sanction, AppealToken, HeroRun, HeroStats, AuctionLot, Guild, GuildEvent, Serial, IdempotentReply, TrialRecord, MailStamp, MailIgnore, MailCounter.
- `Hero` → Hero + HeroRun, HeroStats, AuctionLot, Guild, GuildEvent, Serial, IdempotentReply, TrialRecord, MailStamp, MailIgnore, MailCounter; fixup `User.countCharacters = 0`.
- `Guild` → Guild, GuildEvent; fixup `Hero.guild` unset. `Sanction` → + AppealToken. `RedemptionCodes` → fixup `User.redeemedCodes = []`. `GameServer` → fixup `server = DEFAULT` у Hero/AuctionLot/Guild.
- Не ссылаются ни на что: AuthSession, HeroRun, HeroStats, AuctionLot, GuildEvent, Mail, MailStamp, MailCounter, MailIgnore, BugReport, AppealToken, UserDevice, ModerationEntry, Serial, IdempotentReply, RouteTiming, BackupLock, TrialRecord, RiftAward.
- Все коллекции кода покрыты записями `ALL` (сверено grep по репозиториям).
- FILLS: `Hero.number`, `Guild.code` (короткие коды, `fill` без транзакции, на старте до приёма запросов), `Hero.uniquesFound` (KEY_COUNT). TASKS: `task.globalSerials`, `task.retiredItems` - транзакция с отметкой версии.
- Уникальные индексы: User (email, login, loginKey, device_id, hardware_id - частичные), AuthSession.tokenHash, Hero (name, nameKey, number), Guild (nameKey, tag, code), GameServer.code, RedemptionCodes.code, Sanction.number, UserDevice(userId, hardware), MailIgnore(heroId, targetId), Mail.once, BugReport.key. Нет уникальности: член гильдии (`members.heroId`) - держится замком героя; TrialRecord на (доска, период, герой) - пишется под замком героя.
- На своём стенде автовайп отработал с пустой базой (первый старт: 2 пользователя и промокод сидера), сносов не было.

## Проверенные файлы
`config/{CollectionSchemas,DatabaseSeeder,MongoFactory,TransactionHooks,MongoErrors}.kt`, `base/repository/{BaseRepository,Versioning}.kt`, `features/data/hero/{HeroRepository,HeroRunStore}.kt`, `HeroStatsStore`, `IdempotentReplyStore`, сервисы из `11_BACKEND_AND_API.md`, `TrialService`, `RiftService`, `TrialTables`, `HeroService.create/erase`.

## Запущено
- Стенд (mongod replSet + сервер копии), сценарии `run/race_mail.py 20` (20/20: один успех, второй `BRY_002`, золото +20000 = ожидание), `run/race_auction.py 12` (12/12 один успех, сфер +120 = ожидание), `run/race_seller.py 30` (6 отказов продавцу, 16 покупателю, 8 оба прошли), `run/race_idem.py` (1 исполнение из 6).
- Скрипты и вывод: `/home/user/audit/work/api/run/`.

## Не проверено
- DATA-01, 02, 03, 05, 06, 07, 08 - только код, интеграционно не воспроизводились.
- Гонки гильдии (вместимость, склад) и истечение лотов параллельно с покупкой - код.
