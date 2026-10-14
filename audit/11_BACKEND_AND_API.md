# 11. Бэкенд и API: маршруты, доступ, идемпотентность (SEC)

Источник: копия сервера `ktor-bestgame` @ 2619f0f в `/home/user/audit/work/api` (исходники не менялись), клиент ExileForge @ e304983f (только чтение).
Стенд: свой mongod 7.0.14 (replSet `rsapi`, порт 27341, dbpath `work/api/.mongo/db`), сервер из копии (`installDist`, порт 18341, база `auditdb`). После работы оба процесса остановлены. Production не трогался.

## Сводка находок

| ID | Категория | P | Уверенность | Суть |
|---|---|---|---|---|
| SEC-01 | SECURITY | P1 | высокая (воспроизведено) | Отпечаток устройства (`hardwareId`) - бессрочный ключ к аккаунту: `POST user/byDeviceId` с известным отпечатком выдаёт сессию существующего аккаунта, в т.ч. с привязанным логином/паролем, и гасит секрет устройства владельца |
| SEC-02 | SECURITY | P2 | высокая (воспроизведено) | Публичный `GET /api/v1/notice?id=` без входа отдаёт внутренний комментарий модератора, логин наказавшего (`byLogin`, в т.ч. `admin`) и метку цели |
| SEC-03 | ECONOMY/SECURITY | P2 | высокая (воспроизведено) | Выдачи тестировщика/модератора стопками (сферы, `grant/item`) не помечаются выдачей и уходят в экономику через аукцион; модератор = тестировщик (`Caller.isTester`) |
| SEC-04 | BUG/ARCHITECTURE | P2 | высокая (воспроизведено) | Мутация чужого героя без его замка: продавец в `auctionlot/buy`, участник в `guild/kick` и др.; конфликт гасит команду другой стороны отказом `BRY_002` (HTTP 400), клиент его не повторяет |
| SEC-05 | DESIGN | P3 | высокая (код) | «GET ничего не пишет» нарушается слоем доступа: `HeroPresence.touch`, продление сессии, `revokeAll`/выдача токена апелляции на любом запросе забаненного |
| SEC-06 | SECURITY | P3 | средняя (код) | Лимиты по адресу берут первый `X-Forwarded-For` от доверенного прокси - при прокси, дописывающем заголовок, адрес подделывается; конфиг прокси в репозитории отсутствует (не проверено) |
| SEC-07 | SECURITY | P3 | высокая (проба) | Отказ `AUTH_004` отличает «чужой герой» от «нет такого» (оракул существования id) |
| SEC-08 | DESIGN | P3 | высокая (код) | Окно «в работе» ключа идемпотентности - память процесса, брошенная метка свободна через 2 мин; KDoc `installIdempotency` говорит «хранится сутки», фактический TTL - 1 ч |
| SEC-09 | SECURITY | P3 | высокая (проба) | `/static/index.json` без входа перечисляет все 181 маршрут, включая админские и модерацию |

Что проверено и не дало находок - в конце файла.

## Таблица маршрутов

Сгенерирована по живому манифесту `/static/index.json` (181 маршрут). «Доступ» - результат реальной пробы трёх запросов (без токена / игрок / администратор). «Герой/IDOR» - проба с героем чужого аккаунта в `heroId`: все 132 маршрута, где он есть, ответили `403 AUTH_004`, 16 тестерских/админских - `AUTH_003`. Замок героя (`installHeroLocks`) ставится автоматически на любой запрос с пропущенным `heroId`; идемпотентность - на любой POST вошедшего аккаунта с заголовком `Idempotency-Key`. Клиент: все мутации - `Transport.request` с ключом (`CommandQueue`), кроме исключений `CommandQueue.OWN_RETRY/ROLLED/READS/ADMIN` (журналы захода, испытаний и Разлома повторяются со своим ключом; ролльные команды не встают в офлайн-очередь).

| Метод | Путь | Доступ (проба) | Герой/IDOR | Замок/транзакция/ключ | Лимит | Игрок без параметров |
|---|---|---|---|---|---|---|
| GET | `/api/v1/admin/feedback` | ADMIN/MOD/TESTER |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/admin/feedback/asana` | ADMIN/MOD/TESTER |  | Idem-Key | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/admin/feedback/status` | ADMIN/MOD/TESTER |  | Idem-Key | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/admin/hero/server` | ADMIN/MOD/TESTER |  | Idem-Key | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/admin/mail` | ADMIN/MOD/TESTER |  | Idem-Key | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/admin/moderation/ban` | ADMIN/MOD/TESTER |  | Idem-Key | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/admin/moderation/delete` | ADMIN/MOD/TESTER |  | Idem-Key | global 600/мин | 403:AUTH_003 |
| GET | `/api/v1/admin/moderation/dossier` | ADMIN/MOD/TESTER |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/admin/moderation/lift` | ADMIN/MOD/TESTER |  | Idem-Key | global 600/мин | 403:AUTH_003 |
| GET | `/api/v1/admin/moderation/log` | ADMIN/MOD/TESTER |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/admin/moderation/role` | ADMIN/MOD/TESTER |  | Idem-Key | global 600/мин | 403:AUTH_003 |
| GET | `/api/v1/admin/moderation/rows` | ADMIN/MOD/TESTER |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/admin/servers` | ADMIN/MOD/TESTER |  | Idem-Key | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/admin/testers` | ADMIN/MOD/TESTER |  | Idem-Key | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/auctionlot/buy` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; явная tx (продавец при buy без замка) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/auctionlot/cancel` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; явная tx (продавец при buy без замка) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/auctionlot/extend` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; явная tx (продавец при buy без замка) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/auctionlot/mine` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/auctionlot/price` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/auctionlot/search` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/auctionlot/sell/equipment` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; явная tx (продавец при buy без замка) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/auctionlot/sell/item` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; явная tx (продавец при buy без замка) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/bugreport` | PUBLIC |  | Idem-Key | bug 100/ч | 400:SP_100 |
| POST | `/api/v1/bugreport/delete` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/bugreport/mine` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 200:ok |
| GET | `/api/v1/bugreport/suggestions` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 200:ok |
| POST | `/api/v1/bugreport/vote` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/fate` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 200:ok |
| POST | `/api/v1/fate/choose` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/gameserver` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 200:ok |
| POST | `/api/v1/guild/applications/accept` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/applications/decline` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/apply` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/code` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/contribute` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/create` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/demote` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/disband` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/invite` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/invites/accept` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/invites/decline` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/join` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/join-code` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | guildCode 5/мин | 400:BRT_003 |
| POST | `/api/v1/guild/kick` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/leave` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/guild/log` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/guild/lookup` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/guild/mine` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/promote` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/guild/quests` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/quests/claim` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/quests/refresh` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/guild/search` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/settings` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/guild/stash` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/stash/deposit` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/stash/tab` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/stash/take` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/transfer` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/tree/reset` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/tree/take` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/guild/visit` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx GuildChange (версия гильдии и героев) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/hall/table` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero` | SIGNED_IN |  | Idem-Key | global 600/мин | 400:BRT_001 |
| DELETE | `/api/v1/hero` | ADMIN/MOD/TESTER |  |  | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/atlas/allocate` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/atlas/refund` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/atlas/reset` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/autosell` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/autosell/unwearable` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/hero/byUser` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/campaign/abandon` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/hero/campaign/bosses` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/campaign/events` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/campaign/start` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/hero/card` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/chest/open` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/choose` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/craft` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/hero/crafts` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/crafts/settle` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/crafts/start` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/crafts/stop` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/deletion/erase` | SIGNED_IN (не пробовался) | hero=: владелец в сервисе, явный withLock | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | SKIP |
| POST | `/api/v1/hero/deletion/mark` | SIGNED_IN | hero=: владелец в сервисе, явный withLock | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/hero/deletion/preview` | SIGNED_IN | hero=: владелец в сервисе, явный withLock | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/deletion/restore` | SIGNED_IN | hero=: владелец в сервисе, явный withLock | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/equip` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/essence` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/grant/atlasPoints` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/grant/equipment` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/grant/experience` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/grant/gold` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/grant/item` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/grant/level` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/grant/map` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/grant/profession` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/grant/rares` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/grant/recipes` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/grant/reset` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/grant/skillPoints` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/grant/zones` | ADMIN/MOD/TESTER |  | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/hero/item/lock` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/merchant/buy` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/merchant/buyOrb` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/merchant/trophy` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/orb` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/hero/quests` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/quests/abandon` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/quests/claim` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/quests/claimAll` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/quests/law` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/quests/refresh` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/quests/reset` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/quests/take` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/seal` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/sell` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/sell/batch` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/skills/exchange` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/skills/flask` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/skills/learn` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/skills/rune` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/skills/slot` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/skilltree/allocate` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/skilltree/rechoose` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/skilltree/refund` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/skilltree/refundBranch` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/skilltree/reset` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/socket` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/hero/stash` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/stash/claim` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/stash/expand` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/stash/sell` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/hero/stats` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/temper` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/title` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/trials/events` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/trials/key` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/trials/rift/act` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/hero/trials/rift/board` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/trials/rift/start` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/trials/rush` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/trials/tower` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/uncraft` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/unequip` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/hero/uniques` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/unsocket` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/hero/unveil` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; repo.save (1 док по версии; tx при ключе/забеге) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/hero/view` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/mail/claim` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key; tx герой+письмо | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/mail/delete` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/mail/delete/read` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key | global 600/мин | 200:ok |
| POST | `/api/v1/mail/ignore` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/mail/ignores` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/mail/inbox` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 200:ok |
| POST | `/api/v1/mail/letter` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/mail/quota` | SIGNED_IN | heroId: HeroGate + HeroLocks | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/mail/read` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/mail/read/all` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key | global 600/мин | 200:ok |
| POST | `/api/v1/mail/report` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key | bug 100/ч | 400:BRT_003 |
| POST | `/api/v1/mail/unignore` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/notice` | PUBLIC |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 400:BRT_003 |
| POST | `/api/v1/notice/appeal` | PUBLIC |  | Idem-Key | bug 100/ч | 400:SP_100 |
| POST | `/api/v1/notice/token` | SIGNED_IN |  | Idem-Key | global 600/мин | 400:BRT_003 |
| GET | `/api/v1/redemptioncodes` | ADMIN/MOD/TESTER |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/redemptioncodes` | ADMIN/MOD/TESTER |  | Idem-Key | global 600/мин | 403:AUTH_003 |
| DELETE | `/api/v1/redemptioncodes` | ADMIN/MOD/TESTER |  |  | global 600/мин | 403:AUTH_003 |
| POST | `/api/v1/redemptioncodes/redeem` | SIGNED_IN | heroId: HeroGate + HeroLocks | Idem-Key | redeem 5/мин | 400:BRT_003 |
| POST | `/api/v1/user/bind` | SIGNED_IN |  | Idem-Key | password 5/мин | 400:SP_100 |
| POST | `/api/v1/user/byDeviceId` | PUBLIC |  |  | login 10/мин/IP | 400:SP_100 |
| POST | `/api/v1/user/changePassword` | SIGNED_IN |  | Idem-Key | password 5/мин | 400:SP_100 |
| POST | `/api/v1/user/login` | PUBLIC |  |  | login 10/мин/IP | 400:SP_100 |
| POST | `/api/v1/user/login/byDeviceId` | PUBLIC |  |  | device 60/мин/IP | 400:SP_100 |
| POST | `/api/v1/user/logout` | SIGNED_IN (не пробовался) |  | Idem-Key | global 600/мин | SKIP |
| GET | `/api/v1/user/me` | SIGNED_IN |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 200:ok |
| GET | `/content/{file}` | SIGNED_IN (не пробовался) |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | SKIP |
| GET | `/icons/icons.json` | PUBLIC |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 200:ok |
| GET | `/locale/{language}.json` | SIGNED_IN (не пробовался) |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | SKIP |
| GET | `/portraits/all.json` | PUBLIC |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 200:ok |
| GET | `/static/index.json` | PUBLIC |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 200:ok |
| GET | `/system/health` | PUBLIC |  | GET (игровых записей нет; доступ пишет touch/сессию) | global 600/мин | 200:ok |

Общий CRUD (`BaseRoute`): `hero` - CREATE (игроку, поля кроме имени/описания/класса игнорируются, `HeroService.create`) и DELETE (только админ); `gameserver` - READ; `redemptioncodes` - READ/CREATE/DELETE (только админ). Общий `PUT` не открыт ни у одной коллекции.

## [SEC-01] Отпечаток устройства - бессрочный ключ к аккаунту
- Категория: SECURITY. Приоритет: P1. Уверенность: высокая. Подсистема: вход.
- Файлы: `features/logic/auth/UserService.kt:54-70` (`createByDevice`), `features/data/user/UserRoute.kt:37-41`; клиент `app/.../data/settings/ServerStore.kt:305-314` (`deviceFingerprint` - хеш ANDROID_ID).
- Ожидание: секрет устройства «выдаёт сервер, в базе - только хеш» (KDoc), то есть войти без него нельзя.
- Факт: если аккаунт с хешем отпечатка уже есть, `createByDevice` выдаёт ему новый секрет и сессию. Отпечаток - не секрет сервера, а стабильный идентификатор устройства, приходящий от клиента; ротации нет. Касается и аккаунтов с привязанным логином/паролем (`bind` хранит `hardware_id`) и любого аккаунта, получившего `hardware_id` через вход по устройству (в т.ч. если ему позже выдали роль модератора).
- Доказательство (стенд): гость привязал логин `victimlogin` → `POST user/byDeviceId` с тем же отпечатком из чистой сессии вернул `200`, тот же `user.id`, `login=victimlogin`, список его героев; прежний секрет владельца после этого: `400 US_015 Device is not known`.
- Последствия: захват аккаунта при единичной утечке отпечатка; владелец теряет вход по устройству. Масштаб: все аккаунты с `hardware_id`.
- Варианты: (1) для известного отпечатка не выдавать сессию - только «аккаунт есть, войдите секретом/паролем»; (2) выдавать новый секрет только гостю без логина и только после подтверждения старым секретом; (3) привязать восстановление к Play Integrity/подписанной аттестации.
- Побочные эффекты: переустановка приложения у гостя без логина потеряет аккаунт без пути восстановления (вариант 1) - нужен UX «привяжите логин».
- Регрессионный тест: интеграционный - известный отпечаток не даёт сессии аккаунта с логином.
- Вопрос владельцу: допустима ли потеря гостевого аккаунта при переустановке ради закрытия захвата?
- Статус: воспроизведено.

## [SEC-02] Публичный экран санкции раскрывает служебные поля
- Категория: SECURITY. P2. Уверенность: высокая. Файлы: `features/data/moderation/ModerationRoute.kt` (`GET notice`), `features/logic/moderation/NoticeService.kt:27`, `features/data/moderation/Sanction.kt:101-122`, `AccessPolicy.kt` (`NOTICE` - PUBLIC).
- Ожидание (RULES «Модерация»): знать id санкции мало для апелляции; служебное - модераторам.
- Факт: без токена `GET /api/v1/notice?id=` отдаёт `comment` (внутренняя заметка), `byLogin` (логин наказавшего, напр. `admin`), `byRole`, `label` (логин/имя цели), `number` (сквозной счётчик банов). Id - ObjectId из одного процесса (общий 5-байтовый префикс, растущий счётчик), т.е. не случайный.
- Доказательство: бан администратором с комментарием → анонимный GET вернул `"comment": "internal note: ...", "byLogin": "admin", "byRole": "ADMIN"`.
- Варианты: (1) отдельный `NoticeView` без `comment/byLogin/liftedBy`; (2) требовать токен апелляции/вход для подробностей; (3) случайный публичный id вместо `_id`.
- Вопрос владельцу: должен ли наказанный видеть комментарий модератора (тогда - отдельное поле «причина для игрока»)?
- Статус: воспроизведено.

## [SEC-03] Выдачи стопками уходят в экономику
- Категория: ECONOMY/SECURITY. P2. Уверенность: высокая. Файлы: `features/logic/hero/TesterGrants.kt:54-58` (`stack` без пометки), `features/logic/auth/Caller.kt:23` (`isTester` включает модератора), `AccessPolicy.kt:74`, `features/logic/trade/AuctionService.kt:136-151` (`sellItem` без проверки выдачи).
- Ожидание: «выдача … помечена (`granted`) и другим героям не уходит» (KDoc TesterGrants, SEC-5).
- Факт: вещи помечаются, стопки и золото - нет; лимит `moderation.grantStack` - на вызов, не на сутки. Модератор проходит `Need.TESTER`.
- Доказательство: тестировщик получил 3×1000 `CHAOS_ORB` тремя вызовами, выставил лоты, обычные герои купили - 120 сфер выдачи у игроков (`race_auction.py`).
- Варианты: (1) запрет аукциона/склада/почты для ролей-тестировщиков (флаг на герое); (2) учёт «выданного» в сумке отдельной картой, которая не продаётся; (3) выдачи только на отдельном тестовом игровом сервере (`server`), не пересекающемся с игроками.
- Вопрос владельцу: должны ли модераторы иметь выдачи вообще; живут ли тестировщики на общем игровом сервере?
- Статус: воспроизведено.

## [SEC-04] Мутация чужого героя без его замка
- Категория: BUG/ARCHITECTURE. P2. Уверенность: высокая. Файлы: `features/logic/trade/AuctionService.kt:157-186` (`buy`: `seller` читается и пишется под замком покупателя), `features/logic/guild/GuildService.kt:262-269` (`kick` - участник), `GuildChange.write` (`touched` герои), `GuildService.acceptApplication:152` (здесь замок есть - непоследовательно).
- Ожидание (RULES «Архитектура»): «Любая мутация героя - внутри `HeroLocks.withLock(heroId)` и `transactionExecute`».
- Факт: целостность держит оптимистичная версия, но проигравший получает `BRY_002` (HTTP 400). Сервер считает его временным (ключ освобождает), клиент - окончательным (`CommandQueue.transient` = 409/429/502-504), команда пропадает с ошибкой.
- Доказательство (30 гонок «покупка лота ∥ команда продавца»): 6 - упала команда продавца `BRY_002`, 16 - упала покупка `BRY_002`, 8 - обе прошли. Гонка двух покупателей одного лота (12) и двух героев одного аккаунта за одно письмо (20): ровно один успех, второй `BRY_002`, двойной выдачи нет.
- Варианты: (1) брать замки обоих героев в фиксированном порядке (id) - `HeroLocks.withLocks(a, b)`; (2) писать продавцу не документ, а `$inc` выручки/письмо (продавец не читается); (3) отдавать `BRY_002` статусом 409 - клиент повторит.
- Побочные эффекты: (1) - риск взаимоблокировки без порядка; (3) - повтор поверх ключа требует, чтобы ключ освобождался (так и есть).
- Регрессионный тест: гонка buy ∥ команда продавца - ни одного отказа у продавца.
- Статус: воспроизведено.

## [SEC-05] Записи на GET в слое доступа
- DESIGN, P3, код: `server/addons/Access.kt` (`presence.seen` → `HeroRepository.touch`, раз в 5 мин на героя; `sanctions…revokeAll`, `appeals.issue` - перезапись токена апелляции на каждый запрос забаненного), `AuthSessionRepository.resolve` (продление/удаление сессии). Игрового состояния не трогает; RULES формулирует запрет без исключений. Вопрос владельцу: признать служебные записи исключением в RULES или вынести их из GET.

## [SEC-06] Адрес из `X-Forwarded-For`
- SECURITY, P3, средняя: `server/addons/HTTP.kt:20-29` берёт первый элемент XFF от доверенного прокси. Если прокси дописывает (`$proxy_add_x_forwarded_for`), первый элемент задаёт клиент - лимиты входа (`LOGIN_LIMIT` по адресу) обходятся. Конфиг прокси/`TRUSTED_PROXIES` в репозитории нет - не проверено. Варианты: брать последний недоверенный элемент; только `X-Real-IP`, выставляемый прокси.

## [SEC-07] Оракул существования героя
- P3: `HeroGate.admit` - чужой id → `AUTH_004 "Character … belongs to another account"`, несуществующий → маршрут отвечает «нет героя». KDoc HeroGate утверждает обратное намерение. Id героев и так видны в карточках, поэтому низкий.

## [SEC-08] Идемпотентность: детали
- Хранение: `IdempotentReply`, `_id = "<userId>:<key>"`, поле `request` = SHA-256(метод, путь, отсортированные query, тело); TTL-индекс `createdAt` 1 ч (`Idempotency.TTL_HOURS`); клиент выбрасывает команду из очереди через 1 ч от её создания (`CommandQueue.TTL_MS`) - согласовано (часы клиента; перевод часов назад на устройстве продлит жизнь ключа на клиенте, код).
- Область - аккаунт: тот же ключ другим аккаунтом исполняется независимо (проверено), другим героем того же аккаунта - `422 IDEM_003` (герой входит в отпечаток).
- Тот же ключ, другое тело: `422 IDEM_003` (проверено).
- Гонка одного ключа: 6 одновременных запросов на одного героя - 1 исполнение, 5 повторов `Idempotent-Replay: true`, золото списано один раз (проверено); один ключ на двух героях одновременно - `409 IDEM_001` второму (проверено).
- Метка «исполнено» коммитится в транзакции команды (`CommandKey.commit`), одиночная запись героя под ключом идёт транзакцией (`BaseRepository.replace`).
- Слабые места (код): «в работе» - `ConcurrentHashMap` процесса, через `STALE_MS = 2 мин` метка считается брошенной (долгая команда допускает дубль; RULES признаёт одну копию сервера); ключ необязателен - POST без заголовка не защищён; повтор успешной команды без `KeepReport` отвечает `data:null`; KDoc «хранится сутки» не совпадает с TTL 1 ч.

## [SEC-09] Манифест маршрутов публичен
- P3: `/static/index.json` без входа отдаёт все 181 путь (админка, модерация, выдачи). Клиенту нужен список для экранов; можно отдавать маршруты по роли после входа.

## Проверено, находок нет
- Политика доступа (`AccessPolicy`) на живом сервере: 167 маршрутов требуют токен, 6 публичных GET-статик + 5 публичных POST (вход, отчёт, апелляция) - совпадает с кодом и `AuthTest`.
- IDOR по `heroId`: 148 маршрутов, все отказали; `hero/deletion?hero=`, `hero/byUser?userId=`, `hero?id=` - отказали; `hero/card?target=` - чужая карточка по замыслу (без тайника).
- `ModerationPolicy`: `ban/lift/role/delete` все проходят через таблицу; бан устройства учитывает сильнейшую роль на устройстве.
- Пароли PBKDF2-SHA256 210k, токены 256 бит, в базе SHA-256; подставной хеш против перебора логинов по времени; кеш сессии 60 с сбрасывается при выходе/смене пароля.
- Промокод: условные записи аккаунта и кода в одной транзакции (код).

## Проверенные файлы
`base/route/*`, `server/addons/{Access,Idempotency,RateLimit,HTTP,Routing}.kt`, `features/logic/auth/*`, `features/logic/hero/{HeroLocks,HeroSnapshots,HeroPresence}.kt`, все `features/data/*/*Route.kt`, `AuctionService`, `GuildService/GuildAccess/GuildStashService`, `MailService`, `LetterService`, `RedemptionService`, `ModerationService/Policy`, `NoticeService`, `UserService`, `AuthSessionRepository`; клиент `core/network/{CommandQueue,Transport,CampaignClient}.kt`, `ServerStore.deviceFingerprint`.

## Запущено
- `./gradlew --offline installDist` (копия) - успешно; сервер поднят на своём mongod.
- `run/probe.py` - доступ 181 маршрута ×3 ролей; `run/idor.py` - чужой `heroId` на всех маршрутах.
- `run/race_mail.py 20`, `run/race_auction.py 12`, `run/race_seller.py 30`, `run/race_idem.py` - результаты выше.
- Сценарий SEC-01 и SEC-02 - вручную через `run/drv.py`.

## Не проверено
- Конфиг прокси/VPS (SEC-06), реальный `TRUSTED_PROXIES`.
- Гонки гильдии (вместимость, двойное вступление) - только код: держатся замком героя и версией гильдии.
- Поведение при смене primary/обрыве коммита (`commitWithRetry`) - не моделировалось.
