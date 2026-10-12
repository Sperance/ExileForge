# Сетевой трафик: реестр API, цепочки запросов, избыточность

Источник: чтение кода клиента (`core/network/*Client.kt`, `Transport`, `CommandQueue`, `CommandRunner`, `HeroSync`, `presentation/*Actions`)
и сервера (`features/data/*Route.kt`, `Access.kt`, `HeroGate`), плюс измерения нагрузочного драйвера на настоящем `GameApi`
(`LOAD_TEST_REPORT.md`). Приложение на устройстве не запускалось - частоты экранов оценены по коду, частоты забега измерены драйвером.

## Транспорт клиента

- Один `OkHttpClient` (`ForgeHttp`): соединение 5 с, чтение 20 с, вызов 30 с; `retryOnConnectionFailure(false)`; HTTP-кэша нет.
  GET при обрыве соединения повторяется один раз (`ReadRetry`), POST - никогда на этом уровне.
- Каждый POST вошедшего аккаунта несёт `Idempotency-Key`: новая команда - новый UUID, повтор из очереди - тот же ключ (ключ
  хранится на диске, `CommandStore`; срок 1 ч, как на сервере). Порядок - FIFO; команды с собственным повтором
  (`OWN_RETRY`: сессия, журнал забега, с 4.8.x испытания, Предначертание, письмо), бросаемые (`ROLLED`) и админские идут мимо очереди.
- POST с `heroId` шлёт `X-Hero-Parts`; ответ несёт изменившиеся части героя - отдельного перечитывания после команды нет.
- Чтение героя `GET /hero/view` с `If-None-Match: "<версия>.<непрочитанные>"` - обычно 304; `CommandRunner.read` - одно чтение на
  ключ одновременно, окно свежести 30 с.
- WebSocket нет; периодического опроса героя, почты, заданий нет.

## Реестр API (сервер, `/api/v1`)

Обозначения: Auth P/S/T/M/A - публичный / вошедший / тестировщик / модератор / администратор; Lock H - очередь героя по
проверенному `heroId`, Svc - замок в сервисе; Tx tx - `transactionExecute`, rep - версионная запись (под ключом - транзакция),
cond - условная атомарная; Idem - POST с `Idempotency-Key`.

| Method | Path | Auth | Lock | Tx | Idem / заметки |
|---|---|---|---|---|---|
| GET | `/locale/*`, `/icons/*`, `/portraits/*`, `/content/{f}`, `/static/index.json`, `/system/health` | P | – | – | ETag/хэш в манифесте |
| POST | `/user/login`, `/user/byDeviceId`, `/user/login/byDeviceId` | P | – | tx | лимит частоты по адресу |
| GET | `/user/me` | S | – | – | |
| POST | `/user/logout`, `/user/changePassword`, `/user/bind` | S | – | cond | |
| POST | `/hero` (создание) | S | – | tx (версия User) | да |
| DELETE | `/hero?id=` | A | H | tx | |
| GET | `/hero/byUser` | S | – | – | `userId == caller` |
| GET | `/hero/view` | S | H | rep | **GET пишет** (`reconcile`, досчёт ремесла) |
| GET | `/hero/card`, `/stats`, `/uniques`, `/campaign/bosses`, `/stash`, `/trials/rift/board` | S | H | – | |
| POST | `/hero/grant/{gold,level,skillPoints,atlasPoints,zones,rares,map,profession,recipes,reset,experience,item,equipment}` | T (свой) / A | H | rep | да |
| POST | `/hero/{equip,unequip,socket,unsocket,sell,sell/batch,orb,seal,choose,unveil,chest/open,temper,essence,craft,uncraft,item/lock,title,autosell,...}` | S | H | rep | да |
| POST | `/hero/stash/{expand,claim,sell}`, `/hero/pets/*`, `/hero/skills/*`, `/hero/skilltree/*`, `/hero/atlas/*` | S | H | rep | да |
| GET/POST | `/hero/crafts`, `/hero/crafts/{start,stop}` | S | H | rep | GET пишет |
| POST | `/hero/merchant/{buy,buyOrb,trophy}` | S | H | rep | да; с 4.8.x отказ `CH_020` без места |
| GET/POST | `/hero/quests`, `/hero/quests/{claim,take,reset,abandon,law,claimAll}` | S | H | rep / tx | GET пишет |
| POST | `/hero/campaign/{start,events,abandon}` | S | H | rep / tx | `events` хранит отчёт (`KeepReport`) |
| POST | `/hero/trials/{rush,tower,key,events}`, `/hero/trials/rift/{start,act}` | S | H | rep | `events`, `act` хранят отчёт |
| GET/POST | `/hero/deletion/{preview,mark,restore,erase}` | S | Svc | tx | |
| GET | `/auctionlot/search`, `/mine`, `/price` | S | H | tx | GET пишет (истечение, сверка) |
| POST | `/auctionlot/sell/{equipment,item,pet}`, `/buy`, `/extend`, `/cancel` | S | H | tx | `buy`: продавец под версией, без замка |
| GET | `/guild/mine`, `/stash`, `/quests`, `/log`, `/search`, `/lookup` | S | H | tx (тихие) | GET пишет |
| POST | `/guild/*` (create, join, apply, join-code, invites, applications, invite, code, settings, stash/*, tree/*, leave, kick, promote, demote, transfer, disband, contribute, quests/claim) | S (по роли) | H (+Svc заявителя) | tx | да |
| GET | `/mail/inbox`, `/quota`, `/ignores` | S | H* | – | inbox удаляет истёкшие |
| POST | `/mail/{read,read/all,delete/read,delete,claim,letter,ignore,unignore,report}` | S | H | tx | да |
| GET/POST/DELETE | `/redemptioncodes`, `/redemptioncodes/redeem` | A / S | H | tx + cond | лимит 5/мин |
| POST/GET | `/bugreport`, `/bugreport/{suggestions,mine,delete,vote}` | P / S | – | tx | `suggestions` без предела выборки |
| GET | `/gameserver`, `/hall/table`, `/fate` | S | H для hall | – | |
| POST | `/fate/choose` | S | – | tx + cond | |
| GET/POST | `/notice`, `/notice/appeal` | P | – | tx | см. SEC-7 |
| GET/POST | `/admin/*`, `/admin/moderation/*` | M / A | Svc | tx | |

## Цепочки и частота (клиент)

| Действие | Запросы | Как получено |
|---|---|---|
| Холодный запуск | манифест (ETag) + `/user/me` + `/hero/byUser` + `/hero/view` (полный) + фоновые `locale/icons/portraits` только при смене хэша; GitHub `update.json` | код |
| Вход по устройству (новый игрок) | `byDeviceId`, `fate`, `fate/choose`, `POST /hero`, `hero/view` | измерено драйвером: 5 запросов, p95 33-79 мс |
| Открытие экрана | своё чтение раз в 30 с (задания, гильдия, аукцион); `hero/view` - обычно 304 | код |
| Забег вручную | `campaign/start`, затем `campaign/events` на каждый залп убийств (отправка через 120 мс после события, пачка до 6 событий, контрольные точки сразу, тикер 20 с), `abandon`/`claimAll` в конце | код; драйвер с пачками по 6: ≈4 `events` на заход |
| Возврат из фона | `system/health` (если ответ старше 10 с), манифест, GitHub `update.json` - **без минимального интервала**; > 30 с вне игры - ещё `hero/view` | код |
| Простой в меню | ≈0 запросов/мин (час - проверка обновления) | код |
| Без связи | проба здоровья 2/мин, и в фоне | код |

## Найденная избыточность и ошибки повтора

| ID | Приор. | Статус | Суть | Рекомендация |
|---|---|---|---|---|
| NET-1 | P2 | **ИСПРАВЛЕН** (клиент, `CommandQueue.OWN_RETRY`) | `trials/events` и `trials/rift/act` шли через общую очередь: `Transport` создавал копию команды со **своим** ключом, на проводе уходил ключ вызывающего; при обрыве копия ложилась в очередь, а `TrialActions`/`RiftActions` повторяли сами - сервер получал одну и ту же пачку под двумя ключами; пока очередь занята, каждый повтор (2-16 с) добавлял ещё копию. Регрессия: `CommandQueueRoutesTest` (до правки падал) | — |
| NET-2 | P3 | открыт | Каждый возврат из фона - 3-4 запроса (GitHub + манифест + health) без порога (`UpdateGate.kt:66`, `UpdateViewModel:180`) | порог 5-15 мин для проверки обновления |
| NET-3 | P3 | открыт, задумано | Любой не-2xx ответ, включая бизнес-отказ, поднимает перепроверку обновления (`Transport.alarm`, `ServerAlarm.FAILED`, не чаще 5 мин) | ограничить 5xx/426/битым ответом |
| NET-4 | P3 | открыт | В фоне не останавливаются: проба офлайна (30 с), повтор стартового манифеста (10 с), будильник ремесла на открытой вкладке | пауза в `away()` |
| NET-5 | P3 | открыт | Нет single-flight у `locale`/`icons`/`portraits`; `refreshLocale` в старте и в `signedIn` - второй отменяет первый посреди загрузки | общий замок, как у чанков контента |
| NET-6 | P3 | открыт | Вход в героя всегда тянет полного героя (`CharacterActions:117` - `forget()`), даже при копии на устройстве | засеять части из копии, как быстрый старт |
| NET-7 | P3 | открыт | После разгрузки очереди - всегда перечитывание героя и вкладки; `RiftActions.land` перечитывает героя, хотя ответ несёт снимок | по `readAt == 0` |
| NET-8 | P3 | открыт | 409 `IDEM_001` при разгрузке очереди показывается как «нет связи» | отдельное состояние «сервер занят» |
| NET-9 | P3 | открыт | Ручной бой: ≈1 `events` на залп убийств (до 10-40/мин). Каждый - замок героя, транзакция, запись героя (`$set` целых массивов) | см. `BACKEND_PERFORMANCE.md`, BP-3 |

## Оценка нагрузки

`RPS_total = Σ(active_players × запросов_на_игрока_в_секунду)`. Измерено драйвером (смешанный профиль: ¼ - заходы, ¾ - экраны
каждые 2-6 с; ускорение ×4) - ≈0.37 HTTP-запроса/с на активного игрока, из них ≈0.15/с `campaign/events` (ступень Load: 36.7 RPS / 100 игроков; на реальном темпе в soak - 0.43/с). Таблица по числу игроков,
измеренные ступени и экстраполяция (с пометкой) - в `LOAD_TEST_REPORT.md`. Операции MongoDB на запрос (по коду, `BACKEND_PERFORMANCE.md`):
команда героя ≈5-6 чтений и 4-6 записей, из них ≈4 ожидания `majority`.
