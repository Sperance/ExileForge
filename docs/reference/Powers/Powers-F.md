# Особые силы · F

## POWER_FROST_ARROW <a href="#power_frost_arrow" id="power_frost_arrow"></a>

### Морозная стрела <a href="#морозная-стрела" id="морозная-стрела"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 4-й удар с шансом {v}% замораживает |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Морозная стрела](../Stats/Stats-POWER.md#power_frost_arrow) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 4 |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `FREEZE` |


## POWER_FORESIGHT <a href="#power_foresight" id="power_foresight"></a>

### Предвидение <a href="#предвидение" id="предвидение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение с шансом {v}% делает ваш следующий удар критическим |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Предвидение](../Stats/Stats-POWER.md#power_foresight) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `NEXT_CRIT` |


## POWER_FEAST <a href="#power_feast" id="power_feast"></a>

### Пир <a href="#пир" id="пир"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к скорости восстановления, пока действует фляга |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пир](../Stats/Stats-POWER.md#power_feast) |
| Событие · `on` | `STANDING` |
| checks | check: `FLASK_RUNNING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость восстановления](../Stats/Stats-HERO.md#stock_recovery_rate) |


## POWER_FLEETING_STEP <a href="#power_fleeting_step" id="power_fleeting_step"></a>

### Мимолётный шаг <a href="#мимолётный-шаг" id="мимолётный-шаг"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При уклонении {v}% шанс сразу подготовить следующую атаку |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Мимолётный шаг](../Stats/Stats-POWER.md#power_fleeting_step) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `RUSH` |


## POWER_FORGE_FOCUS <a href="#power_forge_focus" id="power_forge_focus"></a>

### Взгляд горнила <a href="#взгляд-горнила" id="взгляд-горнила"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к броне за каждый 1% сопротивления огню |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Взгляд горнила](../Stats/Stats-POWER.md#power_forge_focus) |
| sheet | Операция: `PER`; Предшествующие зоны: [Сопротивление огню](../Stats/Stats-HERO.md#stock_resist_fire); to: [Броня](../Stats/Stats-HERO.md#stock_armor); per: 1 |


## POWER_FORGE_BULWARK <a href="#power_forge_bulwark" id="power_forge_bulwark"></a>

### Кованый заслон <a href="#кованый-заслон" id="кованый-заслон"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок даёт +{v}% урона на 3 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кованый заслон](../Stats/Stats-POWER.md#power_forge_bulwark) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 3; stacks: 3 |


## POWER_FORGE_GALE <a href="#power_forge_gale" id="power_forge_gale"></a>

### Горновой вихрь <a href="#горновой-вихрь" id="горновой-вихрь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Глоток из фляги даёт {v}% увеличения скорости атаки на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Горновой вихрь](../Stats/Stats-POWER.md#power_forge_gale) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 4 |


## POWER_FIRST_LIGHT <a href="#power_first_light" id="power_first_light"></a>

### Первый луч <a href="#первый-луч" id="первый-луч"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый бой начинается с барьера в {v}% максимума здоровья на 6 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Первый луч](../Stats/Stats-POWER.md#power_first_light) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 6 |


## POWER_FIRST_BLOOD <a href="#power_first_blood" id="power_first_blood"></a>

### Первобытная добыча <a href="#первобытная-добыча" id="первобытная-добыча"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары добивают врагов, у которых меньше {v}% здоровья, кроме уникальных |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Первобытная добыча](../Stats/Stats-POWER.md#power_first_blood) |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `EXECUTE` |


## POWER_FALLEN_GRACE <a href="#power_fallen_grace" id="power_fallen_grace"></a>

### Павшая благодать <a href="#павшая-благодать" id="павшая-благодать"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения опыта с уникальных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Павшая благодать](../Stats/Stats-POWER.md#power_fallen_grace) |
| world | gain: `EXPERIENCE`; against: `UNIQUE` |


## POWER_FREE_STRIDE <a href="#power_free_stride" id="power_free_stride"></a>

### Вольный шаг <a href="#вольный-шаг" id="вольный-шаг"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Охлаждение с шансом {v}% снимает с вас все состояния |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вольный шаг](../Stats/Stats-POWER.md#power_free_stride) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `AILED` |
| checks | check: `AILMENT`; word: `CHILL` |
| Эффекты · `effects` | Действие: `CLEANSE` |


## POWER_FROZEN_WOUND <a href="#power_frozen_wound" id="power_frozen_wound"></a>

### Мёрзлая рана <a href="#мёрзлая-рана" id="мёрзлая-рана"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство кровоточащего врага с шансом {v}% охлаждает всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Мёрзлая рана](../Stats/Stats-POWER.md#power_frozen_wound) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `BLEED` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `CHILL`; to: `ALL` |


## POWER_FORGE_TEMPER <a href="#power_forge_temper" id="power_forge_temper"></a>

### Кузнечная закалка <a href="#кузнечная-закалка" id="кузнечная-закалка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удар огнём по вам даёт {v}% увеличения брони на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кузнечная закалка](../Stats/Stats-POWER.md#power_forge_temper) |
| Событие · `on` | `HIT_TAKEN` |
| checks | check: `DAMAGE_TYPE`; word: `FIRE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Броня](../Stats/Stats-HERO.md#stock_armor); Операция: `INCREASED`; Длительность: 4; stacks: 3 |


## POWER_FANNED_FLAMES <a href="#power_fanned_flames" id="power_fanned_flames"></a>

### Раздутое пламя <a href="#раздутое-пламя" id="раздутое-пламя"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Поджог врага восстанавливает {v}% маны |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Раздутое пламя](../Stats/Stats-POWER.md#power_fanned_flames) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `HEAL`; of: `MANA` |


## POWER_FROZEN_THRONE <a href="#power_frozen_throne" id="power_frozen_throne"></a>

### Ледяной трон <a href="#ледяной-трон" id="ледяной-трон"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к шансу блока против охлаждённых врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ледяной трон](../Stats/Stats-POWER.md#power_frozen_throne) |
| Событие · `on` | `STANDING` |
| checks | check: `TARGET_AILMENT`; word: `CHILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс блока](../Stats/Stats-HERO.md#stock_block_chance) |


## POWER_FLOATING_PRAYER <a href="#power_floating_prayer" id="power_floating_prayer"></a>

### Всплывшая молитва <a href="#всплывшая-молитва" id="всплывшая-молитва"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При получении удара восстанавливает {v}% маны, не чаще раза в 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Всплывшая молитва](../Stats/Stats-POWER.md#power_floating_prayer) |
| Событие · `on` | `HIT_TAKEN` |
| Перезарядка, с · `cooldown` | 4 |
| Эффекты · `effects` | Действие: `HEAL`; of: `MANA` |


## POWER_FIRST_DRAUGHT <a href="#power_first_draught" id="power_first_draught"></a>

### Первый глоток <a href="#первый-глоток" id="первый-глоток"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары с шансом {v}% вызывают у врага кровотечение |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Первый глоток](../Stats/Stats-POWER.md#power_first_draught) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `BLEED` |


## POWER_FRACTURE_SHOT <a href="#power_fracture_shot" id="power_fracture_shot"></a>

### Раскалывающий выстрел <a href="#раскалывающий-выстрел" id="раскалывающий-выстрел"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 3-й удар раскалывается и бьёт всех остальных врагов на {v}% урона оружия холодом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Раскалывающий выстрел](../Stats/Stats-POWER.md#power_fracture_shot) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 3 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `COLD`; to: `OTHERS` |


## POWER_FERRY_TOLL <a href="#power_ferry_toll" id="power_ferry_toll"></a>

### Плата за переправу <a href="#плата-за-переправу" id="плата-за-переправу"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% урона ударов по вам возвращается всем врагам на поле |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Плата за переправу](../Stats/Stats-POWER.md#power_ferry_toll) |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `RETALIATE` |


## POWER_FEED_ON_THE_FALLEN <a href="#power_feed_on_the_fallen" id="power_feed_on_the_fallen"></a>

### Пир над павшим <a href="#пир-над-павшим" id="пир-над-павшим"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство врага ниже 50% здоровья восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пир над павшим](../Stats/Stats-POWER.md#power_feed_on_the_fallen) |
| Событие · `on` | `KILL` |
| checks | check: `LIFE_BELOW`; Значение: 50 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_FORESEEN_STRIKE <a href="#power_foreseen_strike" id="power_foreseen_strike"></a>

### Предвиденный удар <a href="#предвиденный-удар" id="предвиденный-удар"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Глоток с шансом {v}% делает следующий удар критическим |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Предвиденный удар](../Stats/Stats-POWER.md#power_foreseen_strike) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `NEXT_CRIT` |


## POWER_FIRST_WAVE <a href="#power_first_wave" id="power_first_wave"></a>

### Первая волна <a href="#первая-волна" id="первая-волна"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения скорости атаки в первые 3 с боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Первая волна](../Stats/Stats-POWER.md#power_first_wave) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3 |


## POWER_FAR_TROPHIES <a href="#power_far_trophies" id="power_far_trophies"></a>

### Дальние трофеи <a href="#дальние-трофеи" id="дальние-трофеи"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения редкости предметов с уникальных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Дальние трофеи](../Stats/Stats-POWER.md#power_far_trophies) |
| world | gain: `RARITY`; against: `UNIQUE` |


## POWER_FRONTIER_FIND <a href="#power_frontier_find" id="power_frontier_find"></a>

### Находка за рубежом <a href="#находка-за-рубежом" id="находка-за-рубежом"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса найти карты |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Находка за рубежом](../Stats/Stats-POWER.md#power_frontier_find) |
| world | gain: [Map](../../Equipment/Equipment-MAP.md#map) |


## POWER_FRENZY_ON_KILL <a href="#power_frenzy_on_kill" id="power_frenzy_on_kill"></a>

### Яростное убийство <a href="#яростное-убийство" id="яростное-убийство"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс получить заряд ярости при убийстве |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Яростное убийство](../Stats/Stats-POWER.md#power_frenzy_on_kill) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `CHARGE`; charge: `FRENZY` |


## POWER_FRENZY_ON_HIT <a href="#power_frenzy_on_hit" id="power_frenzy_on_hit"></a>

### Яростные удары <a href="#яростные-удары" id="яростные-удары"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс получить заряд ярости при попадании (откат 0,5 с) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Яростные удары](../Stats/Stats-POWER.md#power_frenzy_on_hit) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Перезарядка, с · `cooldown` | 0.5 |
| Эффекты · `effects` | Действие: `CHARGE`; charge: `FRENZY` |


## POWER_FEATHER_WEIGHT <a href="#power_feather_weight" id="power_feather_weight"></a>

### Невесомость <a href="#невесомость" id="невесомость"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Невесомость](../Stats/Stats-POWER.md#power_feather_weight) |
| sheet | Операция: `MORE`; to: [Урон](../Stats/Stats-HERO.md#stock_damage) |


## POWER_FEATHER_FLURRY <a href="#power_feather_flurry" id="power_feather_flurry"></a>

### Вихрь перьев <a href="#вихрь-перьев" id="вихрь-перьев"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс при убийстве сразу подготовить следующую атаку |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вихрь перьев](../Stats/Stats-POWER.md#power_feather_flurry) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `RUSH` |


## POWER_FEAST_CORPSE_BLAST <a href="#power_feast_corpse_blast" id="power_feast_corpse_blast"></a>

### Лопнувший паёк <a href="#лопнувший-паёк" id="лопнувший-паёк"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитые враги взрываются: {v}% их макс. здоровья физическим уроном по остальным |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Лопнувший паёк](../Stats/Stats-POWER.md#power_feast_corpse_blast) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `PHYSICAL`; of: `TARGET_LIFE`; to: `OTHERS` |


## POWER_FEAST_GORGE <a href="#power_feast_gorge" id="power_feast_gorge"></a>

### Обжорство <a href="#обжорство" id="обжорство"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт {v}% к скорости атаки на 3 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Обжорство](../Stats/Stats-POWER.md#power_feast_gorge) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3; stacks: 3 |


## POWER_FLAMECLOAK_HEARTH <a href="#power_flamecloak_hearth" id="power_flamecloak_hearth"></a>

### Очаг Рубежа <a href="#очаг-рубежа" id="очаг-рубежа"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% физического урона от ударов принимается как огонь |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Очаг Рубежа](../Stats/Stats-POWER.md#power_flamecloak_hearth) |
| sheet | Операция: `ADD`; to: [Физический урон как огонь](../Stats/Stats-HERO.md#stock_physical_taken_as_fire) |


## POWER_FLAMECLOAK_EMBERS <a href="#power_flamecloak_embers" id="power_flamecloak_embers"></a>

### Плащ углей <a href="#плащ-углей" id="плащ-углей"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда вас бьют, {v}% шанс поджечь атакующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Плащ углей](../Stats/Stats-POWER.md#power_flamecloak_embers) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `IGNITE` |


## POWER_FORGE_DOME_TEMPER <a href="#power_forge_dome_temper" id="power_forge_dome_temper"></a>

### Закалённый купол <a href="#закалённый-купол" id="закалённый-купол"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Криты по вам не наносят дополнительного урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Закалённый купол](../Stats/Stats-POWER.md#power_forge_dome_temper) |
| sheet | Операция: `SET`; to: [Множитель крита по цели](../Stats/Stats-HERO.md#stock_critical_taken); Значение: -100 |


## POWER_FORGE_DOME_ANVIL <a href="#power_forge_dome_anvil" id="power_forge_dome_anvil"></a>

### Под молотом <a href="#под-молотом" id="под-молотом"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удар по вам даёт {v}% к броне на 4 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Под молотом](../Stats/Stats-POWER.md#power_forge_dome_anvil) |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Броня](../Stats/Stats-HERO.md#stock_armor); Операция: `INCREASED`; Длительность: 4; stacks: 5 |


## POWER_FERRY_TOGETHER <a href="#power_ferry_together" id="power_ferry_together"></a>

### Переправа вдвоём <a href="#переправа-вдвоём" id="переправа-вдвоём"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда питомец убивает врага, вы получаете {v}% к скорости атаки на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Переправа вдвоём](../Stats/Stats-POWER.md#power_ferry_together) |
| Событие · `on` | `PET_KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3 |


## POWER_FERRY_TOLL_PAID <a href="#power_ferry_toll_paid" id="power_ferry_toll_paid"></a>

### Плата внесена <a href="#плата-внесена" id="плата-внесена"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Ваши убийства восстанавливают питомцу {v}% его макс. здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Плата внесена](../Stats/Stats-POWER.md#power_ferry_toll_paid) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `HEAL`; to: `PET` |


## POWER_FLETCHING_VOID_SHOT <a href="#power_fletching_void_shot" id="power_fletching_void_shot"></a>

### Выстрел пустоты <a href="#выстрел-пустоты" id="выстрел-пустоты"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Умение тратит все заряды силы: выстрел пустоты наносит {v}% урона оружия хаосом за каждый заряд |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Выстрел пустоты](../Stats/Stats-POWER.md#power_fletching_void_shot) |
| Событие · `on` | `SKILL_USE` |
| checks | check: `CHARGES_AT_LEAST`; Значение: 1; word: `POWER` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `CHAOS`; scale: `POWER_CHARGES`; Действие: `CHARGE`; charge: `POWER`; consume: Да |


## POWER_FINERY_GLEAM <a href="#power_finery_gleam" id="power_finery_gleam"></a>

### Убор странника <a href="#убор-странника" id="убор-странника"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Модификаторы других надетых волшебных самоцветов сильнее на {v}% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Убор странника](../Stats/Stats-POWER.md#power_finery_gleam) |
| slots | Операция: `AMPLIFY`; slots: `JEWEL`; Редкость: `MAGIC` |


## POWER_FINERY_TRAVEL <a href="#power_finery_travel" id="power_finery_travel"></a>

### Бывалый <a href="#бывалый" id="бывалый"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% к опыту с врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бывалый](../Stats/Stats-POWER.md#power_finery_travel) |
| world | gain: `EXPERIENCE` |


## POWER_FURY_UNRESTED <a href="#power_fury_unrested" id="power_fury_unrested"></a>

### Неупокоенная ярость <a href="#неупокоенная-ярость" id="неупокоенная-ярость"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда питомец убивает врага, он получает {v}% к урону на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Неупокоенная ярость](../Stats/Stats-POWER.md#power_fury_unrested) |
| Событие · `on` | `PET_KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `INCREASED`; Длительность: 4; to: `PET` |


## POWER_FURY_RETURN <a href="#power_fury_return" id="power_fury_return"></a>

### Возврат жнеца <a href="#возврат-жнеца" id="возврат-жнеца"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда питомец погибает, вы получаете {v}% к урону на 6 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Возврат жнеца](../Stats/Stats-POWER.md#power_fury_return) |
| Событие · `on` | `PET_DEATH` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `INCREASED`; Длительность: 6 |


## POWER_FLARE_UP <a href="#power_flare_up" id="power_flare_up"></a>

### Вспышка <a href="#вспышка" id="вспышка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Шок врага даёт {v}% усиления шока на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вспышка](../Stats/Stats-POWER.md#power_flare_up) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `SHOCK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Эффект шока](../Stats/Stats-HERO.md#stock_shock_effect); Длительность: 4 |


## POWER_FURNACE_CORE <a href="#power_furnace_core" id="power_furnace_core"></a>

### Сердце горна <a href="#сердце-горна" id="сердце-горна"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Поджог врага даёт {v}% физ. урона как доп. огонь на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сердце горна](../Stats/Stats-POWER.md#power_furnace_core) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Физический как доп. огонь](../Stats/Stats-HERO.md#stock_physical_as_extra_fire); Длительность: 4 |


## POWER_FALLING_SKY <a href="#power_falling_sky" id="power_falling_sky"></a>

### Падающее небо <a href="#падающее-небо" id="падающее-небо"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 7 с бьёт случайного врага на {v}% урона оружия молнией |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Падающее небо](../Stats/Stats-POWER.md#power_falling_sky) |
| Событие · `on` | `EVERY` |
| every | 7 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `LIGHTNING`; to: `RANDOM` |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
