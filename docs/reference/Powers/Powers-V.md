# Особые силы · V

## POWER_VIRTUOSO_TEMPO <a href="#power_virtuoso_tempo" id="power_virtuoso_tempo"></a>

### Темп виртуоза <a href="#темп-виртуоза" id="темп-виртуоза"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критические удары дают +{v}% к множителю крита на 4 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Темп виртуоза](../Stats/Stats-POWER.md#power_virtuoso_tempo) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Множитель критического удара](../Stats/Stats-HERO.md#stock_critical_multiplier); Длительность: 4; stacks: 5 |


## POWER_VENTORS_WAGER <a href="#power_ventors_wager" id="power_ventors_wager"></a>

### Ставка Вентора <a href="#ставка-вентора" id="ставка-вентора"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения количества предметов с уникальных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ставка Вентора](../Stats/Stats-POWER.md#power_ventors_wager) |
| world | gain: `QUANTITY`; against: `UNIQUE` |


## POWER_VOID_HUNGER <a href="#power_void_hunger" id="power_void_hunger"></a>

### Голод пустоты <a href="#голод-пустоты" id="голод-пустоты"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по проклятым врагам возвращают здоровьем {v}% нанесённого урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Голод пустоты](../Stats/Stats-POWER.md#power_void_hunger) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_CURSED` |
| Эффекты · `effects` | Действие: `HEAL`; of: `DEALT` |


## POWER_VOID_SIGHT <a href="#power_void_sight" id="power_void_sight"></a>

### Взор пустоты <a href="#взор-пустоты" id="взор-пустоты"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса уникального предмета с боссов и стражей |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Взор пустоты](../Stats/Stats-POWER.md#power_void_sight) |
| world | gain: `UNIQUE` |


## POWER_VOID_ECHO <a href="#power_void_echo" id="power_void_echo"></a>

### Эхо пустоты <a href="#эхо-пустоты" id="эхо-пустоты"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Применение умения с шансом {v}% обновляет все перезарядки |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Эхо пустоты](../Stats/Stats-POWER.md#power_void_echo) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `SKILL_USE` |
| Эффекты · `effects` | Действие: `COOLDOWNS`; Количество: 999 |


## POWER_VOID_LASH <a href="#power_void_lash" id="power_void_lash"></a>

### Плеть пустоты <a href="#плеть-пустоты" id="плеть-пустоты"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый третий удар хлещет пустотой: ещё {v}% урона оружия хаосом, пятая часть его возвращается здоровьем |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Плеть пустоты](../Stats/Stats-POWER.md#power_void_lash) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 3 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `CHAOS`; leech: 20 |


## POWER_VOID_TITHE <a href="#power_void_tithe" id="power_void_tithe"></a>

### Десятина пустоты <a href="#десятина-пустоты" id="десятина-пустоты"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% повышения редкости предметов с редких и уникальных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Десятина пустоты](../Stats/Stats-POWER.md#power_void_tithe) |
| world | gain: `RARITY`; against: `RARE`; `UNIQUE` |


## POWER_VOID_PULSE <a href="#power_void_pulse" id="power_void_pulse"></a>

### Пульс пустоты <a href="#пульс-пустоты" id="пульс-пустоты"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство проклятого врага с шансом {v}% проклинает всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пульс пустоты](../Stats/Stats-POWER.md#power_void_pulse) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_CURSED` |
| Эффекты · `effects` | Действие: `CURSE`; to: `ALL` |


## POWER_VAAL_OFFERING <a href="#power_vaal_offering" id="power_vaal_offering"></a>

### Подношение Ваал <a href="#подношение-ваал" id="подношение-ваал"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт {v}% увеличения урона хаосом на 4 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Подношение Ваал](../Stats/Stats-POWER.md#power_vaal_offering) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон хаосом](../Stats/Stats-HERO.md#stock_attack_chaos); Операция: `INCREASED`; Длительность: 4; stacks: 5 |


## POWER_VEILED_GAZE <a href="#power_veiled_gaze" id="power_veiled_gaze"></a>

### Взгляд из-под маски <a href="#взгляд-из-под-маски" id="взгляд-из-под-маски"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Взгляд из-под маски](../Stats/Stats-POWER.md#power_veiled_gaze) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Сопротивление хаосу](../Stats/Stats-HERO.md#stock_resist_chaos); Длительность: 4 |


## POWER_VAAL_BLOODLINE <a href="#power_vaal_bloodline" id="power_vaal_bloodline"></a>

### Кровь Ваал <a href="#кровь-ваал" id="кровь-ваал"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% вампиризма здоровья за каждые 10% недостающего здоровья, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кровь Ваал](../Stats/Stats-POWER.md#power_vaal_bloodline) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Общий вампиризм](../Stats/Stats-HERO.md#stock_leech_all); scale: `MISSING_LIFE`; cap: 5 |


## POWER_VENOM_CHILL <a href="#power_venom_chill" id="power_venom_chill"></a>

### Холод яда <a href="#холод-яда" id="холод-яда"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия {v}% увеличения урона холодом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Холод яда](../Stats/Stats-POWER.md#power_venom_chill) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон холодом](../Stats/Stats-HERO.md#stock_attack_cold); Операция: `INCREASED` |


## POWER_VENT_GUST <a href="#power_vent_gust" id="power_vent_gust"></a>

### Жерловый порыв <a href="#жерловый-порыв" id="жерловый-порыв"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 5 с наносит всем врагам {v}% максимума здоровья огнём |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жерловый порыв](../Stats/Stats-POWER.md#power_vent_gust) |
| Событие · `on` | `EVERY` |
| every | 5 |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `LIFE`; Тип: `FIRE`; to: `ALL` |


## POWER_VAAL_SKIN <a href="#power_vaal_skin" id="power_vaal_skin"></a>

### Ваальская кожа <a href="#ваальская-кожа" id="ваальская-кожа"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удар хаосом по вам даёт заслон в {v}% максимума здоровья на 3 с, не чаще раза в 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ваальская кожа](../Stats/Stats-POWER.md#power_vaal_skin) |
| Событие · `on` | `HIT_TAKEN` |
| checks | check: `DAMAGE_TYPE`; word: `CHAOS` |
| Перезарядка, с · `cooldown` | 3 |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_VESPERS <a href="#power_vespers" id="power_vespers"></a>

### Вечерня <a href="#вечерня" id="вечерня"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | За каждый выигранный бой перезарядка ваших умений короче на {v} с в начале следующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вечерня](../Stats/Stats-POWER.md#power_vespers) |
| Событие · `on` | `FIGHT_CLEAR` |
| Эффекты · `effects` | Действие: `COOLDOWNS` |


## POWER_VOID_RESONANCE <a href="#power_void_resonance" id="power_void_resonance"></a>

### Резонанс пустоты <a href="#резонанс-пустоты" id="резонанс-пустоты"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% к урону заклинаний за каждый заряд силы (до 4) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Резонанс пустоты](../Stats/Stats-POWER.md#power_void_resonance) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон чар](../Stats/Stats-HERO.md#stock_spell_damage); Операция: `INCREASED`; scale: `POWER_CHARGES`; cap: 4 |


## POWER_VENT_ERUPTION <a href="#power_vent_eruption" id="power_vent_eruption"></a>

### Извержение <a href="#извержение" id="извержение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс при ударе вызвать выброс лавы: 100% урона оружия огнём по всем врагам (откат 0,5 с) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Извержение](../Stats/Stats-POWER.md#power_vent_eruption) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Перезарядка, с · `cooldown` | 0.5 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `FIRE`; to: `ALL`; Количество: 100 |


## POWER_VENT_MAW <a href="#power_vent_maw" id="power_vent_maw"></a>

### Серное жерло <a href="#серное-жерло" id="серное-жерло"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% физического урона атак превращается в огонь |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Серное жерло](../Stats/Stats-POWER.md#power_vent_maw) |
| sheet | Операция: `CONVERT`; Предшествующие зоны: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); to: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire) |


## POWER_VISAGE_ABYSS_GAZE <a href="#power_visage_abyss_gaze" id="power_visage_abyss_gaze"></a>

### Взгляд Бездны <a href="#взгляд-бездны" id="взгляд-бездны"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство: все враги 3 с получают на {v}% больше урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Взгляд Бездны](../Stats/Stats-POWER.md#power_visage_abyss_gaze) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken); Длительность: 3; to: `ALL` |


## POWER_VISAGE_DEPTHS <a href="#power_visage_depths" id="power_visage_depths"></a>

### Открытые глубины <a href="#открытые-глубины" id="открытые-глубины"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к крит. множителю, пока здоровья меньше 50% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Открытые глубины](../Stats/Stats-POWER.md#power_visage_depths) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_BELOW`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Множитель критического удара](../Stats/Stats-HERO.md#stock_critical_multiplier) |


## POWER_VAAL_DEBT <a href="#power_vaal_debt" id="power_vaal_debt"></a>

### Долг Ваал <a href="#долг-ваал" id="долг-ваал"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Низкое здоровье восстанавливает {v}% здоровья, раз за бой |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Долг Ваал](../Stats/Stats-POWER.md#power_vaal_debt) |
| Событие · `on` | `LOW_LIFE` |
| Перезарядка, с · `cooldown` | -1 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_VOID_TRADE <a href="#power_void_trade" id="power_void_trade"></a>

### Сделка с пустотой <a href="#сделка-с-пустотой" id="сделка-с-пустотой"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийства дают барьер в 10% здоровья на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сделка с пустотой](../Stats/Stats-POWER.md#power_void_trade) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `KILL` |
| Перезарядка, с · `cooldown` | 3 |
| Эффекты · `effects` | Действие: `BARRIER`; Количество: 10 |


## POWER_VENOM_HEART <a href="#power_venom_heart" id="power_venom_heart"></a>

### Ядовитое сердце <a href="#ядовитое-сердце" id="ядовитое-сердце"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% физического урона атак обращается в хаос |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ядовитое сердце](../Stats/Stats-POWER.md#power_venom_heart) |
| sheet | Операция: `CONVERT`; Предшествующие зоны: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); to: [Урон хаосом](../Stats/Stats-HERO.md#stock_attack_chaos) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
