# WITCH

| Название | Код |
| --- | --- |
| [Огненный шар](#fireball) | FIREBALL |
| [Ледяной импульс](#freezing_pulse) | FREEZING_PULSE |
| [Искра](#spark) | SPARK |
| [Стихийная слабость](#elemental_weakness) | ELEMENTAL_WEAKNESS |
| [Иссушение](#essence_drain) | ESSENCE_DRAIN |
| [Магический барьер](#arcane_ward) | ARCANE_WARD |
| [Вытягивание силы](#power_siphon) | POWER_SIPHON |
| [Дисциплина](#discipline) | DISCIPLINE |
| [Мастер стихий](#elemental_mastery) | ELEMENTAL_MASTERY |
| [Жатва маны](#mana_harvest) | MANA_HARVEST |
| [Ясность](#clarity) | CLARITY |
| [Глубокий резерв](#deep_reserve) | DEEP_RESERVE |
| [Раскол стихий](#elemental_surge) | ELEMENTAL_SURGE |
| [Беглые чары](#swift_casting) | SWIFT_CASTING |
| [Последний оплот](#last_ward) | LAST_WARD |


## FIREBALL <a href="#fireball" id="fireball"></a>

### Огненный шар <a href="#огненный-шар" id="огненный-шар"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 1 |
| icon | `fire` |
| Мана: уровень 1 → 20 · `mana` | 7; 24 |
| Перезарядка, с · `cooldown` | 2 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Заклинание: Тип урона: `FIRE`; Минимум: 28; 279.6; Максимум: 38.5; 419.1; Состояния: Состояние: `IGNITE`; Шанс (единица по правилам подсистемы): 25; 40 |


## FREEZING_PULSE <a href="#freezing_pulse" id="freezing_pulse"></a>

### Ледяной импульс <a href="#ледяной-импульс" id="ледяной-импульс"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 6 |
| icon | `cold` |
| Мана: уровень 1 → 20 · `mana` | 8; 26 |
| Перезарядка, с · `cooldown` | 3 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 2; Заклинание: Тип урона: `COLD`; Минимум: 21; 247.3; Максимум: 35; 370.6; Состояния: Состояние: `FREEZE`; Шанс (единица по правилам подсистемы): 20; 35 |


## SPARK <a href="#spark" id="spark"></a>

### Искра <a href="#искра" id="искра"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 12 |
| icon | `lightning` |
| Мана: уровень 1 → 20 · `mana` | 10; 30 |
| Перезарядка, с · `cooldown` | 3 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `ENEMIES_3` |
| Удар · `hit` | Число целей: 4; Заклинание: Тип урона: `LIGHTNING`; Минимум: 8.8; 74.8; Максимум: 52.5; 451.3; Состояния: Состояние: `SHOCK`; Шанс (единица по правилам подсистемы): 25; 45 |


## ELEMENTAL_WEAKNESS <a href="#elemental_weakness" id="elemental_weakness"></a>

### Стихийная слабость <a href="#стихийная-слабость" id="стихийная-слабость"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `CURSE` |
| Уровень открытия · `unlock` | 20 |
| icon | `resist` |
| Мана: уровень 1 → 20 · `mana` | 16; 38 |
| Перезарядка, с · `cooldown` | 14 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 30; 10 |
| Условие применения · `condition` | `RARE_OR_BOSS` |
| Проклятие · `curse` | Длительность: 8; Число целей: 0; Характеристики: +-10–-30% к всем сопротивлениям стихиям · [Все сопротивления стихиям](../reference/Stats/Stats-HERO.md#stock_resist_all) |


## ESSENCE_DRAIN <a href="#essence_drain" id="essence_drain"></a>

### Иссушение <a href="#иссушение" id="иссушение"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 30 |
| icon | `chaos` |
| Мана: уровень 1 → 20 · `mana` | 14; 34 |
| Перезарядка, с · `cooldown` | 6 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `READY` |
| Урон со временем · `dot` | Число целей: 1; Тип урона: `CHAOS`; Минимум: 87.5; 483.6; Максимум: 133; 731; Длительность: 4; Состояния: Состояние: `POISON`; Шанс (единица по правилам подсистемы): 100; 100 |


## ARCANE_WARD <a href="#arcane_ward" id="arcane_ward"></a>

### Магический барьер <a href="#магический-барьер" id="магический-барьер"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `GUARD` |
| Уровень открытия · `unlock` | 40 |
| icon | `shield_energy` |
| Мана: уровень 1 → 20 · `mana` | 18; 40 |
| Перезарядка, с · `cooldown` | 20 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `SHIELD_BROKEN` |
| Усиление · `buff` | Длительность: 6; Характеристики: 20–50% увеличение скорости восполнения энергетического щита · [Восполнение щита](../reference/Stats/Stats-HERO.md#stock_shield_recharge) |
| Щит · `shield` | 15; 40 |


## POWER_SIPHON <a href="#power_siphon" id="power_siphon"></a>

### Вытягивание силы <a href="#вытягивание-силы" id="вытягивание-силы"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 50 |
| icon | `chaos` |
| Мана: уровень 1 → 20 · `mana` | 10; 28 |
| Перезарядка, с · `cooldown` | 3 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Заклинание: Тип урона: `CHAOS`; Минимум: 30; 300; Максимум: 45; 450 |
| Заряды · `charges` | Вид: `POWER`; gain: 1; 1 |


## DISCIPLINE <a href="#discipline" id="discipline"></a>

### Дисциплина <a href="#дисциплина" id="дисциплина"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 1 |
| icon | `shield_energy` |
| Резерв маны · `reserve` | 25 |
| Характеристики · `stats` | 10–40% увеличение энергетического щита · [Энергетический щит](../reference/Stats/Stats-HERO.md#stock_energy_shield) |


## ELEMENTAL_MASTERY <a href="#elemental_mastery" id="elemental_mastery"></a>

### Мастер стихий <a href="#мастер-стихий" id="мастер-стихий"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 5 |
| icon | `magical` |
| Характеристики · `stats` | 10–35% увеличение урона от огня · [Урон огнём](../reference/Stats/Stats-HERO.md#stock_attack_fire); 10–35% увеличение урона от холода · [Урон холодом](../reference/Stats/Stats-HERO.md#stock_attack_cold); 10–35% увеличение урона от молнии · [Урон молнией](../reference/Stats/Stats-HERO.md#stock_attack_lightning) |


## MANA_HARVEST <a href="#mana_harvest" id="mana_harvest"></a>

### Жатва маны <a href="#жатва-маны" id="жатва-маны"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 10 |
| icon | `mana` |
| Срабатывание · `trigger` | Событие: `SPELL_KILL`; Лечение: Мана: уровень 1 → 20: 3; 8 |


## CLARITY <a href="#clarity" id="clarity"></a>

### Ясность <a href="#ясность" id="ясность"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 16 |
| icon | `mana` |
| Резерв маны · `reserve` | 20 |
| Характеристики · `stats` | 30–120% увеличение скорости регенерации маны · [Восстановление маны](../reference/Stats/Stats-HERO.md#stock_mana_regen) |


## DEEP_RESERVE <a href="#deep_reserve" id="deep_reserve"></a>

### Глубокий резерв <a href="#глубокий-резерв" id="глубокий-резерв"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 24 |
| icon | `intellect` |
| Характеристики · `stats` | 8–25% увеличение максимума маны · [Мана](../reference/Stats/Stats-HERO.md#stock_mana) |


## ELEMENTAL_SURGE <a href="#elemental_surge" id="elemental_surge"></a>

### Раскол стихий <a href="#раскол-стихий" id="раскол-стихий"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 32 |
| icon | `orb_spark` |
| Срабатывание · `trigger` | Событие: `SPELL_CRIT`; Шанс (единица по правилам подсистемы): 20; 40; Состояние: `ELEMENT` |


## SWIFT_CASTING <a href="#swift_casting" id="swift_casting"></a>

### Беглые чары <a href="#беглые-чары" id="беглые-чары"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 42 |
| icon | `speed` |
| Характеристики · `stats` | 6–18% увеличение скорости сотворения · [Скорость применения](../reference/Stats/Stats-HERO.md#stock_cast_speed) |


## LAST_WARD <a href="#last_ward" id="last_ward"></a>

### Последний оплот <a href="#последний-оплот" id="последний-оплот"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Ведьма (Witch)](../Classes.md#witch) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 52 |
| icon | `shield_energy` |
| Срабатывание · `trigger` | Событие: `SHIELD_BROKEN`; Перезарядка, с: 30; Щит: 20; 40 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/skills.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
