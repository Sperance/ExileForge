# TEMPLAR

| Название | Код |
| --- | --- |
| [Кара](Skills-TEMPLAR.md#smite) | SMITE |
| [Очищающее пламя](Skills-TEMPLAR.md#purifying_flame) | PURIFYING_FLAME |
| [Грозовой зов](Skills-TEMPLAR.md#storm_call) | STORM_CALL |
| [Молитва](Skills-TEMPLAR.md#prayer) | PRAYER |
| [Проводимость](Skills-TEMPLAR.md#conductivity) | CONDUCTIVITY |
| [Божественный щит](Skills-TEMPLAR.md#divine_shield) | DIVINE_SHIELD |
| [Земной выброс](Skills-TEMPLAR.md#earthen_release) | EARTHEN_RELEASE |
| [Чистота стихий](Skills-TEMPLAR.md#purity_of_elements) | PURITY_OF_ELEMENTS |
| [Вера](Skills-TEMPLAR.md#faith) | FAITH |
| [Благословение](Skills-TEMPLAR.md#blessing) | BLESSING |
| [Гнев небес](Skills-TEMPLAR.md#wrath) | WRATH |
| [Святое оружие](Skills-TEMPLAR.md#holy_arms) | HOLY_ARMS |
| [Праведность](Skills-TEMPLAR.md#righteousness) | RIGHTEOUSNESS |
| [Стойкость](Skills-TEMPLAR.md#fortitude) | FORTITUDE |
| [Мученик](Skills-TEMPLAR.md#martyr) | MARTYR |


## SMITE

### Кара


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 1 |
| icon | `lightning` |
| Мана: уровень 1 → 20 · `mana` | 7; 22 |
| Перезарядка, с · `cooldown` | 3 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 158; 271; Тип урона: `LIGHTNING`; Конверсия: 40; 40; Состояния: Состояние: `SHOCK`; Шанс (единица по правилам подсистемы): 20; 40 |


## PURIFYING_FLAME

### Очищающее пламя


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 6 |
| icon | `fire` |
| Мана: уровень 1 → 20 · `mana` | 9; 28 |
| Перезарядка, с · `cooldown` | 3 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `ENEMIES_3` |
| Удар · `hit` | Число целей: 0; Заклинание: Тип урона: `FIRE`; Минимум: 15.8; 163.2; Максимум: 24.5; 239.7 |


## STORM_CALL

### Грозовой зов


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 12 |
| icon | `lightning` |
| Мана: уровень 1 → 20 · `mana` | 12; 32 |
| Перезарядка, с · `cooldown` | 5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Заклинание: Тип урона: `LIGHTNING`; Минимум: 37.8; 310.6; Максимум: 107.1; 933.3; Состояния: Состояние: `SHOCK`; Шанс (единица по правилам подсистемы): 100; 100 |


## PRAYER

### Молитва


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `HEAL` |
| Уровень открытия · `unlock` | 20 |
| icon | `light` |
| Мана: уровень 1 → 20 · `mana` | 16; 40 |
| Перезарядка, с · `cooldown` | 18 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `LIFE_50` |
| Лечение · `heal` | life: 15; 40; cleanse: Да |


## CONDUCTIVITY

### Проводимость


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `CURSE` |
| Уровень открытия · `unlock` | 30 |
| icon | `lightning` |
| Мана: уровень 1 → 20 · `mana` | 16; 38 |
| Перезарядка, с · `cooldown` | 14 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 30; 10 |
| Условие применения · `condition` | `RARE_OR_BOSS` |
| Проклятие · `curse` | Длительность: 8; Число целей: 0; Характеристики: +-15–-35% к сопротивлению молнии · [Сопротивление молнии](Stats-HERO.md#stock_resist_lightning); +10–25% к силе шока по цели · [Сила шока по цели](Stats-HERO.md#stock_shock_taken) |


## DIVINE_SHIELD

### Божественный щит


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `GUARD` |
| Уровень открытия · `unlock` | 40 |
| icon | [shield](Tables-MODIFIER.md#shield) |
| Мана: уровень 1 → 20 · `mana` | 18; 42 |
| Перезарядка, с · `cooldown` | 24 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `LIFE_35` |
| Барьер · `barrier` | life: 15; 35; Длительность: 6 |


## EARTHEN_RELEASE

### Земной выброс


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 50 |
| icon | `earth` |
| Мана: уровень 1 → 20 · `mana` | 16; 38 |
| Перезарядка, с · `cooldown` | 6 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 0; Урон оружия: 60; 110; Оглушение: 10; 30 |
| Заряды · `charges` | Вид: `ENDURANCE`; consume: Да; perCharge: 40; 80 |


## PURITY_OF_ELEMENTS

### Чистота стихий


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 1 |
| icon | `resist` |
| Резерв маны · `reserve` | 25 |
| Характеристики · `stats` | +10–25% к всем сопротивлениям стихиям · [Все сопротивления стихиям](Stats-HERO.md#stock_resist_all) |


## FAITH

### Вера


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 5 |
| icon | `light` |
| Характеристики · `stats` | 3–10% увеличение максимума здоровья · [Здоровье](Stats-HERO.md#stock_health); 5–15% увеличение максимума маны · [Мана](Stats-HERO.md#stock_mana) |


## BLESSING

### Благословение


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 10 |
| icon | [armour](Tables-MODIFIER.md#armour) |
| Срабатывание · `trigger` | Событие: `HEALED`; Усиление: Длительность: 4; Характеристики: 20–50% увеличение брони · [Броня](Stats-HERO.md#stock_armor) |


## WRATH

### Гнев небес


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 16 |
| icon | `lightning` |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | +5–132 к урону от молнии · [Урон молнией](Stats-HERO.md#stock_attack_lightning) |


## HOLY_ARMS

### Святое оружие


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 24 |
| icon | `magical` |
| Характеристики · `stats` | 10–30% увеличение урона от огня · [Урон огнём](Stats-HERO.md#stock_attack_fire); 10–30% увеличение урона от холода · [Урон холодом](Stats-HERO.md#stock_attack_cold); 10–30% увеличение урона от молнии · [Урон молнией](Stats-HERO.md#stock_attack_lightning) |


## RIGHTEOUSNESS

### Праведность


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 32 |
| icon | `health` |
| Срабатывание · `trigger` | Событие: `KILL`; Лечение: life: 1; 4; Мана: уровень 1 → 20: 1; 3 |


## FORTITUDE

### Стойкость


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 42 |
| icon | `constitution` |
| Характеристики · `stats` | 10–30% увеличение брони · [Броня](Stats-HERO.md#stock_armor); 10–30% увеличение порога оглушения · [Порог оглушения](Stats-HERO.md#stock_stun_threshold) |


## MARTYR

### Мученик


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](Classes.md#templar) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 52 |
| icon | `regen` |
| Срабатывание · `trigger` | Событие: `LOW_LIFE`; Перезарядка, с: 25; Усиление: Длительность: 4; Характеристики: Восстанавливает 2–5% здоровья в секунду · [Регенерация здоровья, %](Stats-HERO.md#stock_life_regen_percent) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/skills.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
