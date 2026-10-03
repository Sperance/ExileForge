# Особые силы · I

## POWER_IRON_STANCE <a href="#power_iron_stance" id="power_iron_stance"></a>

### Железная стойка <a href="#железная-стойка" id="железная-стойка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения брони за каждую секунду боя, до 10 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Железная стойка](../Stats/Stats-POWER.md#power_iron_stance) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Броня](../Stats/Stats-HERO.md#stock_armor); Операция: `INCREASED`; scale: `SECONDS`; cap: 10 |


## POWER_IRON_OATH <a href="#power_iron_oath" id="power_iron_oath"></a>

### Железная клятва <a href="#железная-клятва" id="железная-клятва"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Физический удар по вам даёт барьер в {v}% максимума здоровья на 3 с, не чаще раза в 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Железная клятва](../Stats/Stats-POWER.md#power_iron_oath) |
| Событие · `on` | `HIT_TAKEN` |
| checks | check: `DAMAGE_TYPE`; word: `PHYSICAL` |
| Перезарядка, с · `cooldown` | 3 |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_IRON_FOOTING <a href="#power_iron_footing" id="power_iron_footing"></a>

### Железная поступь <a href="#железная-поступь" id="железная-поступь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | В начале каждого боя даёт барьер в {v}% максимума здоровья на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Железная поступь](../Stats/Stats-POWER.md#power_iron_footing) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 4 |


## POWER_INK_CLOUD <a href="#power_ink_cloud" id="power_ink_cloud"></a>

### Чернильное облако <a href="#чернильное-облако" id="чернильное-облако"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство врага даёт заслон в {v}% максимума здоровья на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Чернильное облако](../Stats/Stats-POWER.md#power_ink_cloud) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_INDIGO_MIND <a href="#power_indigo_mind" id="power_indigo_mind"></a>

### Индиговый разум <a href="#индиговый-разум" id="индиговый-разум"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к урону заклинаний за каждые 10 маны (до 150%) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Индиговый разум](../Stats/Stats-POWER.md#power_indigo_mind) |
| sheet | Операция: `PER`; Предшествующие зоны: [Мана](../Stats/Stats-HERO.md#stock_mana); to: [Урон чар](../Stats/Stats-HERO.md#stock_spell_damage); per: 10; cap: 150 |


## POWER_INDIGO_SURGE <a href="#power_indigo_surge" id="power_indigo_surge"></a>

### Шёпот прилива <a href="#шёпот-прилива" id="шёпот-прилива"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Умение при мане выше 80% даёт {v}% к скорости каста на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Шёпот прилива](../Stats/Stats-POWER.md#power_indigo_surge) |
| Событие · `on` | `SKILL_USE` |
| checks | check: `MANA_ABOVE`; Значение: 80 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость применения](../Stats/Stats-HERO.md#stock_cast_speed); Операция: `INCREASED`; Длительность: 3 |


## POWER_INNER_TIDE_FLOW <a href="#power_inner_tide_flow" id="power_inner_tide_flow"></a>

### Внутренний прилив <a href="#внутренний-прилив" id="внутренний-прилив"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% максимума здоровья добавляется к ЭЩ |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Внутренний прилив](../Stats/Stats-POWER.md#power_inner_tide_flow) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Здоровье](../Stats/Stats-HERO.md#stock_health); to: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield) |


## POWER_INNER_TIDE_EBB <a href="#power_inner_tide_ebb" id="power_inner_tide_ebb"></a>

### Отлив <a href="#отлив" id="отлив"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда ЭЩ разрушен, восстанавливаете {v}% максимума здоровья (откат 15 с) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отлив](../Stats/Stats-POWER.md#power_inner_tide_ebb) |
| Событие · `on` | `SHIELD_BROKEN` |
| Перезарядка, с · `cooldown` | 15 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_IRON_BLOOD <a href="#power_iron_blood" id="power_iron_blood"></a>

### Железная кровь <a href="#железная-кровь" id="железная-кровь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% возврата здоровья за каждые 1000 брони, до 25% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Железная кровь](../Stats/Stats-POWER.md#power_iron_blood) |
| sheet | Операция: `PER`; Предшествующие зоны: [Броня](../Stats/Stats-HERO.md#stock_armor); to: [Возврат здоровья](../Stats/Stats-HERO.md#stock_life_recoup); per: 1000; cap: 25 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
