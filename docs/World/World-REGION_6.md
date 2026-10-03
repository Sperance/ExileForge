# Бездна

| Зона | Уровень | Страж | Открывается из |
| --- | --- | --- | --- |
| [Край разлома](#c6_rift_edge) | 61 | [Страж разлома](../reference/Bosses.md#boss_rift_watcher) | [Цитадель Пламени](World-REGION_5.md#c5_flame_citadel) |
| [Чумные поля](#c6_blighted_fields) | 61 | [Чумной жнец](../reference/Bosses.md#boss_plague_reaper) | [Цитадель Пламени](World-REGION_5.md#c5_flame_citadel) |
| [Покинутое аббатство](#c6_forsaken_abbey) | 61 | [Падший аббат](../reference/Bosses.md#boss_fallen_abbot) | [Цитадель Пламени](World-REGION_5.md#c5_flame_citadel) |
| [Шепчущая пропасть](#c6_whispering_chasm) | 63 | [Шептун](../reference/Bosses.md#boss_whisperer) | [Край разлома](#c6_rift_edge) |
| [Гниющий сад](#c6_rotting_orchard) | 63 | [Садовник гнили](../reference/Bosses.md#boss_rot_gardener) | [Край разлома](#c6_rift_edge); [Чумные поля](#c6_blighted_fields) |
| [Чумная деревня](#c6_plague_village) | 63 | [Чумной лекарь](../reference/Bosses.md#boss_plague_doctor) | [Чумные поля](#c6_blighted_fields); [Покинутое аббатство](#c6_forsaken_abbey) |
| [Костница](#c6_ossuary) | 63 | [Собиратель костей](../reference/Bosses.md#boss_bone_collector) | [Покинутое аббатство](#c6_forsaken_abbey) |
| [Берег пустоты](#c6_void_shore) | 65 | [Кракен пустоты](../reference/Bosses.md#boss_void_kraken) | [Шепчущая пропасть](#c6_whispering_chasm) |
| [Споровый собор](#c6_spore_cathedral) | 65 | [Споровый кардинал](../reference/Bosses.md#boss_spore_cardinal) | [Гниющий сад](#c6_rotting_orchard); [Чумная деревня](#c6_plague_village) |
| [Павшая звезда](#c6_fallen_star) | 65 | [Звёздное отродье](../reference/Bosses.md#boss_star_spawn) | [Чумная деревня](#c6_plague_village) |
| [Пасть катакомб](#c6_catacomb_maw) | 67 | [Червь катакомб](../reference/Bosses.md#boss_catacomb_worm) | [Берег пустоты](#c6_void_shore) |
| [Иссохший лес](#c6_withered_wood) | 67 | [Иссохший великан](../reference/Bosses.md#boss_withered_giant) | [Берег пустоты](#c6_void_shore); [Споровый собор](#c6_spore_cathedral) |
| [Алтарь затмения](#c6_eclipse_altar) | 67 | [Жрица затмения](../reference/Bosses.md#boss_eclipse_priestess) | [Споровый собор](#c6_spore_cathedral); [Павшая звезда](#c6_fallen_star) |
| [Беззвёздная глубь](#c6_starless_deep) | 67 | [Безглазый](../reference/Bosses.md#boss_eyeless_one) | [Павшая звезда](#c6_fallen_star) |
| [Гнилое сердце](#c6_rotten_heart) | 69 | [Мать гнили](../reference/Bosses.md#boss_mother_blight) | [Иссохший лес](#c6_withered_wood) |
| [Завеса теней](#c6_veil_of_shadows) | 69 | [Ткач завесы](../reference/Bosses.md#boss_veil_weaver) | [Иссохший лес](#c6_withered_wood); [Алтарь затмения](#c6_eclipse_altar) |
| [Бесконечная лестница](#c6_endless_stair) | 69 | [Вечный привратник](../reference/Bosses.md#boss_endless_warden) | [Алтарь затмения](#c6_eclipse_altar); [Беззвёздная глубь](#c6_starless_deep) |
| [Трон бездны](#c6_abyssal_throne) | 70 | [Король бездны](../reference/Bosses.md#boss_abyss_king) | [Гнилое сердце](#c6_rotten_heart); [Завеса теней](#c6_veil_of_shadows); [Бесконечная лестница](#c6_endless_stair) |


## C6_RIFT_EDGE <a href="#c6_rift_edge" id="c6_rift_edge"></a>

### Край разлома <a href="#край-разлома" id="край-разлома"></a>

Земля обрывается в пустоту, и пустота смотрит в ответ.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ABYSS` |
| Уровень · `level` | 61 |
| Предшествующие зоны · `from` | [Цитадель Пламени](World-REGION_5.md#c5_flame_citadel) |
| Монстры · `monsters` | [Порождение пустоты](../reference/Monsters.md#void_spawn); [Ползун бездны](../reference/Monsters.md#abyss_crawler); [Тень бездны](../reference/Monsters.md#abyssal_shade) |
| Количество · `count` | 51; 66 |
| light | 0.6 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Страж разлома](../reference/Bosses.md#boss_rift_watcher) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_BLIGHTED_FIELDS <a href="#c6_blighted_fields" id="c6_blighted_fields"></a>

### Чумные поля <a href="#чумные-поля" id="чумные-поля"></a>

Урожай сгнил на корню, а гниль пошла дальше — в людей.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `BLIGHT` |
| Уровень · `level` | 61 |
| Предшествующие зоны · `from` | [Цитадель Пламени](World-REGION_5.md#c5_flame_citadel) |
| Монстры · `monsters` | [Гнилошляп](../reference/Monsters.md#blightcap); [Чумоносец](../reference/Monsters.md#plague_bearer); [Чумная оса](../reference/Monsters.md#plague_wasp) |
| Количество · `count` | 51; 66 |
| light | 0.75 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Чумной жнец](../reference/Bosses.md#boss_plague_reaper) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_FORSAKEN_ABBEY <a href="#c6_forsaken_abbey" id="c6_forsaken_abbey"></a>

### Покинутое аббатство <a href="#покинутое-аббатство" id="покинутое-аббатство"></a>

Монахи молились, чтобы бездна их не заметила. Она заметила.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `TEMPLE` |
| Уровень · `level` | 61 |
| Предшествующие зоны · `from` | [Цитадель Пламени](World-REGION_5.md#c5_flame_citadel) |
| Монстры · `monsters` | [Чумоносец](../reference/Monsters.md#plague_bearer); [Тень бездны](../reference/Monsters.md#abyssal_shade); [Порождение пустоты](../reference/Monsters.md#void_spawn) |
| Количество · `count` | 51; 66 |
| light | 0.85 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Падший аббат](../reference/Bosses.md#boss_fallen_abbot) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_WHISPERING_CHASM <a href="#c6_whispering_chasm" id="c6_whispering_chasm"></a>

### Шепчущая пропасть <a href="#шепчущая-пропасть" id="шепчущая-пропасть"></a>

Из глубины доносится шёпот, и он знает ваше имя.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ABYSS` |
| Уровень · `level` | 63 |
| Предшествующие зоны · `from` | [Край разлома](#c6_rift_edge) |
| Монстры · `monsters` | [Тень бездны](../reference/Monsters.md#abyssal_shade); [Порождение пустоты](../reference/Monsters.md#void_spawn); [Ползун бездны](../reference/Monsters.md#abyss_crawler) |
| Количество · `count` | 51; 66 |
| light | 0.6 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Шептун](../reference/Bosses.md#boss_whisperer) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_ROTTING_ORCHARD <a href="#c6_rotting_orchard" id="c6_rotting_orchard"></a>

### Гниющий сад <a href="#гниющий-сад" id="гниющий-сад"></a>

Яблоки падают и лопаются, а из них ползут не черви.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `BLIGHT` |
| Уровень · `level` | 63 |
| Предшествующие зоны · `from` | [Край разлома](#c6_rift_edge); [Чумные поля](#c6_blighted_fields) |
| Монстры · `monsters` | [Гнилошляп](../reference/Monsters.md#blightcap); [Чумная оса](../reference/Monsters.md#plague_wasp); [Чумоносец](../reference/Monsters.md#plague_bearer) |
| Количество · `count` | 51; 66 |
| light | 0.75 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Садовник гнили](../reference/Bosses.md#boss_rot_gardener) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_PLAGUE_VILLAGE <a href="#c6_plague_village" id="c6_plague_village"></a>

### Чумная деревня <a href="#чумная-деревня" id="чумная-деревня"></a>

Дома заколочены изнутри, но стучат в двери снаружи.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `BLIGHT` |
| Уровень · `level` | 63 |
| Предшествующие зоны · `from` | [Чумные поля](#c6_blighted_fields); [Покинутое аббатство](#c6_forsaken_abbey) |
| Монстры · `monsters` | [Чумоносец](../reference/Monsters.md#plague_bearer); [Чумная оса](../reference/Monsters.md#plague_wasp); [Гнилошляп](../reference/Monsters.md#blightcap) |
| Количество · `count` | 51; 66 |
| light | 0.75 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Чумной лекарь](../reference/Bosses.md#boss_plague_doctor) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_OSSUARY <a href="#c6_ossuary" id="c6_ossuary"></a>

### Костница <a href="#костница" id="костница"></a>

Стены сложены из костей, и некоторые кости ещё помнят, кем были.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CRYPT` |
| Уровень · `level` | 63 |
| Предшествующие зоны · `from` | [Покинутое аббатство](#c6_forsaken_abbey) |
| Монстры · `monsters` | [Чумоносец](../reference/Monsters.md#plague_bearer); [Ползун бездны](../reference/Monsters.md#abyss_crawler); [Тень бездны](../reference/Monsters.md#abyssal_shade) |
| Количество · `count` | 51; 66 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Собиратель костей](../reference/Bosses.md#boss_bone_collector) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_VOID_SHORE <a href="#c6_void_shore" id="c6_void_shore"></a>

### Берег пустоты <a href="#берег-пустоты" id="берег-пустоты"></a>

Прибой здесь чёрный и беззвучный, и уносит он не песок.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ABYSS` |
| Уровень · `level` | 65 |
| Предшествующие зоны · `from` | [Шепчущая пропасть](#c6_whispering_chasm) |
| Монстры · `monsters` | [Порождение пустоты](../reference/Monsters.md#void_spawn); [Тень бездны](../reference/Monsters.md#abyssal_shade); [Ползун бездны](../reference/Monsters.md#abyss_crawler) |
| Количество · `count` | 51; 66 |
| light | 0.6 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Кракен пустоты](../reference/Bosses.md#boss_void_kraken) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_SPORE_CATHEDRAL <a href="#c6_spore_cathedral" id="c6_spore_cathedral"></a>

### Споровый собор <a href="#споровый-собор" id="споровый-собор"></a>

Собор, заросший грибницей, где хор поёт спорами.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `BLIGHT` |
| Уровень · `level` | 65 |
| Предшествующие зоны · `from` | [Гниющий сад](#c6_rotting_orchard); [Чумная деревня](#c6_plague_village) |
| Монстры · `monsters` | [Гнилошляп](../reference/Monsters.md#blightcap); [Чумоносец](../reference/Monsters.md#plague_bearer); [Тень бездны](../reference/Monsters.md#abyssal_shade) |
| Количество · `count` | 51; 66 |
| light | 0.75 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Споровый кардинал](../reference/Bosses.md#boss_spore_cardinal) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_FALLEN_STAR <a href="#c6_fallen_star" id="c6_fallen_star"></a>

### Павшая звезда <a href="#павшая-звезда" id="павшая-звезда"></a>

Кратер, куда упало то, что светило, но звездой не было.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ABYSS` |
| Уровень · `level` | 65 |
| Предшествующие зоны · `from` | [Чумная деревня](#c6_plague_village) |
| Монстры · `monsters` | [Порождение пустоты](../reference/Monsters.md#void_spawn); [Ползун бездны](../reference/Monsters.md#abyss_crawler); [Чумная оса](../reference/Monsters.md#plague_wasp) |
| Количество · `count` | 51; 66 |
| light | 0.6 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Звёздное отродье](../reference/Bosses.md#boss_star_spawn) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_CATACOMB_MAW <a href="#c6_catacomb_maw" id="c6_catacomb_maw"></a>

### Пасть катакомб <a href="#пасть-катакомб" id="пасть-катакомб"></a>

Катакомбы уходят вниз и вниз, пока не упираются в чью-то глотку.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CRYPT` |
| Уровень · `level` | 67 |
| Предшествующие зоны · `from` | [Берег пустоты](#c6_void_shore) |
| Монстры · `monsters` | [Ползун бездны](../reference/Monsters.md#abyss_crawler); [Чумоносец](../reference/Monsters.md#plague_bearer); [Тень бездны](../reference/Monsters.md#abyssal_shade) |
| Количество · `count` | 51; 66 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Червь катакомб](../reference/Bosses.md#boss_catacomb_worm) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_WITHERED_WOOD <a href="#c6_withered_wood" id="c6_withered_wood"></a>

### Иссохший лес <a href="#иссохший-лес" id="иссохший-лес"></a>

Деревья без листьев тянутся к путнику, словно просят помощи.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `BLIGHT` |
| Уровень · `level` | 67 |
| Предшествующие зоны · `from` | [Берег пустоты](#c6_void_shore); [Споровый собор](#c6_spore_cathedral) |
| Монстры · `monsters` | [Гнилошляп](../reference/Monsters.md#blightcap); [Чумная оса](../reference/Monsters.md#plague_wasp); [Чумоносец](../reference/Monsters.md#plague_bearer) |
| Количество · `count` | 51; 66 |
| light | 0.75 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Иссохший великан](../reference/Bosses.md#boss_withered_giant) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_ECLIPSE_ALTAR <a href="#c6_eclipse_altar" id="c6_eclipse_altar"></a>

### Алтарь затмения <a href="#алтарь-затмения" id="алтарь-затмения"></a>

Алтарь, где солнце гаснет всякий раз, как проливают кровь.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `TEMPLE` |
| Уровень · `level` | 67 |
| Предшествующие зоны · `from` | [Споровый собор](#c6_spore_cathedral); [Павшая звезда](#c6_fallen_star) |
| Монстры · `monsters` | [Тень бездны](../reference/Monsters.md#abyssal_shade); [Порождение пустоты](../reference/Monsters.md#void_spawn); [Чумоносец](../reference/Monsters.md#plague_bearer) |
| Количество · `count` | 51; 66 |
| light | 0.85 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Жрица затмения](../reference/Bosses.md#boss_eclipse_priestess) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_STARLESS_DEEP <a href="#c6_starless_deep" id="c6_starless_deep"></a>

### Беззвёздная глубь <a href="#беззвёздная-глубь" id="беззвёздная-глубь"></a>

Тьма такая густая, что свет факела вязнет в ней, как в смоле.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ABYSS` |
| Уровень · `level` | 67 |
| Предшествующие зоны · `from` | [Павшая звезда](#c6_fallen_star) |
| Монстры · `monsters` | [Ползун бездны](../reference/Monsters.md#abyss_crawler); [Порождение пустоты](../reference/Monsters.md#void_spawn); [Тень бездны](../reference/Monsters.md#abyssal_shade) |
| Количество · `count` | 51; 66 |
| light | 0.6 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Безглазый](../reference/Bosses.md#boss_eyeless_one) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_ROTTEN_HEART <a href="#c6_rotten_heart" id="c6_rotten_heart"></a>

### Гнилое сердце <a href="#гнилое-сердце" id="гнилое-сердце"></a>

Всё поветрие растёт из одного места, и оно бьётся.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `BLIGHT` |
| Уровень · `level` | 69 |
| Предшествующие зоны · `from` | [Иссохший лес](#c6_withered_wood) |
| Монстры · `monsters` | [Гнилошляп](../reference/Monsters.md#blightcap); [Чумоносец](../reference/Monsters.md#plague_bearer); [Чумная оса](../reference/Monsters.md#plague_wasp) |
| Количество · `count` | 51; 66 |
| light | 0.75 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Мать гнили](../reference/Bosses.md#boss_mother_blight) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_VEIL_OF_SHADOWS <a href="#c6_veil_of_shadows" id="c6_veil_of_shadows"></a>

### Завеса теней <a href="#завеса-теней" id="завеса-теней"></a>

Тонкая грань меж миром и бездной, истёртая до дыр.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ABYSS` |
| Уровень · `level` | 69 |
| Предшествующие зоны · `from` | [Иссохший лес](#c6_withered_wood); [Алтарь затмения](#c6_eclipse_altar) |
| Монстры · `monsters` | [Тень бездны](../reference/Monsters.md#abyssal_shade); [Порождение пустоты](../reference/Monsters.md#void_spawn); [Ползун бездны](../reference/Monsters.md#abyss_crawler) |
| Количество · `count` | 51; 66 |
| light | 0.6 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Ткач завесы](../reference/Bosses.md#boss_veil_weaver) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_ENDLESS_STAIR <a href="#c6_endless_stair" id="c6_endless_stair"></a>

### Бесконечная лестница <a href="#бесконечная-лестница" id="бесконечная-лестница"></a>

Ступени ведут вниз дольше, чем можно спускаться живым.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ABYSS` |
| Уровень · `level` | 69 |
| Предшествующие зоны · `from` | [Алтарь затмения](#c6_eclipse_altar); [Беззвёздная глубь](#c6_starless_deep) |
| Монстры · `monsters` | [Порождение пустоты](../reference/Monsters.md#void_spawn); [Ползун бездны](../reference/Monsters.md#abyss_crawler); [Чумная оса](../reference/Monsters.md#plague_wasp) |
| Количество · `count` | 51; 66 |
| light | 0.6 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Вечный привратник](../reference/Bosses.md#boss_endless_warden) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C6_ABYSSAL_THRONE <a href="#c6_abyssal_throne" id="c6_abyssal_throne"></a>

### Трон бездны <a href="#трон-бездны" id="трон-бездны"></a>

На дне всего, во тьме без края, сидит тот, кому поклоняется сама бездна.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ABYSS` |
| Уровень · `level` | 70 |
| Предшествующие зоны · `from` | [Гнилое сердце](#c6_rotten_heart); [Завеса теней](#c6_veil_of_shadows); [Бесконечная лестница](#c6_endless_stair) |
| Финал региона · `finale` | Да |
| Монстры · `monsters` | [Порождение пустоты](../reference/Monsters.md#void_spawn); [Тень бездны](../reference/Monsters.md#abyssal_shade); [Ползун бездны](../reference/Monsters.md#abyss_crawler); [Чумоносец](../reference/Monsters.md#plague_bearer) |
| Количество · `count` | 51; 66 |
| light | 0.6 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_6](../reference/Tables/Tables-LOOT.md#loot-chest_6) |
| Страж · `boss` | [Король бездны](../reference/Bosses.md#boss_abyss_king) |
| Изначально осквернён · `corrupted` | [Осквернённый Архонт](../reference/Monsters.md#corrupted_6) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/campaign.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
