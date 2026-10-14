# 19_STATS · POWERS_2 (группа pw2): силы уникалок, вторая половина POWER

Объём: 407 характеристик `group=POWER` из `stats.json`, с `POWER_REFRACTED_EDGE` по `POWER_HUSHED_SPRING` включительно (порядок файла; 408-я…814-я из 815 POWER).
Код: `rules/.../content/Powers.kt` (PowerBook: validate, applySheet, loot/worldParts, features, slotPowers), клиент `core/.../campaign/BattlePowers.kt` (PowerRunner), `Battle*.kt`, `rules/.../sheet/Sheet.kt` (slotted, WornCount), `Fates.kt` (Осквернение), `CombatSheet.kt` (сводка), сервер `CampaignService.context` и `rules/run/Run.kt` (добыча), `MapFeatures.kt`.
Контент: `powers.json`, `equipment.json` (472 строки UNIQUE + 22 MYTHICAL), `modifiers.json`+`tree.json` (38 вершин древа дают силы этой половины), локали сервера `ru/en.json` (`enum.EnumStatStock.*`, `stat.template.*`), клиент `ui_*.json` (`enum.stat.*`).

## Сводка

| Итог | Кол-во |
|---|---|
| Всего характеристик | 407 |
| Работают (тест с силой/без силы показывает срабатывание и отличие боя/листа/добычи) | 396 |
| Работают частично / условно | 7 (`HELD_GROUND`, `DEEP_REFILL`, `LUCKDASH_GAMBLE`, `SUNSET_ARMY`, `WHETTED`, `SHAMANBANE`, `VEIN_CHARGE`) |
| Мёртвые (никогда не срабатывают) | 3 (`ORACLE_DOOM`, `CROWDED_MAP`, `VESPERS`) |
| Практически мёртвые | 1 (`VEIN_BURST`) |
| Текст расходится с числами контента | 10 сил (STAT-PW2-01) |
| Текст фляжных сил с лишним хвостом «во время действия» | 34 строки (STAT-PW2-06) |

Механизмы (PowerKind): лист `SHEET` 92, `STANDING/BUFF` 39, события боя (`on`) 247 в 120 сочетаниях событие/действие, добыча `WORLD` 18 (GOLD 6, RARITY 4, MAP 3, QUANTITY 2, EXPERIENCE 2, UNIQUE 1), слоты `SLOTS` 7, механики карты `FEATURES` 2, Осквернение `DESECRATION` 2.

Общие свойства всех 407 (проверено кодом):
- Имя: сервер `enum.EnumStatStock.<код>` есть у всех 407 (ru/en); клиент берёт `enum.stat.<код>` из `ui_*.json` (есть у 338), иначе серверное (`Text.rawStatTitle`). Где оба есть - совпадают (0 расхождений).
- Единица/kind: в `stats.json` у POWER нет `kind/unit/section` - только `code/group/order`. Смысл числа задаёт `power.roll`: `CHANCE` (71) - % шанса, `DURATION` (9) - секунды, `AMOUNT` - процент/число эффекта; у 15 сил ролл не читается вовсе (флаг вкл/выкл, колонка «ед.» = FLAG).
- Операция: все источники - `ADD` (строки уникалок `UNIQUE_<вещь>_<n>`, вершины древа `PASSIVE_POWER_*`, суффиксы зарядов). Одинаковые силы с нескольких вещей/вершин складываются (Σмакс в таблице); у CHANCE-сил сумма ≥100 - срабатывает всегда, потолка нет (вреда нет: `chance < 100` проверяется).
- Отрицательные роллы (11 сил, напр. `PRISM_VEIL`, `GRANITE_HIDE`, `UNCLOSING_WOUNDS`): у всех есть шаблон `.negative`, `ModifierText` печатает модуль `{|0|}` - знак верный.
- Мощь (`Might.hero` через `HeroBody`) видит только то, что легло в лист: правила листа (92) и силы слотов (7). Все 286 сил-событий и STANDING Мощь не учитывает (STAT-PW2-13).
- Бесконечные срабатывания: исключены кодом - `PowerRunner.busy`: «сила не отвечает на силу» (удар/лечение/эхо силы не порождают HIT/KILL/…); `EVERY` ≥2 с; `DEATH`/`LOW_LIFE` с откатом `-1` - раз за бой; стопки ограничены `stacks`. Эхо (`ECHO`) бьёт с `busy=true`, повтор (`RECAST`) - через `echoCast` без нового `SKILL_USE`. Цепочек не найдено.
- Капы: собственных потолков у POWER-статов нет (`StatLimits`/`rules.json caps` их не знают); ограничители - внутри силы: откат, NTH, `stacks`, `cap` счёта/правила листа (столбец «капы»).
- Источник → лист проверен тестом для всех немагазинных уникалок (441 строк: значение силы на листе в размахе шаблона; 19 - вещь неактивна из-за требований голой ведьмы 100 ур., не дефект). 34 строки фляг в лист не идут - они в строках глотка (`FlaskSheet.draught`), их проверил бой.

## Метод теста (метаморфный, реальным кодом)
1. События/STANDING (286 сил): `PowersPw2AuditTest.event_powers_with_and_without`. Герой - настоящий лист правил (`SheetCalculator.hero` ведьма 70 ур. + синтетические операции: 2500 здоровья, 800 ЭЩ, крит 20, блок 25, уклонение 1500, шансы недугов 20, атрибуты 100), `SheetModel`→`HeroGear`→`HeroBuild`, умения FIREBALL+FREEZING_PULSE, пояс FLASK_GIANT_LIFE + FLASK_DIAMOND (для сил фляг - сама уникальная фляга, ролл силы = 1.0 / роллу удалён). Сила добавляется операцией листа `ADD` на макс. ролл (из всех источников). Сцены: NORMAL (6 врагов в 2 раунда, редкий/волшебный/обычный, огонь и хаос, осквернённые), HARSH (урон врагов ×7 - низкое здоровье, смерть), TOTEM (уникальный босс со всеми тотемами контента). 6 сидов × 2 (с/без) × 3 сцены, `Battle.advance(0.1)` до 60 с. Метрики: события журнала с `skill == <сила>` (сработки), урон/лечение силы, урон героя/по герою, время, эффекты с источником силы на герое/врагах, барьер, неуязвимость, удержание врагов, заряды, недуги, проклятые, тики `powers.standing`.
2. Повтор (ALT) для 39 не сработавших: точность врагов 300 (уклонение срабатывает), хаос/изранение/иссушение у героя, «включатели» в оба боя (`POWER_ORACLE_CURSE`, `POWER_POWER_ON_CRIT`, `POWER_FRENZY_ON_KILL`, `POWER_ENDURANCE_WHEN_HIT` по 100), старт с 60% здоровья.
3. Лист (92): `PowerBook.applySheet` на готовом листе с источниками (атрибуты 300, броня/уклонение 3000, `WORN_*`, `FATE_EFFECT` 30…) с силой и без, плюс половина ролла (масштабируется ли).
4. Добыча (18): `Powers.loot` по каждой редкости и `rarity=null` (сводка). Механики карты (2): `Powers.features`. Слоты (7): `Sheets.calculate` с реальными вещами (носитель с ролл-строкой силы / без неё). Осквернение: `ItemFactory.create`.
5. Текст: `ModifierText.line` по каждой строке силы уникалок, ru и en; плюс сверка чисел шаблона с числами силы (скрипт по контенту).


## Дефекты

## [STAT-PW2-01] Текст 10 сил называет числа, которых нет в контенте (контент снижен ~×0,6, текст - нет)
- Категория: CONTENT/LOCALIZATION · Приоритет: P2 · Уверенность: высокая (проверено скриптом и `ModifierText`) · Подсистема: локаль сервера, `powers.json`
- Файлы: `content/powers.json` (строки сил ниже), `locale/ru.json`, `locale/en.json` → `stat.template.<сила>.ADD`
- Ожидание: строка вещи говорит те же числа, что делает бой (инвариант локализации «каждый модификатор локализован…»; игрок выбирает вещь по тексту).
- Факт (текст → контент):
  - `POWER_COAL_FERVOUR`: «20% к скорости атаки» → `value 12`
  - `POWER_ABBOT_BLINDNESS`: «−20% к скорости атаки» → `−12`
  - `POWER_RIMESORROW_NUMB`: «−15%» → `−9`
  - `POWER_BROTHERHOOD_MIST`: «−10%» → `−6`
  - `POWER_BIT_OF_LUCK`: «10% к скорости атаки» → `6`
  - `POWER_LUCKDASH_GAMBLE`: скорость атаки «от −10% до +20%» → `−6…+20`
  - `POWER_SAND_VERDICT`: «+15–20% к скорости атак» → `9…20`
  - `POWER_CHEATS_DEAL`: «Король» «20–30%» → `12…30`
  - `POWER_TIMELESS_BOON`: «+20% к скорости атаки и каста» → атака `12`, каст `20`; «3% регенерации здоровья в секунду» → `LIFE_REGEN_PERCENT 0.9`
  - `POWER_ROT_WINE_BOON`: «гнилое: −10% ко всему» / en «10% less of everything» → урон `−10` increased, скорость атаки `−6`, сопротивления `−10` (и это «reduced», а не «less»)
- Доказательство: все отличающиеся числа - строки `STOCK_ATTACK_SPEED` (и одна регенерация); соотношение 12/20, 9/15, 6/10 = 0,6 - след общей правки скорости атаки без текста.
- Последствия: игрок видит силу на 40-67% сильнее, чем она есть. Масштаб: 10 сил, 10 уникалок.
- Варианты: (а) исправить тексты под контент; (б) вернуть контент под тексты; (в) убрать числа строк-констант из шаблонов и подставлять их из `powers.json` (новый плейсхолдер), чтобы расхождение было невозможно.
- Регрессионный тест: контент-тест «каждое число шаблона силы, кроме `{v}`, есть среди чисел её записи `powers.json`» (скрипт аудита это уже делает).
- Вопрос владельцу: см. «Вопросы» → PW2-Q1. Рекомендация: (в), до неё - (а). Статус: открыт.

## [STAT-PW2-02] `POWER_ORACLE_DOOM` мёртвая: на убийстве проклятие цели уже снято
- Категория: BUG · P2 · Уверенность: высокая (код + тест) · Подсистема: бой, `PowerRunner`
- Файлы: `core/.../combat/BattleLand.kt:320-325` (`fell`: `fighter.effects.clear()` до всего), `:351` (`powers.killed(PowerMoment(fighter, …, ailments = ailing))` - недуги сохранены, эффекты нет); `BattlePowers.kt:270` (`TARGET_CURSED -> target?.cursed`, `cursed` = есть эффект `CURSE`).
- Ожидание: «Убитые проклятые враги взрываются: {v}%…» (SEERESSS_FINGERS).
- Факт: при `KILL` у цели уже нет эффектов → условие ложно всегда. Тест: 0 сработок в 6 сидах × 3 сценах и в ALT, где проклятия лежат (cursed-тиков 298+).
- Масштаб: та же ошибка у `POWER_VOID_PULSE` (группа pw1, `KILL`+`TARGET_CURSED`).
- Варианты: (а) снимок «был проклят» в `PowerMoment` до `effects.clear()` (как `ailing` для недугов) и `TARGET_CURSED` читает его; (б) чистить эффекты после `powers.killed`.
- Регрессия: бой с проклятым врагом, убийство → сила срабатывает. Рекомендация: (а). Статус: открыт.

## [STAT-PW2-03] Условие «врагов не меньше 4» почти недостижимо: на поле ≤3 врага
- Категория: DESIGN/BUG · P2 (`CROWDED_MAP` мёртвая), P3 (`HELD_GROUND`) · Уверенность: высокая
- Файлы: `rules.json` → `fight.maxFoes = 3`; `BattleTotems.kt:38` (`enemies()` = живые враги + стоящие тотемы); `powers.json`: `POWER_CROWDED_MAP` (`FIGHT_START` + `FOES_AT_LEAST 4`), `POWER_HELD_GROUND` (`STANDING` + `FOES_AT_LEAST 4`).
- Факт: в начале боя врагов ≤3, тотемы ставятся позже (`totems.first` = 6 с) → `CROWDED_MAP` (MARKED_SHAFTS) не срабатывает никогда (тест: 0, в т.ч. сцена с боссом-тотемщиком). `HELD_GROUND` работает только в бою с боссом, у которого 1+ тотем или свита сверх трёх.
- Варианты: (а) порог 3 («против 3 и более»); (б) считать ожидающих в очереди/раунде врагов; (в) оставить, переписав текст («с тотемами босса»).
- Вопрос: PW2-Q2. Статус: открыт.

## [STAT-PW2-04] `POWER_VESPERS` мёртвая: сокращать в начале боя нечего
- Категория: BUG · P3 · Уверенность: высокая (тест) · Файлы: `BattlePowers.kt:384` (`COOLDOWNS -> hero.readyAt.replaceAll{…}`), `:115` (`FIGHT_CLEAR` на первом тике), RULES «Не несутся … перезарядки».
- Факт: на первом тике `readyAt` пуст (перезарядки не несутся между боями) - сила срабатывает (запись в журнале), но ничего не меняет; бой с силой и без идентичен во всех сценах.
- Варианты: (а) превратить в «умения готовы на {v} с раньше» (сдвиг `openingReady`); (б) событие `SKILL_USE`/`EVERY`; (в) убрать силу. Вопрос: PW2-Q3.

## [STAT-PW2-05] Счётчики силы живут один бой: «каждое 10-е убийство» почти не наступает
- Категория: BUG/DESIGN · P3 · Уверенность: высокая (код + тест)
- Файлы: `BattlePowers.kt:80-85` (`PowerRunner` и `counts`, `stackCount` создаются на каждый `Battle`), `powers.json`: `POWER_VEIN_BURST` (`KILL` + `NTH 10`), `POWER_VEIN_CHARGE` (`stacks 10`, 10 с).
- Факт: в бою похода 3 врага на раунд; счёт NTH обнуляется с новым боем → `VEIN_BURST` не сработал ни разу (6 врагов на бой в тесте). «До 10 зарядов» `VEIN_CHARGE` ограничено числом убийств в одном бою (стопки тоже не переживают бой: перенесённый бафф начинает счёт с 1).
- Варианты: (а) хранить счётчики NTH/стопок сил в заходе (`HeroCarry`); (б) порог NTH под бой (3-5); (в) оставить и переписать текст. Вопрос: PW2-Q4.

## [STAT-PW2-06] Строки сил на уникальных флягах получают хвост «во время действия» (дубль или неправда)
- Категория: LOCALIZATION · P2 · Уверенность: высокая (тест `ModifierText`)
- Файлы: `rules/.../text/ModifierText.kt:35` (`if (FlaskSheet.draught(def, effect)) text += DRAUGHT`), `FlaskSheet.kt:110` (`draught` = тег `flask` и не `FLASK_*`) - `POWER_*` под условие попадает.
- Факт (ru): «Во время действия +27% к урону по отравленным врагам **во время действия**», «Глоток с шансом 78.7% отравляет всех врагов во время действия», «При применении мгновенно восстанавливает 99.6% максимума здоровья во время действия». Все 34 строки сил 18 уникальных фляг (en - «during effect» так же).
- Варианты: (а) `draught` не трогает `POWER_*` (шаблоны сил уже пишут «во время действия» сами); (б) убрать фразу из шаблонов и оставить хвост - но тогда он неверен для сил `on=FLASK`.
- Регрессия: тест текста строк уникальных фляг без повторов. Рекомендация: (а).

## [STAT-PW2-07] `POWER_SUNSET_ARMY` («умения бьют +N цели») действует только на атаки
- Категория: BUG/LOCALIZATION · P3 · Уверенность: высокая · Файлы: `BattleSkills.kt:177` (`val more = if (attack && hit.targets > 0) body[SKILL_TARGETS]…`).
- Факт: чары лишних целей не получают; тест с двумя чарами - бой с силой и без идентичен. THE_KINGS_LAST_LIGHT.
- Варианты: (а) текст «атаки умений бьют +N цели»; (б) применять и к чарам (баланс). Вопрос: PW2-Q5.

## [STAT-PW2-08] «Сокращает остальные откаты» сокращает и откат только что применённого умения
- Категория: LOCALIZATION/BUG · P3 · Уверенность: высокая (код) · Файлы: `BattleSkills.kt:104-109` (`readyAt[slot]` ставится до `powers.fire(SKILL_USE)`), `BattlePowers.kt:384`.
- Затронуто: `POWER_TINKER_GEARWORK` (−1 с), `POWER_MAIDEN_WHIM` (−0,5 с); у `MURMURED_LITANY`, `ORRERY_TURN` текст общий - верен.
- Варианты: (а) исключать слот события из `COOLDOWNS`; (б) текст «сокращает откаты умений».

## [STAT-PW2-09] `POWER_SHAMANBANE` обещает «снимает с вас проклятия», а снимает недуги
- Категория: BUG/LOCALIZATION · P3 · Уверенность: высокая (код) · Файлы: `BattlePowers.kt:390` (`CLEANSE -> hero.ailments.clear()`); проклятие героя - эффект `CURSE` (`Fighter.cursed`), его `CLEANSE` не трогает.
- Факт: проклятие тотема `TotemKind.CURSE` остаётся; снимаются горение/яд/холод. Барьер работает (тест: барьер-тики 512, смерти 0,0 против 1,0).
- Варианты: (а) новое действие `UNCURSE` / флаг у `CLEANSE` (снимать `CURSE`-эффекты); (б) исправить текст. Вопрос: PW2-Q6.

## [STAT-PW2-10] `POWER_LUCKDASH_GAMBLE`: половина про скорость передвижения не действует нигде
- Категория: BUG · P3 · Уверенность: высокая (код) · Файлы: `BattlePowers.kt:309` (BUFF выкидывает `MOVEMENT_SPEED`), бафф живёт в бою/`HeroCarry`, карта читает скорость с листа.
- Факт: «скорость передвижения меняется наугад от −20% до +40%» - в бою отброшено, на карту не попадает. Скорость атаки работает (с числами PW2-01).
- Варианты: (а) переносить строку движения бафа на карту; (б) убрать её из силы и текста.

## [STAT-PW2-11] Сила на фляге с `on=FLASK` срабатывает на глоток любой фляги, пока её глоток идёт
- Категория: EXPLOIT/BUG · P2 · Уверенность: высокая (тест) · Файлы: `BattleSkills.kt:445-460` (`lay(…draught.lines…)`, затем `powers.fire(FLASK)` на каждый глоток), `BattlePowers.kt:109` (значение силы - с тела героя, где строки глотка лежат всё время действия).
- Факт: `POWER_BOG_DRAUGHT_GULP` (FEN_WITCHS_TREAT, «при применении мгновенно восстанавливает 90–100% здоровья»): 2 глотка FEN за бой → 3-4 сработки (лечение 90-100% и на глоток фляги жизни). То же у всех 22 сил `on=FLASK` на 14 уникальных флягах (`EMBER_PYRE`, `STONEBLOOD_STILL`, `SURF_BREAKER`, `CARDINAL_SPORES`…). Тексты говорят «при применении/глоток из фляги» - про эту флягу.
- Масштаб: Ф. жизни + FEN = почти полное лечение на каждом глотке пока длится FEN.
- Варианты: (а) `PowerMoment` с номером фляги и силы фляги отвечают только на свой глоток; (б) текст «глоток любой фляги, пока действует эта»; (в) откат у таких сил.
- Регрессия: глотки FEN=2 → сработок 2. Вопрос: PW2-Q7.

## [STAT-PW2-12] `POWER_WHETTED` «первый удар боя» - на деле все удары первой секунды
- Категория: LOCALIZATION/BUG · P3 · Уверенность: высокая (тест: 8 сработок на бой) · Файлы: `powers.json` (`HIT` + `FIGHT_BEFORE 1`), герой бьёт с 0,35 с, удары умений и по нескольким целям тоже `HIT`.
- Варианты: (а) `NTH`-подобная проверка «первый удар» (флаг в `PowerRunner`); (б) текст «удары первой секунды боя».

## [STAT-PW2-13] Мощь не видит ни одной силы-события и STANDING
- Категория: DESIGN · P3 · Уверенность: высокая (код) · Файлы: `rules/.../sheet/HeroBody.kt` (`stats = fold.with(passiveLines)`), `Might.kt` - `POWER_*` лист несёт, но `Might` их не читает; RULES (Общее): «Новая механика, влияющая на бой, обязана писать в лист характеристик или в HeroBody, чтобы её учла Мощь».
- Факт: 286 сил этой половины (вкл. безусловные STANDING без проверок: `SURF_ROAR` «больше урона», `SUNSET_GLORY`, `CARDINAL_FAITH`, `STORM_VESSEL_*`…) в Мощь не входят; 99 сил листа/слотов - входят.
- Варианты: (а) безусловные STANDING-строки (без checks и scale) класть в `HeroBody`; (б) оставить (Мощь - не прогноз боя) и записать исключение в RULES. Вопрос: PW2-Q8.

## [STAT-PW2-14] Усиление/зеркало слотов множит и силы `POWER_*` чужих уникалок - выше их размаха
- Категория: BALANCE (вопрос) · P3 · Уверенность: высокая (тест) · Файлы: `rules/.../sheet/Sheet.kt:343` (не множатся только силы слотов `op.stat in rules`).
- Факт: TWIN_FACET (`SEAL_HARMONY` 46,9%) поднял свою `POWER_SEAL_FACET` 2,7 → 4,0 (размах 2–3); BALDRIC (`ENTHRONED_REIGN` до 100%) удваивает силы уникальных самоцветов (20 уникальных самоцветов этой половины несут силы); `REFLECTED_IMAGE` копирует силы второго кольца (две копии силы).
- Вопрос: PW2-Q9.

## [STAT-PW2-15] `POWER_ROAD_WEARY`: `stacks: 5` недостижимы
- Категория: DATA · P3 · Уверенность: высокая (код) · Файлы: `BattlePowers.kt:473-477` (`stackCount` нового боя пуст; `FIGHT_CLEAR` - раз за бой).
- Факт: стопка всегда 1 (лежащий перенесённый бафф заменяется тем же источником). Текст стопок не обещает → вреда нет; поле контента мёртвое. Вариант: убрать `stacks` или хранить счёт в заходе (см. PW2-05).

## [STAT-PW2-16] Округление листа до десятых съедает малые «больше скорости атаки»
- Категория: BUG (кросс-ссылка на аудит округления) · P3 · Уверенность: высокая (тест `applySheet`) · Файлы: `Powers.kt:90` (`tenths`), `Sheet.kt:93`.
- Факт: `THICKET_GRACE` +0,5% больше скорости атаки за 1000 уклонения: 1,5 → 1,5225 → 1,5 (эффекта нет); `HOLLOW_INSTINCT` 4% → 1,5→1,6 (+6,7%); `MIRAGE_DRAG` −30% → 1,5→1,0 (−33%); `KEEPER_OARS` 12% → 1,68→1,7.
- Варианты: хранить `STOCK_ATTACK_SPEED` (удары/с) с сотыми, как и бой.

## [STAT-PW2-17] Разнобой названий зарядов
- Категория: LOCALIZATION · P3 · `POWER_IDOLBREAKER_SPLINTERS` «заряд **неистовства**» и `POWER_WARDEN_RESOLVE`/`WARDEN_BASTION` «заряд(ы) **стойкости**» при «ярости»/«выносливости» во всех остальных силах (FRENZY/ENDURANCE). Вариант: единые термины.

## [STAT-PW2-18] «больше скорости каста» там, где каст прибавляется как «увеличение»
- Категория: LOCALIZATION · P3 · `THICKET_GRACE` (en «0.5% more Attack and Cast Speed»; ru без «больше/увеличение») и `HOLLOW_INSTINCT` («2% more Attack and Cast Speed») - атака `MORE`, каст `PER` (сложение с увеличениями). Вариант: «+X% к скорости каста и X% больше скорости атаки».

## Прочее (без карточки, P3)
- `FIGHT_CLEAR`-силы (8 в этой половине) в Разломе не работают: `RiftArena.kt:272` не передаёт `cleared` (поход и испытания передают `wonLast`).
- Сводка героя (`CombatSheet.kt:168-171, 193-194`) показывает только силы добычи «против всех» (`on(null)`) и только виды с характеристикой (`QUANTITY/RARITY/GOLD/EXPERIENCE`): 7 сил против редких/уникальных (`CHARTED_SPOILS`, `PRIZE_OF_THE_ROAD`, `FAR_TROPHIES`, `GALE_SPOILS`, `AVARICE_HOARD`, `EYELESS_SIGHT`, `TRAILFINDER`) и силы `MAP`/`UNIQUE` (4) в сводке не видны, хотя сервер их катит (`CampaignService.context` по каждой редкости).
- `POWER_MOURNING_BELL` «наносят на {v}% больше урона оружия» сделано отдельным ударом `DAMAGE` (свой крит/защита), не множителем удара.
- Тестов на силы в репозиториях нет: ни один тест `core/src/test`, `rules/src/test` не упоминает `POWER_` (TEST_GAP).
- Мелкие несказанные числа в тексте (не ошибка, но игрок не знает): откаты 6/8/12/1/3 с у `SHRUG_OFF`, `RECOUP_WARD`, `CANTORS_BREATH`, `SULPHUR_BREATH`, `VOID_TRADE`; потолки 48/32/60/36/10 у `CARESS_DEVOTION`, `LOYAL_BANNER`, `DEEP_EMBRACE`, `RIFT_STEP`; 30 с у `ROAD_WEARY`.
- `POWER_DEEP_REFILL`, `RISING_HEAT`, `TIMELESS_DAWN` (`FIGHT_CLEAR` + лечение) работают, только если герой вошёл в бой не полным (принёс урон с карты) - в тесте с полным запасом сработка без эффекта, с 60% здоровья - эффект есть.
- `POWER_UNCLOSING_WOUNDS` (минус восстановлению врага) видим только на врагах с регенерацией/вампиризмом; у синтетических врагов теста их нет - проклятие ложится (293 тика), эффекта нет.

## Вопросы владельцу
**Числа текстов (PW2-01)**
- PW2-Q1: десять сил говорят большие числа, чем делают. Что верно? (а) контент - править тексты; (б) тексты - вернуть контент; (в) контент, и ввести плейсхолдеры констант силы, чтобы тексты не расходились впредь.

**`CROWDED_MAP` / `HELD_GROUND` (PW2-03)**
- PW2-Q2: «врагов не меньше 4» при поле в 3. (а) порог 3; (б) считать врагов боя целиком (с ожидающими раундами); (в) оставить для боссов с тотемами и сказать это в тексте; (г) заменить условие.

**`VESPERS` (PW2-04)**
- PW2-Q3: (а) «первые умения боя готовы на {v} с раньше»; (б) сокращение перезарядок раз в N с; (в) убрать силу/заменить.

**`VEIN_BURST`, `VEIN_CHARGE` (PW2-05)**
- PW2-Q4: счёт «каждое N-е» и стопки - на бой или на заход? (а) на заход (несётся `HeroCarry`); (б) на бой, снизить N до 3-5 и стопки до 3-5; (в) оставить как есть, поправив текст.

**`SUNSET_ARMY` (PW2-07)**
- PW2-Q5: (а) только атаки - поправить текст; (б) и чары тоже.

**`SHAMANBANE` (PW2-09)**
- PW2-Q6: (а) снимать проклятия (новое действие/флаг); (б) снимать недуги - поправить текст; (в) и то, и другое.

**Силы фляг на `FLASK` (PW2-11)**
- PW2-Q7: (а) сила фляги отвечает только на свой глоток; (б) на любой глоток, пока действует, - и сказать в тексте; (в) (б) + откат.

**Мощь (PW2-13)**
- PW2-Q8: (а) класть в Мощь безусловные STANDING-строки; (б) оценивать силы-события средней долей (сложно); (в) не учитывать и записать исключение в RULES.

**Усиление слотов (PW2-14)**
- PW2-Q9: множить ли силы чужих уникалок? (а) да, как сейчас (текст «модификаторы» это покрывает); (б) нет - исключать все `POWER_*`; (в) только силы с числом эффекта, не CHANCE/FLAG.

## Запущенные тесты
Копия клиента: `/home/user/audit/work/stats-pw2/EF` (сабмодуль `backend` = текущий ktor-bestgame, `powers/stats/equipment/modifiers.json` и `Powers.kt` побайтно равны). Тесты: `core/src/test/kotlin/com/sperance/exileforge/core/audit/PowersPw2AuditTest.kt`, `PowersPw2StaticAuditTest.kt`, список сил `core/src/test/resources/pw2_powers.txt`.
- `PW2_SEEDS=6 ./gradlew --offline -q :core:test --tests "…audit.PowersPw2AuditTest.event_powers_with_and_without" -i` - BUILD SUCCESSFUL, 846 строк (286 сил × до 3 сцен), лог `work/stats-pw2/ev_run1.log`.
- То же с `PW2_ALT=1 PW2_ONLY=<39 не сработавших>` - SUCCESS, лог `ev_alt.log`; после него не работают 4 (`CROWDED_MAP`, `VESPERS`, `ORACLE_DOOM`, `VEIN_BURST`), без эффекта 2 (`DEEP_REFILL` - условно, `SUNSET_ARMY`).
- `PW2_FLASKS=1 PW2_ONLY=POWER_BOG_DRAUGHT_GULP,POWER_EMBER_PYRE` - глотки FEN=2, сработок 3-4 (PW2-11).
- `./gradlew --offline -q :core:test --tests "…audit.PowersPw2StaticAuditTest" -i` - SUCCESS, лог `static.log` (92 листа, 18 добычи, 2 механики, 7 слотов, 2 Осквернения, 460 строк источников, 494 строки текста на язык).
- Сверка текстов с контентом - `work/stats-pw2/py/check.py` (числа шаблона ↔ числа силы).

## Что не проверено
- Сервер (Ktor + Mongo): `CampaignService.context`→`Run` для добычи проверен кодом и `Powers.loot`, заход сервером не прогонялся; `MapFeatures` (`BLIGHT_SPREAD`, `BLIGHT_CHEST`) - кодом.
- `POWER_ASHEN_CROWN` (boon): по коду (`Fates.strength` → `FateBoons.strengthen`), без теста на осквернённой вещи с раскрытой строкой.
- Силы листа с небоевыми целями (`SKILL_EXPERIENCE`, `RUNE_EFFECT`, `PASSIVE_TIER`, `ACTIVE_RUNE_SOCKETS`, `ASCENDED_DAMAGE`, `FATE_EFFECT`, `LIFE_REGEN_FLOOR`, `MAX_ENDURANCE_CHARGES`, фляжные `STOCK_FLASK_*`): тест показал сдвиг листа; потребители найдены grep'ом, их итоговый эффект не прогонялся.
- Монстры силами не пользуются (PowerRunner только у героя; POWER_* в `modifiers.json` - только суффиксы зарядов и вершины древа героя) - бой монстра со статами POWER не проверялся за ненадобностью.
- Экран (Compose): отображение строк/сводки на устройстве не смотрелось; проверены строки `ModifierText` и данные `CombatSheet`.
- Значение ролла взято максимальным из всех источников; стыковка нескольких одинаковых сил (Σ) тестом не гонялась (сумма ADD - по коду).

## Таблица характеристик

| код | имя (ru) | ед. | ops | источники / макс. | потребители (механизм) | тест | капы | показ | статус |
|---|---|---|---|---|---|---|---|---|---|
| POWER_REFRACTED_EDGE | Преломлённая грань | % | ADD | REFRACTING_PLUMES[3..5]; STAFF_OF_SPLIT_LIGHT[3..5] · Σмакс≈10 | бой героя (PowerRunner on=INFLICT); INFLICT/BUFF; Мощь: нет | работает (сраб. 54.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 3 | ок | работает |
| POWER_PRISM_VEIL | Призменная завеса | % | ADD | REFRACTING_PLUMES[-12..-8] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 6 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_DESCENT_STRIDE | Шаг нисхождения | % | ADD | DESCENDERS_GREAVES[7.5..12.5] | бой героя (PowerRunner on=FIGHT_CLEAR); FIGHT_CLEAR/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_HELD_GROUND | Удержанный рубеж | % | ADD | DESCENDERS_GREAVES[30..50] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает только с тотемами босса (врагов на поле ≤3) | работает (сраб. 0.0/бой, стоит 391 тиков; Δбоя в TOTEM) | — | ок | частично (PW2-03) |
| POWER_ROYAL_JELLY | Маточное молочко | % | ADD | WORKER_IN_AMBER[0.5..1] | бой героя (PowerRunner on=INFLICT); INFLICT/HEAL; Мощь: нет | работает (сраб. 8.3/бой; Δбоя в NORMAL,TOTEM; ALT) | откат 1 с | ок | работает |
| POWER_HIVE_TEMPO | Темп улья | % | ADD | WORKER_IN_AMBER[2..3] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 461 тиков; Δбоя в NORMAL,HARSH,TOTEM) | счёт≤4 | ок | работает |
| POWER_HIVE_VOLLEY | Залп улья | % | ADD | HONEYCOMB_QUIVER[40..60] | бой героя (PowerRunner on=INFLICT); INFLICT/DAMAGE; Мощь: нет | работает (сраб. 3.0/бой; Δбоя в NORMAL,TOTEM; ALT) | каждое 5-е | ок | работает |
| POWER_DRONE_HASTE | Спешка трутня | % | ADD | HONEYCOMB_QUIVER[15..25] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_GLASS_BREATH | Стеклянный вдох | % | ADD | UNBROKEN_SEAGLASS[1..2] | бой героя (PowerRunner on=KILL); KILL/HEAL; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH) | — | ок | работает |
| POWER_FIRST_WAVE | Первая волна | % | ADD | UNBROKEN_SEAGLASS[8..12] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_KINDLING | Растопка | % | ADD | SEED_OF_WILDFIRE[3..5]; THE_HEADMANS_FIREBRAND[3..5] · Σмакс≈10 | бой героя (PowerRunner on=INFLICT); INFLICT/BUFF; Мощь: нет | работает (сраб. 20.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 3 | ок | работает |
| POWER_SMOULDER_SIGHT | Тлеющий взгляд | % | ADD | SEED_OF_WILDFIRE[15..20] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 420 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_WEEPING_GUARD | Плачущий заслон | % шанс | ADD | TEAR_OF_THE_LOST_PASS[20..30] | бой героя (PowerRunner on=BLOCK); BLOCK/AILMENT; Мощь: нет | работает (сраб. 4.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_STILL_POSTURE | Неподвижная стойка | % | ADD | TEAR_OF_THE_LOST_PASS[2..3] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 250 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_PYRE_WARD | Заслон костра | % | ADD | COAL_OF_THE_COLD_HEARTH[10..15] | бой героя (PowerRunner on=LOW_LIFE); LOW_LIFE/BARRIER; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в HARSH,TOTEM) | откат 15 с | ок | работает |
| POWER_EMBER_DRINK | Глоток углей | % | ADD | COAL_OF_THE_COLD_HEARTH[1..2] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/HEAL; Мощь: нет | работает (сраб. 3.7/бой; Δбоя в NORMAL,TOTEM) | откат 2 с | ок | работает |
| POWER_MURMURED_LITANY | Бормотание литании | число | ADD | WORN_ROSARY_BEAD[0.2..0.3]; ROD_OF_ENDLESS_PENANCE[0.2..0.3] · Σмакс≈0.6 | бой героя (PowerRunner on=SKILL_USE); SKILL_USE/COOLDOWNS; Мощь: нет | работает (сраб. 26.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_PENANCE | Епитимья | % | ADD | WORN_ROSARY_BEAD[10..15]; ROD_OF_ENDLESS_PENANCE[10..15] · Σмакс≈30 | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 393 тиков; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_RALLY_THE_LINE | Сомкнуть строй | % | ADD | TWICE_PAINTED_STANDARD[7.5..10]; BANNER_BLADE_OF_THE_IRON_RANK[8.6..11.5] · Σмакс≈21.5 | бой героя (PowerRunner on=FIGHT_CLEAR); FIGHT_CLEAR/BARRIER; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_SWITCHED_SIDES | Сменённая сторона | % | ADD | TWICE_PAINTED_STANDARD[10..15] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/HEX; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_MOLTEN_ECHO | Расплавленное эхо | % | ADD | FIRE_FROM_THE_JAW[30..40] | бой героя (PowerRunner on=HIT); HIT/ECHO; Мощь: нет | работает (сраб. 139.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_ERUPTION | Извержение | % | ADD | FIRE_FROM_THE_JAW[6..10] | бой героя (PowerRunner on=KILL); KILL/DAMAGE; Мощь: нет | работает (сраб. 10.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_STOKED_GRIP | Раздутая хватка | % | ADD | FORGEFIRE_TONGS[5..8] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 500 тиков; Δбоя в NORMAL,HARSH,TOTEM) | счёт≤8 | ок | работает |
| POWER_WHITE_HEAT | Белый жар | % шанс | ADD | FORGEFIRE_TONGS[30..50] | бой героя (PowerRunner on=CRIT); CRIT/AILMENT; Мощь: нет | работает (сраб. 10.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_HAMMERED_BACK | Отбито молотом | % | ADD | BELT_OF_REFORGED_SWORDS[25..35] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/RETALIATE; Мощь: нет | работает (сраб. 64.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_WARFORGE_TEMPER | Кузнечный нрав | % | ADD | BELT_OF_REFORGED_SWORDS[30..50] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 452 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_VENT_GUST | Жерловый порыв | % | ADD | BELLOWS_OF_THE_UNDERFIRE[4..6] | бой героя (PowerRunner on=EVERY); EVERY/DAMAGE; Мощь: нет | работает (сраб. 16.3/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 5 с | ок | работает |
| POWER_RISING_HEAT | Восходящий жар | % | ADD | BELLOWS_OF_THE_UNDERFIRE[5..7.5] | бой героя (PowerRunner on=FIGHT_CLEAR); FIGHT_CLEAR/HEAL; Мощь: нет | работает (ALT: старт с 60% здоровья) | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_KINDLED_PACE | Разожжённый шаг | % | ADD | CHARRED_SOLES[10..15] | бой героя (PowerRunner on=KILL); KILL/BUFF; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_SCORCHED_ARRIVAL | Опалённый приход | % шанс | ADD | CHARRED_SOLES[20..30] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/AILMENT; Мощь: нет | работает (сраб. 0.3/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_UNBROKEN_LINE | Неразорванная нить | % | ADD | BOTTOMLESS_PLUMB_LINE[1.5..2.5] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 519 тиков; Δбоя в NORMAL,HARSH,TOTEM) | счёт≤12 | ок | работает |
| POWER_DEPTH_SIGHT | Глубинное зрение | % шанс | ADD | BOTTOMLESS_PLUMB_LINE[25..50] | бой героя (PowerRunner on=FIGHT_CLEAR); FIGHT_CLEAR/NEXT_CRIT; Мощь: нет | работает (сраб. 0.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_VAAL_SKIN | Ваальская кожа | % | ADD | ROTBEARERS_TREADS[6..10] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/BARRIER; Мощь: нет | работает (сраб. 4.2/бой; Δбоя в NORMAL,HARSH) | откат 3 с | ок | работает |
| POWER_SPREADING_ROT | Ползучая гниль | % | ADD | ROTBEARERS_TREADS[3..5]; ROTHEART_GIRDLE[3..5] · Σмакс≈10 | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 508 тиков; Δбоя в NORMAL,HARSH,TOTEM) | счёт≤5 | ок | работает |
| POWER_CHARTED_SPOILS | Нанесённая добыча | % | ADD | PRODIGAL_BOOTS[10..15] | заход сервера (Powers.loot QUANTITY) + сводка; WORLD/QUANTITY; Мощь: — | Powers.loot: QUANTITY [RARE] → по редкостям ок; сводка(null)=0.0 | — | ок | работает |
| POWER_SIDESTEP | Шаг в сторону | % шанс | ADD | PRODIGAL_BOOTS[20..30] | бой героя (PowerRunner on=EVADE); EVADE/RUSH; Мощь: нет | работает (сраб. 1.3/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | шанс≥100 = всегда | ок | работает |
| POWER_CROWDED_MAP | Людная карта | % | ADD | MARKED_SHAFTS[15..25] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF; Мощь: нет | НЕ срабатывает (6 сидов × 3 сцены + ALT) | — | ок | мёртвая (PW2-03) |
| POWER_MARKED_ROUTES | Отмеченные пути | % | ADD | MARKED_SHAFTS[10..20] | заход сервера (Powers.loot MAP) + сводка; WORLD/MAP; Мощь: — | Powers.loot: MAP [] → по редкостям ок; сводка(null)=20.0 | — | ок | работает |
| POWER_VESPERS | Вечерня | число | ADD | APOSTATES_COWL[1..2] | бой героя (PowerRunner on=FIGHT_CLEAR); FIGHT_CLEAR/COOLDOWNS; Мощь: нет | срабатывает (1.0/бой), эффекта нет | — | ок | мёртвая (PW2-04) |
| POWER_HERETICS_MARK | Метка еретика | % шанс | ADD | APOSTATES_COWL[20..30]; BOG_HAGS_CROOK[20..30] · Σмакс≈60 | бой героя (PowerRunner on=SKILL_USE); SKILL_USE/CURSE; Мощь: нет | работает (сраб. 8.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_SHARED_MALADY | Общий недуг | % шанс | ADD | PLAGUE_BALM[50..80] | бой героя (PowerRunner on=FLASK); FLASK/AILMENT; Мощь: нет | работает (сраб. 1.3/бой, стоит 50 тиков; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | хвост «во время действия» | работает |
| POWER_DOCTORS_CRUELTY | Жестокость лекаря | % | ADD | PLAGUE_BALM[20..30] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 50 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_TENTACLE_SWEEP | Взмах щупалец | % | ADD | BEAK_OF_THE_DEEP[40..60] | бой героя (PowerRunner on=HIT); HIT/DAMAGE; Мощь: нет | работает (сраб. 37.3/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждое 4-е | ок | работает |
| POWER_INK_CLOUD | Чернильное облако | % | ADD | BEAK_OF_THE_DEEP[8..12] | бой героя (PowerRunner on=KILL); KILL/BARRIER; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_TOTALITY | Полная фаза | % | ADD | SUNLESS_MOMENT[60..80] | бой героя (PowerRunner on=EVERY); EVERY/BUFF; Мощь: нет | работает (сраб. 5.3/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 8 с | ок | работает |
| POWER_UMBRA | Тень | с | ADD | SUNLESS_MOMENT[0.5..1] | бой героя (PowerRunner on=CRIT_TAKEN); CRIT_TAKEN/INVULNERABLE; Мощь: нет | работает (сраб. 2.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | откат 10 с | ок | работает |
| POWER_ANSWERING_DARK | Отвечающая тьма | % | ADD | TWOFOLD_ECHO[25..35] | бой героя (PowerRunner on=HIT); HIT/ECHO; Мощь: нет | работает (сраб. 139.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_DEEP_REFILL | Глубинное наполнение | % | ADD | TWOFOLD_ECHO[10..15] | бой героя (PowerRunner on=FIGHT_CLEAR); FIGHT_CLEAR/HEAL; Мощь: нет | условно: ЭЩ в начале боя полон, если не принесён неполным (HeroCarry); тест: сработала, эффекта нет | срабатывает (1.0/бой), эффекта нет | — | частично (условно) | частично |
| POWER_PRIZE_OF_THE_ROAD | Добыча дороги | % | ADD | WANDERING_MENHIR[15..25] | заход сервера (Powers.loot RARITY) + сводка; WORLD/RARITY; Мощь: — | Powers.loot: RARITY [RARE, UNIQUE] → по редкостям ок; сводка(null)=0.0 | — | ок | работает |
| POWER_BESIEGED_GUARD | Осаждённый заслон | % | ADD | WANDERING_MENHIR[5..8] | бой героя (PowerRunner on=BLOCK); BLOCK/BARRIER; Мощь: нет | работает (сраб. 12.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_DISTANT_THUNDER | Далёкий гром | % | ADD | FARSKY_BOW[25..40] | бой героя (PowerRunner on=HIT); HIT/ECHO; Мощь: нет | работает (сраб. 148.3/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_FAR_TROPHIES | Дальние трофеи | % | ADD | FARSKY_BOW[20..30] | заход сервера (Powers.loot RARITY) + сводка; WORLD/RARITY; Мощь: — | Powers.loot: RARITY [UNIQUE] → по редкостям ок; сводка(null)=0.0 | — | ок | работает |
| POWER_UNMAPPED_ARRIVAL | Прибытие без карты | с | ADD | WINGS_BEYOND_THE_MAP[1..1.5] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/INVULNERABLE; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_FRONTIER_FIND | Находка за рубежом | % | ADD | WINGS_BEYOND_THE_MAP[20..30] | заход сервера (Powers.loot MAP) + сводка; WORLD/MAP; Мощь: — | Powers.loot: MAP [] → по редкостям ок; сводка(null)=30.0 | — | ок | работает |
| POWER_FRENZY_ON_KILL | Яростное убийство | % шанс | ADD | SCAVENGERS_HATCHET[10..15]; BLOOD_TRAIL[10..15]; UNDERSIDE_STRIDERS[15..20]; GRAVEPICKERS_WINGS[10..15]; мод FRENZY_CHARGE_ON_KILL(SUFFIX, древо) [[12, 15]]; древо CHG_DUE_0(NOTABLE)=[12.0]; древо CHG_SHA_0(NOTABLE)=[10.0] · Σмакс≈87 | бой героя (PowerRunner on=KILL); KILL/CHARGE; Мощь: нет | работает (сраб. 1.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_POWER_ON_KILL | Убийство силы | % шанс | ADD | MISERLY_HOLLOW[15..20]; мод POWER_CHARGE_ON_KILL(SUFFIX, древо) [[12, 15]]; древо CHG_TEM_0(NOTABLE)=[12.0] · Σмакс≈32 | бой героя (PowerRunner on=KILL); KILL/CHARGE; Мощь: нет | работает (сраб. 1.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_ENDURANCE_ON_KILL | Стойкое убийство | % шанс | ADD | STONETHROATS_ROAR[10..15]; CARVING_CHART[8..12]; THE_FINAL_SENTENCE[10..15]; мод ENDURANCE_CHARGE_ON_KILL(SUFFIX, древо) [[12, 15]]; древо CHG_DUE_0(NOTABLE)=[12.0]; древо CHG_TEM_0(NOTABLE)=[12.0] · Σмакс≈66 | бой героя (PowerRunner on=KILL); KILL/CHARGE; Мощь: нет | работает (сраб. 1.3/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_FRENZY_ON_HIT | Яростные удары | % шанс | ADD | RAIDERS_THIRST[10..15]; древо CHG_RAN_1(NOTABLE)=[8.0] · Σмакс≈23 | бой героя (PowerRunner on=HIT); HIT/CHARGE; Мощь: нет | работает (сраб. 10.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | откат 0.5 с, шанс≥100 = всегда | ок | работает |
| POWER_POWER_ON_CRIT | Сила крита | % шанс | ADD | FEATHERS_OF_NAUGHT[15..20]; THE_AUGURS_COUNTING_BONES[15..20]; древо CHG_WIT_1(NOTABLE)=[15.0] · Σмакс≈55 | бой героя (PowerRunner on=CRIT); CRIT/CHARGE; Мощь: нет | работает (сраб. 4.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_ENDURANCE_WHEN_HIT | Закалка ударами | % шанс | ADD | MAIL_OF_THE_GRANITE_WATCH[15..25]; древо CHG_MAR_1(NOTABLE)=[15.0] · Σмакс≈40 | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/CHARGE; Мощь: нет | работает (сраб. 9.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_RANDOM_CHARGE_ON_KILL | Дар глубин | % шанс | ADD | EMBRACE_OF_THE_SUNKEN[10..10]; DUST_OF_THE_CAST_DOWN_STAR(M)[15..20] · Σмакс≈30 | бой героя (PowerRunner on=KILL); KILL/CHARGE; Мощь: нет | работает (сраб. 1.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_LAST_THUNDER_RELAY | Переданный гром | % | ADD | FINAL_ECHO_OF_THUNDER(M)[140..160]; SCEPTRE_OF_THE_DEATHLESS_LORD[140..160] · Σмакс≈320 | бой героя (PowerRunner on=HIT); HIT/DAMAGE; Мощь: нет | работает (сраб. 96.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | откат 0.3 с | ок | работает |
| POWER_LAST_PEAL | Последний раскат | % шанс | ADD | FINAL_ECHO_OF_THUNDER(M)[20..30] | бой героя (PowerRunner on=CRIT); CRIT/AILMENT; Мощь: нет | работает (сраб. 5.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_GULL_SNATCH | Выхватить | % | ADD | SCAVENGERS_HATCHET[1..2] | бой героя (PowerRunner on=CRIT); CRIT/HEAL; Мощь: нет | работает (сраб. 19.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_SPRIG_KINDLING | Растопка | % | ADD | LAMP_ROCK_SHOOT[2..3]; STAFF_OF_THE_FIRST_SPROUT[2..3] · Σмакс≈6 | бой героя (PowerRunner on=KILL); KILL/HEAL; Мощь: нет | работает (сраб. 3.2/бой; Δбоя в NORMAL,HARSH) | — | ок | работает |
| POWER_BEACON_GLEAM | Отсвет маяка | % | ADD | LAMP_ROCK_SHOOT[10..15] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_VOID_RESONANCE | Резонанс пустоты | % | ADD | MISERLY_HOLLOW[20..25]; STAFF_OF_THE_BOTTOMLESS_DARK[20..25]; THE_AUGURS_COUNTING_BONES[15..20] · Σмакс≈70 | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 461 тиков; Δбоя в NORMAL,HARSH,TOTEM) | счёт≤4 | ок | работает |
| POWER_HARP_DIRGE | Песнь утопленников | % | ADD | DROWNED_SINEW_BOW[8..12] | бой героя (PowerRunner on=CRIT); CRIT/HEX; Мощь: нет | работает (сраб. 20.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_HARP_OPENING_CHORD | Первый аккорд | % | ADD | DROWNED_SINEW_BOW[20..30] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_GALE_SHATTER | Раскол | % | ADD | BLIZZARD_RECURVE[5..8] | бой героя (PowerRunner on=KILL); KILL/DAMAGE; Мощь: нет | работает (сраб. 1.3/бой; Δбоя в NORMAL,HARSH) | — | ок | работает |
| POWER_GALE_SPOILS | Вмёрзшая добыча | % | ADD | BLIZZARD_RECURVE[40..60] | заход сервера (Powers.loot RARITY) + сводка; WORLD/RARITY; Мощь: — | Powers.loot: RARITY [RARE, UNIQUE] → по редкостям ок; сводка(null)=0.0 | — | ок | работает |
| POWER_COUNCIL_VERDICT | Общий приговор | % | ADD | SENTENCE_FOR_ALL[-20..-20] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_DAMAGE:30.0->4.0 | — | ок | работает |
| POWER_SUNLIT_ARROWS | Солнечные стрелы | % | ADD | SENTENCE_FOR_ALL[40..60] | бой героя (PowerRunner on=HIT); HIT/DAMAGE; Мощь: нет | работает (сраб. 36.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждое 4-е | ок | работает |
| POWER_FEATHER_WEIGHT | Невесомость | % | ADD | CLIFFS_DROP[-40..-40] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_DAMAGE:30.0->-22.0 | — | ок | работает |
| POWER_FEATHER_FLURRY | Вихрь перьев | % шанс | ADD | CLIFFS_DROP[20..30] | бой героя (PowerRunner on=KILL); KILL/RUSH; Мощь: нет | работает (сраб. 1.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_MIRAGE_DOUBLE | Двойной образ | % | ADD | THE_SWORD_THAT_ISNT[100..100] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_PHYSICAL:200.0->400.0 STOCK_ATTACK_FIRE:50.0->100.0 STOCK_ATTACK_LIGHTNING:50.0->100.0 STOCK_ATTA | — | ок | работает |
| POWER_MIRAGE_DRAG | Марево | % | ADD | THE_SWORD_THAT_ISNT[-30..-30] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_SPEED:1.5->1.0 | — | ок | работает (PW2-16) |
| POWER_COAL_KINDLE | Самосожжение | с | ADD | BURNING_CHIEFTAINS_HILT[6..6] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/AILMENT; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок (8 = потолок/ролл; 1% = combat.selfBurn 0.01) | работает |
| POWER_COAL_FERVOUR | Жгучий пыл | % | ADD | BURNING_CHIEFTAINS_HILT[40..50] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 265 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | ЧИСЛА ≠ контент | работает (PW2-01) |
| POWER_COAL_HEART | Угольное сердце | % | ADD | BURNING_CHIEFTAINS_HILT[20..20] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_PHYSICAL:200.0->160.0 STOCK_ATTACK_FIRE:50.0->90.0 | — | ок | работает |
| POWER_RIME_DISCHARGE | Ледяной разряд | % | ADD | GRUDGING_FROST[110..130] | бой героя (PowerRunner on=CRIT); CRIT/DAMAGE; Мощь: нет | работает (сраб. 25.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | откат 0.25 с | ок | работает |
| POWER_RIME_BRITTLE | Хрупкий иней | % шанс | ADD | GRUDGING_FROST[15..25] | бой героя (PowerRunner on=CRIT); CRIT/AILMENT; Мощь: нет | работает (сраб. 5.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_HERON_STANCE | Стойка цапли | % | ADD | STILLWATER_HERON[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_DAMAGE:30.0->36.0 | — | ок | работает |
| POWER_HERON_STRIKE | Удар цапли | % шанс | ADD | STILLWATER_HERON[25..35] | бой героя (PowerRunner on=EVADE); EVADE/RUSH; Мощь: нет | работает (сраб. 1.5/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | шанс≥100 = всегда | ок | работает |
| POWER_HOUND_BITE | Хватка ищейки | % | ADD | BLOOD_TRAIL[2..3] | бой героя (PowerRunner on=CRIT); CRIT/HEAL; Мощь: нет | работает (сраб. 19.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_DARK_CONSUMPTION | Чёрное пламя | % | ADD | LIGHTLESS_CANKER[15..15] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_FIRE:50.0->42.5 STOCK_ATTACK_CHAOS:0.0->7.5 | — | ок | работает |
| POWER_DARK_FEAST | Тёмный пир | % | ADD | LIGHTLESS_CANKER[2..3] | бой героя (PowerRunner on=KILL); KILL/HEAL; Мощь: нет | работает (сраб. 3.2/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_ALTAR_WRATH | Гнев алтаря | % | ADD | THE_ALTARS_ANSWER[20..30] | бой героя (PowerRunner on=KILL); KILL/BUFF; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_ALTAR_VAAL_SIGHT | Взгляд Ваал | % | ADD | THE_ALTARS_ANSWER[4..6]; SPURNED_OFFERING[5..6] · Σмакс≈12 | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_DAMAGE:30.0->48.0 | — | ок | работает |
| POWER_BONE_BINDING | Костяная связка | % | ADD | OSSUARY_ROD[10..15] | бой героя (PowerRunner on=KILL); KILL/BUFF; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 3 | ок | работает |
| POWER_BONE_BURST | Костяной взрыв | % | ADD | OSSUARY_ROD[10..15] | бой героя (PowerRunner on=KILL); KILL/DAMAGE; Мощь: нет | работает (сраб. 12.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_ANVIL_SPARK | Бесцветное пламя | число | ADD | COLOURLESS_FLAME(M)[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_RANDOM_ELEMENT_HITS:0.0->1.0 | — | ок | работает |
| POWER_ANVIL_FORGE | Горн Бездны | % | ADD | COLOURLESS_FLAME(M)[80..100] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_FIRE:50.0->250.0 | — | ок | работает |
| POWER_FEAST_CORPSE_BLAST | Лопнувший паёк | % | ADD | UNENDING_SIEGE[8..10] | бой героя (PowerRunner on=KILL); KILL/DAMAGE; Мощь: нет | работает (сраб. 12.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_FEAST_GORGE | Обжорство | % | ADD | UNENDING_SIEGE[6..8] | бой героя (PowerRunner on=KILL); KILL/BUFF; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 3 | ок | работает |
| POWER_VENT_ERUPTION | Извержение | % шанс | ADD | SULPHUR_WHETTED_AXE[15..20] | бой героя (PowerRunner on=HIT); HIT/DAMAGE; Мощь: нет | работает (сраб. 34.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | откат 0.5 с, шанс≥100 = всегда | ок | работает |
| POWER_VENT_MAW | Серное жерло | % | ADD | SULPHUR_WHETTED_AXE[50..50] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_PHYSICAL:200.0->100.0 STOCK_ATTACK_FIRE:50.0->150.0 | — | ок | работает |
| POWER_STONE_RESOLVE | Тупая решимость | FLAG | ADD | RELENTLESS_BOULDER[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_CRITICAL_CHANCE:20.0->0.0 STOCK_SPELL_CRITICAL_CHANCE:20.0->0.0 (ролл не влияет) | — | ок | работает |
| POWER_STONE_CRUSH | Каменный натиск | % шанс | ADD | RELENTLESS_BOULDER[15..20] | бой героя (PowerRunner on=HIT); HIT/STUN; Мощь: нет | работает (сраб. 16.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_DISFAVOUR_REND | Разрыв | % шанс | ADD | SPURNED_OFFERING[40..50] | бой героя (PowerRunner on=HIT); HIT/AILMENT; Мощь: нет | работает (сраб. 32.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 2, шанс≥100 = всегда | ок | работает |
| POWER_TROLL_BELLOW | Рёв тролля | % шанс | ADD | STONETHROATS_ROAR[30..40] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/STUN; Мощь: нет | работает (сраб. 0.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_LADLE_SHARE | Общая похлёбка | % | ADD | PROVIDERS_CLEAVER[1..2] | бой героя (PowerRunner on=HIT); HIT/HEAL; Мощь: нет | работает (сраб. 79.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_LADLE_BROTH | Черпак бульона | % | ADD | PROVIDERS_CLEAVER[2..3] | бой героя (PowerRunner on=KILL); KILL/HEAL; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH) | — | ок | работает |
| POWER_BLIND_DEVOTION | Слепая вера | % | ADD | HELM_OF_THE_SIGHTLESS_SHEPHERD[50..60]; SCEPTRE_OF_THE_DEATHLESS_LORD[50..60] · Σмакс≈120 | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_DAMAGE:30.0->66.0 | — | ок | работает |
| POWER_ABBOT_BLINDNESS | Ослепляющий взор | % шанс | ADD | HELM_OF_THE_SIGHTLESS_SHEPHERD[15..25] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/HEX; Мощь: нет | работает (сраб. 9.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ЧИСЛА ≠ контент | работает (PW2-01) |
| POWER_BILGE_SCURRY | Шмыг | % | ADD | BILGECRAWLERS_CAP[8..12] | бой героя (PowerRunner on=EVADE); EVADE/BUFF; Мощь: нет | работает (сраб. 2.5/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_BILGE_PLAGUE | Трюмная чума | % шанс | ADD | BILGECRAWLERS_CAP[10..15] | бой героя (PowerRunner on=HIT); HIT/AILMENT; Мощь: нет | работает (сраб. 14.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_BARON_MARROW | Костный мозг барона | число | ADD | BONE_COUNTS_CROWN[2..2] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_HEALTH:3000.0->3060.0 | — | ок | работает |
| POWER_BARON_LEGION | Легион катакомб | число | ADD | BONE_COUNTS_CROWN[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_MAX_ENDURANCE_CHARGES:0.0->2.0 (ролл не влияет) | потолок 2 | ок | работает |
| POWER_VISAGE_ABYSS_GAZE | Взгляд Бездны | % | ADD | MASK_OF_THE_MUTE_DEEP[8..12] | бой героя (PowerRunner on=KILL); KILL/HEX; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_VISAGE_DEPTHS | Открытые глубины | % | ADD | MASK_OF_THE_MUTE_DEEP[25..35] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 102 тиков; Δбоя в HARSH,TOTEM) | — | ок | работает |
| POWER_LAMP_SPIRIT_BURST | Всплеск духа | % шанс | ADD | BEACONSNUFFERS_COWL[10..15] | бой героя (PowerRunner on=KILL); KILL/DAMAGE; Мощь: нет | работает (сраб. 6.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_LAMP_HOARD | Украденные огни | % | ADD | BEACONSNUFFERS_COWL[4..6] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_DAMAGE:30.0->42.0 | — | ок | работает |
| POWER_THORN_PRICK | Укол шипа | % | ADD | FENBRIAR_CIRCLET[0.5..0.5] | бой героя (PowerRunner on=HIT); HIT/HURT; Мощь: нет | работает (сраб. 69.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_THORN_RETORT | Колючий ответ | % | ADD | FENBRIAR_CIRCLET[10..15] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/RETALIATE; Мощь: нет | работает (сраб. 106.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_TRIBUNAL_MIND | Разум над плотью | % | ADD | BROW_OF_THE_DUNE_JUDGE[18..22] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_MANA_BEFORE_LIFE:0.0->22.0 | — | ок | работает |
| POWER_TRIBUNAL_VERDICT | Приговор | % | ADD | BROW_OF_THE_DUNE_JUDGE[20..30]; SCEPTRE_OF_THE_THUNDER_COURT[20..30] · Σмакс≈60 | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 127 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_INDIGO_MIND | Индиговый разум | % | ADD | SLATE_HAZE[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_SPELL_DAMAGE:60.0->160.0 | потолок 150 | ок | работает |
| POWER_INDIGO_SURGE | Шёпот прилива | % | ADD | SLATE_HAZE[8..12] | бой героя (PowerRunner on=SKILL_USE); SKILL_USE/BUFF; Мощь: нет | работает (сраб. 4.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_SAILCLOTH_FAIR_WIND | Попутный ветер | % | ADD | CASTAWAYS_RAGS[10..15] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_SAILCLOTH_SALVAGE | Выброшенное морем | % | ADD | CASTAWAYS_RAGS[10..15] | заход сервера (Powers.loot GOLD) + сводка; WORLD/GOLD; Мощь: — | Powers.loot: GOLD [] → по редкостям ок; сводка(null)=15.0 | — | ок | работает |
| POWER_PROTOTYPE_BULWARK | Опытный оплот | число | ADD | FIRSTFORGED_CUIRASS[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_HIGHEST_RESIST_TAKEN:0.0->1.0 | — | ок | работает |
| POWER_PROTOTYPE_PLATING | Обшивка Ваал | % | ADD | FIRSTFORGED_CUIRASS[4..6] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ARMOR:3000.0->3540.0 | — | ок | работает |
| POWER_COIL_GROUNDING | Заземление | % | ADD | STORMROD_MAIL[30..40] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_PHYSICAL_TAKEN_AS_LIGHTNING:0.0->40.0 | — | ок | работает |
| POWER_COIL_DISCHARGE | Разряд | % шанс | ADD | STORMROD_MAIL[15..20]; WINGS_OF_THE_THUNDER_KITE[15..20] · Σмакс≈40 | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/AILMENT; Мощь: нет | работает (сраб. 7.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_FLAMECLOAK_HEARTH | Очаг Рубежа | % | ADD | BORDERFIRE_MANTLE[35..40] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_PHYSICAL_TAKEN_AS_FIRE:0.0->40.0 | — | ок | работает |
| POWER_FLAMECLOAK_EMBERS | Плащ углей | % шанс | ADD | BORDERFIRE_MANTLE[10..15] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/AILMENT; Мощь: нет | работает (сраб. 5.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_DEFIANCE_WILL | Воля изгнанника | % | ADD | ARMOUR_OF_THE_UNBROKEN[25..30] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_MANA_BEFORE_LIFE:0.0->30.0 | — | ок | работает |
| POWER_DEFIANCE_RESOLVE | Несломленный | % | ADD | ARMOUR_OF_THE_UNBROKEN[4..6] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 127 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_THICKET_GRACE | Грация чащи | % | ADD | SYLVAN_MATRONS_BARK[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_CAST_SPEED:10.0->11.5 STOCK_MOVEMENT_SPEED:0.0->3.0 | — | ок | работает (PW2-16, PW2-18) |
| POWER_QUEEN_BARE_BARK | Голая кора | FLAG | ADD | SYLVAN_MATRONS_BARK[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ARMOR:3000.0->0.0 (ролл не влияет) | — | ок | работает |
| POWER_STORMBROKEN_BURST | Сердце грозы | % | ADD | CRACKED_THUNDERSHELL[4..6] | бой героя (PowerRunner on=KILL); KILL/DAMAGE; Мощь: нет | работает (сраб. 5.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_STORMBROKEN_PULSE | Сбитый пульс | % шанс | ADD | CRACKED_THUNDERSHELL[10..15] | бой героя (PowerRunner on=HIT); HIT/AILMENT; Мощь: нет | работает (сраб. 12.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_FORGE_DOME_TEMPER | Закалённый купол | FLAG | ADD | BRASS_FURNACE_PLATE[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_CRITICAL_TAKEN:0.0->-100.0 (ролл не влияет) | — | ок | работает |
| POWER_FORGE_DOME_ANVIL | Под молотом | % | ADD | BRASS_FURNACE_PLATE[5..8] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/BUFF; Мощь: нет | работает (сраб. 51.3/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 5 | ок | работает |
| POWER_DOCTOR_TRANSFUSION | Переливание | % | ADD | CORPSEHEALERS_MANTLE[15..20] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ENERGY_SHIELD:1000.0->1600.0 | — | ок | работает |
| POWER_DOCTOR_MIASMA | Миазмы | % шанс | ADD | CORPSEHEALERS_MANTLE[20..25] | бой героя (PowerRunner on=HIT); HIT/AILMENT; Мощь: нет | работает (сраб. 19.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_EMBALMED | Прививка хаоса | FLAG | ADD | WRAPS_OF_THE_INCORRUPTIBLE[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_HEALTH:3000.0->1.0 STOCK_CHAOS_IMMUNE:0.0->1.0 (ролл не влияет) | — | ок | работает |
| POWER_EMBALMERS_SEAL | Печать бальзамировщика | с | ADD | WRAPS_OF_THE_INCORRUPTIBLE[1..1.5] | бой героя (PowerRunner on=SHIELD_BROKEN); SHIELD_BROKEN/INVULNERABLE; Мощь: нет | работает (сраб. 1.7/бой; Δбоя в HARSH,TOTEM) | откат 20 с | ок | работает |
| POWER_RIMESORROW_GRIP | Ледяная хватка | % | ADD | FROSTWIDOWS_MITTS[100..100] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_PHYSICAL:200.0->0.0 STOCK_ATTACK_COLD:10.0->210.0 | — | ок | работает |
| POWER_RIMESORROW_NUMB | Немеющие слёзы | % шанс | ADD | FROSTWIDOWS_MITTS[15..25] | бой героя (PowerRunner on=HIT); HIT/HEX; Мощь: нет | работает (сраб. 20.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ЧИСЛА ≠ контент | работает (PW2-01) |
| POWER_CRYSTAL_STRENGTH | Кристальная мощь | % | ADD | GEMCUTTERS_GLOVES[1..1]; THE_STOLEN_SCEPTRE[1..1] · Σмакс≈2 | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_DAMAGE:30.0->60.0 | — | ок | работает |
| POWER_CRYSTAL_MIND | Кристальный разум | число | ADD | GEMCUTTERS_GLOVES[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ENERGY_SHIELD:1000.0->1150.0 | — | ок | работает |
| POWER_CRYSTAL_GRACE | Кристальная грация | число | ADD | GEMCUTTERS_GLOVES[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_EVASION:3000.0->3300.0 | — | ок | работает |
| POWER_CARESS_DEVOTION | Осквернённая преданность | % | ADD | GRASP_OF_THE_OFFERING_STONE[5..6] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ENERGY_SHIELD:1000.0->1120.0 STOCK_EVASION:3000.0->3360.0 STOCK_ARMOR:3000.0->3360.0 STOCK_DAMAGE:30.0-> | потолок 48, потолок 32, потолок 32, потолок 32 | ок (8 = потолок/ролл; 1% = combat.selfBurn 0.01) | работает |
| POWER_ALTAR_SCORN | Презрение алтаря | % | ADD | GRASP_OF_THE_OFFERING_STONE[-10..-10] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист:  (ролл не влияет) | — | ок | работает |
| POWER_DEEP_EMBRACE | Объятие глубин | % | ADD | EMBRACE_OF_THE_SUNKEN[3..4] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 461 тиков; Δбоя в NORMAL,HARSH,TOTEM) | счёт≤10, счёт≤10, счёт≤10 | ок | работает |
| POWER_KEEPER_OARS | Ритм гребца | % | ADD | BEACONWARDENS_PALMS[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_SPEED:1.5->1.7 | — | ок | работает (PW2-16) |
| POWER_KEEPER_LOGBOOK | Вахтенный журнал | % | ADD | BEACONWARDENS_PALMS[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_SPELL_DAMAGE:60.0->90.0 | — | ок | работает |
| POWER_ORACLE_CURSE | Нежное касание | % шанс | ADD | SEERESSS_FINGERS[20..30] | бой героя (PowerRunner on=HIT); HIT/CURSE; Мощь: нет | работает (сраб. 22.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_ORACLE_DOOM | Назначенная смерть | % | ADD | SEERESSS_FINGERS[20..25] | бой героя (PowerRunner on=KILL); KILL/DAMAGE; Мощь: нет | НЕ срабатывает (6 сидов × 3 сцены + ALT) | — | ок | мёртвая (PW2-02) |
| POWER_ROOTS_GROUNDED | Вросший | FLAG | ADD | ROOTBOUND_BOOTS[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_EVASION:3000.0->0.0 (ролл не влияет) | — | ок | работает |
| POWER_ROOTS_REGROWTH | Отрастание | % | ADD | ROOTBOUND_BOOTS[2..3] | бой героя (PowerRunner on=EVERY); EVERY/HEAL; Мощь: нет | работает (сраб. 9.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 5 с | ок | работает |
| POWER_TURNCOAT_BETRAYAL | Измена | число | ADD | DEFECTORS_TREADS[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_CHAOS:0.0->37.0 | — | ок | работает |
| POWER_TURNCOAT_SWITCH | Сменённое знамя | % шанс | ADD | DEFECTORS_TREADS[15..20] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/RETALIATE; Мощь: нет | работает (сраб. 20.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_PLAGUE_TRAIL_HOARD | Самоцветный след | % | ADD | PESTILENT_STRIDE[3..4] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_DAMAGE:30.0->38.0 | — | ок | работает |
| POWER_PLAGUE_TRAIL_BURST | Зараза | % шанс | ADD | PESTILENT_STRIDE[10..10] | бой героя (PowerRunner on=KILL); KILL/DAMAGE; Мощь: нет | работает (сраб. 2.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_RIFT_STEP | Шаг разлома | % | ADD | UNDERSIDE_STRIDERS[5..5] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 461 тиков; Δбоя в NORMAL,HARSH,TOTEM) | счёт≤10 | ок | работает |
| POWER_STORMWAKE_BOLT | След грозы | % | ADD | DRY_THUNDER_BOOTS[25..35] | бой героя (PowerRunner on=EVERY); EVERY/AILMENT+DAMAGE; Мощь: нет | работает (сраб. 49.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 2 с | ок | работает |
| POWER_STORMWAKE_STRIDE | Горящий песок | % | ADD | DRY_THUNDER_BOOTS[20..30] | бой героя (PowerRunner on=EVADE); EVADE/BUFF; Мощь: нет | работает (сраб. 2.8/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_LUCKDASH_GAMBLE | Бросок монеты | FLAG | ADD | DICERS_SHOES[1..1] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ЧИСЛА ≠ контент | частично (PW2-01, PW2-10) |
| POWER_LUCKDASH_WINDFALL | Удачный куш | % | ADD | DICERS_SHOES[15..25] | заход сервера (Powers.loot GOLD) + сводка; WORLD/GOLD; Мощь: — | Powers.loot: GOLD [] → по редкостям ок; сводка(null)=25.0 | — | ок | работает |
| POWER_LOYAL_BANNER | Знамя верных | % | ADD | WINGS_OF_THE_UNYIELDING[4..5] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_HEALTH:3000.0->3360.0 STOCK_ENERGY_SHIELD:1000.0->1200.0 STOCK_EVASION:3000.0->3600.0 STOCK_ARMOR:3000.0 | потолок 60, потолок 60, потолок 60, потолок 36 | ок | работает |
| POWER_LOYAL_OATH | Верность клятве | % | ADD | WINGS_OF_THE_UNYIELDING[15..20] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_CARRION_FEAST | Пир падальщицы | % | ADD | GRAVEPICKERS_WINGS[2..3] | бой героя (PowerRunner on=EVADE); EVADE/HEAL; Мощь: нет | работает (сраб. 2.5/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_RAIDER_CHARGE | Налёт | % | ADD | PLUNDERERS_WINGS[20..30] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_RAIDER_PLUNDER | Добыча налётчика | % | ADD | PLUNDERERS_WINGS[20..30] | заход сервера (Powers.loot GOLD) + сводка; WORLD/GOLD; Мощь: — | Powers.loot: GOLD [] → по редкостям ок; сводка(null)=30.0 | — | ок | работает |
| POWER_FERRY_TOGETHER | Переправа вдвоём | % | ADD | BOATMANS_WINGS[8..12] | бой героя (PowerRunner on=KILL); KILL/BUFF; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_FERRY_TOLL_PAID | Плата внесена | % | ADD | BOATMANS_WINGS[5..8] | бой героя (PowerRunner on=KILL); KILL/HEAL; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_WORLDBLOOD_FLOW | Бесконечный ток | число | ADD | VEIN_OF_THE_FIRST_WORLD(M)[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_FLASKS_AUTO:0.0->1.0 STOCK_FLASK_DURATION:0.0->1000.0 | — | ок | работает |
| POWER_WORLDBLOOD_THIRST | Неутолимость | FLAG | ADD | VEIN_OF_THE_FIRST_WORLD(M)[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_FLASK_RECOVERY:0.0->-100.0 (ролл не влияет) | — | ок | работает |
| POWER_TINKER_TUNING | Подогнанные кольца | % | ADD | CLOCKMAKERS_STRAP[80..100] | лист (SheetCalculator.slotted); SLOTS; Мощь: да | лист: STOCK_STRENGTH:123.8->133.3 STOCK_AGILITY:117.2->120.3 STOCK_INTELLECT:165.9->169.0 STOCK_HEALTH:1298.8->1315.2 STOCK_MA | — | ок | работает (PW2-14) |
| POWER_TINKER_GEARWORK | Шестерёнки | % шанс | ADD | CLOCKMAKERS_STRAP[10..15] | бой героя (PowerRunner on=SKILL_USE); SKILL_USE/COOLDOWNS; Мощь: нет | работает (сраб. 3.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает (PW2-08) |
| POWER_ENTHRONED_REIGN | Трон Бездны | % | ADD | BALDRIC_OF_THE_DARK_THRONE[75..100] | лист (SheetCalculator.slotted); SLOTS; Мощь: да | лист: STOCK_HEALTH:1376.8->1388.5 STOCK_MANA:655.4->683.7 STOCK_SPELL_DAMAGE:11.6->22.7 STOCK_SHOCK_CHANCE:5.0->9.8 STOCK_RECO | — | ок | работает (PW2-14) |
| POWER_ENTHRONED_SHADOW | Двор теней | % | ADD | BALDRIC_OF_THE_DARK_THRONE[2..3]; BACKBONE_OF_THE_FIRMAMENT[2..3] · Σмакс≈6 | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_RESIST_ALL:0.0->6.0 | — | ок | работает |
| POWER_SPECTRAL_LEECH | Призрачный вампиризм | число | ADD | SOULWEAVERS_BOND[1..1]; GRAVELIGHT_GEM[1..1] · Σмакс≈2 | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_LEECH_TO_SHIELD:0.0->1.0 | — | ок | работает |
| POWER_TETHER_DECAY | Истончение привязи | % | ADD | SOULWEAVERS_BOND[5..5] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_SHIELD_RECHARGE:0.0->-100.0 STOCK_SHIELD_DEGEN_PERCENT:0.0->5.0 | — | ок | работает |
| POWER_UMBILICUS_TIDE | Питающий прилив | % | ADD | CORD_OF_THE_SUNKEN_GOD[1..1.5] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 112 тиков; Δбоя в HARSH,TOTEM) | — | ок | работает |
| POWER_UMBILICUS_DROUGHT | Утонувшие фляги | FLAG | ADD | CORD_OF_THE_SUNKEN_GOD[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_FLASK_CHARGES_GAINED:0.0->-100.0 (ролл не влияет) | — | ок | работает |
| POWER_SERPENT_COIL_FORTUNE | Мах или недомах | FLAG | ADD | MARSH_ADDER_GIRDLE[1..1] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF+ONE_OF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_SERPENT_CONSTRICT | Удушение | % шанс | ADD | MARSH_ADDER_GIRDLE[8..12] | бой героя (PowerRunner on=HIT); HIT/STUN; Мощь: нет | работает (сраб. 9.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_REFLECTED_IMAGE | Отражение | % | ADD | RING_OF_THE_OTHERGLASS(M)[100..100] | лист (SheetCalculator.slotted); SLOTS; Мощь: да | лист: STOCK_ATTACK_PHYSICAL:4.0->7.5 STOCK_ATTACK_COLD:3.4->6.3 STOCK_GOLD:7.9->14.6 STOCK_RARITY:7.6->14.0 POWER_REFLECTED_IM | — | ок | работает (PW2-14) |
| POWER_REFLECTED_SHEEN | Серебряный блеск | % | ADD | RING_OF_THE_OTHERGLASS(M)[10..20] | лист (SheetCalculator.slotted); SLOTS; Мощь: да | лист: STOCK_ATTACK_PHYSICAL:6.8->7.5 STOCK_ATTACK_COLD:5.8->6.3 STOCK_GOLD:13.4->14.6 STOCK_RARITY:12.8->14.0 POWER_REFLECTED_ | — | ок | работает (PW2-14) |
| POWER_BIT_OF_LUCK | Немного удачи | FLAG | ADD | TRINKET_BAND[1..1] | бой героя (PowerRunner on=KILL); KILL/BUFF+CHARGE+HEAL+ONE_OF; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ЧИСЛА ≠ контент | работает (PW2-01) |
| POWER_BIT_OF_GOLD | Мелочь в кармане | % | ADD | TRINKET_BAND[5..10] | заход сервера (Powers.loot GOLD) + сводка; WORLD/GOLD; Мощь: — | Powers.loot: GOLD [] → по редкостям ок; сводка(null)=10.0 | — | ок | работает |
| POWER_SEAL_HARMONY | Парный рост | % | ADD | TWIN_FACET[40..50] | лист (SheetCalculator.slotted); SLOTS; Мощь: да | лист: POWER_SEAL_HARMONY:null->46.9 | — | ок | работает (PW2-14) |
| POWER_SEAL_FACET | Грань | % | ADD | TWIN_FACET[2..3]; STAIR_OF_SEVEN_HEAVENS[2..3] · Σмакс≈6 | бой героя (PowerRunner on=CRIT); CRIT/HEAL; Мощь: нет | работает (сраб. 9.3/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_TORMENT_SOLE | Лишнее кольцо | FLAG | ADD | THE_ROGUES_ONLY_RING[1..1] | лист (SheetCalculator.slotted); SLOTS; Мощь: да | лист: STOCK_STRENGTH:122.1->114.0 STOCK_INTELLECT:139.8->132.0 STOCK_HEALTH:1287.0->1283.0 STOCK_MANA:646.1->626.0 STOCK_ATTAC | — | ок | работает |
| POWER_TORMENT_BRIBE | Подкупленный стражник | % шанс | ADD | THE_ROGUES_ONLY_RING[10..15] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/CLEANSE; Мощь: нет | работает (сраб. 7.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_BROTHERHOOD_CURRENT | Холодное течение | % | ADD | MISTCROSSING_RING[40..40] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_LIGHTNING:50.0->30.0 STOCK_ATTACK_COLD:10.0->30.0 | — | ок | работает |
| POWER_BROTHERHOOD_MIST | Туман переправы | % шанс | ADD | MISTCROSSING_RING[10..15] | бой героя (PowerRunner on=HIT); HIT/HEX; Мощь: нет | работает (сраб. 10.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ЧИСЛА ≠ контент | работает (PW2-01) |
| POWER_EMERALD_VENOM | Изумрудный яд | % шанс | ADD | VERDANT_STING[10..15] | бой героя (PowerRunner on=HIT); HIT/AILMENT; Мощь: нет | работает (сраб. 14.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_EMERALD_PULSE | Зелёный пульс | % | ADD | VERDANT_STING[1..2] | бой героя (PowerRunner on=EVERY); EVERY/HEAL; Мощь: нет | работает (сраб. 9.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 5 с | ок | работает |
| POWER_AVARICE_HOARD | Капитанский сундук | % | ADD | MISERS_SIGNET[20..30] | заход сервера (Powers.loot RARITY) + сводка; WORLD/RARITY; Мощь: — | Powers.loot: RARITY [RARE, UNIQUE] → по редкостям ок; сводка(null)=0.0 | — | ок | работает |
| POWER_AVARICE_GREED | Жадность | % | ADD | MISERS_SIGNET[10..20] | заход сервера (Powers.loot GOLD) + сводка; WORLD/GOLD; Мощь: — | Powers.loot: GOLD [] → по редкостям ок; сводка(null)=20.0 | — | ок | работает |
| POWER_BLACKFLAME_PACT | Чёрное пламя | число | ADD | BLACKSMOULDER_BAND[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_IGNITE_AS_CHAOS:0.0->1.0 | — | ок | работает |
| POWER_BLACKFLAME_KINDLE | Тление | % шанс | ADD | BLACKSMOULDER_BAND[10..15] | бой героя (PowerRunner on=HIT); HIT/AILMENT; Мощь: нет | работает (сраб. 14.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_DREAMSHARD_GLINT | Отблеск | % шанс | ADD | DREAMGEODE[10..15]; STAFF_OF_THE_WANDERING_PRAYER[10..15] · Σмакс≈30 | бой героя (PowerRunner on=SKILL_USE); SKILL_USE/HEAL; Мощь: нет | работает (сраб. 7.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_DREAMSHARD_SLEEP | Усыпление | % шанс | ADD | DREAMGEODE[5..10] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/STUN; Мощь: нет | работает (сраб. 4.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_STAR_ASHFALL | Пеплопад | % | ADD | DUST_OF_THE_CAST_DOWN_STAR(M)[60..80] | бой героя (PowerRunner on=EVERY); EVERY/DAMAGE; Мощь: нет | работает (сраб. 14.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 6 с | ок | работает |
| POWER_EYELESS_SIGHT | Зрение Безглазого | % | ADD | PUPIL_OF_THE_FACELESS[10..15] | заход сервера (Powers.loot QUANTITY) + сводка; WORLD/QUANTITY; Мощь: — | Powers.loot: QUANTITY [RARE, UNIQUE] → по редкостям ок; сводка(null)=0.0 | — | ок | работает |
| POWER_EYELESS_GAZE | Немигающий взгляд | % | ADD | PUPIL_OF_THE_FACELESS[8..12] | бой героя (PowerRunner on=HIT); HIT/HEX; Мощь: нет | работает (сраб. 69.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_MAIDEN_TIDE | Жемчужный прилив | % | ADD | MERMAIDS_CAPRICE[8..12] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ENERGY_SHIELD:1000.0->1120.0 | — | ок | работает |
| POWER_MAIDEN_WHIM | Прихоть | % шанс | ADD | MERMAIDS_CAPRICE[8..12] | бой героя (PowerRunner on=SKILL_USE); SKILL_USE/COOLDOWNS; Мощь: нет | работает (сраб. 3.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает (PW2-08) |
| POWER_SHOREWATCH_VIGIL | Бдение | % | ADD | SENTINELS_LOCKET[20..30] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_SHOREWATCH_BEACON | Сторожевой огонь | % | ADD | SENTINELS_LOCKET[1..2] | бой героя (PowerRunner on=KILL); KILL/HEAL; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH) | — | ок | работает |
| POWER_SOVEREIGN_BLOOD | Кровь Владыки | % | ADD | VIAL_OF_THE_EMBER_PRINCE[20..20] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_PHYSICAL:200.0->160.0 STOCK_ATTACK_FIRE:50.0->90.0 | — | ок | работает |
| POWER_SOVEREIGN_PYRE | Погребальный костёр | % | ADD | VIAL_OF_THE_EMBER_PRINCE[4..5]; FUNERAL_FIRE_OF_MONARCHS[4..5] · Σмакс≈10 | бой героя (PowerRunner on=KILL); KILL/DAMAGE; Мощь: нет | работает (сраб. 11.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_HUSH_FROSTBITE | Обморожение | % | ADD | CRYPTSILENCE[-20..-15] | бой героя (PowerRunner on=INFLICT); INFLICT/HEX; Мощь: нет | работает (сраб. 13.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_HUSH_STILLNESS | Недвижность | % | ADD | CRYPTSILENCE[3..5]; STAFF_OF_THE_HUSHED_FROST[3..5] · Σмакс≈10 | бой героя (PowerRunner on=KILL); KILL/HEAL; Мощь: нет | работает (сраб. 0.8/бой; Δбоя в NORMAL,HARSH) | — | ок | работает |
| POWER_SPRING_BOND | Родниковая связь | % | ADD | BEASTWELL_CHARM[2..3] | бой героя (PowerRunner on=KILL); KILL/HEAL; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH) | — | ок | работает |
| POWER_SPRING_RENEWAL | Обновление | % | ADD | BEASTWELL_CHARM[4..6] | бой героя (PowerRunner on=EVERY); EVERY/HEAL; Мощь: нет | работает (сраб. 8.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 5 с | ок | работает |
| POWER_SIREN_LUCK | Удача сирены | число | ADD | SONG_OF_THE_FALSE_MAIDEN[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_LUCKY_CRIT:0.0->1.0 | — | ок | работает |
| POWER_SIREN_LURE | Ложная песнь | % | ADD | SONG_OF_THE_FALSE_MAIDEN[-40..-40] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_NON_CRIT_DAMAGE:0.0->-40.0 | — | ок | работает |
| POWER_AURORA_WARD | Покров авроры | % | ADD | BLACKSUN_AEGIS[4..6]; STAFF_OF_THE_UNBROKEN_WALL[4..6] · Σмакс≈12 | бой героя (PowerRunner on=BLOCK); BLOCK/HEAL; Мощь: нет | работает (сраб. 11.3/бой; Δбоя в HARSH,TOTEM) | — | ок | работает |
| POWER_AURORA_ECLIPSE | Полное затмение | % | ADD | BLACKSUN_AEGIS[4..6] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 6 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_GIANT_WITHER | Иссохший сок | % | ADD | DRY_HEART_OF_THE_COLOSSUS[1..1]; BOUGH_OF_THE_FOREFATHER_OAK[1..1] · Σмакс≈2 | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_SPELL_DAMAGE:60.0->160.0 (ролл не влияет) | потолок 100 | ок | работает |
| POWER_GIANT_ROOTS | Корни великана | % | ADD | DRY_HEART_OF_THE_COLOSSUS[2..3]; STAFF_OF_THE_FIRST_SPROUT[2..3] · Σмакс≈6 | бой героя (PowerRunner on=BLOCK); BLOCK/HEAL; Мощь: нет | работает (сраб. 11.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_ELDER_MIRROR | Терпение Древнего | % | ADD | MIRROR_OF_THE_PATIENT_ONE[100..100]; STAFF_OF_THE_UNBROKEN_WALL[100..100] · Σмакс≈200 | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_SPELL_BLOCK:0.0->30.0 | — | ок | работает |
| POWER_ELDER_REBUKE | Укор Древнего | % | ADD | MIRROR_OF_THE_PATIENT_ONE[50..70] | бой героя (PowerRunner on=BLOCK); BLOCK/DAMAGE; Мощь: нет | работает (сраб. 24.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_CHARITY_SHARE | Милость смотрителя | % | ADD | BULWARK_OF_THE_MERCIFUL[2..3] | бой героя (PowerRunner on=BLOCK); BLOCK/HEAL; Мощь: нет | работает (сраб. 11.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_CHARITY_RALLY | Сплочение | % | ADD | BULWARK_OF_THE_MERCIFUL[20..25] | бой героя (PowerRunner on=BLOCK); BLOCK/BUFF; Мощь: нет | работает (сраб. 13.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_GLACIER_TIP | Ледниковые наконечники | % | ADD | PERMAFROST_SIGH[25..30] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_PHYSICAL:200.0->140.0 STOCK_ATTACK_COLD:10.0->70.0 | — | ок | работает |
| POWER_GLACIER_SHARD | Ледяные осколки | % | ADD | PERMAFROST_SIGH[4..6] | бой героя (PowerRunner on=KILL); KILL/DAMAGE; Мощь: нет | работает (сраб. 1.3/бой; Δбоя в NORMAL,HARSH) | — | ок | работает |
| POWER_BEACON_FLARE | Сигнальный огонь | % | ADD | QUIVER_OF_THE_LAST_FIRE[20..25] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_PHYSICAL:200.0->150.0 STOCK_ATTACK_FIRE:50.0->100.0 | — | ок | работает |
| POWER_BEACON_CALL | Зов маяка | % | ADD | QUIVER_OF_THE_LAST_FIRE[8..12] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/HEX; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_PRISM_REFRACTION | Преломление | % | ADD | QUIVER_OF_THREE_RAYS[30..40] | бой героя (PowerRunner on=HIT); HIT/DAMAGE+ONE_OF; Мощь: нет | работает (сраб. 51.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждое 3-е | ок | работает |
| POWER_PRISM_HARMONY | Гармония | % | ADD | QUIVER_OF_THREE_RAYS[3..5] | бой героя (PowerRunner on=INFLICT); INFLICT/BUFF; Мощь: нет | работает (сраб. 54.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 3 | ок | работает |
| POWER_FLETCHING_VOID_SHOT | Выстрел пустоты | % | ADD | FEATHERS_OF_NAUGHT[50..60] | бой героя (PowerRunner on=SKILL_USE); SKILL_USE/CHARGE+DAMAGE; Мощь: нет | работает (сраб. 27.7/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_SPITE_CHILL | Холодная злоба | % | ADD | DRAUGHT_OF_OLD_GRUDGE[18..22] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (строки фляги стоят во время глотка 50 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_SPITE_BITE | Злой укус | % | ADD | DRAUGHT_OF_OLD_GRUDGE[15..20] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (строки фляги стоят во время глотка 50 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_STONEBLOOD_CLOT | Окаменевшая кровь | % | ADD | TEARS_OF_THE_STONE_FOREMOTHER(M)[20..25] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 70 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_STONEBLOOD_STILL | Замершее сердце | % | ADD | TEARS_OF_THE_STONE_FOREMOTHER(M)[10..15] | бой героя (PowerRunner on=FLASK); FLASK/BARRIER; Мощь: нет | работает (сраб. 2.5/бой, стоит 70 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_CARDINAL_SPORES | Споровая проповедь | % | ADD | SPOREBISHOPS_SACRAMENT[-10..-8] | бой героя (PowerRunner on=FLASK); FLASK/HEX; Мощь: нет | работает (сраб. 1.8/бой, стоит 40 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_CARDINAL_FAITH | Вера | % | ADD | SPOREBISHOPS_SACRAMENT[80..100] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 40 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_SUNSET_ARMY | Войско заката | число | ADD | THE_KINGS_LAST_LIGHT[2..2] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | срабатывает (0.0/бой), эффекта нет | — | хвост «во время действия» | частично (PW2-07, PW2-06) |
| POWER_SUNSET_GLORY | Последняя слава | % | ADD | THE_KINGS_LAST_LIGHT[15..20] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (строки фляги стоят во время глотка 60 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_STORM_VESSEL_CHARGE | Заряженный сосуд | % | ADD | STORM_IN_TALONS[60..80] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (строки фляги стоят во время глотка 50 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_STORM_VESSEL_DRAIN | Грозовой вампиризм | % | ADD | STORM_IN_TALONS[0.8..1] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (строки фляги стоят во время глотка 50 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_SURF_ROAR | Рёв прибоя | % | ADD | BREAKWATERS_THUNDER[15..20] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 50 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_SURF_BREAKER | Вал | % шанс | ADD | BREAKWATERS_THUNDER[30..40] | бой героя (PowerRunner on=FLASK); FLASK/STUN; Мощь: нет | работает (сраб. 0.8/бой, стоит 50 тиков; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | хвост «во время действия» | работает |
| POWER_BOG_DRAUGHT_GULP | Запретный глоток | % | ADD | FEN_WITCHS_TREAT[90..100] | бой героя (PowerRunner on=FLASK); FLASK/HEAL; Мощь: нет | работает (сраб. 3.3/бой; Δбоя в HARSH,TOTEM) | — | хвост «во время действия» | работает (PW2-06, PW2-11) |
| POWER_BOG_DRAUGHT_ROT | Болотная гниль | % | ADD | FEN_WITCHS_TREAT[15..15] | бой героя (PowerRunner on=FLASK); FLASK/BUFF; Мощь: нет | работает (сраб. 3.3/бой; Δбоя в HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_OAK_BARK | Обветренная кора | % | ADD | SAP_OF_THE_ANCIENT_OAK[-12..-8] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (строки фляги стоят во время глотка 50 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_OAK_ROOT | Глубокий корень | % | ADD | SAP_OF_THE_ANCIENT_OAK[8..12] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (строки фляги стоят во время глотка 50 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_ROT_WINE_BOON | Урожай гнили | FLAG | ADD | SOUR_PROPHECY[1..1] | бой героя (PowerRunner on=FLASK); FLASK/BUFF+ONE_OF; Мощь: нет | работает (сраб. 2.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | хвост «во время действия» | работает (PW2-01) |
| POWER_ROT_WINE_SPORES | Кислые споры | % шанс | ADD | SOUR_PROPHECY[50..70] | бой героя (PowerRunner on=FLASK); FLASK/AILMENT; Мощь: нет | работает (сраб. 1.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | хвост «во время действия» | работает |
| POWER_GHOST_CHILL | Призрачный холод | % | ADD | GRAVELIGHT_GEM[-50..-50] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_SHIELD_RECHARGE:0.0->-50.0 | — | ок | работает |
| POWER_HOLLOW_INSTINCT | Чутьё пустоты | % | ADD | CALL_OF_EMPTY_POCKETS[5..6] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ENERGY_SHIELD:1000.0->1080.0 STOCK_EVASION:3000.0->3240.0 STOCK_ARMOR:3000.0->3240.0 STOCK_ATTACK_SPEED: | — | ок | работает (PW2-18) |
| POWER_HOLLOW_LIGHTNESS | Лёгкость | % | ADD | CALL_OF_EMPTY_POCKETS[1..2] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_MOVEMENT_SPEED:0.0->4.0 | — | ок | работает |
| POWER_FINERY_GLEAM | Убор странника | % | ADD | VAGABONDS_BAUBLES[50..100] | лист (SheetCalculator.slotted); SLOTS; Мощь: да | лист: STOCK_HEALTH:1308.1->1319.3 STOCK_RESIST_COLD:6.5->12.7 POWER_FINERY_GLEAM:null->95.6 | — | ок | работает (PW2-14) |
| POWER_FINERY_TRAVEL | Бывалый | % | ADD | VAGABONDS_BAUBLES[3..5] | заход сервера (Powers.loot EXPERIENCE) + сводка; WORLD/EXPERIENCE; Мощь: — | Powers.loot: EXPERIENCE [] → по редкостям ок; сводка(null)=5.0 | — | ок | работает |
| POWER_TIMELESS_BOON | Солнце дня | FLAG | ADD | UNENDING_DAWN[1..1]; BACKBONE_OF_THE_FIRMAMENT[1..1] · Σмакс≈2 | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF+ONE_OF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ЧИСЛА ≠ контент | работает (PW2-01) |
| POWER_TIMELESS_DAWN | Новый рассвет | % | ADD | UNENDING_DAWN[2..3] | бой героя (PowerRunner on=FIGHT_CLEAR); FIGHT_CLEAR/HEAL; Мощь: нет | работает (ALT: старт с 60% здоровья) | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_BUTCHER_ANATOMY | Анатомия | число | ADD | CARVING_CHART[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_HEALTH:3000.0->3100.0 | — | ок | работает |
| POWER_INNER_TIDE_FLOW | Внутренний прилив | % | ADD | TIDE_WITHIN[15..20] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ENERGY_SHIELD:1000.0->1600.0 | — | ок | работает |
| POWER_INNER_TIDE_EBB | Отлив | % | ADD | TIDE_WITHIN[8..10] | бой героя (PowerRunner on=SHIELD_BROKEN); SHIELD_BROKEN/HEAL; Мощь: нет | работает (сраб. 2.2/бой; Δбоя в HARSH,TOTEM) | откат 15 с | ок | работает |
| POWER_QUARRY_SOLUTION | Решение каменотёса | % | ADD | THE_PICKAXE_ARGUMENT[80..100]; SCEPTRE_OF_THE_UNBROKEN_WORD[30..40] · Σмакс≈140 | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_INTELLECT:300.0->600.0 | — | ок | работает |
| POWER_QUARRY_GRIT | Выдержка | число | ADD | THE_PICKAXE_ARGUMENT[1..2]; SCEPTRE_OF_THE_STEEL_HAND[1..2] · Σмакс≈4 | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ARMOR:3000.0->3120.0 | — | ок | работает |
| POWER_SOBER_CLARITY | Трезвая ясность | % | ADD | TEETOTALERS_CLARITY[20..25] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_MANA:1000.0->1750.0 | — | ок | работает |
| POWER_SOBER_ABSTINENCE | Воздержание | % | ADD | TEETOTALERS_CLARITY[-10..-10] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_HEALTH:3000.0->2700.0 | — | ок | работает |
| POWER_FURY_UNRESTED | Неупокоенная ярость | % | ADD | GRAVELESS_WRATH[25..30] | бой героя (PowerRunner on=KILL); KILL/BUFF; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_FURY_RETURN | Возврат жнеца | % | ADD | GRAVELESS_WRATH[20..30] | бой героя (PowerRunner on=LOW_LIFE); LOW_LIFE/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в HARSH,TOTEM) | откат 10 с | ок | работает |
| POWER_MARKSMANS_FOCUS | Сосредоточение стрелка | % | ADD | FALCONERS_VOW[30..50]; TROPHIES_OF_THE_FROST_STALKER[34.5..57.5]; RING_OF_THE_KEEN_EAGLE[34.5..57.5]; древо SCN_N2_4(NOTABLE)=[15]; древо RAN_N0_2(NOTABLE)=[20] · Σмакс≈200 | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_LONE_QUARRY | Одинокая добыча | число | ADD | THE_ONLY_SHOT[120..180]; BOW_OF_THE_UPPER_SKY[138..207]; древо RAN_N0_4(NOTABLE)=[80] · Σмакс≈467 | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 137 тиков; Δбоя в NORMAL,HARSH) | — | ок | работает |
| POWER_HEADSMAN | Плата палача | % | ADD | HANGMANS_HANDS[12..18]; MEASURE_OF_RETRIBUTION[13.8..20.7]; EXECUTIONER_OF_THE_DIVINE[13.8..20.7]; древо MAR_N1_2(NOTABLE)=[8]; древо DUE_N0_2(NOTABLE)=[8] · Σмакс≈75.4 | бой героя (PowerRunner on=HIT); HIT/EXECUTE; Мощь: нет | работает; уникальных не добивает (как EXECUTE) | работает (сраб. 69.7/бой; Δбоя в NORMAL,HARSH) | — | ок | работает |
| POWER_WIND_AT_BACK | Попутный ветер | % | ADD | TAILWIND_RUNNERS[8..12]; THE_COURIERS_FINAL_GUST[9.2..13.8]; древо SCN_N3_2(NOTABLE)=[5]; древо RAN_N1_2(NOTABLE)=[5]; древо DUE_N1_4(NOTABLE)=[5] · Σмакс≈40.8 | бой героя (PowerRunner on=KILL); KILL/BUFF; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 3 | ок | работает |
| POWER_CANTORS_BREATH | Дыхание кантора | % | ADD | CHOIRSINGERS_DIADEM[20..30]; RING_OF_THE_TURNING_SPHERES[23..34.5]; древо SCN_N2_2(NOTABLE)=[10]; древо WIT_N0_2(NOTABLE)=[12] · Σмакс≈86.5 | бой героя (PowerRunner on=LOW_LIFE); LOW_LIFE/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в HARSH,TOTEM) | откат 12 с | ок | работает |
| POWER_BLACK_SAP | Чёрный сок | % | ADD | TARBLOOD_AXE[10..15]; древо DUE_N3_4(NOTABLE)=[6]; древо SHA_N0_2(NOTABLE)=[6] · Σмакс≈27 | бой героя (PowerRunner on=KILL); KILL/BUFF; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_WARDING_MANTRA | Охранная мантра | % | ADD | VESTMENT_OF_THE_MEEK[20..30]; CALM_AT_THE_STORMS_HEART[23..34.5]; древо WIT_N3_2(NOTABLE)=[15] · Σмакс≈79.5 | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_TURNING_STEEL | Отводящая сталь | % | ADD | GLANCING_BUCKLER[4..6]; HELM_OF_A_THOUSAND_REFLECTIONS[4.6..6.9]; древо RAN_N2_2(NOTABLE)=[3]; древо TEM_N1_4(NOTABLE)=[3]; древо SHA_N2_2(NOTABLE)=[3] · Σмакс≈21.9 | бой героя (PowerRunner on=BLOCK); BLOCK/BUFF; Мощь: нет | работает (сраб. 12.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 3 | ок | работает |
| POWER_SHRUG_OFF | Отмахнуться | с | ADD | STUBBORN_OX_HIDE[2..3]; CALM_AT_THE_STORMS_HEART[2.3..3.4]; древо MAR_N3_2(NOTABLE)=[2]; древо WIT_N3_4(NOTABLE)=[2] · Σмакс≈10.4 | бой героя (PowerRunner on=CRIT_TAKEN); CRIT_TAKEN/BARRIER; Мощь: нет | работает (сраб. 4.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | откат 6 с | ок | работает |
| POWER_GRANITE_HIDE | Гранитная шкура | % | ADD | STONE_COMPOSURE[-15..-10]; REEFBORN_CARAPACE[-17.2..-11.5]; древо MAR_N0_4(NOTABLE)=[-6]; древо MAR_N3_4(NOTABLE)=[-5]; древо TEM_N3_4(NOTABLE)=[-6] · Σмакс≈-38.5 | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 107 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_RECOUP_WARD | Отскок щита | % | ADD | REBOUND_RING[25..40]; FLOODLINE_BALDRIC[28.7..46]; древо WIT_N1_2(NOTABLE)=[15] · Σмакс≈101 | бой героя (PowerRunner on=SHIELD_BROKEN); SHIELD_BROKEN/HEAL; Мощь: нет | работает (сраб. 3.0/бой; Δбоя в HARSH,TOTEM) | откат 8 с | ок | работает |
| POWER_FLARE_UP | Вспышка | % | ADD | LIPS_OF_THE_TEMPEST[15..25]; THUNDERBITE[17.2..28.7]; древо TEM_N2_2(NOTABLE)=[10] · Σмакс≈63.7 | бой героя (PowerRunner on=INFLICT); INFLICT/BUFF; Мощь: нет | работает (сраб. 11.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_DEEP_FREEZE | Глубокий холод | % шанс | ADD | THE_FROSTS_PATIENCE[20..30]; UNDERTOW_HATCHET[23..34.5]; древо RAN_N3_2(NOTABLE)=[12] · Σмакс≈76.5 | бой героя (PowerRunner on=HIT); HIT/AILMENT; Мощь: нет | работает (сраб. 12.3/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждое 3-е, шанс≥100 = всегда | ок | работает |
| POWER_QUICK_ROT | Быстрая гниль | % | ADD | FESTERBUD[10..15]; древо SHA_N1_2(NOTABLE)=[6] · Σмакс≈21 | бой героя (PowerRunner on=INFLICT); INFLICT/BUFF; Мощь: нет | работает (сраб. 9.2/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_SPELLSTORM | Буря чар | число | ADD | BOTTLED_TEMPEST[20..30]; TIARA_OF_THE_HIGH_TEMPEST[23..34.5]; CIRCLET_OF_THE_UPPER_FLAME[23..34.5]; древо TEM_N0_4(NOTABLE)=[12] · Σмакс≈111 | бой героя (PowerRunner on=CRIT); CRIT/BUFF; Мощь: нет | работает (сраб. 8.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_ARCANE_TIDE | Волшебный прилив | % | ADD | TIDECRONES_SASH[8..12]; RING_OF_THE_TURNING_SPHERES[9.2..13.8]; древо WIT_N0_4(NOTABLE)=[5] · Σмакс≈30.8 | бой героя (PowerRunner on=EVERY); EVERY/HEAL; Мощь: нет | работает (сраб. 7.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 6 с | ок | работает |
| POWER_KILLING_FEVER | Горячка убийства | % шанс | ADD | YOKE_OF_THE_UNSATED[25..35]; HEART_OF_THE_THREEFOLD_STORM(M)[30..40]; BELT_OF_THE_RED_RAMPAGE[34.5..46]; древо SCN_N3_4(NOTABLE)=[12]; древо RAN_N1_4(NOTABLE)=[12]; древо DUE_N1_2(NOTABLE)=[15] · Σмакс≈160 | бой героя (PowerRunner on=KILL); KILL/CHARGE; Мощь: нет | работает (сраб. 3.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_SHROUDED_STEP | Сокрытый шаг | % | ADD | MARK_OF_THE_UNSEEN_KNIFE[8..12]; BOOTS_OF_THE_ABYSSAL_WYRM[9.2..13.8]; STEPS_ACROSS_THE_VOID[9.2..13.8]; SHADOWSTITCH_LEATHERS[6..10]; древо RAN_N2_4(NOTABLE)=[6]; древо SHA_N2_4(NOTABLE)=[6] · Σмакс≈61.6 | бой героя (PowerRunner on=EVADE); EVADE/BUFF; Мощь: нет | работает (сраб. 5.2/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | стопок 3 | ок | работает |
| POWER_MOURNING_BELL | Погребальный колокол | % | ADD | SWIFT_VERDICT[30..45]; AXE_OF_THE_BROKEN_HEAVENS[34.5..51.7]; древо MAR_N1_4(NOTABLE)=[20]; древо DUE_N0_4(NOTABLE)=[20] · Σмакс≈136.7 | бой героя (PowerRunner on=HIT); HIT/DAMAGE; Мощь: нет | работает (сраб. 33.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_PACK_RALLY | Сбор стаи | % | ADD | ROOKERY_HOOD[3..5]; WINGS_OF_THE_HOLLOW_SWARM[4.6..6.9] · Σмакс≈11.9 | бой героя (PowerRunner on=HIT); HIT/BUFF; Мощь: нет | работает (сраб. 27.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждое 3-е, стопок 5 | ок | работает |
| POWER_PACK_BOND | Узы стаи | % | ADD | PACKLEADERS_TORC[8..12] | бой героя (PowerRunner on=KILL); KILL/BUFF; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 3 | ок | работает |
| POWER_GRAVE_OATH | Могильная клятва | % | ADD | NECROMANCERS_FROZEN_HEART[25..35]; ROBES_OF_THE_DWINDLED_DEITY[28.7..40.2] · Σмакс≈75.2 | бой героя (PowerRunner on=DEATH); DEATH/HEAL; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в HARSH,TOTEM) | откат раз за бой | ок | работает |
| POWER_WHISPERED_DOOM | Шёпот гибели | % шанс | ADD | VOICE_FROM_THE_DEPTHS[40..60]; ROD_OF_THE_UNSPOKEN_WORD[46..69]; древо WIT_N2_2(NOTABLE)=[25]; древо SHA_N3_2(NOTABLE)=[25] · Σмакс≈179 | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/CURSE; Мощь: нет | работает (сраб. 0.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_RAIDERS_TOLL | Дань налётчика | % шанс | ADD | BLOOD_TITHE[25..35]; древо SHA_N1_4(NOTABLE)=[15] · Σмакс≈50 | бой героя (PowerRunner on=HIT); HIT/AILMENT; Мощь: нет | работает (сраб. 13.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждое 2-е, шанс≥100 = всегда | ок | работает |
| POWER_VAAL_DEBT | Долг Ваал | % | ADD | SHROUD_OF_THE_SLEEPING_LORD[25..35]; USURERS_BRACERS[30..40]; SASH_OF_THE_DEEP_GLUTTON[34.5..46]; древо MAR_N2_4(NOTABLE)=[12]; древо DUE_N2_2(NOTABLE)=[15] · Σмакс≈148 | бой героя (PowerRunner on=LOW_LIFE); LOW_LIFE/HEAL; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в HARSH,TOTEM) | откат раз за бой | ок | работает |
| POWER_HIVE_SONG | Песнь улья | % | ADD | SILENT_PANTHER_PAWS[5..8]; BLOODGRASP_GLOVES[5.8..9.2] · Σмакс≈17.2 | бой героя (PowerRunner on=EVADE); EVADE/BUFF; Мощь: нет | работает (сраб. 2.5/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | стопок 3 | ок | работает |
| POWER_SPORE_CLOUD | Облако спор | % шанс | ADD | MYCELIUM_CROWN[30..45] | бой героя (PowerRunner on=EVERY); EVERY/AILMENT; Мощь: нет | работает (сраб. 3.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 5 с, шанс≥100 = всегда | ок | работает |
| POWER_FURNACE_CORE | Сердце горна | % | ADD | FISTS_OF_THE_UNQUENCHED_FORGE[10..15]; FISTS_OF_THE_FURNACE_LORD[11.5..17.2]; древо DUE_N3_2(NOTABLE)=[6] · Σмакс≈38.2 | бой героя (PowerRunner on=INFLICT); INFLICT/BUFF; Мощь: нет | работает (сраб. 20.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_PLAGUE_TOUCH | Касание чумы | % шанс | ADD | PESTBEAK_VISOR[15..25] | бой героя (PowerRunner on=HIT); HIT/SPREAD; Мощь: нет | работает (сраб. 8.3/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | шанс≥100 = всегда | ок | работает |
| POWER_BLOOD_HEX | Кровавый сглаз | % шанс | ADD | RING_OF_CEASELESS_MURMUR[15..25]; SHROUD_OF_UNHEEDED_WARNINGS[10..20]; ROD_OF_THE_UNSPOKEN_WORD[17.2..28.7]; GLOVES_OF_THE_DROWNED_PRIEST[17.2..28.7]; древо WIT_N2_4(NOTABLE)=[10]; древо SHA_N3_4(NOTABLE)=[10] · Σмакс≈122.4 | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/CURSE; Мощь: нет | работает (сраб. 13.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_ENDLESS_SIEGE | Бесконечная осада | с | ADD | AGELESS_GATE[2..3] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/BARRIER; Мощь: нет | работает (сраб. 15.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждое 4-е | ок | работает |
| POWER_ABYSS_GRIN | Оскал бездны | % шанс | ADD | DROWNED_MANS_SMILE[10..20] | бой героя (PowerRunner on=KILL); KILL/CURSE; Мощь: нет | работает (сраб. 1.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_VOID_TRADE | Сделка с пустотой | с | ADD | THE_VOIDS_RECEIPT[2..3] | бой героя (PowerRunner on=KILL); KILL/BARRIER; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | откат 3 с | ок | работает |
| POWER_HUNGER_OF_DEPTHS | Голод глубин | % | ADD | GLUTTONOUS_CORD[2..4]; SASH_OF_THE_DEEP_GLUTTON[2.3..4.6] · Σмакс≈8.6 | бой героя (PowerRunner on=HIT); HIT/HEAL; Мощь: нет | работает (сраб. 44.0/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_TWIN_FANGS | Двойные клыки | % шанс | ADD | DOUBLEBITE_DIRK[15..25]; DAGGER_OF_THE_BROKEN_STAIR[17.2..28.7]; древо SHA_N0_4(NOTABLE)=[10] · Σмакс≈63.7 | бой героя (PowerRunner on=HIT); HIT/ECHO; Мощь: нет | работает (сраб. 44.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_UNDERTOW_PULL | Тяга течения | % шанс | ADD | HEART_OF_THE_WHIRLPOOL[20..30]; древо TEM_N0_2(NOTABLE)=[12] · Σмакс≈42 | бой героя (PowerRunner on=CRIT); CRIT/CHARGE; Мощь: нет | работает (сраб. 7.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_SACRIFICIAL_EDGE | Жертвенная кромка | % | ADD | THE_BLOODLETTER[40..60] | бой героя (PowerRunner on=CRIT); CRIT/DAMAGE+HURT; Мощь: нет | работает (сраб. 33.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_WHETTED | Наточенный | % | ADD | SHARPENERS_TALISMAN[40..60] | бой героя (PowerRunner on=HIT); HIT/DAMAGE; Мощь: нет | работает (сраб. 8.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | частично (PW2-12) |
| POWER_CARTOGRAPHERS_LUCK | Везение землемера | % | ADD | WAYSEEKERS_NEEDLE[15..25]; WINGS_OF_THE_FAR_HORIZON[20..30]; KEY_TO_THE_THIRTEENTH_DOOR[23..34.5]; PATHFINDERS_ROSARY[23..34.5] · Σмакс≈124 | заход сервера (Powers.loot MAP) + сводка; WORLD/MAP; Мощь: — | Powers.loot: MAP [] → по редкостям ок; сводка(null)=34.5 | — | ок | работает |
| POWER_TRAILFINDER | Чтец следов | % | ADD | THOUSAND_LEAGUE_SOLES[15..25]; PATHFINDERS_ROSARY[17.2..28.7] · Σмакс≈53.7 | заход сервера (Powers.loot EXPERIENCE) + сводка; WORLD/EXPERIENCE; Мощь: — | Powers.loot: EXPERIENCE [RARE] → по редкостям ок; сводка(null)=0.0 | — | ок | работает |
| POWER_ROAD_WEARY | Утомлённый путём | % | ADD | THOUSAND_LEAGUE_SOLES[2.5..4]; WINGS_OF_THE_FAR_HORIZON[3..5]; DAGGER_OF_THE_BROKEN_STAIR[3.5..5.8]; GREAVES_OF_THE_STUBBORN_CLIMBER[3.5..5.8] · Σмакс≈20.6 | бой героя (PowerRunner on=FIGHT_CLEAR); FIGHT_CLEAR/BUFF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 5 | ок | работает (PW2-15) |
| POWER_CRYSTAL_SIGHT | Кристальный взор | % | ADD | FACETED_GAZE[30..50]; THUNDERBITE[34.5..57.5]; HOOD_OF_THE_NIGHT_WATCHER[34.5..57.5]; древо TEM_N2_4(NOTABLE)=[20] · Σмакс≈185 | бой героя (PowerRunner on=HIT); HIT/BUFF; Мощь: нет | работает (сраб. 47.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_PRISM_CLEAVE | Призменный разруб | % | ADD | FROSTVEIN_BLADE[30..45]; TROPHIES_OF_THE_FROST_STALKER[34.5..51.7]; древо RAN_N3_4(NOTABLE)=[18] · Σмакс≈114.7 | бой героя (PowerRunner on=CRIT); CRIT/DAMAGE; Мощь: нет | работает (сраб. 44.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_STAR_HEART | Звёздное сердце | % | ADD | STONE_OF_LONG_WAITING[4..6]; VEIL_OF_QUIET_ASH[2..3]; древо WIT_N1_4(NOTABLE)=[2] · Σмакс≈11 | бой героя (PowerRunner on=EVERY); EVERY/HEAL; Мощь: нет | работает (сраб. 9.5/бой; Δбоя в HARSH,TOTEM) | каждые 5 с | ок | работает |
| POWER_ATLAS_BURDEN | Ноша Атласа | % | ADD | GIRDLE_OF_THE_WORLDS_WEIGHT[10..20]; THE_RAVENOUS_MISERS_RING[11.5..23] · Σмакс≈43 | заход сервера (Powers.loot UNIQUE) + сводка; WORLD/UNIQUE; Мощь: — | Powers.loot: UNIQUE [] → по редкостям ок; сводка(null)=23.0 | — | ок | работает |
| POWER_FALLING_SKY | Падающее небо | % | ADD | ROD_OF_FALLING_STARS[80..120]; HOOD_OF_THE_NIGHT_WATCHER[92..138]; BOW_OF_THE_UPPER_SKY[92..138] · Σмакс≈396 | бой героя (PowerRunner on=EVERY); EVERY/DAMAGE; Мощь: нет | работает (сраб. 12.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 7 с | ок | работает |
| POWER_ECLIPSE_VEIL | Покров затмения | с | ADD | CROWN_OF_THE_SWALLOWED_SUN(M)[1..1.5]; TIARA_OF_THE_HIGH_TEMPEST[1.1..1.7]; ROBES_OF_THE_DWINDLED_DEITY[1.1..1.7] · Σмакс≈4.9 | бой героя (PowerRunner on=EVERY); EVERY/INVULNERABLE; Мощь: нет | работает (сраб. 5.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 10 с | ок | работает |
| POWER_SOVEREIGN_ORDER | Указ владыки | % | ADD | CROWN_OF_THE_SWALLOWED_SUN(M)[15..25]; THE_CROWN_BELOW_THE_WAVES[17.2..28.7]; MEASURE_OF_RETRIBUTION[17.2..28.7] · Σмакс≈82.4 | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/HEX; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_KINGDOM_HUNGER | Голод по королевствам | % | ADD | DEVOURER_OF_THRONES(M)[5..8]; EXECUTIONER_OF_THE_DIVINE[5.8..9.2] · Σмакс≈17.2 | бой героя (PowerRunner on=KILL); KILL/BUFF; Мощь: нет | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 5 | ок | работает |
| POWER_ENDURING_BLOCK | Стойкий блок | % шанс | ADD | AEGIS_OF_THE_UNDYING_MORNING(M)[30..40]; WARD_OF_THE_UNYIELDED_CROSSING[34.5..46]; древо MAR_N2_2(NOTABLE)=[15]; древо TEM_N1_2(NOTABLE)=[12] · Σмакс≈113 | бой героя (PowerRunner on=BLOCK); BLOCK/CHARGE; Мощь: нет | работает (сраб. 6.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_TEMPERED_SOUL | Закалённая душа | % | ADD | AEGIS_OF_THE_UNDYING_MORNING(M)[30..50]; BANNER_BLADE_OF_THE_IRON_RANK[34.5..57.5]; древо MAR_N0_2(NOTABLE)=[20]; древо TEM_N3_2(NOTABLE)=[20] · Σмакс≈147.5 | бой героя (PowerRunner on=BLOCK); BLOCK/BUFF; Мощь: нет | работает (сраб. 11.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_WILDFIRE_SPREAD | Дикий огонь | % шанс | ADD | CENSER_OF_THE_BLIGHTED_BLOOM(M)[40..60] | бой героя (PowerRunner on=KILL); KILL/SPREAD; Мощь: нет | работает (сраб. 2.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_SULPHUR_BREATH | Серное дыхание | % | ADD | CENSER_OF_THE_BLIGHTED_BLOOM(M)[30..50] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/DAMAGE; Мощь: нет | работает (сраб. 72.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | откат 1 с | ок | работает |
| POWER_CHARGED_EDGE | Заряженная кромка | % | ADD | HEART_OF_THE_THREEFOLD_STORM(M)[40..60] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 115 тиков; Δбоя в NORMAL,HARSH; ALT) | — | ок | работает |
| POWER_IRON_BLOOD | Железная кровь | % | ADD | PLATE_OF_THE_UNSCALED_PEAK[10..15]; древо MAR_K7(KEYSTONE)=[2] · Σмакс≈17 | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_LIFE_RECOUP:0.0->25.0 | потолок 25 | ок | работает |
| POWER_COLD_CALCULUS | Холодный расчёт | % | ADD | древо RAN_K7(KEYSTONE)=[1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_DOUBLE_DAMAGE:0.0->10.0 | потолок 40 | ок | работает |
| POWER_WARDSTONE | Камень-оберег | % | ADD | CIRCLET_OF_THE_UPPER_FLAME[10..15]; древо WIT_K7(KEYSTONE)=[35] · Σмакс≈50 | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ENERGY_SHIELD:1000.0->2050.0 STOCK_EVASION:3000.0->0.0 | — | ок | работает |
| POWER_BLADE_DANCE | Танец клинков | % | ADD | древо DUE_K7(KEYSTONE)=[2] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_BLOCK_CHANCE:30.0->0.0 STOCK_DEFLECTION:0.0->12.0 | потолок 30 | ок | работает |
| POWER_SKYFORGE | Небесная кузня | % | ADD | древо TEM_K7(KEYSTONE)=[100] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_PHYSICAL:200.0->0.0 STOCK_ATTACK_LIGHTNING:50.0->250.0 | — | ок | работает |
| POWER_VENOM_HEART | Ядовитое сердце | % | ADD | древо SHA_K7(KEYSTONE)=[100] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ATTACK_PHYSICAL:200.0->0.0 STOCK_ATTACK_CHAOS:0.0->200.0 | — | ок | работает |
| POWER_SHARED_SOUL | Общая душа | % | ADD | древо SCN_K7(KEYSTONE)=[40] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_HEALTH_REGEN:20.0->0.0 STOCK_LIFE_REGEN_PERCENT:1.0->0.0 STOCK_MANA_REGEN_TO_LIFE:0.0->40.0 | — | ок | работает |
| POWER_GLACIER_STILLNESS | Застывший миг | с | ADD | GLACIER_HOURGLASS[1.5..2.5] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/DELAY; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_GLACIER_TRICKLE | Ледяная капель | число | ADD | GLACIER_HOURGLASS[2..3] | бой героя (PowerRunner on=EVERY); EVERY/COOLDOWNS; Мощь: нет | работает (сраб. 7.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 5 с | ок | работает |
| POWER_PILGRIM_PYRE | Костёр паломника | % | ADD | PYRE_PILGRIMS_TREADS[20..30] | бой героя (PowerRunner on=EVERY); EVERY/AILMENT+BUFF; Мощь: нет | работает (сраб. 8.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждые 5 с | ок | работает |
| POWER_PILGRIM_EMBERS | Тлеющий след | % шанс | ADD | PYRE_PILGRIMS_TREADS[30..40] | бой героя (PowerRunner on=EVADE); EVADE/AILMENT; Мощь: нет | работает (сраб. 1.5/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | шанс≥100 = всегда | ок | работает |
| POWER_SHARED_FEVER | Общая лихорадка | % шанс | ADD | QUIVER_OF_THE_SHARED_FEVER[15..20] | бой героя (PowerRunner on=INFLICT); INFLICT/SPREAD; Мощь: нет | работает (сраб. 1.8/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | шанс≥100 = всегда | ок | работает |
| POWER_FEVER_BLOOM | Цветение заразы | % шанс | ADD | QUIVER_OF_THE_SHARED_FEVER[30..40] | бой героя (PowerRunner on=KILL); KILL/AILMENT; Мощь: нет | работает (сраб. 1.5/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | шанс≥100 = всегда | ок | работает |
| POWER_CHEATS_DEAL | Краплёная колода | FLAG | ADD | CHEATS_RING[1..1] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/BUFF+HURT+ONE_OF; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ЧИСЛА ≠ контент | работает (PW2-01) |
| POWER_CHEATS_PURSE | Двойное дно | % | ADD | CHEATS_RING[20..30] | заход сервера (Powers.loot GOLD) + сводка; WORLD/GOLD; Мощь: — | Powers.loot: GOLD [] → по редкостям ок; сводка(null)=30.0 | — | ок | работает |
| POWER_MARTYRS_RECKONING | Ответ мученика | % | ADD | MARTYRS_BULWARK[40..60] | бой героя (PowerRunner on=BLOCK); BLOCK/DAMAGE; Мощь: нет | работает (сраб. 32.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_MARTYRS_VIGIL | Последнее бдение | % | ADD | MARTYRS_BULWARK[20..25] | бой героя (PowerRunner on=LOW_LIFE); LOW_LIFE/BARRIER; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в HARSH,TOTEM) | откат 20 с | ок | работает |
| POWER_STORMLINK_ARC | Дуга грозы | % шанс | ADD | STORMLINK_GAUNTLETS[40..60] | бой героя (PowerRunner on=CRIT); CRIT/AILMENT; Мощь: нет | работает (сраб. 15.3/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_STORMLINK_CHAIN | Звено цепи | % шанс | ADD | STORMLINK_GAUNTLETS[50..70] | бой героя (PowerRunner on=HIT); HIT/NEXT_CRIT; Мощь: нет | работает (сраб. 11.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждое 5-е, шанс≥100 = всегда | ок | работает |
| POWER_PENITENT_ABSOLUTION | Отпущение | % | ADD | PENITENTS_CORD[12..18] | бой героя (PowerRunner on=FLASK); FLASK/BARRIER+CLEANSE; Мощь: нет | работает (сраб. 4.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_PENITENT_ZEAL | Рвение кающегося | % | ADD | PENITENTS_CORD[15..25] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 138 тиков; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_MIRE_VENOM | Трясинный яд | % шанс | ADD | MIRE_SERPENT_HIDE[30..40] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/AILMENT; Мощь: нет | работает (сраб. 16.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_MIRE_FEAST | Пир змея | % | ADD | MIRE_SERPENT_HIDE[2..3] | бой героя (PowerRunner on=KILL); KILL/HEAL; Мощь: нет | работает (сраб. 3.2/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_SAND_SENTENCE | Приговор | % | ADD | SCALES_OF_THE_SAND_JUDGE[10..15] | бой героя (PowerRunner on=HIT); HIT/EXECUTE; Мощь: нет | работает (сраб. 79.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_SAND_VERDICT | Вердикт | % | ADD | SCALES_OF_THE_SAND_JUDGE[30..40] | бой героя (PowerRunner on=KILL); KILL/BUFF; Мощь: нет | работает (сраб. 2.0/бой; Δбоя в NORMAL,HARSH) | — | ЧИСЛА ≠ контент | работает (PW2-01) |
| POWER_WYRM_ERUPTION | Извержение | % шанс | ADD | CREST_OF_THE_LAVA_WYRM[50..70] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/AILMENT; Мощь: нет | работает (сраб. 0.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_WYRM_CRUST | Застывшая корка | % | ADD | CREST_OF_THE_LAVA_WYRM[6..10] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/BARRIER; Мощь: нет | работает (сраб. 3.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | откат 4 с | ок | работает |
| POWER_ORRERY_TURN | Полный оборот | число | ADD | COG_OF_THE_ENDLESS_TURN[5..8] | бой героя (PowerRunner on=SKILL_USE); SKILL_USE/COOLDOWNS; Мощь: нет | работает (сраб. 4.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждое 5-е | ок | работает |
| POWER_ORRERY_ECHO | Отзвук сфер | % | ADD | COG_OF_THE_ENDLESS_TURN[100..100] | бой героя (PowerRunner on=HIT); HIT/ECHO; Мощь: нет | работает (сраб. 39.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждое 4-е | ок | работает |
| POWER_THOUSAND_CUTS | Тысяча порезов | % шанс | ADD | BLADE_OF_A_THOUSAND_CUTS[25..35] | бой героя (PowerRunner on=INFLICT); INFLICT/SPREAD; Мощь: нет | работает (сраб. 4.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_OPEN_WOUNDS | Открытые раны | % | ADD | BLADE_OF_A_THOUSAND_CUTS[4..6] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 483 тиков; Δбоя в NORMAL,HARSH,TOTEM) | счёт≤5 | ок | работает |
| POWER_TAILWIND_SURGE | Порыв | % шанс | ADD | TAILWIND_PINIONS[40..60] | бой героя (PowerRunner on=EVADE); EVADE/RUSH; Мощь: нет | работает (сраб. 2.7/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | шанс≥100 = всегда | ок | работает |
| POWER_TAILWIND_OPENING | Ветер в спину | % шанс | ADD | TAILWIND_PINIONS[60..100] | бой героя (PowerRunner on=FIGHT_START); FIGHT_START/NEXT_CRIT; Мощь: нет | работает (сраб. 1.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_SPLIT_ELEMENTS | Расколотая стихия | % шанс | ADD | SHARD_OF_THE_SPLIT_ELEMENTS[20..25] | бой героя (PowerRunner on=HIT); HIT/AILMENT+ONE_OF; Мощь: нет | работает (сраб. 18.5/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_ELEMENTAL_SPOILS | Стихийный трофей | % шанс | ADD | SHARD_OF_THE_SPLIT_ELEMENTS[30..40] | бой героя (PowerRunner on=KILL); KILL/CHARGE; Мощь: нет | работает (сраб. 3.3/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает |
| POWER_WARDEN_RESOLVE | Стойкость стража | % шанс | ADD | STEADFAST_WARDENS_BELT[15..25] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/CHARGE; Мощь: нет | работает (сраб. 9.2/бой; Δбоя в NORMAL,HARSH,TOTEM) | шанс≥100 = всегда | ок | работает (PW2-17) |
| POWER_WARDEN_BASTION | Бастион | % | ADD | STEADFAST_WARDENS_BELT[15..25] | бой героя (PowerRunner on=BLOCK); BLOCK/BARRIER+CHARGE; Мощь: нет | работает (сраб. 8.3/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает (PW2-17) |
| POWER_IDOLBREAKER | Идолобой | % | ADD | IDOLBREAKER_GRIPS[8..12] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 508 тиков; Δбоя в TOTEM) | счёт≤6 | ок | работает |
| POWER_SHAMANBANE | Гроза шаманов | % | ADD | SHAMANBANE_TALISMAN[10..15] | бой героя (PowerRunner on=TOTEM_RAISED); TOTEM_RAISED/BARRIER+CLEANSE; Мощь: нет | работает (сраб. 20.0/бой; Δбоя в TOTEM) | — | ок | частично (PW2-09) |
| POWER_IDOL_ASH | Пепел идолов | % | ADD | ASH_OF_IDOLS[6..10] | бой героя (PowerRunner on=TOTEM_FALLS); TOTEM_FALLS/HEAL; Мощь: нет | работает (сраб. 14.2/бой; Δбоя в TOTEM) | — | ок | работает |
| POWER_IDOLBREAKER_SPLINTERS | Щепа идола | FLAG | ADD | IDOLBREAKER_GRIPS[1..1] | бой героя (PowerRunner on=TOTEM_FALLS); TOTEM_FALLS/CHARGE; Мощь: нет | работает (сраб. 9.8/бой; Δбоя в TOTEM) | — | ок | работает (PW2-17) |
| POWER_SHAMANBANE_WARD | Немота шамана | % | ADD | SHAMANBANE_TALISMAN[6..10] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 480 тиков; Δбоя в TOTEM) | счёт≤3 | ок | работает |
| POWER_IDOL_ASH_EMBERS | Тлеющий идол | % | ADD | ASH_OF_IDOLS[80..120] | бой героя (PowerRunner on=TOTEM_FALLS); TOTEM_FALLS/DAMAGE; Мощь: нет | работает (сраб. 18.0/бой; Δбоя в TOTEM) | — | ок | работает |
| POWER_MOTHERS_CORD | Пуповина | % | ADD | THE_MOTHERS_CORD[3..5] | бой героя (PowerRunner on=KILL); KILL/HEAL; Мощь: нет | работает (сраб. 3.0/бой; Δбоя в NORMAL) | — | ок | работает |
| POWER_BLIGHT_SPREAD | Расползание | % | ADD | THE_MOTHERS_CORD[20..30] | заход (RunContext.feature → MapFeatures); FEATURES; Мощь: — | features → {BLIGHT_SPREAD=30.0} | — | ок | работает |
| POWER_BILE_TOUCH | Желчное касание | % шанс | ADD | BILE_CROWN[20..30] | бой героя (PowerRunner on=HIT); HIT/AILMENT; Мощь: нет | работает (сраб. 9.0/бой; Δбоя в NORMAL,HARSH) | шанс≥100 = всегда | ок | работает |
| POWER_FESTERING_HOARD | Гнойный клад | % | ADD | BILE_CROWN[15..25] | заход (RunContext.feature → MapFeatures); FEATURES; Мощь: — | features → {BLIGHT_CHEST=25.0} | — | ок | работает |
| POWER_VEIN_CHARGE | Заряд Скверны | % | ADD | VEINBLADE[4..6] | бой героя (PowerRunner on=KILL); KILL/BUFF; Мощь: нет | срабатывает; эффект - при уроне хаосом; стопок не больше убийств за бой | работает (сраб. 6.0/бой; Δбоя в NORMAL,HARSH,TOTEM) | стопок 10 | ок | частично (PW2-05) |
| POWER_VEIN_BURST | Разрыв жилы | % | ADD | VEINBLADE[100..150] | бой героя (PowerRunner on=KILL); KILL/DAMAGE; Мощь: нет | НЕ срабатывает (6 сидов × 3 сцены + ALT) | каждое 10-е | ок | практически мёртвая (PW2-05) |
| POWER_ABSCESS_BARRIER | Нарыв | % | ADD | ABSCESS_SHROUD[6..10] | бой героя (PowerRunner on=HIT_TAKEN); HIT_TAKEN/BARRIER; Мощь: нет | работает (сраб. 13.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | откат 4 с | ок | работает |
| POWER_ABSCESS_WARD | Рубцовая плоть | % | ADD | ABSCESS_SHROUD[15..25] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_BLIGHT_REDUCTION:0.0->25.0 | — | ок | работает |
| POWER_UNCLOSING_WOUNDS | Незаживающие раны | % | ADD | SURGEONS_BANE[-40..-25] | бой героя (PowerRunner on=INFLICT); INFLICT/HEX; Мощь: нет | срабатывает (проклятие ложится); эффект только у врагов с восстановлением | работает (сраб. 11.8/бой; Δбоя в NORMAL,HARSH,TOTEM) | — | ок | работает |
| POWER_SUTURE_THIEF | Похититель швов | % | ADD | SURGEONS_BANE[50..80] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_RECOVERY_SUPPRESSION:0.0->24.0 | потолок 25 | ок | работает |
| POWER_CAUTERY | Прижигание | % | ADD | CAUTERIZERS_GRIP[20..30] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_RECOVERY_SUPPRESSION:0.0->30.0 | — | ок | работает |
| POWER_LINGERING_ROT | Затяжная гниль | % | ADD | WITHERING_COIL[60..100] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_RECOVERY_SUPPRESSION:0.0->20.0 | потолок 20 | ок | работает |
| POWER_WITHER_FEAST | Пир увядания | % | ADD | WITHERING_COIL[2..4] | бой героя (PowerRunner on=KILL); KILL/HEAL; Мощь: нет | работает (сраб. 3.2/бой; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_MARSHLIGHT_GLOW | Болотный огонёк | % | ADD | MARSHLIGHT_TONIC[1..1.5] | бой героя (PowerRunner on=FLASK); FLASK/BUFF; Мощь: нет | работает (сраб. 3.0/бой; Δбоя в HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_BRAMBLE_HIDE | Терновая шкура | % | ADD | BRAMBLEHEART_DRAUGHT[8..12] | бой героя (PowerRunner on=FLASK); FLASK/BUFF; Мощь: нет | работает (сраб. 3.0/бой; Δбоя в HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_BRAMBLE_SNARE | Терновый силок | % шанс | ADD | BRAMBLEHEART_DRAUGHT[25..35] | бой героя (PowerRunner on=FLASK); FLASK/AILMENT; Мощь: нет | работает (сраб. 1.7/бой; Δбоя в HARSH,TOTEM) | шанс≥100 = всегда | хвост «во время действия» | работает |
| POWER_LAST_RATION_WARD | Последний паёк | % | ADD | WARDENS_LAST_RATION[10..15] | бой героя (PowerRunner on=FLASK); FLASK/BARRIER; Мощь: нет | работает (сраб. 3.0/бой; Δбоя в HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_LAST_RATION_RESOLVE | Стойкость стража | % | ADD | WARDENS_LAST_RATION[10..15] | бой героя (PowerRunner on=FLASK); FLASK/BUFF; Мощь: нет | работает (сраб. 3.0/бой; Δбоя в HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_EMBER_REBIRTH | Возрождение из углей | % | ADD | PHOENIX_EMBER_ELIXIR[15..20] | бой героя (PowerRunner on=FLASK); FLASK/CLEANSE+HEAL; Мощь: нет | работает (сраб. 3.0/бой; Δбоя в HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_EMBER_PYRE | Погребальный костёр | % | ADD | PHOENIX_EMBER_ELIXIR[60..80] | бой героя (PowerRunner on=FLASK); FLASK/DAMAGE; Мощь: нет | работает (сраб. 10.8/бой; Δбоя в HARSH,TOTEM) | — | хвост «во время действия» | работает |
| POWER_INKWELL_FOCUS | Сосредоточенность ученика | % | ADD | NOVICES_INKWELL[10..15] | бой героя (PowerRunner on=FLASK); FLASK/BUFF; Мощь: нет | работает (сраб. 4.2/бой; Δбоя в NORMAL,TOTEM) | — | хвост «во время действия» | работает |
| POWER_MOONWELL_SILVER | Лунное серебро | % | ADD | MOONWELL_DRAUGHT[8..12] | бой героя (PowerRunner on=FLASK); FLASK/HEAL; Мощь: нет | работает (сраб. 4.0/бой; Δбоя в TOTEM) | — | хвост «во время действия» | работает |
| POWER_MOONWELL_CALM | Лунный покой | % | ADD | MOONWELL_DRAUGHT[30..40] | бой героя (PowerRunner on=FLASK); FLASK/BUFF; Мощь: нет | работает (сраб. 4.0/бой; Δбоя в NORMAL,TOTEM) | — | хвост «во время действия» | работает |
| POWER_BITTER_INSIGHT | Горькое прозрение | % | ADD | SAGES_BITTER_INK[30..50] | бой героя (PowerRunner on=FLASK); FLASK/BUFF; Мощь: нет | работает (сраб. 4.2/бой; Δбоя в NORMAL,TOTEM) | — | хвост «во время действия» | работает |
| POWER_BITTER_PRICE | Горькая цена | % | ADD | SAGES_BITTER_INK[20..30] | бой героя (PowerRunner on=FLASK); FLASK/BUFF+HURT; Мощь: нет | работает (сраб. 4.2/бой; Δбоя в NORMAL,TOTEM) | — | хвост «во время действия» | работает |
| POWER_STARFALL_SHOWER | Звездопад | % | ADD | STARFALL_ESSENCE[50..70] | бой героя (PowerRunner on=FLASK); FLASK/DAMAGE; Мощь: нет | работает (сраб. 8.0/бой; Δбоя в NORMAL,TOTEM) | — | хвост «во время действия» | работает |
| POWER_STARFALL_SURGE | Звёздный прилив | % | ADD | STARFALL_ESSENCE[25..35] | бой героя (PowerRunner on=FLASK); FLASK/BUFF; Мощь: нет | работает (сраб. 4.0/бой; Δбоя в NORMAL,TOTEM) | — | хвост «во время действия» | работает |
| POWER_RUNE_CARVER | Резец | число | ADD | CARVERS_CIRCLET[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ACTIVE_RUNE_SOCKETS:0.0->1.0 | — | ок | работает |
| POWER_ASHEN_CROWN | Пепельный ореол | % | ADD | ASHEN_CROWN[25..25] | вещь (ItemFactory / Fates.worn); DESECRATION; Мощь: — | по коду: Fates.strength суммирует ролл → ×(1+v/100) к строке | — | ок | работает |
| POWER_CHARRED_VOW | Обугленная клятва | число | ADD | CHARRED_VOW[1..1] | вещь (ItemFactory / Fates.worn); DESECRATION; Мощь: — | ItemFactory: создаётся запечатанной | — | ок | работает |
| POWER_SCHOLARS_INSIGHT | Прозрение книжника | % | ADD | SCHOLARS_AMULET[40..60] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_SKILL_EXPERIENCE:0.0->60.0 | — | ок | работает |
| POWER_RUNE_RESONANCE | Резонанс | % | ADD | RESONANCE_BAND[25..35] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_RUNE_EFFECT:0.0->35.0 | — | ок | работает |
| POWER_ASCENDANCY | Восхождение | число | ADD | ASCENDANT_MANTLE[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_PASSIVE_TIER:0.0->1.0 | — | ок | работает |
| POWER_ASCENDED_FURY | Ярость возвышения | % | ADD | ASCENDANT_MANTLE[20..30] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_ASCENDED_DAMAGE:0.0->30.0 | — | ок | работает |
| POWER_ECHOING_GRIPS | Отзвук | % | ADD | ECHOING_GRIPS[40..60] | бой героя (PowerRunner on=SKILL_USE); SKILL_USE/RECAST; Мощь: нет | работает (сраб. 4.7/бой; Δбоя в NORMAL,HARSH,TOTEM) | каждое 5-е | ок | работает |
| POWER_FATE_THREAD | Нить судьбы | % | ADD | THREAD_OF_FATE[20..30] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_FATE_EFFECT:30.0->60.0 | — | ок | работает |
| POWER_LINEAGE_SIGNET | Печать рода | % | ADD | SIGNET_OF_LINEAGE[10..15] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_FATE_EFFECT:30.0->45.0 | — | ок | работает |
| POWER_ANCESTRAL_BRAND | Клеймо предков | % | ADD | ANCESTRAL_BRAND[35..50] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_FATE_EFFECT:30.0->80.0 | — | ок | работает |
| POWER_LINEAGE_FURY | Ярость рода | % | ADD | SIGNET_OF_LINEAGE[2..3] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_DAMAGE:30.0->39.0 | — | ок | работает |
| POWER_ANCESTRAL_VIGOUR | Сила предков | % | ADD | ANCESTRAL_BRAND[1..2] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_HEALTH:3000.0->3180.0 | — | ок | работает |
| POWER_DROUGHT_MAW | Пасть засухи | % | ADD | DROUGHT_MAW[30..50] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_PARCH_EFFECT:0.0->50.0 | — | ок | работает |
| POWER_ETERNITY_SEAM | Вечный шов | % | ADD | ETERNITYS_SEAM[50..50] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_LIFE_REGEN_FLOOR:0.0->50.0 | — | ок | работает |
| POWER_HOLLOW_VEINS | Пустые жилы | % | ADD | RING_OF_HOLLOW_VEINS[40..60] | бой героя (PowerRunner on=STANDING); STANDING/BUFF; Мощь: нет | работает (сраб. 0.0/бой, стоит 380 тиков; Δбоя в NORMAL,HARSH,TOTEM; ALT) | — | ок | работает |
| POWER_DEADWOOD_CROWN | Сухостой | % | ADD | CROWN_OF_DEADWOOD(M)[15..15] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_DAMAGE_VS_WOUNDED:0.0->15.0 STOCK_DAMAGE_VS_PARCHED:0.0->15.0 | — | ок | работает |
| POWER_TIDAL_MIND | Разум прилива | число | ADD | TIDAL_MIND[1..1] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_MANA_REGEN_FLAT:12.0->32.0 STOCK_LEECH_MANA:2.0->0.0 | — | ок | работает |
| POWER_DRY_WELL | Пересохший колодец | % | ADD | DRY_WELL_RING[3..3] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_SPELL_DAMAGE:60.0->96.0 STOCK_NO_MANA_REGEN:0.0->1.0 | — | ок | работает |
| POWER_HUSHED_SPRING | Тихий источник | % | ADD | HUSHED_SPRING_CIRCLET[20..20] | лист (Powers.applySheet) → бой, сводка; SHEET; Мощь: да | лист: STOCK_MANA_REGEN_TO_LIFE:0.0->20.0 | — | ок | работает |
