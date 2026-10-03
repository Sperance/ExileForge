# Особые силы · B

## POWER_BORROWED_TIME <a href="#power_borrowed_time" id="power_borrowed_time"></a>

### Заёмное время <a href="#заёмное-время" id="заёмное-время"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Падение здоровья до низкого обновляет все перезарядки и восстанавливает {v}% маны, не чаще раза в 20 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Заёмное время](../Stats/Stats-POWER.md#power_borrowed_time) |
| Событие · `on` | `LOW_LIFE` |
| Перезарядка, с · `cooldown` | 20 |
| Эффекты · `effects` | Действие: `COOLDOWNS`; Количество: 999; Действие: `HEAL`; of: `MANA` |


## POWER_BRINE_MEND <a href="#power_brine_mend" id="power_brine_mend"></a>

### Солёная штопка <a href="#солёная-штопка" id="солёная-штопка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к регенерации здоровья в секунду, пока здоровья меньше 35% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Солёная штопка](../Stats/Stats-POWER.md#power_brine_mend) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_BELOW`; Значение: 35 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Восстановление здоровья](../Stats/Stats-HERO.md#stock_health_regen) |


## POWER_BLOOD_SCENT <a href="#power_blood_scent" id="power_blood_scent"></a>

### Запах крови <a href="#запах-крови" id="запах-крови"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт {v}% увеличения скорости атаки на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Запах крови](../Stats/Stats-POWER.md#power_blood_scent) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 4; stacks: 3 |


## POWER_BEAST_FURY <a href="#power_beast_fury" id="power_beast_fury"></a>

### Звериная ярость <a href="#звериная-ярость" id="звериная-ярость"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение даёт {v}% увеличения скорости атаки на 3 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Звериная ярость](../Stats/Stats-POWER.md#power_beast_fury) |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3; stacks: 3 |


## POWER_BONE_CRUNCH <a href="#power_bone_crunch" id="power_bone_crunch"></a>

### Хруст костей <a href="#хруст-костей" id="хруст-костей"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критические удары оглушают на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Хруст костей](../Stats/Stats-POWER.md#power_bone_crunch) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `STUN` |


## POWER_BEREKS_STORM <a href="#power_bereks_storm" id="power_bereks_storm"></a>

### Буря Берека <a href="#буря-берека" id="буря-берека"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Шок врага с шансом {v}% ещё и охлаждает его |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Буря Берека](../Stats/Stats-POWER.md#power_bereks_storm) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `SHOCK` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `CHILL` |


## POWER_BEREKS_FROST <a href="#power_bereks_frost" id="power_bereks_frost"></a>

### Мороз Берека <a href="#мороз-берека" id="мороз-берека"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство охлаждённого врага восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Мороз Берека](../Stats/Stats-POWER.md#power_bereks_frost) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `CHILL` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_BISCOS_FIND <a href="#power_biscos_find" id="power_biscos_find"></a>

### Находка Биско <a href="#находка-биско" id="находка-биско"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения редкости предметов с обычных и волшебных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Находка Биско](../Stats/Stats-POWER.md#power_biscos_find) |
| world | gain: `RARITY`; against: `NORMAL`; `MAGIC` |


## POWER_BINOS_SPREAD <a href="#power_binos_spread" id="power_binos_spread"></a>

### Рецепт Бино <a href="#рецепт-бино" id="рецепт-бино"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство отравленного врага с шансом {v}% переносит его состояния на остальных врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Рецепт Бино](../Stats/Stats-POWER.md#power_binos_spread) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `POISON` |
| Эффекты · `effects` | Действие: `SPREAD` |


## POWER_BRINESHELL <a href="#power_brineshell" id="power_brineshell"></a>

### Солёный панцирь <a href="#солёный-панцирь" id="солёный-панцирь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок даёт барьер в {v}% максимума здоровья на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Солёный панцирь](../Stats/Stats-POWER.md#power_brineshell) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_BOG_BREATH <a href="#power_bog_breath" id="power_bog_breath"></a>

### Болотное дыхание <a href="#болотное-дыхание" id="болотное-дыхание"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 4 с с шансом {v}% отравляет всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Болотное дыхание](../Stats/Stats-POWER.md#power_bog_breath) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `EVERY` |
| every | 4 |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `POISON`; to: `ALL` |


## POWER_BONE_TITHE <a href="#power_bone_tithe" id="power_bone_tithe"></a>

### Костяная десятина <a href="#костяная-десятина" id="костяная-десятина"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство восстанавливает {v}% маны |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Костяная десятина](../Stats/Stats-POWER.md#power_bone_tithe) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `HEAL`; of: `MANA` |


## POWER_BRAZEN_CALL <a href="#power_brazen_call" id="power_brazen_call"></a>

### Дерзкий вызов <a href="#дерзкий-вызов" id="дерзкий-вызов"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Враги, чей удар вы заблокировали, получают на {v}% больше урона 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Дерзкий вызов](../Stats/Stats-POWER.md#power_brazen_call) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken); Длительность: 4 |


## POWER_BLOOD_FRENZY <a href="#power_blood_frenzy" id="power_blood_frenzy"></a>

### Кровавое неистовство <a href="#кровавое-неистовство" id="кровавое-неистовство"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Кровотечение у врага даёт {v}% увеличения скорости атаки на 3 с, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кровавое неистовство](../Stats/Stats-POWER.md#power_blood_frenzy) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `BLEED` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3; stacks: 5 |


## POWER_BLOODBATH <a href="#power_bloodbath" id="power_bloodbath"></a>

### Кровавая баня <a href="#кровавая-баня" id="кровавая-баня"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство кровоточащего врага восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кровавая баня](../Stats/Stats-POWER.md#power_bloodbath) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `BLEED` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_BELLOWS_GUST <a href="#power_bellows_gust" id="power_bellows_gust"></a>

### Порыв мехов <a href="#порыв-мехов" id="порыв-мехов"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Глоток из фляги раздувает пламя: всем врагам {v}% урона оружия огнём |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Порыв мехов](../Stats/Stats-POWER.md#power_bellows_gust) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `FIRE`; to: `ALL` |


## POWER_BRAZEN_ROAR <a href="#power_brazen_roar" id="power_brazen_roar"></a>

### Дерзкий рёв <a href="#дерзкий-рёв" id="дерзкий-рёв"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Дерзкий рёв](../Stats/Stats-POWER.md#power_brazen_roar) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 4; to: `ALL` |


## POWER_BLIND_FURY <a href="#power_blind_fury" id="power_blind_fury"></a>

### Слепая ярость <a href="#слепая-ярость" id="слепая-ярость"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия удары с шансом {v}% вызывают кровотечение |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Слепая ярость](../Stats/Stats-POWER.md#power_blind_fury) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `BLEED` |


## POWER_BRAMBLE_GUARD <a href="#power_bramble_guard" id="power_bramble_guard"></a>

### Колючая защита <a href="#колючая-защита" id="колючая-защита"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Шипы, равные {v}% уклонения |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Колючая защита](../Stats/Stats-POWER.md#power_bramble_guard) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); to: [Шипы](../Stats/Stats-HERO.md#stock_thorns) |


## POWER_BUZZING_CLOUD <a href="#power_buzzing_cloud" id="power_buzzing_cloud"></a>

### Жужжащее облако <a href="#жужжащее-облако" id="жужжащее-облако"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 4 с с шансом {v}% отравляет случайного врага |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жужжащее облако](../Stats/Stats-POWER.md#power_buzzing_cloud) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `EVERY` |
| every | 4 |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `POISON`; to: `RANDOM` |


## POWER_BLOOD_ALTAR <a href="#power_blood_altar" id="power_blood_altar"></a>

### Кровавый алтарь <a href="#кровавый-алтарь" id="кровавый-алтарь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона, пока здоровья меньше 50% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кровавый алтарь](../Stats/Stats-POWER.md#power_blood_altar) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_BELOW`; Значение: 50 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage) |


## POWER_BEAM_MARK <a href="#power_beam_mark" id="power_beam_mark"></a>

### Пойман лучом <a href="#пойман-лучом" id="пойман-лучом"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение метит атакующего: 3 с он получает на {v}% больше урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пойман лучом](../Stats/Stats-POWER.md#power_beam_mark) |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken); Длительность: 3 |


## POWER_BARROW_WARD <a href="#power_barrow_ward" id="power_barrow_ward"></a>

### Курганный заслон <a href="#курганный-заслон" id="курганный-заслон"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | На низком здоровье - заслон в {v}% максимума здоровья на 3 с, не чаще раза в 15 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Курганный заслон](../Stats/Stats-POWER.md#power_barrow_ward) |
| Событие · `on` | `LOW_LIFE` |
| Перезарядка, с · `cooldown` | 15 |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_BRINE_SPRAY <a href="#power_brine_spray" id="power_brine_spray"></a>

### Солёные брызги <a href="#солёные-брызги" id="солёные-брызги"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | С шансом {v}% при ударе по вам охлаждает атакующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Солёные брызги](../Stats/Stats-POWER.md#power_brine_spray) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `CHILL` |


## POWER_BUTCHERS_RHYTHM <a href="#power_butchers_rhythm" id="power_butchers_rhythm"></a>

### Ритм мясника <a href="#ритм-мясника" id="ритм-мясника"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения физического урона за каждый удар подряд по одному врагу, до 10 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ритм мясника](../Stats/Stats-POWER.md#power_butchers_rhythm) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); Операция: `INCREASED`; scale: `MOMENTUM`; cap: 10 |


## POWER_BLOODED_TAKINGS <a href="#power_blooded_takings" id="power_blooded_takings"></a>

### Кровавая добыча <a href="#кровавая-добыча" id="кровавая-добыча"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство кровоточащего врага восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кровавая добыча](../Stats/Stats-POWER.md#power_blooded_takings) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `BLEED` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_BLOOD_TITHE <a href="#power_blood_tithe" id="power_blood_tithe"></a>

### Кровавая десятина <a href="#кровавая-десятина" id="кровавая-десятина"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Глоток стоит 10% максимума здоровья и даёт +{v}% урона на 5 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кровавая десятина](../Stats/Stats-POWER.md#power_blood_tithe) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `HURT`; of: `LIFE`; Количество: 10; Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); Длительность: 5 |


## POWER_BESIEGED_GUARD <a href="#power_besieged_guard" id="power_besieged_guard"></a>

### Осаждённый заслон <a href="#осаждённый-заслон" id="осаждённый-заслон"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок против 3 и более врагов даёт заслон в {v}% максимума здоровья на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Осаждённый заслон](../Stats/Stats-POWER.md#power_besieged_guard) |
| Событие · `on` | `BLOCK` |
| checks | check: `FOES_AT_LEAST`; Значение: 3 |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_BEACON_GLEAM <a href="#power_beacon_gleam" id="power_beacon_gleam"></a>

### Отсвет маяка <a href="#отсвет-маяка" id="отсвет-маяка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% к скорости каста на 4 с в начале боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отсвет маяка](../Stats/Stats-POWER.md#power_beacon_gleam) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость применения](../Stats/Stats-HERO.md#stock_cast_speed); Операция: `INCREASED`; Длительность: 4 |


## POWER_BONE_BINDING <a href="#power_bone_binding" id="power_bone_binding"></a>

### Костяная связка <a href="#костяная-связка" id="костяная-связка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Ваш питомец: +1 уровень и {v}% к максимуму здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Костяная связка](../Stats/Stats-POWER.md#power_bone_binding) |
| sheet | Операция: `ADD`; to: [Уровень питомца](../Stats/Stats-HERO.md#stock_pet_level); Значение: 1; Операция: `ADD`; to: [Здоровье питомца](../Stats/Stats-HERO.md#stock_pet_health) |


## POWER_BONE_BURST <a href="#power_bone_burst" id="power_bone_burst"></a>

### Костяной взрыв <a href="#костяной-взрыв" id="костяной-взрыв"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Погибший питомец взрывается: {v}% его макс. здоровья уроном хаосом по всем врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Костяной взрыв](../Stats/Stats-POWER.md#power_bone_burst) |
| Событие · `on` | `PET_DEATH` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `CHAOS`; of: `PET_LIFE`; to: `ALL` |


## POWER_BLIND_DEVOTION <a href="#power_blind_devotion" id="power_blind_devotion"></a>

### Слепая вера <a href="#слепая-вера" id="слепая-вера"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% ваших увеличений урона заклинаний действуют на весь урон |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Слепая вера](../Stats/Stats-POWER.md#power_blind_devotion) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Урон чар](../Stats/Stats-HERO.md#stock_spell_damage); to: [Урон](../Stats/Stats-HERO.md#stock_damage) |


## POWER_BILGE_SCURRY <a href="#power_bilge_scurry" id="power_bilge_scurry"></a>

### Шмыг <a href="#шмыг" id="шмыг"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение даёт {v}% к скорости атаки на 3 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Шмыг](../Stats/Stats-POWER.md#power_bilge_scurry) |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3 |


## POWER_BILGE_PLAGUE <a href="#power_bilge_plague" id="power_bilge_plague"></a>

### Трюмная чума <a href="#трюмная-чума" id="трюмная-чума"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс отравить при ударе |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Трюмная чума](../Stats/Stats-POWER.md#power_bilge_plague) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `POISON` |


## POWER_BARON_MARROW <a href="#power_baron_marrow" id="power_baron_marrow"></a>

### Костный мозг барона <a href="#костный-мозг-барона" id="костный-мозг-барона"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Питомец получает +{v}% к максимуму здоровья за каждые 10 Силы |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Костный мозг барона](../Stats/Stats-POWER.md#power_baron_marrow) |
| sheet | Операция: `PER`; Предшествующие зоны: [Сила](../Stats/Stats-HERO.md#stock_strength); to: [Здоровье питомца](../Stats/Stats-HERO.md#stock_pet_health); per: 10 |


## POWER_BARON_LEGION <a href="#power_baron_legion" id="power_baron_legion"></a>

### Легион катакомб <a href="#легион-катакомб" id="легион-катакомб"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Питомец получает +{v} уровень за каждые 50 Силы (до +3) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Легион катакомб](../Stats/Stats-POWER.md#power_baron_legion) |
| sheet | Операция: `PER`; Предшествующие зоны: [Сила](../Stats/Stats-HERO.md#stock_strength); to: [Уровень питомца](../Stats/Stats-HERO.md#stock_pet_level); per: 50; cap: 3 |


## POWER_BIT_OF_LUCK <a href="#power_bit_of_luck" id="power_bit_of_luck"></a>

### Немного удачи <a href="#немного-удачи" id="немного-удачи"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт что-то одно: 2% здоровья, 5% маны, 10% к скорости атаки на 3 с или случайный заряд |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Немного удачи](../Stats/Stats-POWER.md#power_bit_of_luck) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `ONE_OF`; Варианты: Действие: `HEAL`; of: `LIFE`; Количество: 2; Действие: `HEAL`; of: `MANA`; Количество: 5; Действие: `BUFF`; Свойства: 10% увеличение скорости атаки · [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Длительность: 3; Действие: `CHARGE`; charge: `RANDOM` |


## POWER_BIT_OF_GOLD <a href="#power_bit_of_gold" id="power_bit_of_gold"></a>

### Мелочь в кармане <a href="#мелочь-в-кармане" id="мелочь-в-кармане"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Враги роняют на {v}% больше золота |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Мелочь в кармане](../Stats/Stats-POWER.md#power_bit_of_gold) |
| world | gain: `GOLD` |


## POWER_BROTHERHOOD_CURRENT <a href="#power_brotherhood_current" id="power_brotherhood_current"></a>

### Холодное течение <a href="#холодное-течение" id="холодное-течение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% урона молнией превращается в холод |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Холодное течение](../Stats/Stats-POWER.md#power_brotherhood_current) |
| sheet | Операция: `CONVERT`; Предшествующие зоны: [Урон молнией](../Stats/Stats-HERO.md#stock_attack_lightning); to: [Урон холодом](../Stats/Stats-HERO.md#stock_attack_cold) |


## POWER_BROTHERHOOD_MIST <a href="#power_brotherhood_mist" id="power_brotherhood_mist"></a>

### Туман переправы <a href="#туман-переправы" id="туман-переправы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс при ударе охладить врага: −10% к скорости атаки на 2 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Туман переправы](../Stats/Stats-POWER.md#power_brotherhood_mist) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: -10% увеличение скорости атаки · [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Длительность: 2 |


## POWER_BLACKFLAME_PACT <a href="#power_blackflame_pact" id="power_blackflame_pact"></a>

### Чёрное пламя <a href="#чёрное-пламя" id="чёрное-пламя"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Ваши поджоги наносят урон хаосом вместо огня |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Чёрное пламя](../Stats/Stats-POWER.md#power_blackflame_pact) |
| sheet | Операция: `ADD`; to: [Поджоги хаосом](../Stats/Stats-HERO.md#stock_ignite_as_chaos) |


## POWER_BLACKFLAME_KINDLE <a href="#power_blackflame_kindle" id="power_blackflame_kindle"></a>

### Тление <a href="#тление" id="тление"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс поджечь при ударе |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тление](../Stats/Stats-POWER.md#power_blackflame_kindle) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `IGNITE` |


## POWER_BEACON_FLARE <a href="#power_beacon_flare" id="power_beacon_flare"></a>

### Сигнальный огонь <a href="#сигнальный-огонь" id="сигнальный-огонь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% физического урона атак превращается в огонь |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сигнальный огонь](../Stats/Stats-POWER.md#power_beacon_flare) |
| sheet | Операция: `CONVERT`; Предшествующие зоны: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); to: [Урон огнём](../Stats/Stats-HERO.md#stock_attack_fire) |


## POWER_BEACON_CALL <a href="#power_beacon_call" id="power_beacon_call"></a>

### Зов маяка <a href="#зов-маяка" id="зов-маяка"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | В начале боя все враги 3 с получают на {v}% больше урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Зов маяка](../Stats/Stats-POWER.md#power_beacon_call) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken); Длительность: 3; to: `ALL` |


## POWER_BOG_DRAUGHT_GULP <a href="#power_bog_draught_gulp" id="power_bog_draught_gulp"></a>

### Запретный глоток <a href="#запретный-глоток" id="запретный-глоток"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | При применении мгновенно восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Запретный глоток](../Stats/Stats-POWER.md#power_bog_draught_gulp) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_BOG_DRAUGHT_ROT <a href="#power_bog_draught_rot" id="power_bog_draught_rot"></a>

### Болотная гниль <a href="#болотная-гниль" id="болотная-гниль"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Затем 4 с вы теряете {v}% максимума здоровья в секунду (хаос) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Болотная гниль](../Stats/Stats-POWER.md#power_bog_draught_rot) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Потеря здоровья, %](../Stats/Stats-HERO.md#stock_life_degen_percent); Длительность: 4 |


## POWER_BUTCHER_ANATOMY <a href="#power_butcher_anatomy" id="power_butcher_anatomy"></a>

### Анатомия <a href="#анатомия" id="анатомия"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к максимуму здоровья за каждые 3 Интеллекта |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Анатомия](../Stats/Stats-POWER.md#power_butcher_anatomy) |
| sheet | Операция: `PER`; Предшествующие зоны: [Интеллект](../Stats/Stats-HERO.md#stock_intellect); to: [Здоровье](../Stats/Stats-HERO.md#stock_health); per: 3 |


## POWER_BLACK_SAP <a href="#power_black_sap" id="power_black_sap"></a>

### Чёрный сок <a href="#чёрный-сок" id="чёрный-сок"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийства дают {v}% физического урона как доп. хаос на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Чёрный сок](../Stats/Stats-POWER.md#power_black_sap) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Физический как доп. хаос](../Stats/Stats-HERO.md#stock_physical_as_extra_chaos); Длительность: 4 |


## POWER_BLOOD_HEX <a href="#power_blood_hex" id="power_blood_hex"></a>

### Кровавый сглаз <a href="#кровавый-сглаз" id="кровавый-сглаз"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по вам с шансом {v}% проклинают атакующего |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кровавый сглаз](../Stats/Stats-POWER.md#power_blood_hex) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT_TAKEN` |
| Эффекты · `effects` | Действие: `CURSE` |


## POWER_BLADE_DANCE <a href="#power_blade_dance" id="power_blade_dance"></a>

### Танец клинков <a href="#танец-клинков" id="танец-клинков"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% отклонения за каждые 500 уклонения, до 30%; блока нет |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Танец клинков](../Stats/Stats-POWER.md#power_blade_dance) |
| sheet | Операция: `PER`; Предшествующие зоны: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); to: [Отклонение](../Stats/Stats-HERO.md#stock_deflection); per: 500; cap: 30; Операция: `SET`; to: [Шанс блока](../Stats/Stats-HERO.md#stock_block_chance); Значение: 0 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
