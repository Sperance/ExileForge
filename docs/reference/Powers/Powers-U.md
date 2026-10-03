# Особые силы · U

## POWER_UNDERTOW <a href="#power_undertow" id="power_undertow"></a>

### Отбойное течение <a href="#отбойное-течение" id="отбойное-течение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары холодом с шансом {v}% задерживают следующий удар врага на 0,3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отбойное течение](../Stats/Stats-POWER.md#power_undertow) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| checks | check: `DAMAGE_TYPE`; word: `COLD` |
| Эффекты · `effects` | Действие: `DELAY`; Длительность: 0.3 |


## POWER_UNYIELDING <a href="#power_unyielding" id="power_unyielding"></a>

### Непреклонность <a href="#непреклонность" id="непреклонность"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к снижению физического урона, пока здоровья меньше 50% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Непреклонность](../Stats/Stats-POWER.md#power_unyielding) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_BELOW`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Снижение физического урона](../Stats/Stats-HERO.md#stock_physical_reduction) |


## POWER_UMBRAL_BARRIER <a href="#power_umbral_barrier" id="power_umbral_barrier"></a>

### Теневой заслон <a href="#теневой-заслон" id="теневой-заслон"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Чары дают барьер в {v}% максимума здоровья на 3 с, не чаще раза в 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Теневой заслон](../Stats/Stats-POWER.md#power_umbral_barrier) |
| Событие · `on` | `SKILL_USE` |
| checks | check: `SPELL` |
| Перезарядка, с · `cooldown` | 4 |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_UNDYING_EMBERS <a href="#power_undying_embers" id="power_undying_embers"></a>

### Неугасимые угли <a href="#неугасимые-угли" id="неугасимые-угли"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Поджог врага даёт +{v}% к пробиванию огнём на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Неугасимые угли](../Stats/Stats-POWER.md#power_undying_embers) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Пробивание огня](../Stats/Stats-HERO.md#stock_penetrate_fire); Длительность: 4; stacks: 3 |


## POWER_UNWILLING_DEATH <a href="#power_unwilling_death" id="power_unwilling_death"></a>

### Непринятая смерть <a href="#непринятая-смерть" id="непринятая-смерть"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Раз за бой, когда вы должны погибнуть, - поднимаетесь с {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Непринятая смерть](../Stats/Stats-POWER.md#power_unwilling_death) |
| Событие · `on` | `DEATH` |
| Перезарядка, с · `cooldown` | -1 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_UPDRAFT_GUST <a href="#power_updraft_gust" id="power_updraft_gust"></a>

### Восходящий порыв <a href="#восходящий-порыв" id="восходящий-порыв"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 6 с даёт {v}% увеличения скорости атаки на 2 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Восходящий порыв](../Stats/Stats-POWER.md#power_updraft_gust) |
| Событие · `on` | `EVERY` |
| every | 6 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 2 |


## POWER_UNBROKEN_LINE <a href="#power_unbroken_line" id="power_unbroken_line"></a>

### Неразорванная нить <a href="#неразорванная-нить" id="неразорванная-нить"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона за каждый удар подряд по одному врагу, до 12 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Неразорванная нить](../Stats/Stats-POWER.md#power_unbroken_line) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); scale: `MOMENTUM`; cap: 12 |


## POWER_UMBRA <a href="#power_umbra" id="power_umbra"></a>

### Тень <a href="#тень" id="тень"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар по вам даёт неуязвимость на {v} с, не чаще раза в 10 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тень](../Stats/Stats-POWER.md#power_umbra) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `CRIT_TAKEN` |
| Перезарядка, с · `cooldown` | 10 |
| Эффекты · `effects` | Действие: `INVULNERABLE` |


## POWER_UNMAPPED_ARRIVAL <a href="#power_unmapped_arrival" id="power_unmapped_arrival"></a>

### Прибытие без карты <a href="#прибытие-без-карты" id="прибытие-без-карты"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Неуязвимость в первые {v} с боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Прибытие без карты](../Stats/Stats-POWER.md#power_unmapped_arrival) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `INVULNERABLE` |


## POWER_UMBILICUS_TIDE <a href="#power_umbilicus_tide" id="power_umbilicus_tide"></a>

### Питающий прилив <a href="#питающий-прилив" id="питающий-прилив"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Регенерация {v}% максимума здоровья в секунду, пока его меньше 50% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Питающий прилив](../Stats/Stats-POWER.md#power_umbilicus_tide) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_BELOW`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Регенерация здоровья, %](../Stats/Stats-HERO.md#stock_life_regen_percent) |


## POWER_UMBILICUS_DROUGHT <a href="#power_umbilicus_drought" id="power_umbilicus_drought"></a>

### Утонувшие фляги <a href="#утонувшие-фляги" id="утонувшие-фляги"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Ваши фляги не получают зарядов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Утонувшие фляги](../Stats/Stats-POWER.md#power_umbilicus_drought) |
| sheet | Операция: `SET`; to: [Заряды фляг](../Stats/Stats-HERO.md#stock_flask_charges_gained); Значение: -100 |


## POWER_UNDERTOW_PULL <a href="#power_undertow_pull" id="power_undertow_pull"></a>

### Тяга течения <a href="#тяга-течения" id="тяга-течения"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс получить заряд силы при крите |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тяга течения](../Stats/Stats-POWER.md#power_undertow_pull) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `CHARGE`; charge: `POWER`; Количество: 1 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
