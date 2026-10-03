# Особые силы · A

## POWER_AMBUSH <a href="#power_ambush" id="power_ambush"></a>

### Засада <a href="#засада" id="засада"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары в первые 3 с боя бьют ещё раз на {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Засада](../Stats/Stats-POWER.md#power_ambush) |
| Событие · `on` | `HIT` |
| checks | check: `FIGHT_BEFORE`; Значение: 3 |
| Эффекты · `effects` | Действие: `DAMAGE` |


## POWER_AFTERSHOCK <a href="#power_aftershock" id="power_aftershock"></a>

### Повторный толчок <a href="#повторный-толчок" id="повторный-толчок"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Оглушение врага бьёт остальных врагов на {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Повторный толчок](../Stats/Stats-POWER.md#power_aftershock) |
| Событие · `on` | `STUN` |
| Эффекты · `effects` | Действие: `DAMAGE`; to: `OTHERS` |


## POWER_ASH_BURST <a href="#power_ash_burst" id="power_ash_burst"></a>

### Всполох пепла <a href="#всполох-пепла" id="всполох-пепла"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Падение здоровья до низкого наносит всем врагам урон огнём в {v}% максимума здоровья, не чаще раза в 10 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Всполох пепла](../Stats/Stats-POWER.md#power_ash_burst) |
| Событие · `on` | `LOW_LIFE` |
| Перезарядка, с · `cooldown` | 10 |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `LIFE`; Тип: `FIRE`; to: `ALL` |


## POWER_ASHEN_END <a href="#power_ashen_end" id="power_ashen_end"></a>

### Пепельный конец <a href="#пепельный-конец" id="пепельный-конец"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары добивают горящих врагов, у которых меньше {v}% здоровья, кроме уникальных |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пепельный конец](../Stats/Stats-POWER.md#power_ashen_end) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `EXECUTE` |


## POWER_ABYSS_DODGE <a href="#power_abyss_dodge" id="power_abyss_dodge"></a>

### Уход в бездну <a href="#уход-в-бездну" id="уход-в-бездну"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение с шансом {v}% проклинает нападавшего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Уход в бездну](../Stats/Stats-POWER.md#power_abyss_dodge) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `CURSE` |


## POWER_ALL_SEEING <a href="#power_all_seeing" id="power_all_seeing"></a>

### Всевидение <a href="#всевидение" id="всевидение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к шансу крита за каждые 10 интеллекта |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Всевидение](../Stats/Stats-POWER.md#power_all_seeing) |
| sheet | Операция: `PER`; Предшествующие зоны: [Интеллект](../Stats/Stats-HERO.md#stock_intellect); to: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); per: 10 |


## POWER_ACUITY_DRAIN <a href="#power_acuity_drain" id="power_acuity_drain"></a>

### Проницательный глоток <a href="#проницательный-глоток" id="проницательный-глоток"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критические удары возвращают здоровьем {v}% нанесённого урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Проницательный глоток](../Stats/Stats-POWER.md#power_acuity_drain) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `HEAL`; of: `DEALT` |


## POWER_ASTRAL_HARMONY <a href="#power_astral_harmony" id="power_astral_harmony"></a>

### Звёздная гармония <a href="#звёздная-гармония" id="звёздная-гармония"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона за каждые 100 силы |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Звёздная гармония](../Stats/Stats-POWER.md#power_astral_harmony) |
| sheet | Операция: `PER`; Предшествующие зоны: [Сила](../Stats/Stats-HERO.md#stock_strength); to: [Урон](../Stats/Stats-HERO.md#stock_damage); per: 100 |


## POWER_ALPHA_HOWL <a href="#power_alpha_howl" id="power_alpha_howl"></a>

### Вой вожака <a href="#вой-вожака" id="вой-вожака"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вой вожака](../Stats/Stats-POWER.md#power_alpha_howl) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 4; to: `ALL` |


## POWER_ANVIL_HEART <a href="#power_anvil_heart" id="power_anvil_heart"></a>

### Сердце наковальни <a href="#сердце-наковальни" id="сердце-наковальни"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сердце наковальни](../Stats/Stats-POWER.md#power_anvil_heart) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_ABOVE`; Значение: 90 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken) |


## POWER_ABYSSAL_GAZE <a href="#power_abyssal_gaze" id="power_abyssal_gaze"></a>

### Взгляд бездны <a href="#взгляд-бездны" id="взгляд-бездны"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона за каждого проклятого врага, до 5 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Взгляд бездны](../Stats/Stats-POWER.md#power_abyssal_gaze) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); scale: `CURSED_FOES`; cap: 5 |


## POWER_ABYSS_CALL <a href="#power_abyss_call" id="power_abyss_call"></a>

### Зов бездны <a href="#зов-бездны" id="зов-бездны"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | С шансом {v}% в начале боя проклинает всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Зов бездны](../Stats/Stats-POWER.md#power_abyss_call) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `CURSE`; to: `ALL` |


## POWER_ATZIRIS_VOW <a href="#power_atziris_vow" id="power_atziris_vow"></a>

### Обет Атзири <a href="#обет-атзири" id="обет-атзири"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия удары наносят ещё {v}% своего урона хаосом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Обет Атзири](../Stats/Stats-POWER.md#power_atziris_vow) |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `DEALT`; Тип: `CHAOS` |


## POWER_ABYSSAL_TIDE <a href="#power_abyssal_tide" id="power_abyssal_tide"></a>

### Прилив бездны <a href="#прилив-бездны" id="прилив-бездны"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Заклинание выпускает волну: урон хаосом всем врагам в {v}% интеллекта |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Прилив бездны](../Stats/Stats-POWER.md#power_abyssal_tide) |
| Событие · `on` | `SKILL_USE` |
| checks | check: `SPELL` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `INTELLECT`; Тип: `CHAOS`; to: `ALL` |


## POWER_ABYSSAL_HEART <a href="#power_abyssal_heart" id="power_abyssal_heart"></a>

### Сердце бездны <a href="#сердце-бездны" id="сердце-бездны"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к максимуму здоровья за каждые 10 силы |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сердце бездны](../Stats/Stats-POWER.md#power_abyssal_heart) |
| sheet | Операция: `PER`; Предшествующие зоны: [Сила](../Stats/Stats-HERO.md#stock_strength); to: [Здоровье](../Stats/Stats-HERO.md#stock_health); per: 10 |


## POWER_ABYSS_WILL <a href="#power_abyss_will" id="power_abyss_will"></a>

### Воля бездны <a href="#воля-бездны" id="воля-бездны"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к максимуму маны за каждые 10 интеллекта |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Воля бездны](../Stats/Stats-POWER.md#power_abyss_will) |
| sheet | Операция: `PER`; Предшествующие зоны: [Интеллект](../Stats/Stats-HERO.md#stock_intellect); to: [Мана](../Stats/Stats-HERO.md#stock_mana); per: 10 |


## POWER_ABYSSAL_HUNGER <a href="#power_abyssal_hunger" id="power_abyssal_hunger"></a>

### Голод Бездны <a href="#голод-бездны" id="голод-бездны"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона за каждое убийство в этом бою, до 10 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Голод Бездны](../Stats/Stats-POWER.md#power_abyssal_hunger) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); scale: `KILLS`; cap: 10 |


## POWER_ANVIL_RING <a href="#power_anvil_ring" id="power_anvil_ring"></a>

### Звон наковальни <a href="#звон-наковальни" id="звон-наковальни"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При получении удара бьёт в ответ физическим уроном в {v}% вашей брони, не чаще раза в 2 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Звон наковальни](../Stats/Stats-POWER.md#power_anvil_ring) |
| Событие · `on` | `HIT_TAKEN` |
| Перезарядка, с · `cooldown` | 2 |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `ARMOUR`; Тип: `PHYSICAL` |


## POWER_AVALANCHE <a href="#power_avalanche" id="power_avalanche"></a>

### Лавина <a href="#лавина" id="лавина"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 5-й удар обрушивает на всех врагов камнепад на {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Лавина](../Stats/Stats-POWER.md#power_avalanche) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 5 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `PHYSICAL`; to: `ALL` |


## POWER_ASHEN_UPDRAFT <a href="#power_ashen_updraft" id="power_ashen_updraft"></a>

### Пепельный вихрь <a href="#пепельный-вихрь" id="пепельный-вихрь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения урона огнём в первые 5 с боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пепельный вихрь](../Stats/Stats-POWER.md#power_ashen_updraft) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire); Операция: `INCREASED`; Длительность: 5 |


## POWER_ALTAR_BLOOD <a href="#power_altar_blood" id="power_altar_blood"></a>

### Кровь алтаря <a href="#кровь-алтаря" id="кровь-алтаря"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары хаосом по вам возвращают здоровьем {v}% своего урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кровь алтаря](../Stats/Stats-POWER.md#power_altar_blood) |
| Событие · `on` | `HIT_TAKEN` |
| checks | check: `DAMAGE_TYPE`; word: `CHAOS` |
| Эффекты · `effects` | Действие: `HEAL`; of: `TAKEN` |


## POWER_ATLAS_DOMINION <a href="#power_atlas_dominion" id="power_atlas_dominion"></a>

### Власть над Атласом <a href="#власть-над-атласом" id="власть-над-атласом"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения количества предметов с монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Власть над Атласом](../Stats/Stats-POWER.md#power_atlas_dominion) |
| world | gain: `QUANTITY` |


## POWER_ANCHOR_DRAG <a href="#power_anchor_drag" id="power_anchor_drag"></a>

### Тяга якоря <a href="#тяга-якоря" id="тяга-якоря"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 3-й удар отзывается эхом через 1 с на {v}% своего урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тяга якоря](../Stats/Stats-POWER.md#power_anchor_drag) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 3 |
| Эффекты · `effects` | Действие: [Отзвук](../../Skills/Skills-SCION.md#echo); Длительность: 1 |


## POWER_ANCESTRAL_WRATH <a href="#power_ancestral_wrath" id="power_ancestral_wrath"></a>

### Гнев предков <a href="#гнев-предков" id="гнев-предков"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство врага даёт {v}% увеличения физического урона на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Гнев предков](../Stats/Stats-POWER.md#power_ancestral_wrath) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); Операция: `INCREASED`; Длительность: 4; stacks: 3 |


## POWER_ANVIL_PEAL <a href="#power_anvil_peal" id="power_anvil_peal"></a>

### Звон наковальни <a href="#звон-наковальни-1" id="звон-наковальни-1"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 4-й удар с шансом {v}% оглушает врага на 0,8 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Звон наковальни](../Stats/Stats-POWER.md#power_anvil_peal) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 4 |
| Эффекты · `effects` | Действие: `STUN`; Длительность: 0.8 |


## POWER_ANSWERING_DARK <a href="#power_answering_dark" id="power_answering_dark"></a>

### Отвечающая тьма <a href="#отвечающая-тьма" id="отвечающая-тьма"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Против 3 и более врагов удары отзываются через 1 с на {v}% урона хаосом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отвечающая тьма](../Stats/Stats-POWER.md#power_answering_dark) |
| Событие · `on` | `HIT` |
| checks | check: `FOES_AT_LEAST`; Значение: 3 |
| Эффекты · `effects` | Действие: [Отзвук](../../Skills/Skills-SCION.md#echo); Длительность: 1; Тип: `CHAOS` |


## POWER_ALTAR_WRATH <a href="#power_altar_wrath" id="power_altar_wrath"></a>

### Гнев алтаря <a href="#гнев-алтаря" id="гнев-алтаря"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт {v}% к урону на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Гнев алтаря](../Stats/Stats-POWER.md#power_altar_wrath) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `INCREASED`; Длительность: 4 |


## POWER_ALTAR_VAAL_SIGHT <a href="#power_altar_vaal_sight" id="power_altar_vaal_sight"></a>

### Взгляд Ваал <a href="#взгляд-ваал" id="взгляд-ваал"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к урону за каждый надетый осквернённый предмет |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Взгляд Ваал](../Stats/Stats-POWER.md#power_altar_vaal_sight) |
| sheet | Операция: `PER`; Предшествующие зоны: [Надетые осквернённые предметы](../Stats/Stats-HERO.md#stock_worn_corrupted); to: [Урон](../Stats/Stats-HERO.md#stock_damage) |


## POWER_ANVIL_SPARK <a href="#power_anvil_spark" id="power_anvil_spark"></a>

### Бесцветное пламя <a href="#бесцветное-пламя" id="бесцветное-пламя"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый удар наносит весь урон одной случайной стихией: огнём, холодом или молнией |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бесцветное пламя](../Stats/Stats-POWER.md#power_anvil_spark) |
| sheet | Операция: `ADD`; to: [Удары случайной стихии](../Stats/Stats-HERO.md#stock_random_element_hits) |


## POWER_ANVIL_FORGE <a href="#power_anvil_forge" id="power_anvil_forge"></a>

### Горн Бездны <a href="#горн-бездны" id="горн-бездны"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% физического урона атак добавляется уроном стихии удара |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Горн Бездны](../Stats/Stats-POWER.md#power_anvil_forge) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); to: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire) |


## POWER_ABBOT_BLINDNESS <a href="#power_abbot_blindness" id="power_abbot_blindness"></a>

### Ослепляющий взор <a href="#ослепляющий-взор" id="ослепляющий-взор"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда вас бьют, {v}% шанс ослепить атакующего: −20% к скорости атаки на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ослепляющий взор](../Stats/Stats-POWER.md#power_abbot_blindness) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: -20% увеличение скорости атаки · [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Длительность: 3 |


## POWER_ALTAR_SCORN <a href="#power_altar_scorn" id="power_altar_scorn"></a>

### Презрение алтаря <a href="#презрение-алтаря" id="презрение-алтаря"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Презрение алтаря](../Stats/Stats-POWER.md#power_altar_scorn) |
| sheet | Операция: `ADD`; Предшествующие зоны: [Надетые осквернённые предметы](../Stats/Stats-HERO.md#stock_worn_corrupted); to: [Урон](../Stats/Stats-HERO.md#stock_damage); under: 1 |


## POWER_AVARICE_HOARD <a href="#power_avarice_hoard" id="power_avarice_hoard"></a>

### Капитанский сундук <a href="#капитанский-сундук" id="капитанский-сундук"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% к редкости предметов с редких и уникальных врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Капитанский сундук](../Stats/Stats-POWER.md#power_avarice_hoard) |
| world | gain: `RARITY`; against: `RARE`; `UNIQUE` |


## POWER_AVARICE_GREED <a href="#power_avarice_greed" id="power_avarice_greed"></a>

### Жадность <a href="#жадность" id="жадность"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Враги роняют на {v}% больше золота |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жадность](../Stats/Stats-POWER.md#power_avarice_greed) |
| world | gain: `GOLD` |


## POWER_AURORA_WARD <a href="#power_aurora_ward" id="power_aurora_ward"></a>

### Покров авроры <a href="#покров-авроры" id="покров-авроры"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок восстанавливает {v}% максимума ЭЩ |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Покров авроры](../Stats/Stats-POWER.md#power_aurora_ward) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `HEAL`; of: `SHIELD` |


## POWER_AURORA_ECLIPSE <a href="#power_aurora_eclipse" id="power_aurora_eclipse"></a>

### Полное затмение <a href="#полное-затмение" id="полное-затмение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к шансу блока, пока ЭЩ полон |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Полное затмение](../Stats/Stats-POWER.md#power_aurora_eclipse) |
| Событие · `on` | `STANDING` |
| checks | check: `SHIELD_FULL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс блока](../Stats/Stats-HERO.md#stock_block_chance) |


## POWER_ARCANE_TIDE <a href="#power_arcane_tide" id="power_arcane_tide"></a>

### Волшебный прилив <a href="#волшебный-прилив" id="волшебный-прилив"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 6 с восстанавливает {v}% маны |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Волшебный прилив](../Stats/Stats-POWER.md#power_arcane_tide) |
| Событие · `on` | `EVERY` |
| every | 6 |
| Эффекты · `effects` | Действие: `HEAL`; of: `MANA` |


## POWER_ABYSS_GRIN <a href="#power_abyss_grin" id="power_abyss_grin"></a>

### Оскал бездны <a href="#оскал-бездны" id="оскал-бездны"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство с шансом {v}% проклинает всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Оскал бездны](../Stats/Stats-POWER.md#power_abyss_grin) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `CURSE`; to: `ALL` |


## POWER_ALPHA_BLOOD <a href="#power_alpha_blood" id="power_alpha_blood"></a>

### Кровь вожака <a href="#кровь-вожака" id="кровь-вожака"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство питомцем даёт ему {v}% увеличения скорости атаки на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кровь вожака](../Stats/Stats-POWER.md#power_alpha_blood) |
| Событие · `on` | `PET_KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 4; to: `PET` |


## POWER_ATLAS_BURDEN <a href="#power_atlas_burden" id="power_atlas_burden"></a>

### Ноша Атласа <a href="#ноша-атласа" id="ноша-атласа"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уникальных предметов находится на {v}% больше |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ноша Атласа](../Stats/Stats-POWER.md#power_atlas_burden) |
| world | gain: `UNIQUE` |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
