# SHADOW

| Название | Код |
| --- | --- |
| [Змеиный укус](Skills-SHADOW.md#viper_strike) | VIPER_STRIKE |
| [Шквал клинков](Skills-SHADOW.md#blade_flurry) | BLADE_FLURRY |
| [Покров теней](Skills-SHADOW.md#shadow_cloak) | SHADOW_CLOAK |
| [Ядовитое облако](Skills-SHADOW.md#toxic_cloud) | TOXIC_CLOUD |
| [Отчаяние](Skills-SHADOW.md#despair) | DESPAIR |
| [Удар в спину](Skills-SHADOW.md#backstab) | BACKSTAB |
| [Разрядка](Skills-SHADOW.md#discharge) | DISCHARGE |
| [Злоба](Skills-SHADOW.md#malevolence) | MALEVOLENCE |
| [Убийца](Skills-SHADOW.md#assassin) | ASSASSIN |
| [Исчезновение](Skills-SHADOW.md#vanish) | VANISH |
| [Проворство](Skills-SHADOW.md#swiftness) | SWIFTNESS |
| [Ядовед](Skills-SHADOW.md#toxicologist) | TOXICOLOGIST |
| [Смертельный яд](Skills-SHADOW.md#lethal_venom) | LETHAL_VENOM |
| [Незаметность](Skills-SHADOW.md#stealth) | STEALTH |
| [Кража сил](Skills-SHADOW.md#siphon) | SIPHON |


## VIPER_STRIKE

### Змеиный укус


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 1 |
| icon | `poison` |
| Мана: уровень 1 → 20 · `mana` | 6; 20 |
| Перезарядка, с · `cooldown` | 2.5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 152; 278; Состояния: Состояние: `POISON`; Шанс (единица по правилам подсистемы): 100; 100 |


## BLADE_FLURRY

### Шквал клинков


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 6 |
| icon | [blade](Tables-MODIFIER.md#blade) |
| Мана: уровень 1 → 20 · `mana` | 10; 28 |
| Перезарядка, с · `cooldown` | 5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `ENEMIES_3` |
| Удар · `hit` | Число целей: 0; Число ударов: 2; Урон оружия: 76; 139 |


## SHADOW_CLOAK

### Покров теней


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `GUARD` |
| Уровень открытия · `unlock` | 12 |
| icon | `invisible` |
| Мана: уровень 1 → 20 · `mana` | 12; 30 |
| Перезарядка, с · `cooldown` | 16 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `LIFE_50` |
| Усиление · `buff` | Длительность: 4; Характеристики: 30–80% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion); nextCrit: Да |


## TOXIC_CLOUD

### Ядовитое облако


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 20 |
| icon | `poison` |
| Мана: уровень 1 → 20 · `mana` | 14; 36 |
| Перезарядка, с · `cooldown` | 7 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `ENEMIES_3` |
| Урон со временем · `dot` | Число целей: 0; Тип урона: `CHAOS`; Минимум: 66.5; 376.6; Максимум: 98; 559.3; Длительность: 4; Состояния: Состояние: `POISON`; Шанс (единица по правилам подсистемы): 100; 100 |


## DESPAIR

### Отчаяние


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `CURSE` |
| Уровень открытия · `unlock` | 30 |
| icon | `dark` |
| Мана: уровень 1 → 20 · `mana` | 16; 38 |
| Перезарядка, с · `cooldown` | 14 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 30; 10 |
| Условие применения · `condition` | `RARE_OR_BOSS` |
| Проклятие · `curse` | Длительность: 8; Число целей: 0; Характеристики: +-15–-35% к сопротивлению хаосу · [Сопротивление хаосу](Stats-HERO.md#stock_resist_chaos); Получаемый урон со временем · ADD · 10–30 · [Получаемый урон со временем](Stats-HERO.md#stock_dot_taken) |


## BACKSTAB

### Удар в спину


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 40 |
| icon | [blade](Tables-MODIFIER.md#blade) |
| Мана: уровень 1 → 20 · `mana` | 16; 38 |
| Перезарядка, с · `cooldown` | 10 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 60; 30 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 342; 568.8; finisher: 300; 500 |


## DISCHARGE

### Разрядка


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 50 |
| icon | `lightning` |
| Мана: уровень 1 → 20 · `mana` | 18; 40 |
| Перезарядка, с · `cooldown` | 8 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 0; Заклинание: Тип урона: `LIGHTNING`; Минимум: 10; 90; Максимум: 20; 160 |
| Заряды · `charges` | Вид: `ALL`; consume: Да; perCharge: 60; 120 |


## MALEVOLENCE

### Злоба


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 1 |
| icon | `dark` |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | 10–30% увеличение урона от горения · [Урон от горения](Stats-HERO.md#stock_burning_damage); 10–30% увеличение урона от яда · [Урон от яда](Stats-HERO.md#stock_poison_damage); 10–30% увеличение урона от кровотечения · [Урон от кровотечения](Stats-HERO.md#stock_bleed_damage); 10–25% увеличение длительности состояний на врагах · [Длительность состояний](Stats-HERO.md#stock_ailment_duration) |


## ASSASSIN

### Убийца


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 5 |
| icon | `critical` |
| Характеристики · `stats` | 20–60% увеличение шанса критического удара · [Шанс критического удара](Stats-HERO.md#stock_critical_chance); +10–30% к множителю критического удара · [Множитель критического удара](Stats-HERO.md#stock_critical_multiplier) |


## VANISH

### Исчезновение


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 10 |
| icon | `invisible` |
| Срабатывание · `trigger` | Событие: `KILL`; Усиление: Длительность: 2; Характеристики: 50–100% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion) |


## SWIFTNESS

### Проворство


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 16 |
| icon | `speed` |
| Резерв маны · `reserve` | 25 |
| Характеристики · `stats` | 6–16% увеличение скорости атаки · [Скорость атаки](Stats-HERO.md#stock_attack_speed); 5–15% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion) |


## TOXICOLOGIST

### Ядовед


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 24 |
| icon | `poison` |
| Характеристики · `stats` | 15–45% увеличение урона от яда · [Урон от яда](Stats-HERO.md#stock_poison_damage) |


## LETHAL_VENOM

### Смертельный яд


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 32 |
| icon | `poison` |
| Срабатывание · `trigger` | Событие: `CRIT`; Состояние: `POISON`; twice: Да |


## STEALTH

### Незаметность


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 42 |
| icon | `evasion` |
| Характеристики · `stats` | 15–45% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion) |


## SIPHON

### Кража сил


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Тень (Shadow)](Classes.md#shadow) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 52 |
| icon | `mana` |
| Срабатывание · `trigger` | Событие: `EVADE`; Лечение: Мана: уровень 1 → 20: 1; 4 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/skills.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
