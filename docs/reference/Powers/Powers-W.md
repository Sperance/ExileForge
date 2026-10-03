# Особые силы · W

## POWER_WARDENS_KEYS <a href="#power_wardens_keys" id="power_wardens_keys"></a>

### Ключи стража <a href="#ключи-стража" id="ключи-стража"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения количества предметов с редких монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ключи стража](../Stats/Stats-POWER.md#power_wardens_keys) |
| world | gain: `QUANTITY`; against: `RARE` |


## POWER_WORLDEATER <a href="#power_worldeater" id="power_worldeater"></a>

### Пожиратель миров <a href="#пожиратель-миров" id="пожиратель-миров"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 4-й удар наносит {v}% максимума здоровья врага физическим уроном |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пожиратель миров](../Stats/Stats-POWER.md#power_worldeater) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 4 |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TARGET_LIFE`; Тип: `PHYSICAL` |


## POWER_WANDERERS_START <a href="#power_wanderers_start" id="power_wanderers_start"></a>

### Первый шаг странника <a href="#первый-шаг-странника" id="первый-шаг-странника"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения скорости атаки первые 3 с боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Первый шаг странника](../Stats/Stats-POWER.md#power_wanderers_start) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3 |


## POWER_WYRM_HOARD <a href="#power_wyrm_hoard" id="power_wyrm_hoard"></a>

### Клад змея <a href="#клад-змея" id="клад-змея"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения золота с редких и уникальных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Клад змея](../Stats/Stats-POWER.md#power_wyrm_hoard) |
| world | gain: `GOLD`; against: `RARE`; `UNIQUE` |


## POWER_WARDENS_VOW <a href="#power_wardens_vow" id="power_wardens_vow"></a>

### Клятва стража <a href="#клятва-стража" id="клятва-стража"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Полученный удар даёт {v}% увеличения брони на 4 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Клятва стража](../Stats/Stats-POWER.md#power_wardens_vow) |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Броня](../Stats/Stats-HERO.md#stock_armor); Операция: `INCREASED`; Длительность: 4; stacks: 5 |


## POWER_WINTER_GRIP <a href="#power_winter_grip" id="power_winter_grip"></a>

### Хватка зимы <a href="#хватка-зимы" id="хватка-зимы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к множителю крита по охлаждённым врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Хватка зимы](../Stats/Stats-POWER.md#power_winter_grip) |
| Событие · `on` | `STANDING` |
| checks | check: `TARGET_AILMENT`; word: `CHILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Множитель критического удара](../Stats/Stats-HERO.md#stock_critical_multiplier) |


## POWER_WAR_SHOUT <a href="#power_war_shout" id="power_war_shout"></a>

### Боевой клич <a href="#боевой-клич" id="боевой-клич"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Боевой клич](../Stats/Stats-POWER.md#power_war_shout) |
| Событие · `on` | `EVERY` |
| every | 8 |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 3; to: `ALL` |


## POWER_WORLDBREAK <a href="#power_worldbreak" id="power_worldbreak"></a>

### Мироразлом <a href="#мироразлом" id="мироразлом"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 6-й удар сотрясает землю: {v}% урона оружия всем врагам и оглушение на 0,5 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Мироразлом](../Stats/Stats-POWER.md#power_worldbreak) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 6 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `PHYSICAL`; to: `ALL`; Действие: `STUN`; Длительность: 0.5; to: `ALL` |


## POWER_WHISPERED_HEX <a href="#power_whispered_hex" id="power_whispered_hex"></a>

### Нашёптанное проклятие <a href="#нашёптанное-проклятие" id="нашёптанное-проклятие"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удар с шансом {v}% проклинает цель |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Нашёптанное проклятие](../Stats/Stats-POWER.md#power_whispered_hex) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `CURSE` |


## POWER_WARM_COAL <a href="#power_warm_coal" id="power_warm_coal"></a>

### Тёплый уголёк <a href="#тёплый-уголёк" id="тёплый-уголёк"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 5 с восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тёплый уголёк](../Stats/Stats-POWER.md#power_warm_coal) |
| Событие · `on` | `EVERY` |
| every | 5 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_WIDOWS_TOLL <a href="#power_widows_toll" id="power_widows_toll"></a>

### Вдовья доля <a href="#вдовья-доля" id="вдовья-доля"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары добивают врагов, у которых меньше {v}% здоровья, кроме уникальных |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вдовья доля](../Stats/Stats-POWER.md#power_widows_toll) |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `EXECUTE` |


## POWER_WATCHING_SHADOWS <a href="#power_watching_shadows" id="power_watching_shadows"></a>

### Бдящие тени <a href="#бдящие-тени" id="бдящие-тени"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При получении удара {v}% шанс проклясть нападавшего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бдящие тени](../Stats/Stats-POWER.md#power_watching_shadows) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `CURSE` |


## POWER_WAYFINDER <a href="#power_wayfinder" id="power_wayfinder"></a>

### Следопыт <a href="#следопыт" id="следопыт"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса найти карты |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Следопыт](../Stats/Stats-POWER.md#power_wayfinder) |
| world | gain: [Map](../../Equipment/Equipment-MAP.md#map) |


## POWER_WORLDS_WEIGHT <a href="#power_worlds_weight" id="power_worlds_weight"></a>

### Тяжесть мира <a href="#тяжесть-мира" id="тяжесть-мира"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса найти карты с редких и уникальных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тяжесть мира](../Stats/Stats-POWER.md#power_worlds_weight) |
| world | gain: [Map](../../Equipment/Equipment-MAP.md#map); against: `RARE`; `UNIQUE` |


## POWER_WICK_FLARE <a href="#power_wick_flare" id="power_wick_flare"></a>

### Вспышка фитиля <a href="#вспышка-фитиля" id="вспышка-фитиля"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Глоток с шансом {v}% поджигает всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вспышка фитиля](../Stats/Stats-POWER.md#power_wick_flare) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `IGNITE`; to: `ALL` |


## POWER_WAYPOINT_WARD <a href="#power_waypoint_ward" id="power_waypoint_ward"></a>

### Заслон привала <a href="#заслон-привала" id="заслон-привала"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Глоток из фляги даёт заслон в {v}% максимума здоровья на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Заслон привала](../Stats/Stats-POWER.md#power_waypoint_ward) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_WARDEN_BOUNTY <a href="#power_warden_bounty" id="power_warden_bounty"></a>

### Награда за стража <a href="#награда-за-стража" id="награда-за-стража"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения опыта с уникальных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Награда за стража](../Stats/Stats-POWER.md#power_warden_bounty) |
| world | gain: `EXPERIENCE`; against: `UNIQUE` |


## POWER_WEEPING_GUARD <a href="#power_weeping_guard" id="power_weeping_guard"></a>

### Плачущий заслон <a href="#плачущий-заслон" id="плачущий-заслон"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок с шансом {v}% охлаждает атакующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Плачущий заслон](../Stats/Stats-POWER.md#power_weeping_guard) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `CHILL` |


## POWER_WHITE_HEAT <a href="#power_white_heat" id="power_white_heat"></a>

### Белый жар <a href="#белый-жар" id="белый-жар"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар с шансом {v}% поджигает |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Белый жар](../Stats/Stats-POWER.md#power_white_heat) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `IGNITE` |


## POWER_WARFORGE_TEMPER <a href="#power_warforge_temper" id="power_warforge_temper"></a>

### Кузнечный нрав <a href="#кузнечный-нрав" id="кузнечный-нрав"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения брони выше 50% здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кузнечный нрав](../Stats/Stats-POWER.md#power_warforge_temper) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_ABOVE`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Броня](../Stats/Stats-HERO.md#stock_armor); Операция: `INCREASED` |


## POWER_WORLDBLOOD_FLOW <a href="#power_worldblood_flow" id="power_worldblood_flow"></a>

### Бесконечный ток <a href="#бесконечный-ток" id="бесконечный-ток"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Ваши фляги выпиваются в начале каждого боя без траты зарядов и действуют на 1000% дольше |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бесконечный ток](../Stats/Stats-POWER.md#power_worldblood_flow) |
| sheet | Операция: `ADD`; to: [Бесконечные фляги](../Stats/Stats-HERO.md#stock_flasks_auto); Операция: `MORE`; to: [Длительность фляг](../Stats/Stats-HERO.md#stock_flask_duration); Значение: 1000 |


## POWER_WORLDBLOOD_THIRST <a href="#power_worldblood_thirst" id="power_worldblood_thirst"></a>

### Неутолимость <a href="#неутолимость" id="неутолимость"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Ваши фляги не восстанавливают здоровье и ману |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Неутолимость](../Stats/Stats-POWER.md#power_worldblood_thirst) |
| sheet | Операция: `SET`; to: [Восстановление фляги](../Stats/Stats-HERO.md#stock_flask_recovery); Значение: -100 |


## POWER_WIND_AT_BACK <a href="#power_wind_at_back" id="power_wind_at_back"></a>

### Попутный ветер <a href="#попутный-ветер" id="попутный-ветер"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийства дают {v}% увеличения скорости атаки на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Попутный ветер](../Stats/Stats-POWER.md#power_wind_at_back) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 4; stacks: 3 |


## POWER_WARDING_MANTRA <a href="#power_warding_mantra" id="power_warding_mantra"></a>

### Охранная мантра <a href="#охранная-мантра" id="охранная-мантра"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к подавлению чар на 5 с в начале боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Охранная мантра](../Stats/Stats-POWER.md#power_warding_mantra) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Подавление чар](../Stats/Stats-HERO.md#stock_spell_suppression); Длительность: 5 |


## POWER_WHISPERED_DOOM <a href="#power_whispered_doom" id="power_whispered_doom"></a>

### Шёпот гибели <a href="#шёпот-гибели" id="шёпот-гибели"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | С шансом {v}% проклинает врага в начале боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Шёпот гибели](../Stats/Stats-POWER.md#power_whispered_doom) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `CURSE` |


## POWER_WHETTED <a href="#power_whetted" id="power_whetted"></a>

### Наточенный <a href="#наточенный" id="наточенный"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Первый удар боя наносит на {v}% больше урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Наточенный](../Stats/Stats-POWER.md#power_whetted) |
| Событие · `on` | `HIT` |
| checks | check: `FIGHT_BEFORE`; Значение: 1 |
| Эффекты · `effects` | Действие: `DAMAGE` |


## POWER_WILDFIRE_SPREAD <a href="#power_wildfire_spread" id="power_wildfire_spread"></a>

### Дикий огонь <a href="#дикий-огонь" id="дикий-огонь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство подожжённого врага с шансом {v}% разносит поджог |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Дикий огонь](../Stats/Stats-POWER.md#power_wildfire_spread) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `SPREAD` |


## POWER_WARDSTONE <a href="#power_wardstone" id="power_wardstone"></a>

### Камень-оберег <a href="#камень-оберег" id="камень-оберег"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% уклонения достаётся энергощитом; уклонения нет |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Камень-оберег](../Stats/Stats-POWER.md#power_wardstone) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); to: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield); Операция: `SET`; to: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Значение: 0 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
