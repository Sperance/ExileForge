# MARAUDER

| Название | Код |
| --- | --- |
| [Тяжёлый удар](#heavy_strike) | HEAVY_STRIKE |
| [Клич стойкости](#enduring_cry) | ENDURING_CRY |
| [Сотрясение земли](#ground_slam) | GROUND_SLAM |
| [Адский удар](#infernal_blow) | INFERNAL_BLOW |
| [Уязвимость](#vulnerability) | VULNERABILITY |
| [Кровавая ярость](#blood_rage) | BLOOD_RAGE |
| [Рёв стойкости](#enduring_roar) | ENDURING_ROAR |
| [Решимость](#determination) | DETERMINATION |
| [Железная кожа](#iron_skin) | IRON_SKIN |
| [Кровопролитие](#bloodletting) | BLOODLETTING |
| [Гнев](#anger) | ANGER |
| [Ярость берсерка](#berserker_fury) | BERSERKER_FURY |
| [Несокрушимость](#unbreakable) | UNBREAKABLE |
| [Крепкий лоб](#thick_skull) | THICK_SKULL |
| [Отмщение](#retaliation) | RETALIATION |


## HEAVY_STRIKE <a href="#heavy_strike" id="heavy_strike"></a>

### Тяжёлый удар <a href="#тяжёлый-удар" id="тяжёлый-удар"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 1 |
| icon | `physical` |
| Мана: уровень 1 → 20 · `mana` | 6; 20 |
| Перезарядка, с · `cooldown` | 3 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 139; 232; Оглушение: 20; 40 |


## ENDURING_CRY <a href="#enduring_cry" id="enduring_cry"></a>

### Клич стойкости <a href="#клич-стойкости" id="клич-стойкости"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `WARCRY` |
| Уровень открытия · `unlock` | 6 |
| icon | [armour](../reference/Tables/Tables-MODIFIER.md#armour) |
| Мана: уровень 1 → 20 · `mana` | 10; 30 |
| Перезарядка, с · `cooldown` | 12 |
| Условие применения · `condition` | `FIGHT_START` |
| Усиление · `buff` | Длительность: 6; Характеристики: 20–60% увеличение брони · [Броня](../reference/Stats/Stats-HERO.md#stock_armor); Восстанавливает 1–3% здоровья в секунду · [Регенерация здоровья, %](../reference/Stats/Stats-HERO.md#stock_life_regen_percent) |


## GROUND_SLAM <a href="#ground_slam" id="ground_slam"></a>

### Сотрясение земли <a href="#сотрясение-земли" id="сотрясение-земли"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 12 |
| icon | `earth` |
| Мана: уровень 1 → 20 · `mana` | 12; 34 |
| Перезарядка, с · `cooldown` | 6 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `ENEMIES_3` |
| Удар · `hit` | Число целей: 0; Урон оружия: 85; 147; Оглушение: 25; 25 |


## INFERNAL_BLOW <a href="#infernal_blow" id="infernal_blow"></a>

### Адский удар <a href="#адский-удар" id="адский-удар"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 20 |
| icon | `fire` |
| Мана: уровень 1 → 20 · `mana` | 14; 36 |
| Перезарядка, с · `cooldown` | 5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 116; 201; Тип урона: `FIRE`; Конверсия: 40; 80; Состояния: Состояние: `IGNITE`; Шанс (единица по правилам подсистемы): 30; 50 |


## VULNERABILITY <a href="#vulnerability" id="vulnerability"></a>

### Уязвимость <a href="#уязвимость" id="уязвимость"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `CURSE` |
| Уровень открытия · `unlock` | 30 |
| icon | `bleeding` |
| Мана: уровень 1 → 20 · `mana` | 18; 40 |
| Перезарядка, с · `cooldown` | 15 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 30; 10 |
| Условие применения · `condition` | `RARE_OR_BOSS` |
| Проклятие · `curse` | Длительность: 8; Число целей: 0; Характеристики: 15–35% увеличение получаемого физического урона · [Получаемый физический урон](../reference/Stats/Stats-HERO.md#stock_physical_taken); Шанс кровотечения по цели · ADD · 10–25 · [Шанс кровотечения по цели](../reference/Stats/Stats-HERO.md#stock_bleed_taken) |


## BLOOD_RAGE <a href="#blood_rage" id="blood_rage"></a>

### Кровавая ярость <a href="#кровавая-ярость" id="кровавая-ярость"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `GUARD` |
| Уровень открытия · `unlock` | 40 |
| icon | `leech` |
| Мана: уровень 1 → 20 · `mana` | 16; 34 |
| Перезарядка, с · `cooldown` | 20 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `LIFE_50` |
| Усиление · `buff` | Длительность: 8; Характеристики: 10–25% увеличение скорости атаки · [Скорость атаки](../reference/Stats/Stats-HERO.md#stock_attack_speed); 1–3% урона крадётся здоровьем · [Общий вампиризм](../reference/Stats/Stats-HERO.md#stock_leech_all) |


## ENDURING_ROAR <a href="#enduring_roar" id="enduring_roar"></a>

### Рёв стойкости <a href="#рёв-стойкости" id="рёв-стойкости"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `WARCRY` |
| Уровень открытия · `unlock` | 50 |
| icon | [armour](../reference/Tables/Tables-MODIFIER.md#armour) |
| Мана: уровень 1 → 20 · `mana` | 14; 34 |
| Перезарядка, с · `cooldown` | 10 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 60; 30 |
| Условие применения · `condition` | `FIGHT_START` |
| Усиление · `buff` | Длительность: 4; Характеристики: 2–6% дополнительного снижения физического урона · [Снижение физического урона](../reference/Stats/Stats-HERO.md#stock_physical_reduction) |
| Заряды · `charges` | Вид: `ENDURANCE`; gain: 1; 3 |


## DETERMINATION <a href="#determination" id="determination"></a>

### Решимость <a href="#решимость" id="решимость"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 1 |
| icon | [armour](../reference/Tables/Tables-MODIFIER.md#armour) |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | 20–50% увеличение брони · [Броня](../reference/Stats/Stats-HERO.md#stock_armor) |


## IRON_SKIN <a href="#iron_skin" id="iron_skin"></a>

### Железная кожа <a href="#железная-кожа" id="железная-кожа"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 5 |
| icon | `constitution` |
| Характеристики · `stats` | 4–12% увеличение максимума здоровья · [Здоровье](../reference/Stats/Stats-HERO.md#stock_health); 10–30% увеличение брони · [Броня](../reference/Stats/Stats-HERO.md#stock_armor) |


## BLOODLETTING <a href="#bloodletting" id="bloodletting"></a>

### Кровопролитие <a href="#кровопролитие" id="кровопролитие"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 10 |
| icon | `health` |
| Срабатывание · `trigger` | Событие: `KILL`; Лечение: life: 2; 6 |


## ANGER <a href="#anger" id="anger"></a>

### Гнев <a href="#гнев" id="гнев"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 16 |
| icon | `fire` |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | +6–140 к урону от огня · [Урон огнём](../reference/Stats/Stats-HERO.md#stock_attack_fire) |


## BERSERKER_FURY <a href="#berserker_fury" id="berserker_fury"></a>

### Ярость берсерка <a href="#ярость-берсерка" id="ярость-берсерка"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 24 |
| icon | `strength` |
| Характеристики · `stats` | 10–30% увеличение урона · [Урон](../reference/Stats/Stats-HERO.md#stock_damage) |
| lowLife | Да |


## UNBREAKABLE <a href="#unbreakable" id="unbreakable"></a>

### Несокрушимость <a href="#несокрушимость" id="несокрушимость"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 32 |
| icon | [shield](../reference/Tables/Tables-MODIFIER.md#shield) |
| Срабатывание · `trigger` | Событие: `LOW_LIFE`; Перезарядка, с: 30; Барьер: life: 10; 25; Длительность: 4 |


## THICK_SKULL <a href="#thick_skull" id="thick_skull"></a>

### Крепкий лоб <a href="#крепкий-лоб" id="крепкий-лоб"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 42 |
| icon | `stun` |
| Характеристики · `stats` | 15–45% увеличение порога оглушения · [Порог оглушения](../reference/Stats/Stats-HERO.md#stock_stun_threshold); 1–4% дополнительного снижения физического урона · [Снижение физического урона](../reference/Stats/Stats-HERO.md#stock_physical_reduction) |


## RETALIATION <a href="#retaliation" id="retaliation"></a>

### Отмщение <a href="#отмщение" id="отмщение"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](../Classes.md#marauder) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 52 |
| icon | `combat` |
| Срабатывание · `trigger` | Событие: `HIT_TAKEN`; Шанс (единица по правилам подсистемы): 10; 20; Удар: Число целей: 1; Урон оружия: 100; 180 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/skills.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
