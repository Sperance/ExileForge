# Кристальный хребет

| Зона | Уровень | Страж | Открывается из |
| --- | --- | --- | --- |
| [Рудничный посёлок](#c2_miners_village) | 13 | [Бригадир Проходчиков](../reference/Bosses.md#boss_foreman) | [Склеп изгнанников](World-REGION_1.md#c1_crypt_of_exiles) |
| [Кристальные копи](#c2_crystal_mines) | 13 | [Королева Осколков](../reference/Bosses.md#boss_shard_queen) | [Склеп изгнанников](World-REGION_1.md#c1_crypt_of_exiles) |
| [Предгорный тракт](#c2_foothill_road) | 13 | [Белая Рысь](../reference/Bosses.md#boss_white_lynx) | [Склеп изгнанников](World-REGION_1.md#c1_crypt_of_exiles) |
| [Глубинные штольни](#c2_deep_adits) | 15 | [Рудный Голем](../reference/Bosses.md#boss_ore_golem) | [Рудничный посёлок](#c2_miners_village) |
| [Жеодовая лощина](#c2_geode_hollow) | 15 | [Прядильщица Жеод](../reference/Bosses.md#boss_geode_spinner) | [Рудничный посёлок](#c2_miners_village); [Кристальные копи](#c2_crystal_mines) |
| [Замёрзший перевал](#c2_frozen_pass) | 15 | [Ледяной Монарх](../reference/Bosses.md#boss_frost_monarch) | [Кристальные копи](#c2_crystal_mines); [Предгорный тракт](#c2_foothill_road) |
| [Пепельная пустошь](#c2_ashen_wastes) | 15 | [Пепельный Тиран](../reference/Bosses.md#boss_ashen_tyrant) | [Предгорный тракт](#c2_foothill_road) |
| [Ледниковые гробницы](#c2_glacier_tombs) | 17 | [Лич Инея](../reference/Bosses.md#boss_rime_lich) | [Глубинные штольни](#c2_deep_adits) |
| [Тролльи пещеры](#c2_troll_caves) | 17 | [Старейший Тролль](../reference/Bosses.md#boss_elder_troll) | [Глубинные штольни](#c2_deep_adits); [Замёрзший перевал](#c2_frozen_pass) |
| [Тлеющие поля](#c2_ember_fields) | 17 | [Мать Гончих](../reference/Bosses.md#boss_hound_mother) | [Замёрзший перевал](#c2_frozen_pass); [Пепельная пустошь](#c2_ashen_wastes) |
| [Грозовой пик](#c2_storm_peak) | 19 | [Громовержец](../reference/Bosses.md#boss_thunder_caller) | [Ледниковые гробницы](#c2_glacier_tombs) |
| [Затонувшее святилище](#c2_sunken_shrine) | 19 | [Жрица Глубин](../reference/Bosses.md#boss_deep_priestess) | [Ледниковые гробницы](#c2_glacier_tombs); [Тролльи пещеры](#c2_troll_caves) |
| [Обсидиановый карьер](#c2_obsidian_quarry) | 19 | [Обсидиановый Колосс](../reference/Bosses.md#boss_obsidian_colossus) | [Тролльи пещеры](#c2_troll_caves); [Тлеющие поля](#c2_ember_fields) |
| [Шлаковые ямы](#c2_slag_pits) | 19 | [Шлаковый Червь](../reference/Bosses.md#boss_slag_worm) | [Тлеющие поля](#c2_ember_fields) |
| [Ледник Хладоклыка](#c2_frostfang_glacier) | 21 | [Хладоклык](../reference/Bosses.md#boss_frostfang) | [Грозовой пик](#c2_storm_peak) |
| [Лестница в бездну](#c2_abyssal_stair) | 21 | [Глашатай Бездны](../reference/Bosses.md#boss_abyss_herald) | [Затонувшее святилище](#c2_sunken_shrine); [Обсидиановый карьер](#c2_obsidian_quarry) |
| [Тлеющая цитадель](#c2_cinder_citadel) | 21 | [Кастелян Пепла](../reference/Bosses.md#boss_ash_castellan) | [Обсидиановый карьер](#c2_obsidian_quarry) |
| [Храм утонувших](#c2_drowned_temple) | 23 | [Утонувший Бог](../reference/Bosses.md#boss_drowned_god) | [Ледник Хладоклыка](#c2_frostfang_glacier); [Лестница в бездну](#c2_abyssal_stair); [Тлеющая цитадель](#c2_cinder_citadel) |


## C2_MINERS_VILLAGE <a href="#c2_miners_village" id="c2_miners_village"></a>

### Рудничный посёлок <a href="#рудничный-посёлок" id="рудничный-посёлок"></a>

Посёлок у входа в копи: шахтёры ушли вглубь и вернулись другими.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `MINES` |
| Уровень · `level` | 13 |
| Предшествующие зоны · `from` | [Склеп изгнанников](World-REGION_1.md#c1_crypt_of_exiles) |
| Монстры · `monsters` | [Обитатель туннелей](../reference/Monsters.md#tunnel_dweller); [Осколочный ползун](../reference/Monsters.md#shard_scuttler); [Пещерная летучая мышь](../reference/Monsters.md#cave_bat) |
| Количество · `count` | 51; 66 |
| light | 0.85 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](../reference/Tables/Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Бригадир Проходчиков](../reference/Bosses.md#boss_foreman) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](../reference/Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_CRYSTAL_MINES <a href="#c2_crystal_mines" id="c2_crystal_mines"></a>

### Кристальные копи <a href="#кристальные-копи" id="кристальные-копи"></a>

Шахты, где камень потрескивает молниями.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `MINES` |
| Уровень · `level` | 13 |
| Предшествующие зоны · `from` | [Склеп изгнанников](World-REGION_1.md#c1_crypt_of_exiles) |
| Монстры · `monsters` | [Кристальный голем](../reference/Monsters.md#crystal_golem); [Осколочный ползун](../reference/Monsters.md#shard_scuttler); [Обитатель туннелей](../reference/Monsters.md#tunnel_dweller) |
| Количество · `count` | 51; 66 |
| light | 0.75 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](../reference/Tables/Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Королева Осколков](../reference/Bosses.md#boss_shard_queen) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](../reference/Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_FOOTHILL_ROAD <a href="#c2_foothill_road" id="c2_foothill_road"></a>

### Предгорный тракт <a href="#предгорный-тракт" id="предгорный-тракт"></a>

Старая дорога в горы, где снег заметает следы охотников.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `FROST` |
| Уровень · `level` | 13 |
| Предшествующие зоны · `from` | [Склеп изгнанников](World-REGION_1.md#c1_crypt_of_exiles) |
| Монстры · `monsters` | [Снежный охотник](../reference/Monsters.md#snow_stalker); [Одичавший волк](../reference/Monsters.md#feral_wolf); [Обитатель туннелей](../reference/Monsters.md#tunnel_dweller) |
| Количество · `count` | 51; 66 |
| light | 1.1 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](../reference/Tables/Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Белая Рысь](../reference/Bosses.md#boss_white_lynx) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](../reference/Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_DEEP_ADITS <a href="#c2_deep_adits" id="c2_deep_adits"></a>

### Глубинные штольни <a href="#глубинные-штольни" id="глубинные-штольни"></a>

Штольни ниже любой карты, где сама руда начинает двигаться.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `MINES` |
| Уровень · `level` | 15 |
| Предшествующие зоны · `from` | [Рудничный посёлок](#c2_miners_village) |
| Монстры · `monsters` | [Кристальный голем](../reference/Monsters.md#crystal_golem); [Обитатель туннелей](../reference/Monsters.md#tunnel_dweller); [Осколочный ползун](../reference/Monsters.md#shard_scuttler) |
| Количество · `count` | 51; 66 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Рудный Голем](../reference/Bosses.md#boss_ore_golem) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_GEODE_HOLLOW <a href="#c2_geode_hollow" id="c2_geode_hollow"></a>

### Жеодовая лощина <a href="#жеодовая-лощина" id="жеодовая-лощина"></a>

Пещера, выстланная кристаллами, в которых что-то шевелится.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `MINES` |
| Уровень · `level` | 15 |
| Предшествующие зоны · `from` | [Рудничный посёлок](#c2_miners_village); [Кристальные копи](#c2_crystal_mines) |
| Монстры · `monsters` | [Осколочный ползун](../reference/Monsters.md#shard_scuttler); [Кристальный голем](../reference/Monsters.md#crystal_golem); [Пещерная летучая мышь](../reference/Monsters.md#cave_bat) |
| Количество · `count` | 51; 66 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Прядильщица Жеод](../reference/Bosses.md#boss_geode_spinner) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_FROZEN_PASS <a href="#c2_frozen_pass" id="c2_frozen_pass"></a>

### Замёрзший перевал <a href="#замёрзший-перевал" id="замёрзший-перевал"></a>

Горная тропа, где холод убивает быстрее клинка.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `FROST` |
| Уровень · `level` | 15 |
| Предшествующие зоны · `from` | [Кристальные копи](#c2_crystal_mines); [Предгорный тракт](#c2_foothill_road) |
| Монстры · `monsters` | [Ледяной тролль](../reference/Monsters.md#frost_troll); [Ледяной призрак](../reference/Monsters.md#ice_wraith); [Снежный охотник](../reference/Monsters.md#snow_stalker) |
| Количество · `count` | 51; 66 |
| light | 1.1 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Ледяной Монарх](../reference/Bosses.md#boss_frost_monarch) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_ASHEN_WASTES <a href="#c2_ashen_wastes" id="c2_ashen_wastes"></a>

### Пепельная пустошь <a href="#пепельная-пустошь" id="пепельная-пустошь"></a>

Выжженная равнина под тлеющим небом.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ASH` |
| Уровень · `level` | 15 |
| Предшествующие зоны · `from` | [Предгорный тракт](#c2_foothill_road) |
| Монстры · `monsters` | [Угольная гончая](../reference/Monsters.md#cinder_hound); [Пепельный мертвец](../reference/Monsters.md#ash_revenant); [Магмовый слизень](../reference/Monsters.md#magma_slug) |
| Количество · `count` | 51; 66 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Пепельный Тиран](../reference/Bosses.md#boss_ashen_tyrant) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_GLACIER_TOMBS <a href="#c2_glacier_tombs" id="c2_glacier_tombs"></a>

### Ледниковые гробницы <a href="#ледниковые-гробницы" id="ледниковые-гробницы"></a>

Древние гробницы, вмёрзшие в ледник вместе с хозяевами.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `FROST` |
| Уровень · `level` | 17 |
| Предшествующие зоны · `from` | [Глубинные штольни](#c2_deep_adits) |
| Монстры · `monsters` | [Ледяной призрак](../reference/Monsters.md#ice_wraith); [Упырь](../reference/Monsters.md#ghoul); [Снежный охотник](../reference/Monsters.md#snow_stalker) |
| Количество · `count` | 51; 66 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Лич Инея](../reference/Bosses.md#boss_rime_lich) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_TROLL_CAVES <a href="#c2_troll_caves" id="c2_troll_caves"></a>

### Тролльи пещеры <a href="#тролльи-пещеры" id="тролльи-пещеры"></a>

Логово горных троллей, усыпанное костями неудачливых путников.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `FROST` |
| Уровень · `level` | 17 |
| Предшествующие зоны · `from` | [Глубинные штольни](#c2_deep_adits); [Замёрзший перевал](#c2_frozen_pass) |
| Монстры · `monsters` | [Ледяной тролль](../reference/Monsters.md#frost_troll); [Снежный охотник](../reference/Monsters.md#snow_stalker); [Пещерная летучая мышь](../reference/Monsters.md#cave_bat) |
| Количество · `count` | 51; 66 |
| light | 0.85 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Старейший Тролль](../reference/Bosses.md#boss_elder_troll) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_EMBER_FIELDS <a href="#c2_ember_fields" id="c2_ember_fields"></a>

### Тлеющие поля <a href="#тлеющие-поля" id="тлеющие-поля"></a>

Поля, где пепел не остывает, а гончие не спят.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ASH` |
| Уровень · `level` | 17 |
| Предшествующие зоны · `from` | [Замёрзший перевал](#c2_frozen_pass); [Пепельная пустошь](#c2_ashen_wastes) |
| Монстры · `monsters` | [Угольная гончая](../reference/Monsters.md#cinder_hound); [Магмовый слизень](../reference/Monsters.md#magma_slug); [Пепельный мертвец](../reference/Monsters.md#ash_revenant) |
| Количество · `count` | 51; 66 |
| light | 0.95 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Мать Гончих](../reference/Bosses.md#boss_hound_mother) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_STORM_PEAK <a href="#c2_storm_peak" id="c2_storm_peak"></a>

### Грозовой пик <a href="#грозовой-пик" id="грозовой-пик"></a>

Вершина, в которую молнии бьют чаще, чем в землю.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `FROST` |
| Уровень · `level` | 19 |
| Предшествующие зоны · `from` | [Ледниковые гробницы](#c2_glacier_tombs) |
| Монстры · `monsters` | [Ледяной призрак](../reference/Monsters.md#ice_wraith); [Снежный охотник](../reference/Monsters.md#snow_stalker); [Кристальный голем](../reference/Monsters.md#crystal_golem) |
| Количество · `count` | 51; 66 |
| light | 1.2 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Громовержец](../reference/Bosses.md#boss_thunder_caller) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_SUNKEN_SHRINE <a href="#c2_sunken_shrine" id="c2_sunken_shrine"></a>

### Затонувшее святилище <a href="#затонувшее-святилище" id="затонувшее-святилище"></a>

Горное озеро скрыло святилище, но не заставило его жрецов замолчать.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `TEMPLE` |
| Уровень · `level` | 19 |
| Предшествующие зоны · `from` | [Ледниковые гробницы](#c2_glacier_tombs); [Тролльи пещеры](#c2_troll_caves) |
| Монстры · `monsters` | [Глубинный](../reference/Monsters.md#deep_one); [Храмовый фанатик](../reference/Monsters.md#temple_zealot); [Храмовый страж](../reference/Monsters.md#temple_guardian) |
| Количество · `count` | 51; 66 |
| light | 0.85 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Жрица Глубин](../reference/Bosses.md#boss_deep_priestess) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_OBSIDIAN_QUARRY <a href="#c2_obsidian_quarry" id="c2_obsidian_quarry"></a>

### Обсидиановый карьер <a href="#обсидиановый-карьер" id="обсидиановый-карьер"></a>

Карьер, где из застывшей лавы высекают чёрное стекло и чудовищ.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ASH` |
| Уровень · `level` | 19 |
| Предшествующие зоны · `from` | [Тролльи пещеры](#c2_troll_caves); [Тлеющие поля](#c2_ember_fields) |
| Монстры · `monsters` | [Магмовый слизень](../reference/Monsters.md#magma_slug); [Кристальный голем](../reference/Monsters.md#crystal_golem); [Пепельный мертвец](../reference/Monsters.md#ash_revenant) |
| Количество · `count` | 51; 66 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Обсидиановый Колосс](../reference/Bosses.md#boss_obsidian_colossus) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_SLAG_PITS <a href="#c2_slag_pits" id="c2_slag_pits"></a>

### Шлаковые ямы <a href="#шлаковые-ямы" id="шлаковые-ямы"></a>

Ямы с раскалённым шлаком, в котором кто-то живёт.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ASH` |
| Уровень · `level` | 19 |
| Предшествующие зоны · `from` | [Тлеющие поля](#c2_ember_fields) |
| Монстры · `monsters` | [Магмовый слизень](../reference/Monsters.md#magma_slug); [Угольная гончая](../reference/Monsters.md#cinder_hound) |
| Количество · `count` | 51; 66 |
| light | 0.95 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Шлаковый Червь](../reference/Bosses.md#boss_slag_worm) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_FROSTFANG_GLACIER <a href="#c2_frostfang_glacier" id="c2_frostfang_glacier"></a>

### Ледник Хладоклыка <a href="#ледник-хладоклыка" id="ледник-хладоклыка"></a>

Ледяной язык горы, под которым спит змей старше хребта.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `FROST` |
| Уровень · `level` | 21 |
| Предшествующие зоны · `from` | [Грозовой пик](#c2_storm_peak) |
| Монстры · `monsters` | [Ледяной тролль](../reference/Monsters.md#frost_troll); [Ледяной призрак](../reference/Monsters.md#ice_wraith); [Снежный охотник](../reference/Monsters.md#snow_stalker) |
| Количество · `count` | 51; 66 |
| light | 1.15 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Хладоклык](../reference/Bosses.md#boss_frostfang) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_ABYSSAL_STAIR <a href="#c2_abyssal_stair" id="c2_abyssal_stair"></a>

### Лестница в бездну <a href="#лестница-в-бездну" id="лестница-в-бездну"></a>

Ступени, вырубленные вниз, к тому, что молится в темноте.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `TEMPLE` |
| Уровень · `level` | 21 |
| Предшествующие зоны · `from` | [Затонувшее святилище](#c2_sunken_shrine); [Обсидиановый карьер](#c2_obsidian_quarry) |
| Монстры · `monsters` | [Послушник бездны](../reference/Monsters.md#abyssal_acolyte); [Глубинный](../reference/Monsters.md#deep_one); [Храмовый фанатик](../reference/Monsters.md#temple_zealot) |
| Количество · `count` | 51; 66 |
| light | 0.75 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Глашатай Бездны](../reference/Bosses.md#boss_abyss_herald) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_CINDER_CITADEL <a href="#c2_cinder_citadel" id="c2_cinder_citadel"></a>

### Тлеющая цитадель <a href="#тлеющая-цитадель" id="тлеющая-цитадель"></a>

Крепость, сгоревшая много лет назад и так и не догоревшая.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ASH` |
| Уровень · `level` | 21 |
| Предшествующие зоны · `from` | [Обсидиановый карьер](#c2_obsidian_quarry) |
| Монстры · `monsters` | [Пепельный мертвец](../reference/Monsters.md#ash_revenant); [Угольная гончая](../reference/Monsters.md#cinder_hound); [Храмовый страж](../reference/Monsters.md#temple_guardian) |
| Количество · `count` | 51; 66 |
| light | 0.85 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Кастелян Пепла](../reference/Bosses.md#boss_ash_castellan) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C2_DROWNED_TEMPLE <a href="#c2_drowned_temple" id="c2_drowned_temple"></a>

### Храм утонувших <a href="#храм-утонувших" id="храм-утонувших"></a>

Святилище, ушедшее под воду вместе со своими жрецами.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `TEMPLE` |
| Уровень · `level` | 23 |
| Предшествующие зоны · `from` | [Ледник Хладоклыка](#c2_frostfang_glacier); [Лестница в бездну](#c2_abyssal_stair); [Тлеющая цитадель](#c2_cinder_citadel) |
| Финал региона · `finale` | Да |
| Монстры · `monsters` | [Храмовый фанатик](../reference/Monsters.md#temple_zealot); [Глубинный](../reference/Monsters.md#deep_one); [Послушник бездны](../reference/Monsters.md#abyssal_acolyte); [Храмовый страж](../reference/Monsters.md#temple_guardian) |
| Количество · `count` | 51; 66 |
| light | 0.85 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_3](../reference/Tables/Tables-LOOT.md#loot-chest_3) |
| Страж · `boss` | [Утонувший Бог](../reference/Bosses.md#boss_drowned_god) |
| Изначально осквернён · `corrupted` | [Осквернённый Владыка](../reference/Monsters.md#corrupted_3) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/campaign.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
