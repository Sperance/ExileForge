# SHADOW

| Название | Код |
| --- | --- |
| [Змеиный укус](#viper_strike) | VIPER_STRIKE |
| [Шквал клинков](#blade_flurry) | BLADE_FLURRY |
| [Покров теней](#shadow_cloak) | SHADOW_CLOAK |
| [Ядовитое облако](#toxic_cloud) | TOXIC_CLOUD |
| [Отчаяние](#despair) | DESPAIR |
| [Удар в спину](#backstab) | BACKSTAB |
| [Разрядка](#discharge) | DISCHARGE |
| [Злоба](#malevolence) | MALEVOLENCE |
| [Убийца](#assassin) | ASSASSIN |
| [Исчезновение](#vanish) | VANISH |
| [Проворство](#swiftness) | SWIFTNESS |
| [Ядовед](#toxicologist) | TOXICOLOGIST |
| [Смертельный яд](#lethal_venom) | LETHAL_VENOM |
| [Незаметность](#stealth) | STEALTH |
| [Кража сил](#siphon) | SIPHON |


## VIPER_STRIKE <a href="#viper_strike" id="viper_strike"></a>

### Змеиный укус <a href="#змеиный-укус" id="змеиный-укус"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 1 |
| icon | `poison` |
| Мана: уровень 1 → 20 · `mana` | 6; 20 |
| Перезарядка, с · `cooldown` | 2.5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 152; 278; Состояния: Состояние: `POISON`; Шанс (единица по правилам подсистемы): 100; 100 |


## BLADE_FLURRY <a href="#blade_flurry" id="blade_flurry"></a>

### Шквал клинков <a href="#шквал-клинков" id="шквал-клинков"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 6 |
| icon | [blade](../reference/Tables/Tables-MODIFIER.md#blade) |
| Мана: уровень 1 → 20 · `mana` | 10; 28 |
| Перезарядка, с · `cooldown` | 5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `ENEMIES_3` |
| Удар · `hit` | Число целей: 0; Число ударов: 2; Урон оружия: 76; 139 |


## SHADOW_CLOAK <a href="#shadow_cloak" id="shadow_cloak"></a>

### Покров теней <a href="#покров-теней" id="покров-теней"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `GUARD` |
| Уровень открытия · `unlock` | 12 |
| icon | `invisible` |
| Мана: уровень 1 → 20 · `mana` | 12; 30 |
| Перезарядка, с · `cooldown` | 16 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `LIFE_50` |
| Усиление · `buff` | Длительность: 4; Характеристики: 30–80% увеличение уклонения · [Уклонение](../reference/Stats/Stats-HERO.md#stock_evasion); nextCrit: Да |


## TOXIC_CLOUD <a href="#toxic_cloud" id="toxic_cloud"></a>

### Ядовитое облако <a href="#ядовитое-облако" id="ядовитое-облако"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 20 |
| icon | `poison` |
| Мана: уровень 1 → 20 · `mana` | 14; 36 |
| Перезарядка, с · `cooldown` | 7 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `ENEMIES_3` |
| Урон со временем · `dot` | Число целей: 0; Тип урона: `CHAOS`; Минимум: 66.5; 376.6; Максимум: 98; 559.3; Длительность: 4; Состояния: Состояние: `POISON`; Шанс (единица по правилам подсистемы): 100; 100 |


## DESPAIR <a href="#despair" id="despair"></a>

### Отчаяние <a href="#отчаяние" id="отчаяние"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `CURSE` |
| Уровень открытия · `unlock` | 30 |
| icon | `dark` |
| Мана: уровень 1 → 20 · `mana` | 16; 38 |
| Перезарядка, с · `cooldown` | 14 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 30; 10 |
| Условие применения · `condition` | `RARE_OR_BOSS` |
| Проклятие · `curse` | Длительность: 8; Число целей: 0; Характеристики: +-15–-35% к сопротивлению хаосу · [Сопротивление хаосу](../reference/Stats/Stats-HERO.md#stock_resist_chaos); Получаемый урон со временем · ADD · 10–30 · [Получаемый урон со временем](../reference/Stats/Stats-HERO.md#stock_dot_taken) |


## BACKSTAB <a href="#backstab" id="backstab"></a>

### Удар в спину <a href="#удар-в-спину" id="удар-в-спину"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 40 |
| icon | [blade](../reference/Tables/Tables-MODIFIER.md#blade) |
| Мана: уровень 1 → 20 · `mana` | 16; 38 |
| Перезарядка, с · `cooldown` | 10 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 60; 30 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 342; 568.8; finisher: 300; 500 |


## DISCHARGE <a href="#discharge" id="discharge"></a>

### Разрядка <a href="#разрядка" id="разрядка"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 50 |
| icon | `lightning` |
| Мана: уровень 1 → 20 · `mana` | 18; 40 |
| Перезарядка, с · `cooldown` | 8 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 0; Заклинание: Тип урона: `LIGHTNING`; Минимум: 10; 90; Максимум: 20; 160 |
| Заряды · `charges` | Вид: `ALL`; consume: Да; perCharge: 60; 120 |


## MALEVOLENCE <a href="#malevolence" id="malevolence"></a>

### Злоба <a href="#злоба" id="злоба"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 1 |
| icon | `dark` |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | 10–30% увеличение урона от горения · [Урон от горения](../reference/Stats/Stats-HERO.md#stock_burning_damage); 10–30% увеличение урона от яда · [Урон от яда](../reference/Stats/Stats-HERO.md#stock_poison_damage); 10–30% увеличение урона от кровотечения · [Урон от кровотечения](../reference/Stats/Stats-HERO.md#stock_bleed_damage); 10–25% увеличение длительности состояний на врагах · [Длительность состояний](../reference/Stats/Stats-HERO.md#stock_ailment_duration) |


## ASSASSIN <a href="#assassin" id="assassin"></a>

### Убийца <a href="#убийца" id="убийца"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 5 |
| icon | `critical` |
| Характеристики · `stats` | 20–60% увеличение шанса критического удара · [Шанс критического удара](../reference/Stats/Stats-HERO.md#stock_critical_chance); +10–30% к множителю критического удара · [Множитель критического удара](../reference/Stats/Stats-HERO.md#stock_critical_multiplier) |


## VANISH <a href="#vanish" id="vanish"></a>

### Исчезновение <a href="#исчезновение" id="исчезновение"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 10 |
| icon | `invisible` |
| Срабатывание · `trigger` | Событие: `KILL`; Усиление: Длительность: 2; Характеристики: 50–100% увеличение уклонения · [Уклонение](../reference/Stats/Stats-HERO.md#stock_evasion) |


## SWIFTNESS <a href="#swiftness" id="swiftness"></a>

### Проворство <a href="#проворство" id="проворство"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 16 |
| icon | `speed` |
| Резерв маны · `reserve` | 25 |
| Характеристики · `stats` | 6–16% увеличение скорости атаки · [Скорость атаки](../reference/Stats/Stats-HERO.md#stock_attack_speed); 5–15% увеличение уклонения · [Уклонение](../reference/Stats/Stats-HERO.md#stock_evasion) |


## TOXICOLOGIST <a href="#toxicologist" id="toxicologist"></a>

### Ядовед <a href="#ядовед" id="ядовед"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 24 |
| icon | `poison` |
| Характеристики · `stats` | 15–45% увеличение урона от яда · [Урон от яда](../reference/Stats/Stats-HERO.md#stock_poison_damage) |


## LETHAL_VENOM <a href="#lethal_venom" id="lethal_venom"></a>

### Смертельный яд <a href="#смертельный-яд" id="смертельный-яд"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 32 |
| icon | `poison` |
| Срабатывание · `trigger` | Событие: `CRIT`; Состояние: `POISON`; twice: Да |


## STEALTH <a href="#stealth" id="stealth"></a>

### Незаметность <a href="#незаметность" id="незаметность"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 42 |
| icon | `evasion` |
| Характеристики · `stats` | 15–45% увеличение уклонения · [Уклонение](../reference/Stats/Stats-HERO.md#stock_evasion) |


## SIPHON <a href="#siphon" id="siphon"></a>

### Кража сил <a href="#кража-сил" id="кража-сил"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](../Classes.md#shadow) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 52 |
| icon | `mana` |
| Срабатывание · `trigger` | Событие: `EVADE`; Лечение: Мана: уровень 1 → 20: 1; 4 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/skills.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
