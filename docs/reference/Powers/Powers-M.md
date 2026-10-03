# Особые силы · M

## POWER_MIRROR_GUARD <a href="#power_mirror_guard" id="power_mirror_guard"></a>

### Зеркальная защита <a href="#зеркальная-защита" id="зеркальная-защита"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Отражение урона, равное {v}% шанса блока |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Зеркальная защита](../Stats/Stats-POWER.md#power_mirror_guard) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Шанс блока](../Stats/Stats-HERO.md#stock_block_chance); to: [Отражение](../Stats/Stats-HERO.md#stock_reflect) |


## POWER_MAW_HUNGER <a href="#power_maw_hunger" id="power_maw_hunger"></a>

### Голод пасти <a href="#голод-пасти" id="голод-пасти"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт +{v}% урона на 5 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Голод пасти](../Stats/Stats-POWER.md#power_maw_hunger) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 5; stacks: 5 |


## POWER_MAGNATE_TAX <a href="#power_magnate_tax" id="power_magnate_tax"></a>

### Налог магната <a href="#налог-магната" id="налог-магната"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения редкости предметов за каждые 20 силы |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Налог магната](../Stats/Stats-POWER.md#power_magnate_tax) |
| sheet | Операция: `PER`; Предшествующие зоны: [Сила](../Stats/Stats-HERO.md#stock_strength); to: [Редкость добычи](../Stats/Stats-HERO.md#stock_rarity); per: 20 |


## POWER_MAROHI_CRUSH <a href="#power_marohi_crush" id="power_marohi_crush"></a>

### Сокрушение Марохи <a href="#сокрушение-марохи" id="сокрушение-марохи"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% больше физического урона, на 15% меньше скорость атаки |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сокрушение Марохи](../Stats/Stats-POWER.md#power_marohi_crush) |
| sheet | Операция: `MORE`; to: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); Операция: `MORE`; to: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Значение: -15 |


## POWER_MIRE_BLOOD <a href="#power_mire_blood" id="power_mire_blood"></a>

### Болотная кровь <a href="#болотная-кровь" id="болотная-кровь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Отравление восстанавливает {v}% максимума здоровья, не чаще раза в 2 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Болотная кровь](../Stats/Stats-POWER.md#power_mire_blood) |
| Событие · `on` | `AILED` |
| checks | check: `AILMENT`; word: `POISON` |
| Перезарядка, с · `cooldown` | 2 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_MOUNTAIN_BREAK <a href="#power_mountain_break" id="power_mountain_break"></a>

### Горолом <a href="#горолом" id="горолом"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по врагам, у которых больше 90% здоровья, бьют ещё раз на {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Горолом](../Stats/Stats-POWER.md#power_mountain_break) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_LIFE_ABOVE`; Значение: 90 |
| Эффекты · `effects` | Действие: `DAMAGE` |


## POWER_MOLTEN_CORE <a href="#power_molten_core" id="power_molten_core"></a>

### Расплавленное ядро <a href="#расплавленное-ядро" id="расплавленное-ядро"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок с шансом {v}% поджигает нападавшего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Расплавленное ядро](../Stats/Stats-POWER.md#power_molten_core) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `IGNITE` |


## POWER_MIRROR_RIPOSTE <a href="#power_mirror_riposte" id="power_mirror_riposte"></a>

### Зеркальный ответ <a href="#зеркальный-ответ" id="зеркальный-ответ"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия уклонение бьёт нападавшего на {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Зеркальный ответ](../Stats/Stats-POWER.md#power_mirror_riposte) |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `DAMAGE` |


## POWER_METEOR <a href="#power_meteor" id="power_meteor"></a>

### Метеор <a href="#метеор" id="метеор"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 4 с метеор бьёт всех врагов на {v}% урона оружия огнём |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Метеор](../Stats/Stats-POWER.md#power_meteor) |
| Событие · `on` | `EVERY` |
| every | 4 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `FIRE`; to: `ALL` |


## POWER_MAW_FEAST <a href="#power_maw_feast" id="power_maw_feast"></a>

### Пир пасти <a href="#пир-пасти" id="пир-пасти"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитые враги кормят пасть: {v}% их максимума здоровья хаосом случайному врагу |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пир пасти](../Stats/Stats-POWER.md#power_maw_feast) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TARGET_LIFE`; Тип: `CHAOS`; to: `RANDOM` |


## POWER_MARKED_PREY <a href="#power_marked_prey" id="power_marked_prey"></a>

### Меченая добыча <a href="#меченая-добыча" id="меченая-добыча"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 4-й удар метит врага: 4 с он получает на {v}% больше урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Меченая добыча](../Stats/Stats-POWER.md#power_marked_prey) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 4 |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken); Длительность: 4 |


## POWER_MOURNING <a href="#power_mourning" id="power_mourning"></a>

### Траур <a href="#траур" id="траур"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Траур](../Stats/Stats-POWER.md#power_mourning) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 3; to: `ALL` |


## POWER_MATRIARCHS_VENOM <a href="#power_matriarchs_venom" id="power_matriarchs_venom"></a>

### Яд матриарха <a href="#яд-матриарха" id="яд-матриарха"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | С шансом {v}% при ударе по вам отравляет атакующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Яд матриарха](../Stats/Stats-POWER.md#power_matriarchs_venom) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `POISON` |


## POWER_MURMURED_LITANY <a href="#power_murmured_litany" id="power_murmured_litany"></a>

### Бормотание литании <a href="#бормотание-литании" id="бормотание-литании"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждое заклинание сокращает перезарядку ваших умений на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бормотание литании](../Stats/Stats-POWER.md#power_murmured_litany) |
| Событие · `on` | `SKILL_USE` |
| checks | check: `SPELL` |
| Эффекты · `effects` | Действие: `COOLDOWNS` |


## POWER_MOLTEN_ECHO <a href="#power_molten_echo" id="power_molten_echo"></a>

### Расплавленное эхо <a href="#расплавленное-эхо" id="расплавленное-эхо"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по горящим врагам отзываются через 1 с на {v}% урона огнём |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Расплавленное эхо](../Stats/Stats-POWER.md#power_molten_echo) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: [Отзвук](../../Skills/Skills-SCION.md#echo); Длительность: 1; Тип: `FIRE` |


## POWER_MARKED_ROUTES <a href="#power_marked_routes" id="power_marked_routes"></a>

### Отмеченные пути <a href="#отмеченные-пути" id="отмеченные-пути"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса найти карты |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отмеченные пути](../Stats/Stats-POWER.md#power_marked_routes) |
| world | gain: [Map](../../Equipment/Equipment-MAP.md#map) |


## POWER_MIRAGE_DOUBLE <a href="#power_mirage_double" id="power_mirage_double"></a>

### Двойной образ <a href="#двойной-образ" id="двойной-образ"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% больше урона атак |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Двойной образ](../Stats/Stats-POWER.md#power_mirage_double) |
| sheet | Операция: `MORE`; to: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); Операция: `MORE`; to: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire); Операция: `MORE`; to: [Урон холодом](../Stats/Stats-HERO.md#stock_attack_cold); Операция: `MORE`; to: [Урон молнией](../Stats/Stats-HERO.md#stock_attack_lightning); Операция: `MORE`; to: [Урон хаосом](../Stats/Stats-HERO.md#stock_attack_chaos) |


## POWER_MIRAGE_DRAG <a href="#power_mirage_drag" id="power_mirage_drag"></a>

### Марево <a href="#марево" id="марево"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Марево](../Stats/Stats-POWER.md#power_mirage_drag) |
| sheet | Операция: `MORE`; to: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed) |


## POWER_MAIDEN_TIDE <a href="#power_maiden_tide" id="power_maiden_tide"></a>

### Жемчужный прилив <a href="#жемчужный-прилив" id="жемчужный-прилив"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% максимума маны добавляется к ЭЩ |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жемчужный прилив](../Stats/Stats-POWER.md#power_maiden_tide) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Мана](../Stats/Stats-HERO.md#stock_mana); to: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield) |


## POWER_MAIDEN_WHIM <a href="#power_maiden_whim" id="power_maiden_whim"></a>

### Прихоть <a href="#прихоть" id="прихоть"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Умение с шансом {v}% сокращает остальные откаты на 0,5 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Прихоть](../Stats/Stats-POWER.md#power_maiden_whim) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `SKILL_USE` |
| Эффекты · `effects` | Действие: `COOLDOWNS`; Количество: 0.5 |


## POWER_MARKSMANS_FOCUS <a href="#power_marksmans_focus" id="power_marksmans_focus"></a>

### Сосредоточение стрелка <a href="#сосредоточение-стрелка" id="сосредоточение-стрелка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличение меткости в первые 6 с боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сосредоточение стрелка](../Stats/Stats-POWER.md#power_marksmans_focus) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Меткость](../Stats/Stats-HERO.md#stock_accuracy); Операция: `INCREASED`; Длительность: 6 |


## POWER_MOURNING_BELL <a href="#power_mourning_bell" id="power_mourning_bell"></a>

### Погребальный колокол <a href="#погребальный-колокол" id="погребальный-колокол"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по врагам ниже 30% здоровья наносят на {v}% больше урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Погребальный колокол](../Stats/Stats-POWER.md#power_mourning_bell) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_LIFE_BELOW`; Значение: 30 |
| Эффекты · `effects` | Действие: `DAMAGE` |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
