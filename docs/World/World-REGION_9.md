# Чертоги богов

| Зона | Уровень | Страж | Открывается из |
| --- | --- | --- | --- |
| [Порог богов](#c9_gods_threshold) | 91 | [Страж порога](../reference/Bosses.md#boss_threshold_guardian) | [Утонувший трон](World-REGION_8.md#c8_drowned_throne) |
| [Астральный берег](#c9_astral_shore) | 91 | [Звёздный прилив](../reference/Bosses.md#boss_star_tide) | [Утонувший трон](World-REGION_8.md#c8_drowned_throne) |
| [Поля забвения](#c9_fields_of_oblivion) | 91 | [Забвение](../reference/Bosses.md#boss_forgetting) | [Утонувший трон](World-REGION_8.md#c8_drowned_throne) |
| [Зал суда](#c9_hall_of_judgement) | 93 | [Судья душ](../reference/Bosses.md#boss_judge_of_souls) | [Порог богов](#c9_gods_threshold) |
| [Мост созвездий](#c9_constellation_bridge) | 93 | [Зверь созвездий](../reference/Bosses.md#boss_constellation_beast) | [Порог богов](#c9_gods_threshold); [Астральный берег](#c9_astral_shore) |
| [Безымянный архив](#c9_nameless_archive) | 93 | [Последний архивариус](../reference/Bosses.md#boss_archivist) | [Астральный берег](#c9_astral_shore); [Поля забвения](#c9_fields_of_oblivion) |
| [Солнечная кузня](#c9_sunforge) | 93 | [Солнечный кузнец](../reference/Bosses.md#boss_sun_smith) | [Поля забвения](#c9_fields_of_oblivion) |
| [Астральный сад](#c9_astral_orchard) | 95 | [Хранитель сада](../reference/Bosses.md#boss_orchard_keeper) | [Зал суда](#c9_hall_of_judgement); [Мост созвездий](#c9_constellation_bridge) |
| [Пустой пантеон](#c9_hollow_pantheon) | 95 | [Пустой бог](../reference/Bosses.md#boss_hollow_god) | [Зал суда](#c9_hall_of_judgement); [Мост созвездий](#c9_constellation_bridge); [Безымянный архив](#c9_nameless_archive) |
| [Зал войны](#c9_hall_of_war) | 95 | [Аватар войны](../reference/Bosses.md#boss_war_avatar) | [Мост созвездий](#c9_constellation_bridge); [Безымянный архив](#c9_nameless_archive); [Солнечная кузня](#c9_sunforge) |
| [Беззвёздная пустота](#c9_starless_void) | 95 | [Мать пустоты](../reference/Bosses.md#boss_void_mother) | [Безымянный архив](#c9_nameless_archive); [Солнечная кузня](#c9_sunforge) |
| [Небесная обсерватория](#c9_celestial_observatory) | 98 | [Звездочёт](../reference/Bosses.md#boss_stargazer) | [Астральный сад](#c9_astral_orchard); [Пустой пантеон](#c9_hollow_pantheon) |
| [Дорога к трону](#c9_throne_road) | 98 | [Собиратель корон](../reference/Bosses.md#boss_crown_collector) | [Пустой пантеон](#c9_hollow_pantheon); [Зал войны](#c9_hall_of_war) |
| [Край ничто](#c9_edge_of_nothing) | 98 | [Странник ничто](../reference/Bosses.md#boss_nothing_walker) | [Зал войны](#c9_hall_of_war); [Беззвёздная пустота](#c9_starless_void) |
| [Последний трон](#c9_last_throne) | 100 | [Последний бог](../reference/Bosses.md#boss_last_god) | [Небесная обсерватория](#c9_celestial_observatory); [Дорога к трону](#c9_throne_road); [Край ничто](#c9_edge_of_nothing) |


## C9_GODS_THRESHOLD <a href="#c9_gods_threshold" id="c9_gods_threshold"></a>

### Порог богов <a href="#порог-богов" id="порог-богов"></a>

Смертным не положено стоять здесь. Ты стоишь.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `GODHALL` |
| Уровень · `level` | 91 |
| Предшествующие зоны · `from` | [Утонувший трон](World-REGION_8.md#c8_drowned_throne) |
| Монстры · `monsters` | [Божественный страж](../reference/Monsters.md#divine_warden); [Падший архонт](../reference/Monsters.md#fallen_archon); [Титан пустоты](../reference/Monsters.md#void_titan) |
| Количество · `count` | 57; 74 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Страж порога](../reference/Bosses.md#boss_threshold_guardian) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_ASTRAL_SHORE <a href="#c9_astral_shore" id="c9_astral_shore"></a>

### Астральный берег <a href="#астральный-берег" id="астральный-берег"></a>

Звёзды выносит на этот берег, как ракушки.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ASTRAL` |
| Уровень · `level` | 91 |
| Предшествующие зоны · `from` | [Утонувший трон](World-REGION_8.md#c8_drowned_throne) |
| Монстры · `monsters` | [Астральный зверь](../reference/Monsters.md#astral_beast); [Звёздный дракон](../reference/Monsters.md#star_dragon); [Клятвенный фанатик](../reference/Monsters.md#godsworn_zealot) |
| Количество · `count` | 57; 74 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Звёздный прилив](../reference/Bosses.md#boss_star_tide) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_FIELDS_OF_OBLIVION <a href="#c9_fields_of_oblivion" id="c9_fields_of_oblivion"></a>

### Поля забвения <a href="#поля-забвения" id="поля-забвения"></a>

Иди слишком долго — и забудешь, зачем пришёл.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `OBLIVION` |
| Уровень · `level` | 91 |
| Предшествующие зоны · `from` | [Утонувший трон](World-REGION_8.md#c8_drowned_throne) |
| Монстры · `monsters` | [Тень забвения](../reference/Monsters.md#oblivion_shade); [Клятвенный фанатик](../reference/Monsters.md#godsworn_zealot); [Титан пустоты](../reference/Monsters.md#void_titan) |
| Количество · `count` | 57; 74 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Забвение](../reference/Bosses.md#boss_forgetting) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_HALL_OF_JUDGEMENT <a href="#c9_hall_of_judgement" id="c9_hall_of_judgement"></a>

### Зал суда <a href="#зал-суда" id="зал-суда"></a>

Здесь взвешивали каждую душу. Большинство оказались лёгкими.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `GODHALL` |
| Уровень · `level` | 93 |
| Предшествующие зоны · `from` | [Порог богов](#c9_gods_threshold) |
| Монстры · `monsters` | [Божественный страж](../reference/Monsters.md#divine_warden); [Падший архонт](../reference/Monsters.md#fallen_archon); [Титан пустоты](../reference/Monsters.md#void_titan) |
| Количество · `count` | 57; 74 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Судья душ](../reference/Bosses.md#boss_judge_of_souls) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_CONSTELLATION_BRIDGE <a href="#c9_constellation_bridge" id="c9_constellation_bridge"></a>

### Мост созвездий <a href="#мост-созвездий" id="мост-созвездий"></a>

Мост из звёзд, и каждая звезда — глаз бога.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ASTRAL` |
| Уровень · `level` | 93 |
| Предшествующие зоны · `from` | [Порог богов](#c9_gods_threshold); [Астральный берег](#c9_astral_shore) |
| Монстры · `monsters` | [Астральный зверь](../reference/Monsters.md#astral_beast); [Звёздный дракон](../reference/Monsters.md#star_dragon); [Клятвенный фанатик](../reference/Monsters.md#godsworn_zealot) |
| Количество · `count` | 57; 74 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Зверь созвездий](../reference/Bosses.md#boss_constellation_beast) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_NAMELESS_ARCHIVE <a href="#c9_nameless_archive" id="c9_nameless_archive"></a>

### Безымянный архив <a href="#безымянный-архив" id="безымянный-архив"></a>

Книги обо всём, что было забыто.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `OBLIVION` |
| Уровень · `level` | 93 |
| Предшествующие зоны · `from` | [Астральный берег](#c9_astral_shore); [Поля забвения](#c9_fields_of_oblivion) |
| Монстры · `monsters` | [Тень забвения](../reference/Monsters.md#oblivion_shade); [Клятвенный фанатик](../reference/Monsters.md#godsworn_zealot); [Титан пустоты](../reference/Monsters.md#void_titan) |
| Количество · `count` | 57; 74 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Последний архивариус](../reference/Bosses.md#boss_archivist) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_SUNFORGE <a href="#c9_sunforge" id="c9_sunforge"></a>

### Солнечная кузня <a href="#солнечная-кузня" id="солнечная-кузня"></a>

Здесь боги выковали солнце и оставили огонь гореть.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `GODHALL` |
| Уровень · `level` | 93 |
| Предшествующие зоны · `from` | [Поля забвения](#c9_fields_of_oblivion) |
| Монстры · `monsters` | [Божественный страж](../reference/Monsters.md#divine_warden); [Падший архонт](../reference/Monsters.md#fallen_archon); [Титан пустоты](../reference/Monsters.md#void_titan) |
| Количество · `count` | 57; 74 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Солнечный кузнец](../reference/Bosses.md#boss_sun_smith) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_ASTRAL_ORCHARD <a href="#c9_astral_orchard" id="c9_astral_orchard"></a>

### Астральный сад <a href="#астральный-сад" id="астральный-сад"></a>

Его плоды — новорождённые звёзды.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ASTRAL` |
| Уровень · `level` | 95 |
| Предшествующие зоны · `from` | [Зал суда](#c9_hall_of_judgement); [Мост созвездий](#c9_constellation_bridge) |
| Монстры · `monsters` | [Астральный зверь](../reference/Monsters.md#astral_beast); [Звёздный дракон](../reference/Monsters.md#star_dragon); [Клятвенный фанатик](../reference/Monsters.md#godsworn_zealot) |
| Количество · `count` | 57; 74 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Хранитель сада](../reference/Bosses.md#boss_orchard_keeper) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_HOLLOW_PANTHEON <a href="#c9_hollow_pantheon" id="c9_hollow_pantheon"></a>

### Пустой пантеон <a href="#пустой-пантеон" id="пустой-пантеон"></a>

Статуи на месте. Богов нет.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `OBLIVION` |
| Уровень · `level` | 95 |
| Предшествующие зоны · `from` | [Зал суда](#c9_hall_of_judgement); [Мост созвездий](#c9_constellation_bridge); [Безымянный архив](#c9_nameless_archive) |
| Монстры · `monsters` | [Тень забвения](../reference/Monsters.md#oblivion_shade); [Клятвенный фанатик](../reference/Monsters.md#godsworn_zealot); [Титан пустоты](../reference/Monsters.md#void_titan) |
| Количество · `count` | 57; 74 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Пустой бог](../reference/Bosses.md#boss_hollow_god) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_HALL_OF_WAR <a href="#c9_hall_of_war" id="c9_hall_of_war"></a>

### Зал войны <a href="#зал-войны" id="зал-войны"></a>

Бог войны всё ещё муштрует своих избранных.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `GODHALL` |
| Уровень · `level` | 95 |
| Предшествующие зоны · `from` | [Мост созвездий](#c9_constellation_bridge); [Безымянный архив](#c9_nameless_archive); [Солнечная кузня](#c9_sunforge) |
| Монстры · `monsters` | [Божественный страж](../reference/Monsters.md#divine_warden); [Падший архонт](../reference/Monsters.md#fallen_archon); [Титан пустоты](../reference/Monsters.md#void_titan) |
| Количество · `count` | 57; 74 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Аватар войны](../reference/Bosses.md#boss_war_avatar) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_STARLESS_VOID <a href="#c9_starless_void" id="c9_starless_void"></a>

### Беззвёздная пустота <a href="#беззвёздная-пустота" id="беззвёздная-пустота"></a>

Здесь забыты даже звёзды.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `OBLIVION` |
| Уровень · `level` | 95 |
| Предшествующие зоны · `from` | [Безымянный архив](#c9_nameless_archive); [Солнечная кузня](#c9_sunforge) |
| Монстры · `monsters` | [Тень забвения](../reference/Monsters.md#oblivion_shade); [Клятвенный фанатик](../reference/Monsters.md#godsworn_zealot); [Титан пустоты](../reference/Monsters.md#void_titan) |
| Количество · `count` | 57; 74 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Мать пустоты](../reference/Bosses.md#boss_void_mother) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_CELESTIAL_OBSERVATORY <a href="#c9_celestial_observatory" id="c9_celestial_observatory"></a>

### Небесная обсерватория <a href="#небесная-обсерватория" id="небесная-обсерватория"></a>

Отсюда боги смотрели на нас. Теперь смотрим мы.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `ASTRAL` |
| Уровень · `level` | 98 |
| Предшествующие зоны · `from` | [Астральный сад](#c9_astral_orchard); [Пустой пантеон](#c9_hollow_pantheon) |
| Монстры · `monsters` | [Астральный зверь](../reference/Monsters.md#astral_beast); [Звёздный дракон](../reference/Monsters.md#star_dragon); [Клятвенный фанатик](../reference/Monsters.md#godsworn_zealot) |
| Количество · `count` | 57; 74 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Звездочёт](../reference/Bosses.md#boss_stargazer) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_THRONE_ROAD <a href="#c9_throne_road" id="c9_throne_road"></a>

### Дорога к трону <a href="#дорога-к-трону" id="дорога-к-трону"></a>

Вымощена коронами королей, что приходили раньше.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `GODHALL` |
| Уровень · `level` | 98 |
| Предшествующие зоны · `from` | [Пустой пантеон](#c9_hollow_pantheon); [Зал войны](#c9_hall_of_war) |
| Монстры · `monsters` | [Божественный страж](../reference/Monsters.md#divine_warden); [Падший архонт](../reference/Monsters.md#fallen_archon); [Титан пустоты](../reference/Monsters.md#void_titan) |
| Количество · `count` | 57; 74 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Собиратель корон](../reference/Bosses.md#boss_crown_collector) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_EDGE_OF_NOTHING <a href="#c9_edge_of_nothing" id="c9_edge_of_nothing"></a>

### Край ничто <a href="#край-ничто" id="край-ничто"></a>

Дальше — ничего. Даже тьмы.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `OBLIVION` |
| Уровень · `level` | 98 |
| Предшествующие зоны · `from` | [Зал войны](#c9_hall_of_war); [Беззвёздная пустота](#c9_starless_void) |
| Монстры · `monsters` | [Тень забвения](../reference/Monsters.md#oblivion_shade); [Клятвенный фанатик](../reference/Monsters.md#godsworn_zealot); [Титан пустоты](../reference/Monsters.md#void_titan) |
| Количество · `count` | 57; 74 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Странник ничто](../reference/Bosses.md#boss_nothing_walker) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C9_LAST_THRONE <a href="#c9_last_throne" id="c9_last_throne"></a>

### Последний трон <a href="#последний-трон" id="последний-трон"></a>

Один трон, один бог, один изгнанник. Уйдёт только один.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `GODHALL` |
| Уровень · `level` | 100 |
| Предшествующие зоны · `from` | [Небесная обсерватория](#c9_celestial_observatory); [Дорога к трону](#c9_throne_road); [Край ничто](#c9_edge_of_nothing) |
| Финал региона · `finale` | Да |
| Монстры · `monsters` | [Божественный страж](../reference/Monsters.md#divine_warden); [Падший архонт](../reference/Monsters.md#fallen_archon); [Титан пустоты](../reference/Monsters.md#void_titan); [Звёздный дракон](../reference/Monsters.md#star_dragon) |
| Количество · `count` | 57; 74 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_9](../reference/Tables/Tables-LOOT.md#loot-chest_9) |
| Страж · `boss` | [Последний бог](../reference/Bosses.md#boss_last_god) |
| Изначально осквернён · `corrupted` | [Осквернённая машина богов](../reference/Monsters.md#corrupted_9) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/campaign.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
