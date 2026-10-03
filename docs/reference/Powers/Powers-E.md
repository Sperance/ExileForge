# Особые силы · E

## POWER_EMBER_TRAIL <a href="#power_ember_trail" id="power_ember_trail"></a>

### Огненный след <a href="#огненный-след" id="огненный-след"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 3 с с шансом {v}% поджигает случайного врага |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Огненный след](../Stats/Stats-POWER.md#power_ember_trail) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `EVERY` |
| every | 3 |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `IGNITE`; to: `RANDOM` |


## POWER_EMBER_WARD <a href="#power_ember_ward" id="power_ember_ward"></a>

### Угольная защита <a href="#угольная-защита" id="угольная-защита"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% ко всем сопротивлениям за каждого врага под состоянием, до 5 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Угольная защита](../Stats/Stats-POWER.md#power_ember_ward) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all); scale: `AILED_FOES`; cap: 5 |


## POWER_ENDLESS_ROAD <a href="#power_endless_road" id="power_endless_road"></a>

### Бесконечная дорога <a href="#бесконечная-дорога" id="бесконечная-дорога"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бесконечная дорога](../Stats/Stats-POWER.md#power_endless_road) |
| Событие · `on` | `STANDING` |
| checks | check: `FIGHT_BEFORE`; Значение: 5 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken) |


## POWER_EVERFLASK <a href="#power_everflask" id="power_everflask"></a>

### Вечная фляга <a href="#вечная-фляга" id="вечная-фляга"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 5 с даёт {v} зарядов всем флягам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вечная фляга](../Stats/Stats-POWER.md#power_everflask) |
| Событие · `on` | `EVERY` |
| every | 5 |
| Эффекты · `effects` | Действие: `CHARGES` |


## POWER_EMBERHEART <a href="#power_emberheart" id="power_emberheart"></a>

### Сердце углей <a href="#сердце-углей" id="сердце-углей"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Поджог врага даёт {v}% увеличения урона огнём на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сердце углей](../Stats/Stats-POWER.md#power_emberheart) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire); Операция: `INCREASED`; Длительность: 4; stacks: 3 |


## POWER_EMBER_TOUCH <a href="#power_ember_touch" id="power_ember_touch"></a>

### Касание углей <a href="#касание-углей" id="касание-углей"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 3-й удар с шансом {v}% поджигает |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Касание углей](../Stats/Stats-POWER.md#power_ember_touch) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 3 |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `IGNITE` |


## POWER_ETERNAL_SANDS <a href="#power_eternal_sands" id="power_eternal_sands"></a>

### Вечные пески <a href="#вечные-пески" id="вечные-пески"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Применение умения сокращает перезарядку ваших умений на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вечные пески](../Stats/Stats-POWER.md#power_eternal_sands) |
| Событие · `on` | `SKILL_USE` |
| Эффекты · `effects` | Действие: `COOLDOWNS` |


## POWER_ETERNITY_SHROUD <a href="#power_eternity_shroud" id="power_eternity_shroud"></a>

### Покров веков <a href="#покров-веков" id="покров-веков"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Покров веков](../Stats/Stats-POWER.md#power_eternity_shroud) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken); scale: `MISSING_LIFE`; cap: 6 |


## POWER_ENDLESS_STRIDE <a href="#power_endless_stride" id="power_endless_stride"></a>

### Бесконечный путь <a href="#бесконечный-путь" id="бесконечный-путь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение даёт {v}% увеличения уклонения на 4 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бесконечный путь](../Stats/Stats-POWER.md#power_endless_stride) |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Операция: `INCREASED`; Длительность: 4; stacks: 5 |


## POWER_ENTROPY <a href="#power_entropy" id="power_entropy"></a>

### Энтропия <a href="#энтропия" id="энтропия"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Энтропия](../Stats/Stats-POWER.md#power_entropy) |
| Событие · `on` | `EVERY` |
| every | 3 |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Сопротивление хаосу](../Stats/Stats-HERO.md#stock_resist_chaos); Длительность: 3; to: `ALL` |


## POWER_EMBER_RESOLVE <a href="#power_ember_resolve" id="power_ember_resolve"></a>

### Тлеющая решимость <a href="#тлеющая-решимость" id="тлеющая-решимость"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Наложенное на вас состояние даёт барьер в {v}% максимума здоровья на 3 с, не чаще раза в 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тлеющая решимость](../Stats/Stats-POWER.md#power_ember_resolve) |
| Событие · `on` | `AILED` |
| Перезарядка, с · `cooldown` | 4 |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_ENDLESS_FALL <a href="#power_endless_fall" id="power_endless_fall"></a>

### Бесконечное падение <a href="#бесконечное-падение" id="бесконечное-падение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения уклонения за каждого проклятого врага, до 5 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бесконечное падение](../Stats/Stats-POWER.md#power_endless_fall) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Операция: `INCREASED`; scale: `CURSED_FOES`; cap: 5 |


## POWER_ESSENCE_SPARK <a href="#power_essence_spark" id="power_essence_spark"></a>

### Искра эссенции <a href="#искра-эссенции" id="искра-эссенции"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Заклинание с шансом {v}% вызывает шок у цели |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Искра эссенции](../Stats/Stats-POWER.md#power_essence_spark) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `SKILL_USE` |
| checks | check: `SPELL` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `SHOCK` |


## POWER_ELEMENTAL_ATTUNEMENT <a href="#power_elemental_attunement" id="power_elemental_attunement"></a>

### Настрой на стихии <a href="#настрой-на-стихии" id="настрой-на-стихии"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к энергощиту за каждый 1% ко всем сопротивлениям стихиям |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Настрой на стихии](../Stats/Stats-POWER.md#power_elemental_attunement) |
| sheet | Операция: `PER`; Предшествующие зоны: [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all); to: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield); per: 1 |


## POWER_ESSENCE_HARVEST <a href="#power_essence_harvest" id="power_essence_harvest"></a>

### Жатва эссенций <a href="#жатва-эссенций" id="жатва-эссенций"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство редкого или уникального врага восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жатва эссенций](../Stats/Stats-POWER.md#power_essence_harvest) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_RARE` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_ESSENCE_INFUSION <a href="#power_essence_infusion" id="power_essence_infusion"></a>

### Вливание эссенции <a href="#вливание-эссенции" id="вливание-эссенции"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство редкого или уникального врага даёт +{v}% к пробиванию стихиями на 10 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вливание эссенции](../Stats/Stats-POWER.md#power_essence_infusion) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_RARE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Пробивание стихий](../Stats/Stats-HERO.md#stock_penetrate_elemental); Длительность: 10 |


## POWER_EMBALMED_VIGOUR <a href="#power_embalmed_vigour" id="power_embalmed_vigour"></a>

### Бальзамная сила <a href="#бальзамная-сила" id="бальзамная-сила"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% скорости восстановления ниже 50% здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бальзамная сила](../Stats/Stats-POWER.md#power_embalmed_vigour) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_BELOW`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость восстановления](../Stats/Stats-HERO.md#stock_recovery_rate) |


## POWER_EMBER_DRINK <a href="#power_ember_drink" id="power_ember_drink"></a>

### Глоток углей <a href="#глоток-углей" id="глоток-углей"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Огненный удар по вам восстанавливает {v}% максимума здоровья, не чаще раза в 2 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Глоток углей](../Stats/Stats-POWER.md#power_ember_drink) |
| Событие · `on` | `HIT_TAKEN` |
| checks | check: `DAMAGE_TYPE`; word: `FIRE` |
| Перезарядка, с · `cooldown` | 2 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_ERUPTION <a href="#power_eruption" id="power_eruption"></a>

### Извержение <a href="#извержение" id="извержение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Горящий враг при смерти извергается на {v}% своего максимума здоровья огнём по всем врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Извержение](../Stats/Stats-POWER.md#power_eruption) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TARGET_LIFE`; Тип: `FIRE`; to: `ALL` |


## POWER_ENDURANCE_ON_KILL <a href="#power_endurance_on_kill" id="power_endurance_on_kill"></a>

### Стойкое убийство <a href="#стойкое-убийство" id="стойкое-убийство"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс получить заряд выносливости при убийстве |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Стойкое убийство](../Stats/Stats-POWER.md#power_endurance_on_kill) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `CHARGE`; charge: `ENDURANCE` |


## POWER_ENDURANCE_WHEN_HIT <a href="#power_endurance_when_hit" id="power_endurance_when_hit"></a>

### Закалка ударами <a href="#закалка-ударами" id="закалка-ударами"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс получить заряд выносливости, когда вас бьют |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Закалка ударами](../Stats/Stats-POWER.md#power_endurance_when_hit) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `CHARGE`; charge: `ENDURANCE` |


## POWER_EMBALMED <a href="#power_embalmed" id="power_embalmed"></a>

### Прививка хаоса <a href="#прививка-хаоса" id="прививка-хаоса"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Максимум здоровья равен 1; иммунитет к урону хаосом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Прививка хаоса](../Stats/Stats-POWER.md#power_embalmed) |
| sheet | Операция: `SET`; to: [Здоровье](../Stats/Stats-HERO.md#stock_health); Значение: 1; Операция: `SET`; to: [Иммунитет к хаосу](../Stats/Stats-HERO.md#stock_chaos_immune); Значение: 1 |


## POWER_EMBALMERS_SEAL <a href="#power_embalmers_seal" id="power_embalmers_seal"></a>

### Печать бальзамировщика <a href="#печать-бальзамировщика" id="печать-бальзамировщика"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда ЭЩ разрушен, вы неуязвимы {v} с (откат 20 с) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Печать бальзамировщика](../Stats/Stats-POWER.md#power_embalmers_seal) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `SHIELD_BROKEN` |
| Перезарядка, с · `cooldown` | 20 |
| Эффекты · `effects` | Действие: `INVULNERABLE` |


## POWER_ENTHRONED_REIGN <a href="#power_enthroned_reign" id="power_enthroned_reign"></a>

### Трон Бездны <a href="#трон-бездны" id="трон-бездны"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Модификаторы надетых самоцветов сильнее на {v}% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Трон Бездны](../Stats/Stats-POWER.md#power_enthroned_reign) |
| slots | Операция: `AMPLIFY`; slots: `JEWEL` |


## POWER_ENTHRONED_SHADOW <a href="#power_enthroned_shadow" id="power_enthroned_shadow"></a>

### Двор теней <a href="#двор-теней" id="двор-теней"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% ко всем стихийным сопротивлениям за каждый надетый самоцвет |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Двор теней](../Stats/Stats-POWER.md#power_enthroned_shadow) |
| sheet | Операция: `PER`; Предшествующие зоны: [Надетые самоцветы](../Stats/Stats-HERO.md#stock_worn_jewels); to: [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all) |


## POWER_EMERALD_VENOM <a href="#power_emerald_venom" id="power_emerald_venom"></a>

### Изумрудный яд <a href="#изумрудный-яд" id="изумрудный-яд"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс отравить при ударе |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Изумрудный яд](../Stats/Stats-POWER.md#power_emerald_venom) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `POISON` |


## POWER_EMERALD_PULSE <a href="#power_emerald_pulse" id="power_emerald_pulse"></a>

### Зелёный пульс <a href="#зелёный-пульс" id="зелёный-пульс"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 5 с восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Зелёный пульс](../Stats/Stats-POWER.md#power_emerald_pulse) |
| Событие · `on` | `EVERY` |
| every | 5 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_EYELESS_SIGHT <a href="#power_eyeless_sight" id="power_eyeless_sight"></a>

### Зрение Безглазого <a href="#зрение-безглазого" id="зрение-безглазого"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% к количеству предметов с редких и уникальных врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Зрение Безглазого](../Stats/Stats-POWER.md#power_eyeless_sight) |
| world | gain: `QUANTITY`; against: `RARE`; `UNIQUE` |


## POWER_EYELESS_GAZE <a href="#power_eyeless_gaze" id="power_eyeless_gaze"></a>

### Немигающий взгляд <a href="#немигающий-взгляд" id="немигающий-взгляд"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удар по редкому или уникальному врагу: 3 с он получает на {v}% больше урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Немигающий взгляд](../Stats/Stats-POWER.md#power_eyeless_gaze) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_RARE` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken); Длительность: 3 |


## POWER_ELDER_MIRROR <a href="#power_elder_mirror" id="power_elder_mirror"></a>

### Терпение Древнего <a href="#терпение-древнего" id="терпение-древнего"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанса блока атак добавляется к шансу блока заклинаний |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Терпение Древнего](../Stats/Stats-POWER.md#power_elder_mirror) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Шанс блока](../Stats/Stats-HERO.md#stock_block_chance); to: [Шанс блока заклинаний](../Stats/Stats-HERO.md#stock_spell_block) |


## POWER_ELDER_REBUKE <a href="#power_elder_rebuke" id="power_elder_rebuke"></a>

### Укор Древнего <a href="#укор-древнего" id="укор-древнего"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок бьёт атакующего на {v}% урона оружия хаосом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Укор Древнего](../Stats/Stats-POWER.md#power_elder_rebuke) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `CHAOS` |


## POWER_ENDLESS_SIEGE <a href="#power_endless_siege" id="power_endless_siege"></a>

### Бесконечная осада <a href="#бесконечная-осада" id="бесконечная-осада"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 4-й удар по вам даёт барьер в 15% здоровья на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бесконечная осада](../Stats/Stats-POWER.md#power_endless_siege) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `HIT_TAKEN` |
| checks | check: `NTH`; Значение: 4 |
| Эффекты · `effects` | Действие: `BARRIER`; Количество: 15 |


## POWER_ECLIPSE_VEIL <a href="#power_eclipse_veil" id="power_eclipse_veil"></a>

### Покров затмения <a href="#покров-затмения" id="покров-затмения"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 10 с неуязвимость на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Покров затмения](../Stats/Stats-POWER.md#power_eclipse_veil) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `EVERY` |
| every | 10 |
| Эффекты · `effects` | Действие: `INVULNERABLE` |


## POWER_ENDURING_BLOCK <a href="#power_enduring_block" id="power_enduring_block"></a>

### Стойкий блок <a href="#стойкий-блок" id="стойкий-блок"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс получить заряд выносливости при блоке |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Стойкий блок](../Stats/Stats-POWER.md#power_enduring_block) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `CHARGE`; charge: `ENDURANCE`; Количество: 1 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
