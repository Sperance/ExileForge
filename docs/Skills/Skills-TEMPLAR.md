# TEMPLAR

| Название | Код |
| --- | --- |
| [Кара](#smite) | SMITE |
| [Очищающее пламя](#purifying_flame) | PURIFYING_FLAME |
| [Грозовой зов](#storm_call) | STORM_CALL |
| [Молитва](#prayer) | PRAYER |
| [Проводимость](#conductivity) | CONDUCTIVITY |
| [Божественный щит](#divine_shield) | DIVINE_SHIELD |
| [Земной выброс](#earthen_release) | EARTHEN_RELEASE |
| [Чистота стихий](#purity_of_elements) | PURITY_OF_ELEMENTS |
| [Вера](#faith) | FAITH |
| [Благословение](#blessing) | BLESSING |
| [Гнев небес](#wrath) | WRATH |
| [Святое оружие](#holy_arms) | HOLY_ARMS |
| [Праведность](#righteousness) | RIGHTEOUSNESS |
| [Стойкость](#fortitude) | FORTITUDE |
| [Мученик](#martyr) | MARTYR |


## SMITE <a href="#smite" id="smite"></a>

### Кара <a href="#кара" id="кара"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 1 |
| icon | `lightning` |
| Мана: уровень 1 → 20 · `mana` | 7; 22 |
| Перезарядка, с · `cooldown` | 3 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 158; 271; Тип урона: `LIGHTNING`; Конверсия: 40; 40; Состояния: Состояние: `SHOCK`; Шанс (единица по правилам подсистемы): 20; 40 |


## PURIFYING_FLAME <a href="#purifying_flame" id="purifying_flame"></a>

### Очищающее пламя <a href="#очищающее-пламя" id="очищающее-пламя"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 6 |
| icon | `fire` |
| Мана: уровень 1 → 20 · `mana` | 9; 28 |
| Перезарядка, с · `cooldown` | 3 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `ENEMIES_3` |
| Удар · `hit` | Число целей: 0; Заклинание: Тип урона: `FIRE`; Минимум: 15.8; 163.2; Максимум: 24.5; 239.7 |


## STORM_CALL <a href="#storm_call" id="storm_call"></a>

### Грозовой зов <a href="#грозовой-зов" id="грозовой-зов"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 12 |
| icon | `lightning` |
| Мана: уровень 1 → 20 · `mana` | 12; 32 |
| Перезарядка, с · `cooldown` | 5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Заклинание: Тип урона: `LIGHTNING`; Минимум: 37.8; 310.6; Максимум: 107.1; 933.3; Состояния: Состояние: `SHOCK`; Шанс (единица по правилам подсистемы): 100; 100 |


## PRAYER <a href="#prayer" id="prayer"></a>

### Молитва <a href="#молитва" id="молитва"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `HEAL` |
| Уровень открытия · `unlock` | 20 |
| icon | `light` |
| Мана: уровень 1 → 20 · `mana` | 16; 40 |
| Перезарядка, с · `cooldown` | 18 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `LIFE_50` |
| Лечение · `heal` | life: 15; 40; cleanse: Да |


## CONDUCTIVITY <a href="#conductivity" id="conductivity"></a>

### Проводимость <a href="#проводимость" id="проводимость"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `CURSE` |
| Уровень открытия · `unlock` | 30 |
| icon | `lightning` |
| Мана: уровень 1 → 20 · `mana` | 16; 38 |
| Перезарядка, с · `cooldown` | 14 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 30; 10 |
| Условие применения · `condition` | `RARE_OR_BOSS` |
| Проклятие · `curse` | Длительность: 8; Число целей: 0; Характеристики: +-15–-35% к сопротивлению молнии · [Сопротивление молнии](../reference/Stats/Stats-HERO.md#stock_resist_lightning); +10–25% к силе шока по цели · [Сила шока по цели](../reference/Stats/Stats-HERO.md#stock_shock_taken) |


## DIVINE_SHIELD <a href="#divine_shield" id="divine_shield"></a>

### Божественный щит <a href="#божественный-щит" id="божественный-щит"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `GUARD` |
| Уровень открытия · `unlock` | 40 |
| icon | [shield](../reference/Tables/Tables-MODIFIER.md#shield) |
| Мана: уровень 1 → 20 · `mana` | 18; 42 |
| Перезарядка, с · `cooldown` | 24 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `LIFE_35` |
| Барьер · `barrier` | life: 15; 35; Длительность: 6 |


## EARTHEN_RELEASE <a href="#earthen_release" id="earthen_release"></a>

### Земной выброс <a href="#земной-выброс" id="земной-выброс"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 50 |
| icon | `earth` |
| Мана: уровень 1 → 20 · `mana` | 16; 38 |
| Перезарядка, с · `cooldown` | 6 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 20 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 0; Урон оружия: 60; 110; Оглушение: 10; 30 |
| Заряды · `charges` | Вид: `ENDURANCE`; consume: Да; perCharge: 40; 80 |


## PURITY_OF_ELEMENTS <a href="#purity_of_elements" id="purity_of_elements"></a>

### Чистота стихий <a href="#чистота-стихий" id="чистота-стихий"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 1 |
| icon | `resist` |
| Резерв маны · `reserve` | 25 |
| Характеристики · `stats` | +10–25% к всем сопротивлениям стихиям · [Все сопротивления стихиям](../reference/Stats/Stats-HERO.md#stock_resist_all) |


## FAITH <a href="#faith" id="faith"></a>

### Вера <a href="#вера" id="вера"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 5 |
| icon | `light` |
| Характеристики · `stats` | 3–10% увеличение максимума здоровья · [Здоровье](../reference/Stats/Stats-HERO.md#stock_health); 5–15% увеличение максимума маны · [Мана](../reference/Stats/Stats-HERO.md#stock_mana) |


## BLESSING <a href="#blessing" id="blessing"></a>

### Благословение <a href="#благословение" id="благословение"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 10 |
| icon | [armour](../reference/Tables/Tables-MODIFIER.md#armour) |
| Срабатывание · `trigger` | Событие: `HEALED`; Усиление: Длительность: 4; Характеристики: 20–50% увеличение брони · [Броня](../reference/Stats/Stats-HERO.md#stock_armor) |


## WRATH <a href="#wrath" id="wrath"></a>

### Гнев небес <a href="#гнев-небес" id="гнев-небес"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 16 |
| icon | `lightning` |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | +5–132 к урону от молнии · [Урон молнией](../reference/Stats/Stats-HERO.md#stock_attack_lightning) |


## HOLY_ARMS <a href="#holy_arms" id="holy_arms"></a>

### Святое оружие <a href="#святое-оружие" id="святое-оружие"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 24 |
| icon | `magical` |
| Характеристики · `stats` | 10–30% увеличение урона от огня · [Урон огнём](../reference/Stats/Stats-HERO.md#stock_attack_fire); 10–30% увеличение урона от холода · [Урон холодом](../reference/Stats/Stats-HERO.md#stock_attack_cold); 10–30% увеличение урона от молнии · [Урон молнией](../reference/Stats/Stats-HERO.md#stock_attack_lightning) |


## RIGHTEOUSNESS <a href="#righteousness" id="righteousness"></a>

### Праведность <a href="#праведность" id="праведность"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 32 |
| icon | `health` |
| Срабатывание · `trigger` | Событие: `KILL`; Лечение: life: 1; 4; Мана: уровень 1 → 20: 1; 3 |


## FORTITUDE <a href="#fortitude" id="fortitude"></a>

### Стойкость <a href="#стойкость" id="стойкость"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 42 |
| icon | `constitution` |
| Характеристики · `stats` | 10–30% увеличение брони · [Броня](../reference/Stats/Stats-HERO.md#stock_armor); 10–30% увеличение порога оглушения · [Порог оглушения](../reference/Stats/Stats-HERO.md#stock_stun_threshold) |


## MARTYR <a href="#martyr" id="martyr"></a>

### Мученик <a href="#мученик" id="мученик"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Храмовник (Templar)](../Classes.md#templar) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 52 |
| icon | `regen` |
| Срабатывание · `trigger` | Событие: `LOW_LIFE`; Перезарядка, с: 25; Усиление: Длительность: 4; Характеристики: Восстанавливает 2–5% здоровья в секунду · [Регенерация здоровья, %](../reference/Stats/Stats-HERO.md#stock_life_regen_percent) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/skills.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
