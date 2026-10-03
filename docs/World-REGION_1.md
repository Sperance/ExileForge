# Берег изгнанников

| Зона | Уровень | Страж | Открывается из |
| --- | --- | --- | --- |
| [Приливный берег](World-REGION_1.md#c1_tidal_shore) | 1 | [Зовущий Прилив](Bosses.md#boss_tidecaller) | — |
| [Кладбище кораблей](World-REGION_1.md#c1_ship_graveyard) | 3 | [Капитан «Скорби»](Bosses.md#boss_sorrow_captain) | [Приливный берег](World-REGION_1.md#c1_tidal_shore) |
| [Солёные пещеры](World-REGION_1.md#c1_brine_caves) | 3 | [Мать Рассола](Bosses.md#boss_brine_mother) | [Приливный берег](World-REGION_1.md#c1_tidal_shore) |
| [Птичьи утёсы](World-REGION_1.md#c1_gull_cliffs) | 3 | [Королева Гнёзд](Bosses.md#boss_nest_queen) | [Приливный берег](World-REGION_1.md#c1_tidal_shore) |
| [Маяк утопленников](World-REGION_1.md#c1_drowned_lighthouse) | 5 | [Смотритель Маяка](Bosses.md#boss_lamp_warden) | [Кладбище кораблей](World-REGION_1.md#c1_ship_graveyard) |
| [Гнилая топь](World-REGION_1.md#c1_rotting_mire) | 5 | [Болотная Королева](Bosses.md#boss_bog_queen) | [Кладбище кораблей](World-REGION_1.md#c1_ship_graveyard); [Солёные пещеры](World-REGION_1.md#c1_brine_caves) |
| [Туманная переправа](World-REGION_1.md#c1_misty_ferry) | 5 | [Паромщик](Bosses.md#boss_ferryman) | [Солёные пещеры](World-REGION_1.md#c1_brine_caves); [Птичьи утёсы](World-REGION_1.md#c1_gull_cliffs) |
| [Грот контрабандистов](World-REGION_1.md#c1_smugglers_grotto) | 5 | [Король Контрабандистов](Bosses.md#boss_smuggler_king) | [Птичьи утёсы](World-REGION_1.md#c1_gull_cliffs) |
| [Шепчущий лес](World-REGION_1.md#c1_whispering_wood) | 7 | [Вожак Стаи](Bosses.md#boss_alpha_wolf) | [Маяк утопленников](World-REGION_1.md#c1_drowned_lighthouse); [Гнилая топь](World-REGION_1.md#c1_rotting_mire) |
| [Чёрная гать](World-REGION_1.md#c1_black_causeway) | 7 | [Трясинный Змей](Bosses.md#boss_mire_serpent) | [Гнилая топь](World-REGION_1.md#c1_rotting_mire); [Туманная переправа](World-REGION_1.md#c1_misty_ferry) |
| [Волчья чаща](World-REGION_1.md#c1_wolf_thicket) | 7 | [Матёрый Волколак](Bosses.md#boss_old_werewolf) | [Туманная переправа](World-REGION_1.md#c1_misty_ferry) |
| [Павшие руины](World-REGION_1.md#c1_fallen_ruins) | 9 | [Страж Руин](Bosses.md#boss_ruin_warden) | [Шепчущий лес](World-REGION_1.md#c1_whispering_wood); [Чёрная гать](World-REGION_1.md#c1_black_causeway) |
| [Старая застава](World-REGION_1.md#c1_old_outpost) | 9 | [Комендант Заставы](Bosses.md#boss_outpost_commander) | [Чёрная гать](World-REGION_1.md#c1_black_causeway); [Волчья чаща](World-REGION_1.md#c1_wolf_thicket) |
| [Курган вождя](World-REGION_1.md#c1_chieftains_barrow) | 9 | [Вождь Кургана](Bosses.md#boss_barrow_chieftain) | [Волчья чаща](World-REGION_1.md#c1_wolf_thicket) |
| [Склеп изгнанников](World-REGION_1.md#c1_crypt_of_exiles) | 11 | [Владыка Склепа](Bosses.md#boss_crypt_lord) | [Павшие руины](World-REGION_1.md#c1_fallen_ruins); [Старая застава](World-REGION_1.md#c1_old_outpost) |


## C1_TIDAL_SHORE

### Приливный берег

Мокрый песок, обломки кораблей и те, кого море не отпустило.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SHORE` |
| Уровень · `level` | 1 |
| Монстры · `monsters` | [Утопленник](Monsters.md#drowned); [Береговой краб](Monsters.md#shore_crab) |
| Количество · `count` | 37; 51 |
| light | 1.2 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Зовущий Прилив](Bosses.md#boss_tidecaller) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_SHIP_GRAVEYARD

### Кладбище кораблей

Остовы кораблей, севших на мель, и команды, что так и не сошли на берег.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SHORE` |
| Уровень · `level` | 3 |
| Предшествующие зоны · `from` | [Приливный берег](World-REGION_1.md#c1_tidal_shore) |
| Монстры · `monsters` | [Утопленник](Monsters.md#drowned); [Береговой краб](Monsters.md#shore_crab); [Солёный плевун](Monsters.md#brine_spitter) |
| Количество · `count` | 37; 51 |
| light | 1.1 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Капитан «Скорби»](Bosses.md#boss_sorrow_captain) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_BRINE_CAVES

### Солёные пещеры

Гроты, куда прилив загоняет всё живое и не очень.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CAVE` |
| Уровень · `level` | 3 |
| Предшествующие зоны · `from` | [Приливный берег](World-REGION_1.md#c1_tidal_shore) |
| Монстры · `monsters` | [Береговой краб](Monsters.md#shore_crab); [Пещерная летучая мышь](Monsters.md#cave_bat); [Солёный плевун](Monsters.md#brine_spitter) |
| Количество · `count` | 37; 51 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Мать Рассола](Bosses.md#boss_brine_mother) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_GULL_CLIFFS

### Птичьи утёсы

Скалы над прибоем, где гнездятся твари крупнее чаек.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SHORE` |
| Уровень · `level` | 3 |
| Предшествующие зоны · `from` | [Приливный берег](World-REGION_1.md#c1_tidal_shore) |
| Монстры · `monsters` | [Пещерная летучая мышь](Monsters.md#cave_bat); [Береговой краб](Monsters.md#shore_crab); [Утопленник](Monsters.md#drowned) |
| Количество · `count` | 37; 51 |
| light | 1.3 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Королева Гнёзд](Bosses.md#boss_nest_queen) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_DROWNED_LIGHTHOUSE

### Маяк утопленников

Маяк всё ещё светит, но зовёт он теперь мертвецов.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `SHORE` |
| Уровень · `level` | 5 |
| Предшествующие зоны · `from` | [Кладбище кораблей](World-REGION_1.md#c1_ship_graveyard) |
| Монстры · `monsters` | [Утопленник](Monsters.md#drowned); [Береговой краб](Monsters.md#shore_crab); [Солёный плевун](Monsters.md#brine_spitter) |
| Количество · `count` | 44; 57 |
| light | 1 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Смотритель Маяка](Bosses.md#boss_lamp_warden) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_ROTTING_MIRE

### Гнилая топь

Трясина, где мёртвые не лежат спокойно.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `MIRE` |
| Уровень · `level` | 5 |
| Предшествующие зоны · `from` | [Кладбище кораблей](World-REGION_1.md#c1_ship_graveyard); [Солёные пещеры](World-REGION_1.md#c1_brine_caves) |
| Монстры · `monsters` | [Болотный зомби](Monsters.md#mire_zombie); [Трясинный скрытень](Monsters.md#bog_lurker); [Болотная пиявка](Monsters.md#swamp_leech) |
| Количество · `count` | 44; 57 |
| light | 0.9 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Болотная Королева](Bosses.md#boss_bog_queen) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_MISTY_FERRY

### Туманная переправа

Паром ходит сквозь туман, и никто не платит за обратный путь.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `MIRE` |
| Уровень · `level` | 5 |
| Предшествующие зоны · `from` | [Солёные пещеры](World-REGION_1.md#c1_brine_caves); [Птичьи утёсы](World-REGION_1.md#c1_gull_cliffs) |
| Монстры · `monsters` | [Болотный зомби](Monsters.md#mire_zombie); [Утопленник](Monsters.md#drowned); [Болотная пиявка](Monsters.md#swamp_leech) |
| Количество · `count` | 44; 57 |
| light | 0.8 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Паромщик](Bosses.md#boss_ferryman) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_SMUGGLERS_GROTTO

### Грот контрабандистов

Тайная бухта, где добычу делят ножами.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CAVE` |
| Уровень · `level` | 5 |
| Предшествующие зоны · `from` | [Птичьи утёсы](World-REGION_1.md#c1_gull_cliffs) |
| Монстры · `monsters` | [Береговой краб](Monsters.md#shore_crab); [Пещерная летучая мышь](Monsters.md#cave_bat); [Утопленник](Monsters.md#drowned) |
| Количество · `count` | 44; 57 |
| light | 0.75 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_1](Tables-LOOT.md#loot-chest_1) |
| Страж · `boss` | [Король Контрабандистов](Bosses.md#boss_smuggler_king) |
| Изначально осквернён · `corrupted` | [Осквернённый Страж](Monsters.md#corrupted_1) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_WHISPERING_WOOD

### Шепчущий лес

Лес, где охотятся волки и пауки, а деревья смотрят вслед.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `FOREST` |
| Уровень · `level` | 7 |
| Предшествующие зоны · `from` | [Маяк утопленников](World-REGION_1.md#c1_drowned_lighthouse); [Гнилая топь](World-REGION_1.md#c1_rotting_mire) |
| Монстры · `monsters` | [Одичавший волк](Monsters.md#feral_wolf); [Терновая дриада](Monsters.md#thorn_dryad); [Лесной паук](Monsters.md#wood_spider) |
| Количество · `count` | 44; 57 |
| light | 1 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Вожак Стаи](Bosses.md#boss_alpha_wolf) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_BLACK_CAUSEWAY

### Чёрная гать

Бревенчатая тропа через трясину, что проседает под каждым шагом.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `MIRE` |
| Уровень · `level` | 7 |
| Предшествующие зоны · `from` | [Гнилая топь](World-REGION_1.md#c1_rotting_mire); [Туманная переправа](World-REGION_1.md#c1_misty_ferry) |
| Монстры · `monsters` | [Трясинный скрытень](Monsters.md#bog_lurker); [Болотная пиявка](Monsters.md#swamp_leech); [Болотный зомби](Monsters.md#mire_zombie) |
| Количество · `count` | 44; 57 |
| light | 0.85 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Трясинный Змей](Bosses.md#boss_mire_serpent) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_WOLF_THICKET

### Волчья чаща

Бурелом, где вой не стихает даже днём.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `FOREST` |
| Уровень · `level` | 7 |
| Предшествующие зоны · `from` | [Туманная переправа](World-REGION_1.md#c1_misty_ferry) |
| Монстры · `monsters` | [Одичавший волк](Monsters.md#feral_wolf); [Лесной паук](Monsters.md#wood_spider) |
| Количество · `count` | 44; 57 |
| light | 0.95 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Матёрый Волколак](Bosses.md#boss_old_werewolf) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_FALLEN_RUINS

### Павшие руины

Развалины крепости, всё ещё охраняемые её гарнизоном.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `RUINS` |
| Уровень · `level` | 9 |
| Предшествующие зоны · `from` | [Шепчущий лес](World-REGION_1.md#c1_whispering_wood); [Чёрная гать](World-REGION_1.md#c1_black_causeway) |
| Монстры · `monsters` | [Скелет-воин](Monsters.md#skeleton_warrior); [Костяной лучник](Monsters.md#bone_archer); [Страж руин](Monsters.md#ruin_sentry) |
| Количество · `count` | 44; 57 |
| light | 1 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Страж Руин](Bosses.md#boss_ruin_warden) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_OLD_OUTPOST

### Старая застава

Пограничная крепостца, гарнизон которой до сих пор несёт караул.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `RUINS` |
| Уровень · `level` | 9 |
| Предшествующие зоны · `from` | [Чёрная гать](World-REGION_1.md#c1_black_causeway); [Волчья чаща](World-REGION_1.md#c1_wolf_thicket) |
| Монстры · `monsters` | [Скелет-воин](Monsters.md#skeleton_warrior); [Костяной лучник](Monsters.md#bone_archer); [Одичавший волк](Monsters.md#feral_wolf) |
| Количество · `count` | 44; 57 |
| light | 1 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Комендант Заставы](Bosses.md#boss_outpost_commander) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_CHIEFTAINS_BARROW

### Курган вождя

Могильный холм древнего вождя и его верной дружины.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CRYPT` |
| Уровень · `level` | 9 |
| Предшествующие зоны · `from` | [Волчья чаща](World-REGION_1.md#c1_wolf_thicket) |
| Монстры · `monsters` | [Упырь](Monsters.md#ghoul); [Скелет-воин](Monsters.md#skeleton_warrior); [Костяной лучник](Monsters.md#bone_archer) |
| Количество · `count` | 44; 57 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Вождь Кургана](Bosses.md#boss_barrow_chieftain) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |


## C1_CRYPT_OF_EXILES

### Склеп изгнанников

Усыпальница тех, кто пришёл сюда до вас.


| Параметр | Значение |
| --- | --- |
| Биом · `biome` | `CRYPT` |
| Уровень · `level` | 11 |
| Предшествующие зоны · `from` | [Павшие руины](World-REGION_1.md#c1_fallen_ruins); [Старая застава](World-REGION_1.md#c1_old_outpost) |
| Финал региона · `finale` | Да |
| Монстры · `monsters` | [Упырь](Monsters.md#ghoul); [Скелет-воин](Monsters.md#skeleton_warrior); [Склепный призрак](Monsters.md#crypt_wraith); [Костяной лучник](Monsters.md#bone_archer) |
| Количество · `count` | 44; 57 |
| light | 0.7 |
| Добыча сундуков · `chestLoot` | [loot:CHEST_2](Tables-LOOT.md#loot-chest_2) |
| Страж · `boss` | [Владыка Склепа](Bosses.md#boss_crypt_lord) |
| Изначально осквернён · `corrupted` | [Осквернённый Вестник](Monsters.md#corrupted_2) |
| size | 72 |
| Таблицы · `tables` | [mob:monster](Tables-MODIFIER.md#mob-monster) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/campaign.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
