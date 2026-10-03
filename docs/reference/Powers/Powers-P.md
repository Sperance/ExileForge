# Особые силы · P

## POWER_PREDATOR_SENSE <a href="#power_predator_sense" id="power_predator_sense"></a>

### Чутьё хищника <a href="#чутьё-хищника" id="чутьё-хищника"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса крита по врагам, у которых меньше 35% здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Чутьё хищника](../Stats/Stats-POWER.md#power_predator_sense) |
| Событие · `on` | `STANDING` |
| checks | check: `TARGET_LIFE_BELOW`; Значение: 35 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED` |


## POWER_PHOENIX_REBIRTH <a href="#power_phoenix_rebirth" id="power_phoenix_rebirth"></a>

### Возрождение феникса <a href="#возрождение-феникса" id="возрождение-феникса"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Раз за бой вместо смерти вы встаёте с {v}% максимума здоровья и 1 с неуязвимости |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Возрождение феникса](../Stats/Stats-POWER.md#power_phoenix_rebirth) |
| Событие · `on` | `DEATH` |
| Перезарядка, с · `cooldown` | -1 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE`; Действие: `INVULNERABLE`; Длительность: 1 |


## POWER_PACK_TACTICS <a href="#power_pack_tactics" id="power_pack_tactics"></a>

### Тактика стаи <a href="#тактика-стаи" id="тактика-стаи"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения скорости атаки за каждого врага, до 5 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тактика стаи](../Stats/Stats-POWER.md#power_pack_tactics) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; scale: `FOES`; cap: 5 |


## POWER_PHASE_STEP <a href="#power_phase_step" id="power_phase_step"></a>

### Фазовый шаг <a href="#фазовый-шаг" id="фазовый-шаг"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При получении удара {v}% шанс стать неуязвимым на 1 с, не чаще раза в 5 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Фазовый шаг](../Stats/Stats-POWER.md#power_phase_step) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Перезарядка, с · `cooldown` | 5 |
| Эффекты · `effects` | Действие: `INVULNERABLE`; Длительность: 1 |


## POWER_PHOENIX_FLARE <a href="#power_phoenix_flare" id="power_phoenix_flare"></a>

### Вспышка феникса <a href="#вспышка-феникса" id="вспышка-феникса"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 5 с феникс вспыхивает и наносит всем врагам урон огнём в {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вспышка феникса](../Stats/Stats-POWER.md#power_phoenix_flare) |
| Событие · `on` | `EVERY` |
| every | 5 |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `LIFE`; Тип: `FIRE`; to: `ALL` |


## POWER_PHANTOM_STEP <a href="#power_phantom_step" id="power_phantom_step"></a>

### Призрачный шаг <a href="#призрачный-шаг" id="призрачный-шаг"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение с шансом {v}% задерживает следующий удар нападавшего на 0.4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Призрачный шаг](../Stats/Stats-POWER.md#power_phantom_step) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `DELAY`; Длительность: 0.4 |


## POWER_POLISHED_FACE <a href="#power_polished_face" id="power_polished_face"></a>

### Зеркальная гладь <a href="#зеркальная-гладь" id="зеркальная-гладь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок даёт +{v}% к отражению урона на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Зеркальная гладь](../Stats/Stats-POWER.md#power_polished_face) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Отражение](../Stats/Stats-HERO.md#stock_reflect); Длительность: 4; stacks: 3 |


## POWER_PRISM_SPLIT <a href="#power_prism_split" id="power_prism_split"></a>

### Расщепление света <a href="#расщепление-света" id="расщепление-света"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок расщепляет свет и бьёт всех врагов на {v}% урона оружия молнией |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Расщепление света](../Stats/Stats-POWER.md#power_prism_split) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `LIGHTNING`; to: `ALL` |


## POWER_PAID_PASSAGE <a href="#power_paid_passage" id="power_paid_passage"></a>

### Оплаченный путь <a href="#оплаченный-путь" id="оплаченный-путь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство врага восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Оплаченный путь](../Stats/Stats-POWER.md#power_paid_passage) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_PICK_AND_HAUL <a href="#power_pick_and_haul" id="power_pick_and_haul"></a>

### Кайло и тачка <a href="#кайло-и-тачка" id="кайло-и-тачка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 4-й удар с шансом {v}% оглушает врага на 0,8 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кайло и тачка](../Stats/Stats-POWER.md#power_pick_and_haul) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 4 |
| Эффекты · `effects` | Действие: `STUN`; Длительность: 0.8 |


## POWER_POUNCE <a href="#power_pounce" id="power_pounce"></a>

### Прыжок <a href="#прыжок" id="прыжок"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар с шансом {v}% делает следующую атаку мгновенной |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Прыжок](../Stats/Stats-POWER.md#power_pounce) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `RUSH` |


## POWER_PRAYING_STRIKE <a href="#power_praying_strike" id="power_praying_strike"></a>

### Молитвенный удар <a href="#молитвенный-удар" id="молитвенный-удар"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по врагам ниже 30% здоровья бьют снова на {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Молитвенный удар](../Stats/Stats-POWER.md#power_praying_strike) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_LIFE_BELOW`; Значение: 30 |
| Эффекты · `effects` | Действие: `DAMAGE` |


## POWER_PRINCES_APPETITE <a href="#power_princes_appetite" id="power_princes_appetite"></a>

### Аппетит принца <a href="#аппетит-принца" id="аппетит-принца"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство врага даёт +{v}% множителя крита на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Аппетит принца](../Stats/Stats-POWER.md#power_princes_appetite) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Множитель критического удара](../Stats/Stats-HERO.md#stock_critical_multiplier); Длительность: 4; stacks: 3 |


## POWER_PRISM_VEIL <a href="#power_prism_veil" id="power_prism_veil"></a>

### Призменная завеса <a href="#призменная-завеса" id="призменная-завеса"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Призменная завеса](../Stats/Stats-POWER.md#power_prism_veil) |
| Событие · `on` | `STANDING` |
| checks | check: `SHIELD_FULL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Получаемый урон стихиями](../Stats/Stats-HERO.md#stock_elemental_taken) |


## POWER_PYRE_WARD <a href="#power_pyre_ward" id="power_pyre_ward"></a>

### Заслон костра <a href="#заслон-костра" id="заслон-костра"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | На низком здоровье - заслон в {v}% максимума здоровья на 3 с, не чаще раза в 15 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Заслон костра](../Stats/Stats-POWER.md#power_pyre_ward) |
| Событие · `on` | `LOW_LIFE` |
| Перезарядка, с · `cooldown` | 15 |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_PENANCE <a href="#power_penance" id="power_penance"></a>

### Епитимья <a href="#епитимья" id="епитимья"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения урона чарами по проклятым врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Епитимья](../Stats/Stats-POWER.md#power_penance) |
| Событие · `on` | `STANDING` |
| checks | check: `TARGET_CURSED` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон чар](../Stats/Stats-HERO.md#stock_spell_damage); Операция: `INCREASED` |


## POWER_PRIZE_OF_THE_ROAD <a href="#power_prize_of_the_road" id="power_prize_of_the_road"></a>

### Добыча дороги <a href="#добыча-дороги" id="добыча-дороги"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения редкости предметов с редких и уникальных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Добыча дороги](../Stats/Stats-POWER.md#power_prize_of_the_road) |
| world | gain: `RARITY`; against: `RARE`; `UNIQUE` |


## POWER_POWER_ON_KILL <a href="#power_power_on_kill" id="power_power_on_kill"></a>

### Убийство силы <a href="#убийство-силы" id="убийство-силы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс получить заряд силы при убийстве |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Убийство силы](../Stats/Stats-POWER.md#power_power_on_kill) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `CHARGE`; charge: `POWER` |


## POWER_POWER_ON_CRIT <a href="#power_power_on_crit" id="power_power_on_crit"></a>

### Сила крита <a href="#сила-крита" id="сила-крита"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс получить заряд силы при критическом ударе |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сила крита](../Stats/Stats-POWER.md#power_power_on_crit) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `CHARGE`; charge: `POWER` |


## POWER_PROTOTYPE_BULWARK <a href="#power_prototype_bulwark" id="power_prototype_bulwark"></a>

### Опытный оплот <a href="#опытный-оплот" id="опытный-оплот"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Стихийный урон по вам снижается наивысшим из ваших стихийных сопротивлений |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Опытный оплот](../Stats/Stats-POWER.md#power_prototype_bulwark) |
| sheet | Операция: `ADD`; to: [Защита наивысшим сопротивлением](../Stats/Stats-HERO.md#stock_highest_resist_taken) |


## POWER_PROTOTYPE_PLATING <a href="#power_prototype_plating" id="power_prototype_plating"></a>

### Обшивка Ваал <a href="#обшивка-ваал" id="обшивка-ваал"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% больше брони за каждый надетый осквернённый предмет |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Обшивка Ваал](../Stats/Stats-POWER.md#power_prototype_plating) |
| sheet | Операция: `MORE`; Предшествующие зоны: [Надетые осквернённые предметы](../Stats/Stats-HERO.md#stock_worn_corrupted); to: [Броня](../Stats/Stats-HERO.md#stock_armor) |


## POWER_PLAGUE_TRAIL_HOARD <a href="#power_plague_trail_hoard" id="power_plague_trail_hoard"></a>

### Самоцветный след <a href="#самоцветный-след" id="самоцветный-след"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к урону за каждый надетый самоцвет |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Самоцветный след](../Stats/Stats-POWER.md#power_plague_trail_hoard) |
| sheet | Операция: `PER`; Предшествующие зоны: [Надетые самоцветы](../Stats/Stats-HERO.md#stock_worn_jewels); to: [Урон](../Stats/Stats-HERO.md#stock_damage) |


## POWER_PLAGUE_TRAIL_BURST <a href="#power_plague_trail_burst" id="power_plague_trail_burst"></a>

### Зараза <a href="#зараза" id="зараза"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | С шансом {v}% убитый враг взрывается: 10% его макс. здоровья физическим уроном по остальным |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Зараза](../Stats/Stats-POWER.md#power_plague_trail_burst) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `PHYSICAL`; of: `TARGET_LIFE`; to: `OTHERS`; Количество: 10 |


## POWER_PRISM_REFRACTION <a href="#power_prism_refraction" id="power_prism_refraction"></a>

### Преломление <a href="#преломление" id="преломление"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 3-й удар добавляет преломлённый луч: {v}% урона оружия случайной стихией |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Преломление](../Stats/Stats-POWER.md#power_prism_refraction) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 3 |
| Эффекты · `effects` | Действие: `ONE_OF`; Варианты: Действие: `DAMAGE`; Тип: `FIRE`; Действие: `DAMAGE`; Тип: `COLD`; Действие: `DAMAGE`; Тип: `LIGHTNING` |


## POWER_PRISM_HARMONY <a href="#power_prism_harmony" id="power_prism_harmony"></a>

### Гармония <a href="#гармония" id="гармония"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Наложение недуга даёт {v}% к урону на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Гармония](../Stats/Stats-POWER.md#power_prism_harmony) |
| Событие · `on` | `INFLICT` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `INCREASED`; Длительность: 4; stacks: 3 |


## POWER_PACK_RALLY <a href="#power_pack_rally" id="power_pack_rally"></a>

### Сбор стаи <a href="#сбор-стаи" id="сбор-стаи"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удар питомца даёт вам {v}% увеличения урона на 3 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сбор стаи](../Stats/Stats-POWER.md#power_pack_rally) |
| Событие · `on` | `PET_HIT` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 3; stacks: 5 |


## POWER_PACK_BOND <a href="#power_pack_bond" id="power_pack_bond"></a>

### Узы стаи <a href="#узы-стаи" id="узы-стаи"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Ваши убийства лечат питомца на {v}% его здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Узы стаи](../Stats/Stats-POWER.md#power_pack_bond) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `HEAL`; of: `PET_LIFE`; to: `PET` |


## POWER_PLAGUE_TOUCH <a href="#power_plague_touch" id="power_plague_touch"></a>

### Касание чумы <a href="#касание-чумы" id="касание-чумы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по отравленным с шансом {v}% разносят яд |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Касание чумы](../Stats/Stats-POWER.md#power_plague_touch) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_AILMENT`; word: `POISON` |
| Эффекты · `effects` | Действие: `SPREAD` |


## POWER_PRISM_CLEAVE <a href="#power_prism_cleave" id="power_prism_cleave"></a>

### Призменный разруб <a href="#призменный-разруб" id="призменный-разруб"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Криты наносят {v}% урона оружия холодом другому врагу |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Призменный разруб](../Stats/Stats-POWER.md#power_prism_cleave) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `COLD`; to: `RANDOM` |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
