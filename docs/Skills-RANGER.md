# RANGER

| Название | Код |
| --- | --- |
| [Расщеплённая стрела](Skills-RANGER.md#split_arrow) | SPLIT_ARROW |
| [Прицельный выстрел](Skills-RANGER.md#snipe) | SNIPE |
| [Ледяной выстрел](Skills-RANGER.md#ice_shot) | ICE_SHOT |
| [Дождь стрел](Skills-RANGER.md#rain_of_arrows) | RAIN_OF_ARROWS |
| [Метка снайпера](Skills-RANGER.md#snipers_mark) | SNIPERS_MARK |
| [Покров ветра](Skills-RANGER.md#wind_veil) | WIND_VEIL |
| [Яростный выстрел](Skills-RANGER.md#frenzy_strike) | FRENZY_STRIKE |
| [Грация](Skills-RANGER.md#grace) | GRACE |
| [Зоркий глаз](Skills-RANGER.md#eagle_eye) | EAGLE_EYE |
| [Охотничий азарт](Skills-RANGER.md#hunters_thrill) | HUNTERS_THRILL |
| [Спешка](Skills-RANGER.md#haste) | HASTE |
| [Лёгкая стопа](Skills-RANGER.md#light_foot) | LIGHT_FOOT |
| [Порыв ветра](Skills-RANGER.md#gust) | GUST |
| [Ядовитые наконечники](Skills-RANGER.md#poisoned_tips) | POISONED_TIPS |
| [Ледяная вспышка](Skills-RANGER.md#frost_burst) | FROST_BURST |


## SPLIT_ARROW

### Расщеплённая стрела


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 1 |
| icon | [bow](Tables-MODIFIER.md#bow) |
| Мана: уровень 1 → 20 · `mana` | 6; 20 |
| Перезарядка, с · `cooldown` | 2.5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 3; Урон оружия: 93; 155 |


## SNIPE

### Прицельный выстрел


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 6 |
| icon | `critical` |
| Мана: уровень 1 → 20 · `mana` | 10; 28 |
| Перезарядка, с · `cooldown` | 5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 228; 393; Характеристики: +5–15% к шансу критического удара · [Шанс критического удара](Stats-HERO.md#stock_critical_chance) |


## ICE_SHOT

### Ледяной выстрел


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 12 |
| icon | `cold` |
| Мана: уровень 1 → 20 · `mana` | 12; 32 |
| Перезарядка, с · `cooldown` | 5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 135; 228; Тип урона: `COLD`; Конверсия: 50; 50; Состояния: Состояние: `CHILL`; Шанс (единица по правилам подсистемы): 100; 100; Состояние: `FREEZE`; Шанс (единица по правилам подсистемы): 10; 25 |


## RAIN_OF_ARROWS

### Дождь стрел


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 20 |
| icon | [quiver](Tables-MODIFIER.md#quiver) |
| Мана: уровень 1 → 20 · `mana` | 16; 40 |
| Перезарядка, с · `cooldown` | 8 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 60; 30 |
| Условие применения · `condition` | `ENEMIES_3` |
| Удар · `hit` | Число целей: 0; Число ударов: 2; Урон оружия: 100.8; 173.6 |


## SNIPERS_MARK

### Метка снайпера


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `CURSE` |
| Уровень открытия · `unlock` | 30 |
| icon | `focus` |
| Мана: уровень 1 → 20 · `mana` | 16; 36 |
| Перезарядка, с · `cooldown` | 14 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 30; 10 |
| Условие применения · `condition` | `RARE_OR_BOSS` |
| Проклятие · `curse` | Длительность: 8; Число целей: 1; Характеристики: Получаемый множитель крит. удара +20–50% · [Множитель крита по цели](Stats-HERO.md#stock_critical_taken); 10–25% увеличение получаемого урона · [Получаемый урон](Stats-HERO.md#stock_damage_taken) |


## WIND_VEIL

### Покров ветра


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `GUARD` |
| Уровень открытия · `unlock` | 40 |
| icon | `air` |
| Мана: уровень 1 → 20 · `mana` | 14; 32 |
| Перезарядка, с · `cooldown` | 16 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `LIFE_50` |
| Усиление · `buff` | Длительность: 5; Характеристики: 30–80% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion) |


## FRENZY_STRIKE

### Яростный выстрел


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 50 |
| icon | `speed` |
| Мана: уровень 1 → 20 · `mana` | 8; 24 |
| Перезарядка, с · `cooldown` | 3 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 110; 170 |
| Заряды · `charges` | Вид: `FRENZY`; gain: 1; 1 |


## GRACE

### Грация


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 1 |
| icon | `evasion` |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | 20–60% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion) |


## EAGLE_EYE

### Зоркий глаз


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 5 |
| icon | `critical` |
| Характеристики · `stats` | 20–60% увеличение шанса критического удара · [Шанс критического удара](Stats-HERO.md#stock_critical_chance) |


## HUNTERS_THRILL

### Охотничий азарт


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 10 |
| icon | `potion` |
| Срабатывание · `trigger` | Событие: `KILL`; Шанс (единица по правилам подсистемы): 20; 50; flaskCharges: 1 |


## HASTE

### Спешка


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 16 |
| icon | `speed` |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | 6–16% увеличение скорости атаки · [Скорость атаки](Stats-HERO.md#stock_attack_speed); 5–15% увеличение скорости передвижения · [Скорость передвижения](Stats-HERO.md#stock_movement_speed) |


## LIGHT_FOOT

### Лёгкая стопа


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 24 |
| icon | `agility` |
| Характеристики · `stats` | 5–15% увеличение скорости передвижения · [Скорость передвижения](Stats-HERO.md#stock_movement_speed); 10–30% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion) |


## GUST

### Порыв ветра


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 32 |
| icon | `air` |
| Срабатывание · `trigger` | Событие: `EVADE`; Усиление: Длительность: 3; Характеристики: 10–25% увеличение скорости атаки · [Скорость атаки](Stats-HERO.md#stock_attack_speed) |


## POISONED_TIPS

### Ядовитые наконечники


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 42 |
| icon | `poison` |
| Характеристики · `stats` | 10–30% шанс отравить · [Шанс отравления](Stats-HERO.md#stock_poison_chance); 10–30% увеличение урона от яда · [Урон от яда](Stats-HERO.md#stock_poison_damage) |


## FROST_BURST

### Ледяная вспышка


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Охотница (Ranger)](Classes.md#ranger) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 52 |
| icon | `cold` |
| Срабатывание · `trigger` | Событие: `CRIT`; Шанс (единица по правилам подсистемы): 15; 30; Удар: Число целей: 0; Урон оружия: 60; 120; Тип урона: `COLD`; Конверсия: 100; 100 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/skills.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
