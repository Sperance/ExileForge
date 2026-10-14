# Обычные монстры, семейства и приставки эссенций

Ключи `monster.<CODE>.name`; семейство - поле `form` в `content/campaign.json` (`monsters[]`). Модификаторы монстров и карт своих имён не имеют - игрок видит только строки статов (`stat.template.*`, клиент `enum.stat.*`), поэтому «имён строк» как отдельной категории нет. Исключение - 5 ключей `modifier.*.name` (скрытые префикс/суффикс, Скверна) - оставить.

## Монстры по семействам (85)

Вердикты: оставить - 71, переименовать - 13, оставить* - 1

| Код | Сейчас ru | Сейчас en | Семейство (form) | Черта | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|---|---|
| FALLEN_SERAPH | Падший серафим | Fallen Seraph | ANGEL | Разряд | оставить |  |  |  |
| PEARL_SIREN | Жемчужная сирена | Pearl Siren | ANGEL | Слабость | оставить |  |  |  |
| FALLEN_ARCHON | Падший архонт | Fallen Archon | ANGEL | Огненный шар | оставить |  |  |  |
| CAVE_BAT | Пещерная летучая мышь | Cave Bat | BAT | Кровопийца | оставить |  |  |  |
| CLIFF_VULTURE | Скальный стервятник | Cliff Vulture | BAT | Рваные раны | оставить |  |  |  |
| GARGOYLE | Горгулья | Gargoyle | BAT | Каменная кожа | оставить |  |  |  |
| STORM_HARPY | Грозовая гарпия | Storm Harpy | BAT | Статика | оставить |  |  |  |
| FERAL_WOLF | Одичавший волк | Feral Wolf | BEAST | Рваные раны | оставить |  |  |  |
| CINDER_HOUND | Угольная гончая | Cinder Hound | BEAST | Горящая кровь | оставить |  |  |  |
| SNOW_STALKER | Снежный охотник | Snow Stalker | BEAST | Обморожение | оставить |  |  |  |
| DUNE_STALKER | Барханный хищник | Dune Stalker | BEAST | Засада | оставить |  |  |  |
| EMERALD_PANTHER | Изумрудная пантера | Emerald Panther | BEAST | Разрыв | оставить |  |  |  |
| BLIGHT_ROTSTALKER | Гнилостайник | Rotstalker | BEAST | Гнилая охота | оставить |  |  |  |
| FROST_TROLL | Ледяной тролль | Frost Troll | BRUTE | Отрастание | оставить |  |  |  |
| CHASM_BRUTE | Громила Бездны | Abyss Brute | BRUTE | Сокрушитель | оставить |  |  |  |
| VOID_TITAN | Титан пустоты | Void Titan | BRUTE | Удар по земле | оставить |  |  |  |
| BLIGHT_DEVOURER | Пожиратель очагов | Hearth Devourer | BRUTE | Обжорство | оставить |  |  |  |
| SKY_SENTINEL | Небесный часовой | Sky Sentinel | CONSTRUCT | Обереги | оставить |  |  |  |
| TIDE_CONSTRUCT | Приливный конструкт | Tide Construct | CONSTRUCT | Ледяное дыхание | оставить |  |  |  |
| DIVINE_WARDEN | Божественный страж | Divine Warden | CONSTRUCT | Стена щитов | оставить |  |  |  |
| SHORE_CRAB | Береговой краб | Shore Crab | CRAB | Провокация | оставить |  |  |  |
| CORAL_BEHEMOTH | Коралловый исполин | Coral Behemoth | CRAB | Шипы | оставить |  |  |  |
| GLASS_STALKER | Стеклянный ловчий | Glass Stalker | CRYSTAL | Зоркий глаз | оставить |  |  |  |
| ASTRAL_BEAST | Астральный зверь | Astral Beast | CRYSTAL | Раскат грома | оставить |  |  |  |
| LAVA_IMP | Лавовый бес | Lava Imp | DEMON | Огненная смерть | оставить |  |  |  |
| INFERNAL_KNIGHT | Инфернальный рыцарь | Infernal Knight | DEMON | Боевой крик | переименовать | Рыцарь пламени | Flamesworn Knight | Армия Владыки Пламени (S5_FORGES «несут клеймо Пламенного трона»), не «инфернальные» демоны. |
| VOID_SPAWN | Порождение пустоты | Void Spawn | DEMON | Нестабильность | оставить |  |  |  |
| CHASM_SPAWN | Отродье Бездны | Abyss Spawn | DEMON | Берсерк | оставить |  |  |  |
| THUNDER_DRAKE | Громовой дрейк | Thunder Drake | DRAGON | Раскат грома | оставить |  |  |  |
| BRINE_DRAKE | Солёный дрейк | Brine Drake | DRAGON | Ледяное дыхание | оставить |  |  |  |
| STAR_DRAGON | Звёздный дракон | Star Dragon | DRAGON | Пронзающая стихия | оставить |  |  |  |
| SPORELING | Споровик | Sporeling | FUNGUS | Ядовитый плевок | оставить |  |  |  |
| BLIGHTCAP | Гнилошляп | Blightcap | FUNGUS | Отрастание | оставить |  |  |  |
| RUIN_SENTRY | Страж руин | Ruin Sentry | GOLEM | Провокация | переименовать | Каменный часовой | Stone Sentry | Коллизия с боссом «Страж Руин»; это голем. |
| CRYSTAL_GOLEM | Кристальный голем | Crystal Golem | GOLEM | Заряд бури | оставить |  |  |  |
| TEMPLE_GUARDIAN | Храмовый страж | Temple Guardian | GOLEM | Провокация | оставить |  |  |  |
| SANDSTONE_GOLEM | Песчаниковый голем | Sandstone Golem | GOLEM | Провокация | оставить |  |  |  |
| MAGMA_GOLEM | Магмовый голем | Magma Golem | GOLEM | Провокация | оставить |  |  |  |
| SHARD_GOLEM | Осколочный голем | Shard Golem | GOLEM | Удар по земле | оставить |  |  |  |
| DROWNED | Утопленник | Drowned | HUMANOID | Ледяное касание | оставить |  |  |  |
| MIRE_ZOMBIE | Болотный зомби | Mire Zombie | HUMANOID | Отрастание | переименовать | Болотный мертвяк | Mire Zombie | «Зомби» - единственное заимствование среди нежити (упырь, мертвец, утопленник). Мелочь. |
| THORN_DRYAD | Терновая дриада | Thorn Dryad | HUMANOID | Исцеление | оставить |  |  |  |
| TUNNEL_DWELLER | Обитатель туннелей | Tunnel Dweller | HUMANOID | Зоркий глаз | переименовать | Одержимый рудокоп | Entranced Miner | Глава II: рудокопы ушли слушать кристаллы и вернулись «послушными руками» (S2_VILLAGE after). |
| TEMPLE_ZEALOT | Храмовый фанатик | Temple Zealot | HUMANOID | Огненный шар | оставить |  |  |  |
| CANYON_RAIDER | Налётчик каньона | Canyon Raider | HUMANOID | Засада | оставить |  |  |  |
| BLOWGUN_HUNTER | Охотник с трубкой | Blowgun Hunter | HUMANOID | Ядовитый плевок | переименовать | Духовой стрелок | Blowgun Hunter | «Охотник с трубкой» читается как курильщик. |
| CITADEL_WARLOCK | Чернокнижник цитадели | Citadel Warlock | HUMANOID | Слабость | оставить |  |  |  |
| CHASM_HEXER | Проклинатель пустоты | Void Hexer | HUMANOID | Предсмертная молитва | переименовать | Проклинатель Бездны | Abyss Hexer | Монстр Бездны назван «пустоты / Void»; соседи - «Отродье Бездны», «Шептун Бездны». |
| GODSWORN_ZEALOT | Клятвенный фанатик | Godsworn Zealot | HUMANOID | Боевой клич | переименовать | Богоприсяжный фанатик | Godsworn Zealot | «Клятвенный фанатик» - калька, теряет «богам». |
| BLIGHT_PROPHET | Желчный пророк | Bile Prophet | HUMANOID | Желчная проповедь | оставить |  |  |  |
| GIANT_MANTIS | Богомол-великан | Giant Mantis | INSECT | Рваные раны | оставить |  |  |  |
| HIVE_DRONE | Трутень улья | Hive Drone | INSECT | Увёртка | оставить |  |  |  |
| HIVE_SOLDIER | Солдат улья | Hive Soldier | INSECT | Стена щитов | оставить |  |  |  |
| BROOD_SPITTER | Плевун выводка | Brood Spitter | INSECT | Ядовитый плевок | оставить |  |  |  |
| PLAGUE_WASP | Чумная оса | Plague Wasp | INSECT | Ядовитое жало | оставить |  |  |  |
| SAND_SCORPION | Песчаный скорпион | Sand Scorpion | SCORPION | Железная шкура | оставить |  |  |  |
| BRINE_SPITTER | Солёный плевун | Brine Spitter | SERPENT | Ледяная стрела | оставить |  |  |  |
| BOG_LURKER | Трясинный скрытень | Bog Lurker | SERPENT | Ядовитый плевок | оставить |  |  |  |
| DEEP_ONE | Глубинный | Deep One | SERPENT | Кража маны | оставить* | Глубинный | Deep One | Коллизия с фракцией «Глубинные». Предлагается переименовать фракцию (см. guild.faction.DEEPBOUND), монстра оставить. |
| FIRE_DRAKE | Огненный змей | Fire Drake | SERPENT | Огненное дыхание | переименовать | Огненный дрейк | Fire Drake | Остальные drake - «дрейк»; «Огненный змей» путается с «Лавовый змей». |
| ABYSSAL_EEL | Бездонный угорь | Abyssal Eel | SERPENT | Грозовой разряд | переименовать | Глубоководный угорь | Fathomless Eel | R8 - не Бездна; en «Abyssal» резервировать за Бездной. |
| SWAMP_LEECH | Болотная пиявка | Swamp Leech | SLUG | Отрастание | оставить |  |  |  |
| MAGMA_SLUG | Магмовый слизень | Magma Slug | SLUG | Огненная смерть | оставить |  |  |  |
| CHASM_LEECH | Пиявка глубин | Depth Leech | SLUG | Нестабильность | оставить |  |  |  |
| DEEP_LURKER | Глубинный скрытень | Deep Lurker | SLUG | Кольцо хаоса | оставить |  |  |  |
| WOOD_SPIDER | Лесной паук | Wood Spider | SPIDER | Паутина | оставить |  |  |  |
| SHARD_SCUTTLER | Осколочный ползун | Shard Scuttler | SPIDER | Статика | оставить |  |  |  |
| ABYSS_CRAWLER | Ползун бездны | Abyss Crawler | SPIDER | Касание бездны | переименовать | Ползун Бездны | Abyss Crawler | Бездна с прописной. |
| CHASM_STALKER | Ловец из пропасти | Chasm Stalker | SPIDER | Засада | оставить |  |  |  |
| SKELETON_WARRIOR | Скелет-воин | Skeleton Warrior | UNDEAD | Стена щитов | оставить |  |  |  |
| BONE_ARCHER | Костяной лучник | Bone Archer | UNDEAD | Зоркий глаз | оставить |  |  |  |
| GHOUL | Упырь | Ghoul | UNDEAD | Берсерк | оставить |  |  |  |
| ASH_REVENANT | Пепельный мертвец | Ash Revenant | UNDEAD | Выжженная земля | оставить |  |  |  |
| MUMMY | Мумия | Mummy | UNDEAD | Уязвимость | оставить |  |  |  |
| PLAGUE_BEARER | Чумоносец | Plague Bearer | UNDEAD | Нестабильность | оставить |  |  |  |
| DROWNED_LEGIONARY | Утопленник-легионер | Drowned Legionary | UNDEAD | Костяная броня | оставить |  |  |  |
| CRYPT_WRAITH | Склепный призрак | Crypt Wraith | WRAITH | Ледяная стрела | оставить |  |  |  |
| ICE_WRAITH | Ледяной призрак | Ice Wraith | WRAITH | Обморожение | оставить |  |  |  |
| ABYSSAL_ACOLYTE | Послушник бездны | Abyssal Acolyte | WRAITH | Кольцо хаоса | переименовать | Послушник Бездны | Abyssal Acolyte | Бездна - имя собственное, с прописной. |
| SANDSTORM_SPIRIT | Дух самума | Sandstorm Spirit | WRAITH | Разряд | оставить |  |  |  |
| ABYSSAL_SHADE | Тень бездны | Abyssal Shade | WRAITH | Энергетический покров | переименовать | Тень Бездны | Abyssal Shade | Бездна с прописной. |
| CHASM_WHISPERER | Шептун Бездны | Abyss Whisperer | WRAITH | Кража маны | оставить |  |  |  |
| ZEPHYR_WRAITH | Зефирный призрак | Zephyr Wraith | WRAITH | Увёртка | переименовать | Призрак ветра | Zephyr Wraith | «Зефирный» по-русски - про сладость (зефир). Смешно, ломает тон. |
| OBLIVION_SHADE | Тень забвения | Oblivion Shade | WRAITH | Пронзающая стихия | оставить |  |  |  |
| RIFT_SHADE | Тень украденного | Shade of the Stolen | WRAITH |  | оставить |  |  |  |

## Приставка монстра-носителя эссенции (`essence.<KIND>.monster`) (24)

Вердикты: оставить - 22, оставить* - 1, решение по стилю - 1

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| ANGER | Пылающий | Wrathful | оставить |  |  |  |
| ANGUISH | Мучимый | Anguished | оставить |  |  |  |
| CONTEMPT | Презирающий | Contemptuous | оставить |  |  |  |
| DELIRIUM | Бредящий | Delirious | оставить |  |  |  |
| DOUBT | Сомневающийся | Doubtful | оставить |  |  |  |
| DREAD | Трепещущий | Dreadful | оставить |  |  |  |
| ENVY | Завистливый | Envious | оставить |  |  |  |
| FEAR | Устрашающий | Fearsome | оставить |  |  |  |
| GREED | Ненасытный | Greedy | оставить* |  |  | совпадает с: skilltree.DUE_K0.name |
| HATRED | Ледяной | Hateful | решение по стилю | Ненавидящий | Hateful | ru «Ледяной» (стихия) vs en «Hateful» (чувство). Выбрать одну логику для всех 24 приставок; сейчас рассогласованы ANGER, HATRED, SORROW, TORMENT, WRATH, FEAR. |
| HORROR | Кошмарный | Horrifying | оставить |  |  |  |
| HYSTERIA | Истеричный | Hysterical | оставить |  |  |  |
| INSANITY | Безумный | Insane | оставить |  |  |  |
| LOATHING | Отвратный | Loathsome | оставить |  |  |  |
| MISERY | Несчастный | Miserable | оставить |  |  |  |
| RAGE | Яростный | Raging | оставить |  |  |  |
| SCORN | Надменный | Scornful | оставить |  |  |  |
| SORROW | Гниющий | Sorrowful | оставить |  |  |  |
| SPITE | Злобный | Spiteful | оставить |  |  |  |
| SUFFERING | Страждущий | Suffering | оставить |  |  |  |
| TORMENT | Искрящий | Tormenting | оставить |  |  |  |
| WOE | Скорбный | Woeful | оставить |  |  |  |
| WRATH | Мстительный | Vengeful | оставить |  |  |  |
| ZEAL | Рьяный | Zealous | оставить |  |  |  |
