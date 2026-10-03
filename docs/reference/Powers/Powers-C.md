# Особые силы · C

## POWER_CONSTRICT <a href="#power_constrict" id="power_constrict"></a>

### Удушье <a href="#удушье" id="удушье"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона ядом за каждого врага под состоянием, до 5 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Удушье](../Stats/Stats-POWER.md#power_constrict) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон от яда](../Stats/Stats-HERO.md#stock_poison_damage); scale: `AILED_FOES`; cap: 5 |


## POWER_CRUCIBLE <a href="#power_crucible" id="power_crucible"></a>

### Горнило <a href="#горнило" id="горнило"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Даёт урон огнём, равный {v}% брони |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Горнило](../Stats/Stats-POWER.md#power_crucible) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Броня](../Stats/Stats-HERO.md#stock_armor); to: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire) |


## POWER_CRYSTAL_RETORT <a href="#power_crystal_retort" id="power_crystal_retort"></a>

### Хрустальный ответ <a href="#хрустальный-ответ" id="хрустальный-ответ"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Полученный крит наносит всем врагам {v}% полученного урона молнией |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Хрустальный ответ](../Stats/Stats-POWER.md#power_crystal_retort) |
| Событие · `on` | `CRIT_TAKEN` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TAKEN`; Тип: `LIGHTNING`; to: `ALL` |


## POWER_CINDER_PIERCE <a href="#power_cinder_pierce" id="power_cinder_pierce"></a>

### Прожигание <a href="#прожигание" id="прожигание"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Урон огнём пробивает сопротивление огню на {v}% вашего сопротивления огню |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Прожигание](../Stats/Stats-POWER.md#power_cinder_pierce) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Сопротивление огню](../Stats/Stats-HERO.md#stock_resist_fire); to: [Пробивание огня](../Stats/Stats-HERO.md#stock_penetrate_fire) |


## POWER_CARCASS_BURST <a href="#power_carcass_burst" id="power_carcass_burst"></a>

### Разрыв туши <a href="#разрыв-туши" id="разрыв-туши"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При получении удара {v}% шанс ударить всех врагов на 60% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Разрыв туши](../Stats/Stats-POWER.md#power_carcass_burst) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `DAMAGE`; to: `ALL`; Количество: 60 |


## POWER_CARNAGE <a href="#power_carnage" id="power_carnage"></a>

### Бойня <a href="#бойня" id="бойня"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% вампиризма здоровья за каждого врага под состоянием, до 3 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Бойня](../Stats/Stats-POWER.md#power_carnage) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Общий вампиризм](../Stats/Stats-HERO.md#stock_leech_all); scale: `AILED_FOES`; cap: 3 |


## POWER_CATALYST_SURGE <a href="#power_catalyst_surge" id="power_catalyst_surge"></a>

### Всплеск катализатора <a href="#всплеск-катализатора" id="всплеск-катализатора"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Шок врага восстанавливает {v}% маны |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Всплеск катализатора](../Stats/Stats-POWER.md#power_catalyst_surge) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `SHOCK` |
| Эффекты · `effects` | Действие: `HEAL`; of: `MANA` |


## POWER_CINDER_SKIN <a href="#power_cinder_skin" id="power_cinder_skin"></a>

### Пепельная кожа <a href="#пепельная-кожа" id="пепельная-кожа"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары огнём по вам возвращают здоровьем {v}% своего урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пепельная кожа](../Stats/Stats-POWER.md#power_cinder_skin) |
| Событие · `on` | `HIT_TAKEN` |
| checks | check: `DAMAGE_TYPE`; word: `FIRE` |
| Эффекты · `effects` | Действие: `HEAL`; of: `TAKEN` |


## POWER_CHAINBIND <a href="#power_chainbind" id="power_chainbind"></a>

### Цепная хватка <a href="#цепная-хватка" id="цепная-хватка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При получении удара {v}% шанс сковать нападавшего, оглушив на 0,5 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Цепная хватка](../Stats/Stats-POWER.md#power_chainbind) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `STUN`; Длительность: 0.5 |


## POWER_CRUEL_ART <a href="#power_cruel_art" id="power_cruel_art"></a>

### Жестокое искусство <a href="#жестокое-искусство" id="жестокое-искусство"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар с шансом {v}% вызывает у врага кровотечение |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жестокое искусство](../Stats/Stats-POWER.md#power_cruel_art) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `BLEED` |


## POWER_COVERING_FIRE <a href="#power_covering_fire" id="power_covering_fire"></a>

### Прикрывающий огонь <a href="#прикрывающий-огонь" id="прикрывающий-огонь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок отвечает нападавшему выстрелом на {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Прикрывающий огонь](../Stats/Stats-POWER.md#power_covering_fire) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `DAMAGE` |


## POWER_CATALYSIS <a href="#power_catalysis" id="power_catalysis"></a>

### Катализ <a href="#катализ" id="катализ"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Шок врага даёт {v}% увеличения урона чар на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Катализ](../Stats/Stats-POWER.md#power_catalysis) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `SHOCK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон чар](../Stats/Stats-HERO.md#stock_spell_damage); Операция: `INCREASED`; Длительность: 4; stacks: 3 |


## POWER_CRUSHING_BLOW <a href="#power_crushing_blow" id="power_crushing_blow"></a>

### Сокрушительный удар <a href="#сокрушительный-удар" id="сокрушительный-удар"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 3-й удар оглушает на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сокрушительный удар](../Stats/Stats-POWER.md#power_crushing_blow) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 3 |
| Эффекты · `effects` | Действие: `STUN` |


## POWER_CRYSTAL_EDGE <a href="#power_crystal_edge" id="power_crystal_edge"></a>

### Хрустальная кромка <a href="#хрустальная-кромка" id="хрустальная-кромка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар с шансом {v}% вызывает шок |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Хрустальная кромка](../Stats/Stats-POWER.md#power_crystal_edge) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `SHOCK` |


## POWER_CHITIN_SHELL <a href="#power_chitin_shell" id="power_chitin_shell"></a>

### Хитиновый панцирь <a href="#хитиновый-панцирь" id="хитиновый-панцирь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение даёт барьер в {v}% максимума здоровья на 3 с, не чаще раза в 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Хитиновый панцирь](../Stats/Stats-POWER.md#power_chitin_shell) |
| Событие · `on` | `EVADE` |
| Перезарядка, с · `cooldown` | 3 |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_CALM_CENTER <a href="#power_calm_center" id="power_calm_center"></a>

### Спокойный центр <a href="#спокойный-центр" id="спокойный-центр"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса крита, пока перед вами один враг |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Спокойный центр](../Stats/Stats-POWER.md#power_calm_center) |
| Событие · `on` | `STANDING` |
| checks | check: `FOES_AT_MOST`; Значение: 1 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED` |


## POWER_COLD_DRAW <a href="#power_cold_draw" id="power_cold_draw"></a>

### Холодная тетива <a href="#холодная-тетива" id="холодная-тетива"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения скорости атаки против охлаждённых врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Холодная тетива](../Stats/Stats-POWER.md#power_cold_draw) |
| Событие · `on` | `STANDING` |
| checks | check: `TARGET_AILMENT`; word: `CHILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED` |


## POWER_CRUCIBLE_HEAT <a href="#power_crucible_heat" id="power_crucible_heat"></a>

### Жар тигля <a href="#жар-тигля" id="жар-тигля"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждый 4-й удар наносит урон огнём в {v}% вашей брони |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жар тигля](../Stats/Stats-POWER.md#power_crucible_heat) |
| Событие · `on` | `HIT` |
| checks | check: `NTH`; Значение: 4 |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `ARMOUR`; Тип: `FIRE` |


## POWER_COOLING_DRAUGHT <a href="#power_cooling_draught" id="power_cooling_draught"></a>

### Охлаждающий глоток <a href="#охлаждающий-глоток" id="охлаждающий-глоток"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Глоток из фляги даёт +{v}% ко всем сопротивлениям стихиям на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Охлаждающий глоток](../Stats/Stats-POWER.md#power_cooling_draught) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all); Длительность: 4 |


## POWER_CRYPT_CHILL <a href="#power_crypt_chill" id="power_crypt_chill"></a>

### Холод склепа <a href="#холод-склепа" id="холод-склепа"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При получении удара {v}% шанс охладить нападавшего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Холод склепа](../Stats/Stats-POWER.md#power_crypt_chill) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `CHILL` |


## POWER_CORRUPTED_BLOOD <a href="#power_corrupted_blood" id="power_corrupted_blood"></a>

### Порченая кровь <a href="#порченая-кровь" id="порченая-кровь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к пробиванию хаосом за каждого врага под состоянием, до 5 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Порченая кровь](../Stats/Stats-POWER.md#power_corrupted_blood) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Пробивание хаоса](../Stats/Stats-HERO.md#stock_penetrate_chaos); scale: `AILED_FOES`; cap: 5 |


## POWER_CORRUPTION_SURGE <a href="#power_corruption_surge" id="power_corruption_surge"></a>

### Прилив порчи <a href="#прилив-порчи" id="прилив-порчи"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 8 с порча вскипает: +{v}% урона на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Прилив порчи](../Stats/Stats-POWER.md#power_corruption_surge) |
| Событие · `on` | `EVERY` |
| every | 8 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 4 |


## POWER_CONQUERORS_MARK <a href="#power_conquerors_mark" id="power_conquerors_mark"></a>

### Метка завоевателя <a href="#метка-завоевателя" id="метка-завоевателя"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона против редких и уникальных врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Метка завоевателя](../Stats/Stats-POWER.md#power_conquerors_mark) |
| Событие · `on` | `STANDING` |
| checks | check: `TARGET_RARE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage) |


## POWER_CRYSTAL_RESONANCE <a href="#power_crystal_resonance" id="power_crystal_resonance"></a>

### Резонанс кристалла <a href="#резонанс-кристалла" id="резонанс-кристалла"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения урона чар за каждого врага под состоянием, до 5 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Резонанс кристалла](../Stats/Stats-POWER.md#power_crystal_resonance) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон чар](../Stats/Stats-HERO.md#stock_spell_damage); Операция: `INCREASED`; scale: `AILED_FOES`; cap: 5 |


## POWER_CONDUIT <a href="#power_conduit" id="power_conduit"></a>

### Проводник <a href="#проводник" id="проводник"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Шок врага восстанавливает {v}% энергощита |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Проводник](../Stats/Stats-POWER.md#power_conduit) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `SHOCK` |
| Эффекты · `effects` | Действие: `HEAL`; of: `SHIELD` |


## POWER_CRYSTAL_SHATTER <a href="#power_crystal_shatter" id="power_crystal_shatter"></a>

### Хрустальный раскол <a href="#хрустальный-раскол" id="хрустальный-раскол"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитый враг под шоком раскалывается: {v}% его максимума здоровья молнией остальным врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Хрустальный раскол](../Stats/Stats-POWER.md#power_crystal_shatter) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `SHOCK` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `TARGET_LIFE`; Тип: `LIGHTNING`; to: `OTHERS` |


## POWER_CRYSTAL_GROWTH <a href="#power_crystal_growth" id="power_crystal_growth"></a>

### Рост кристаллов <a href="#рост-кристаллов" id="рост-кристаллов"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 6 с вас обрастают кристаллы: барьер в {v}% максимума здоровья на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Рост кристаллов](../Stats/Stats-POWER.md#power_crystal_growth) |
| Событие · `on` | `EVERY` |
| every | 6 |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 4 |


## POWER_CARAPACE_STING <a href="#power_carapace_sting" id="power_carapace_sting"></a>

### Жало панциря <a href="#жало-панциря" id="жало-панциря"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% урона ударов по вам возвращается хаосом всем врагам на поле |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жало панциря](../Stats/Stats-POWER.md#power_carapace_sting) |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `RETALIATE`; Тип: `CHAOS` |


## POWER_CRYSTAL_DRAUGHT <a href="#power_crystal_draught" id="power_crystal_draught"></a>

### Кристальный глоток <a href="#кристальный-глоток" id="кристальный-глоток"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Глоток восстанавливает {v}% максимума энергетического щита |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кристальный глоток](../Stats/Stats-POWER.md#power_crystal_draught) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `HEAL`; of: `SHIELD` |


## POWER_CHOKING_CLOUD <a href="#power_choking_cloud" id="power_choking_cloud"></a>

### Удушливое облако <a href="#удушливое-облако" id="удушливое-облако"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Недуг на вас - и все враги 3 с получают на {v}% больше урона, не чаще раза в 6 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Удушливое облако](../Stats/Stats-POWER.md#power_choking_cloud) |
| Событие · `on` | `AILED` |
| Перезарядка, с · `cooldown` | 6 |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken); Длительность: 3; to: `ALL` |


## POWER_CROWD_FEVER <a href="#power_crowd_fever" id="power_crowd_fever"></a>

### Лихорадка толпы <a href="#лихорадка-толпы" id="лихорадка-толпы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения скорости атаки за каждого врага в бою, до 6 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Лихорадка толпы](../Stats/Stats-POWER.md#power_crowd_fever) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; scale: `FOES`; cap: 6 |


## POWER_CORRUPT_ZEAL <a href="#power_corrupt_zeal" id="power_corrupt_zeal"></a>

### Порченое рвение <a href="#порченое-рвение" id="порченое-рвение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% множителя крита ниже 50% здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Порченое рвение](../Stats/Stats-POWER.md#power_corrupt_zeal) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_BELOW`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Множитель критического удара](../Stats/Stats-HERO.md#stock_critical_multiplier) |


## POWER_CHARTED_SPOILS <a href="#power_charted_spoils" id="power_charted_spoils"></a>

### Нанесённая добыча <a href="#нанесённая-добыча" id="нанесённая-добыча"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения количества предметов с редких монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Нанесённая добыча](../Stats/Stats-POWER.md#power_charted_spoils) |
| world | gain: `QUANTITY`; against: `RARE` |


## POWER_CROWDED_MAP <a href="#power_crowded_map" id="power_crowded_map"></a>

### Людная карта <a href="#людная-карта" id="людная-карта"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Бой против 4 и более врагов начинается с +{v}% урона на 6 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Людная карта](../Stats/Stats-POWER.md#power_crowded_map) |
| Событие · `on` | `FIGHT_START` |
| checks | check: `FOES_AT_LEAST`; Значение: 4 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 6 |


## POWER_COUNCIL_VERDICT <a href="#power_council_verdict" id="power_council_verdict"></a>

### Общий приговор <a href="#общий-приговор" id="общий-приговор"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Общий приговор](../Stats/Stats-POWER.md#power_council_verdict) |
| sheet | Операция: `MORE`; to: [Урон](../Stats/Stats-HERO.md#stock_damage) |


## POWER_COAL_KINDLE <a href="#power_coal_kindle" id="power_coal_kindle"></a>

### Самосожжение <a href="#самосожжение" id="самосожжение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | В начале боя вы поджигаете себя (около 1% макс. здоровья огнём в секунду, {v} с) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Самосожжение](../Stats/Stats-POWER.md#power_coal_kindle) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `IGNITE`; to: `SELF` |


## POWER_COAL_FERVOUR <a href="#power_coal_fervour" id="power_coal_fervour"></a>

### Жгучий пыл <a href="#жгучий-пыл" id="жгучий-пыл"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Пока на вас недуг: {v}% к урону, 20% к скорости атаки и 100% к урону огнём |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жгучий пыл](../Stats/Stats-POWER.md#power_coal_fervour) |
| Событие · `on` | `STANDING` |
| checks | check: `SELF_AILED` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `INCREASED`; 20% увеличение скорости атаки · [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); 100% увеличение урона от огня · [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire) |


## POWER_COAL_HEART <a href="#power_coal_heart" id="power_coal_heart"></a>

### Угольное сердце <a href="#угольное-сердце" id="угольное-сердце"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% физического урона атак превращается в огонь |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Угольное сердце](../Stats/Stats-POWER.md#power_coal_heart) |
| sheet | Операция: `CONVERT`; Предшествующие зоны: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); to: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire) |


## POWER_COIL_GROUNDING <a href="#power_coil_grounding" id="power_coil_grounding"></a>

### Заземление <a href="#заземление" id="заземление"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% физического урона от ударов принимается как молния |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Заземление](../Stats/Stats-POWER.md#power_coil_grounding) |
| sheet | Операция: `ADD`; to: [Физический урон как молния](../Stats/Stats-HERO.md#stock_physical_taken_as_lightning) |


## POWER_COIL_DISCHARGE <a href="#power_coil_discharge" id="power_coil_discharge"></a>

### Разряд <a href="#разряд" id="разряд"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда вас бьют, {v}% шанс шокировать атакующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Разряд](../Stats/Stats-POWER.md#power_coil_discharge) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `SHOCK` |


## POWER_CRYSTAL_STRENGTH <a href="#power_crystal_strength" id="power_crystal_strength"></a>

### Кристальная мощь <a href="#кристальная-мощь" id="кристальная-мощь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к урону за каждые 10 Силы |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кристальная мощь](../Stats/Stats-POWER.md#power_crystal_strength) |
| sheet | Операция: `PER`; Предшествующие зоны: [Сила](../Stats/Stats-HERO.md#stock_strength); to: [Урон](../Stats/Stats-HERO.md#stock_damage); per: 10 |


## POWER_CRYSTAL_MIND <a href="#power_crystal_mind" id="power_crystal_mind"></a>

### Кристальный разум <a href="#кристальный-разум" id="кристальный-разум"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к ЭЩ за каждые 2 Интеллекта |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кристальный разум](../Stats/Stats-POWER.md#power_crystal_mind) |
| sheet | Операция: `PER`; Предшествующие зоны: [Интеллект](../Stats/Stats-HERO.md#stock_intellect); to: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield); per: 2 |


## POWER_CRYSTAL_GRACE <a href="#power_crystal_grace" id="power_crystal_grace"></a>

### Кристальная грация <a href="#кристальная-грация" id="кристальная-грация"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к уклонению за каждую единицу Ловкости |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кристальная грация](../Stats/Stats-POWER.md#power_crystal_grace) |
| sheet | Операция: `PER`; Предшествующие зоны: [Ловкость](../Stats/Stats-HERO.md#stock_agility); to: [Уклонение](../Stats/Stats-HERO.md#stock_evasion) |


## POWER_CARESS_DEVOTION <a href="#power_caress_devotion" id="power_caress_devotion"></a>

### Осквернённая преданность <a href="#осквернённая-преданность" id="осквернённая-преданность"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | За каждый надетый осквернённый предмет (до 8): +{v}% к урону и +4% к броне, уклонению и ЭЩ |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Осквернённая преданность](../Stats/Stats-POWER.md#power_caress_devotion) |
| sheet | Операция: `PER`; Предшествующие зоны: [Надетые осквернённые предметы](../Stats/Stats-HERO.md#stock_worn_corrupted); to: [Урон](../Stats/Stats-HERO.md#stock_damage); cap: 48; Операция: `MORE`; Предшествующие зоны: [Надетые осквернённые предметы](../Stats/Stats-HERO.md#stock_worn_corrupted); to: [Броня](../Stats/Stats-HERO.md#stock_armor); Значение: 4; cap: 32; Операция: `MORE`; Предшествующие зоны: [Надетые осквернённые предметы](../Stats/Stats-HERO.md#stock_worn_corrupted); to: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Значение: 4; cap: 32; Операция: `MORE`; Предшествующие зоны: [Надетые осквернённые предметы](../Stats/Stats-HERO.md#stock_worn_corrupted); to: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield); Значение: 4; cap: 32 |


## POWER_CARRION_FEAST <a href="#power_carrion_feast" id="power_carrion_feast"></a>

### Пир падальщицы <a href="#пир-падальщицы" id="пир-падальщицы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пир падальщицы](../Stats/Stats-POWER.md#power_carrion_feast) |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_CHARITY_SHARE <a href="#power_charity_share" id="power_charity_share"></a>

### Милость смотрителя <a href="#милость-смотрителя" id="милость-смотрителя"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Питомец получает {v}% вашей брони и +20% ко всем стихийным сопротивлениям |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Милость смотрителя](../Stats/Stats-POWER.md#power_charity_share) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Броня](../Stats/Stats-HERO.md#stock_armor); to: [Прибавка брони питомца](../Stats/Stats-HERO.md#stock_pet_armor); Операция: `ADD`; to: [Стихийные сопротивления питомца](../Stats/Stats-HERO.md#stock_pet_resist); Значение: 20 |


## POWER_CHARITY_RALLY <a href="#power_charity_rally" id="power_charity_rally"></a>

### Сплочение <a href="#сплочение" id="сплочение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок даёт питомцу {v}% к урону на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сплочение](../Stats/Stats-POWER.md#power_charity_rally) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `INCREASED`; Длительность: 4; to: `PET` |


## POWER_CARDINAL_SPORES <a href="#power_cardinal_spores" id="power_cardinal_spores"></a>

### Споровая проповедь <a href="#споровая-проповедь" id="споровая-проповедь"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Споровая проповедь](../Stats/Stats-POWER.md#power_cardinal_spores) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all); Длительность: 6; to: `ALL` |


## POWER_CARDINAL_FAITH <a href="#power_cardinal_faith" id="power_cardinal_faith"></a>

### Вера <a href="#вера" id="вера"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия {v}% к шансу крита |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вера](../Stats/Stats-POWER.md#power_cardinal_faith) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED` |


## POWER_CANTORS_BREATH <a href="#power_cantors_breath" id="power_cantors_breath"></a>

### Дыхание кантора <a href="#дыхание-кантора" id="дыхание-кантора"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Низкое здоровье даёт {v}% увеличения скорости сотворения на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Дыхание кантора](../Stats/Stats-POWER.md#power_cantors_breath) |
| Событие · `on` | `LOW_LIFE` |
| Перезарядка, с · `cooldown` | 12 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость применения](../Stats/Stats-HERO.md#stock_cast_speed); Длительность: 4 |


## POWER_CARTOGRAPHERS_LUCK <a href="#power_cartographers_luck" id="power_cartographers_luck"></a>

### Везение землемера <a href="#везение-землемера" id="везение-землемера"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Карт находится на {v}% больше |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Везение землемера](../Stats/Stats-POWER.md#power_cartographers_luck) |
| world | gain: [Map](../../Equipment/Equipment-MAP.md#map) |


## POWER_CRYSTAL_SIGHT <a href="#power_crystal_sight" id="power_crystal_sight"></a>

### Кристальный взор <a href="#кристальный-взор" id="кристальный-взор"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удар по шокированному врагу даёт {v}% шанса крита на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кристальный взор](../Stats/Stats-POWER.md#power_crystal_sight) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_AILMENT`; word: `SHOCK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED`; Длительность: 3 |


## POWER_CHARGED_EDGE <a href="#power_charged_edge" id="power_charged_edge"></a>

### Заряженная кромка <a href="#заряженная-кромка" id="заряженная-кромка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При 3 зарядах ярости — {v}% увеличения шанса крита |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Заряженная кромка](../Stats/Stats-POWER.md#power_charged_edge) |
| Событие · `on` | `STANDING` |
| checks | check: `CHARGES_AT_LEAST`; word: `FRENZY`; Значение: 3 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Шанс критического удара](../Stats/Stats-HERO.md#stock_critical_chance); Операция: `INCREASED` |


## POWER_COLD_CALCULUS <a href="#power_cold_calculus" id="power_cold_calculus"></a>

### Холодный расчёт <a href="#холодный-расчёт" id="холодный-расчёт"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% шанса двойного урона за каждые 200 меткости, до 40% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Холодный расчёт](../Stats/Stats-POWER.md#power_cold_calculus) |
| sheet | Операция: `PER`; Предшествующие зоны: [Меткость](../Stats/Stats-HERO.md#stock_accuracy); to: [Шанс двойного урона](../Stats/Stats-HERO.md#stock_double_damage); per: 200; cap: 40 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
