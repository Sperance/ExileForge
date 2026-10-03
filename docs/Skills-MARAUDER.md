# MARAUDER

| Название | Код |
| --- | --- |
| [Тяжёлый удар](Skills-MARAUDER.md#heavy_strike) | HEAVY_STRIKE |
| [Клич стойкости](Skills-MARAUDER.md#enduring_cry) | ENDURING_CRY |
| [Сотрясение земли](Skills-MARAUDER.md#ground_slam) | GROUND_SLAM |
| [Адский удар](Skills-MARAUDER.md#infernal_blow) | INFERNAL_BLOW |
| [Уязвимость](Skills-MARAUDER.md#vulnerability) | VULNERABILITY |
| [Кровавая ярость](Skills-MARAUDER.md#blood_rage) | BLOOD_RAGE |
| [Рёв стойкости](Skills-MARAUDER.md#enduring_roar) | ENDURING_ROAR |
| [Решимость](Skills-MARAUDER.md#determination) | DETERMINATION |
| [Железная кожа](Skills-MARAUDER.md#iron_skin) | IRON_SKIN |
| [Кровопролитие](Skills-MARAUDER.md#bloodletting) | BLOODLETTING |
| [Гнев](Skills-MARAUDER.md#anger) | ANGER |
| [Ярость берсерка](Skills-MARAUDER.md#berserker_fury) | BERSERKER_FURY |
| [Несокрушимость](Skills-MARAUDER.md#unbreakable) | UNBREAKABLE |
| [Крепкий лоб](Skills-MARAUDER.md#thick_skull) | THICK_SKULL |
| [Отмщение](Skills-MARAUDER.md#retaliation) | RETALIATION |


## HEAVY_STRIKE

### Тяжёлый удар


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 1 |
| icon | `physical` |
| Мана: уровень 1 → 20 · `mana` | 6; 20 |
| Перезарядка, с · `cooldown` | 3 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 139; 232; Оглушение: 20; 40 |


## ENDURING_CRY

### Клич стойкости


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `WARCRY` |
| Уровень открытия · `unlock` | 6 |
| icon | [armour](Tables-MODIFIER.md#armour) |
| Мана: уровень 1 → 20 · `mana` | 10; 30 |
| Перезарядка, с · `cooldown` | 12 |
| Условие применения · `condition` | `FIGHT_START` |
| Усиление · `buff` | Длительность: 6; Характеристики: 20–60% увеличение брони · [Броня](Stats-HERO.md#stock_armor); Восстанавливает 1–3% здоровья в секунду · [Регенерация здоровья, %](Stats-HERO.md#stock_life_regen_percent) |


## GROUND_SLAM

### Сотрясение земли


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 12 |
| icon | `earth` |
| Мана: уровень 1 → 20 · `mana` | 12; 34 |
| Перезарядка, с · `cooldown` | 6 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `ENEMIES_3` |
| Удар · `hit` | Число целей: 0; Урон оружия: 85; 147; Оглушение: 25; 25 |


## INFERNAL_BLOW

### Адский удар


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 20 |
| icon | `fire` |
| Мана: уровень 1 → 20 · `mana` | 14; 36 |
| Перезарядка, с · `cooldown` | 5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 116; 201; Тип урона: `FIRE`; Конверсия: 40; 80; Состояния: Состояние: `IGNITE`; Шанс (единица по правилам подсистемы): 30; 50 |


## VULNERABILITY

### Уязвимость


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `CURSE` |
| Уровень открытия · `unlock` | 30 |
| icon | `bleeding` |
| Мана: уровень 1 → 20 · `mana` | 18; 40 |
| Перезарядка, с · `cooldown` | 15 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 30; 10 |
| Условие применения · `condition` | `RARE_OR_BOSS` |
| Проклятие · `curse` | Длительность: 8; Число целей: 0; Характеристики: 15–35% увеличение получаемого физического урона · [Получаемый физический урон](Stats-HERO.md#stock_physical_taken); Шанс кровотечения по цели · ADD · 10–25 · [Шанс кровотечения по цели](Stats-HERO.md#stock_bleed_taken) |


## BLOOD_RAGE

### Кровавая ярость


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `GUARD` |
| Уровень открытия · `unlock` | 40 |
| icon | `leech` |
| Мана: уровень 1 → 20 · `mana` | 16; 34 |
| Перезарядка, с · `cooldown` | 20 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `LIFE_50` |
| Усиление · `buff` | Длительность: 8; Характеристики: 10–25% увеличение скорости атаки · [Скорость атаки](Stats-HERO.md#stock_attack_speed); 1–3% урона крадётся здоровьем · [Общий вампиризм](Stats-HERO.md#stock_leech_all) |


## ENDURING_ROAR

### Рёв стойкости


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `WARCRY` |
| Уровень открытия · `unlock` | 50 |
| icon | [armour](Tables-MODIFIER.md#armour) |
| Мана: уровень 1 → 20 · `mana` | 14; 34 |
| Перезарядка, с · `cooldown` | 10 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 60; 30 |
| Условие применения · `condition` | `FIGHT_START` |
| Усиление · `buff` | Длительность: 4; Характеристики: 2–6% дополнительного снижения физического урона · [Снижение физического урона](Stats-HERO.md#stock_physical_reduction) |
| Заряды · `charges` | Вид: `ENDURANCE`; gain: 1; 3 |


## DETERMINATION

### Решимость


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 1 |
| icon | [armour](Tables-MODIFIER.md#armour) |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | 20–50% увеличение брони · [Броня](Stats-HERO.md#stock_armor) |


## IRON_SKIN

### Железная кожа


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 5 |
| icon | `constitution` |
| Характеристики · `stats` | 4–12% увеличение максимума здоровья · [Здоровье](Stats-HERO.md#stock_health); 10–30% увеличение брони · [Броня](Stats-HERO.md#stock_armor) |


## BLOODLETTING

### Кровопролитие


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 10 |
| icon | `health` |
| Срабатывание · `trigger` | Событие: `KILL`; Лечение: life: 2; 6 |


## ANGER

### Гнев


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 16 |
| icon | `fire` |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | +6–140 к урону от огня · [Урон огнём](Stats-HERO.md#stock_attack_fire) |


## BERSERKER_FURY

### Ярость берсерка


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 24 |
| icon | `strength` |
| Характеристики · `stats` | 10–30% увеличение урона · [Урон](Stats-HERO.md#stock_damage) |
| lowLife | Да |


## UNBREAKABLE

### Несокрушимость


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 32 |
| icon | [shield](Tables-MODIFIER.md#shield) |
| Срабатывание · `trigger` | Событие: `LOW_LIFE`; Перезарядка, с: 30; Барьер: life: 10; 25; Длительность: 4 |


## THICK_SKULL

### Крепкий лоб


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 42 |
| icon | `stun` |
| Характеристики · `stats` | 15–45% увеличение порога оглушения · [Порог оглушения](Stats-HERO.md#stock_stun_threshold); 1–4% дополнительного снижения физического урона · [Снижение физического урона](Stats-HERO.md#stock_physical_reduction) |


## RETALIATION

### Отмщение


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Мародёр (Marauder)](Classes.md#marauder) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 52 |
| icon | `combat` |
| Срабатывание · `trigger` | Событие: `HIT_TAKEN`; Шанс (единица по правилам подсистемы): 10; 20; Удар: Число целей: 1; Урон оружия: 100; 180 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/skills.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
