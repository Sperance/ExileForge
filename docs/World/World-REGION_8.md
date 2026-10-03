# Утонувшая империя

| Зона | Уровень | Страж | Открывается из |
| --- | --- | --- | --- |
| [Затонувшие врата](#c8_sunken_gate) | 81 | [Претор врат](../reference/Bosses.md#boss_gate_praetor) | [Трон разбитого неба](World-REGION_7.md#c7_shattered_throne) |
| [Коралловые сады](#c8_coral_gardens) | 81 | [Коралловая королева](../reference/Bosses.md#boss_coral_queen) | [Трон разбитого неба](World-REGION_7.md#c7_shattered_throne) |
| [Приливные хранилища](#c8_tide_vaults) | 81 | [Страж хранилищ](../reference/Bosses.md#boss_vault_warden) | [Трон разбитого неба](World-REGION_7.md#c7_shattered_throne) |
| [Утонувший форум](#c8_drowned_forum) | 83 | [Утонувший оратор](../reference/Bosses.md#boss_drowned_orator) | [Затонувшие врата](#c8_sunken_gate) |
| [Жемчужный риф](#c8_pearl_reef) | 83 | [Пожиратель жемчуга](../reference/Bosses.md#boss_pearl_eater) | [Затонувшие врата](#c8_sunken_gate); [Коралловые сады](#c8_coral_gardens) |
| [Затопленный акведук](#c8_flooded_aqueduct) | 83 | [Ужас акведука](../reference/Bosses.md#boss_aqueduct_horror) | [Коралловые сады](#c8_coral_gardens); [Приливные хранилища](#c8_tide_vaults) |
| [Казармы легиона](#c8_legion_barracks) | 83 | [Трибун легиона](../reference/Bosses.md#boss_legion_tribune) | [Приливные хранилища](#c8_tide_vaults) |
| [Собор водорослей](#c8_kelp_cathedral) | 85 | [Жрец водорослей](../reference/Bosses.md#boss_kelp_priest) | [Утонувший форум](#c8_drowned_forum); [Жемчужный риф](#c8_pearl_reef) |
| [Запечатанная цистерна](#c8_sealed_cistern) | 85 | [То, что в цистерне](../reference/Bosses.md#boss_cistern_thing) | [Утонувший форум](#c8_drowned_forum); [Жемчужный риф](#c8_pearl_reef); [Затопленный акведук](#c8_flooded_aqueduct) |
| [Затонувший рынок](#c8_sunken_market) | 85 | [Рыночный барон](../reference/Bosses.md#boss_market_baron) | [Жемчужный риф](#c8_pearl_reef); [Затопленный акведук](#c8_flooded_aqueduct); [Казармы легиона](#c8_legion_barracks) |
| [Выбеленный атолл](#c8_bleached_atoll) | 85 | [Пасть атолла](../reference/Bosses.md#boss_atoll_maw) | [Затопленный акведук](#c8_flooded_aqueduct); [Казармы легиона](#c8_legion_barracks) |
| [Машина приливов](#c8_tidal_engine) | 88 | [Инженер приливов](../reference/Bosses.md#boss_tide_engineer) | [Собор водорослей](#c8_kelp_cathedral); [Запечатанная цистерна](#c8_sealed_cistern) |
| [Императорские термы](#c8_imperial_baths) | 88 | [Хозяйка терм](../reference/Bosses.md#boss_bath_matron) | [Запечатанная цистерна](#c8_sealed_cistern); [Затонувший рынок](#c8_sunken_market) |
| [Бездонная впадина](#c8_abyssal_trench) | 88 | [Дрейк впадины](../reference/Bosses.md#boss_trench_drake) | [Затонувший рынок](#c8_sunken_market); [Выбеленный атолл](#c8_bleached_atoll) |
| [Утонувший трон](#c8_drowned_throne) | 90 | [Утонувший император](../reference/Bosses.md#boss_drowned_emperor) | [Машина приливов](#c8_tidal_engine); [Императорские термы](#c8_imperial_baths) |


## C8_SUNKEN_GATE <a href="#c8_sunken_gate" id="c8_sunken_gate"></a>

### Затонувшие врата <a href="#затонувшие-врата" id="затонувшие-врата"></a>

Парадные врата империи под сорока саженями воды.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SUNKEN` |
| Уровень · `level` | 81 |
| Предшествующие зоны · `from` | [Трон разбитого неба](World-REGION_7.md#c7_shattered_throne) |
| Монстры · `monsters` | [Утопленник-легионер](../reference/Monsters.md#drowned_legionary); [Глубинный скрытень](../reference/Monsters.md#deep_lurker); [Солёный дрейк](../reference/Monsters.md#brine_drake) |
| Количество · `count` | 55; 71 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Претор врат](../reference/Bosses.md#boss_gate_praetor) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_CORAL_GARDENS <a href="#c8_coral_gardens" id="c8_coral_gardens"></a>

### Коралловые сады <a href="#коралловые-сады" id="коралловые-сады"></a>

Сады всё ещё цветут — цветами кости и крови.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CORAL` |
| Уровень · `level` | 81 |
| Предшествующие зоны · `from` | [Трон разбитого неба](World-REGION_7.md#c7_shattered_throne) |
| Монстры · `monsters` | [Коралловый исполин](../reference/Monsters.md#coral_behemoth); [Жемчужная сирена](../reference/Monsters.md#pearl_siren); [Бездонный угорь](../reference/Monsters.md#abyssal_eel) |
| Количество · `count` | 55; 71 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Коралловая королева](../reference/Bosses.md#boss_coral_queen) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_TIDE_VAULTS <a href="#c8_tide_vaults" id="c8_tide_vaults"></a>

### Приливные хранилища <a href="#приливные-хранилища" id="приливные-хранилища"></a>

Здесь империя хранила воду и свои тайны.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `TIDEVAULT` |
| Уровень · `level` | 81 |
| Предшествующие зоны · `from` | [Трон разбитого неба](World-REGION_7.md#c7_shattered_throne) |
| Монстры · `monsters` | [Приливный конструкт](../reference/Monsters.md#tide_construct); [Бездонный угорь](../reference/Monsters.md#abyssal_eel); [Солёный дрейк](../reference/Monsters.md#brine_drake) |
| Количество · `count` | 55; 71 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Страж хранилищ](../reference/Bosses.md#boss_vault_warden) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_DROWNED_FORUM <a href="#c8_drowned_forum" id="c8_drowned_forum"></a>

### Утонувший форум <a href="#утонувший-форум" id="утонувший-форум"></a>

Здесь всё ещё произносят речи — мёртвые.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SUNKEN` |
| Уровень · `level` | 83 |
| Предшествующие зоны · `from` | [Затонувшие врата](#c8_sunken_gate) |
| Монстры · `monsters` | [Утопленник-легионер](../reference/Monsters.md#drowned_legionary); [Глубинный скрытень](../reference/Monsters.md#deep_lurker); [Солёный дрейк](../reference/Monsters.md#brine_drake) |
| Количество · `count` | 55; 71 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Утонувший оратор](../reference/Bosses.md#boss_drowned_orator) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_PEARL_REEF <a href="#c8_pearl_reef" id="c8_pearl_reef"></a>

### Жемчужный риф <a href="#жемчужный-риф" id="жемчужный-риф"></a>

Каждая жемчужина здесь когда-то была глазом моряка.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CORAL` |
| Уровень · `level` | 83 |
| Предшествующие зоны · `from` | [Затонувшие врата](#c8_sunken_gate); [Коралловые сады](#c8_coral_gardens) |
| Монстры · `monsters` | [Коралловый исполин](../reference/Monsters.md#coral_behemoth); [Жемчужная сирена](../reference/Monsters.md#pearl_siren); [Бездонный угорь](../reference/Monsters.md#abyssal_eel) |
| Количество · `count` | 55; 71 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Пожиратель жемчуга](../reference/Bosses.md#boss_pearl_eater) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_FLOODED_AQUEDUCT <a href="#c8_flooded_aqueduct" id="c8_flooded_aqueduct"></a>

### Затопленный акведук <a href="#затопленный-акведук" id="затопленный-акведук"></a>

Вода теперь течёт в гору — к трону.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `TIDEVAULT` |
| Уровень · `level` | 83 |
| Предшествующие зоны · `from` | [Коралловые сады](#c8_coral_gardens); [Приливные хранилища](#c8_tide_vaults) |
| Монстры · `monsters` | [Приливный конструкт](../reference/Monsters.md#tide_construct); [Бездонный угорь](../reference/Monsters.md#abyssal_eel); [Солёный дрейк](../reference/Monsters.md#brine_drake) |
| Количество · `count` | 55; 71 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Ужас акведука](../reference/Bosses.md#boss_aqueduct_horror) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_LEGION_BARRACKS <a href="#c8_legion_barracks" id="c8_legion_barracks"></a>

### Казармы легиона <a href="#казармы-легиона" id="казармы-легиона"></a>

Легион не покинул пост. Он лишь утонул.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SUNKEN` |
| Уровень · `level` | 83 |
| Предшествующие зоны · `from` | [Приливные хранилища](#c8_tide_vaults) |
| Монстры · `monsters` | [Утопленник-легионер](../reference/Monsters.md#drowned_legionary); [Глубинный скрытень](../reference/Monsters.md#deep_lurker); [Солёный дрейк](../reference/Monsters.md#brine_drake) |
| Количество · `count` | 55; 71 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Трибун легиона](../reference/Bosses.md#boss_legion_tribune) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_KELP_CATHEDRAL <a href="#c8_kelp_cathedral" id="c8_kelp_cathedral"></a>

### Собор водорослей <a href="#собор-водорослей" id="собор-водорослей"></a>

Гимны морскому богу отдаются эхом под волнами.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CORAL` |
| Уровень · `level` | 85 |
| Предшествующие зоны · `from` | [Утонувший форум](#c8_drowned_forum); [Жемчужный риф](#c8_pearl_reef) |
| Монстры · `monsters` | [Коралловый исполин](../reference/Monsters.md#coral_behemoth); [Жемчужная сирена](../reference/Monsters.md#pearl_siren); [Бездонный угорь](../reference/Monsters.md#abyssal_eel) |
| Количество · `count` | 55; 71 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Жрец водорослей](../reference/Bosses.md#boss_kelp_priest) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_SEALED_CISTERN <a href="#c8_sealed_cistern" id="c8_sealed_cistern"></a>

### Запечатанная цистерна <a href="#запечатанная-цистерна" id="запечатанная-цистерна"></a>

Вместе с водой здесь запечатали что-то ещё.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `TIDEVAULT` |
| Уровень · `level` | 85 |
| Предшествующие зоны · `from` | [Утонувший форум](#c8_drowned_forum); [Жемчужный риф](#c8_pearl_reef); [Затопленный акведук](#c8_flooded_aqueduct) |
| Монстры · `monsters` | [Приливный конструкт](../reference/Monsters.md#tide_construct); [Бездонный угорь](../reference/Monsters.md#abyssal_eel); [Солёный дрейк](../reference/Monsters.md#brine_drake) |
| Количество · `count` | 55; 71 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [То, что в цистерне](../reference/Bosses.md#boss_cistern_thing) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_SUNKEN_MARKET <a href="#c8_sunken_market" id="c8_sunken_market"></a>

### Затонувший рынок <a href="#затонувший-рынок" id="затонувший-рынок"></a>

На прилавках товары, которые никто не купит.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SUNKEN` |
| Уровень · `level` | 85 |
| Предшествующие зоны · `from` | [Жемчужный риф](#c8_pearl_reef); [Затопленный акведук](#c8_flooded_aqueduct); [Казармы легиона](#c8_legion_barracks) |
| Монстры · `monsters` | [Утопленник-легионер](../reference/Monsters.md#drowned_legionary); [Глубинный скрытень](../reference/Monsters.md#deep_lurker); [Солёный дрейк](../reference/Monsters.md#brine_drake) |
| Количество · `count` | 55; 71 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Рыночный барон](../reference/Bosses.md#boss_market_baron) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_BLEACHED_ATOLL <a href="#c8_bleached_atoll" id="c8_bleached_atoll"></a>

### Выбеленный атолл <a href="#выбеленный-атолл" id="выбеленный-атолл"></a>

Риф умер белым, и мёртвый риф голоден.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CORAL` |
| Уровень · `level` | 85 |
| Предшествующие зоны · `from` | [Затопленный акведук](#c8_flooded_aqueduct); [Казармы легиона](#c8_legion_barracks) |
| Монстры · `monsters` | [Коралловый исполин](../reference/Monsters.md#coral_behemoth); [Жемчужная сирена](../reference/Monsters.md#pearl_siren); [Бездонный угорь](../reference/Monsters.md#abyssal_eel) |
| Количество · `count` | 55; 71 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Пасть атолла](../reference/Bosses.md#boss_atoll_maw) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_TIDAL_ENGINE <a href="#c8_tidal_engine" id="c8_tidal_engine"></a>

### Машина приливов <a href="#машина-приливов" id="машина-приливов"></a>

Этим империя двигала море. Оно всё ещё вращается.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `TIDEVAULT` |
| Уровень · `level` | 88 |
| Предшествующие зоны · `from` | [Собор водорослей](#c8_kelp_cathedral); [Запечатанная цистерна](#c8_sealed_cistern) |
| Монстры · `monsters` | [Приливный конструкт](../reference/Monsters.md#tide_construct); [Бездонный угорь](../reference/Monsters.md#abyssal_eel); [Солёный дрейк](../reference/Monsters.md#brine_drake) |
| Количество · `count` | 55; 71 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Инженер приливов](../reference/Bosses.md#boss_tide_engineer) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_IMPERIAL_BATHS <a href="#c8_imperial_baths" id="c8_imperial_baths"></a>

### Императорские термы <a href="#императорские-термы" id="императорские-термы"></a>

Тёплая вода, холодные тела.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SUNKEN` |
| Уровень · `level` | 88 |
| Предшествующие зоны · `from` | [Запечатанная цистерна](#c8_sealed_cistern); [Затонувший рынок](#c8_sunken_market) |
| Монстры · `monsters` | [Утопленник-легионер](../reference/Monsters.md#drowned_legionary); [Глубинный скрытень](../reference/Monsters.md#deep_lurker); [Солёный дрейк](../reference/Monsters.md#brine_drake) |
| Количество · `count` | 55; 71 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Хозяйка терм](../reference/Bosses.md#boss_bath_matron) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_ABYSSAL_TRENCH <a href="#c8_abyssal_trench" id="c8_abyssal_trench"></a>

### Бездонная впадина <a href="#бездонная-впадина" id="бездонная-впадина"></a>

Свет сдаётся задолго до дна.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CORAL` |
| Уровень · `level` | 88 |
| Предшествующие зоны · `from` | [Затонувший рынок](#c8_sunken_market); [Выбеленный атолл](#c8_bleached_atoll) |
| Монстры · `monsters` | [Коралловый исполин](../reference/Monsters.md#coral_behemoth); [Жемчужная сирена](../reference/Monsters.md#pearl_siren); [Бездонный угорь](../reference/Monsters.md#abyssal_eel) |
| Количество · `count` | 55; 71 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Дрейк впадины](../reference/Bosses.md#boss_trench_drake) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C8_DROWNED_THRONE <a href="#c8_drowned_throne" id="c8_drowned_throne"></a>

### Утонувший трон <a href="#утонувший-трон" id="утонувший-трон"></a>

Император всё ещё сидит здесь — и всё ещё в короне.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `TIDEVAULT` |
| Уровень · `level` | 90 |
| Предшествующие зоны · `from` | [Машина приливов](#c8_tidal_engine); [Императорские термы](#c8_imperial_baths) |
| Финал региона · `finale` | Да |
| Монстры · `monsters` | [Приливный конструкт](../reference/Monsters.md#tide_construct); [Бездонный угорь](../reference/Monsters.md#abyssal_eel); [Солёный дрейк](../reference/Monsters.md#brine_drake); [Жемчужная сирена](../reference/Monsters.md#pearl_siren) |
| Количество · `count` | 55; 71 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_8](../reference/Tables/Tables-LOOT.md#loot-chest_8) |
| Страж · `boss` | [Утонувший император](../reference/Bosses.md#boss_drowned_emperor) |
| Изначально осквернён · `corrupted` | [Осквернённый левиафан](../reference/Monsters.md#corrupted_8) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/campaign.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
