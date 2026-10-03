# Берег изгнанников

| Зона | Уровень | Страж | Открывается из |
| --- | --- | --- | --- |
| [Приливный берег](#c1_tidal_shore) | 1 | [Зовущий Прилив](../reference/Bosses.md#boss_tidecaller) | — |
| [Кладбище кораблей](#c1_ship_graveyard) | 3 | [Капитан «Скорби»](../reference/Bosses.md#boss_sorrow_captain) | [Приливный берег](#c1_tidal_shore) |
| [Солёные пещеры](#c1_brine_caves) | 3 | [Мать Рассола](../reference/Bosses.md#boss_brine_mother) | [Приливный берег](#c1_tidal_shore) |
| [Птичьи утёсы](#c1_gull_cliffs) | 3 | [Королева Гнёзд](../reference/Bosses.md#boss_nest_queen) | [Приливный берег](#c1_tidal_shore) |
| [Маяк утопленников](#c1_drowned_lighthouse) | 5 | [Смотритель Маяка](../reference/Bosses.md#boss_lamp_warden) | [Кладбище кораблей](#c1_ship_graveyard) |
| [Гнилая топь](#c1_rotting_mire) | 5 | [Болотная Королева](../reference/Bosses.md#boss_bog_queen) | [Кладбище кораблей](#c1_ship_graveyard); [Солёные пещеры](#c1_brine_caves) |
| [Туманная переправа](#c1_misty_ferry) | 5 | [Паромщик](../reference/Bosses.md#boss_ferryman) | [Солёные пещеры](#c1_brine_caves); [Птичьи утёсы](#c1_gull_cliffs) |
| [Грот контрабандистов](#c1_smugglers_grotto) | 5 | [Король Контрабандистов](../reference/Bosses.md#boss_smuggler_king) | [Птичьи утёсы](#c1_gull_cliffs) |
| [Шепчущий лес](#c1_whispering_wood) | 7 | [Вожак Стаи](../reference/Bosses.md#boss_alpha_wolf) | [Маяк утопленников](#c1_drowned_lighthouse); [Гнилая топь](#c1_rotting_mire) |
| [Чёрная гать](#c1_black_causeway) | 7 | [Трясинный Змей](../reference/Bosses.md#boss_mire_serpent) | [Гнилая топь](#c1_rotting_mire); [Туманная переправа](#c1_misty_ferry) |
| [Волчья чаща](#c1_wolf_thicket) | 7 | [Матёрый Волколак](../reference/Bosses.md#boss_old_werewolf) | [Туманная переправа](#c1_misty_ferry) |
| [Павшие руины](#c1_fallen_ruins) | 9 | [Страж Руин](../reference/Bosses.md#boss_ruin_warden) | [Шепчущий лес](#c1_whispering_wood); [Чёрная гать](#c1_black_causeway) |
| [Старая застава](#c1_old_outpost) | 9 | [Комендант Заставы](../reference/Bosses.md#boss_outpost_commander) | [Чёрная гать](#c1_black_causeway); [Волчья чаща](#c1_wolf_thicket) |
| [Курган вождя](#c1_chieftains_barrow) | 9 | [Вождь Кургана](../reference/Bosses.md#boss_barrow_chieftain) | [Волчья чаща](#c1_wolf_thicket) |
| [Склеп изгнанников](#c1_crypt_of_exiles) | 11 | [Владыка Склепа](../reference/Bosses.md#boss_crypt_lord) | [Павшие руины](#c1_fallen_ruins); [Старая застава](#c1_old_outpost) |


## C1_TIDAL_SHORE <a href="#c1_tidal_shore" id="c1_tidal_shore"></a>

### Приливный берег <a href="#приливный-берег" id="приливный-берег"></a>

Мокрый песок, обломки кораблей и те, кого море не отпустило.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SHORE` |
| Уровень · `level` | 1 |
| Монстры · `monsters` | [Утопленник](../reference/Monsters.md#drowned); [Береговой краб](../reference/Monsters.md#shore_crab) |
| Количество · `count` | 37; 51 |
| light | 1.2 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](../reference/Tables/Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Зовущий Прилив](../reference/Bosses.md#boss_tidecaller) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](../reference/Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_SHIP_GRAVEYARD <a href="#c1_ship_graveyard" id="c1_ship_graveyard"></a>

### Кладбище кораблей <a href="#кладбище-кораблей" id="кладбище-кораблей"></a>

Остовы кораблей, севших на мель, и команды, что так и не сошли на берег.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SHORE` |
| Уровень · `level` | 3 |
| Предшествующие зоны · `from` | [Приливный берег](#c1_tidal_shore) |
| Монстры · `monsters` | [Утопленник](../reference/Monsters.md#drowned); [Береговой краб](../reference/Monsters.md#shore_crab); [Солёный плевун](../reference/Monsters.md#brine_spitter) |
| Количество · `count` | 37; 51 |
| light | 1.1 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](../reference/Tables/Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Капитан «Скорби»](../reference/Bosses.md#boss_sorrow_captain) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](../reference/Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_BRINE_CAVES <a href="#c1_brine_caves" id="c1_brine_caves"></a>

### Солёные пещеры <a href="#солёные-пещеры" id="солёные-пещеры"></a>

Гроты, куда прилив загоняет всё живое и не очень.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CAVE` |
| Уровень · `level` | 3 |
| Предшествующие зоны · `from` | [Приливный берег](#c1_tidal_shore) |
| Монстры · `monsters` | [Береговой краб](../reference/Monsters.md#shore_crab); [Пещерная летучая мышь](../reference/Monsters.md#cave_bat); [Солёный плевун](../reference/Monsters.md#brine_spitter) |
| Количество · `count` | 37; 51 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](../reference/Tables/Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Мать Рассола](../reference/Bosses.md#boss_brine_mother) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](../reference/Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_GULL_CLIFFS <a href="#c1_gull_cliffs" id="c1_gull_cliffs"></a>

### Птичьи утёсы <a href="#птичьи-утёсы" id="птичьи-утёсы"></a>

Скалы над прибоем, где гнездятся твари крупнее чаек.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SHORE` |
| Уровень · `level` | 3 |
| Предшествующие зоны · `from` | [Приливный берег](#c1_tidal_shore) |
| Монстры · `monsters` | [Пещерная летучая мышь](../reference/Monsters.md#cave_bat); [Береговой краб](../reference/Monsters.md#shore_crab); [Утопленник](../reference/Monsters.md#drowned) |
| Количество · `count` | 37; 51 |
| light | 1.3 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](../reference/Tables/Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Королева Гнёзд](../reference/Bosses.md#boss_nest_queen) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](../reference/Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_DROWNED_LIGHTHOUSE <a href="#c1_drowned_lighthouse" id="c1_drowned_lighthouse"></a>

### Маяк утопленников <a href="#маяк-утопленников" id="маяк-утопленников"></a>

Маяк всё ещё светит, но зовёт он теперь мертвецов.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SHORE` |
| Уровень · `level` | 5 |
| Предшествующие зоны · `from` | [Кладбище кораблей](#c1_ship_graveyard) |
| Монстры · `monsters` | [Утопленник](../reference/Monsters.md#drowned); [Береговой краб](../reference/Monsters.md#shore_crab); [Солёный плевун](../reference/Monsters.md#brine_spitter) |
| Количество · `count` | 44; 57 |
| light | 1 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](../reference/Tables/Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Смотритель Маяка](../reference/Bosses.md#boss_lamp_warden) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](../reference/Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_ROTTING_MIRE <a href="#c1_rotting_mire" id="c1_rotting_mire"></a>

### Гнилая топь <a href="#гнилая-топь" id="гнилая-топь"></a>

Трясина, где мёртвые не лежат спокойно.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `MIRE` |
| Уровень · `level` | 5 |
| Предшествующие зоны · `from` | [Кладбище кораблей](#c1_ship_graveyard); [Солёные пещеры](#c1_brine_caves) |
| Монстры · `monsters` | [Болотный зомби](../reference/Monsters.md#mire_zombie); [Трясинный скрытень](../reference/Monsters.md#bog_lurker); [Болотная пиявка](../reference/Monsters.md#swamp_leech) |
| Количество · `count` | 44; 57 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](../reference/Tables/Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Болотная Королева](../reference/Bosses.md#boss_bog_queen) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](../reference/Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_MISTY_FERRY <a href="#c1_misty_ferry" id="c1_misty_ferry"></a>

### Туманная переправа <a href="#туманная-переправа" id="туманная-переправа"></a>

Паром ходит сквозь туман, и никто не платит за обратный путь.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `MIRE` |
| Уровень · `level` | 5 |
| Предшествующие зоны · `from` | [Солёные пещеры](#c1_brine_caves); [Птичьи утёсы](#c1_gull_cliffs) |
| Монстры · `monsters` | [Болотный зомби](../reference/Monsters.md#mire_zombie); [Утопленник](../reference/Monsters.md#drowned); [Болотная пиявка](../reference/Monsters.md#swamp_leech) |
| Количество · `count` | 44; 57 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](../reference/Tables/Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Паромщик](../reference/Bosses.md#boss_ferryman) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](../reference/Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_SMUGGLERS_GROTTO <a href="#c1_smugglers_grotto" id="c1_smugglers_grotto"></a>

### Грот контрабандистов <a href="#грот-контрабандистов" id="грот-контрабандистов"></a>

Тайная бухта, где добычу делят ножами.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CAVE` |
| Уровень · `level` | 5 |
| Предшествующие зоны · `from` | [Птичьи утёсы](#c1_gull_cliffs) |
| Монстры · `monsters` | [Береговой краб](../reference/Monsters.md#shore_crab); [Пещерная летучая мышь](../reference/Monsters.md#cave_bat); [Утопленник](../reference/Monsters.md#drowned) |
| Количество · `count` | 44; 57 |
| light | 0.75 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](../reference/Tables/Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Король Контрабандистов](../reference/Bosses.md#boss_smuggler_king) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](../reference/Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_WHISPERING_WOOD <a href="#c1_whispering_wood" id="c1_whispering_wood"></a>

### Шепчущий лес <a href="#шепчущий-лес" id="шепчущий-лес"></a>

Лес, где охотятся волки и пауки, а деревья смотрят вслед.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `FOREST` |
| Уровень · `level` | 7 |
| Предшествующие зоны · `from` | [Маяк утопленников](#c1_drowned_lighthouse); [Гнилая топь](#c1_rotting_mire) |
| Монстры · `monsters` | [Одичавший волк](../reference/Monsters.md#feral_wolf); [Терновая дриада](../reference/Monsters.md#thorn_dryad); [Лесной паук](../reference/Monsters.md#wood_spider) |
| Количество · `count` | 44; 57 |
| light | 1 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](../reference/Tables/Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Вожак Стаи](../reference/Bosses.md#boss_alpha_wolf) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](../reference/Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_BLACK_CAUSEWAY <a href="#c1_black_causeway" id="c1_black_causeway"></a>

### Чёрная гать <a href="#чёрная-гать" id="чёрная-гать"></a>

Бревенчатая тропа через трясину, что проседает под каждым шагом.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `MIRE` |
| Уровень · `level` | 7 |
| Предшествующие зоны · `from` | [Гнилая топь](#c1_rotting_mire); [Туманная переправа](#c1_misty_ferry) |
| Монстры · `monsters` | [Трясинный скрытень](../reference/Monsters.md#bog_lurker); [Болотная пиявка](../reference/Monsters.md#swamp_leech); [Болотный зомби](../reference/Monsters.md#mire_zombie) |
| Количество · `count` | 44; 57 |
| light | 0.85 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](../reference/Tables/Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Трясинный Змей](../reference/Bosses.md#boss_mire_serpent) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](../reference/Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_WOLF_THICKET <a href="#c1_wolf_thicket" id="c1_wolf_thicket"></a>

### Волчья чаща <a href="#волчья-чаща" id="волчья-чаща"></a>

Бурелом, где вой не стихает даже днём.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `FOREST` |
| Уровень · `level` | 7 |
| Предшествующие зоны · `from` | [Туманная переправа](#c1_misty_ferry) |
| Монстры · `monsters` | [Одичавший волк](../reference/Monsters.md#feral_wolf); [Лесной паук](../reference/Monsters.md#wood_spider) |
| Количество · `count` | 44; 57 |
| light | 0.95 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](../reference/Tables/Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Матёрый Волколак](../reference/Bosses.md#boss_old_werewolf) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](../reference/Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_FALLEN_RUINS <a href="#c1_fallen_ruins" id="c1_fallen_ruins"></a>

### Павшие руины <a href="#павшие-руины" id="павшие-руины"></a>

Развалины крепости, всё ещё охраняемые её гарнизоном.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `RUINS` |
| Уровень · `level` | 9 |
| Предшествующие зоны · `from` | [Шепчущий лес](#c1_whispering_wood); [Чёрная гать](#c1_black_causeway) |
| Монстры · `monsters` | [Скелет-воин](../reference/Monsters.md#skeleton_warrior); [Костяной лучник](../reference/Monsters.md#bone_archer); [Страж руин](../reference/Monsters.md#ruin_sentry) |
| Количество · `count` | 44; 57 |
| light | 1 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](../reference/Tables/Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Страж Руин](../reference/Bosses.md#boss_ruin_warden) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](../reference/Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_OLD_OUTPOST <a href="#c1_old_outpost" id="c1_old_outpost"></a>

### Старая застава <a href="#старая-застава" id="старая-застава"></a>

Пограничная крепостца, гарнизон которой до сих пор несёт караул.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `RUINS` |
| Уровень · `level` | 9 |
| Предшествующие зоны · `from` | [Чёрная гать](#c1_black_causeway); [Волчья чаща](#c1_wolf_thicket) |
| Монстры · `monsters` | [Скелет-воин](../reference/Monsters.md#skeleton_warrior); [Костяной лучник](../reference/Monsters.md#bone_archer); [Одичавший волк](../reference/Monsters.md#feral_wolf) |
| Количество · `count` | 44; 57 |
| light | 1 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](../reference/Tables/Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Комендант Заставы](../reference/Bosses.md#boss_outpost_commander) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](../reference/Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_CHIEFTAINS_BARROW <a href="#c1_chieftains_barrow" id="c1_chieftains_barrow"></a>

### Курган вождя <a href="#курган-вождя" id="курган-вождя"></a>

Могильный холм древнего вождя и его верной дружины.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CRYPT` |
| Уровень · `level` | 9 |
| Предшествующие зоны · `from` | [Волчья чаща](#c1_wolf_thicket) |
| Монстры · `monsters` | [Упырь](../reference/Monsters.md#ghoul); [Скелет-воин](../reference/Monsters.md#skeleton_warrior); [Костяной лучник](../reference/Monsters.md#bone_archer) |
| Количество · `count` | 44; 57 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](../reference/Tables/Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Вождь Кургана](../reference/Bosses.md#boss_barrow_chieftain) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](../reference/Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |


## C1_CRYPT_OF_EXILES <a href="#c1_crypt_of_exiles" id="c1_crypt_of_exiles"></a>

### Склеп изгнанников <a href="#склеп-изгнанников" id="склеп-изгнанников"></a>

Усыпальница тех, кто пришёл сюда до вас.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CRYPT` |
| Уровень · `level` | 11 |
| Предшествующие зоны · `from` | [Павшие руины](#c1_fallen_ruins); [Старая застава](#c1_old_outpost) |
| Финал региона · `finale` | Да |
| Монстры · `monsters` | [Упырь](../reference/Monsters.md#ghoul); [Скелет-воин](../reference/Monsters.md#skeleton_warrior); [Склепный призрак](../reference/Monsters.md#crypt_wraith); [Костяной лучник](../reference/Monsters.md#bone_archer) |
| Количество · `count` | 44; 57 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](../reference/Tables/Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Владыка Склепа](../reference/Bosses.md#boss_crypt_lord) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](../reference/Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](../reference/Tables/Tables-MODIFIER.md#mob-monster) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/campaign.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
