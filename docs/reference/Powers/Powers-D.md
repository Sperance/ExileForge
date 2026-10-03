# Особые силы · D

## POWER_DEFIANCE <a href="#power_defiance" id="power_defiance"></a>

### Непокорность <a href="#непокорность" id="непокорность"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона, пока на вас есть состояние |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Непокорность](../Stats/Stats-POWER.md#power_defiance) |
| Событие · `on` | `STANDING` |
| checks | check: `SELF_AILED` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage) |


## POWER_DESPERATE_THIRST <a href="#power_desperate_thirst" id="power_desperate_thirst"></a>

### Отчаянная жажда <a href="#отчаянная-жажда" id="отчаянная-жажда"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% вампиризма здоровья, пока здоровья меньше 50% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отчаянная жажда](../Stats/Stats-POWER.md#power_desperate_thirst) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_BELOW`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Общий вампиризм](../Stats/Stats-HERO.md#stock_leech_all) |


## POWER_DEATHS_VEIL <a href="#power_deaths_veil" id="power_deaths_veil"></a>

### Вуаль смерти <a href="#вуаль-смерти" id="вуаль-смерти"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт барьер в {v}% максимума здоровья на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вуаль смерти](../Stats/Stats-POWER.md#power_deaths_veil) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 4 |


## POWER_DEEP_TIDE <a href="#power_deep_tide" id="power_deep_tide"></a>

### Глубинный прилив <a href="#глубинный-прилив" id="глубинный-прилив"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона за каждые 10% вашей маны |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Глубинный прилив](../Stats/Stats-POWER.md#power_deep_tide) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); scale: `MANA` |


## POWER_DEVOUR <a href="#power_devour" id="power_devour"></a>

### Пожирание <a href="#пожирание" id="пожирание"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт {v}% увеличения максимума здоровья на 10 с, до 10 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пожирание](../Stats/Stats-POWER.md#power_devour) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Здоровье](../Stats/Stats-HERO.md#stock_health); Операция: `INCREASED`; Длительность: 10; stacks: 10 |


## POWER_DEVOTED_SWIFTNESS <a href="#power_devoted_swiftness" id="power_devoted_swiftness"></a>

### Преданная стремительность <a href="#преданная-стремительность" id="преданная-стремительность"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона за каждые 10% скорости передвижения |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Преданная стремительность](../Stats/Stats-POWER.md#power_devoted_swiftness) |
| sheet | Операция: `PER`; Предшествующие зоны: [Скорость передвижения](../Stats/Stats-HERO.md#stock_movement_speed); to: [Урон](../Stats/Stats-HERO.md#stock_damage); per: 10 |


## POWER_DESERT_WIND <a href="#power_desert_wind" id="power_desert_wind"></a>

### Ветер пустыни <a href="#ветер-пустыни" id="ветер-пустыни"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 5 с бьёт случайного врага на {v}% урона оружия молнией |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ветер пустыни](../Stats/Stats-POWER.md#power_desert_wind) |
| Событие · `on` | `EVERY` |
| every | 5 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `LIGHTNING`; to: `RANDOM` |


## POWER_DRILL_SHOT <a href="#power_drill_shot" id="power_drill_shot"></a>

### Сквозной выстрел <a href="#сквозной-выстрел" id="сквозной-выстрел"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 4-й удар пробивает насквозь: бьёт всех остальных врагов на {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сквозной выстрел](../Stats/Stats-POWER.md#power_drill_shot) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 4 |
| Эффекты · `effects` | Действие: `DAMAGE`; to: `OTHERS` |


## POWER_DAWNBURST <a href="#power_dawnburst" id="power_dawnburst"></a>

### Рассветный всполох <a href="#рассветный-всполох" id="рассветный-всполох"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок наносит всем врагам урон огнём в {v}% максимума здоровья, не чаще раза в секунду |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Рассветный всполох](../Stats/Stats-POWER.md#power_dawnburst) |
| Событие · `on` | `BLOCK` |
| Перезарядка, с · `cooldown` | 1 |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `LIFE`; Тип: `FIRE`; to: `ALL` |


## POWER_DEEP_MIND <a href="#power_deep_mind" id="power_deep_mind"></a>

### Глубокий разум <a href="#глубокий-разум" id="глубокий-разум"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Даёт энергощит, равный {v}% максимума маны |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Глубокий разум](../Stats/Stats-POWER.md#power_deep_mind) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Мана](../Stats/Stats-HERO.md#stock_mana); to: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield) |


## POWER_DIVINE_FALL <a href="#power_divine_fall" id="power_divine_fall"></a>

### Падение божества <a href="#падение-божества" id="падение-божества"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Падение здоровья до низкого даёт неуязвимость на {v} с, не чаще раза в 30 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Падение божества](../Stats/Stats-POWER.md#power_divine_fall) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `LOW_LIFE` |
| Перезарядка, с · `cooldown` | 30 |
| Эффекты · `effects` | Действие: `INVULNERABLE` |


## POWER_DREAD_GAZE <a href="#power_dread_gaze" id="power_dread_gaze"></a>

### Взгляд ужаса <a href="#взгляд-ужаса" id="взгляд-ужаса"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар: враг получает на {v}% больше урона 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Взгляд ужаса](../Stats/Stats-POWER.md#power_dread_gaze) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken); Длительность: 4 |


## POWER_DEEP_RECOIL <a href="#power_deep_recoil" id="power_deep_recoil"></a>

### Отдача глубин <a href="#отдача-глубин" id="отдача-глубин"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Полученный удар возвращается нападавшему: {v}% урона хаосом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отдача глубин](../Stats/Stats-POWER.md#power_deep_recoil) |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TAKEN`; Тип: `CHAOS` |


## POWER_DEPTH_FRENZY <a href="#power_depth_frenzy" id="power_depth_frenzy"></a>

### Неистовство глубин <a href="#неистовство-глубин" id="неистовство-глубин"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт {v}% повышения скорости атаки и чар на 4 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Неистовство глубин](../Stats/Stats-POWER.md#power_depth_frenzy) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Характеристика: [Скорость применения](../Stats/Stats-HERO.md#stock_cast_speed); Операция: `INCREASED`; Длительность: 4; stacks: 5 |


## POWER_DEVOTED_ZEAL <a href="#power_devoted_zeal" id="power_devoted_zeal"></a>

### Рвение <a href="#рвение" id="рвение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения скорости атаки в первые 5 с боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Рвение](../Stats/Stats-POWER.md#power_devoted_zeal) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 5 |


## POWER_DAWN_OF_AN_ERA <a href="#power_dawn_of_an_era" id="power_dawn_of_an_era"></a>

### Заря эпохи <a href="#заря-эпохи" id="заря-эпохи"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Оглушение врага восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Заря эпохи](../Stats/Stats-POWER.md#power_dawn_of_an_era) |
| Событие · `on` | `STUN` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_DROWNING_JAWS <a href="#power_drowning_jaws" id="power_drowning_jaws"></a>

### Челюсти глубин <a href="#челюсти-глубин" id="челюсти-глубин"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по редким и уникальным врагам возвращают здоровьем {v}% нанесённого урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Челюсти глубин](../Stats/Stats-POWER.md#power_drowning_jaws) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_RARE` |
| Эффекты · `effects` | Действие: `HEAL`; of: `DEALT` |


## POWER_DEEP_DRAUGHT <a href="#power_deep_draught" id="power_deep_draught"></a>

### Глубокий глоток <a href="#глубокий-глоток" id="глубокий-глоток"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 5-й удар возвращает здоровьем {v}% своего урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Глубокий глоток](../Stats/Stats-POWER.md#power_deep_draught) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 5 |
| Эффекты · `effects` | Действие: `HEAL`; of: `DEALT` |


## POWER_DEAD_WEIGHT <a href="#power_dead_weight" id="power_dead_weight"></a>

### Мёртвый груз <a href="#мёртвый-груз" id="мёртвый-груз"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удар с шансом {v}% откладывает следующую атаку врага на 0,4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Мёртвый груз](../Stats/Stats-POWER.md#power_dead_weight) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `DELAY`; Длительность: 0.4 |


## POWER_DEEPER_STILL <a href="#power_deeper_still" id="power_deeper_still"></a>

### Всё глубже <a href="#всё-глубже" id="всё-глубже"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Пройденный этап даёт +{v}% урона на первые 8 с следующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Всё глубже](../Stats/Stats-POWER.md#power_deeper_still) |
| Событие · `on` | `STAGE_CLEAR` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 8 |


## POWER_DUNE_SONG <a href="#power_dune_song" id="power_dune_song"></a>

### Песнь барханов <a href="#песнь-барханов" id="песнь-барханов"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | С шансом {v}% в начале боя проклинает всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Песнь барханов](../Stats/Stats-POWER.md#power_dune_song) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `CURSE`; to: `ALL` |


## POWER_DISTILLED_EDGE <a href="#power_distilled_edge" id="power_distilled_edge"></a>

### Перегнанное остриё <a href="#перегнанное-остриё" id="перегнанное-остриё"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия +{v}% проникания стихий |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Перегнанное остриё](../Stats/Stats-POWER.md#power_distilled_edge) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Пробивание стихий](../Stats/Stats-HERO.md#stock_penetrate_elemental) |


## POWER_DUSK_TITHE <a href="#power_dusk_tithe" id="power_dusk_tithe"></a>

### Закатная десятина <a href="#закатная-десятина" id="закатная-десятина"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство врага восстанавливает {v}% максимума маны |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Закатная десятина](../Stats/Stats-POWER.md#power_dusk_tithe) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `HEAL`; of: `MANA` |


## POWER_DESECRATED_OFFERING <a href="#power_desecrated_offering" id="power_desecrated_offering"></a>

### Осквернённое подношение <a href="#осквернённое-подношение" id="осквернённое-подношение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждое убийство стоит 2% максимума здоровья и даёт {v}% увеличения физического урона на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Осквернённое подношение](../Stats/Stats-POWER.md#power_desecrated_offering) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `HURT`; of: `LIFE`; Количество: 2; Действие: `BUFF`; Свойства: Характеристика: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); Операция: `INCREASED`; Длительность: 4; stacks: 3 |


## POWER_DESCENT_STRIDE <a href="#power_descent_stride" id="power_descent_stride"></a>

### Шаг нисхождения <a href="#шаг-нисхождения" id="шаг-нисхождения"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Пройденный этап даёт {v}% увеличения скорости атаки на первые 6 с следующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Шаг нисхождения](../Stats/Stats-POWER.md#power_descent_stride) |
| Событие · `on` | `STAGE_CLEAR` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 6 |


## POWER_DRONE_HASTE <a href="#power_drone_haste" id="power_drone_haste"></a>

### Спешка трутня <a href="#спешка-трутня" id="спешка-трутня"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения скорости атаки в первые 4 с боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Спешка трутня](../Stats/Stats-POWER.md#power_drone_haste) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 4 |


## POWER_DEPTH_SIGHT <a href="#power_depth_sight" id="power_depth_sight"></a>

### Глубинное зрение <a href="#глубинное-зрение" id="глубинное-зрение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Пройденный этап с шансом {v}% делает первый удар следующего критическим |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Глубинное зрение](../Stats/Stats-POWER.md#power_depth_sight) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `STAGE_CLEAR` |
| Эффекты · `effects` | Действие: `NEXT_CRIT` |


## POWER_DOCTORS_CRUELTY <a href="#power_doctors_cruelty" id="power_doctors_cruelty"></a>

### Жестокость лекаря <a href="#жестокость-лекаря" id="жестокость-лекаря"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия {v}% увеличения урона по отравленным врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жестокость лекаря](../Stats/Stats-POWER.md#power_doctors_cruelty) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон по отравленным](../Stats/Stats-HERO.md#stock_damage_vs_poisoned) |


## POWER_DEEP_REFILL <a href="#power_deep_refill" id="power_deep_refill"></a>

### Глубинное наполнение <a href="#глубинное-наполнение" id="глубинное-наполнение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Пройденный этап восстанавливает {v}% максимума энергетического щита в начале следующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Глубинное наполнение](../Stats/Stats-POWER.md#power_deep_refill) |
| Событие · `on` | `STAGE_CLEAR` |
| Эффекты · `effects` | Действие: `HEAL`; of: `SHIELD` |


## POWER_DISTANT_THUNDER <a href="#power_distant_thunder" id="power_distant_thunder"></a>

### Далёкий гром <a href="#далёкий-гром" id="далёкий-гром"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по редким и уникальным врагам отзываются через 1 с на {v}% урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Далёкий гром](../Stats/Stats-POWER.md#power_distant_thunder) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_RARE` |
| Эффекты · `effects` | Действие: [Отзвук](../../Skills/Skills-SCION.md#echo); Длительность: 1 |


## POWER_DARK_CONSUMPTION <a href="#power_dark_consumption" id="power_dark_consumption"></a>

### Чёрное пламя <a href="#чёрное-пламя" id="чёрное-пламя"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% урона огнём превращается в хаос |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Чёрное пламя](../Stats/Stats-POWER.md#power_dark_consumption) |
| sheet | Операция: `CONVERT`; Предшествующие зоны: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire); to: [Урон хаосом](../Stats/Stats-HERO.md#stock_attack_chaos) |


## POWER_DARK_FEAST <a href="#power_dark_feast" id="power_dark_feast"></a>

### Тёмный пир <a href="#тёмный-пир" id="тёмный-пир"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство отравленного врага восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тёмный пир](../Stats/Stats-POWER.md#power_dark_feast) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `POISON` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_DISFAVOUR_REND <a href="#power_disfavour_rend" id="power_disfavour_rend"></a>

### Разрыв <a href="#разрыв" id="разрыв"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс удара атакой вызвать кровотечение в 2 стака |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Разрыв](../Stats/Stats-POWER.md#power_disfavour_rend) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| checks | check: `ATTACK` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `BLEED`; stacks: 2 |


## POWER_DEFIANCE_WILL <a href="#power_defiance_will" id="power_defiance_will"></a>

### Воля изгнанника <a href="#воля-изгнанника" id="воля-изгнанника"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% получаемого урона снимается с маны до здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Воля изгнанника](../Stats/Stats-POWER.md#power_defiance_will) |
| sheet | Операция: `ADD`; to: [Мана прежде здоровья](../Stats/Stats-HERO.md#stock_mana_before_life) |


## POWER_DEFIANCE_RESOLVE <a href="#power_defiance_resolve" id="power_defiance_resolve"></a>

### Несломленный <a href="#несломленный" id="несломленный"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к снижению физического урона, пока маны больше 50% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Несломленный](../Stats/Stats-POWER.md#power_defiance_resolve) |
| Событие · `on` | `STANDING` |
| checks | check: `MANA_ABOVE`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Снижение физического урона](../Stats/Stats-HERO.md#stock_physical_reduction) |


## POWER_DOCTOR_TRANSFUSION <a href="#power_doctor_transfusion" id="power_doctor_transfusion"></a>

### Переливание <a href="#переливание" id="переливание"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Питомец получает {v}% вашего максимума здоровья и брони |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Переливание](../Stats/Stats-POWER.md#power_doctor_transfusion) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Здоровье](../Stats/Stats-HERO.md#stock_health); to: [Прибавка здоровья питомца](../Stats/Stats-HERO.md#stock_pet_life); Операция: `GAIN`; Предшествующие зоны: [Броня](../Stats/Stats-HERO.md#stock_armor); to: [Прибавка брони питомца](../Stats/Stats-HERO.md#stock_pet_armor) |


## POWER_DOCTOR_MIASMA <a href="#power_doctor_miasma" id="power_doctor_miasma"></a>

### Миазмы <a href="#миазмы" id="миазмы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары питомца с шансом {v}% отравляют |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Миазмы](../Stats/Stats-POWER.md#power_doctor_miasma) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `PET_HIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `POISON` |


## POWER_DEEP_EMBRACE <a href="#power_deep_embrace" id="power_deep_embrace"></a>

### Объятие глубин <a href="#объятие-глубин" id="объятие-глубин"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к урону за каждый заряд ярости, силы и выносливости |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Объятие глубин](../Stats/Stats-POWER.md#power_deep_embrace) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `INCREASED`; scale: `FRENZY_CHARGES`; cap: 10; Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `INCREASED`; scale: `POWER_CHARGES`; cap: 10; Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `INCREASED`; scale: `ENDURANCE_CHARGES`; cap: 10 |


## POWER_DREAMSHARD_GLINT <a href="#power_dreamshard_glint" id="power_dreamshard_glint"></a>

### Отблеск <a href="#отблеск" id="отблеск"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Умение с шансом {v}% восстанавливает 5% максимума маны |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отблеск](../Stats/Stats-POWER.md#power_dreamshard_glint) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `SKILL_USE` |
| Эффекты · `effects` | Действие: `HEAL`; of: `MANA`; Количество: 5 |


## POWER_DREAMSHARD_SLEEP <a href="#power_dreamshard_sleep" id="power_dreamshard_sleep"></a>

### Усыпление <a href="#усыпление" id="усыпление"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда вас бьют, {v}% шанс оглушить атакующего на 0,5 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Усыпление](../Stats/Stats-POWER.md#power_dreamshard_sleep) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `STUN`; Длительность: 0.5 |


## POWER_DEEP_FREEZE <a href="#power_deep_freeze" id="power_deep_freeze"></a>

### Глубокий холод <a href="#глубокий-холод" id="глубокий-холод"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 3-й удар с шансом {v}% замораживает |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Глубокий холод](../Stats/Stats-POWER.md#power_deep_freeze) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 3 |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `FREEZE` |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
