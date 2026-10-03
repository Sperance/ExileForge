# Особые силы · H

## POWER_HOLLOW_VESSEL <a href="#power_hollow_vessel" id="power_hollow_vessel"></a>

### Полый сосуд <a href="#полый-сосуд" id="полый-сосуд"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | У вас нет маны; даёт энергощит, равный {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Полый сосуд](../Stats/Stats-POWER.md#power_hollow_vessel) |
| sheet | Операция: `SET`; to: [Мана](../Stats/Stats-HERO.md#stock_mana); Значение: 0; Операция: `GAIN`; Предшествующие зоны: [Здоровье](../Stats/Stats-HERO.md#stock_health); to: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield) |


## POWER_HORIZON <a href="#power_horizon" id="power_horizon"></a>

### Горизонт <a href="#горизонт" id="горизонт"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения редкости предметов с уникальных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Горизонт](../Stats/Stats-POWER.md#power_horizon) |
| world | gain: `RARITY`; against: `UNIQUE` |


## POWER_HEADHUNT <a href="#power_headhunt" id="power_headhunt"></a>

### Охота за головами <a href="#охота-за-головами" id="охота-за-головами"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство редкого или уникального врага даёт +{v}% урона и 20% увеличения скорости атаки на 20 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Охота за головами](../Stats/Stats-POWER.md#power_headhunt) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_RARE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); 20% увеличение скорости атаки · [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Длительность: 20 |


## POWER_HYRRIS_TOXIN <a href="#power_hyrris_toxin" id="power_hyrris_toxin"></a>

### Яд Хирри <a href="#яд-хирри" id="яд-хирри"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Охлаждение врага с шансом {v}% вызывает у него кровотечение |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Яд Хирри](../Stats/Stats-POWER.md#power_hyrris_toxin) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `CHILL` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `BLEED` |


## POWER_HEGEMONY <a href="#power_hegemony" id="power_hegemony"></a>

### Гегемония <a href="#гегемония" id="гегемония"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство оглушает остальных врагов на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Гегемония](../Stats/Stats-POWER.md#power_hegemony) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `STUN`; to: `ALL` |


## POWER_HIVEMIND <a href="#power_hivemind" id="power_hivemind"></a>

### Разум роя <a href="#разум-роя" id="разум-роя"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение наносит нападавшему урон хаосом, равный {v}% вашего уклонения |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Разум роя](../Stats/Stats-POWER.md#power_hivemind) |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `EVASION`; Тип: `CHAOS` |


## POWER_HEARTBEAT <a href="#power_heartbeat" id="power_heartbeat"></a>

### Сердцебиение <a href="#сердцебиение" id="сердцебиение"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 3 с восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сердцебиение](../Stats/Stats-POWER.md#power_heartbeat) |
| Событие · `on` | `EVERY` |
| every | 3 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_HOARD_FIRE <a href="#power_hoard_fire" id="power_hoard_fire"></a>

### Пламя сокровищницы <a href="#пламя-сокровищницы" id="пламя-сокровищницы"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство редкого или уникального врага наносит всем врагам урон огнём в {v}% вашего максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пламя сокровищницы](../Stats/Stats-POWER.md#power_hoard_fire) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_RARE` |
| Эффекты · `effects` | Действие: `DAMAGE`; of: `LIFE`; Тип: `FIRE`; to: `ALL` |


## POWER_HOT_HANDS <a href="#power_hot_hands" id="power_hot_hands"></a>

### Горячие руки <a href="#горячие-руки" id="горячие-руки"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения скорости атаки против горящих врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Горячие руки](../Stats/Stats-POWER.md#power_hot_hands) |
| Событие · `on` | `STANDING` |
| checks | check: `TARGET_AILMENT`; word: `IGNITE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED` |


## POWER_HEAVY_LINKS <a href="#power_heavy_links" id="power_heavy_links"></a>

### Тяжёлые звенья <a href="#тяжёлые-звенья" id="тяжёлые-звенья"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тяжёлые звенья](../Stats/Stats-POWER.md#power_heavy_links) |
| Событие · `on` | `STUN` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 4 |


## POWER_HEX_FEAST <a href="#power_hex_feast" id="power_hex_feast"></a>

### Пир проклятий <a href="#пир-проклятий" id="пир-проклятий"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия удары по проклятым врагам возвращают здоровьем {v}% нанесённого урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Пир проклятий](../Stats/Stats-POWER.md#power_hex_feast) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_CURSED` |
| Эффекты · `effects` | Действие: `HEAL`; of: `DEALT` |


## POWER_HOLLOW_ECHO <a href="#power_hollow_echo" id="power_hollow_echo"></a>

### Эхо пустоты <a href="#эхо-пустоты" id="эхо-пустоты"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Когда энергощит разбит, даёт барьер в {v}% максимума здоровья на 3 с, не чаще раза в 8 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Эхо пустоты](../Stats/Stats-POWER.md#power_hollow_echo) |
| Событие · `on` | `SHIELD_BROKEN` |
| Перезарядка, с · `cooldown` | 8 |
| Эффекты · `effects` | Действие: `BARRIER`; Длительность: 3 |


## POWER_HAMMER_HOME <a href="#power_hammer_home" id="power_hammer_home"></a>

### Добить заклёпку <a href="#добить-заклёпку" id="добить-заклёпку"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Оглушение врага бьёт его ещё раз на {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Добить заклёпку](../Stats/Stats-POWER.md#power_hammer_home) |
| Событие · `on` | `STUN` |
| Эффекты · `effects` | Действие: `DAMAGE` |


## POWER_HIDDEN_CACHE <a href="#power_hidden_cache" id="power_hidden_cache"></a>

### Тайник <a href="#тайник" id="тайник"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения золота с редких монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тайник](../Stats/Stats-POWER.md#power_hidden_cache) |
| world | gain: `GOLD`; against: `RARE` |


## POWER_HAILSTORM <a href="#power_hailstorm" id="power_hailstorm"></a>

### Град стрел <a href="#град-стрел" id="град-стрел"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждые 6 с град стрел бьёт всех врагов на {v}% урона оружия |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Град стрел](../Stats/Stats-POWER.md#power_hailstorm) |
| Событие · `on` | `EVERY` |
| every | 6 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `PHYSICAL`; to: `ALL` |


## POWER_HEADCRACK <a href="#power_headcrack" id="power_headcrack"></a>

### Треск черепа <a href="#треск-черепа" id="треск-черепа"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар оглушает на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Треск черепа](../Stats/Stats-POWER.md#power_headcrack) |
| Тип броска · `roll` | `DURATION` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `STUN` |


## POWER_HEARTH_GLOW <a href="#power_hearth_glow" id="power_hearth_glow"></a>

### Отсвет очага <a href="#отсвет-очага" id="отсвет-очага"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Заклинание с шансом {v}% поджигает цель |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отсвет очага](../Stats/Stats-POWER.md#power_hearth_glow) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `SKILL_USE` |
| checks | check: `SPELL` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `IGNITE` |


## POWER_HEART_TAKER <a href="#power_heart_taker" id="power_heart_taker"></a>

### Сердцеед <a href="#сердцеед" id="сердцеед"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство редкого или уникального врага даёт {v}% увеличения максимума здоровья на 20 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сердцеед](../Stats/Stats-POWER.md#power_heart_taker) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_RARE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Здоровье](../Stats/Stats-HERO.md#stock_health); Операция: `INCREASED`; Длительность: 20 |


## POWER_HORIZON_CHASER <a href="#power_horizon_chaser" id="power_horizon_chaser"></a>

### Погоня за горизонтом <a href="#погоня-за-горизонтом" id="погоня-за-горизонтом"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения опыта с редких и уникальных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Погоня за горизонтом](../Stats/Stats-POWER.md#power_horizon_chaser) |
| world | gain: `EXPERIENCE`; against: `RARE`; `UNIQUE` |


## POWER_HELD_GROUND <a href="#power_held_ground" id="power_held_ground"></a>

### Удержанный рубеж <a href="#удержанный-рубеж" id="удержанный-рубеж"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения уклонения, пока против вас не меньше 4 врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Удержанный рубеж](../Stats/Stats-POWER.md#power_held_ground) |
| Событие · `on` | `STANDING` |
| checks | check: `FOES_AT_LEAST`; Значение: 4 |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Операция: `INCREASED` |


## POWER_HIVE_TEMPO <a href="#power_hive_tempo" id="power_hive_tempo"></a>

### Темп улья <a href="#темп-улья" id="темп-улья"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения скорости атаки за каждого врага под недугом, до 4 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Темп улья](../Stats/Stats-POWER.md#power_hive_tempo) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; scale: `AILED_FOES`; cap: 4 |


## POWER_HIVE_VOLLEY <a href="#power_hive_volley" id="power_hive_volley"></a>

### Залп улья <a href="#залп-улья" id="залп-улья"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Каждое 5-е отравление выпускает рой во всех врагов на {v}% урона оружия хаосом |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Залп улья](../Stats/Stats-POWER.md#power_hive_volley) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `POISON`; check: `NTH`; Значение: 5 |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `CHAOS`; to: `ALL` |


## POWER_HAMMERED_BACK <a href="#power_hammered_back" id="power_hammered_back"></a>

### Отбито молотом <a href="#отбито-молотом" id="отбито-молотом"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% урона физических ударов по вам возвращается огнём всем врагам на поле |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Отбито молотом](../Stats/Stats-POWER.md#power_hammered_back) |
| Событие · `on` | `HIT_TAKEN` |
| checks | check: `DAMAGE_TYPE`; word: `PHYSICAL` |
| Эффекты · `effects` | Действие: `RETALIATE`; Тип: `FIRE` |


## POWER_HERETICS_MARK <a href="#power_heretics_mark" id="power_heretics_mark"></a>

### Метка еретика <a href="#метка-еретика" id="метка-еретика"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Заклинание с шансом {v}% проклинает цель |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Метка еретика](../Stats/Stats-POWER.md#power_heretics_mark) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `SKILL_USE` |
| checks | check: `SPELL` |
| Эффекты · `effects` | Действие: `CURSE` |


## POWER_HARP_DIRGE <a href="#power_harp_dirge" id="power_harp_dirge"></a>

### Песнь утопленников <a href="#песнь-утопленников" id="песнь-утопленников"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар: цель 3 с получает на {v}% больше урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Песнь утопленников](../Stats/Stats-POWER.md#power_harp_dirge) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Получаемый урон](../Stats/Stats-HERO.md#stock_damage_taken); Длительность: 3 |


## POWER_HARP_OPENING_CHORD <a href="#power_harp_opening_chord" id="power_harp_opening_chord"></a>

### Первый аккорд <a href="#первый-аккорд" id="первый-аккорд"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к крит. множителю на 5 с в начале боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Первый аккорд](../Stats/Stats-POWER.md#power_harp_opening_chord) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Множитель критического удара](../Stats/Stats-HERO.md#stock_critical_multiplier); Длительность: 5 |


## POWER_HERON_STANCE <a href="#power_heron_stance" id="power_heron_stance"></a>

### Стойка цапли <a href="#стойка-цапли" id="стойка-цапли"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к урону за каждые 450 уклонения |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Стойка цапли](../Stats/Stats-POWER.md#power_heron_stance) |
| sheet | Операция: `PER`; Предшествующие зоны: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); to: [Урон](../Stats/Stats-HERO.md#stock_damage); per: 450 |


## POWER_HERON_STRIKE <a href="#power_heron_strike" id="power_heron_strike"></a>

### Удар цапли <a href="#удар-цапли" id="удар-цапли"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс сразу подготовить атаку при уклонении |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Удар цапли](../Stats/Stats-POWER.md#power_heron_strike) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `RUSH` |


## POWER_HOUND_BITE <a href="#power_hound_bite" id="power_hound_bite"></a>

### Хватка ищейки <a href="#хватка-ищейки" id="хватка-ищейки"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар лечит на {v}% нанесённого урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Хватка ищейки](../Stats/Stats-POWER.md#power_hound_bite) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `HEAL`; of: `DEALT` |


## POWER_HUSH_FROSTBITE <a href="#power_hush_frostbite" id="power_hush_frostbite"></a>

### Обморожение <a href="#обморожение" id="обморожение"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Обморожение](../Stats/Stats-POWER.md#power_hush_frostbite) |
| Событие · `on` | `INFLICT` |
| checks | check: `AILMENT`; word: `CHILL` |
| Эффекты · `effects` | Действие: `HEX`; Свойства: Характеристика: [Сопротивление холоду](../Stats/Stats-HERO.md#stock_resist_cold); Длительность: 4 |


## POWER_HUSH_STILLNESS <a href="#power_hush_stillness" id="power_hush_stillness"></a>

### Недвижность <a href="#недвижность" id="недвижность"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство замороженного врага восстанавливает {v}% максимума маны |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Недвижность](../Stats/Stats-POWER.md#power_hush_stillness) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `FREEZE` |
| Эффекты · `effects` | Действие: `HEAL`; of: `MANA` |


## POWER_HOLLOW_INSTINCT <a href="#power_hollow_instinct" id="power_hollow_instinct"></a>

### Чутьё пустоты <a href="#чутьё-пустоты" id="чутьё-пустоты"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | За каждый пустой слот экипировки: +{v}% к урону, +4% к броне, уклонению и ЭЩ и +2% к скорости атаки и каста |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Чутьё пустоты](../Stats/Stats-POWER.md#power_hollow_instinct) |
| sheet | Операция: `PER`; Предшествующие зоны: [Пустые слоты экипировки](../Stats/Stats-HERO.md#stock_worn_empty_slots); to: [Урон](../Stats/Stats-HERO.md#stock_damage); Операция: `MORE`; Предшествующие зоны: [Пустые слоты экипировки](../Stats/Stats-HERO.md#stock_worn_empty_slots); to: [Броня](../Stats/Stats-HERO.md#stock_armor); Значение: 4; Операция: `MORE`; Предшествующие зоны: [Пустые слоты экипировки](../Stats/Stats-HERO.md#stock_worn_empty_slots); to: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Значение: 4; Операция: `MORE`; Предшествующие зоны: [Пустые слоты экипировки](../Stats/Stats-HERO.md#stock_worn_empty_slots); to: [Энергетический щит](../Stats/Stats-HERO.md#stock_energy_shield); Значение: 4; Операция: `PER`; Предшествующие зоны: [Пустые слоты экипировки](../Stats/Stats-HERO.md#stock_worn_empty_slots); to: [Скорость применения](../Stats/Stats-HERO.md#stock_cast_speed); Значение: 2; Операция: `MORE`; Предшествующие зоны: [Пустые слоты экипировки](../Stats/Stats-HERO.md#stock_worn_empty_slots); to: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Значение: 2 |


## POWER_HOLLOW_LIGHTNESS <a href="#power_hollow_lightness" id="power_hollow_lightness"></a>

### Лёгкость <a href="#лёгкость" id="лёгкость"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к скорости передвижения за каждый пустой слот экипировки |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Лёгкость](../Stats/Stats-POWER.md#power_hollow_lightness) |
| sheet | Операция: `PER`; Предшествующие зоны: [Пустые слоты экипировки](../Stats/Stats-HERO.md#stock_worn_empty_slots); to: [Скорость передвижения](../Stats/Stats-HERO.md#stock_movement_speed) |


## POWER_HEADSMAN <a href="#power_headsman" id="power_headsman"></a>

### Плата палача <a href="#плата-палача" id="плата-палача"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары добивают редких врагов ниже {v}% здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Плата палача](../Stats/Stats-POWER.md#power_headsman) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_RARE` |
| Эффекты · `effects` | Действие: `EXECUTE` |


## POWER_HIVE_SONG <a href="#power_hive_song" id="power_hive_song"></a>

### Песнь улья <a href="#песнь-улья" id="песнь-улья"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение даёт {v}% скорости атаки на 3 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Песнь улья](../Stats/Stats-POWER.md#power_hive_song) |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; Длительность: 3; stacks: 3 |


## POWER_HUNGER_OF_DEPTHS <a href="#power_hunger_of_depths" id="power_hunger_of_depths"></a>

### Голод глубин <a href="#голод-глубин" id="голод-глубин"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по отравленным возвращают {v}% урона здоровьем |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Голод глубин](../Stats/Stats-POWER.md#power_hunger_of_depths) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_AILMENT`; word: `POISON` |
| Эффекты · `effects` | Действие: `HEAL`; of: `DEALT` |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
