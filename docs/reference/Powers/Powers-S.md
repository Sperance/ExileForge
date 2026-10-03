# Особые силы · S

## POWER_SOUL_HARVEST <a href="#power_soul_harvest" id="power_soul_harvest"></a>

### Жатва душ <a href="#жатва-душ" id="жатва-душ"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство восстанавливает {v}% энергощита |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жатва душ](../Stats/Stats-POWER.md#power_soul_harvest) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `HEAL`; of: `SHIELD` |


## POWER_STATIC_DODGE <a href="#power_static_dodge" id="power_static_dodge"></a>

### Статический уход <a href="#статический-уход" id="статический-уход"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение с шансом {v}% шокирует нападавшего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Статический уход](../Stats/Stats-POWER.md#power_static_dodge) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `SHOCK` |


## POWER_SWORN_STRIKE <a href="#power_sworn_strike" id="power_sworn_strike"></a>

### Удар по клятве <a href="#удар-по-клятве" id="удар-по-клятве"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Применение умения с шансом {v}% делает следующий удар критическим |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Удар по клятве](../Stats/Stats-POWER.md#power_sworn_strike) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `SKILL_USE` |
| Эффекты · `effects` | Действие: `NEXT_CRIT` |


## POWER_STORM_EYE <a href="#power_storm_eye" id="power_storm_eye"></a>

### Глаз бури <a href="#глаз-бури" id="глаз-бури"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к множителю крита за каждые 10 ловкости |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Глаз бури](../Stats/Stats-POWER.md#power_storm_eye) |
| sheet | Операция: `PER`; Предшествующие зоны: [Ловкость](../Stats/Stats-HERO.md#stock_agility); to: [Множитель критического удара](../Stats/Stats-HERO.md#stock_critical_multiplier); per: 10 |


## POWER_STOKED <a href="#power_stoked" id="power_stoked"></a>

### Раздутый жар <a href="#раздутый-жар" id="раздутый-жар"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения урона огнём за каждую секунду боя, до 10 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Раздутый жар](../Stats/Stats-POWER.md#power_stoked) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire); Операция: `INCREASED`; scale: `SECONDS`; cap: 10 |


## POWER_SLAG_CLOG <a href="#power_slag_clog" id="power_slag_clog"></a>

### Шлаковая корка <a href="#шлаковая-корка" id="шлаковая-корка"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Шлаковая корка](../Stats/Stats-POWER.md#power_slag_clog) |
| Событие · `on` | `HIT_TAKEN` |
| checks | check: `DAMAGE_TYPE`; word: `PHYSICAL` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3 |


## POWER_SUNFLARE <a href="#power_sunflare" id="power_sunflare"></a>

### Солнечный протуберанец <a href="#солнечный-протуберанец" id="солнечный-протуберанец"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 5-й удар огнём бьёт всех врагов на {v}% урона оружия огнём |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Солнечный протуберанец](../Stats/Stats-POWER.md#power_sunflare) |
| Событие · `on` | `HIT` |
| checks | check: `DAMAGE_TYPE`; word: `FIRE`; check: `NTH`; Значение: 5 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `FIRE`; to: `ALL` |


## POWER_SWARM_BURST <a href="#power_swarm_burst" id="power_swarm_burst"></a>

### Рой из тела <a href="#рой-из-тела" id="рой-из-тела"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Из убитого врага вылетает рой и бьёт случайного врага на {v}% урона оружия хаосом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Рой из тела](../Stats/Stats-POWER.md#power_swarm_burst) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `CHAOS`; to: `RANDOM` |


## POWER_SLOW_TIME <a href="#power_slow_time" id="power_slow_time"></a>

### Замедление времени <a href="#замедление-времени" id="замедление-времени"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 5 с задерживает следующий удар всех врагов на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Замедление времени](../Stats/Stats-POWER.md#power_slow_time) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `EVERY` |
| every | 5 |
| Эффекты · `effects` | Действие: `DELAY`; to: `ALL` |


## POWER_SOULFORGE <a href="#power_soulforge" id="power_soulforge"></a>

### Душекузня <a href="#душекузня" id="душекузня"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к шансу блока за каждые 100 энергощита |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Душекузня](../Stats/Stats-POWER.md#power_soulforge) |
| sheet | Операция: `PER`; Предшествующие зоны: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield); to: [Шанс блока](../Stats/Stats-HERO.md#stock_block_chance); per: 100 |


## POWER_SOUL_SUNDER <a href="#power_soul_sunder" id="power_soul_sunder"></a>

### Раскол души <a href="#раскол-души" id="раскол-души"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Раскол души](../Stats/Stats-POWER.md#power_soul_sunder) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all); Характеристика: [Сопротивление хаосу](../Stats/Stats-HERO.md#stock_resist_chaos); Длительность: 5 |


## POWER_SEALED_HEART <a href="#power_sealed_heart" id="power_sealed_heart"></a>

### Запечатанное сердце <a href="#запечатанное-сердце" id="запечатанное-сердце"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Запечатанное сердце](../Stats/Stats-POWER.md#power_sealed_heart) |
| sheet | Операция: `MORE`; to: [Мана](../Stats/Stats-HERO.md#stock_mana) |


## POWER_SEVENTH_VEIL <a href="#power_seventh_veil" id="power_seventh_veil"></a>

### Седьмой покров <a href="#седьмой-покров" id="седьмой-покров"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% максимума здоровья превращается в энергощит |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Седьмой покров](../Stats/Stats-POWER.md#power_seventh_veil) |
| sheet | Операция: `CONVERT`; Предшествующие зоны: [Здоровье](../Stats/Stats-HERO.md#stock_health); to: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield) |


## POWER_SAFFELL_PRISM <a href="#power_saffell_prism" id="power_saffell_prism"></a>

### Призма Саффелла <a href="#призма-саффелла" id="призма-саффелла"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к шансу блока за каждые 10% ко всем сопротивлениям |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Призма Саффелла](../Stats/Stats-POWER.md#power_saffell_prism) |
| sheet | Операция: `PER`; Предшествующие зоны: [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all); to: [Шанс блока](../Stats/Stats-HERO.md#stock_block_chance); per: 10 |


## POWER_STARFORGED <a href="#power_starforged" id="power_starforged"></a>

### Звёздная ковка <a href="#звёздная-ковка" id="звёздная-ковка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 5-й удар обрушивает звезду: {v}% урона оружия молнией всем врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Звёздная ковка](../Stats/Stats-POWER.md#power_starforged) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 5 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `LIGHTNING`; to: `ALL` |


## POWER_SALT_SPINES <a href="#power_salt_spines" id="power_salt_spines"></a>

### Соляные шипы <a href="#соляные-шипы" id="соляные-шипы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Шипы, равные {v}% брони |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Соляные шипы](../Stats/Stats-POWER.md#power_salt_spines) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Броня](../Stats/Stats-HERO.md#stock_armor); to: [Шипы](../Stats/Stats-HERO.md#stock_thorns) |


## POWER_SHARD_SPLINTER <a href="#power_shard_splinter" id="power_shard_splinter"></a>

### Осколочный скол <a href="#осколочный-скол" id="осколочный-скол"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар раскалывается и бьёт случайного врага на {v}% урона оружия молнией |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Осколочный скол](../Stats/Stats-POWER.md#power_shard_splinter) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `LIGHTNING`; to: `RANDOM` |


## POWER_SHATTER <a href="#power_shatter" id="power_shatter"></a>

### Раскол <a href="#раскол" id="раскол"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитый охлаждённый враг раскалывается: {v}% его максимума здоровья холодом остальным врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Раскол](../Stats/Stats-POWER.md#power_shatter) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `CHILL` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TARGET_LIFE`; Тип: `COLD`; to: `OTHERS` |


## POWER_SMELT <a href="#power_smelt" id="power_smelt"></a>

### Переплавка <a href="#переплавка" id="переплавка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% физического урона превращается в урон огнём |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Переплавка](../Stats/Stats-POWER.md#power_smelt) |
| sheet | Операция: `CONVERT`; Предшествующие зоны: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); to: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire) |


## POWER_SMITH_TEMPER <a href="#power_smith_temper" id="power_smith_temper"></a>

### Закалка кузнеца <a href="#закалка-кузнеца" id="закалка-кузнеца"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения редкости предметов с редких монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Закалка кузнеца](../Stats/Stats-POWER.md#power_smith_temper) |
| world | gain: `RARITY`; against: `RARE` |


## POWER_SOLAR_FLARE <a href="#power_solar_flare" id="power_solar_flare"></a>

### Солнечная вспышка <a href="#солнечная-вспышка" id="солнечная-вспышка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 6 с наносит всем врагам урон огнём, равный {v}% максимума маны |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Солнечная вспышка](../Stats/Stats-POWER.md#power_solar_flare) |
| Событие · `on` | `EVERY` |
| every | 6 |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `MANA`; Тип: `FIRE`; to: `ALL` |


## POWER_SUNS_BLESSING <a href="#power_suns_blessing" id="power_suns_blessing"></a>

### Благословение солнца <a href="#благословение-солнца" id="благословение-солнца"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона при полном здоровье |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Благословение солнца](../Stats/Stats-POWER.md#power_suns_blessing) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_FULL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage) |


## POWER_SOVEREIGN_BRAND <a href="#power_sovereign_brand" id="power_sovereign_brand"></a>

### Клеймо владыки <a href="#клеймо-владыки" id="клеймо-владыки"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по горящим врагам опаляют остальных врагов на {v}% урона оружия огнём |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Клеймо владыки](../Stats/Stats-POWER.md#power_sovereign_brand) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `FIRE`; to: `OTHERS` |


## POWER_SAINTS_GRACE <a href="#power_saints_grace" id="power_saints_grace"></a>

### Милость святой <a href="#милость-святой" id="милость-святой"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Глоток снимает все состояния и даёт барьер в {v}% максимума здоровья на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Милость святой](../Stats/Stats-POWER.md#power_saints_grace) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `CLEANSE`; Действие: `BARRIER`; Длительность: 3 |


## POWER_SEETHING_RAGE <a href="#power_seething_rage" id="power_seething_rage"></a>

### Кипящий гнев <a href="#кипящий-гнев" id="кипящий-гнев"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия +{v}% урона за каждые 10% недостающего здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кипящий гнев](../Stats/Stats-POWER.md#power_seething_rage) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); scale: `MISSING_LIFE` |


## POWER_STARLIGHT_WARD <a href="#power_starlight_ward" id="power_starlight_ward"></a>

### Звёздный заслон <a href="#звёздный-заслон" id="звёздный-заслон"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия разбитый энергощит даёт неуязвимость на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Звёздный заслон](../Stats/Stats-POWER.md#power_starlight_ward) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `SHIELD_BROKEN` |
| Эффекты · `effects` | Действие: `INVULNERABLE` |


## POWER_STARSHARD <a href="#power_starshard" id="power_starshard"></a>

### Звёздный осколок <a href="#звёздный-осколок" id="звёздный-осколок"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Звёздный осколок](../Stats/Stats-POWER.md#power_starshard) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all); Характеристика: [Сопротивление хаосу](../Stats/Stats-HERO.md#stock_resist_chaos); Длительность: 4 |


## POWER_STORM_VOLLEY <a href="#power_storm_volley" id="power_storm_volley"></a>

### Грозовой залп <a href="#грозовой-залп" id="грозовой-залп"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 3-й удар бьёт всех врагов на {v}% урона оружия молнией |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Грозовой залп](../Stats/Stats-POWER.md#power_storm_volley) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 3 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `LIGHTNING`; to: `ALL` |


## POWER_STORMCALL <a href="#power_stormcall" id="power_stormcall"></a>

### Призыв бури <a href="#призыв-бури" id="призыв-бури"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Шок врага даёт {v}% увеличения шанса крита на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Призыв бури](../Stats/Stats-POWER.md#power_stormcall) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `SHOCK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED`; Длительность: 4 |


## POWER_SWALLOW_WHOLE <a href="#power_swallow_whole" id="power_swallow_whole"></a>

### Поглощение <a href="#поглощение" id="поглощение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитый враг рвётся Бездной: {v}% его максимума здоровья хаосом остальным врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Поглощение](../Stats/Stats-POWER.md#power_swallow_whole) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TARGET_LIFE`; Тип: `CHAOS`; to: `OTHERS` |


## POWER_STITCHED_FLESH <a href="#power_stitched_flesh" id="power_stitched_flesh"></a>

### Сшитая плоть <a href="#сшитая-плоть" id="сшитая-плоть"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт {v}% увеличения максимума здоровья на 8 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сшитая плоть](../Stats/Stats-POWER.md#power_stitched_flesh) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Здоровье](../Stats/Stats-HERO.md#stock_health); Операция: `INCREASED`; Длительность: 8; stacks: 5 |


## POWER_SIROCCO <a href="#power_sirocco" id="power_sirocco"></a>

### Сирокко <a href="#сирокко" id="сирокко"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сирокко](../Stats/Stats-POWER.md#power_sirocco) |
| Событие · `on` | `EVERY` |
| every | 6 |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 2; to: `ALL` |


## POWER_SEVENTH_ASCENT <a href="#power_seventh_ascent" id="power_seventh_ascent"></a>

### Седьмое вознесение <a href="#седьмое-вознесение" id="седьмое-вознесение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда энергощит разбит, восстанавливает {v}% энергощита, не чаще раза в 10 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Седьмое вознесение](../Stats/Stats-POWER.md#power_seventh_ascent) |
| Событие · `on` | `SHIELD_BROKEN` |
| Перезарядка, с · `cooldown` | 10 |
| Эффекты · `effects` | Действие: `HEAL`; of: `SHIELD` |


## POWER_STARBOUND_GRACE <a href="#power_starbound_grace" id="power_starbound_grace"></a>

### Звёздная грация <a href="#звёздная-грация" id="звёздная-грация"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к уклонению за каждые 10 ловкости |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Звёздная грация](../Stats/Stats-POWER.md#power_starbound_grace) |
| sheet | Операция: `PER`; Предшествующие зоны: [Ловкость](../Stats/Stats-HERO.md#stock_agility); to: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); per: 10 |


## POWER_SUNDER <a href="#power_sunder" id="power_sunder"></a>

### Раскол брони <a href="#раскол-брони" id="раскол-брони"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 3-й удар заставляет врага получать на {v}% больше физического урона 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Раскол брони](../Stats/Stats-POWER.md#power_sunder) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 3 |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Получаемый физический урон](../Stats/Stats-HERO.md#stock_physical_taken); Длительность: 4 |


## POWER_STARBORN_EDGE <a href="#power_starborn_edge" id="power_starborn_edge"></a>

### Звёздная кромка <a href="#звёздная-кромка" id="звёздная-кромка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт +{v}% к множителю крита на 5 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Звёздная кромка](../Stats/Stats-POWER.md#power_starborn_edge) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Множитель критического удара](../Stats/Stats-HERO.md#stock_critical_multiplier); Длительность: 5; stacks: 5 |


## POWER_SHIELD_BASH <a href="#power_shield_bash" id="power_shield_bash"></a>

### Удар щитом <a href="#удар-щитом" id="удар-щитом"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок оглушает нападавшего на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Удар щитом](../Stats/Stats-POWER.md#power_shield_bash) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `STUN` |


## POWER_SLAG_BURST <a href="#power_slag_burst" id="power_slag_burst"></a>

### Выплеск шлака <a href="#выплеск-шлака" id="выплеск-шлака"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитый горящий враг взрывается: {v}% его максимума здоровья огнём остальным врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Выплеск шлака](../Stats/Stats-POWER.md#power_slag_burst) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TARGET_LIFE`; Тип: `FIRE`; to: `OTHERS` |


## POWER_SNOWMELT <a href="#power_snowmelt" id="power_snowmelt"></a>

### Талый снег <a href="#талый-снег" id="талый-снег"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Поджог восстанавливает {v}% максимума здоровья, не чаще раза в 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Талый снег](../Stats/Stats-POWER.md#power_snowmelt) |
| Событие · `on` | `AILED` |
| checks | check: `AILMENT`; word: `IGNITE` |
| Перезарядка, с · `cooldown` | 3 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_SPREADING_STING <a href="#power_spreading_sting" id="power_spreading_sting"></a>

### Расползающееся жало <a href="#расползающееся-жало" id="расползающееся-жало"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Отравление врага с шансом {v}% отравляет ещё и случайного врага |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Расползающееся жало](../Stats/Stats-POWER.md#power_spreading_sting) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `POISON` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `POISON`; to: `RANDOM` |


## POWER_SEARING_RIM <a href="#power_searing_rim" id="power_searing_rim"></a>

### Раскалённый обод <a href="#раскалённый-обод" id="раскалённый-обод"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок обжигает нападавшего на {v}% урона оружия огнём |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Раскалённый обод](../Stats/Stats-POWER.md#power_searing_rim) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `FIRE` |


## POWER_SILVER_REFLEX <a href="#power_silver_reflex" id="power_silver_reflex"></a>

### Ртутный рефлекс <a href="#ртутный-рефлекс" id="ртутный-рефлекс"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия уклонение даёт {v}% увеличения скорости атаки на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ртутный рефлекс](../Stats/Stats-POWER.md#power_silver_reflex) |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3 |


## POWER_SCORCHED_PATH <a href="#power_scorched_path" id="power_scorched_path"></a>

### Выжженная тропа <a href="#выжженная-тропа" id="выжженная-тропа"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона от горения за каждого врага под состоянием, до 5 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Выжженная тропа](../Stats/Stats-POWER.md#power_scorched_path) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон от горения](../Stats/Stats-HERO.md#stock_burning_damage); scale: `AILED_FOES`; cap: 5 |


## POWER_SECOND_HELPING <a href="#power_second_helping" id="power_second_helping"></a>

### Добавка <a href="#добавка" id="добавка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство с шансом {v}% даёт всем флягам 5 зарядов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Добавка](../Stats/Stats-POWER.md#power_second_helping) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `CHARGES`; Количество: 5 |


## POWER_SILENT_OPENING <a href="#power_silent_opening" id="power_silent_opening"></a>

### Тихое начало <a href="#тихое-начало" id="тихое-начало"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса крита в первые 3 с боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тихое начало](../Stats/Stats-POWER.md#power_silent_opening) |
| Событие · `on` | `STANDING` |
| checks | check: `FIGHT_BEFORE`; Значение: 3 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED` |


## POWER_SLAG_SHELL <a href="#power_slag_shell" id="power_slag_shell"></a>

### Шлаковый панцирь <a href="#шлаковый-панцирь" id="шлаковый-панцирь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% ко всем сопротивлениям стихиям, пока здоровья меньше 50% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Шлаковый панцирь](../Stats/Stats-POWER.md#power_slag_shell) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_BELOW`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all) |


## POWER_SEA_HUM <a href="#power_sea_hum" id="power_sea_hum"></a>

### Гул моря <a href="#гул-моря" id="гул-моря"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения урона чар, пока маны больше 50% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Гул моря](../Stats/Stats-POWER.md#power_sea_hum) |
| Событие · `on` | `STANDING` |
| checks | check: `MANA_ABOVE`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон чар](../Stats/Stats-HERO.md#stock_spell_damage); Операция: `INCREASED` |


## POWER_SHARD_WARD <a href="#power_shard_ward" id="power_shard_ward"></a>

### Осколочный заслон <a href="#осколочный-заслон" id="осколочный-заслон"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Полученный крит даёт барьер в {v}% максимума здоровья на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Осколочный заслон](../Stats/Stats-POWER.md#power_shard_ward) |
| Событие · `on` | `CRIT_TAKEN` |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_SMOULDER <a href="#power_smoulder" id="power_smoulder"></a>

### Тление <a href="#тление" id="тление"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона против горящих врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тление](../Stats/Stats-POWER.md#power_smoulder) |
| Событие · `on` | `STANDING` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage) |


## POWER_STAR_THREAD <a href="#power_star_thread" id="power_star_thread"></a>

### Звёздная нить <a href="#звёздная-нить" id="звёздная-нить"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Заклинание обрушивает звезду на случайного врага: урон молнией в {v}% интеллекта, не чаще раза в 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Звёздная нить](../Stats/Stats-POWER.md#power_star_thread) |
| Событие · `on` | `SKILL_USE` |
| checks | check: `SPELL` |
| Перезарядка, с · `cooldown` | 3 |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `INTELLECT`; Тип: `LIGHTNING`; to: `RANDOM` |


## POWER_SACRIFICIAL_PACT <a href="#power_sacrificial_pact" id="power_sacrificial_pact"></a>

### Жертвенный договор <a href="#жертвенный-договор" id="жертвенный-договор"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к энергощиту за каждый 1% сопротивления хаосу |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жертвенный договор](../Stats/Stats-POWER.md#power_sacrificial_pact) |
| sheet | Операция: `PER`; Предшествующие зоны: [Сопротивление хаосу](../Stats/Stats-HERO.md#stock_resist_chaos); to: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield); per: 1 |


## POWER_SURVEY <a href="#power_survey" id="power_survey"></a>

### Разведка <a href="#разведка" id="разведка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% повышения редкости предметов с монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Разведка](../Stats/Stats-POWER.md#power_survey) |
| world | gain: `RARITY` |


## POWER_SURVEYORS_NOTES <a href="#power_surveyors_notes" id="power_surveyors_notes"></a>

### Записи землемера <a href="#записи-землемера" id="записи-землемера"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения опыта с монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Записи землемера](../Stats/Stats-POWER.md#power_surveyors_notes) |
| world | gain: `EXPERIENCE` |


## POWER_SCOUTS_VOLLEY <a href="#power_scouts_volley" id="power_scouts_volley"></a>

### Залп разведчика <a href="#залп-разведчика" id="залп-разведчика"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | В начале боя залп бьёт всех врагов на {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Залп разведчика](../Stats/Stats-POWER.md#power_scouts_volley) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `PHYSICAL`; to: `ALL` |


## POWER_SWEEPING_BEAM <a href="#power_sweeping_beam" id="power_sweeping_beam"></a>

### Бегущий луч <a href="#бегущий-луч" id="бегущий-луч"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 6 с даёт {v}% увеличения уклонения на 2 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бегущий луч](../Stats/Stats-POWER.md#power_sweeping_beam) |
| Событие · `on` | `EVERY` |
| every | 6 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Операция: `INCREASED`; Длительность: 2 |


## POWER_SMUGGLERS_PERSISTENCE <a href="#power_smugglers_persistence" id="power_smugglers_persistence"></a>

### Упорство контрабандиста <a href="#упорство-контрабандиста" id="упорство-контрабандиста"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона за каждый удар подряд по одному врагу, до 8 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Упорство контрабандиста](../Stats/Stats-POWER.md#power_smugglers_persistence) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); scale: `MOMENTUM`; cap: 8 |


## POWER_SHORE_WIND <a href="#power_shore_wind" id="power_shore_wind"></a>

### Береговой ветер <a href="#береговой-ветер" id="береговой-ветер"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения уклонения в первые 4 с боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Береговой ветер](../Stats/Stats-POWER.md#power_shore_wind) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Операция: `INCREASED`; Длительность: 4 |


## POWER_SHIFT_CHANGE <a href="#power_shift_change" id="power_shift_change"></a>

### Пересменка <a href="#пересменка" id="пересменка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Пройденный этап даёт {v} зарядов каждой фляге |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пересменка](../Stats/Stats-POWER.md#power_shift_change) |
| Событие · `on` | `STAGE_CLEAR` |
| Эффекты · `effects` | Действие: `CHARGES` |


## POWER_SPINNERS_SPARK <a href="#power_spinners_spark" id="power_spinners_spark"></a>

### Искра прядильщицы <a href="#искра-прядильщицы" id="искра-прядильщицы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар с шансом {v}% шокирует |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Искра прядильщицы](../Stats/Stats-POWER.md#power_spinners_spark) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `SHOCK` |


## POWER_STAIR_BREATH <a href="#power_stair_breath" id="power_stair_breath"></a>

### Вдох на ступени <a href="#вдох-на-ступени" id="вдох-на-ступени"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Пройденный этап восстанавливает {v}% максимума здоровья в начале следующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вдох на ступени](../Stats/Stats-POWER.md#power_stair_breath) |
| Событие · `on` | `STAGE_CLEAR` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_SKY_ECHO <a href="#power_sky_echo" id="power_sky_echo"></a>

### Небесное эхо <a href="#небесное-эхо" id="небесное-эхо"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удар с шансом {v}% отзывается эхом через 1 с на весь свой урон |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Небесное эхо](../Stats/Stats-POWER.md#power_sky_echo) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: [Отзвук](../../Skills/Skills-SCION.md#echo); Количество: 100; Длительность: 1 |


## POWER_SURVEYORS_TITHE <a href="#power_surveyors_tithe" id="power_surveyors_tithe"></a>

### Десятина землемера <a href="#десятина-землемера" id="десятина-землемера"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения золота с монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Десятина землемера](../Stats/Stats-POWER.md#power_surveyors_tithe) |
| world | gain: `GOLD` |


## POWER_STALKING_FOCUS <a href="#power_stalking_focus" id="power_stalking_focus"></a>

### Сосредоточение охотника <a href="#сосредоточение-охотника" id="сосредоточение-охотника"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса крита за каждый удар подряд по одному врагу, до 6 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сосредоточение охотника](../Stats/Stats-POWER.md#power_stalking_focus) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED`; scale: `MOMENTUM`; cap: 6 |


## POWER_SECOND_STING <a href="#power_second_sting" id="power_second_sting"></a>

### Второе жало <a href="#второе-жало" id="второе-жало"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 3-й удар жалит снова через 1 с на {v}% урона хаосом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Второе жало](../Stats/Stats-POWER.md#power_second_sting) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 3 |
| Эффекты · `effects` | Действие: [Отзвук](../../Skills/Skills-SCION.md#echo); Длительность: 1; Тип: `CHAOS` |


## POWER_SWARMING_FRENZY <a href="#power_swarming_frenzy" id="power_swarming_frenzy"></a>

### Роевое бешенство <a href="#роевое-бешенство" id="роевое-бешенство"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Отравление врага даёт {v}% увеличения скорости атаки на 3 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Роевое бешенство](../Stats/Stats-POWER.md#power_swarming_frenzy) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `POISON` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3; stacks: 5 |


## POWER_SERPENT_INSIGHT <a href="#power_serpent_insight" id="power_serpent_insight"></a>

### Змеиное прозрение <a href="#змеиное-прозрение" id="змеиное-прозрение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия +{v}% проникания хаоса |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Змеиное прозрение](../Stats/Stats-POWER.md#power_serpent_insight) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Пробивание хаоса](../Stats/Stats-HERO.md#stock_penetrate_chaos) |


## POWER_SPORE_BURST <a href="#power_spore_burst" id="power_spore_burst"></a>

### Споровый выброс <a href="#споровый-выброс" id="споровый-выброс"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Пока на вас недуг, {v}% урона ударов по вам выбрасывается хаосом во всех врагов на поле |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Споровый выброс](../Stats/Stats-POWER.md#power_spore_burst) |
| Событие · `on` | `HIT_TAKEN` |
| checks | check: `SELF_AILED` |
| Эффекты · `effects` | Действие: `RETALIATE`; Тип: `CHAOS` |


## POWER_SCATTER_THE_PACK <a href="#power_scatter_the_pack" id="power_scatter_the_pack"></a>

### Разогнать стаю <a href="#разогнать-стаю" id="разогнать-стаю"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство врага, пока их не меньше 3, бьёт всех остальных на {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Разогнать стаю](../Stats/Stats-POWER.md#power_scatter_the_pack) |
| Событие · `on` | `KILL` |
| checks | check: `FOES_AT_LEAST`; Значение: 3 |
| Эффекты · `effects` | Действие: `DAMAGE`; to: `OTHERS` |


## POWER_SMOULDER_SIGHT <a href="#power_smoulder_sight" id="power_smoulder_sight"></a>

### Тлеющий взгляд <a href="#тлеющий-взгляд" id="тлеющий-взгляд"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса крита по горящим врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тлеющий взгляд](../Stats/Stats-POWER.md#power_smoulder_sight) |
| Событие · `on` | `STANDING` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED` |


## POWER_STILL_POSTURE <a href="#power_still_posture" id="power_still_posture"></a>

### Неподвижная стойка <a href="#неподвижная-стойка" id="неподвижная-стойка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% шанса блока, пока на вас нет недугов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Неподвижная стойка](../Stats/Stats-POWER.md#power_still_posture) |
| Событие · `on` | `STANDING` |
| checks | check: `SELF_CLEAN` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс блока](../Stats/Stats-HERO.md#stock_block_chance) |


## POWER_SWITCHED_SIDES <a href="#power_switched_sides" id="power_switched_sides"></a>

### Сменённая сторона <a href="#сменённая-сторона" id="сменённая-сторона"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | В начале боя все враги 5 с получают на {v}% больше урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сменённая сторона](../Stats/Stats-POWER.md#power_switched_sides) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken); Длительность: 5; to: `ALL` |


## POWER_STOKED_GRIP <a href="#power_stoked_grip" id="power_stoked_grip"></a>

### Раздутая хватка <a href="#раздутая-хватка" id="раздутая-хватка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения урона огнём за каждый удар подряд по одному врагу, до 8 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Раздутая хватка](../Stats/Stats-POWER.md#power_stoked_grip) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire); Операция: `INCREASED`; scale: `MOMENTUM`; cap: 8 |


## POWER_SCORCHED_ARRIVAL <a href="#power_scorched_arrival" id="power_scorched_arrival"></a>

### Опалённый приход <a href="#опалённый-приход" id="опалённый-приход"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | С шансом {v}% в начале боя поджигает всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Опалённый приход](../Stats/Stats-POWER.md#power_scorched_arrival) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `IGNITE`; to: `ALL` |


## POWER_SPREADING_ROT <a href="#power_spreading_rot" id="power_spreading_rot"></a>

### Ползучая гниль <a href="#ползучая-гниль" id="ползучая-гниль"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона за каждого врага под недугом, до 5 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ползучая гниль](../Stats/Stats-POWER.md#power_spreading_rot) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); scale: `AILED_FOES`; cap: 5 |


## POWER_SIDESTEP <a href="#power_sidestep" id="power_sidestep"></a>

### Шаг в сторону <a href="#шаг-в-сторону" id="шаг-в-сторону"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение с шансом {v}% делает следующую атаку мгновенной |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Шаг в сторону](../Stats/Stats-POWER.md#power_sidestep) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `RUSH` |


## POWER_SHARED_MALADY <a href="#power_shared_malady" id="power_shared_malady"></a>

### Общий недуг <a href="#общий-недуг" id="общий-недуг"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Глоток с шансом {v}% отравляет всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Общий недуг](../Stats/Stats-POWER.md#power_shared_malady) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `POISON`; to: `ALL` |


## POWER_SPRIG_KINDLING <a href="#power_sprig_kindling" id="power_sprig_kindling"></a>

### Растопка <a href="#растопка" id="растопка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство заклинанием восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Растопка](../Stats/Stats-POWER.md#power_sprig_kindling) |
| Событие · `on` | `KILL` |
| checks | check: `SPELL` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_SUNLIT_ARROWS <a href="#power_sunlit_arrows" id="power_sunlit_arrows"></a>

### Солнечные стрелы <a href="#солнечные-стрелы" id="солнечные-стрелы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 4-й удар пускает солнечную стрелу в случайного врага: {v}% урона оружия огнём |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Солнечные стрелы](../Stats/Stats-POWER.md#power_sunlit_arrows) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 4 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `FIRE`; to: `RANDOM` |


## POWER_STONE_RESOLVE <a href="#power_stone_resolve" id="power_stone_resolve"></a>

### Тупая решимость <a href="#тупая-решимость" id="тупая-решимость"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Вы не наносите критических ударов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тупая решимость](../Stats/Stats-POWER.md#power_stone_resolve) |
| sheet | Операция: `SET`; to: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Значение: 0; Операция: `SET`; to: [Шанс крита заклинаний](../Stats/Stats-HERO.md#stock_spell_critical_chance); Значение: 0 |


## POWER_STONE_CRUSH <a href="#power_stone_crush" id="power_stone_crush"></a>

### Каменный натиск <a href="#каменный-натиск" id="каменный-натиск"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс оглушить цель на 0,5 с при ударе |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Каменный натиск](../Stats/Stats-POWER.md#power_stone_crush) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `STUN`; Длительность: 0.5 |


## POWER_SAILCLOTH_FAIR_WIND <a href="#power_sailcloth_fair_wind" id="power_sailcloth_fair_wind"></a>

### Попутный ветер <a href="#попутный-ветер" id="попутный-ветер"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% к скорости атаки на 3 с в начале боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Попутный ветер](../Stats/Stats-POWER.md#power_sailcloth_fair_wind) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3 |


## POWER_SAILCLOTH_SALVAGE <a href="#power_sailcloth_salvage" id="power_sailcloth_salvage"></a>

### Выброшенное морем <a href="#выброшенное-морем" id="выброшенное-морем"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Враги роняют на {v}% больше золота |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Выброшенное морем](../Stats/Stats-POWER.md#power_sailcloth_salvage) |
| world | gain: `GOLD` |


## POWER_STORMBROKEN_BURST <a href="#power_stormbroken_burst" id="power_stormbroken_burst"></a>

### Сердце грозы <a href="#сердце-грозы" id="сердце-грозы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитые шокированные враги взрываются: {v}% их макс. здоровья уроном молнией по остальным |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сердце грозы](../Stats/Stats-POWER.md#power_stormbroken_burst) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `SHOCK` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `LIGHTNING`; of: `TARGET_LIFE`; to: `OTHERS` |


## POWER_STORMBROKEN_PULSE <a href="#power_stormbroken_pulse" id="power_stormbroken_pulse"></a>

### Сбитый пульс <a href="#сбитый-пульс" id="сбитый-пульс"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс шокировать при ударе |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сбитый пульс](../Stats/Stats-POWER.md#power_stormbroken_pulse) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `SHOCK` |


## POWER_STORMWAKE_BOLT <a href="#power_stormwake_bolt" id="power_stormwake_bolt"></a>

### След грозы <a href="#след-грозы" id="след-грозы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 2 с молния бьёт случайного врага на {v}% урона оружия и шокирует случайного врага |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [След грозы](../Stats/Stats-POWER.md#power_stormwake_bolt) |
| Событие · `on` | `EVERY` |
| every | 2 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `LIGHTNING`; to: `RANDOM`; Действие: `AILMENT`; Состояние: `SHOCK`; to: `RANDOM` |


## POWER_STORMWAKE_STRIDE <a href="#power_stormwake_stride" id="power_stormwake_stride"></a>

### Горящий песок <a href="#горящий-песок" id="горящий-песок"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение даёт {v}% к урону молнией на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Горящий песок](../Stats/Stats-POWER.md#power_stormwake_stride) |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон молнией](../Stats/Stats-HERO.md#stock_attack_lightning); Операция: `INCREASED`; Длительность: 3 |


## POWER_SPECTRAL_LEECH <a href="#power_spectral_leech" id="power_spectral_leech"></a>

### Призрачный вампиризм <a href="#призрачный-вампиризм" id="призрачный-вампиризм"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Вампиризм здоровья восстанавливает ЭЩ вместо здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Призрачный вампиризм](../Stats/Stats-POWER.md#power_spectral_leech) |
| sheet | Операция: `ADD`; to: [Вампиризм в ЭЩ](../Stats/Stats-HERO.md#stock_leech_to_shield) |


## POWER_SERPENT_COIL_FORTUNE <a href="#power_serpent_coil_fortune" id="power_serpent_coil_fortune"></a>

### Мах или недомах <a href="#мах-или-недомах" id="мах-или-недомах"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый бой — одно из двух: «Мах» — +30% к урону или «Недомах» — −20% к урону |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Мах или недомах](../Stats/Stats-POWER.md#power_serpent_coil_fortune) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `ONE_OF`; Варианты: Действие: `BUFF`; Свойства: 30% увеличение урона · [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 3600; Действие: `BUFF`; Свойства: -20% увеличение урона · [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 3600 |


## POWER_SERPENT_CONSTRICT <a href="#power_serpent_constrict" id="power_serpent_constrict"></a>

### Удушение <a href="#удушение" id="удушение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс оглушить цель на 0,4 с при ударе |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Удушение](../Stats/Stats-POWER.md#power_serpent_constrict) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `STUN`; Длительность: 0.4 |


## POWER_SEAL_HARMONY <a href="#power_seal_harmony" id="power_seal_harmony"></a>

### Парный рост <a href="#парный-рост" id="парный-рост"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Если второе кольцо уникальное, модификаторы этого кольца сильнее на {v}% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Парный рост](../Stats/Stats-POWER.md#power_seal_harmony) |
| slots | Операция: `AMPLIFY`; pick: `SELF`; ifOtherUnique: Да |


## POWER_SEAL_FACET <a href="#power_seal_facet" id="power_seal_facet"></a>

### Грань <a href="#грань" id="грань"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Крит заклинанием восстанавливает {v}% максимума маны |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Грань](../Stats/Stats-POWER.md#power_seal_facet) |
| Событие · `on` | `CRIT` |
| checks | check: `SPELL` |
| Эффекты · `effects` | Действие: `HEAL`; of: `MANA` |


## POWER_STAR_ASHFALL <a href="#power_star_ashfall" id="power_star_ashfall"></a>

### Пеплопад <a href="#пеплопад" id="пеплопад"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 6 с пепел падает на всех врагов: {v}% урона оружия огнём |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пеплопад](../Stats/Stats-POWER.md#power_star_ashfall) |
| Событие · `on` | `EVERY` |
| every | 6 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `FIRE`; to: `ALL` |


## POWER_SHOREWATCH_VIGIL <a href="#power_shorewatch_vigil" id="power_shorewatch_vigil"></a>

### Бдение <a href="#бдение" id="бдение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% к броне на 4 с в начале боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бдение](../Stats/Stats-POWER.md#power_shorewatch_vigil) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Броня](../Stats/Stats-HERO.md#stock_armor); Операция: `INCREASED`; Длительность: 4 |


## POWER_SHOREWATCH_BEACON <a href="#power_shorewatch_beacon" id="power_shorewatch_beacon"></a>

### Сторожевой огонь <a href="#сторожевой-огонь" id="сторожевой-огонь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сторожевой огонь](../Stats/Stats-POWER.md#power_shorewatch_beacon) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_SOVEREIGN_BLOOD <a href="#power_sovereign_blood" id="power_sovereign_blood"></a>

### Кровь Владыки <a href="#кровь-владыки" id="кровь-владыки"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% физического урона атак превращается в огонь |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кровь Владыки](../Stats/Stats-POWER.md#power_sovereign_blood) |
| sheet | Операция: `CONVERT`; Предшествующие зоны: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); to: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire) |


## POWER_SOVEREIGN_PYRE <a href="#power_sovereign_pyre" id="power_sovereign_pyre"></a>

### Погребальный костёр <a href="#погребальный-костёр" id="погребальный-костёр"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитые горящие враги взрываются: {v}% их макс. здоровья уроном огнём по остальным |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Погребальный костёр](../Stats/Stats-POWER.md#power_sovereign_pyre) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `FIRE`; of: `TARGET_LIFE`; to: `OTHERS` |


## POWER_SPRING_BOND <a href="#power_spring_bond" id="power_spring_bond"></a>

### Родниковая связь <a href="#родниковая-связь" id="родниковая-связь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда питомец убивает врага, вы восстанавливаете {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Родниковая связь](../Stats/Stats-POWER.md#power_spring_bond) |
| Событие · `on` | `PET_KILL` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_SPRING_RENEWAL <a href="#power_spring_renewal" id="power_spring_renewal"></a>

### Обновление <a href="#обновление" id="обновление"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 5 с питомец восстанавливает {v}% своего макс. здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Обновление](../Stats/Stats-POWER.md#power_spring_renewal) |
| Событие · `on` | `EVERY` |
| every | 5 |
| Эффекты · `effects` | Действие: `HEAL`; to: `PET` |


## POWER_SIREN_LUCK <a href="#power_siren_luck" id="power_siren_luck"></a>

### Удача сирены <a href="#удача-сирены" id="удача-сирены"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Ваш шанс крита удачлив: бросается дважды |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Удача сирены](../Stats/Stats-POWER.md#power_siren_luck) |
| sheet | Операция: `ADD`; to: [Удачливые криты](../Stats/Stats-HERO.md#stock_lucky_crit) |


## POWER_SIREN_LURE <a href="#power_siren_lure" id="power_siren_lure"></a>

### Ложная песнь <a href="#ложная-песнь" id="ложная-песнь"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ложная песнь](../Stats/Stats-POWER.md#power_siren_lure) |
| sheet | Операция: `ADD`; to: [Урон некритических ударов](../Stats/Stats-HERO.md#stock_non_crit_damage) |


## POWER_SPITE_CHILL <a href="#power_spite_chill" id="power_spite_chill"></a>

### Холодная злоба <a href="#холодная-злоба" id="холодная-злоба"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия {v}% физического урона от ударов принимается как холод |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Холодная злоба](../Stats/Stats-POWER.md#power_spite_chill) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Физический урон как холод](../Stats/Stats-HERO.md#stock_physical_taken_as_cold) |


## POWER_SPITE_BITE <a href="#power_spite_bite" id="power_spite_bite"></a>

### Злой укус <a href="#злой-укус" id="злой-укус"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия {v}% к урону холодом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Злой укус](../Stats/Stats-POWER.md#power_spite_bite) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон холодом](../Stats/Stats-HERO.md#stock_attack_cold); Операция: `INCREASED` |


## POWER_STONEBLOOD_CLOT <a href="#power_stoneblood_clot" id="power_stoneblood_clot"></a>

### Окаменевшая кровь <a href="#окаменевшая-кровь" id="окаменевшая-кровь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия {v}% урона по здоровью растягивается на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Окаменевшая кровь](../Stats/Stats-POWER.md#power_stoneblood_clot) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Отложенный урон по здоровью](../Stats/Stats-HERO.md#stock_life_damage_delayed); Срок отложенного урона · ADD · 4 · [Срок отложенного урона](../Stats/Stats-HERO.md#stock_life_damage_delay) |


## POWER_STONEBLOOD_STILL <a href="#power_stoneblood_still" id="power_stoneblood_still"></a>

### Замершее сердце <a href="#замершее-сердце" id="замершее-сердце"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При применении даёт барьер в {v}% максимума здоровья на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Замершее сердце](../Stats/Stats-POWER.md#power_stoneblood_still) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 4 |


## POWER_SUNSET_ARMY <a href="#power_sunset_army" id="power_sunset_army"></a>

### Войско заката <a href="#войско-заката" id="войско-заката"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия умения бьют +{v} цели |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Войско заката](../Stats/Stats-POWER.md#power_sunset_army) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Цели умений](../Stats/Stats-HERO.md#stock_skill_targets) |


## POWER_SUNSET_GLORY <a href="#power_sunset_glory" id="power_sunset_glory"></a>

### Последняя слава <a href="#последняя-слава" id="последняя-слава"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия {v}% к урону |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Последняя слава](../Stats/Stats-POWER.md#power_sunset_glory) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `INCREASED` |


## POWER_STORM_VESSEL_CHARGE <a href="#power_storm_vessel_charge" id="power_storm_vessel_charge"></a>

### Заряженный сосуд <a href="#заряженный-сосуд" id="заряженный-сосуд"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия +{v}% к шансу шока |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Заряженный сосуд](../Stats/Stats-POWER.md#power_storm_vessel_charge) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс шока](../Stats/Stats-HERO.md#stock_shock_chance) |


## POWER_STORM_VESSEL_DRAIN <a href="#power_storm_vessel_drain" id="power_storm_vessel_drain"></a>

### Грозовой вампиризм <a href="#грозовой-вампиризм" id="грозовой-вампиризм"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия {v}% урона возвращается здоровьем и +20% к урону молнией |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Грозовой вампиризм](../Stats/Stats-POWER.md#power_storm_vessel_drain) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Общий вампиризм](../Stats/Stats-HERO.md#stock_leech_all); 20% увеличение урона от молнии · [Урон молнией](../Stats/Stats-HERO.md#stock_attack_lightning) |


## POWER_SURF_ROAR <a href="#power_surf_roar" id="power_surf_roar"></a>

### Рёв прибоя <a href="#рёв-прибоя" id="рёв-прибоя"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия {v}% больше урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Рёв прибоя](../Stats/Stats-POWER.md#power_surf_roar) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `MORE` |


## POWER_SURF_BREAKER <a href="#power_surf_breaker" id="power_surf_breaker"></a>

### Вал <a href="#вал" id="вал"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При применении {v}% шанс оглушить всех врагов на 0,5 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вал](../Stats/Stats-POWER.md#power_surf_breaker) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `STUN`; to: `ALL`; Длительность: 0.5 |


## POWER_SOBER_CLARITY <a href="#power_sober_clarity" id="power_sober_clarity"></a>

### Трезвая ясность <a href="#трезвая-ясность" id="трезвая-ясность"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% максимума здоровья добавляется к мане |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Трезвая ясность](../Stats/Stats-POWER.md#power_sober_clarity) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Здоровье](../Stats/Stats-HERO.md#stock_health); to: [Мана](../Stats/Stats-HERO.md#stock_mana) |


## POWER_SOBER_ABSTINENCE <a href="#power_sober_abstinence" id="power_sober_abstinence"></a>

### Воздержание <a href="#воздержание" id="воздержание"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Воздержание](../Stats/Stats-POWER.md#power_sober_abstinence) |
| sheet | Операция: `MORE`; to: [Здоровье](../Stats/Stats-HERO.md#stock_health) |


## POWER_SHRUG_OFF <a href="#power_shrug_off" id="power_shrug_off"></a>

### Отмахнуться <a href="#отмахнуться" id="отмахнуться"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар по вам даёт барьер в 15% здоровья на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отмахнуться](../Stats/Stats-POWER.md#power_shrug_off) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `CRIT_TAKEN` |
| Перезарядка, с · `cooldown` | 6 |
| Эффекты · `effects` | Действие: `BARRIER`; Количество: 15 |


## POWER_SPELLSTORM <a href="#power_spellstorm" id="power_spellstorm"></a>

### Буря чар <a href="#буря-чар" id="буря-чар"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критические заклинания дают +{v} урона от молнии к заклинаниям на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Буря чар](../Stats/Stats-POWER.md#power_spellstorm) |
| Событие · `on` | `CRIT` |
| checks | check: `SPELL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Грозовой урон к заклинаниям](../Stats/Stats-HERO.md#stock_spell_add_lightning); Длительность: 4 |


## POWER_SHROUDED_STEP <a href="#power_shrouded_step" id="power_shrouded_step"></a>

### Сокрытый шаг <a href="#сокрытый-шаг" id="сокрытый-шаг"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение даёт {v}% увеличения уклонения на 3 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сокрытый шаг](../Stats/Stats-POWER.md#power_shrouded_step) |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Операция: `INCREASED`; Длительность: 3; stacks: 3 |


## POWER_SPORE_CLOUD <a href="#power_spore_cloud" id="power_spore_cloud"></a>

### Облако спор <a href="#облако-спор" id="облако-спор"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 5 с с шансом {v}% отравляет всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Облако спор](../Stats/Stats-POWER.md#power_spore_cloud) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `EVERY` |
| every | 5 |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `POISON`; to: `ALL` |


## POWER_SACRIFICIAL_EDGE <a href="#power_sacrificial_edge" id="power_sacrificial_edge"></a>

### Жертвенная кромка <a href="#жертвенная-кромка" id="жертвенная-кромка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Криты стоят 3% здоровья и наносят ещё {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жертвенная кромка](../Stats/Stats-POWER.md#power_sacrificial_edge) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `HURT`; of: `LIFE`; Количество: 3; Действие: `DAMAGE` |


## POWER_STAR_HEART <a href="#power_star_heart" id="power_star_heart"></a>

### Звёздное сердце <a href="#звёздное-сердце" id="звёздное-сердце"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 5 с восстанавливает {v}% энергощита |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Звёздное сердце](../Stats/Stats-POWER.md#power_star_heart) |
| Событие · `on` | `EVERY` |
| every | 5 |
| Эффекты · `effects` | Действие: `HEAL`; of: `SHIELD` |


## POWER_SOVEREIGN_ORDER <a href="#power_sovereign_order" id="power_sovereign_order"></a>

### Указ владыки <a href="#указ-владыки" id="указ-владыки"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | В начале боя все враги получают на {v}% больше урона 5 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Указ владыки](../Stats/Stats-POWER.md#power_sovereign_order) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken); Длительность: 5; to: `ALL` |


## POWER_SULPHUR_BREATH <a href="#power_sulphur_breath" id="power_sulphur_breath"></a>

### Серное дыхание <a href="#серное-дыхание" id="серное-дыхание"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удар по вам наносит {v}% полученного урона огнём всем врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Серное дыхание](../Stats/Stats-POWER.md#power_sulphur_breath) |
| Событие · `on` | `HIT_TAKEN` |
| Перезарядка, с · `cooldown` | 1 |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TAKEN`; Тип: `FIRE`; to: `ALL` |


## POWER_SKYFORGE <a href="#power_skyforge" id="power_skyforge"></a>

### Небесная кузня <a href="#небесная-кузня" id="небесная-кузня"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% физического урона атак обращается в молнию |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Небесная кузня](../Stats/Stats-POWER.md#power_skyforge) |
| sheet | Операция: `CONVERT`; Предшествующие зоны: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); to: [Урон молнией](../Stats/Stats-HERO.md#stock_attack_lightning) |


## POWER_SHARED_SOUL <a href="#power_shared_soul" id="power_shared_soul"></a>

### Общая душа <a href="#общая-душа" id="общая-душа"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Питомец получает {v}% вашего максимума здоровья; максимум здоровья на 15% меньше |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Общая душа](../Stats/Stats-POWER.md#power_shared_soul) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Здоровье](../Stats/Stats-HERO.md#stock_health); to: [Прибавка здоровья питомца](../Stats/Stats-HERO.md#stock_pet_life); Операция: `MORE`; to: [Здоровье](../Stats/Stats-HERO.md#stock_health); Значение: -15 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
