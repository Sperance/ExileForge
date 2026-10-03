# DUELIST

| Название | Код |
| --- | --- |
| [Двойной удар](#double_strike) | DOUBLE_STRIKE |
| [Молниеносный выпад](#lunge) | LUNGE |
| [Вихрь стали](#steel_whirl) | STEEL_WHIRL |
| [Вызов](#challenge) | CHALLENGE |
| [Бессилие](#enfeeble) | ENFEEBLE |
| [Рипост](#riposte) | RIPOSTE |
| [Заряженный рывок](#charged_dash) | CHARGED_DASH |
| [Точность](#precision) | PRECISION |
| [Мастер клинка](#blade_master) | BLADE_MASTER |
| [Натиск](#onslaught) | ONSLAUGHT |
| [Жажда крови](#bloodlust) | BLOODLUST |
| [Фехтовальщик](#swordsman) | SWORDSMAN |
| [Второе дыхание](#second_wind) | SECOND_WIND |
| [Беспощадность](#ruthless) | RUTHLESS |
| [Смертельный удар](#coup_de_grace) | COUP_DE_GRACE |


## DOUBLE_STRIKE <a href="#double_strike" id="double_strike"></a>

### Двойной удар <a href="#двойной-удар" id="двойной-удар"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 1 |
| icon | `doublesword` |
| Мана: уровень 1 → 20 · `mana` | 6; 20 |
| Перезарядка, с · `cooldown` | 2.5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Число ударов: 2; Урон оружия: 73.5; 129 |


## LUNGE <a href="#lunge" id="lunge"></a>

### Молниеносный выпад <a href="#молниеносный-выпад" id="молниеносный-выпад"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 6 |
| icon | `longsword` |
| Мана: уровень 1 → 20 · `mana` | 9; 26 |
| Перезарядка, с · `cooldown` | 4 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 208; 355; Характеристики: +10–25% к шансу критического удара · [Шанс критического удара](../reference/Stats/Stats-HERO.md#stock_critical_chance) |


## STEEL_WHIRL <a href="#steel_whirl" id="steel_whirl"></a>

### Вихрь стали <a href="#вихрь-стали" id="вихрь-стали"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 12 |
| icon | `dual` |
| Мана: уровень 1 → 20 · `mana` | 14; 36 |
| Перезарядка, с · `cooldown` | 8 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 60; 30 |
| Условие применения · `condition` | `ENEMIES_3` |
| Удар · `hit` | Число целей: 0; Число ударов: 3; Урон оружия: 74; 122 |


## CHALLENGE <a href="#challenge" id="challenge"></a>

### Вызов <a href="#вызов" id="вызов"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `WARCRY` |
| Уровень открытия · `unlock` | 20 |
| icon | `combat` |
| Мана: уровень 1 → 20 · `mana` | 12; 30 |
| Перезарядка, с · `cooldown` | 12 |
| Условие применения · `condition` | `FIGHT_START` |
| Усиление · `buff` | Длительность: 6; Характеристики: 15–40% увеличение урона · [Урон](../reference/Stats/Stats-HERO.md#stock_damage) |


## ENFEEBLE <a href="#enfeeble" id="enfeeble"></a>

### Бессилие <a href="#бессилие" id="бессилие"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `CURSE` |
| Уровень открытия · `unlock` | 30 |
| icon | `dark` |
| Мана: уровень 1 → 20 · `mana` | 16; 36 |
| Перезарядка, с · `cooldown` | 15 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 30; 10 |
| Условие применения · `condition` | `RARE_OR_BOSS` |
| Проклятие · `curse` | Длительность: 8; Число целей: 0; Характеристики: Урон · MORE · -15–-35 · [Урон](../reference/Stats/Stats-HERO.md#stock_damage) |


## RIPOSTE <a href="#riposte" id="riposte"></a>

### Рипост <a href="#рипост" id="рипост"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `GUARD` |
| Уровень открытия · `unlock` | 40 |
| icon | `block` |
| Мана: уровень 1 → 20 · `mana` | 14; 32 |
| Перезарядка, с · `cooldown` | 18 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `LIFE_50` |
| Усиление · `buff` | Длительность: 6; Характеристики: +15–35% к шансу блока · [Шанс блока](../reference/Stats/Stats-HERO.md#stock_block_chance); Счётчик: 100; 200 |


## CHARGED_DASH <a href="#charged_dash" id="charged_dash"></a>

### Заряженный рывок <a href="#заряженный-рывок" id="заряженный-рывок"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 50 |
| icon | `dual` |
| Мана: уровень 1 → 20 · `mana` | 14; 34 |
| Перезарядка, с · `cooldown` | 6 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 2; Урон оружия: 70; 120 |
| Заряды · `charges` | Вид: `FRENZY`; consume: Да; perCharge: 30; 60 |


## PRECISION <a href="#precision" id="precision"></a>

### Точность <a href="#точность" id="точность"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 1 |
| icon | `critical` |
| Резерв маны · `reserve` | 25 |
| Характеристики · `stats` | 15–45% увеличение шанса критического удара · [Шанс критического удара](../reference/Stats/Stats-HERO.md#stock_critical_chance); +10–30% к множителю критического удара · [Множитель критического удара](../reference/Stats/Stats-HERO.md#stock_critical_multiplier) |


## BLADE_MASTER <a href="#blade_master" id="blade_master"></a>

### Мастер клинка <a href="#мастер-клинка" id="мастер-клинка"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 5 |
| icon | [sword](../reference/Tables/Tables-MODIFIER.md#sword) |
| Характеристики · `stats` | 10–35% увеличение физического урона · [Физический урон](../reference/Stats/Stats-HERO.md#stock_attack_physical) |


## ONSLAUGHT <a href="#onslaught" id="onslaught"></a>

### Натиск <a href="#натиск" id="натиск"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 10 |
| icon | `speed` |
| Срабатывание · `trigger` | Событие: `KILL`; Усиление: Длительность: 4; Характеристики: 10–25% увеличение скорости атаки · [Скорость атаки](../reference/Stats/Stats-HERO.md#stock_attack_speed) |


## BLOODLUST <a href="#bloodlust" id="bloodlust"></a>

### Жажда крови <a href="#жажда-крови" id="жажда-крови"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 16 |
| icon | `bleeding` |
| Резерв маны · `reserve` | 25 |
| Характеристики · `stats` | 15–40% шанс вызвать кровотечение · [Шанс кровотечения](../reference/Stats/Stats-HERO.md#stock_bleed_chance); 10–30% увеличение урона от кровотечения · [Урон от кровотечения](../reference/Stats/Stats-HERO.md#stock_bleed_damage) |


## SWORDSMAN <a href="#swordsman" id="swordsman"></a>

### Фехтовальщик <a href="#фехтовальщик" id="фехтовальщик"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 24 |
| icon | `block` |
| Характеристики · `stats` | +3–10% к шансу блока · [Шанс блока](../reference/Stats/Stats-HERO.md#stock_block_chance) |


## SECOND_WIND <a href="#second_wind" id="second_wind"></a>

### Второе дыхание <a href="#второе-дыхание" id="второе-дыхание"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 32 |
| icon | `regen` |
| Срабатывание · `trigger` | Событие: `BLOCK`; Лечение: life: 1; 4 |


## RUTHLESS <a href="#ruthless" id="ruthless"></a>

### Беспощадность <a href="#беспощадность" id="беспощадность"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 42 |
| icon | `critical` |
| Характеристики · `stats` | +15–45% к множителю критического удара · [Множитель критического удара](../reference/Stats/Stats-HERO.md#stock_critical_multiplier) |


## COUP_DE_GRACE <a href="#coup_de_grace" id="coup_de_grace"></a>

### Смертельный удар <a href="#смертельный-удар" id="смертельный-удар"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Дуэлянт (Duelist)](../Classes.md#duelist) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 52 |
| icon | `bleeding` |
| Характеристики · `stats` | 10–30% увеличение урона от кровотечения · [Урон от кровотечения](../reference/Stats/Stats-HERO.md#stock_bleed_damage) |
| Срабатывание · `trigger` | Событие: `CRIT`; Состояние: `BLEED` |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/skills.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
