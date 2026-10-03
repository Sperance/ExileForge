# SCION

| Название | Код |
| --- | --- |
| [Стихийный удар](#elemental_hit) | ELEMENTAL_HIT |
| [Спектральный бросок](#spectral_throw) | SPECTRAL_THROW |
| [Дуга](#arc) | ARC |
| [Клич единства](#rallying_cry) | RALLYING_CRY |
| [Путы времени](#temporal_chains) | TEMPORAL_CHAINS |
| [Возрождение](#renewal) | RENEWAL |
| [Гармония](#harmony) | HARMONY |
| [Наследие](#legacy) | LEGACY |
| [Отзвук](#echo) | ECHO |
| [Резонанс](#resonance) | RESONANCE |
| [Универсал](#adept) | ADEPT |
| [Эхо фляг](#flask_echo) | FLASK_ECHO |
| [Жажда силы](#hunger_for_power) | HUNGER_FOR_POWER |
| [Воля к жизни](#will_to_live) | WILL_TO_LIVE |


## ELEMENTAL_HIT <a href="#elemental_hit" id="elemental_hit"></a>

### Стихийный удар <a href="#стихийный-удар" id="стихийный-удар"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 1 |
| icon | `orb_facet` |
| Мана: уровень 1 → 20 · `mana` | 7; 22 |
| Перезарядка, с · `cooldown` | 3 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 1; Урон оружия: 127; 212; Тип урона: `RANDOM`; Конверсия: 100; 100; Состояния: Состояние: `ELEMENT`; Шанс (единица по правилам подсистемы): 25; 40 |


## SPECTRAL_THROW <a href="#spectral_throw" id="spectral_throw"></a>

### Спектральный бросок <a href="#спектральный-бросок" id="спектральный-бросок"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `ATTACK` |
| Уровень открытия · `unlock` | 6 |
| icon | `throwing` |
| Мана: уровень 1 → 20 · `mana` | 9; 26 |
| Перезарядка, с · `cooldown` | 3.5 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `READY` |
| Удар · `hit` | Число целей: 2; Урон оружия: 93; 161 |


## ARC <a href="#arc" id="arc"></a>

### Дуга <a href="#дуга" id="дуга"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `SPELL` |
| Уровень открытия · `unlock` | 12 |
| icon | `lightning` |
| Мана: уровень 1 → 20 · `mana` | 12; 32 |
| Перезарядка, с · `cooldown` | 4 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 50; 25 |
| Условие применения · `condition` | `ENEMIES_3` |
| Удар · `hit` | Число целей: 4; Заклинание: Тип урона: `LIGHTNING`; Минимум: 16.1; 145.7; Максимум: 47.3; 437.2 |


## RALLYING_CRY <a href="#rallying_cry" id="rallying_cry"></a>

### Клич единства <a href="#клич-единства" id="клич-единства"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `WARCRY` |
| Уровень открытия · `unlock` | 20 |
| icon | `combat` |
| Мана: уровень 1 → 20 · `mana` | 14; 34 |
| Перезарядка, с · `cooldown` | 13 |
| Условие применения · `condition` | `FIGHT_START` |
| Усиление · `buff` | Длительность: 6; Характеристики: 10–25% увеличение урона · [Урон](../reference/Stats/Stats-HERO.md#stock_damage); +10–20% к всем сопротивлениям стихиям · [Все сопротивления стихиям](../reference/Stats/Stats-HERO.md#stock_resist_all) |


## TEMPORAL_CHAINS <a href="#temporal_chains" id="temporal_chains"></a>

### Путы времени <a href="#путы-времени" id="путы-времени"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `CURSE` |
| Уровень открытия · `unlock` | 30 |
| icon | `dark` |
| Мана: уровень 1 → 20 · `mana` | 16; 38 |
| Перезарядка, с · `cooldown` | 15 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 30; 10 |
| Условие применения · `condition` | `RARE_OR_BOSS` |
| Проклятие · `curse` | Длительность: 8; Число целей: 0; Характеристики: Скорость атаки · MORE · -15–-35 · [Скорость атаки](../reference/Stats/Stats-HERO.md#stock_attack_speed) |


## RENEWAL <a href="#renewal" id="renewal"></a>

### Возрождение <a href="#возрождение" id="возрождение"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `HEAL` |
| Уровень открытия · `unlock` | 40 |
| icon | `regen` |
| Мана: уровень 1 → 20 · `mana` | 18; 42 |
| Перезарядка, с · `cooldown` | 20 |
| Начальная подготовка: уровень 1 → 20 · `prepare` | 40; 15 |
| Условие применения · `condition` | `LIFE_50` |
| Лечение · `heal` | life: 15; 35; Мана: уровень 1 → 20: 10; 20 |


## HARMONY <a href="#harmony" id="harmony"></a>

### Гармония <a href="#гармония" id="гармония"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 1 |
| icon | `orb` |
| Резерв маны · `reserve` | 25 |
| Характеристики · `stats` | +10–40 к силе · [Сила](../reference/Stats/Stats-HERO.md#stock_strength); +10–40 к ловкости · [Ловкость](../reference/Stats/Stats-HERO.md#stock_agility); +10–40 к интеллекту · [Интеллект](../reference/Stats/Stats-HERO.md#stock_intellect) |


## LEGACY <a href="#legacy" id="legacy"></a>

### Наследие <a href="#наследие" id="наследие"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 5 |
| icon | `resist` |
| Характеристики · `stats` | +5–15% к всем сопротивлениям стихиям · [Все сопротивления стихиям](../reference/Stats/Stats-HERO.md#stock_resist_all) |


## ECHO <a href="#echo" id="echo"></a>

### Отзвук <a href="#отзвук" id="отзвук"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 10 |
| icon | `mirror` |
| Срабатывание · `trigger` | Событие: `SKILL_USE`; Шанс (единица по правилам подсистемы): 10; 20; refund: Да |


## RESONANCE <a href="#resonance" id="resonance"></a>

### Резонанс <a href="#резонанс" id="резонанс"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `AURA` |
| Уровень открытия · `unlock` | 16 |
| icon | `magical` |
| Резерв маны · `reserve` | 30 |
| Характеристики · `stats` | 8–25% увеличение урона · [Урон](../reference/Stats/Stats-HERO.md#stock_damage) |


## ADEPT <a href="#adept" id="adept"></a>

### Универсал <a href="#универсал" id="универсал"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 24 |
| icon | `speed` |
| Характеристики · `stats` | 8–20% увеличение скорости перезарядки умений · [Перезарядка умений](../reference/Stats/Stats-HERO.md#stock_cooldown_recovery) |


## FLASK_ECHO <a href="#flask_echo" id="flask_echo"></a>

### Эхо фляг <a href="#эхо-фляг" id="эхо-фляг"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 32 |
| icon | `potion` |
| Срабатывание · `trigger` | Событие: `KILL`; Шанс (единица по правилам подсистемы): 15; 30; flaskCharges: 1 |


## HUNGER_FOR_POWER <a href="#hunger_for_power" id="hunger_for_power"></a>

### Жажда силы <a href="#жажда-силы" id="жажда-силы"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `BONUS` |
| Уровень открытия · `unlock` | 42 |
| icon | `leech` |
| Характеристики · `stats` | +2–8 здоровья за убийство · [Здоровье за убийство](../reference/Stats/Stats-HERO.md#stock_health_on_kill); +1–4 маны за убийство · [Мана за убийство](../reference/Stats/Stats-HERO.md#stock_mana_on_kill) |


## WILL_TO_LIVE <a href="#will_to_live" id="will_to_live"></a>

### Воля к жизни <a href="#воля-к-жизни" id="воля-к-жизни"></a>


| Параметр | Значение |
| --- | --- |
| Класс · `heroClass` | [Скион (Scion)](../Classes.md#scion) |
| Тип · `type` | `TRIGGER` |
| Уровень открытия · `unlock` | 52 |
| icon | `health` |
| Срабатывание · `trigger` | Событие: `LOW_LIFE`; Перезарядка, с: 25; Усиление: Длительность: 4; Характеристики: 50–150% увеличение скорости восстановления здоровья и щита · [Скорость восстановления](../reference/Stats/Stats-HERO.md#stock_recovery_rate) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/skills.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
