# Особые силы · K

## POWER_KAOMS_BLAZE <a href="#power_kaoms_blaze" id="power_kaoms_blaze"></a>

### Пламя Каома <a href="#пламя-каома" id="пламя-каома"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство наносит всем врагам урон огнём, равный {v}% вашего максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пламя Каома](../Stats/Stats-POWER.md#power_kaoms_blaze) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `LIFE`; Тип: `FIRE`; to: `ALL` |


## POWER_KAOMS_RAGE <a href="#power_kaoms_rage" id="power_kaoms_rage"></a>

### Ярость Каома <a href="#ярость-каома" id="ярость-каома"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар с шансом {v}% даёт всем флягам 3 заряда |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ярость Каома](../Stats/Stats-POWER.md#power_kaoms_rage) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `CHARGES`; Количество: 3 |


## POWER_KARUI_GRIT <a href="#power_karui_grit" id="power_karui_grit"></a>

### Стойкость каруи <a href="#стойкость-каруи" id="стойкость-каруи"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения брони, пока здоровья меньше 50% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Стойкость каруи](../Stats/Stats-POWER.md#power_karui_grit) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_BELOW`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Броня](../Stats/Stats-HERO.md#stock_armor); Операция: `INCREASED` |


## POWER_KAOMS_ENDURANCE <a href="#power_kaoms_endurance" id="power_kaoms_endurance"></a>

### Стойкость Каома <a href="#стойкость-каома" id="стойкость-каома"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар даёт +{v}% к снижению физического урона на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Стойкость Каома](../Stats/Stats-POWER.md#power_kaoms_endurance) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Снижение физического урона](../Stats/Stats-HERO.md#stock_physical_reduction); Длительность: 4; stacks: 3 |


## POWER_KEEN_TIPS <a href="#power_keen_tips" id="power_keen_tips"></a>

### Острые наконечники <a href="#острые-наконечники" id="острые-наконечники"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса крита против врагов, у которых больше 90% здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Острые наконечники](../Stats/Stats-POWER.md#power_keen_tips) |
| Событие · `on` | `STANDING` |
| checks | check: `TARGET_LIFE_ABOVE`; Значение: 90 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED` |


## POWER_KEPT_PROMISE <a href="#power_kept_promise" id="power_kept_promise"></a>

### Сдержанное слово <a href="#сдержанное-слово" id="сдержанное-слово"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сдержанное слово](../Stats/Stats-POWER.md#power_kept_promise) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_KINDLING <a href="#power_kindling" id="power_kindling"></a>

### Растопка <a href="#растопка" id="растопка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Поджог врага даёт +{v}% урона на 3 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Растопка](../Stats/Stats-POWER.md#power_kindling) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 3; stacks: 3 |


## POWER_KINDLED_PACE <a href="#power_kindled_pace" id="power_kindled_pace"></a>

### Разожжённый шаг <a href="#разожжённый-шаг" id="разожжённый-шаг"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство горящего врага даёт {v}% увеличения скорости атаки на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Разожжённый шаг](../Stats/Stats-POWER.md#power_kindled_pace) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3 |


## POWER_KEEPER_OARS <a href="#power_keeper_oars" id="power_keeper_oars"></a>

### Ритм гребца <a href="#ритм-гребца" id="ритм-гребца"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к скорости атаки за каждые 25 Интеллекта |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ритм гребца](../Stats/Stats-POWER.md#power_keeper_oars) |
| sheet | Операция: `MORE`; Предшествующие зоны: [Интеллект](../Stats/Stats-HERO.md#stock_intellect); to: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); per: 25 |


## POWER_KEEPER_LOGBOOK <a href="#power_keeper_logbook" id="power_keeper_logbook"></a>

### Вахтенный журнал <a href="#вахтенный-журнал" id="вахтенный-журнал"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к урону заклинаний за каждые 10 Ловкости |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вахтенный журнал](../Stats/Stats-POWER.md#power_keeper_logbook) |
| sheet | Операция: `PER`; Предшествующие зоны: [Ловкость](../Stats/Stats-HERO.md#stock_agility); to: [Урон чар](../Stats/Stats-HERO.md#stock_spell_damage); per: 10 |


## POWER_KILLING_FEVER <a href="#power_killing_fever" id="power_killing_fever"></a>

### Горячка убийства <a href="#горячка-убийства" id="горячка-убийства"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс получить заряд ярости при убийстве |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Горячка убийства](../Stats/Stats-POWER.md#power_killing_fever) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `CHARGE`; charge: `FRENZY`; Количество: 1 |


## POWER_KINGDOM_HUNGER <a href="#power_kingdom_hunger" id="power_kingdom_hunger"></a>

### Голод по королевствам <a href="#голод-по-королевствам" id="голод-по-королевствам"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийства дают {v}% больше урона на 5 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Голод по королевствам](../Stats/Stats-POWER.md#power_kingdom_hunger) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `MORE`; Длительность: 5; stacks: 5 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
