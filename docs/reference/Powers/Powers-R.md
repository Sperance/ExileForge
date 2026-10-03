# Особые силы · R

## POWER_ROT_SPELL <a href="#power_rot_spell" id="power_rot_spell"></a>

### Гнилое заклятие <a href="#гнилое-заклятие" id="гнилое-заклятие"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Заклинание с шансом {v}% отравляет всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Гнилое заклятие](../Stats/Stats-POWER.md#power_rot_spell) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `SKILL_USE` |
| checks | check: `SPELL` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `POISON`; to: `ALL` |


## POWER_RIVET <a href="#power_rivet" id="power_rivet"></a>

### Заклёпка <a href="#заклёпка" id="заклёпка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 2-й удар оглушает на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Заклёпка](../Stats/Stats-POWER.md#power_rivet) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 2 |
| Эффекты · `effects` | Действие: `STUN` |


## POWER_ROOT_DRINK <a href="#power_root_drink" id="power_root_drink"></a>

### Корневое питьё <a href="#корневое-питьё" id="корневое-питьё"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Регенерация здоровья в секунду, равная {v}% сопротивления хаосу |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Корневое питьё](../Stats/Stats-POWER.md#power_root_drink) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Сопротивление хаосу](../Stats/Stats-HERO.md#stock_resist_chaos); to: [Восстановление здоровья](../Stats/Stats-HERO.md#stock_health_regen) |


## POWER_RIME_BLOCK <a href="#power_rime_block" id="power_rime_block"></a>

### Инеистый блок <a href="#инеистый-блок" id="инеистый-блок"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок с шансом {v}% замораживает нападавшего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Инеистый блок](../Stats/Stats-POWER.md#power_rime_block) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `FREEZE` |


## POWER_REMORSEFUL_GUARD <a href="#power_remorseful_guard" id="power_remorseful_guard"></a>

### Покаянная стойка <a href="#покаянная-стойка" id="покаянная-стойка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Покаянная стойка](../Stats/Stats-POWER.md#power_remorseful_guard) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_REARGUARD <a href="#power_rearguard" id="power_rearguard"></a>

### Арьергард <a href="#арьергард" id="арьергард"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок задерживает следующий удар нападавшего на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Арьергард](../Stats/Stats-POWER.md#power_rearguard) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `DELAY` |


## POWER_ROYAL_VENOM <a href="#power_royal_venom" id="power_royal_venom"></a>

### Царский яд <a href="#царский-яд" id="царский-яд"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Царский яд](../Stats/Stats-POWER.md#power_royal_venom) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `POISON` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 4 |


## POWER_REFRACTION <a href="#power_refraction" id="power_refraction"></a>

### Преломление <a href="#преломление" id="преломление"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Преломление](../Stats/Stats-POWER.md#power_refraction) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all); Длительность: 4 |


## POWER_RALLYING_CRY <a href="#power_rallying_cry" id="power_rallying_cry"></a>

### Ободряющий клич <a href="#ободряющий-клич" id="ободряющий-клич"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 8 с боевой клич даёт +{v}% урона на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ободряющий клич](../Stats/Stats-POWER.md#power_rallying_cry) |
| Событие · `on` | `EVERY` |
| every | 8 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 3 |


## POWER_ROYAL_PYRE <a href="#power_royal_pyre" id="power_royal_pyre"></a>

### Королевский костёр <a href="#королевский-костёр" id="королевский-костёр"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство горящего врага даёт +{v}% урона на 5 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Королевский костёр](../Stats/Stats-POWER.md#power_royal_pyre) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 5; stacks: 3 |


## POWER_ROT_BLOOM <a href="#power_rot_bloom" id="power_rot_bloom"></a>

### Гнилой цвет <a href="#гнилой-цвет" id="гнилой-цвет"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитый отравленный враг лопается: {v}% его максимума здоровья хаосом остальным врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Гнилой цвет](../Stats/Stats-POWER.md#power_rot_bloom) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `POISON` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TARGET_LIFE`; Тип: `CHAOS`; to: `OTHERS` |


## POWER_ROOTED_IN_ROT <a href="#power_rooted_in_rot" id="power_rooted_in_rot"></a>

### Корни гнили <a href="#корни-гнили" id="корни-гнили"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Отравление с шансом {v}% снимает с вас все состояния |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Корни гнили](../Stats/Stats-POWER.md#power_rooted_in_rot) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `AILED` |
| checks | check: `AILMENT`; word: `POISON` |
| Эффекты · `effects` | Действие: `CLEANSE` |


## POWER_RITUAL_CLEAVE <a href="#power_ritual_cleave" id="power_ritual_cleave"></a>

### Ритуальный разруб <a href="#ритуальный-разруб" id="ритуальный-разруб"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 3-й удар рассекает всех остальных врагов на {v}% урона оружия хаосом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ритуальный разруб](../Stats/Stats-POWER.md#power_ritual_cleave) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 3 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `CHAOS`; to: `OTHERS` |


## POWER_ROAD_PROVISIONS <a href="#power_road_provisions" id="power_road_provisions"></a>

### Дорожный запас <a href="#дорожный-запас" id="дорожный-запас"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Начало боя даёт {v} зарядов всем флягам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Дорожный запас](../Stats/Stats-POWER.md#power_road_provisions) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `CHARGES` |


## POWER_REFRACTED_EDGE <a href="#power_refracted_edge" id="power_refracted_edge"></a>

### Преломлённая грань <a href="#преломлённая-грань" id="преломлённая-грань"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Наложенный недуг даёт +{v}% проникания стихий на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Преломлённая грань](../Stats/Stats-POWER.md#power_refracted_edge) |
| Событие · `on` | `INFLICT` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Пробивание стихий](../Stats/Stats-HERO.md#stock_penetrate_elemental); Длительность: 4; stacks: 3 |


## POWER_ROYAL_JELLY <a href="#power_royal_jelly" id="power_royal_jelly"></a>

### Маточное молочко <a href="#маточное-молочко" id="маточное-молочко"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Отравление врага восстанавливает {v}% максимума здоровья, не чаще раза в секунду |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Маточное молочко](../Stats/Stats-POWER.md#power_royal_jelly) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `POISON` |
| Перезарядка, с · `cooldown` | 1 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_RALLY_THE_LINE <a href="#power_rally_the_line" id="power_rally_the_line"></a>

### Сомкнуть строй <a href="#сомкнуть-строй" id="сомкнуть-строй"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Пройденный этап поднимает заслон в {v}% максимума здоровья на первые 5 с следующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сомкнуть строй](../Stats/Stats-POWER.md#power_rally_the_line) |
| Событие · `on` | `STAGE_CLEAR` |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 5 |


## POWER_RISING_HEAT <a href="#power_rising_heat" id="power_rising_heat"></a>

### Восходящий жар <a href="#восходящий-жар" id="восходящий-жар"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Пройденный этап восстанавливает {v}% максимума здоровья в начале следующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Восходящий жар](../Stats/Stats-POWER.md#power_rising_heat) |
| Событие · `on` | `STAGE_CLEAR` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_RANDOM_CHARGE_ON_KILL <a href="#power_random_charge_on_kill" id="power_random_charge_on_kill"></a>

### Дар глубин <a href="#дар-глубин" id="дар-глубин"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс при убийстве получить случайный заряд: ярости, силы или выносливости |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Дар глубин](../Stats/Stats-POWER.md#power_random_charge_on_kill) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `CHARGE`; charge: `RANDOM` |


## POWER_RIME_DISCHARGE <a href="#power_rime_discharge" id="power_rime_discharge"></a>

### Ледяной разряд <a href="#ледяной-разряд" id="ледяной-разряд"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Крит атакой запускает ваш иней: {v}% Интеллекта уроном холодом по цели (откат 0,25 с) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ледяной разряд](../Stats/Stats-POWER.md#power_rime_discharge) |
| Событие · `on` | `CRIT` |
| checks | check: `ATTACK` |
| Перезарядка, с · `cooldown` | 0.25 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `COLD`; of: `INTELLECT` |


## POWER_RIME_BRITTLE <a href="#power_rime_brittle" id="power_rime_brittle"></a>

### Хрупкий иней <a href="#хрупкий-иней" id="хрупкий-иней"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс заморозить цель при критическом ударе |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Хрупкий иней](../Stats/Stats-POWER.md#power_rime_brittle) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `FREEZE` |


## POWER_RIMESORROW_GRIP <a href="#power_rimesorrow_grip" id="power_rimesorrow_grip"></a>

### Ледяная хватка <a href="#ледяная-хватка" id="ледяная-хватка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% физического урона атак превращается в холод |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ледяная хватка](../Stats/Stats-POWER.md#power_rimesorrow_grip) |
| sheet | Операция: `CONVERT`; Предшествующие зоны: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); to: [Урон холодом](../Stats/Stats-HERO.md#stock_attack_cold) |


## POWER_RIMESORROW_NUMB <a href="#power_rimesorrow_numb" id="power_rimesorrow_numb"></a>

### Немеющие слёзы <a href="#немеющие-слёзы" id="немеющие-слёзы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс при ударе охладить врага: −15% к скорости атаки на 2 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Немеющие слёзы](../Stats/Stats-POWER.md#power_rimesorrow_numb) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: -15% увеличение скорости атаки · [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Длительность: 2 |


## POWER_ROOTS_GROUNDED <a href="#power_roots_grounded" id="power_roots_grounded"></a>

### Вросший <a href="#вросший" id="вросший"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Вы не можете уклоняться |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вросший](../Stats/Stats-POWER.md#power_roots_grounded) |
| sheet | Операция: `SET`; to: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Значение: 0 |


## POWER_ROOTS_REGROWTH <a href="#power_roots_regrowth" id="power_roots_regrowth"></a>

### Отрастание <a href="#отрастание" id="отрастание"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 5 с восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отрастание](../Stats/Stats-POWER.md#power_roots_regrowth) |
| Событие · `on` | `EVERY` |
| every | 5 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_RIFT_STEP <a href="#power_rift_step" id="power_rift_step"></a>

### Шаг разлома <a href="#шаг-разлома" id="шаг-разлома"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к уклонению за каждый заряд ярости |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Шаг разлома](../Stats/Stats-POWER.md#power_rift_step) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Операция: `INCREASED`; scale: `FRENZY_CHARGES`; cap: 10 |


## POWER_RAIDER_CHARGE <a href="#power_raider_charge" id="power_raider_charge"></a>

### Налёт <a href="#налёт" id="налёт"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% к скорости атаки на 3 с в начале боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Налёт](../Stats/Stats-POWER.md#power_raider_charge) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3 |


## POWER_RAIDER_PLUNDER <a href="#power_raider_plunder" id="power_raider_plunder"></a>

### Добыча налётчика <a href="#добыча-налётчика" id="добыча-налётчика"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Враги роняют на {v}% больше золота |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Добыча налётчика](../Stats/Stats-POWER.md#power_raider_plunder) |
| world | gain: `GOLD` |


## POWER_REFLECTED_IMAGE <a href="#power_reflected_image" id="power_reflected_image"></a>

### Отражение <a href="#отражение" id="отражение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Копирует {v}% модификаторов кольца в другом слоте |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отражение](../Stats/Stats-POWER.md#power_reflected_image) |
| slots | Операция: `MIRROR`; pick: `OTHER_RING` |


## POWER_REFLECTED_SHEEN <a href="#power_reflected_sheen" id="power_reflected_sheen"></a>

### Серебряный блеск <a href="#серебряный-блеск" id="серебряный-блеск"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Модификаторы кольца в другом слоте сильнее на {v}% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Серебряный блеск](../Stats/Stats-POWER.md#power_reflected_sheen) |
| slots | Операция: `AMPLIFY`; pick: `OTHER_RING` |


## POWER_ROT_WINE_BOON <a href="#power_rot_wine_boon" id="power_rot_wine_boon"></a>

### Урожай гнили <a href="#урожай-гнили" id="урожай-гнили"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При применении даёт одно из десяти благ на 6 с: урон, скорость атаки или каста, крит, защиты, регенерацию, вампиризм, сопротивления, снижение урона — или «гнилое»: −10% ко всему |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Урожай гнили](../Stats/Stats-POWER.md#power_rot_wine_boon) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `ONE_OF`; Варианты: Действие: `BUFF`; Свойства: 30% увеличение урона · [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 6; Действие: `BUFF`; Свойства: 20% увеличение скорости атаки · [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Длительность: 6; Действие: `BUFF`; Свойства: 20% увеличение скорости сотворения · [Скорость применения](../Stats/Stats-HERO.md#stock_cast_speed); Длительность: 6; Действие: `BUFF`; Свойства: 60% увеличение шанса критического удара · [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Длительность: 6; Действие: `BUFF`; Свойства: 40% увеличение брони · [Броня](../Stats/Stats-HERO.md#stock_armor); 40% увеличение уклонения · [Уклонение](../Stats/Stats-HERO.md#stock_evasion); 40% увеличение энергетического щита · [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield); Длительность: 6; Действие: `BUFF`; Свойства: Восстанавливает 3% здоровья в секунду · [Регенерация здоровья, %](../Stats/Stats-HERO.md#stock_life_regen_percent); Длительность: 6; Действие: `BUFF`; Свойства: 2% урона крадётся здоровьем · [Общий вампиризм](../Stats/Stats-HERO.md#stock_leech_all); Длительность: 6; Действие: `BUFF`; Свойства: +20% к всем сопротивлениям стихиям · [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all); Длительность: 6; Действие: `BUFF`; Свойства: 10% дополнительного снижения физического урона · [Снижение физического урона](../Stats/Stats-HERO.md#stock_physical_reduction); Длительность: 6; Действие: `BUFF`; Свойства: -10% увеличение урона · [Урон](../Stats/Stats-HERO.md#stock_damage); -10% увеличение скорости атаки · [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); +-10% к всем сопротивлениям стихиям · [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all); Длительность: 6 |


## POWER_ROT_WINE_SPORES <a href="#power_rot_wine_spores" id="power_rot_wine_spores"></a>

### Кислые споры <a href="#кислые-споры" id="кислые-споры"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При применении {v}% шанс отравить всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кислые споры](../Stats/Stats-POWER.md#power_rot_wine_spores) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `POISON`; to: `ALL` |


## POWER_RECOUP_WARD <a href="#power_recoup_ward" id="power_recoup_ward"></a>

### Отскок щита <a href="#отскок-щита" id="отскок-щита"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда щит пробит, возвращается {v}% его |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отскок щита](../Stats/Stats-POWER.md#power_recoup_ward) |
| Событие · `on` | `SHIELD_BROKEN` |
| Перезарядка, с · `cooldown` | 8 |
| Эффекты · `effects` | Действие: `HEAL`; of: `SHIELD` |


## POWER_RAIDERS_TOLL <a href="#power_raiders_toll" id="power_raiders_toll"></a>

### Дань налётчика <a href="#дань-налётчика" id="дань-налётчика"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 2-й удар с шансом {v}% вызывает кровотечение |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Дань налётчика](../Stats/Stats-POWER.md#power_raiders_toll) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 2 |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `BLEED` |


## POWER_ROAD_WEARY <a href="#power_road_weary" id="power_road_weary"></a>

### Утомлённый путём <a href="#утомлённый-путём" id="утомлённый-путём"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый пройденный этап даёт {v}% увеличения урона на следующий |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Утомлённый путём](../Stats/Stats-POWER.md#power_road_weary) |
| Событие · `on` | `STAGE_CLEAR` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 30; stacks: 5 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
