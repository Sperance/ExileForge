# Регионы, зоны, главы и квесты сюжета

Источник: `ktor-bestgame/src/main/resources/locale/{ru,en}.json`, ключи `region.*.name`, `map.<ZONE>.name` (+`.description`), `quest.chapter.*`, `quest.law.*`, `quest.story.*`. Биомы - клиент `core/src/main/resources/i18n/ui_{ru,en}.json`, `expedition.biome.*`. «оставить*» - оставить с оговоркой.

## Регионы (9)

Вердикты: оставить - 5, переименовать - 2, нужен лор - 1, оставить* - 1

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| REGION_1 | Берег изгнанников | The Exiles' Shore | оставить |  |  |  |
| REGION_2 | Кристальный хребет | The Crystal Ridge | оставить |  |  |  |
| REGION_3 | Выжженные пески | The Scorched Sands | оставить |  |  |  |
| REGION_4 | Изумрудные дебри | The Emerald Wilds | оставить |  |  |  |
| REGION_5 | Огненный рубеж | The Burning Frontier | оставить |  |  |  |
| REGION_6 | Бездна | The Abyss | нужен лор | Бездна | The Abyss | Имя совпадает с механикой «Бездна» (Расщелина, влияние, Сфера Бездны). По лору это одно и то же место-сила (глава VI) - оставить, но описать связь в кодексе; альтернатива «Дно Бездны / The Abyssal Depths». |
| REGION_7 | Расколотые небеса | Shattered Heavens | переименовать | Разбитое небо | The Broken Sky | Глава VII «Разбитое небо», трон «Трон разбитого неба», босс «Владыка разбитого неба» - а регион «Расколотые небеса / Shattered Heavens». Одно место - одно имя. |
| REGION_8 | Утонувшая империя | Drowned Empire | переименовать | Затопленная империя | The Flooded Empire | По лору империя не «утонула», её затопил сам император («Империя утонула не от шторма...»). Плюс разводит корни «утонувш-» (R1 утопленники, R2 Храм утонувших, R8). Альтернатива: оставить «Утонувшая империя», en «The Drowned Empire» (артикль). |
| REGION_9 | Чертоги богов | Halls of the Gods | оставить* | Чертоги богов | The Halls of the Gods | ru оставить; en - артикль, как у регионов 1-6. |

## Зоны кампании (они же карты Атласа: `equipment.MAP.zone` = «Карта: {зона}») (150)

Вердикты: оставить - 131, переименовать - 15, оставить* - 4

| Код | Сейчас ru | Сейчас en | Рег. | Биом | Ур. | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|---|---|---|
| C1_TIDAL_SHORE | Приливный берег | Tidal Shore | 1 | SHORE | 1 | оставить |  |  |  |
| C1_SHIP_GRAVEYARD | Кладбище кораблей | Ship Graveyard | 1 | SHORE | 3 | оставить |  |  |  |
| C1_BRINE_CAVES | Солёные пещеры | Brine Caves | 1 | CAVE | 3 | оставить |  |  |  |
| C1_GULL_CLIFFS | Птичьи утёсы | Gull Cliffs | 1 | SHORE | 3 | переименовать | Чаячьи утёсы | Gull Cliffs | ru «Птичьи» vs en «Gull»: выровнять (мелочь). |
| C1_DROWNED_LIGHTHOUSE | Маяк утопленников | Drowned Lighthouse | 1 | SHORE | 5 | оставить |  |  |  |
| C1_ROTTING_MIRE | Гнилая топь | Rotting Mire | 1 | MIRE | 5 | оставить |  |  |  |
| C1_MISTY_FERRY | Туманная переправа | Misty Ferry | 1 | MIRE | 5 | оставить |  |  |  |
| C1_SMUGGLERS_GROTTO | Грот контрабандистов | Smugglers' Grotto | 1 | CAVE | 5 | оставить |  |  |  |
| C1_WHISPERING_WOOD | Шепчущий лес | Whispering Wood | 1 | FOREST | 7 | оставить |  |  |  |
| C1_BLACK_CAUSEWAY | Чёрная гать | Black Causeway | 1 | MIRE | 7 | оставить |  |  |  |
| C1_WOLF_THICKET | Волчья чаща | Wolf Thicket | 1 | FOREST | 7 | оставить |  |  |  |
| C1_FALLEN_RUINS | Павшие руины | Fallen Ruins | 1 | RUINS | 9 | переименовать | Руины дозора | Watchfort Ruins | «Павшие руины» - тавтология; описание: «развалины крепости, всё ещё охраняемые гарнизоном», уникалка стража - «Кираса последнего часового». |
| C1_OLD_OUTPOST | Старая застава | Old Outpost | 1 | RUINS | 9 | оставить |  |  |  |
| C1_CHIEFTAINS_BARROW | Курган вождя | Chieftain's Barrow | 1 | CRYPT | 9 | оставить |  |  |  |
| C1_CRYPT_OF_EXILES | Склеп изгнанников | Crypt of Exiles | 1 | CRYPT | 11 | оставить |  |  |  |
| C2_MINERS_VILLAGE | Рудничный посёлок | Miners' Village | 2 | MINES | 13 | оставить |  |  |  |
| C2_CRYSTAL_MINES | Кристальные копи | Crystal Mines | 2 | MINES | 13 | оставить |  |  |  |
| C2_FOOTHILL_ROAD | Предгорный тракт | Foothill Road | 2 | FROST | 13 | оставить |  |  |  |
| C2_DEEP_ADITS | Глубинные штольни | Deep Adits | 2 | MINES | 15 | оставить |  |  |  |
| C2_GEODE_HOLLOW | Жеодовая лощина | Geode Hollow | 2 | MINES | 15 | оставить |  |  |  |
| C2_FROZEN_PASS | Замёрзший перевал | Frozen Pass | 2 | FROST | 15 | оставить |  |  |  |
| C2_ASHEN_WASTES | Пепельная пустошь | Ashen Wastes | 2 | ASH | 15 | переименовать | Пепельный склон | Ashen Slope | Пепельные зоны внутри «Кристального хребта» без лора; почти дубль R5 «Тлеющая пустошь». Сделать их вулканическим склоном Хребта (см. расширение лора R2). |
| C2_GLACIER_TOMBS | Ледниковые гробницы | Glacier Tombs | 2 | FROST | 17 | оставить |  |  |  |
| C2_TROLL_CAVES | Тролльи пещеры | Troll Caves | 2 | FROST | 17 | оставить |  |  |  |
| C2_EMBER_FIELDS | Тлеющие поля | Ember Fields | 2 | ASH | 17 | оставить |  |  |  |
| C2_STORM_PEAK | Грозовой пик | Storm Peak | 2 | FROST | 19 | оставить |  |  |  |
| C2_SUNKEN_SHRINE | Затонувшее святилище | Sunken Shrine | 2 | TEMPLE | 19 | оставить |  |  |  |
| C2_OBSIDIAN_QUARRY | Обсидиановый карьер | Obsidian Quarry | 2 | ASH | 19 | оставить |  |  |  |
| C2_SLAG_PITS | Шлаковые ямы | Slag Pits | 2 | ASH | 19 | оставить |  |  |  |
| C2_FROSTFANG_GLACIER | Ледник Хладоклыка | Frostfang Glacier | 2 | FROST | 21 | оставить |  |  |  |
| C2_ABYSSAL_STAIR | Лестница в бездну | Abyssal Stair | 2 | TEMPLE | 21 | переименовать | Затопленная лестница | Flooded Stair | Бездна в главе II преждевременна и дублирует R6 «Бесконечная лестница». Описание («ступени вниз, к тому, что молится в темноте») = спуск к Храму утонувших. Альтернатива: оставить как намёк на главу VI и прописать это в лоре. |
| C2_CINDER_CITADEL | Тлеющая цитадель | Cinder Citadel | 2 | ASH | 21 | переименовать | Тлеющий бастион | Smouldering Bastion | Дубль-корень с R5 «Цитадель Пламени» (финал главы V) и R5 «Тлеющая пустошь». |
| C2_DROWNED_TEMPLE | Храм утонувших | Drowned Temple | 2 | TEMPLE | 23 | оставить |  |  |  |
| C3_SALT_FLATS | Солончаки | Salt Flats | 3 | DESERT | 25 | оставить |  |  |  |
| C3_CARAVAN_ROAD | Караванный путь | Caravan Road | 3 | DESERT | 25 | оставить |  |  |  |
| C3_RED_GORGE | Красное ущелье | Red Gorge | 3 | CANYON | 25 | оставить |  |  |  |
| C3_SINGING_DUNES | Поющие барханы | Singing Dunes | 3 | DESERT | 27 | переименовать | Гудящие барханы | Booming Dunes | «Поющие» - сквозной мотив главы II (Поющий трон, Поющие жилы). Описание уже говорит «гудят». |
| C3_BURIED_CITY | Засыпанный город | Buried City | 3 | RUINS | 27 | оставить |  |  |  |
| C3_VULTURE_ROOST | Гнездовье стервятников | Vulture Roost | 3 | CANYON | 27 | оставить |  |  |  |
| C3_WITHERED_OASIS | Иссохший оазис | Withered Oasis | 3 | DESERT | 27 | оставить |  |  |  |
| C3_GLASS_DESERT | Стеклянная пустыня | Glass Desert | 3 | DESERT | 29 | переименовать | Оплавленная пустыня | Fused Desert | Стекло R3 и стекло R7 (Стеклянные дюны, Поющее стекло) сливаются. В R3 причина - жар Солнечного трона («сплавленный древним пламенем»). |
| C3_RAIDER_CAMP | Стан налётчиков | Raider Camp | 3 | CANYON | 29 | оставить |  |  |  |
| C3_SCORPION_NEST | Скорпионье гнездо | Scorpion Nest | 3 | CANYON | 29 | оставить |  |  |  |
| C3_MIRAGE_VALE | Долина миражей | Mirage Vale | 3 | DESERT | 31 | оставить |  |  |  |
| C3_TOMBS_OF_KINGS | Гробницы царей | Tombs of Kings | 3 | CRYPT | 31 | оставить |  |  |  |
| C3_SANDSTONE_ARCH | Песчаниковая арка | Sandstone Arch | 3 | CANYON | 31 | оставить |  |  |  |
| C3_DRY_RIVERBED | Сухое русло | Dry Riverbed | 3 | DESERT | 31 | оставить |  |  |  |
| C3_EMBALMERS_HALL | Зал бальзамировщиков | Embalmers' Hall | 3 | CRYPT | 33 | оставить |  |  |  |
| C3_SUN_TEMPLE | Храм Солнца | Temple of the Sun | 3 | TEMPLE | 33 | оставить |  |  |  |
| C3_STORM_MESA | Грозовое плато | Storm Mesa | 3 | CANYON | 33 | оставить |  |  |  |
| C3_SUN_KINGS_PYRAMID | Пирамида Солнечного царя | The Sun King's Pyramid | 3 | CRYPT | 35 | оставить |  |  |  |
| C4_JUNGLE_EDGE | Кромка джунглей | Jungle's Edge | 4 | JUNGLE | 37 | оставить |  |  |  |
| C4_MANGROVE_DELTA | Мангровая дельта | Mangrove Delta | 4 | MIRE | 37 | оставить |  |  |  |
| C4_OVERGROWN_ROAD | Заросшая дорога | Overgrown Road | 4 | RUINS | 37 | оставить |  |  |  |
| C4_ORCHID_GLADE | Орхидейная поляна | Orchid Glade | 4 | JUNGLE | 39 | оставить |  |  |  |
| C4_PANTHER_TRAIL | Тропа пантеры | Panther Trail | 4 | JUNGLE | 39 | оставить |  |  |  |
| C4_WASP_HOLLOW | Осиная лощина | Wasp Hollow | 4 | HIVE | 39 | оставить |  |  |  |
| C4_STRANGLER_GROVE | Роща душителей | Strangler Grove | 4 | JUNGLE | 39 | оставить |  |  |  |
| C4_SERPENT_SHRINE | Змеиное святилище | Serpent Shrine | 4 | TEMPLE | 41 | оставить |  |  |  |
| C4_AMBER_TUNNELS | Янтарные ходы | Amber Tunnels | 4 | HIVE | 41 | оставить |  |  |  |
| C4_WEEPING_CANOPY | Плачущий полог | Weeping Canopy | 4 | JUNGLE | 41 | оставить |  |  |  |
| C4_FUNGAL_LOWLANDS | Грибные низины | Fungal Lowlands | 4 | JUNGLE | 43 | оставить |  |  |  |
| C4_LOST_EXPEDITION | Стоянка пропавшей экспедиции | Lost Expedition Camp | 4 | RUINS | 43 | оставить |  |  |  |
| C4_BROOD_CHAMBERS | Выводковые камеры | Brood Chambers | 4 | HIVE | 43 | оставить |  |  |  |
| C4_FLOODED_THICKET | Затопленная чаща | Flooded Thicket | 4 | MIRE | 43 | оставить |  |  |  |
| C4_GREAT_TREE | Древо-великан | The Great Tree | 4 | JUNGLE | 45 | оставить |  |  |  |
| C4_MANTIS_GARDENS | Сады богомолов | Mantis Gardens | 4 | JUNGLE | 45 | оставить |  |  |  |
| C4_ROYAL_COMBS | Маточные соты | Royal Combs | 4 | HIVE | 45 | оставить |  |  |  |
| C4_HIVE_HEART | Сердце улья | Heart of the Hive | 4 | HIVE | 47 | оставить |  |  |  |
| C5_SCORCHED_FOOTHILLS | Опалённые предгорья | Scorched Foothills | 5 | VOLCANO | 49 | оставить |  |  |  |
| C5_BRIMSTONE_MINES | Серные копи | Brimstone Mines | 5 | MINES | 49 | оставить |  |  |  |
| C5_FRONTIER_FORT | Пограничный форт | Frontier Fort | 5 | CITADEL | 49 | оставить |  |  |  |
| C5_LAVA_FIELDS | Лавовые поля | Lava Fields | 5 | VOLCANO | 51 | оставить |  |  |  |
| C5_OBSIDIAN_SPIRES | Обсидиановые шпили | Obsidian Spires | 5 | VOLCANO | 51 | оставить |  |  |  |
| C5_SMELTERY | Плавильня | The Smeltery | 5 | MINES | 51 | оставить |  |  |  |
| C5_SIEGE_CAMP | Осадный лагерь | Siege Camp | 5 | CITADEL | 51 | оставить |  |  |  |
| C5_ASHFALL_VALLEY | Долина пеплопада | Ashfall Valley | 5 | ASH | 53 | оставить |  |  |  |
| C5_MAGMA_RIVER | Огненная река | Magma River | 5 | VOLCANO | 53 | переименовать | Река магмы | Magma River | ru «Огненная река» vs en «Magma River». |
| C5_OUTER_WALLS | Внешние стены | Outer Walls | 5 | CITADEL | 53 | оставить |  |  |  |
| C5_CALDERA_LAKE | Кальдерное озеро | Caldera Lake | 5 | VOLCANO | 55 | оставить |  |  |  |
| C5_CINDER_WASTES | Тлеющая пустошь | Cinder Wastes | 5 | ASH | 55 | оставить |  |  |  |
| C5_WAR_FORGES | Военные кузни | War Forges | 5 | CITADEL | 55 | оставить |  |  |  |
| C5_BURNING_BARRACKS | Горящие казармы | Burning Barracks | 5 | CITADEL | 55 | оставить |  |  |  |
| C5_DRAGONS_VENT | Драконье жерло | Dragon's Vent | 5 | VOLCANO | 57 | оставить |  |  |  |
| C5_INFERNAL_GATE | Инфернальные врата | Infernal Gate | 5 | CITADEL | 57 | переименовать | Врата Цитадели | Citadel Gate | Ада/преисподней в лоре нет; S5_GATE: «врата Цитадели охраняет пламя, у которого есть имя и клятва». Альтернатива: «Огненные врата / Fire Gate». |
| C5_MOLTEN_CORE | Расплавленное ядро | Molten Core | 5 | VOLCANO | 57 | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_MOLTEN_CORE |
| C5_FLAME_CITADEL | Цитадель Пламени | Citadel of Flame | 5 | CITADEL | 59 | оставить |  |  |  |
| C6_RIFT_EDGE | Край разлома | Rift's Edge | 6 | ABYSS | 61 | переименовать | Край пропасти | Chasm's Edge | «Разлом» в игре - режим Разлома и Владыка Разлома (глава VII). Трещина главы VI - «пропасть/расщелина». Связанные ключи: quest.story.S6_RIFT.name, монстр BOSS_RIFT_WATCHER. |
| C6_BLIGHTED_FIELDS | Чумные поля | Blighted Fields | 6 | BLIGHT | 61 | оставить |  |  |  |
| C6_FORSAKEN_ABBEY | Покинутое аббатство | Forsaken Abbey | 6 | TEMPLE | 61 | оставить |  |  |  |
| C6_WHISPERING_CHASM | Шепчущая пропасть | Whispering Chasm | 6 | ABYSS | 63 | оставить |  |  |  |
| C6_ROTTING_ORCHARD | Гниющий сад | Rotting Orchard | 6 | BLIGHT | 63 | оставить |  |  |  |
| C6_PLAGUE_VILLAGE | Чумная деревня | Plague Village | 6 | BLIGHT | 63 | оставить |  |  |  |
| C6_OSSUARY | Костница | The Ossuary | 6 | CRYPT | 63 | оставить |  |  |  |
| C6_VOID_SHORE | Берег пустоты | Void Shore | 6 | ABYSS | 65 | оставить |  |  |  |
| C6_SPORE_CATHEDRAL | Споровый собор | Spore Cathedral | 6 | BLIGHT | 65 | оставить |  |  |  |
| C6_FALLEN_STAR | Павшая звезда | Fallen Star | 6 | ABYSS | 65 | оставить |  |  |  |
| C6_CATACOMB_MAW | Пасть катакомб | Catacomb Maw | 6 | CRYPT | 67 | оставить |  |  |  |
| C6_WITHERED_WOOD | Иссохший лес | Withered Wood | 6 | BLIGHT | 67 | оставить |  |  |  |
| C6_ECLIPSE_ALTAR | Алтарь затмения | Eclipse Altar | 6 | TEMPLE | 67 | оставить |  |  |  |
| C6_STARLESS_DEEP | Беззвёздная глубь | Starless Deep | 6 | ABYSS | 67 | оставить |  |  |  |
| C6_ROTTEN_HEART | Гнилое сердце | The Rotten Heart | 6 | BLIGHT | 69 | оставить |  |  |  |
| C6_VEIL_OF_SHADOWS | Завеса теней | Veil of Shadows | 6 | ABYSS | 69 | оставить* | Завеса теней | Veil of Shadows | Оставить; в описании «бездна» со строчной - исправить на «Бездна». |
| C6_ENDLESS_STAIR | Бесконечная лестница | Endless Stair | 6 | ABYSS | 69 | оставить |  |  |  |
| C6_ABYSSAL_THRONE | Трон Бездны | Throne of the Abyss | 6 | ABYSS | 70 | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_ENTHRONED_REIGN |
| C7_SKY_LANDING | Небесная пристань | Sky Landing | 7 | SKYREACH | 71 | оставить |  |  |  |
| C7_GLASS_DUNES | Стеклянные дюны | Glass Dunes | 7 | GLASSWASTE | 71 | оставить* | Стеклянные дюны | Glass Dunes | Оставить: стекло R7 - от падающих звёзд/разбитого неба (описание это говорит). Переименовывается R3. |
| C7_THUNDER_STEPS | Громовые ступени | Thunder Steps | 7 | STORMPEAK | 71 | оставить |  |  |  |
| C7_BROKEN_BRIDGE | Разбитый мост | Broken Bridge | 7 | SKYREACH | 73 | оставить |  |  |  |
| C7_MIRROR_FLATS | Зеркальная равнина | Mirror Flats | 7 | GLASSWASTE | 73 | оставить |  |  |  |
| C7_LIGHTNING_SPIRE | Шпиль молний | Lightning Spire | 7 | STORMPEAK | 73 | оставить |  |  |  |
| C7_WIND_TEMPLE | Храм ветров | Temple of Winds | 7 | SKYREACH | 73 | оставить |  |  |  |
| C7_SHATTERED_CROWN | Расколотый венец | Shattered Crown | 7 | GLASSWASTE | 75 | оставить |  |  |  |
| C7_CLOUD_CITADEL | Облачная цитадель | Cloud Citadel | 7 | SKYREACH | 75 | оставить |  |  |  |
| C7_STORMWALL | Стена бурь | Stormwall | 7 | STORMPEAK | 75 | оставить |  |  |  |
| C7_PRISM_CANYON | Призменный каньон | Prism Canyon | 7 | GLASSWASTE | 75 | оставить |  |  |  |
| C7_FALLEN_ORRERY | Павший планетарий | Fallen Orrery | 7 | SKYREACH | 78 | оставить |  |  |  |
| C7_EYE_OF_THE_STORM | Око бури | Eye of the Storm | 7 | STORMPEAK | 78 | оставить |  |  |  |
| C7_SINGING_GLASS | Поющее стекло | Singing Glass | 7 | GLASSWASTE | 78 | переименовать | Звенящее стекло | Ringing Glass | Третий «поющий» топоним; босс «Стеклянный хор» сохраняет мотив звука. |
| C7_SHATTERED_THRONE | Трон разбитого неба | Throne of the Broken Sky | 7 | STORMPEAK | 80 | оставить |  |  |  |
| C8_SUNKEN_GATE | Затонувшие врата | Sunken Gate | 8 | SUNKEN | 81 | оставить |  |  |  |
| C8_CORAL_GARDENS | Коралловые сады | Coral Gardens | 8 | CORAL | 81 | оставить |  |  |  |
| C8_TIDE_VAULTS | Приливные хранилища | Tide Vaults | 8 | TIDEVAULT | 81 | оставить |  |  |  |
| C8_DROWNED_FORUM | Утонувший форум | Drowned Forum | 8 | SUNKEN | 83 | переименовать | Затонувший форум | Sunken Forum | Правило: места - «затонувший / Sunken», существа - «утонувший / Drowned». Глава VIII - «Затонувшая корона / The Sunken Crown». |
| C8_PEARL_REEF | Жемчужный риф | Pearl Reef | 8 | CORAL | 83 | оставить |  |  |  |
| C8_FLOODED_AQUEDUCT | Затопленный акведук | Flooded Aqueduct | 8 | TIDEVAULT | 83 | оставить |  |  |  |
| C8_LEGION_BARRACKS | Казармы легиона | Legion Barracks | 8 | SUNKEN | 83 | оставить |  |  |  |
| C8_KELP_CATHEDRAL | Собор водорослей | Kelp Cathedral | 8 | CORAL | 85 | оставить |  |  |  |
| C8_SEALED_CISTERN | Запечатанная цистерна | Sealed Cistern | 8 | TIDEVAULT | 85 | оставить |  |  |  |
| C8_SUNKEN_MARKET | Затонувший рынок | Sunken Market | 8 | SUNKEN | 85 | оставить |  |  |  |
| C8_BLEACHED_ATOLL | Выбеленный атолл | Bleached Atoll | 8 | CORAL | 85 | оставить |  |  |  |
| C8_TIDAL_ENGINE | Машина приливов | Tidal Engine | 8 | TIDEVAULT | 88 | оставить |  |  |  |
| C8_IMPERIAL_BATHS | Императорские термы | Imperial Baths | 8 | SUNKEN | 88 | оставить |  |  |  |
| C8_ABYSSAL_TRENCH | Бездонная впадина | Abyssal Trench | 8 | CORAL | 88 | переименовать | Бездонная впадина | Fathomless Trench | en «Abyssal» резервировать за Бездной (регион VI и механика). |
| C8_DROWNED_THRONE | Утонувший трон | The Drowned Throne | 8 | TIDEVAULT | 90 | переименовать | Затонувший трон | The Sunken Throne | То же правило; совпадает с «Затонувшая корона». |
| C9_GODS_THRESHOLD | Порог богов | Threshold of the Gods | 9 | GODHALL | 91 | оставить |  |  |  |
| C9_ASTRAL_SHORE | Астральный берег | Astral Shore | 9 | ASTRAL | 91 | оставить |  |  |  |
| C9_FIELDS_OF_OBLIVION | Поля забвения | Fields of Oblivion | 9 | OBLIVION | 91 | оставить |  |  |  |
| C9_HALL_OF_JUDGEMENT | Зал суда | Hall of Judgement | 9 | GODHALL | 93 | оставить |  |  |  |
| C9_CONSTELLATION_BRIDGE | Мост созвездий | Constellation Bridge | 9 | ASTRAL | 93 | оставить |  |  |  |
| C9_NAMELESS_ARCHIVE | Безымянный архив | Nameless Archive | 9 | OBLIVION | 93 | оставить |  |  |  |
| C9_SUNFORGE | Солнечная кузня | The Sunforge | 9 | GODHALL | 93 | оставить |  |  |  |
| C9_ASTRAL_ORCHARD | Астральный сад | Astral Orchard | 9 | ASTRAL | 95 | оставить |  |  |  |
| C9_HOLLOW_PANTHEON | Пустой пантеон | Hollow Pantheon | 9 | OBLIVION | 95 | оставить |  |  |  |
| C9_HALL_OF_WAR | Зал войны | Hall of War | 9 | GODHALL | 95 | оставить |  |  |  |
| C9_STARLESS_VOID | Беззвёздная пустота | Starless Void | 9 | OBLIVION | 95 | переименовать | Безвидная пустота | The Formless Void | Дубль-основа с R6 «Беззвёздная глубь»; S7_5: за печатями - «пустота без цвета и формы». |
| C9_CELESTIAL_OBSERVATORY | Небесная обсерватория | Celestial Observatory | 9 | ASTRAL | 98 | оставить |  |  |  |
| C9_THRONE_ROAD | Дорога к трону | Throne Road | 9 | GODHALL | 98 | оставить |  |  |  |
| C9_EDGE_OF_NOTHING | Край ничто | Edge of Nothing | 9 | OBLIVION | 98 | оставить |  |  |  |
| C9_LAST_THRONE | Последний трон | The Last Throne | 9 | GODHALL | 100 | оставить |  |  |  |

## Главы (9)

Вердикты: оставить - 8, переименовать - 1

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| REGION_1 | Глава I. Выброшенные морем | Chapter I. Cast Ashore | оставить |  |  |  |
| REGION_2 | Глава II. Голос кристаллов | Chapter II. Voice of the Crystals | оставить |  |  |  |
| REGION_3 | Глава III. Царь под солнцем | Chapter III. The King Beneath the Sun | оставить |  |  |  |
| REGION_4 | Глава IV. Сердце улья | Chapter IV. Heart of the Hive | оставить |  |  |  |
| REGION_5 | Глава V. Цитадель пламени | Chapter V. The Flame Citadel | переименовать | Глава V. Цитадель Пламени | Chapter V. The Citadel of Flame | Регистр и форма как у зоны «Цитадель Пламени / Citadel of Flame». |
| REGION_6 | Глава VI. Трон Бездны | Chapter VI. The Abyssal Throne | оставить |  |  |  |
| REGION_7 | Глава VII. Разбитое небо | Chapter VII. The Broken Sky | оставить |  |  |  |
| REGION_8 | Глава VIII. Затонувшая корона | Chapter VIII. The Sunken Crown | оставить |  |  |  |
| REGION_9 | Глава IX. Последний трон | Chapter IX. The Last Throne | оставить |  |  |  |

## Законы тронов (18)

Вердикты: оставить - 17, решение по стилю - 1

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| LAW_TIDE | Закон прилива | Law of the Tide | решение по стилю | Закон Прилива | Law of the Tide | В тексте главы - «Закон Прилива» (с прописной, как «Трон Прилива»). Выбрать один регистр для всех 18 законов (см. вопрос о регистре). |
| LAW_DROWNED_BREATH | Закон утопленника | Law of the Drowned Breath | оставить |  |  |  |
| LAW_CRYSTAL_WARD | Закон кристальной брони | Law of the Crystal Ward | оставить |  |  |  |
| LAW_FROZEN_VOICE | Закон ледяного голоса | Law of the Frozen Voice | оставить |  |  |  |
| LAW_SUN_CROWN | Закон солнечного венца | Law of the Sun Crown | оставить |  |  |  |
| LAW_EMBALMED_SUN | Закон бальзамированного солнца | Law of the Embalmed Sun | оставить |  |  |  |
| LAW_SWARM | Закон роя | Law of the Swarm | оставить |  |  |  |
| LAW_QUEEN_VENOM | Закон яда царицы | Law of the Queen's Venom | оставить |  |  |  |
| LAW_IRON_WALL | Закон железной стены | Law of the Iron Wall | оставить |  |  |  |
| LAW_FLAME_OATH | Закон огненной клятвы | Law of the Flame Oath | оставить |  |  |  |
| LAW_ABYSS_HUNGER | Закон голода Бездны | Law of the Abyss's Hunger | оставить |  |  |  |
| LAW_ABYSS_SKIN | Закон кожи Бездны | Law of the Abyssal Skin | оставить |  |  |  |
| LAW_STORM_EYE | Закон ока бури | Law of the Storm's Eye | оставить |  |  |  |
| LAW_WIND_STEP | Закон шага ветра | Law of the Wind's Step | оставить |  |  |  |
| LAW_DROWNED_MIND | Закон утонувшего разума | Law of the Drowned Mind | оставить |  |  |  |
| LAW_PEARL_SHELL | Закон жемчужной раковины | Law of the Pearl Shell | оставить |  |  |  |
| LAW_CROWNED_EDGE | Закон венчанного клинка | Law of the Crowned Edge | оставить |  |  |  |
| LAW_TENTH_THRONE | Закон десятого трона | Law of the Tenth Throne | оставить |  |  |  |

## Сюжетные задания (54)

Вердикты: оставить - 50, оставить* - 2, переименовать - 2

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| S1_LANDING | Выброшенный на берег | Washed Ashore | оставить |  |  |  |
| S1_WRECKS | Мёртвые корабли | Dead Ships | оставить |  |  |  |
| S1_LIGHT | Погасший маяк | The Dark Lighthouse | оставить |  |  |  |
| S1_SMUGGLERS | Долг контрабандистов | The Smugglers' Debt | оставить |  |  |  |
| S1_BARROW | Курган вождя | The Chieftain's Barrow | оставить |  |  |  |
| S1_CRYPT | Склеп изгнанников | Crypt of the Exiled | оставить |  |  |  |
| S2_VILLAGE | Рудничный посёлок | The Miners' Village | оставить |  |  |  |
| S2_MINES | Поющие жилы | Singing Veins | оставить |  |  |  |
| S2_TOOLS | Инструменты мастера | The Master's Tools | оставить |  |  |  |
| S2_PASS | Замёрзший перевал | The Frozen Pass | оставить |  |  |  |
| S2_PEAK | Грозовой пик | Storm Peak | оставить |  |  |  |
| S2_TEMPLE | Храм утонувших | The Drowned Temple | оставить |  |  |  |
| S3_CARAVAN | Караванный путь | The Caravan Road | оставить |  |  |  |
| S3_TRADE | Цена воды | The Price of Water | оставить |  |  |  |
| S3_RAIDERS | Стан налётчиков | The Raider Camp | оставить |  |  |  |
| S3_TOMBS | Гробницы царей | Tombs of Kings | оставить |  |  |  |
| S3_EMBALMER | Бальзамировщик | The Embalmer | оставить |  |  |  |
| S3_PYRAMID | Пирамида Солнечного царя | Pyramid of the Sun King | оставить |  |  |  |
| S4_EDGE | Кромка джунглей | The Jungle's Edge | оставить |  |  |  |
| S4_EXPEDITION | Пропавшая экспедиция | The Lost Expedition | оставить |  |  |  |
| S4_BROOD | Выводок | The Brood | оставить |  |  |  |
| S4_CRAFT | Янтарное снаряжение | Amber Gear | оставить |  |  |  |
| S4_TREE | Древо-великан | The Great Tree | оставить |  |  |  |
| S4_HIVE | Сердце улья | Heart of the Hive | оставить |  |  |  |
| S5_FORT | Пограничный форт | The Frontier Fort | оставить |  |  |  |
| S5_GENERAL | Генерал-перебежчик | The Turncoat General | оставить |  |  |  |
| S5_LEVEL | Закалка | Tempered | оставить* |  |  | совпадает с: skilltree.SCN_WA_3.name, skilltree.WIT_C5_1.name |
| S5_FORGES | Военные кузни | The War Forges | оставить |  |  |  |
| S5_GATE | Инфернальные врата | The Infernal Gate | оставить |  |  |  |
| S5_CITADEL | Цитадель пламени | The Flame Citadel | оставить |  |  |  |
| S6_RIFT | Край разлома | The Rift's Edge | переименовать | Край пропасти | The Chasm's Edge | Следует за переименованием зоны C6_RIFT_EDGE. |
| S6_ABBEY | Павший настоятель | The Fallen Abbot | переименовать | Падший настоятель | The Fallen Abbot | Квест «Павший настоятель», босс «Падший аббат», лор «Настоятель», фракция «Аббат» - одно лицо, четыре имени. |
| S6_OSSUARY | Костница | The Ossuary | оставить |  |  |  |
| S6_STAR | Павшая звезда | The Fallen Star | оставить |  |  |  |
| S6_STAIR | Бесконечная лестница | The Endless Stair | оставить |  |  |  |
| S6_THRONE | Трон Бездны | The Abyssal Throne | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_ENTHRONED_REIGN |
| S7_1 | Небесная пристань | Sky Landing | оставить |  |  |  |
| S7_2 | Зеркальная равнина | Mirror Flats | оставить |  |  |  |
| S7_3 | Храм ветров | Temple of Winds | оставить |  |  |  |
| S7_4 | Облачная цитадель | Cloud Citadel | оставить |  |  |  |
| S7_5 | Око бури | Eye of the Storm | оставить |  |  |  |
| S7_6 | Трон разбитого неба | Throne of the Broken Sky | оставить |  |  |  |
| S8_1 | Затонувшие врата | Sunken Gate | оставить |  |  |  |
| S8_2 | Жемчужный риф | Pearl Reef | оставить |  |  |  |
| S8_3 | Казармы легиона | Legion Barracks | оставить |  |  |  |
| S8_4 | Запечатанная цистерна | Sealed Cistern | оставить |  |  |  |
| S8_5 | Императорские термы | Imperial Baths | оставить |  |  |  |
| S8_6 | Утонувший трон | The Drowned Throne | оставить |  |  |  |
| S9_1 | Порог богов | Threshold of the Gods | оставить |  |  |  |
| S9_2 | Мост созвездий | Constellation Bridge | оставить |  |  |  |
| S9_3 | Солнечная кузня | The Sunforge | оставить |  |  |  |
| S9_4 | Пустой пантеон | Hollow Pantheon | оставить |  |  |  |
| S9_5 | Дорога к трону | Throne Road | оставить |  |  |  |
| S9_6 | Последний трон | The Last Throne | оставить |  |  |  |

## Биомы (клиент) (27)

Вердикты: оставить - 27

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| SHORE | побережье | shore | оставить |  |  |  |
| CAVE | пещеры | caves | оставить |  |  |  |
| MIRE | топь | mire | оставить |  |  |  |
| FOREST | лес | forest | оставить |  |  |  |
| RUINS | руины | ruins | оставить |  |  |  |
| CRYPT | склеп | crypt | оставить |  |  |  |
| MINES | копи | mines | оставить |  |  |  |
| ASH | пепел | ash | оставить |  |  |  |
| FROST | мороз | frost | оставить |  |  |  |
| TEMPLE | храм | temple | оставить |  |  |  |
| DESERT | пустыня | desert | оставить |  |  |  |
| CANYON | каньон | canyon | оставить |  |  |  |
| JUNGLE | джунгли | jungle | оставить |  |  |  |
| HIVE | улей | hive | оставить |  |  |  |
| VOLCANO | вулкан | volcano | оставить |  |  |  |
| CITADEL | цитадель | citadel | оставить |  |  |  |
| ABYSS | бездна | abyss | оставить |  |  |  |
| BLIGHT | гниль | blight | оставить |  |  | клиент ui_*.json; BLIGHT «гниль» - см. терминологию Скверны (Q7) |
| ASTRAL | астрал | astral | оставить |  |  |  |
| CORAL | коралловый риф | coral reef | оставить |  |  |  |
| GLASSWASTE | стеклянная пустошь | glass waste | оставить |  |  |  |
| GODHALL | чертог богов | hall of gods | оставить |  |  |  |
| OBLIVION | забвение | oblivion | оставить |  |  |  |
| SKYREACH | небесный предел | skyreach | оставить |  |  |  |
| STORMPEAK | грозовой пик | storm peak | оставить |  |  |  |
| SUNKEN | затонувшие руины | sunken ruins | оставить |  |  |  |
| TIDEVAULT | приливный свод | tide vault | оставить |  |  |  |
