# Особые силы · T

## POWER_THORN_LASH <a href="#power_thorn_lash" id="power_thorn_lash"></a>

### Терновый хлыст <a href="#терновый-хлыст" id="терновый-хлыст"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При получении удара {v}% шанс вызвать у нападавшего кровотечение |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Терновый хлыст](../Stats/Stats-POWER.md#power_thorn_lash) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `BLEED` |


## POWER_TIDE_SONG <a href="#power_tide_song" id="power_tide_song"></a>

### Песнь прилива <a href="#песнь-прилива" id="песнь-прилива"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 6 с восстанавливает {v}% маны |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Песнь прилива](../Stats/Stats-POWER.md#power_tide_song) |
| Событие · `on` | `EVERY` |
| every | 6 |
| Эффекты · `effects` | Действие: `HEAL`; of: `MANA` |


## POWER_TIME_LOOP <a href="#power_time_loop" id="power_time_loop"></a>

### Петля времени <a href="#петля-времени" id="петля-времени"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Раз за бой вместо смерти время отматывается: полное здоровье, все перезарядки обновлены и {v} с неуязвимости |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Петля времени](../Stats/Stats-POWER.md#power_time_loop) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `DEATH` |
| Перезарядка, с · `cooldown` | -1 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE`; Количество: 100; Действие: `COOLDOWNS`; Количество: 999; Действие: `INVULNERABLE` |


## POWER_THICK_HIDE <a href="#power_thick_hide" id="power_thick_hide"></a>

### Толстая шкура <a href="#толстая-шкура" id="толстая-шкура"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Даёт броню, равную {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Толстая шкура](../Stats/Stats-POWER.md#power_thick_hide) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Здоровье](../Stats/Stats-HERO.md#stock_health); to: [Броня](../Stats/Stats-HERO.md#stock_armor) |


## POWER_TROPHY_HUNTER <a href="#power_trophy_hunter" id="power_trophy_hunter"></a>

### Собиратель трофеев <a href="#собиратель-трофеев" id="собиратель-трофеев"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения опыта с редких монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Собиратель трофеев](../Stats/Stats-POWER.md#power_trophy_hunter) |
| world | gain: `EXPERIENCE`; against: `RARE` |


## POWER_TIDE_TURN <a href="#power_tide_turn" id="power_tide_turn"></a>

### Смена прилива <a href="#смена-прилива" id="смена-прилива"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Под ударом восстанавливает {v}% энергощита, не чаще раза в 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Смена прилива](../Stats/Stats-POWER.md#power_tide_turn) |
| Событие · `on` | `HIT_TAKEN` |
| Перезарядка, с · `cooldown` | 3 |
| Эффекты · `effects` | Действие: `HEAL`; of: `SHIELD` |


## POWER_TEMPERED_TIPS <a href="#power_tempered_tips" id="power_tempered_tips"></a>

### Закалённые наконечники <a href="#закалённые-наконечники" id="закалённые-наконечники"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство с шансом {v}% сразу готовит следующую атаку |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Закалённые наконечники](../Stats/Stats-POWER.md#power_tempered_tips) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `RUSH` |


## POWER_TIMELESS <a href="#power_timeless" id="power_timeless"></a>

### Вне времени <a href="#вне-времени" id="вне-времени"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения опыта |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вне времени](../Stats/Stats-POWER.md#power_timeless) |
| world | gain: `EXPERIENCE` |


## POWER_TIMELESS_BODY <a href="#power_timeless_body" id="power_timeless_body"></a>

### Нетленное тело <a href="#нетленное-тело" id="нетленное-тело"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 10 с снимает ваши состояния и восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Нетленное тело](../Stats/Stats-POWER.md#power_timeless_body) |
| Событие · `on` | `EVERY` |
| every | 10 |
| Эффекты · `effects` | Действие: `CLEANSE`; Действие: `HEAL`; of: `LIFE` |


## POWER_TITAN_GRIP <a href="#power_titan_grip" id="power_titan_grip"></a>

### Хватка титана <a href="#хватка-титана" id="хватка-титана"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к физическому урону за каждые 5 силы |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Хватка титана](../Stats/Stats-POWER.md#power_titan_grip) |
| sheet | Операция: `PER`; Предшествующие зоны: [Сила](../Stats/Stats-HERO.md#stock_strength); to: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); per: 5 |


## POWER_TITANS_MIGHT <a href="#power_titans_might" id="power_titans_might"></a>

### Мощь титанов <a href="#мощь-титанов" id="мощь-титанов"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Даёт физический урон, равный {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Мощь титанов](../Stats/Stats-POWER.md#power_titans_might) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Здоровье](../Stats/Stats-HERO.md#stock_health); to: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical) |


## POWER_TITAN_SKIN <a href="#power_titan_skin" id="power_titan_skin"></a>

### Кожа титана <a href="#кожа-титана" id="кожа-титана"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Полученный критический удар даёт барьер в {v}% максимума здоровья на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кожа титана](../Stats/Stats-POWER.md#power_titan_skin) |
| Событие · `on` | `CRIT_TAKEN` |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_TASTE_TEST <a href="#power_taste_test" id="power_taste_test"></a>

### Проба на вкус <a href="#проба-на-вкус" id="проба-на-вкус"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по отравленным врагам возвращают здоровьем {v}% нанесённого урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Проба на вкус](../Stats/Stats-POWER.md#power_taste_test) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_AILMENT`; word: `POISON` |
| Эффекты · `effects` | Действие: `HEAL`; of: `DEALT` |


## POWER_THUNDERHIDE <a href="#power_thunderhide" id="power_thunderhide"></a>

### Громовая шкура <a href="#громовая-шкура" id="громовая-шкура"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения уклонения против врагов под шоком |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Громовая шкура](../Stats/Stats-POWER.md#power_thunderhide) |
| Событие · `on` | `STANDING` |
| checks | check: `TARGET_AILMENT`; word: `SHOCK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Операция: `INCREASED` |


## POWER_TIME_DEBT <a href="#power_time_debt" id="power_time_debt"></a>

### Долг времени <a href="#долг-времени" id="долг-времени"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Умение с шансом {v}% сокращает перезарядку ваших умений на 1 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Долг времени](../Stats/Stats-POWER.md#power_time_debt) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `SKILL_USE` |
| Эффекты · `effects` | Действие: `COOLDOWNS`; Количество: 1 |


## POWER_TIGHTENING_COIL <a href="#power_tightening_coil" id="power_tightening_coil"></a>

### Сжатие колец <a href="#сжатие-колец" id="сжатие-колец"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сжатие колец](../Stats/Stats-POWER.md#power_tightening_coil) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `POISON` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3 |


## POWER_TREMOR <a href="#power_tremor" id="power_tremor"></a>

### Толчок <a href="#толчок" id="толчок"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 4-й удар сотрясает землю и оглушает всех врагов на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Толчок](../Stats/Stats-POWER.md#power_tremor) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 4 |
| Эффекты · `effects` | Действие: `STUN`; to: `ALL` |


## POWER_TYRANTS_TRIBUTE <a href="#power_tyrants_tribute" id="power_tyrants_tribute"></a>

### Дань тирана <a href="#дань-тирана" id="дань-тирана"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство горящего врага восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Дань тирана](../Stats/Stats-POWER.md#power_tyrants_tribute) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_TWICE_STRUCK <a href="#power_twice_struck" id="power_twice_struck"></a>

### Дважды поражённый <a href="#дважды-поражённый" id="дважды-поражённый"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 2-й удар с шансом {v}% вызывает шок |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Дважды поражённый](../Stats/Stats-POWER.md#power_twice_struck) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 2 |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `SHOCK` |


## POWER_TRUE_SWING <a href="#power_true_swing" id="power_true_swing"></a>

### Верный замах <a href="#верный-замах" id="верный-замах"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса крита при полном здоровье |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Верный замах](../Stats/Stats-POWER.md#power_true_swing) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_FULL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED` |


## POWER_TAINTED_EDGE <a href="#power_tainted_edge" id="power_tainted_edge"></a>

### Порченое лезвие <a href="#порченое-лезвие" id="порченое-лезвие"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары с шансом {v}% отравляют |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Порченое лезвие](../Stats/Stats-POWER.md#power_tainted_edge) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `POISON` |


## POWER_TAINT <a href="#power_taint" id="power_taint"></a>

### Скверна <a href="#скверна" id="скверна"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Отравление врага с шансом {v}% проклинает его |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Скверна](../Stats/Stats-POWER.md#power_taint) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `POISON` |
| Эффекты · `effects` | Действие: `CURSE` |


## POWER_TRAILBLAZE <a href="#power_trailblaze" id="power_trailblaze"></a>

### Прокладка пути <a href="#прокладка-пути" id="прокладка-пути"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт {v}% увеличения скорости передвижения на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Прокладка пути](../Stats/Stats-POWER.md#power_trailblaze) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость передвижения](../Stats/Stats-HERO.md#stock_movement_speed); Операция: `INCREASED`; Длительность: 4; stacks: 3 |


## POWER_TAILWIND <a href="#power_tailwind" id="power_tailwind"></a>

### Попутный ветер <a href="#попутный-ветер" id="попутный-ветер"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения скорости атаки за каждые 10% скорости передвижения |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Попутный ветер](../Stats/Stats-POWER.md#power_tailwind) |
| sheet | Операция: `PER`; Предшествующие зоны: [Скорость передвижения](../Stats/Stats-HERO.md#stock_movement_speed); to: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); per: 10 |


## POWER_TROPHY_TRAIL <a href="#power_trophy_trail" id="power_trophy_trail"></a>

### Тропа трофеев <a href="#тропа-трофеев" id="тропа-трофеев"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса уникальных предметов с боссов и стражей |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тропа трофеев](../Stats/Stats-POWER.md#power_trophy_trail) |
| world | gain: `UNIQUE` |


## POWER_TRINITY <a href="#power_trinity" id="power_trinity"></a>

### Триединство <a href="#триединство" id="триединство"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона за каждого врага под состоянием, до 3 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Триединство](../Stats/Stats-POWER.md#power_trinity) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); scale: `AILED_FOES`; cap: 3 |


## POWER_TIDE_TURNS <a href="#power_tide_turns" id="power_tide_turns"></a>

### Прилив поворачивает <a href="#прилив-поворачивает" id="прилив-поворачивает"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | На низком здоровье восстанавливает {v}% максимума здоровья, не чаще раза в 10 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Прилив поворачивает](../Stats/Stats-POWER.md#power_tide_turns) |
| Событие · `on` | `LOW_LIFE` |
| Перезарядка, с · `cooldown` | 10 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_THUNDERCLAP <a href="#power_thunderclap" id="power_thunderclap"></a>

### Раскат <a href="#раскат" id="раскат"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критические удары отзываются через 0,5 с на {v}% урона молнией |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Раскат](../Stats/Stats-POWER.md#power_thunderclap) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: [Отзвук](../../Skills/Skills-SCION.md#echo); Длительность: 0.5; Тип: `LIGHTNING` |


## POWER_TAINTED_VIGOUR <a href="#power_tainted_vigour" id="power_tainted_vigour"></a>

### Порченая сила <a href="#порченая-сила" id="порченая-сила"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия +{v}% похищения здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Порченая сила](../Stats/Stats-POWER.md#power_tainted_vigour) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Общий вампиризм](../Stats/Stats-HERO.md#stock_leech_all) |


## POWER_TEMPERED_SPINES <a href="#power_tempered_spines" id="power_tempered_spines"></a>

### Закалённые шипы <a href="#закалённые-шипы" id="закалённые-шипы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% брони добавляется к шипам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Закалённые шипы](../Stats/Stats-POWER.md#power_tempered_spines) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Броня](../Stats/Stats-HERO.md#stock_armor); to: [Шипы](../Stats/Stats-HERO.md#stock_thorns) |


## POWER_TROPHY_BREATH <a href="#power_trophy_breath" id="power_trophy_breath"></a>

### Трофейный вдох <a href="#трофейный-вдох" id="трофейный-вдох"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство редкого или уникального врага восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Трофейный вдох](../Stats/Stats-POWER.md#power_trophy_breath) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_RARE` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_TENTACLE_SWEEP <a href="#power_tentacle_sweep" id="power_tentacle_sweep"></a>

### Взмах щупалец <a href="#взмах-щупалец" id="взмах-щупалец"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 4-й удар сметает всех врагов на {v}% урона оружия хаосом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Взмах щупалец](../Stats/Stats-POWER.md#power_tentacle_sweep) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 4 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `CHAOS`; to: `ALL` |


## POWER_TOTALITY <a href="#power_totality" id="power_totality"></a>

### Полная фаза <a href="#полная-фаза" id="полная-фаза"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 8 с даёт {v}% увеличения шанса крита на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Полная фаза](../Stats/Stats-POWER.md#power_totality) |
| Событие · `on` | `EVERY` |
| every | 8 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED`; Длительность: 3 |


## POWER_TROLL_BELLOW <a href="#power_troll_bellow" id="power_troll_bellow"></a>

### Рёв тролля <a href="#рёв-тролля" id="рёв-тролля"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс в начале боя оглушить всех врагов на 1 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Рёв тролля](../Stats/Stats-POWER.md#power_troll_bellow) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `STUN`; to: `ALL`; Длительность: 1 |


## POWER_THORN_PRICK <a href="#power_thorn_prick" id="power_thorn_prick"></a>

### Укол шипа <a href="#укол-шипа" id="укол-шипа"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый ваш удар стоит вам {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Укол шипа](../Stats/Stats-POWER.md#power_thorn_prick) |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `HURT`; of: `LIFE` |


## POWER_THORN_RETORT <a href="#power_thorn_retort" id="power_thorn_retort"></a>

### Колючий ответ <a href="#колючий-ответ" id="колючий-ответ"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% урона ударов по вам возвращается всем врагам на поле |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Колючий ответ](../Stats/Stats-POWER.md#power_thorn_retort) |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `RETALIATE` |


## POWER_TRIBUNAL_MIND <a href="#power_tribunal_mind" id="power_tribunal_mind"></a>

### Разум над плотью <a href="#разум-над-плотью" id="разум-над-плотью"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% получаемого урона снимается с маны до здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Разум над плотью](../Stats/Stats-POWER.md#power_tribunal_mind) |
| sheet | Операция: `ADD`; to: [Мана прежде здоровья](../Stats/Stats-HERO.md#stock_mana_before_life) |


## POWER_TRIBUNAL_VERDICT <a href="#power_tribunal_verdict" id="power_tribunal_verdict"></a>

### Приговор <a href="#приговор" id="приговор"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% к урону молнией, пока маны больше 50% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Приговор](../Stats/Stats-POWER.md#power_tribunal_verdict) |
| Событие · `on` | `STANDING` |
| checks | check: `MANA_ABOVE`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон молнией](../Stats/Stats-HERO.md#stock_attack_lightning); Операция: `INCREASED` |


## POWER_THICKET_GRACE <a href="#power_thicket_grace" id="power_thicket_grace"></a>

### Грация чащи <a href="#грация-чащи" id="грация-чащи"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | За каждые 1000 уклонения: +{v}% к скорости передвижения и +0,5% к скорости атаки и каста |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Грация чащи](../Stats/Stats-POWER.md#power_thicket_grace) |
| sheet | Операция: `PER`; Предшествующие зоны: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); to: [Скорость передвижения](../Stats/Stats-HERO.md#stock_movement_speed); per: 1000; Операция: `PER`; Предшествующие зоны: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); to: [Скорость применения](../Stats/Stats-HERO.md#stock_cast_speed); per: 1000; Значение: 0.5; Операция: `MORE`; Предшествующие зоны: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); to: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); per: 1000; Значение: 0.5 |


## POWER_TURNCOAT_BETRAYAL <a href="#power_turncoat_betrayal" id="power_turncoat_betrayal"></a>

### Измена <a href="#измена" id="измена"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} урона хаосом к атакам за каждые 8 Силы |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Измена](../Stats/Stats-POWER.md#power_turncoat_betrayal) |
| sheet | Операция: `PER`; Предшествующие зоны: [Сила](../Stats/Stats-HERO.md#stock_strength); to: [Урон хаосом](../Stats/Stats-HERO.md#stock_attack_chaos); per: 8 |


## POWER_TURNCOAT_SWITCH <a href="#power_turncoat_switch" id="power_turncoat_switch"></a>

### Сменённое знамя <a href="#сменённое-знамя" id="сменённое-знамя"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда вас бьют, {v}% шанс ответить всем врагам на поле 20% урона хаосом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сменённое знамя](../Stats/Stats-POWER.md#power_turncoat_switch) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `RETALIATE`; Количество: 20; Тип: `CHAOS` |


## POWER_TINKER_TUNING <a href="#power_tinker_tuning" id="power_tinker_tuning"></a>

### Подогнанные кольца <a href="#подогнанные-кольца" id="подогнанные-кольца"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Модификаторы надетых колец сильнее на {v}% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Подогнанные кольца](../Stats/Stats-POWER.md#power_tinker_tuning) |
| slots | Операция: `AMPLIFY`; slots: `RING`; `RING_2` |


## POWER_TINKER_GEARWORK <a href="#power_tinker_gearwork" id="power_tinker_gearwork"></a>

### Шестерёнки <a href="#шестерёнки" id="шестерёнки"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Умение с шансом {v}% сокращает остальные откаты на 1 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Шестерёнки](../Stats/Stats-POWER.md#power_tinker_gearwork) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `SKILL_USE` |
| Эффекты · `effects` | Действие: `COOLDOWNS`; Количество: 1 |


## POWER_TETHER_DECAY <a href="#power_tether_decay" id="power_tether_decay"></a>

### Истончение привязи <a href="#истончение-привязи" id="истончение-привязи"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | ЭЩ не перезаряжается; теряете {v}% максимума ЭЩ в секунду |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Истончение привязи](../Stats/Stats-POWER.md#power_tether_decay) |
| sheet | Операция: `SET`; to: [Восполнение щита](../Stats/Stats-HERO.md#stock_shield_recharge); Значение: -100; Операция: `ADD`; to: [Потеря ЭЩ](../Stats/Stats-HERO.md#stock_shield_degen_percent) |


## POWER_TORMENT_SOLE <a href="#power_torment_sole" id="power_torment_sole"></a>

### Лишнее кольцо <a href="#лишнее-кольцо" id="лишнее-кольцо"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Кольцо в другом слоте не действует |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Лишнее кольцо](../Stats/Stats-POWER.md#power_torment_sole) |
| slots | Операция: `AMPLIFY`; pick: `OTHER_RING`; Значение: -100 |


## POWER_TORMENT_BRIBE <a href="#power_torment_bribe" id="power_torment_bribe"></a>

### Подкупленный стражник <a href="#подкупленный-стражник" id="подкупленный-стражник"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда вас бьют, {v}% шанс снять с себя недуги |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Подкупленный стражник](../Stats/Stats-POWER.md#power_torment_bribe) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `CLEANSE` |


## POWER_TIMELESS_BOON <a href="#power_timeless_boon" id="power_timeless_boon"></a>

### Солнце дня <a href="#солнце-дня" id="солнце-дня"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | В начале боя одно благо из шести: +30% к урону, +30% к защитам, +20% к скорости атаки и каста, 3% регенерации здоровья в секунду, +15% к шансу крита или +20% ко всем стихийным сопротивлениям |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Солнце дня](../Stats/Stats-POWER.md#power_timeless_boon) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `ONE_OF`; Варианты: Действие: `BUFF`; Свойства: 30% увеличение урона · [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 3600; Действие: `BUFF`; Свойства: 30% увеличение брони · [Броня](../Stats/Stats-HERO.md#stock_armor); 30% увеличение уклонения · [Уклонение](../Stats/Stats-HERO.md#stock_evasion); 30% увеличение энергетического щита · [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield); Длительность: 3600; Действие: `BUFF`; Свойства: 20% увеличение скорости атаки · [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); 20% увеличение скорости сотворения · [Скорость применения](../Stats/Stats-HERO.md#stock_cast_speed); Длительность: 3600; Действие: `BUFF`; Свойства: Восстанавливает 3% здоровья в секунду · [Регенерация здоровья, %](../Stats/Stats-HERO.md#stock_life_regen_percent); Длительность: 3600; Действие: `BUFF`; Свойства: 15% увеличение шанса критического удара · [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Длительность: 3600; Действие: `BUFF`; Свойства: +20% к всем сопротивлениям стихиям · [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all); Длительность: 3600 |


## POWER_TIMELESS_DAWN <a href="#power_timeless_dawn" id="power_timeless_dawn"></a>

### Новый рассвет <a href="#новый-рассвет" id="новый-рассвет"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда этап боя пройден, восстанавливаете {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Новый рассвет](../Stats/Stats-POWER.md#power_timeless_dawn) |
| Событие · `on` | `STAGE_CLEAR` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_TURNING_STEEL <a href="#power_turning_steel" id="power_turning_steel"></a>

### Отводящая сталь <a href="#отводящая-сталь" id="отводящая-сталь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок даёт +{v}% к отклонению на 3 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отводящая сталь](../Stats/Stats-POWER.md#power_turning_steel) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Отклонение](../Stats/Stats-HERO.md#stock_deflection); Длительность: 3; stacks: 3 |


## POWER_TWIN_FANGS <a href="#power_twin_fangs" id="power_twin_fangs"></a>

### Двойные клыки <a href="#двойные-клыки" id="двойные-клыки"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары с шансом {v}% повторяются через 0,3 с за половину урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Двойные клыки](../Stats/Stats-POWER.md#power_twin_fangs) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: [Отзвук](../../Skills/Skills-SCION.md#echo); Количество: 50; Длительность: 0.3 |


## POWER_TRAILFINDER <a href="#power_trailfinder" id="power_trailfinder"></a>

### Чтец следов <a href="#чтец-следов" id="чтец-следов"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% больше опыта за редких врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Чтец следов](../Stats/Stats-POWER.md#power_trailfinder) |
| world | gain: `EXPERIENCE`; against: `RARE` |


## POWER_TEMPERED_SOUL <a href="#power_tempered_soul" id="power_tempered_soul"></a>

### Закалённая душа <a href="#закалённая-душа" id="закалённая-душа"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок даёт {v}% увеличения брони на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Закалённая душа](../Stats/Stats-POWER.md#power_tempered_soul) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Броня](../Stats/Stats-HERO.md#stock_armor); Операция: `INCREASED`; Длительность: 4 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
