# Классы, гильдии, профессии, работы, достижения, титулы, предначертания, Разлом

Ключи: `class.*`, `guild.*`, `profession.*`, `job.*`, `achievement.*`, `title.*`, `fate.*`, `rift.*` (сервер). Клиент держит копии части имён (`tester.mechanic.*`, `feature.*`, `map.legend_*`, `hall.board.*`).

## Классы (и старты дерева) (11)

Вердикты: оставить - 6, переименовать - 3, оставить* - 2

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| DUELIST | Дуэлянт | Duelist | оставить* | Дуэлянт | Duelist | Имена классов - PoE как есть (общие слова); решение - Q3/Q4. |
| MARAUDER | Мародёр | Marauder | оставить |  |  |  |
| RANGER | Охотница | Ranger | переименовать | Следопытка | Ranger | Класс «Охотница», старт дерева «Следопыт», лор «Следопытка диких земель». Одно имя на все три места. |
| SCION | Скион | Scion | переименовать | Потомок | Scion | В дереве старт уже «Потомок / Scion»; «Скион» - транслитерация. Лор класса «наследник знатного рода» ложится на тему рода и наследника тронов. |
| SHADOW | Тень | Shadow | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_UMBRA, rift.boon.RIFT_SHADOW.name |
| TEMPLAR | Храмовник | Templar | оставить |  |  |  |
| WITCH | Ведьма | Witch | оставить |  |  |  |
| DEX_START (старт дерева) | Следопыт | Ranger | переименовать | Следопытка | Ranger | Старт дерева должен совпадать с именем класса (см. class.RANGER). |
| INT_START (старт дерева) | Ведьма | Witch | оставить |  |  |  |
| SCION_START (старт дерева) | Потомок | Scion | оставить |  |  |  |
| STR_START (старт дерева) | Мародёр | Marauder | оставить |  |  |  |

## Фракции гильдий (3)

Вердикты: оставить - 2, переименовать - 1

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| WARDENS | Стражи Маяка | Lighthouse Wardens | оставить |  |  |  |
| HEEDING | Внемлющие | The Heeding | оставить |  |  |  |
| DEEPBOUND | Глубинные | The Deepbound | переименовать | Нисходящие | The Deepbound | Коллизия с монстром «Глубинный / Deep One» (R2, R8). «Мы спускаемся туда, откуда другие бегут». |

## Ранги, ветви, узлы, гербы гильдий (35)

Вердикты: оставить - 25, оставить* - 10

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| NOVICE | Новичок | Novice | оставить |  |  |  |
| BROTHER | Брат | Brother | оставить |  |  |  |
| VETERAN | Ветеран | Veteran | оставить* |  |  | совпадает с: achievement.VETERAN.name, title.VETERAN |
| CHAMPION | Чемпион | Champion | оставить |  |  |  |
| LEGEND | Легенда | Legend | оставить |  |  |  |
| SWORD | Меч | Sword | оставить |  |  |  |
| SHIELD | Щит | Shield | оставить* |  |  | совпадает с: skilltree.DUE_T0_1.name, skilltree.RAN_B1_2.name, skilltree.RAN_B1_3.name, skilltree.RAN_C1_4.name |
| AXE | Топор | Axe | оставить |  |  |  |
| HAMMER | Молот | Hammer | оставить* |  |  | совпадает с: atlas.node.ATLAS_CRAFT_A01.name, atlas.node.ATLAS_CRAFT_A02.name, atlas.node.ATLAS_CRAFT_A03.name, atlas.node.ATLAS_CRAFT_A04.name … |
| CROWN | Корона | Crown | оставить |  |  |  |
| SKULL | Череп | Skull | оставить |  |  |  |
| WOLF | Волк | Wolf | оставить |  |  |  |
| EAGLE | Орёл | Eagle | оставить |  |  |  |
| TOWER | Башня | Tower | оставить |  |  |  |
| FLAME | Пламя | Flame | оставить |  |  |  |
| SUN | Солнце | Sun | оставить |  |  |  |
| COIN | Монета | Coin | оставить |  |  |  |
| C_LIFE | Закалка тел | Hardened Bodies | оставить |  |  |  |
| C_DAMAGE | Острые клинки | Keen Blades | оставить |  |  |  |
| C_RESIST | Обереги | Wards | оставить* |  |  | совпадает с: trait.TRAIT_WARDED.name |
| C_FURY | Боевой клич | War Cry | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_WAR_SHOUT, trait.TRAIT_HOWL.name |
| C_BULWARK | Оплот | Bulwark | оставить* |  |  | совпадает с: rift.boon.RIFT_BULWARK.name |
| L_GOLD | Казначеи | Treasurers | оставить |  |  |  |
| L_EXPERIENCE | Наставники | Mentors | оставить |  |  |  |
| L_RARITY | Чутьё добычи | Loot Sense | оставить |  |  |  |
| L_QUANTITY | Жадность | Greed | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_AVARICE_GREED, skilltree.SCN_W_3.name, rift.curse.CURSE_GREED.name |
| L_CHESTS | Взломщики | Lockbreakers | оставить |  |  |  |
| E_TABS | Склады | Warehouses | оставить |  |  |  |
| E_CRAFT | Мастерские | Workshops | оставить |  |  |  |
| E_LIMIT | Щедрость | Generosity | оставить* |  |  | совпадает с: rift.mutator.MUT_GENEROUS.name |
| E_TAKES | Интенданты | Quartermasters | оставить |  |  |  |
| E_GROWTH | Слава | Renown | оставить* |  |  | совпадает с: rift.mutator.MUT_GLORY.name |
| COMBAT | Бой | Combat | оставить* |  |  | совпадает с: rift.node.FIGHT.name |
| LOOT | Добыча | Loot | оставить |  |  |  |
| ECONOMY | Хозяйство | Economy | оставить |  |  |  |

## Профессии (7)

Вердикты: оставить - 6, переименовать - 1

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| ALCHEMY | Алхимия | Alchemy | оставить |  |  |  |
| CARTOGRAPHY | Картография | Cartography | оставить |  |  |  |
| ENCHANTING | Зачарование | Enchanting | оставить |  |  |  |
| HERBALISM | Травничество | Herbalism | оставить |  |  |  |
| MINING | Горное дело | Mining | оставить |  |  |  |
| SMITHING | Кузнечное дело | Smithing | оставить |  |  |  |
| WOODCUTTING | Лесоруб | Woodcutting | переименовать | Лесорубство | Woodcutting | Остальные профессии - занятие («Горное дело», «Травничество»), эта - человек («Лесоруб»). |

## Работы ремёсел (138)

Вердикты: оставить - 133, переименовать - 3, решение по стилю - 1, оставить* - 1

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| ADAMANT_FORGING | Адамантовая ковка | Adamant Forging | оставить |  |  |  |
| ADAMANT_VEIN | Адамантовая жила | Adamant Vein | оставить |  |  |  |
| ALCHEMY_BREW | Варка: Сфера алхимии | Brew: Orb of Alchemy | оставить |  |  |  |
| ALTERATION_BREW | Варка: Сфера перемен | Brew: Orb of Alteration | оставить |  |  |  |
| AMBER_TAPPING | Подсочка смолы | Resin Tapping | оставить |  |  |  |
| AMETHYST_FLASK_BREW | Варка: аметистовая фляга | Brew: Amethyst Flask | оставить |  |  |  |
| AUGMENTATION_BREW | Варка: Сфера усовершенствования | Brew: Orb of Augmentation | оставить |  |  |  |
| BASALT_FLASK_BREW | Варка: базальтовая фляга | Brew: Basalt Flask | оставить |  |  |  |
| BAUBLE_BREW | Варка: Сфера качества | Brew: Orb of Quality | оставить |  |  |  |
| BIRCH_GROVE | Березняк | Birch Grove | оставить |  |  |  |
| BLOODWORT_PATCH | Заросли кровавника | Bloodwort Patch | переименовать | Заросли кровяницы | Bloodwort Patch | Предмет - «Кровяница», узел - «кровавника». |
| BLOOD_FLUX_BREW | Варка: примесь крови | Brew: Blood Flux | оставить |  |  |  |
| BOUNTY_BREW | Варка: Сфера отмены | Brew: Orb of Annulment | оставить |  |  |  |
| CAVE_MUSHROOMS | Пещерные грибы | Cave Mushrooms | оставить |  |  |  |
| CHAOS_BREW | Варка: Сфера хаоса | Brew: Chaos Orb | оставить |  |  |  |
| COPPER_FORGING | Медная ковка | Copper Forging | оставить |  |  |  |
| COPPER_VEIN | Медная жила | Copper Vein | оставить |  |  |  |
| DIAMOND_FLASK_BREW | Варка: алмазная фляга | Brew: Diamond Flask | оставить |  |  |  |
| DIVINE_BREW | Варка: Божественная сфера | Brew: Divine Orb | оставить |  |  |  |
| DIVINE_LIFE_FLASK_BREW | Варка: божественная фляга жизни | Brew: Divine Life Flask | оставить |  |  |  |
| DIVINE_MANA_FLASK_BREW | Варка: божественная фляга маны | Brew: Divine Mana Flask | оставить |  |  |  |
| ELITE_BREW | Варка: Знамение осквернения | Brew: Omen of Corruption | оставить |  |  |  |
| EMPOWERING_BREW | Варка: Знамение тира | Brew: Omen of Tiers | оставить |  |  |  |
| ESSENCE_BREW | Варка: Сфера раскрытия | Brew: Unveiling Orb | оставить |  |  |  |
| EXALTED_BREW | Варка: Сфера возвышения | Brew: Exalted Orb | оставить |  |  |  |
| FIRE_FLUX_BREW | Варка: огненная примесь | Brew: Fire Flux | оставить |  |  |  |
| FROST_FLUX_BREW | Варка: ледяная примесь | Brew: Frost Flux | оставить |  |  |  |
| GHOST_ASH_GROVE | Призрачный ясеневник | Ghost Ash Grove | оставить |  |  |  |
| GIANT_LIFE_FLASK_BREW | Варка: гигантская фляга жизни | Brew: Giant Life Flask | оставить |  |  |  |
| GIANT_MANA_FLASK_BREW | Варка: гигантская фляга маны | Brew: Giant Mana Flask | оставить |  |  |  |
| GILDED_BREW | Варка: Знамение великого возвышения | Brew: Omen of Greater Exaltation | оставить |  |  |  |
| GLOWCAP_GROTTO | Грот светляков | Glowcap Grotto | переименовать | Грот светошляпок | Glowcap Grotto | Предмет - «Светошляпка», узел - «Грот светляков» (насекомые). |
| GRAND_LIFE_FLASK_BREW | Варка: грандиозная фляга жизни | Brew: Grand Life Flask | оставить |  |  |  |
| GRAND_MANA_FLASK_BREW | Варка: грандиозная фляга маны | Brew: Grand Mana Flask | оставить |  |  |  |
| GRANITE_FLASK_BREW | Варка: гранитная фляга | Brew: Granite Flask | оставить |  |  |  |
| GREATER_LIFE_FLASK_BREW | Варка: великая фляга жизни | Brew: Greater Life Flask | оставить |  |  |  |
| GREATER_MANA_FLASK_BREW | Варка: великая фляга маны | Brew: Greater Mana Flask | оставить |  |  |  |
| HORDE_BREW | Варка: Сфера удачи | Brew: Orb of Chance | оставить |  |  |  |
| IRONWOOD_GROVE | Роща железного дерева | Ironwood Grove | оставить |  |  |  |
| IRON_FORGING | Железная ковка | Iron Forging | оставить |  |  |  |
| IRON_VEIN | Железная жила | Iron Vein | оставить |  |  |  |
| JADE_FLASK_BREW | Варка: нефритовая фляга | Brew: Jade Flask | оставить |  |  |  |
| LARGE_LIFE_FLASK_BREW | Варка: большая фляга жизни | Brew: Large Life Flask | оставить |  |  |  |
| LARGE_MANA_FLASK_BREW | Варка: большая фляга маны | Brew: Large Mana Flask | оставить |  |  |  |
| MAGUS_BREW | Варка: Сфера сожаления | Brew: Orb of Regret | оставить |  |  |  |
| MEDIUM_LIFE_FLASK_BREW | Варка: средняя фляга жизни | Brew: Medium Life Flask | оставить |  |  |  |
| MEDIUM_MANA_FLASK_BREW | Варка: средняя фляга маны | Brew: Medium Mana Flask | оставить |  |  |  |
| MERCY_BREW | Варка: Сфера Ваал | Brew: Vaal Orb | решение по стилю | Варка: Сфера Изнанки | Brew: Underside Orb | Ваал - народ мира PoE; при выборе «Изнанка» (Q10) - как в предложении |
| MITHRIL_FORGING | Мифриловая ковка | Mithril Forging | оставить |  |  |  |
| MITHRIL_VEIN | Мифриловая жила | Mithril Vein | оставить |  |  |  |
| MOON_LOTUS_POND | Пруд лунного лотоса | Moon Lotus Pond | оставить |  |  |  |
| OAK_GROVE | Дубрава | Oak Grove | оставить |  |  |  |
| PERIL_BREW | Варка: Благодатная сфера | Brew: Blessed Orb | оставить |  |  |  |
| QUARTZ_FLASK_BREW | Варка: кварцевая фляга | Brew: Quartz Flask | оставить |  |  |  |
| QUICKSILVER_FLASK_BREW | Варка: ртутная фляга | Brew: Quicksilver Flask | оставить |  |  |  |
| QUICK_FLUX_BREW | Варка: ртутная примесь | Brew: Quick Flux | оставить |  |  |  |
| REGAL_BREW | Варка: Сфера царей | Brew: Regal Orb | оставить |  |  |  |
| RUBY_DEPOSIT | Залежь рубина | Ruby Deposit | оставить |  |  |  |
| RUBY_FLASK_BREW | Варка: рубиновая фляга | Brew: Ruby Flask | оставить |  |  |  |
| SAPPHIRE_DEPOSIT | Залежь сапфира | Sapphire Deposit | оставить |  |  |  |
| SAPPHIRE_FLASK_BREW | Варка: сапфировая фляга | Brew: Sapphire Flask | оставить |  |  |  |
| SCOURING_BREW | Варка: Сфера очищения | Brew: Orb of Scouring | оставить |  |  |  |
| SCRIBE_BREW | Варка: Знамение выбора | Brew: Omen of Choice | оставить |  |  |  |
| SHADEBLOOM_PATCH | Поляна теневцвета | Shadebloom Glade | переименовать | Поляна теневого цвета | Shadebloom Patch | Предмет - «Теневой цвет», узел - «теневцвета»; en Glade/Patch. |
| SILVER_FLASK_BREW | Варка: серебряная фляга | Brew: Silver Flask | оставить |  |  |  |
| SILVER_FORGING | Серебряная ковка | Silver Forging | оставить |  |  |  |
| SILVER_VEIN | Серебряная жила | Silver Vein | оставить |  |  |  |
| SMALL_LIFE_FLASK_BREW | Варка: малая фляга жизни | Brew: Small Life Flask | оставить |  |  |  |
| SMALL_MANA_FLASK_BREW | Варка: малая фляга маны | Brew: Small Mana Flask | оставить |  |  |  |
| STONE_FLUX_BREW | Варка: каменная примесь | Brew: Stone Flux | оставить |  |  |  |
| STORM_FLUX_BREW | Варка: грозовая примесь | Brew: Storm Flux | оставить |  |  |  |
| SULPHUR_FLASK_BREW | Варка: сернистая фляга | Brew: Sulphur Flask | оставить |  |  |  |
| TOPAZ_DEPOSIT | Залежь топаза | Topaz Deposit | оставить |  |  |  |
| TOPAZ_FLASK_BREW | Варка: топазовая фляга | Brew: Topaz Flask | оставить |  |  |  |
| TRANSMUTATION_BREW | Варка: Сфера превращения | Brew: Orb of Transmutation | оставить |  |  |  |
| TREASURE_BREW | Варка: Знамение света | Brew: Omen of Light | оставить |  |  |  |
| VOID_ROOT_BED | Ложе корня пустоты | Void Root Bed | оставить |  |  |  |
| WARDEN_BREW | Варка: Сфера раскола | Brew: Fracturing Orb | оставить |  |  |  |
| WORMWOOD_PATCH | Заросли полыни | Wormwood Patch | оставить |  |  |  |
| YEW_GROVE | Тисовая роща | Yew Grove | оставить |  |  |  |
| CONDENSE_ESSENCE | Сгустить эссенцию | Condense Essence | оставить |  |  |  |
| CHART_REGION_1 | Карта: Берег изгнанников | Chart: The Exiles' Shore | оставить |  |  |  |
| CHART_REGION_2 | Карта: Кристальный хребет | Chart: The Crystal Ridge | оставить |  |  |  |
| CHART_REGION_3 | Карта: Выжженные пески | Chart: The Scorched Sands | оставить |  |  |  |
| CHART_REGION_4 | Карта: Изумрудные дебри | Chart: The Emerald Wilds | оставить |  |  |  |
| CHART_REGION_5 | Карта: Огненный рубеж | Chart: The Burning Frontier | оставить |  |  |  |
| CHART_REGION_6 | Карта: Бездна | Chart: The Abyss | оставить |  |  |  |
| CHART_REGION_7 | Карта: Расколотые небеса | Chart: Shattered Heavens | оставить |  |  |  |
| CHART_REGION_8 | Карта: Утонувшая империя | Chart: Drowned Empire | оставить |  |  |  |
| CHART_REGION_9 | Карта: Чертоги богов | Chart: Halls of the Gods | оставить |  |  |  |
| SCRIBE_BOOK_1 | Переписать книгу умений I | Rewrite a Skill Book I | оставить |  |  |  |
| SCRIBE_BOOK_2 | Переписать книгу умений II | Rewrite a Skill Book II | оставить |  |  |  |
| SCRIBE_BOOK_3 | Переписать книгу умений III | Rewrite a Skill Book III | оставить |  |  |  |
| STARMETAL_VEIN | Звёздная жила | Starmetal Vein | оставить |  |  |  |
| ONYX_DEPOSIT | Залежь оникса | Onyx Deposit | оставить |  |  |  |
| MINING_REFINE | Переплавка | Smelt Up | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_SMELT |
| STARBLOOM_PATCH | Поляна звёздных цветов | Starbloom Patch | оставить |  |  |  |
| WORLDROOT_GROVE | Лощина корня мира | Worldroot Hollow | оставить |  |  |  |
| HERBALISM_REFINE | Перегонка трав | Distil Up | оставить |  |  |  |
| ELDERHEART_GROVE | Роща древнего сердца | Elderheart Grove | оставить |  |  |  |
| WORLDTREE_GROVE | Роща мирового древа | Worldtree Grove | оставить |  |  |  |
| WOODCUTTING_REFINE | Выдержка древесины | Season Up | оставить |  |  |  |
| STARMETAL_FORGING | Ковка звёздного металла | Starmetal Forging | оставить |  |  |  |
| WORLD_FORGING | Ковка мирового горна | World Forging | оставить |  |  |  |
| STAR_FLUX_BREW | Варка: звёздная примесь | Brew Star Flux | оставить |  |  |  |
| CHART_TIERED | Карта с тиром | Chart: Tiered Map | оставить |  |  |  |
| CUT_JEWEL_1 | Огранить самоцвет I | Cut a Jewel I | оставить |  |  |  |
| CUT_JEWEL_2 | Огранить самоцвет II | Cut a Jewel II | оставить |  |  |  |
| BRONZE_TOOLMAKING | Бронзовые инструменты | Bronze Toolmaking | оставить |  |  |  |
| STEEL_TOOLMAKING | Стальные инструменты | Steel Toolmaking | оставить |  |  |  |
| RUNIC_TOOLMAKING | Рунные инструменты | Runic Toolmaking | оставить |  |  |  |
| POTION_EXPERIENCE_T1_BREW | Варка: малое зелье опыта | Brew: Minor Experience Potion | оставить |  |  |  |
| POTION_EXPERIENCE_T2_BREW | Варка: зелье опыта | Brew: Experience Potion | оставить |  |  |  |
| POTION_EXPERIENCE_T3_BREW | Варка: великое зелье опыта | Brew: Greater Experience Potion | оставить |  |  |  |
| POTION_GOLD_T1_BREW | Варка: малое зелье золота | Brew: Minor Gold Potion | оставить |  |  |  |
| POTION_GOLD_T2_BREW | Варка: зелье золота | Brew: Gold Potion | оставить |  |  |  |
| POTION_GOLD_T3_BREW | Варка: великое зелье золота | Brew: Greater Gold Potion | оставить |  |  |  |
| POTION_RARITY_T1_BREW | Варка: малое зелье удачи | Brew: Minor Fortune Potion | оставить |  |  |  |
| POTION_RARITY_T2_BREW | Варка: зелье удачи | Brew: Fortune Potion | оставить |  |  |  |
| POTION_RARITY_T3_BREW | Варка: великое зелье удачи | Brew: Greater Fortune Potion | оставить |  |  |  |
| POTION_RESIST_T1_BREW | Варка: малое зелье стойкости | Brew: Minor Warding Potion | оставить |  |  |  |
| POTION_RESIST_T2_BREW | Варка: зелье стойкости | Brew: Warding Potion | оставить |  |  |  |
| POTION_RESIST_T3_BREW | Варка: великое зелье стойкости | Brew: Greater Warding Potion | оставить |  |  |  |
| POTION_DAMAGE_T1_BREW | Варка: малое зелье ярости | Brew: Minor Fury Potion | оставить |  |  |  |
| POTION_DAMAGE_T2_BREW | Варка: зелье ярости | Brew: Fury Potion | оставить |  |  |  |
| POTION_DAMAGE_T3_BREW | Варка: великое зелье ярости | Brew: Greater Fury Potion | оставить |  |  |  |
| POTION_LIFE_T1_BREW | Варка: малое зелье жизни | Brew: Minor Vitality Potion | оставить |  |  |  |
| POTION_LIFE_T2_BREW | Варка: зелье жизни | Brew: Vitality Potion | оставить |  |  |  |
| POTION_LIFE_T3_BREW | Варка: великое зелье жизни | Brew: Greater Vitality Potion | оставить |  |  |  |
| SCARAB_CHESTS_CARVE | Резьба: скарабей тайников | Carve: Cache Scarab | оставить |  |  |  |
| SCARAB_PACK_CARVE | Резьба: скарабей стаи | Carve: Pack Scarab | оставить |  |  |  |
| SCARAB_RARITY_CARVE | Резьба: скарабей удачи | Carve: Fortune Scarab | оставить |  |  |  |
| CARVE_RUNE_1 | Вырезать руну I | Carve a Rune I | оставить |  |  |  |
| CARVE_RUNE_2 | Вырезать руну II | Carve a Rune II | оставить |  |  |  |
| CARVE_RUNE_3 | Вырезать руну III | Carve a Rune III | оставить |  |  |  |
| EVOLVE_SKILL_2 | Ритуал: тир II умения | Ritual: Skill Tier II | оставить |  |  |  |
| EVOLVE_SKILL_3 | Ритуал: тир III умения | Ritual: Skill Tier III | оставить |  |  |  |
| DESECRATE_FATE | Осквернение Предначертания | Fate Desecration | оставить |  |  |  |

## Достижения (38)

Вердикты: оставить - 31, переименовать - 4, оставить* - 2, решение по стилю - 1

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| SLAYER | Истребитель | Slayer | оставить |  |  |  |
| MAGIC_HUNTER | Охотник на волшебных | Magic Hunt | переименовать | Охотник на волшебных | Magic Hunter | en «Magic Hunt» - не титул, как прочие. |
| RARE_HUNTER | Охотник на редких | Rare Hunter | оставить |  |  |  |
| BOSS_BANE | Гроза стражей | Bane of Guardians | оставить |  |  |  |
| DEFILER | Осквернитель | Defiler | оставить |  |  |  |
| RESTLESS | Неупокоенный | Restless | оставить |  |  |  |
| BOTTOMLESS | Бездонный | Bottomless | оставить |  |  |  |
| WANDERER | Странник | Wanderer | оставить* |  |  | совпадает с: atlas.node.ATLAS_MAPS_A01.name, atlas.node.ATLAS_MAPS_A02.name, atlas.node.ATLAS_MAPS_A03.name, atlas.node.ATLAS_MAPS_A04.name … |
| TREASURE | Кладоискатель | Treasure Seeker | оставить |  |  |  |
| ESSENCE_GATHERER | Собиратель эссенций | Essence Gatherer | оставить |  |  |  |
| FINDER | Находчивый | Finder | оставить |  |  |  |
| COLLECTOR | Коллекционер | Collector | оставить |  |  |  |
| MAGNATE | Магнат | Magnate | оставить |  |  |  |
| PATRON | Меценат | Patron | оставить |  |  |  |
| PEDDLER | Торговец | Peddler | переименовать | Коробейник | Peddler | «Торговец» = NPC «Торговец» (город, карта). |
| BROKER | Маклер | Broker | оставить |  |  |  |
| ORB_MASTER | Мастер сфер | Orb Master | оставить |  |  |  |
| ESSENCE_ALCHEMIST | Алхимик эссенций | Essence Alchemist | оставить |  |  |  |
| REFLECTED | Отражённый | Reflected | оставить |  |  |  |
| ARTISAN | Ремесленник | Artisan | оставить |  |  |  |
| VETERAN | Ветеран | Veteran | оставить |  |  |  |
| PATHFINDER | Первопроходец | Pathfinder | оставить |  |  |  |
| CARTOGRAPHER | Картограф | Cartographer | оставить* |  |  | совпадает с: atlas.node.ATLAS_MAPS_C01.name, atlas.node.ATLAS_MAPS_C02.name, atlas.node.ATLAS_MAPS_C03.name, atlas.node.ATLAS_MAPS_C04.name … |
| TOWER_CLIMBER | Покоритель башни | Tower Climber | оставить |  |  |  |
| RUSH_MASTER | Мастер испытания боссов | Boss Trial Master | оставить |  |  |  |
| RUSH_HUNTER | Охотник на боссов | Boss Hunter | оставить |  |  |  |
| HOARD_KEEPER | Хранитель кладов | Hoard Keeper | оставить |  |  |  |
| TRIALIST | Испытуемый | Trialist | оставить |  |  |  |
| MAP_CONQUEROR | Завоеватель карт | Map Conqueror | оставить |  |  |  |
| VAAL_WALKER | Ходок Ваал | Vaal Walker | решение по стилю | Ходок Изнанки | Underside Walker | Ваал - народ мира PoE; при выборе «Изнанка» (Q10) - как в предложении |
| ORB_HOARDER | Копилка сфер | Orb Hoarder | оставить |  |  |  |
| ESSENCE_FINDER | Искатель эссенций | Essence Finder | оставить |  |  |  |
| MAP_FINDER | Собиратель карт | Map Finder | оставить |  |  |  |
| MYTH_SEEKER | Искатель мифов | Myth Seeker | оставить |  |  |  |
| RIFT_WALKER | Странник Разлома | Rift Walker | оставить |  |  |  |
| RIFT_SCORER | Счёт Разлома | Rift Score | переименовать | Счётчик Разлома | Rift Tallier | «Счёт Разлома» - не имя достижения. |
| RIFT_CONQUEROR | Покоритель Разлома | Rift Conqueror | оставить |  |  |  |
| RIFT_SOVEREIGN | Повелитель Разлома | Rift Sovereign | переименовать | Гроза Разлома | Bane of the Rift | То же, что титул. |

## Титулы (28)

Вердикты: оставить - 24, оставить* - 2, переименовать - 2

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| ARTISAN | Ремесленник | the Artisan | оставить |  |  |  |
| BOSS_BANE | Гроза стражей | Bane of Guardians | оставить |  |  |  |
| BOTTOMLESS | Бездонный | the Bottomless | оставить |  |  |  |
| BROKER | Маклер | the Broker | оставить |  |  |  |
| CARTOGRAPHER | Картограф | the Cartographer | оставить* |  |  | совпадает с: atlas.node.ATLAS_MAPS_C01.name, atlas.node.ATLAS_MAPS_C02.name, atlas.node.ATLAS_MAPS_C03.name, atlas.node.ATLAS_MAPS_C04.name … |
| COLLECTOR | Коллекционер | the Collector | оставить |  |  |  |
| DEFILER | Осквернитель | the Defiler | оставить |  |  |  |
| ESSENCE_ALCHEMIST | Алхимик эссенций | the Essence Alchemist | оставить |  |  |  |
| ESSENCE_GATHERER | Собиратель эссенций | the Essence Gatherer | оставить |  |  |  |
| MAGNATE | Магнат | the Magnate | оставить |  |  |  |
| ORB_MASTER | Мастер сфер | Orb Master | оставить |  |  |  |
| PATHFINDER | Первопроходец | the Pathfinder | оставить |  |  |  |
| PATRON | Меценат | the Patron | оставить |  |  |  |
| PEDDLER | Торговец | the Peddler | переименовать | Коробейник | the Peddler | То же. |
| RARE_HUNTER | Охотник на редких | the Rare Hunter | оставить |  |  |  |
| REFLECTED | Отражённый | the Reflected | оставить |  |  |  |
| RESTLESS | Неупокоенный | the Restless | оставить |  |  |  |
| SLAYER | Истребитель | the Slayer | оставить |  |  |  |
| TREASURE_SEEKER | Кладоискатель | the Treasure Seeker | оставить |  |  |  |
| VETERAN | Ветеран | the Veteran | оставить |  |  |  |
| WANDERER | Странник | the Wanderer | оставить* |  |  | совпадает с: atlas.node.ATLAS_MAPS_A01.name, atlas.node.ATLAS_MAPS_A02.name, atlas.node.ATLAS_MAPS_A03.name, atlas.node.ATLAS_MAPS_A04.name … |
| TOWER_LORD | Владыка башни | Tower Lord | оставить |  |  |  |
| RUSH_MASTER | Мастер испытания боссов | Boss Trial Master | оставить |  |  |  |
| TRIALIST | Испытуемый | Trialist | оставить |  |  |  |
| MAP_CONQUEROR | Завоеватель карт | Map Conqueror | оставить |  |  |  |
| MYTH_SEEKER | Искатель мифов | Myth Seeker | оставить |  |  |  |
| RIFT_SOVEREIGN | Повелитель Разлома | Rift Sovereign | переименовать | Гроза Разлома | Bane of the Rift | «Повелитель Разлома» почти совпадает с антагонистом «Владыка Разлома»: титул делает игрока им. |
| RIFT_CONQUEROR | Покоритель Разлома | Rift Conqueror | оставить |  |  |  |

## Предначертания (дары рода) (29)

Вердикты: оставить - 20, оставить* - 9

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| ANCESTRAL_BLOOD | Кровь предков | Ancestral Blood | оставить* |  |  | совпадает с: skilltree.DUE_C1_3.name |
| AVENGER | Мститель | Avenger | оставить |  |  |  |
| BLOOD_HARVEST | Кровавая жатва | Blood Harvest | оставить |  |  |  |
| ELEMENTAL_TRIAD | Триада стихий | Elemental Triad | оставить |  |  |  |
| SOUL_SPLIT | Раскол души | Soul Split | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_SOUL_SUNDER |
| EXECUTIONER | Палач | Executioner | оставить* |  |  | совпадает с: atlas.node.ATLAS_BOSSES_C01.name, atlas.node.ATLAS_BOSSES_C04.name, atlas.node.ATLAS_BOSSES_C07.name, atlas.node.ATLAS_BOSSES_C08.name … |
| LOYAL_PACK | Кровь вожака | Packleader's Blood | оставить |  |  |  |
| ICY_HEART | Ледяное сердце | Icy Heart | оставить |  |  |  |
| CURSED_METAL | Проклятый металл | Cursed Metal | оставить |  |  |  |
| STONE_SKIN | Каменная кожа | Stone Skin | оставить* |  |  | совпадает с: skill.MOB_STONESKIN.name, skilltree.MAR_C1_2.name, trait.TRAIT_SKILL_STONESKIN.name |
| RUNE_CIRCLE | Рунный круг | Rune Circle | оставить |  |  |  |
| CARTOGRAPHER | Картограф | Cartographer | оставить* |  |  | совпадает с: achievement.CARTOGRAPHER.name, title.CARTOGRAPHER, atlas.node.ATLAS_MAPS_C01.name, atlas.node.ATLAS_MAPS_C02.name … |
| TRACKER | Следопыт | Tracker | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_WAYFINDER, skilltree.DEX_START.name |
| TROPHY_HUNTER | Трофейщик | Trophy Hunter | оставить |  |  |  |
| KIN_HERBALIST | Травник рода | Kin Herbalist | оставить |  |  |  |
| OLD_SCHOOL | Старая школа | Old School | оставить |  |  |  |
| THRIFTY_SMITH | Бережливый кузнец | Thrifty Smith | оставить |  |  |  |
| FATE_SMITH | Кузнец судеб | Fate Smith | оставить |  |  |  |
| RESONANCE | Резонанс | Resonance | оставить* |  |  | совпадает с: skill.RESONANCE.name, enum.EnumStatStock.POWER_RUNE_RESONANCE |
| OVERFLOW | Переполнение | Overflow | оставить |  |  |  |
| MOMENTUM | Разгон | Momentum | оставить* |  |  | совпадает с: skilltree.RAN_N1_5.name |
| EQUILIBRIUM | Равновесие | Equilibrium | оставить* |  |  | совпадает с: skilltree.SCN_MP_1.name, skilltree.SCN_MP_15.name, skilltree.SCN_MP_8.name, skilltree.SCN_R0.name … |
| DAMAGE_MIRROR | Зеркало урона | Damage Mirror | оставить |  |  |  |
| LIVING_FORTRESS | Живая крепость | Living Fortress | оставить |  |  |  |
| GENEROUS_FATE | Щедрость судьбы | Generous Fate | оставить |  |  |  |
| BLOOD_FOR_BLOOD | Кровь за кровь | Blood for Blood | оставить |  |  |  |
| KIN_LUCK | Удача рода | Kin Luck | оставить |  |  |  |
| BLOOD_OATH | Обет крови | Blood Oath | оставить |  |  |  |
| PHANTOM_BLADE | Призрачный клинок | Phantom Blade | оставить |  |  |  |

## Разлом: дары, проклятия, мутаторы, законы, испытания, узлы (104)

Вердикты: оставить - 81, оставить* - 20, развести - 2, переименовать - 1

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| boon.RIFT_MIGHT | Мощь Разлома | Rift Might | оставить |  |  |  |
| boon.RIFT_FURY | Ярость | Fury | оставить* |  |  | совпадает с: phase.RAGE.name, skilltree.DUE_N1_0.name, skilltree.DUE_N1_1.name, skilltree.DUE_N1_3.name |
| boon.RIFT_HASTE | Поспешность | Haste | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| boon.RIFT_VIGOR | Живучесть | Vigour | оставить* |  |  | совпадает с: skilltree.DUE_C1_0.name, skilltree.DUE_C1_1.name, skilltree.MAR_C0_0.name, skilltree.MAR_IN_1.name … |
| boon.RIFT_WARD | Оберег | Ward | оставить |  |  |  |
| boon.RIFT_IRON | Железная кожа | Iron Skin | оставить* |  |  | совпадает с: skill.IRON_SKIN.name |
| boon.RIFT_SHADOW | Тень | Shadow | оставить* |  |  | совпадает с: class.SHADOW.name, skilltree.DEX_INT_START.name |
| boon.RIFT_KEEN | Зоркость | Keenness | оставить |  |  |  |
| boon.RIFT_ARCANE_EYE | Тайное око | Arcane Eye | оставить |  |  |  |
| boon.RIFT_CRUELTY | Жестокость | Cruelty | оставить* |  |  | совпадает с: skilltree.DUE_C5_0.name, skilltree.RAN_B3_2.name, skilltree.RAN_C3_2.name, skilltree.RAN_C3_3.name … |
| boon.RIFT_PRISM | Призма | Prism | оставить* |  |  | совпадает с: skilltree.SCN_EE_3.name, atlas.node.ATLAS_CRYSTALS_C01.name, atlas.node.ATLAS_CRYSTALS_C03.name, atlas.node.ATLAS_CRYSTALS_C04.name … |
| boon.RIFT_BRUTE | Грубая сила | Brute Force | оставить |  |  |  |
| boon.RIFT_BLIGHT | Скверна | Blight | оставить* |  |  | совпадает с: skilltree.WIT_C3_3.name |
| boon.RIFT_RESOLVE | Стойкость | Resolve | оставить* |  |  | совпадает с: skill.FORTITUDE.name, skilltree.MAR_C1_1.name, skilltree.TEM_C1_2.name, skilltree.TEM_T1_1.name |
| boon.RIFT_BULWARK | Оплот | Bulwark | оставить* |  |  | совпадает с: guild.node.C_BULWARK.name |
| boon.RIFT_THIRST | Жажда | Thirst | оставить |  |  |  |
| boon.RIFT_MENDING | Заживление | Mending | оставить |  |  |  |
| boon.RIFT_RECOVERY | Передышка | Respite | оставить |  |  |  |
| boon.RIFT_DRAUGHT | Крепкий настой | Strong Draught | оставить* |  |  | совпадает с: skilltree.MAR_C2_4.name |
| boon.RIFT_STONESKIN | Каменная плоть | Stoneflesh | оставить |  |  |  |
| boon.RIFT_PRECISION | Точность | Precision | оставить* |  |  | совпадает с: skill.PRECISION.name, skilltree.DUE_B5_1.name, skilltree.DUE_B5_2.name, skilltree.RAN_C3_0.name … |
| boon.RIFT_HUNGER | Голод | Hunger | оставить |  |  |  |
| boon.RIFT_MIND | Ясный разум | Clear Mind | оставить* |  |  | совпадает с: skilltree.TEM_C5_4.name |
| boon.RIFT_ESSENCE | Суть | Essence | оставить |  |  |  |
| boon.RIFT_EMBER_TRAIL | Тлеющий след | Ember Trail | оставить |  |  |  |
| boon.RIFT_AMBUSH | Засада | Ambush | развести |  |  | дубль внутри категории: rift.curse.CURSE_AMBUSH.name; совпадает с: trait.TRAIT_AMBUSHER.name |
| boon.RIFT_GRIEF | Скорбь | Grief | оставить |  |  |  |
| boon.RIFT_DEATHS_VEIL | Покров смерти | Death's Veil | оставить |  |  |  |
| boon.RIFT_EYE | Око Разлома | Eye of the Rift | оставить |  |  |  |
| boon.RIFT_SWARM | Рой | Swarm | оставить* |  |  | совпадает с: trait.TRAIT_SWARM.name |
| boon.RIFT_WORLDEATER | Пожиратель миров | Worldeater | оставить |  |  |  |
| boon.RIFT_STORM_VOLLEY | Грозовой залп | Storm Volley | оставить |  |  |  |
| boon.RIFT_ROT_SPELL | Гнилое слово | Rotting Word | оставить |  |  |  |
| boon.RIFT_BLOOD_SCENT | Запах крови | Blood Scent | оставить |  |  |  |
| boon.RIFT_EVERFLASK | Неиссякаемая фляга | Everflask | оставить |  |  |  |
| boon.RIFT_TIDE_SONG | Песнь прилива | Tide Song | оставить |  |  |  |
| boon.RIFT_HEARTBEAT | Сердцебиение | Heartbeat | оставить |  |  |  |
| boon.RIFT_TIME_LOOP | Петля времени | Time Loop | оставить |  |  |  |
| boon.RIFT_SLOW_TIME | Вязкое время | Slow Time | оставить |  |  |  |
| boon.RIFT_FIRST_BLOOD | Первая кровь | First Blood | оставить |  |  |  |
| boon.RIFT_METEOR | Звездопад | Starfall | оставить |  |  |  |
| boon.RIFT_VOID_ECHO | Эхо пустоты | Void Echo | оставить |  |  |  |
| boon.RIFT_ABYSSAL_HUNGER | Голод бездны | Abyssal Hunger | переименовать | Голод Бездны | Abyssal Hunger | Бездна с прописной. |
| curse.CURSE_FRAILTY | Хрупкость | Frailty | оставить |  |  |  |
| curse.CURSE_PERMEABLE | Проницаемость | Permeability | оставить |  |  |  |
| curse.CURSE_WEAKNESS | Слабость | Weakness | оставить* |  |  | совпадает с: skill.MOB_ENFEEBLE.name, trait.TRAIT_SKILL_ENFEEBLE.name |
| curse.CURSE_SLUGGISH | Вялость | Sluggishness | оставить |  |  |  |
| curse.CURSE_DRAINED | Истощение | Exhaustion | оставить |  |  |  |
| curse.CURSE_CRACKED_WARD | Расколотый щит | Cracked Ward | оставить |  |  |  |
| curse.CURSE_DULLED | Затупление | Dullness | оставить |  |  |  |
| curse.CURSE_DRYNESS | Сухость | Dryness | оставить |  |  |  |
| curse.CURSE_ROT | Гниль | Rot | оставить |  |  |  |
| curse.CURSE_WOUND | Рана | Wound | оставить |  |  |  |
| curse.CURSE_BLOATED | Раздутые | Bloated | оставить |  |  |  |
| curse.CURSE_SHARP | Острые | Sharp | оставить |  |  |  |
| curse.CURSE_SWIFT | Шустрые | Swift | оставить |  |  |  |
| curse.CURSE_CROWD | Толпа | Crowd | оставить |  |  |  |
| curse.CURSE_WRATH | Стихийный гнев | Elemental Wrath | оставить |  |  |  |
| curse.CURSE_VENOM | Ядовитые | Venomous | оставить |  |  |  |
| curse.CURSE_AMBUSH | Засада | Ambush | развести |  |  | дубль внутри категории: rift.boon.RIFT_AMBUSH.name; совпадает с: trait.TRAIT_AMBUSHER.name |
| curse.CURSE_GREED | Жадность | Greed | оставить* |  |  | совпадает с: skilltree.SCN_W_3.name, guild.node.L_QUANTITY.name |
| curse.CURSE_FOG | Туман | Fog | оставить |  |  |  |
| curse.CURSE_SLEEPLESS | Бессонница | Sleeplessness | оставить |  |  |  |
| mutator.MUT_ECHOING | Гулкое эхо | Resounding Echo | оставить |  |  |  |
| mutator.MUT_GENEROUS | Щедрость | Generosity | оставить* |  |  | совпадает с: guild.node.E_LIMIT.name |
| mutator.MUT_SOOTHING | Тихая гавань | Quiet Haven | оставить* | Тихая гавань | Quiet Haven | Оставить. |
| mutator.MUT_EMPOWERED | Сила Разлома | Rift Power | оставить |  |  |  |
| mutator.MUT_STALWART | Несгибаемость | Stalwart | оставить |  |  |  |
| mutator.MUT_FRAIL_FOES | Хилые враги | Frail Foes | оставить |  |  |  |
| mutator.MUT_BRISK | Бодрость | Brisk | оставить |  |  |  |
| mutator.MUT_LEECHING | Вампиризм | Leeching | оставить |  |  |  |
| mutator.MUT_FULL_FLASKS | Полные фляги | Full Flasks | оставить |  |  |  |
| mutator.MUT_GLORY | Слава | Glory | оставить* |  |  | совпадает с: guild.node.E_GROWTH.name |
| mutator.MUT_FIERCE | Свирепость | Ferocity | оставить |  |  |  |
| mutator.MUT_HARDY | Выносливые | Hardy | оставить |  |  |  |
| mutator.MUT_RESILIENT | Стойкие | Resilient | оставить |  |  |  |
| mutator.MUT_FRENZIED | Неистовство | Frenzy | оставить* |  |  | совпадает с: trait.TRAIT_FRENZIED.name |
| mutator.MUT_THRONGING | Давка | Throng | оставить |  |  |  |
| mutator.MUT_BRITTLE | Ломкость | Brittleness | оставить |  |  |  |
| mutator.MUT_SCARCE | Скудость | Scarcity | оставить |  |  |  |
| mutator.MUT_PIERCING | Пронзание | Piercing | оставить |  |  |  |
| mutator.MUT_CRITICAL | Меткость | Deadeye | оставить |  |  |  |
| mutator.MUT_BLEEDING | Кровотечение | Bleeding | оставить |  |  |  |
| law.ONE_ELEMENT | Одна стихия | One Element | оставить |  |  |  |
| law.HEALING_HURTS | Лечение ранит | Healing Hurts | оставить |  |  |  |
| law.NO_CRITS | Без критов | No Crits | оставить |  |  |  |
| law.HEAVY_BLOWS | Тяжкие удары | Heavy Blows | оставить |  |  |  |
| law.NO_SHIELD | Без щита | No Shield | оставить |  |  |  |
| law.NO_MANA | Без маны | No Mana | оставить |  |  |  |
| law.BLOOD_PRICE | Цена крови | Blood Price | оставить |  |  |  |
| law.REWIND | Время вспять | Rewind | оставить |  |  |  |
| trial.NO_FLASKS | Без фляг | No Flasks | оставить |  |  |  |
| trial.FAST | Быстрота | Swiftness | оставить |  |  |  |
| trial.HALF_LIFE | Без ран | Unscathed | оставить |  |  |  |
| trial.NO_RECOVERY | Без восстановления | No Recovery | оставить |  |  |  |
| trial.ONE_FOCUS | Один фокус | One Focus | оставить |  |  |  |
| node.FIGHT | Бой | Fight | оставить |  |  |  |
| node.ELITE | Элита | Elite | оставить |  |  |  |
| node.ALTAR | Алтарь сделки | Altar of Bargains | оставить |  |  |  |
| node.REST | Отдых | Rest | оставить |  |  |  |
| node.TREASURE | Сокровище | Treasure | оставить |  |  |  |
| node.FORGE | Кузница Разлома | Rift Forge | оставить |  |  |  |
| node.CHAMPION | Испытание чемпиона | Champion's Trial | оставить |  |  |  |
| node.GUARDIAN | Страж | Guardian | оставить |  |  |  |
