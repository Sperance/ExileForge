# Особые силы · L

## POWER_LEAGUE_STRIDE <a href="#power_league_stride" id="power_league_stride"></a>

### Семимильный шаг <a href="#семимильный-шаг" id="семимильный-шаг"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса найти карту |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Семимильный шаг](../Stats/Stats-POWER.md#power_league_stride) |
| world | gain: [Map](../../Equipment/Equipment-MAP.md#map) |


## POWER_LIONEYES_AIM <a href="#power_lioneyes_aim" id="power_lioneyes_aim"></a>

### Прицел Львиного Глаза <a href="#прицел-львиного-глаза" id="прицел-львиного-глаза"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона, пока против вас один враг |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Прицел Львиного Глаза](../Stats/Stats-POWER.md#power_lioneyes_aim) |
| Событие · `on` | `STANDING` |
| checks | check: `FOES_AT_MOST`; Значение: 1 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage) |


## POWER_LEVIATHAN_BITE <a href="#power_leviathan_bite" id="power_leviathan_bite"></a>

### Укус левиафана <a href="#укус-левиафана" id="укус-левиафана"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 3-й удар по редкому или уникальному врагу наносит {v}% его максимума здоровья хаосом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Укус левиафана](../Stats/Stats-POWER.md#power_leviathan_bite) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_RARE`; check: `NTH`; Значение: 3 |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TARGET_LIFE`; Тип: `CHAOS` |


## POWER_LAST_STAND <a href="#power_last_stand" id="power_last_stand"></a>

### Последний рубеж <a href="#последний-рубеж" id="последний-рубеж"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Падение здоровья до низкого даёт барьер в {v}% максимума здоровья на 4 с, не чаще раза в 15 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Последний рубеж](../Stats/Stats-POWER.md#power_last_stand) |
| Событие · `on` | `LOW_LIFE` |
| Перезарядка, с · `cooldown` | 15 |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 4 |


## POWER_LUCKY_BREAK <a href="#power_lucky_break" id="power_lucky_break"></a>

### Удачный расклад <a href="#удачный-расклад" id="удачный-расклад"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство с шансом {v}% обновляет все перезарядки |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Удачный расклад](../Stats/Stats-POWER.md#power_lucky_break) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `COOLDOWNS`; Количество: 999 |


## POWER_LAST_GROUND <a href="#power_last_ground" id="power_last_ground"></a>

### Последний рубеж <a href="#последний-рубеж-1" id="последний-рубеж-1"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к шансу блока, пока здоровья меньше 50% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Последний рубеж](../Stats/Stats-POWER.md#power_last_ground) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_BELOW`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс блока](../Stats/Stats-HERO.md#stock_block_chance) |


## POWER_LULLABY <a href="#power_lullaby" id="power_lullaby"></a>

### Колыбельная <a href="#колыбельная" id="колыбельная"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Падение здоровья до низкого восстанавливает {v}% максимума здоровья, не чаще раза в 20 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Колыбельная](../Stats/Stats-POWER.md#power_lullaby) |
| Событие · `on` | `LOW_LIFE` |
| Перезарядка, с · `cooldown` | 20 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_LEADERS_HOWL <a href="#power_leaders_howl" id="power_leaders_howl"></a>

### Вой вожака <a href="#вой-вожака" id="вой-вожака"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона в первые 4 с боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вой вожака](../Stats/Stats-POWER.md#power_leaders_howl) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 4 |


## POWER_LAST_VERSE <a href="#power_last_verse" id="power_last_verse"></a>

### Последний куплет <a href="#последний-куплет" id="последний-куплет"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство охлаждённого врага с шансом {v}% сразу готовит следующую атаку |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Последний куплет](../Stats/Stats-POWER.md#power_last_verse) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `CHILL` |
| Эффекты · `effects` | Действие: `RUSH` |


## POWER_LAMPBLACK_SIGHT <a href="#power_lampblack_sight" id="power_lampblack_sight"></a>

### Копотный взгляд <a href="#копотный-взгляд" id="копотный-взгляд"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия {v}% увеличения шанса критического удара |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Копотный взгляд](../Stats/Stats-POWER.md#power_lampblack_sight) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED` |


## POWER_LICHS_FOCUS <a href="#power_lichs_focus" id="power_lichs_focus"></a>

### Сосредоточение лича <a href="#сосредоточение-лича" id="сосредоточение-лича"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения урона чарами при полном энергетическом щите |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сосредоточение лича](../Stats/Stats-POWER.md#power_lichs_focus) |
| Событие · `on` | `STANDING` |
| checks | check: `SHIELD_FULL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон чар](../Stats/Stats-HERO.md#stock_spell_damage); Операция: `INCREASED` |


## POWER_LEAPING_BOLT <a href="#power_leaping_bolt" id="power_leaping_bolt"></a>

### Скачущая молния <a href="#скачущая-молния" id="скачущая-молния"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитый враг бьёт случайного врага молнией на {v}% своего максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Скачущая молния](../Stats/Stats-POWER.md#power_leaping_bolt) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TARGET_LIFE`; Тип: `LIGHTNING`; to: `RANDOM` |


## POWER_LURED_PREY <a href="#power_lured_prey" id="power_lured_prey"></a>

### Заманенная добыча <a href="#заманенная-добыча" id="заманенная-добыча"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения скорости атаки по проклятым врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Заманенная добыча](../Stats/Stats-POWER.md#power_lured_prey) |
| Событие · `on` | `STANDING` |
| checks | check: `TARGET_CURSED` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED` |


## POWER_LAST_THUNDER_RELAY <a href="#power_last_thunder_relay" id="power_last_thunder_relay"></a>

### Переданный гром <a href="#переданный-гром" id="переданный-гром"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удар атакой запускает вашу молнию: {v}% Интеллекта уроном молнией по цели (откат 0,3 с) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Переданный гром](../Stats/Stats-POWER.md#power_last_thunder_relay) |
| Событие · `on` | `HIT` |
| checks | check: `ATTACK` |
| Перезарядка, с · `cooldown` | 0.3 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `LIGHTNING`; of: `INTELLECT` |


## POWER_LAST_PEAL <a href="#power_last_peal" id="power_last_peal"></a>

### Последний раскат <a href="#последний-раскат" id="последний-раскат"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс шокировать цель при критическом ударе |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Последний раскат](../Stats/Stats-POWER.md#power_last_peal) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `SHOCK` |


## POWER_LADLE_SHARE <a href="#power_ladle_share" id="power_ladle_share"></a>

### Общая похлёбка <a href="#общая-похлёбка" id="общая-похлёбка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% урона ударов питомца лечит вас |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Общая похлёбка](../Stats/Stats-POWER.md#power_ladle_share) |
| Событие · `on` | `PET_HIT` |
| Эффекты · `effects` | Действие: `HEAL`; of: `DEALT` |


## POWER_LADLE_BROTH <a href="#power_ladle_broth" id="power_ladle_broth"></a>

### Черпак бульона <a href="#черпак-бульона" id="черпак-бульона"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Ваш удар восстанавливает питомцу {v}% его макс. здоровья (откат 0,5 с) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Черпак бульона](../Stats/Stats-POWER.md#power_ladle_broth) |
| Событие · `on` | `HIT` |
| Перезарядка, с · `cooldown` | 0.5 |
| Эффекты · `effects` | Действие: `HEAL`; to: `PET` |


## POWER_LAMP_SPIRIT_BURST <a href="#power_lamp_spirit_burst" id="power_lamp_spirit_burst"></a>

### Всплеск духа <a href="#всплеск-духа" id="всплеск-духа"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс при убийстве выпустить всплеск духа: три удара по 50% урона оружия хаосом по случайным врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Всплеск духа](../Stats/Stats-POWER.md#power_lamp_spirit_burst) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `CHAOS`; to: `RANDOM`; Количество: 50; Действие: `DAMAGE`; Тип: `CHAOS`; to: `RANDOM`; Количество: 50; Действие: `DAMAGE`; Тип: `CHAOS`; to: `RANDOM`; Количество: 50 |


## POWER_LAMP_HOARD <a href="#power_lamp_hoard" id="power_lamp_hoard"></a>

### Украденные огни <a href="#украденные-огни" id="украденные-огни"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к урону за каждый надетый самоцвет |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Украденные огни](../Stats/Stats-POWER.md#power_lamp_hoard) |
| sheet | Операция: `PER`; Предшествующие зоны: [Надетые самоцветы](../Stats/Stats-HERO.md#stock_worn_jewels); to: [Урон](../Stats/Stats-HERO.md#stock_damage) |


## POWER_LUCKDASH_GAMBLE <a href="#power_luckdash_gamble" id="power_luckdash_gamble"></a>

### Бросок монеты <a href="#бросок-монеты" id="бросок-монеты"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | В каждом бою скорость передвижения меняется наугад от −20% до +40%, скорость атаки — от −10% до +20% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бросок монеты](../Stats/Stats-POWER.md#power_luckdash_gamble) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: -20% увеличение скорости передвижения · [Скорость передвижения](../Stats/Stats-HERO.md#stock_movement_speed); -10% увеличение скорости атаки · [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Длительность: 3600 |


## POWER_LUCKDASH_WINDFALL <a href="#power_luckdash_windfall" id="power_luckdash_windfall"></a>

### Удачный куш <a href="#удачный-куш" id="удачный-куш"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Враги роняют на {v}% больше золота |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Удачный куш](../Stats/Stats-POWER.md#power_luckdash_windfall) |
| world | gain: `GOLD` |


## POWER_LOYAL_BANNER <a href="#power_loyal_banner" id="power_loyal_banner"></a>

### Знамя верных <a href="#знамя-верных" id="знамя-верных"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | За каждый надетый уникальный предмет: +{v}% к броне, уклонению и ЭЩ и +3% к максимуму здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Знамя верных](../Stats/Stats-POWER.md#power_loyal_banner) |
| sheet | Операция: `MORE`; Предшествующие зоны: [Надетые уникальные предметы](../Stats/Stats-HERO.md#stock_worn_uniques); to: [Броня](../Stats/Stats-HERO.md#stock_armor); cap: 60; Операция: `MORE`; Предшествующие зоны: [Надетые уникальные предметы](../Stats/Stats-HERO.md#stock_worn_uniques); to: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); cap: 60; Операция: `MORE`; Предшествующие зоны: [Надетые уникальные предметы](../Stats/Stats-HERO.md#stock_worn_uniques); to: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield); cap: 60; Операция: `MORE`; Предшествующие зоны: [Надетые уникальные предметы](../Stats/Stats-HERO.md#stock_worn_uniques); to: [Здоровье](../Stats/Stats-HERO.md#stock_health); Значение: 3; cap: 36 |


## POWER_LOYAL_OATH <a href="#power_loyal_oath" id="power_loyal_oath"></a>

### Верность клятве <a href="#верность-клятве" id="верность-клятве"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% к урону на 5 с в начале боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Верность клятве](../Stats/Stats-POWER.md#power_loyal_oath) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `INCREASED`; Длительность: 5 |


## POWER_LONE_QUARRY <a href="#power_lone_quarry" id="power_lone_quarry"></a>

### Одинокая добыча <a href="#одинокая-добыча" id="одинокая-добыча"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к меткости, пока стоит один враг |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Одинокая добыча](../Stats/Stats-POWER.md#power_lone_quarry) |
| Событие · `on` | `STANDING` |
| checks | check: `FOES_AT_MOST`; Значение: 1 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Меткость](../Stats/Stats-HERO.md#stock_accuracy) |


## POWER_LAST_HOWL <a href="#power_last_howl" id="power_last_howl"></a>

### Последний вой <a href="#последний-вой" id="последний-вой"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда питомец падает, получаете {v}% увеличения урона на 6 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Последний вой](../Stats/Stats-POWER.md#power_last_howl) |
| Событие · `on` | `PET_DEATH` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 6 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
