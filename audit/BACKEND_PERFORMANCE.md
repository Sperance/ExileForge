# Ktor и MongoDB: производительность, транзакции, ресурсы

Измерения - локальный сервер ветки (Netty, JDK 21, `-Xmx1g`, G1) и одиночный replica set MongoDB 7.0.14 на той же машине
(4 ядра, 15 ГБ). Цифры под нагрузкой - `LOAD_TEST_REPORT.md`. Production не измерялся; одна машина для сервера, базы и генератора
нагрузки - абсолютные значения задержек занижены (нет сети), относительные - показательны.

## Горячий путь команды героя (по коду, подтверждено чтением)

1. `IdempotentReply.insertOne` (ключ «в работе»).
2. `requireHero`: полный документ героя (без проекции) + `HeroRun`.
3. Запись: под `CommandKey` `replace()` идёт транзакцией - `updateOne` героя с `$set` **каждого изменившегося поля верхнего
   уровня целиком** (`EntityCodec.changes`: одна вещь → весь массив `items`, один счётчик → вся карта `counters`), `HeroRun`,
   метка исполнения; коммит `majority`.
4. `HeroStats` - upsert `$inc/$max` вне транзакции.
5. Снимок ответа читает героя **ещё раз** (+ `HeroRun`), может сохранить сверку и досчёт ремесла; `countDocuments` почты.
6. `IdempotentReply.complete`.

Итого на команду ≈5-6 чтений и 4-6 записей, ≈4 ожидания `majority`. Средний документ героя на свежей базе - 9.7 КБ
(`collstats` после нагрузки); у развитого героя растёт с `items` (до 1000 мест + 100 переполнения) и `uniques[].serials` (без предела).

## Найдено

| ID | Приор. | Статус | Где | Суть / доказательство |
|---|---|---|---|---|
| BP-1 | P2 | **ИСПРАВЛЕН** | `HeroRepository.patchGuild` (`GuildAccess.kt:71`, `GuildTreeService.kt:99`, `GuildService.kt:301`) | Правка всех героев гильдии фильтром `guild.id` внутри транзакции без индекса: `explain()` на тестовой базе - `COLLSCAN`, `docsExamined` = все герои. Добавлен индекс `guild.id` (`IndexSpec.on`, создаётся на старте `ensureIndexes`, схема и вайп не нужны) |
| BP-2 | P2 | открыт | `HeroRepository.professionTop/Size/Place` из `HallSources.kt:59-62` | Доска профессии - три запроса по `server` + `professions.<код>.*` без индекса, без кэша: `explain()` - `COLLSCAN` + сортировка в памяти, `docsExamined` = все герои, на каждый просмотр. Рекомендация: кэш доски на минуты (как `bosses`) - индексы на 7 профессий дороже записи героя |
| BP-3 | P2 | открыт | `EntityCodec.kt:44`, `HeroSnapshots.kt:136-138` | Усиление записи: `$set` целых массивов/карт верхнего уровня на каждую команду (журнал забега - на каждый залп), повторное чтение героя для снимка, 3 записи `IdempotentReply`. Рекомендация: `$set` по элементам (`items.$[id]`) для частых команд, снимок из записанного объекта |
| BP-4 | P2 | открыт | `MailService.kt:84-90` | Рассылка администратора - одна транзакция на N вставок + `find(isActive)` без индекса: упрётся в 60 с `transactionLifetimeLimitSeconds` при росте игроков |
| BP-5 | P2 | открыт | `AuctionLotRepository.kt:25`, `AuctionService.kt:98, 232` | Закрытые лоты не удаляются никогда; `mine` отдаёт всю историю продавца и может открыть транзакцию на лот (`reconciled`); `countDocuments` на каждую страницу витрины |
| BP-6 | P3 | открыт | `Hero.kt:211` | `uniques[шаблон].serials` - список, растёт без предела внутри документа героя; `recordUniques` - O(вещи × номера) через `List.contains` |
| BP-7 | P3 | открыт | `ModerationWatch.kt:28`, `BoundedCache.kt:35` | Каждую минуту на каждый аккаунт под баном (и вечным) - `revokeAll` + `SessionCache.evictUser` = `removeIf` по 20 тыс. записей под `synchronized`, блокирующим чтения кэша запросами |
| BP-8 | P3 | открыт | `RouteTimings.kt:46, 63-81`; `Mail`, `AuthSession`, `GuildEvent`, `TrialRecord` | Нет TTL/чистки: `RouteTiming` растёт по дням, а пути сканеров (404) становятся своими ключами; сироты `HeroStats`/`TrialRecord`/`MailStamp` после стирания героя |
| BP-9 | P3 | открыт | `extensions.kt:12` → `LogManager.kt:52`, `Monitoring.kt:39`, `MongoFactory`, `BaseRepository` | `printLog` - синхронный `println` на потоке запроса на каждый старт/коммит транзакции, вставку/правку и запрос (+ второй раз через logback ConsoleAppender), новый `DateTimeFormatter` на вызов; `rotateLog` из двух корутин без замка |
| BP-10 | P3 | открыт | `HeroPresence.kt:11` | `written` - `ConcurrentHashMap` без вытеснения: запись на каждого когда-либо виденного героя |
| BP-11 | P3 | открыт | `MongoFactory.transactionExecute` | Тело транзакции не повторяется на `TransientTransactionError`/`WriteConflict` - отказ `BRY_007`/503, повторяет клиент; повторяется только коммит (`UnknownTransactionCommitResult`, 3 раза) |
| BP-12 | P3 | открыт | пул драйвера по умолчанию | maxPoolSize 100, ожидание пула **120 с** при удержании `HeroLocks` - при исчерпании пула запросы героя висят до 2 мин. Не увеличивать без замера; задать `maxWaitTime`/`timeoutMS` |
| BP-13 | P3 | открыт | `MongoBackupManager.kt:58-81` | Полный `find` каждой коллекции раз в `intervalDays` на каждой копии сервера |
| BP-14 | P3 | открыт | `BugReportRepository.kt:18` | `suggestions` - вся коллекция предложений в память и сортировка, на маршруте игрока |

Проверено и в порядке: фоновые задачи (`AuctionExpiry` 50 лотов/мин по индексу, `RiftAwards` раз в 10 мин) на своих scope, утечек
корутин нет; `runBlocking`/`Thread.sleep` в коде запросов нет; HTTP Asana - вне транзакции; `HeroLocks` удаляет свои записи;
`SessionCache`, `RateKeys`, `ownerOf` ограничены по размеру.
