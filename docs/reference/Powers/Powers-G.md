# Особые силы · G

## POWER_GRAVE_CHILL <a href="#power_grave_chill" id="power_grave_chill"></a>

### Могильный холод <a href="#могильный-холод" id="могильный-холод"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Могильный холод](../Stats/Stats-POWER.md#power_grave_chill) |
| sheet | Операция: `MORE`; to: [Здоровье](../Stats/Stats-HERO.md#stock_health) |


## POWER_GHOSTLY_ENTRY <a href="#power_ghostly_entry" id="power_ghostly_entry"></a>

### Призрачный вход <a href="#призрачный-вход" id="призрачный-вход"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения уклонения первые 5 с боя |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Призрачный вход](../Stats/Stats-POWER.md#power_ghostly_entry) |
| Событие · `on` | `FIGHT_START` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Операция: `INCREASED`; Длительность: 5 |


## POWER_GLUTTONY <a href="#power_gluttony" id="power_gluttony"></a>

### Чревоугодие <a href="#чревоугодие" id="чревоугодие"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Глоток из фляги ещё и восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Чревоугодие](../Stats/Stats-POWER.md#power_gluttony) |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_GRIEF <a href="#power_grief" id="power_grief"></a>

### Скорбь <a href="#скорбь" id="скорбь"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% урона за каждого убитого в этом бою врага, до 10 |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Скорбь](../Stats/Stats-POWER.md#power_grief) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон](../Stats/Stats-HERO.md#stock_damage); scale: `KILLS`; cap: 10 |


## POWER_GILDED_GREED <a href="#power_gilded_greed" id="power_gilded_greed"></a>

### Позолоченная алчность <a href="#позолоченная-алчность" id="позолоченная-алчность"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% редкости предметов действует и на найденное золото |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Позолоченная алчность](../Stats/Stats-POWER.md#power_gilded_greed) |
| sheet | Операция: `GAIN`; Предшествующие зоны: [Редкость добычи](../Stats/Stats-HERO.md#stock_rarity); to: [Золото](../Stats/Stats-HERO.md#stock_gold) |


## POWER_GLUTTONOUS_HIDE <a href="#power_gluttonous_hide" id="power_gluttonous_hide"></a>

### Ненасытная шкура <a href="#ненасытная-шкура" id="ненасытная-шкура"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ненасытная шкура](../Stats/Stats-POWER.md#power_gluttonous_hide) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_GIANT_BLOOD <a href="#power_giant_blood" id="power_giant_blood"></a>

### Кровь великана <a href="#кровь-великана" id="кровь-великана"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к регенерации здоровья в секунду за каждые 10 силы |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кровь великана](../Stats/Stats-POWER.md#power_giant_blood) |
| sheet | Операция: `PER`; Предшествующие зоны: [Сила](../Stats/Stats-HERO.md#stock_strength); to: [Восстановление здоровья](../Stats/Stats-HERO.md#stock_health_regen); per: 10 |


## POWER_GRAVE_PACT <a href="#power_grave_pact" id="power_grave_pact"></a>

### Могильный договор <a href="#могильный-договор" id="могильный-договор"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт +{v}% вампиризма здоровья на 4 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Могильный договор](../Stats/Stats-POWER.md#power_grave_pact) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Общий вампиризм](../Stats/Stats-HERO.md#stock_leech_all); Длительность: 4 |


## POWER_GODS_BOUNTY <a href="#power_gods_bounty" id="power_gods_bounty"></a>

### Дар бога <a href="#дар-бога" id="дар-бога"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения шанса найти книгу умения |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Дар бога](../Stats/Stats-POWER.md#power_gods_bounty) |
| world | gain: `BOOK` |


## POWER_GILDED_TOUCH <a href="#power_gilded_touch" id="power_gilded_touch"></a>

### Золотое касание <a href="#золотое-касание" id="золотое-касание"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения золота с монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Золотое касание](../Stats/Stats-POWER.md#power_gilded_touch) |
| world | gain: `GOLD` |


## POWER_GOLDEN_PURSE <a href="#power_golden_purse" id="power_golden_purse"></a>

### Тугой кошель <a href="#тугой-кошель" id="тугой-кошель"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v} к силе за каждые 10% редкости предметов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Тугой кошель](../Stats/Stats-POWER.md#power_golden_purse) |
| sheet | Операция: `PER`; Предшествующие зоны: [Редкость добычи](../Stats/Stats-HERO.md#stock_rarity); to: [Сила](../Stats/Stats-HERO.md#stock_strength); per: 10 |


## POWER_GOOD_DOG <a href="#power_good_dog" id="power_good_dog"></a>

### Хороший пёс <a href="#хороший-пёс" id="хороший-пёс"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения количества предметов с обычных и волшебных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Хороший пёс](../Stats/Stats-POWER.md#power_good_dog) |
| world | gain: `QUANTITY`; against: `NORMAL`; `MAGIC` |


## POWER_GEODE_SHELL <a href="#power_geode_shell" id="power_geode_shell"></a>

### Жеодовая скорлупа <a href="#жеодовая-скорлупа" id="жеодовая-скорлупа"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Полученный крит даёт +{v}% ко всем сопротивлениям стихиям на 5 с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Жеодовая скорлупа](../Stats/Stats-POWER.md#power_geode_shell) |
| Событие · `on` | `CRIT_TAKEN` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Все сопротивления стихиям](../Stats/Stats-HERO.md#stock_resist_all); Длительность: 5 |


## POWER_GNAWING_FURY <a href="#power_gnawing_fury" id="power_gnawing_fury"></a>

### Грызущая ярость <a href="#грызущая-ярость" id="грызущая-ярость"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения скорости атаки за каждый удар подряд по одному врагу, до 5 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Грызущая ярость](../Stats/Stats-POWER.md#power_gnawing_fury) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Скорость атаки](../Stats/Stats-HERO.md#stock_attack_speed); Операция: `INCREASED`; scale: `MOMENTUM`; cap: 5 |


## POWER_GULLS_SWERVE <a href="#power_gulls_swerve" id="power_gulls_swerve"></a>

### Чаячий вираж <a href="#чаячий-вираж" id="чаячий-вираж"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Уклонение восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Чаячий вираж](../Stats/Stats-POWER.md#power_gulls_swerve) |
| Событие · `on` | `EVADE` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_GEODE_RESONANCE <a href="#power_geode_resonance" id="power_geode_resonance"></a>

### Кристальный резонанс <a href="#кристальный-резонанс" id="кристальный-резонанс"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удары по шокированным врагам отзываются эхом через 1 с на {v}% урона |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Кристальный резонанс](../Stats/Stats-POWER.md#power_geode_resonance) |
| Событие · `on` | `HIT` |
| checks | check: `TARGET_AILMENT`; word: `SHOCK` |
| Эффекты · `effects` | Действие: [Отзвук](../../Skills/Skills-SCION.md#echo); Длительность: 1 |


## POWER_GRAVE_FROST <a href="#power_grave_frost" id="power_grave_frost"></a>

### Могильный иней <a href="#могильный-иней" id="могильный-иней"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок с шансом {v}% охлаждает всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Могильный иней](../Stats/Stats-POWER.md#power_grave_frost) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `CHILL`; to: `ALL` |


## POWER_GLACIAL_BITE <a href="#power_glacial_bite" id="power_glacial_bite"></a>

### Ледниковый укус <a href="#ледниковый-укус" id="ледниковый-укус"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Глоток с шансом {v}% замораживает всех врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ледниковый укус](../Stats/Stats-POWER.md#power_glacial_bite) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `FLASK` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `FREEZE`; to: `ALL` |


## POWER_GLASS_BREATH <a href="#power_glass_breath" id="power_glass_breath"></a>

### Стеклянный вдох <a href="#стеклянный-вдох" id="стеклянный-вдох"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство врага восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Стеклянный вдох](../Stats/Stats-POWER.md#power_glass_breath) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_GULL_SNATCH <a href="#power_gull_snatch" id="power_gull_snatch"></a>

### Выхватить <a href="#выхватить" id="выхватить"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Выхватить](../Stats/Stats-POWER.md#power_gull_snatch) |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_GALE_SHATTER <a href="#power_gale_shatter" id="power_gale_shatter"></a>

### Раскол <a href="#раскол" id="раскол"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитый замороженный враг раскалывается: {v}% его макс. здоровья уроном холодом по остальным врагам |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Раскол](../Stats/Stats-POWER.md#power_gale_shatter) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `FREEZE` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `COLD`; of: `TARGET_LIFE`; to: `OTHERS` |


## POWER_GALE_SPOILS <a href="#power_gale_spoils" id="power_gale_spoils"></a>

### Вмёрзшая добыча <a href="#вмёрзшая-добыча" id="вмёрзшая-добыча"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% к редкости предметов с редких и уникальных врагов |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Вмёрзшая добыча](../Stats/Stats-POWER.md#power_gale_spoils) |
| world | gain: `RARITY`; against: `RARE`; `UNIQUE` |


## POWER_GIANT_WITHER <a href="#power_giant_wither" id="power_giant_wither"></a>

### Иссохший сок <a href="#иссохший-сок" id="иссохший-сок"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | +{v}% к урону заклинаний за каждые 10 здоровья (до 100%) |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Иссохший сок](../Stats/Stats-POWER.md#power_giant_wither) |
| sheet | Операция: `PER`; Предшествующие зоны: [Здоровье](../Stats/Stats-HERO.md#stock_health); to: [Урон чар](../Stats/Stats-HERO.md#stock_spell_damage); per: 10; cap: 100 |


## POWER_GIANT_ROOTS <a href="#power_giant_roots" id="power_giant_roots"></a>

### Корни великана <a href="#корни-великана" id="корни-великана"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Блок восстанавливает {v}% максимума здоровья |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Корни великана](../Stats/Stats-POWER.md#power_giant_roots) |
| Событие · `on` | `BLOCK` |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_GLACIER_TIP <a href="#power_glacier_tip" id="power_glacier_tip"></a>

### Ледниковые наконечники <a href="#ледниковые-наконечники" id="ледниковые-наконечники"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% физического урона атак превращается в холод |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ледниковые наконечники](../Stats/Stats-POWER.md#power_glacier_tip) |
| sheet | Операция: `CONVERT`; Предшествующие зоны: [Физический урон](../Stats/Stats-HERO.md#stock_attack_physical); to: [Урон холодом](../Stats/Stats-HERO.md#stock_attack_cold) |


## POWER_GLACIER_SHARD <a href="#power_glacier_shard" id="power_glacier_shard"></a>

### Ледяные осколки <a href="#ледяные-осколки" id="ледяные-осколки"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитые замороженные враги раскалываются: {v}% их макс. здоровья уроном холодом по остальным |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Ледяные осколки](../Stats/Stats-POWER.md#power_glacier_shard) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_AILMENT`; word: `FREEZE` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `COLD`; of: `TARGET_LIFE`; to: `OTHERS` |


## POWER_GHOST_CHILL <a href="#power_ghost_chill" id="power_ghost_chill"></a>

### Призрачный холод <a href="#призрачный-холод" id="призрачный-холод"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Призрачный холод](../Stats/Stats-POWER.md#power_ghost_chill) |
| sheet | Операция: `MORE`; to: [Восполнение щита](../Stats/Stats-HERO.md#stock_shield_recharge) |


## POWER_GRANITE_HIDE <a href="#power_granite_hide" id="power_granite_hide"></a>

### Гранитная шкура <a href="#гранитная-шкура" id="гранитная-шкура"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% к урону от ударов при полном здоровье |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Гранитная шкура](../Stats/Stats-POWER.md#power_granite_hide) |
| Событие · `on` | `STANDING` |
| checks | check: `LIFE_FULL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон от ударов](../Stats/Stats-HERO.md#stock_hit_taken) |


## POWER_GRAVE_OATH <a href="#power_grave_oath" id="power_grave_oath"></a>

### Могильная клятва <a href="#могильная-клятва" id="могильная-клятва"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | После смерти возвращает {v}% здоровья, раз за бой |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Могильная клятва](../Stats/Stats-POWER.md#power_grave_oath) |
| Событие · `on` | `DEATH` |
| Перезарядка, с · `cooldown` | -1 |
| Эффекты · `effects` | Действие: `HEAL`; of: `LIFE` |


## POWER_GUARD_DOG <a href="#power_guard_dog" id="power_guard_dog"></a>

### Сторожевой пёс <a href="#сторожевой-пёс" id="сторожевой-пёс"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Удар по вам лечит питомца на {v}% |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сторожевой пёс](../Stats/Stats-POWER.md#power_guard_dog) |
| Событие · `on` | `HIT_TAKEN` |
| Перезарядка, с · `cooldown` | 2 |
| Эффекты · `effects` | Действие: `HEAL`; of: `PET_LIFE`; to: `PET` |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
