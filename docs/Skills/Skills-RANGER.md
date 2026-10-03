# RANGER

| Название | Код |
| --- | --- |
| [Расщеплённая стрела](#split_arrow) | SPLIT_ARROW |
| [Прицельный выстрел](#snipe) | SNIPE |
| [Ледяной выстрел](#ice_shot) | ICE_SHOT |
| [Дождь стрел](#rain_of_arrows) | RAIN_OF_ARROWS |
| [Метка снайпера](#snipers_mark) | SNIPERS_MARK |
| [Покров ветра](#wind_veil) | WIND_VEIL |
| [Яростный выстрел](#frenzy_strike) | FRENZY_STRIKE |
| [Грация](#grace) | GRACE |
| [Зоркий глаз](#eagle_eye) | EAGLE_EYE |
| [Охотничий азарт](#hunters_thrill) | HUNTERS_THRILL |
| [Спешка](#haste) | HASTE |
| [Лёгкая стопа](#light_foot) | LIGHT_FOOT |
| [Порыв ветра](#gust) | GUST |
| [Ядовитые наконечники](#poisoned_tips) | POISONED_TIPS |
| [Ледяная вспышка](#frost_burst) | FROST_BURST |


## SPLIT_ARROW <a href="#split_arrow" id="split_arrow"></a>

### Расщеплённая стрела <a href="#расщеплённая-стрела" id="расщеплённая-стрела"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 1 |
| icon | [bow](../reference/Tables/Tables-MODIFIER.md#bow) |
| Мана: уровень 1 → 20 · `mana` | 6; 20 |
| Перезарядка, с · `cooldown` | 2.5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 3; Урон оружия: 93; 155 |


## SNIPE <a href="#snipe" id="snipe"></a>

### Прицельный выстрел <a href="#прицельный-выстрел" id="прицельный-выстрел"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 6 |
| icon | `critical` |
| Мана: уровень 1 → 20 · `mana` | 10; 28 |
| Перезарядка, с · `cooldown` | 5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 228; 393; Характеристики: +5–15% к шансу критического удара · [Шанс критического удара](../reference/Stats/Stats-HERO.md#stock_critical_chance) |


## ICE_SHOT <a href="#ice_shot" id="ice_shot"></a>

### Ледяной выстрел <a href="#ледяной-выстрел" id="ледяной-выстрел"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 12 |
| icon | `cold` |
| Мана: уровень 1 → 20 · `mana` | 12; 32 |
| Перезарядка, с · `cooldown` | 5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 135; 228; Тип урона: `COLD`; Конверсия: 50; 50; Состояния: Состояние: `CHILL`; Шанс (единица по правилам подсистемы): 100; 100; Состояние: `FREEZE`; Шанс (единица по правилам подсистемы): 10; 25 |


## RAIN_OF_ARROWS <a href="#rain_of_arrows" id="rain_of_arrows"></a>

### Дождь стрел <a href="#дождь-стрел" id="дождь-стрел"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 20 |
| icon | [quiver](../reference/Tables/Tables-MODIFIER.md#quiver) |
| Мана: уровень 1 → 20 · `mana` | 16; 40 |
| Перезарядка, с · `cooldown` | 8 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 60; 30 |
| Условие применения · `condition` | `ENEMIES_3` |
| Удар · `hit` | Число целей: 0; Число ударов: 2; Урон оружия: 100.8; 173.6 |


## SNIPERS_MARK <a href="#snipers_mark" id="snipers_mark"></a>

### Метка снайпера <a href="#метка-снайпера" id="метка-снайпера"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `CURSE` |
| Уровень открытия · `unlock` | 30 |
| icon | `focus` |
| Мана: уровень 1 → 20 · `mana` | 16; 36 |
| Перезарядка, с · `cooldown` | 14 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 30; 10 |
| Условие применения · `condition` | `RARE_OR_BOSS` |
| Проклятие · `curse` | Длительность: 8; Число целей: 1; Характеристики: Получаемый множитель крит. удара +20–50% · [Множитель крита по цели](../reference/Stats/Stats-HERO.md#stock_critical_taken); 10–25% увеличение получаемого урона · [Получаемый урон](../reference/Stats/Stats-HERO.md#stock_damage_taken) |


## WIND_VEIL <a href="#wind_veil" id="wind_veil"></a>

### Покров ветра <a href="#покров-ветра" id="покров-ветра"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `GUARD` |
| Уровень открытия · `unlock` | 40 |
| icon | `air` |
| Мана: уровень 1 → 20 · `mana` | 14; 32 |
| Перезарядка, с · `cooldown` | 16 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `LIFE_50` |
| Усиление · `buff` | Длительность: 5; Характеристики: 30–80% увеличение уклонения · [Уклонение](../reference/Stats/Stats-HERO.md#stock_evasion) |


## FRENZY_STRIKE <a href="#frenzy_strike" id="frenzy_strike"></a>

### Яростный выстрел <a href="#яростный-выстрел" id="яростный-выстрел"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 50 |
| icon | `speed` |
| Мана: уровень 1 → 20 · `mana` | 8; 24 |
| Перезарядка, с · `cooldown` | 3 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 110; 170 |
| Заряды · `charges` | Вид: `FRENZY`; gain: 1; 1 |


## GRACE <a href="#grace" id="grace"></a>

### Грация <a href="#грация" id="грация"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 1 |
| icon | `evasion` |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | 20–60% увеличение уклонения · [Уклонение](../reference/Stats/Stats-HERO.md#stock_evasion) |


## EAGLE_EYE <a href="#eagle_eye" id="eagle_eye"></a>

### Зоркий глаз <a href="#зоркий-глаз" id="зоркий-глаз"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 5 |
| icon | `critical` |
| Характеристики · `stats` | 20–60% увеличение шанса критического удара · [Шанс критического удара](../reference/Stats/Stats-HERO.md#stock_critical_chance) |


## HUNTERS_THRILL <a href="#hunters_thrill" id="hunters_thrill"></a>

### Охотничий азарт <a href="#охотничий-азарт" id="охотничий-азарт"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 10 |
| icon | `potion` |
| Срабатывание · `trigger` | Событие: `KILL`; Шанс (единица по правилам подсистемы): 20; 50; flaskCharges: 1 |


## HASTE <a href="#haste" id="haste"></a>

### Спешка <a href="#спешка" id="спешка"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 16 |
| icon | `speed` |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | 6–16% увеличение скорости атаки · [Скорость атаки](../reference/Stats/Stats-HERO.md#stock_attack_speed); 5–15% увеличение скорости передвижения · [Скорость передвижения](../reference/Stats/Stats-HERO.md#stock_movement_speed) |


## LIGHT_FOOT <a href="#light_foot" id="light_foot"></a>

### Лёгкая стопа <a href="#лёгкая-стопа" id="лёгкая-стопа"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 24 |
| icon | `agility` |
| Характеристики · `stats` | 5–15% увеличение скорости передвижения · [Скорость передвижения](../reference/Stats/Stats-HERO.md#stock_movement_speed); 10–30% увеличение уклонения · [Уклонение](../reference/Stats/Stats-HERO.md#stock_evasion) |


## GUST <a href="#gust" id="gust"></a>

### Порыв ветра <a href="#порыв-ветра" id="порыв-ветра"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 32 |
| icon | `air` |
| Срабатывание · `trigger` | Событие: `EVADE`; Усиление: Длительность: 3; Характеристики: 10–25% увеличение скорости атаки · [Скорость атаки](../reference/Stats/Stats-HERO.md#stock_attack_speed) |


## POISONED_TIPS <a href="#poisoned_tips" id="poisoned_tips"></a>

### Ядовитые наконечники <a href="#ядовитые-наконечники" id="ядовитые-наконечники"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 42 |
| icon | `poison` |
| Характеристики · `stats` | 10–30% шанс отравить · [Шанс отравления](../reference/Stats/Stats-HERO.md#stock_poison_chance); 10–30% увеличение урона от яда · [Урон от яда](../reference/Stats/Stats-HERO.md#stock_poison_damage) |


## FROST_BURST <a href="#frost_burst" id="frost_burst"></a>

### Ледяная вспышка <a href="#ледяная-вспышка" id="ледяная-вспышка"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](../Classes.md#ranger) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 52 |
| icon | `cold` |
| Срабатывание · `trigger` | Событие: `CRIT`; Шанс (единица по правилам подсистемы): 15; 30; Удар: Число целей: 0; Урон оружия: 60; 120; Тип урона: `COLD`; Конверсия: 100; 100 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/skills.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
