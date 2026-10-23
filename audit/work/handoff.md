# Работа 0.17.4: баги - 2026-10-23
Шаг: вопросы
Ветки: клиент claude/sharp-bardeen-6i3dkz, сервер claude/tender-pasteur-a36kj2; база wip: клиент 16ebe3f1, сервер f8520ad

Issues: открытые bug #486-#528 (43 шт.). Вне работы: #445 (enhancement).

## Пакеты
| # | Пакет | Issues | Сервер | Дизайн | Оценка | Статус |
|---|-------|--------|--------|--------|--------|--------|
| 1 | Поход: правила захода | 526 522 511 518 510 525 | да | мало | 20 мин / 12 ч | выбран |
| 2 | Экран боя | 513 514 515 512 516 517 | нет | да | 25 мин / 18 ч | выбран |
| 3 | Характеристики и баланс | 519 520 496 521 524 | да | мало | 25 мин / 16 ч | выбран |
| 4 | Карточка предмета и строки | 499 504 506 507 508 523 | нет | да | 15 мин / 9 ч | выбран |
| 5 | Сумка и пул модов | 487 488 501 497 | да | да | 10 мин / 5 ч | выбран |
| 6 | Торговля | 502 503 505 | да | да | 15 мин / 10 ч | выбран |
| 7 | Город и герои | 486 491 492 500 527 | да | да | 20 мин / 13 ч | выбран |
| 8 | Модерация | 489 490 528 | да | да | 15 мин / 14 ч | выбран |
| 9 | Панели и ловушки на карте | 493 509 | нет | да | 10 мин / 7 ч | выбран |
| 10 | Новые системы: убер-боссы, уникалки | 494 495 498 | да | да | 25 мин / 24 ч | выбран |

## Issues
| # | Пакет | Статус | Кратко решение | Файл |
|---|-------|--------|----------------|------|
| 526 | 1 | вопросы | | 526.md |
| 522 | 1 | вопросы | | 522.md |
| 511 | 1 | вопросы | | 511.md |
| 518 | 1 | вопросы | | 518.md |
| 510 | 1 | вопросы | | 510.md |
| 525 | 1 | вопросы | | 525.md |
| 513 | 2 | вопросы | | 513.md |
| 514 | 2 | вопросы | | 514.md |
| 515 | 2 | вопросы | | 515.md |
| 512 | 2 | вопросы | | 512.md |
| 516 | 2 | вопросы | | 516.md |
| 517 | 2 | вопросы | | 517.md |
| 519 | 3 | вопросы | | 519.md |
| 520 | 3 | вопросы | | 520.md |
| 496 | 3 | вопросы | | 496.md |
| 521 | 3 | вопросы | | 521.md |
| 524 | 3 | вопросы | | 524.md |
| 499 | 4 | вопросы | | 499.md |
| 504 | 4 | вопросы | | 504.md |
| 506 | 4 | вопросы | | 506.md |
| 507 | 4 | вопросы | | 507.md |
| 508 | 4 | вопросы | | 508.md |
| 523 | 4 | вопросы | | 523.md |
| 487 | 5 | вопросы | | 487.md |
| 488 | 5 | вопросы | | 488.md |
| 501 | 5 | вопросы | | 501.md |
| 497 | 5 | вопросы | | 497.md |
| 502 | 6 | вопросы | | 502.md |
| 503 | 6 | вопросы | | 503.md |
| 505 | 6 | вопросы | | 505.md |
| 486 | 7 | вопросы | | 486.md |
| 491 | 7 | вопросы | | 491.md |
| 492 | 7 | вопросы | | 492.md |
| 500 | 7 | вопросы | | 500.md |
| 527 | 7 | вопросы | | 527.md |
| 489 | 8 | вопросы | | 489.md |
| 490 | 8 | вопросы | | 490.md |
| 528 | 8 | вопросы | | 528.md |
| 493 | 9 | вопросы | | 493.md |
| 509 | 9 | вопросы | | 509.md |
| 494 | 10 | вопросы | | 494.md |
| 495 | 10 | вопросы | | 495.md |
| 498 | 10 | вопросы | | 498.md |

## Занято
| Что | Значение | Пакет |
|-----|----------|-------|
| Провод: enum | `RefusalReason` (rules/run): RULE, GONE, BOSS_COOLDOWN, BOSS_SEALED, NO_PORTAL, NO_VAAL_ZONE, PRICE, CHOICE, TOOL, STASH, DEPTH, SUMMON, GEAR, FIGHT; поле `RunReport.refusals` | 1 |
| Провод: поле | `VaalZone.atlasEffect`, `FountainRule.shield` | 1 |
| Ключи локали | `expedition.refused.because`, `expedition.refused.reason.*`, `run.mods.*` (окно модификаторов карты) | 1 |
| Компонент UI | `ui/components/SourcedSumRow`, `SumShare` (строка-сумма с источниками) | 1 |
| Ключи локали | `fountain.part.*`, `vaal.with_map`; удалены `fight.flow.rest`, `fight.flow.part.rest`, `fight.flow.rest_in`, `map.atlas`, `map.guild`, `map.potion`, `map.modifiers`, `map.scarabs`, `map.influence` | 1 |
| Маршрут сервера | `POST hero/chest/open/all?code=` (отказы - существующие `CH_008`, `CH_020`; новых кодов нет) | 4 |
| Провод: поле | `ChestOpening.opened`, `ChestOpening.unopened` | 4 |
| Правило `rules` | `StashRules.free(held, bought)` | 4 |
| Ключи локали | `item.level_full`, `relic.serial_short`, `chest.open_all`, `chest.stash_full`, `chest.from_all.*`, `chest.unopened`, `row.might_up`, `row.might_down`, `row.might_up_full`, `row.might_down_full` | 4 |
| Компонент UI | `ui/components/LootRow` (компактная строка предмета), `SerialMark`, `ItemLevelMark` | 4 |
| Ключи локали | `fight.level_badge`, `fight.swing_at_you`, `fight.pack_left`, `fight.feed_invite`, `fight.peek.*`, `fight.flask_card`, `fight.stats_button`, `expedition.stats_fight*`, `stat.trend.now`; удалены `fight.at_you`, `expedition.pack_left`, `expedition.monster_line`, `fight.skills_title`, `fight.influence`, `fight.modifiers`, `fight.line_map`, `fight.stat_damage`; `fight.stage` - «Волна» | 2 |
| Компонент UI | `LevelBadge(level, tint, label)` (Relic.kt), `ItemCardSheet` (ItemInspector.kt), `ArenaHold`/`LocalArenaHold`, `core/sheet/SheetShift` | 2 |
| Маршрут | `POST hero/autosell/shelves` | 5 |
| Поле провода | `AutoSell.shelves` (`BagCategory` правил, именами) | 5 |
| Ключи локали | `bag.section_omens`, `bag.section_shards`, `bag.section_keys`, `bag.section_potions`, `bag.books_other`, `autosell.tab.*`, `autosell.all`, `autosell.group`, `autosell.sell_shelf`, `autosell.bag_note` | 5 |
| Код ошибки | `CH_048` (купленную вещь не надеть: в тайнике нет места) | 6 |
| Маршрут | `POST hero/merchant/buy?offerId=&wear=true&slot=` (покупка с надеванием) | 6 |
| Провод: поле | `RunEvent.Feature.wear` (`WearRequest` правил: место надевания купленного у бродячего торговца) | 6 |
| Ключи локали | `error.CH_048`, `merchant.buy_wear*`, `merchant.wear_*`, `auction.lot_*`, `auction.your_bid*` ; удалены `error.GR_001`, `guild.deposit_tester` | 6 |
| Маршрут сервера | `POST admin/moderation/purge?id=<sanctionId>` (стереть из корзины сейчас, только администратор) | 8 |
| Код ошибки | `MOD_015` - санкция не в корзине (не удаление, удаление героя вместе с аккаунтом, уже восстановлено) | 8 |
| Провод: форма | `admin/moderation/rows` - `Page<ModerationRow>` (`account` + `hero`, null - удалённый аккаунт); поля `ModerationHero.guild/gold/createdAt/server`, `ModerationAccount.heroCount`; `SanctionView.targetId/parentId`; у `QueueSight` нет `kind` | 8 |
| Ключи локали | `presence.online/just_now/minutes.*` (`presence.unknown` - «нет данных»), `calendar.*`, `moderation.purge.*`, `moderation.row.*`, `moderation.hero_count.*`; удалены `moderation.seen_*`, `dossier.seen.*`, `presence.now/hours/days/minutes`, `moderation.purge_in`; сервер: `error.MOD_015`, `enum.FeedbackKind.APPEAL` - «Модерация», `COMPLAINT` - «Жалоба» | 8 |
| Ядро и UI | `core/display/PresenceText.kt` (`presenceText`, `calendarText`, `dayText`, `dayTimeText`, `dateText`), `core/admin/TrashTerm`, `core/network/epochOfStamp`; `ui/components/HoldRingButton`, `rememberPresence`/`LivePresence`, `ReportKindLabel`; удалены `BaseRepository.groupedPage` сервера, `epochOf`/`seenText` клиента | 8 |

## dev-notes
Записаны: 524 496 510 511 512. Не записаны (запись прервана проверкой безопасности, gh на запись запрещён): 526 522 518 525 513 514 515 516 517 519 520 521 — записать через MCP позже.

## Сведение
- pkg1 сведён в wip (клиент 99ef083a+пин, сервер 50df1f6). Подъёмы: RULES_VERSION (#525), API_REVISION (#511 убрал поля контента без default у старых клиентов). Сомнения: тик недугов ловушек режется сопротивлением; знак доли по шаблону.

## Дальше
Владелец выбрал все 10 пакетов, версия 0.17.4 (patch). Вопросы с пакета 1. Разборы - audit/work/triage/.
