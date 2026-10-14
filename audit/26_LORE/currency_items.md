# Сферы, валюта, знамения, катализаторы, эссенции, материалы, сундуки, книги

Ключи `item.<CODE>.name` (+`item.<CODE>.description`), английское торговое имя - `locale/common.json` `item.<CODE>.trade`. Имена сфер продублированы в `enum.EnumCurrencyOrb.*` (сервер) и в клиенте `enum.orb.*`, `enum.catalyst.*` (таблица в конце).

## Сферы, печати Атласа, осколки, ключи испытаний (35)

Вердикты: оставить - 23, решение по стилю - 7, переименовать - 5

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| ORB_OF_TRANSMUTATION | Сфера превращения | Orb of Transmutation | оставить |  |  |  |
| ORB_OF_AUGMENTATION | Сфера усовершенствования | Orb of Augmentation | оставить |  |  |  |
| ORB_OF_ALTERATION | Сфера перемен | Orb of Alteration | оставить |  |  |  |
| ORB_OF_ALCHEMY | Сфера алхимии | Orb of Alchemy | оставить |  |  |  |
| REGAL_ORB | Сфера царей | Regal Orb | оставить |  |  |  |
| CHAOS_ORB | Сфера хаоса | Chaos Orb | оставить |  |  |  |
| SHARD_TRANSMUTATION | Осколок сферы превращения | Shard of Transmutation | переименовать | Осколок сферы превращения | Transmutation Shard | en «Shard of Transmutation» ≠ trade «Transmutation Shard»; выровнять. |
| SHARD_ALCHEMY | Осколок сферы алхимии | Shard of Alchemy | переименовать | Осколок сферы алхимии | Alchemy Shard | То же. |
| SHARD_CHAOS | Осколок сферы хаоса | Shard of Chaos | переименовать | Осколок сферы хаоса | Chaos Shard | То же. |
| EXALTED_ORB | Сфера возвышения | Exalted Orb | оставить |  |  |  |
| DIVINE_ORB | Божественная сфера | Divine Orb | оставить |  |  |  |
| ENGRAVERS_ORB | Сфера гравёра | Engraver's Orb | оставить |  |  |  |
| ORB_OF_ANNULMENT | Сфера отмены | Orb of Annulment | оставить |  |  |  |
| ORB_OF_SCOURING | Сфера очищения | Orb of Scouring | оставить |  |  |  |
| BLESSED_ORB | Благодатная сфера | Blessed Orb | оставить |  |  |  |
| VAAL_ORB | Сфера Ваал | Vaal Orb | решение по стилю | Сфера Изнанки | Orb of the Underside | Ваал - народ мира PoE, в лоре ExileForge его нет. Изнанка = то, что за печатями, куда ушёл Владыка Разлома (S7_5); вещь, тронутая Изнанкой, больше не меняется. Альтернатива: «Сфера порчи / Orb of Corruption» или оставить «Ваал» и дописать лор. Затронет ~74 строки «Ваал» (Ваал-зона, сундук, стражи, достижение). |
| ORB_OF_CHANCE | Сфера удачи | Orb of Chance | оставить |  |  |  |
| MIRROR_OF_KALANDRA | Зеркало Каландры | Mirror of Kalandra | переименовать | Зерцало Равнины | Mirror of the Flats | Каландра - собственное имя мира PoE. Зеркальная равнина (R7) «отражает тех, кем вы станете» - зеркало-двойник оттуда. Альтернатива: «Зеркало двойника / Twin Mirror». Ключи-копии: enum.EnumCurrencyOrb, клиент enum.orb. |
| ORB_OF_REGRET | Сфера сожаления | Orb of Regret | оставить |  |  |  |
| FRACTURING_ORB | Сфера раскола | Fracturing Orb | оставить |  |  |  |
| SHAPERS_ORB | Сфера Создателя | Shaper's Orb | решение по стилю | Сфера Зодчего | Architect's Orb | «Никто не помнит, кто воздвиг троны» - Зодчий. Создатель/Shaper - сущность мира PoE. Влияние, печать Атласа и узлы меняются вместе. |
| ELDER_ORB | Сфера Древнего | Elder Orb | решение по стилю | Сфера Пустоты | Void Orb | Древний/Elder - сущность PoE. Противник печатей - «ничто», «пустота без цвета и формы». Пустота уже живёт в R6/R9 (Берег пустоты, Мать пустоты). |
| ABYSS_ORB | Сфера Бездны | Abyss Orb | оставить |  |  |  |
| SEAL_OF_ALCHEMY | Печать алхимии | Seal of Alchemy | решение по стилю | Оттиск алхимии | Alchemy Imprint | «Печать» в лоре - девять печатей-тронов; эпилог: Атлас - «мир без печатей». Предмет Атласа с именем «Печать» спорит с лором. |
| SEAL_OF_THE_SHAPER | Печать Создателя | Seal of the Shaper | решение по стилю | Оттиск Зодчего | Architect's Imprint | См. Печать алхимии и Сферу Зодчего. |
| SEAL_OF_THE_ELDER | Печать Древнего | Seal of the Elder | решение по стилю | Оттиск Пустоты | Void Imprint | См. Печать алхимии и Сферу Пустоты. |
| SEAL_OF_THE_ABYSS | Печать Бездны | Seal of the Abyss | решение по стилю | Оттиск Бездны | Abyss Imprint | См. Печать алхимии. |
| UNVEILING_ORB | Сфера раскрытия | Unveiling Orb | оставить |  |  |  |
| INSIGHT_ORB | Сфера прозрения | Orb of Insight | оставить |  |  |  |
| QUALITY_ORB | Сфера качества | Orb of Quality | оставить |  |  |  |
| FATE_ASH | Пепел судьбы | Fate Ash | оставить |  |  |  |
| CREST_FRAGMENT | Фрагмент герба | Crest Fragment | оставить |  |  |  |
| RUSH_KEY | Ключ испытания боссов | Boss Trial Key | переименовать | Ключ испытания боссов | Boss Trial Key | Торговое имя (common.json) - «Rush Key», en-имя - «Boss Trial Key». Выровнять trade. |
| TOWER_SEAL | Печать башни | Tower Seal | оставить |  |  |  |
| RIFT_KEY | Ключ Разлома | Rift Key | оставить |  |  |  |

## Знамения и катализаторы (категория OMEN) (21)

Вердикты: оставить - 12, оставить* - 7, решение по стилю - 2

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| CATALYST_LIFE | Живительный катализатор | Vivid Catalyst | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| CATALYST_DEFENCE | Защитный катализатор | Fertile Catalyst | решение по стилю | Защитный катализатор | Warding Catalyst | ru «Защитный» ≠ en «Fertile» (PoE). Если PoE-имена катализаторов не держим - en по ru. |
| CATALYST_ELEMENTAL | Призматический катализатор | Prismatic Catalyst | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| CATALYST_PHYSICAL | Абразивный катализатор | Abrasive Catalyst | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| CATALYST_CHAOS | Ядовитый катализатор | Noxious Catalyst | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| CATALYST_SPEED | Бурный катализатор | Turbulent Catalyst | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| CATALYST_ATTRIBUTE | Врождённый катализатор | Intrinsic Catalyst | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| CATALYST_CASTER | Насыщенный катализатор | Imbued Catalyst | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| OMEN_SINISTRAL_CHAOS | Знамение левого хаоса | Omen of Sinistral Chaos | решение по стилю | Знамение хаоса: приставки | Omen of Chaos: Prefixes | «Левый/правый» - калька PoE2 Sinistral/Dextral, смысла игроку не несёт. Альтернатива лорная: «Знамение восхода/заката хаоса». Применить ко всем 8 левым/правым знамениям. |
| OMEN_DEXTRAL_CHAOS | Знамение правого хаоса | Omen of Dextral Chaos | оставить |  |  |  |
| OMEN_SINISTRAL_EXALTATION | Знамение левого возвышения | Omen of Sinistral Exaltation | оставить |  |  |  |
| OMEN_DEXTRAL_EXALTATION | Знамение правого возвышения | Omen of Dextral Exaltation | оставить |  |  |  |
| OMEN_GREATER_EXALTATION | Знамение великого возвышения | Omen of Greater Exaltation | оставить |  |  |  |
| OMEN_SINISTRAL_CORONATION | Знамение левой коронации | Omen of Sinistral Coronation | оставить |  |  |  |
| OMEN_DEXTRAL_CORONATION | Знамение правой коронации | Omen of Dextral Coronation | оставить |  |  |  |
| OMEN_SINISTRAL_ANNULMENT | Знамение левого аннулирования | Omen of Sinistral Annulment | оставить |  |  |  |
| OMEN_DEXTRAL_ANNULMENT | Знамение правого аннулирования | Omen of Dextral Annulment | оставить |  |  |  |
| OMEN_LIGHT | Знамение света | Omen of Light | оставить |  |  |  |
| OMEN_CORRUPTION | Знамение осквернения | Omen of Corruption | оставить |  |  |  |
| OMEN_CHOICE | Знамение выбора | Omen of Choice | оставить |  |  |  |
| OMEN_TIER | Знамение тира | Omen of Tiers | оставить |  |  |  |

## Эссенции (по видам; 8 ступеней имеют одно имя вида) (24)

Вердикты: оставить* - 21, переименовать - 3

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| GREED (8 шт.) | Шепчущая эссенция Жадности … Возвышенная эссенция Жадности | Whispering Essence of Greed | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| CONTEMPT (8 шт.) | Шепчущая эссенция Презрения … Возвышенная эссенция Презрения | Whispering Essence of Contempt | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| HATRED (8 шт.) | Шепчущая эссенция Ненависти … Возвышенная эссенция Ненависти | Whispering Essence of Hatred | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| WOE (8 шт.) | Шепчущая эссенция Скорби … Возвышенная эссенция Скорби | Whispering Essence of Woe | переименовать | Шепчущая эссенция Горести | Whispering Essence of Woe | «Скорби» дублирует корабль «Скорбь», дар Разлома «Скорбь», силу «Скорбь» и «Печали» (Sorrow). Для всех 8 ступеней. |
| FEAR (8 шт.) | Шепчущая эссенция Страха … Возвышенная эссенция Страха | Whispering Essence of Fear | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| ANGER (8 шт.) | Шепчущая эссенция Гнева … Возвышенная эссенция Гнева | Whispering Essence of Anger | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| TORMENT (8 шт.) | Шепчущая эссенция Муки … Возвышенная эссенция Муки | Whispering Essence of Torment | переименовать | Шепчущая эссенция Терзания | Whispering Essence of Torment | «Муки» (Torment) и «Мучения» (Anguish) - синонимы. Для всех 8 ступеней. |
| SORROW (8 шт.) | Шепчущая эссенция Печали … Возвышенная эссенция Печали | Whispering Essence of Sorrow | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| RAGE (8 шт.) | Шепчущая эссенция Ярости … Возвышенная эссенция Ярости | Whispering Essence of Rage | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| SUFFERING (8 шт.) | Шепчущая эссенция Страдания … Возвышенная эссенция Страдания | Whispering Essence of Suffering | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| WRATH (8 шт.) | Шепчущая эссенция Возмездия … Возвышенная эссенция Возмездия | Whispering Essence of Wrath | переименовать | Шепчущая эссенция Возмездия | Whispering Essence of Vengeance | ru «Возмездия», монстр «Мстительный», en «Wrath» (PoE). Выровнять en по ru для всех 8 ступеней. |
| DOUBT (8 шт.) | Шепчущая эссенция Сомнения … Возвышенная эссенция Сомнения | Whispering Essence of Doubt | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| LOATHING (8 шт.) | Шепчущая эссенция Отвращения … Возвышенная эссенция Отвращения | Whispering Essence of Loathing | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| ZEAL (8 шт.) | Шепчущая эссенция Рвения … Возвышенная эссенция Рвения | Whispering Essence of Zeal | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| ANGUISH (8 шт.) | Шепчущая эссенция Мучения … Возвышенная эссенция Мучения | Whispering Essence of Anguish | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| SPITE (8 шт.) | Шепчущая эссенция Злобы … Возвышенная эссенция Злобы | Whispering Essence of Spite | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| SCORN (8 шт.) | Шепчущая эссенция Надменности … Возвышенная эссенция Надменности | Whispering Essence of Scorn | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| ENVY (8 шт.) | Шепчущая эссенция Зависти … Возвышенная эссенция Зависти | Whispering Essence of Envy | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| MISERY (8 шт.) | Шепчущая эссенция Несчастья … Возвышенная эссенция Несчастья | Whispering Essence of Misery | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| DREAD (8 шт.) | Шепчущая эссенция Трепета … Возвышенная эссенция Трепета | Whispering Essence of Dread | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| INSANITY (1 шт.) | Эссенция Безумия | Essence of Insanity | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| HORROR (1 шт.) | Эссенция Кошмара | Essence of Horror | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| DELIRIUM (1 шт.) | Эссенция Бреда | Essence of Delirium | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |
| HYSTERIA (1 шт.) | Эссенция Истерии | Essence of Hysteria | оставить* |  |  | имя PoE «как есть» (вопрос Q3) |

## Ступени эссенций (8)

Вердикты: оставить* - 8

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| DEAFENING | Оглушающая | Deafening | оставить* |  |  | ступени PoE «как есть»; ложатся на лор «голосов» (шёпот печатей, пение кристаллов) - Q3 |
| MUTTERING | Бормочущая | Muttering | оставить* |  |  | ступени PoE «как есть»; ложатся на лор «голосов» (шёпот печатей, пение кристаллов) - Q3 |
| SCREAMING | Кричащая | Screaming | оставить* |  |  | ступени PoE «как есть»; ложатся на лор «голосов» (шёпот печатей, пение кристаллов) - Q3 |
| SHRIEKING | Визжащая | Shrieking | оставить* |  |  | ступени PoE «как есть»; ложатся на лор «голосов» (шёпот печатей, пение кристаллов) - Q3 |
| WAILING | Воющая | Wailing | оставить* |  |  | ступени PoE «как есть»; ложатся на лор «голосов» (шёпот печатей, пение кристаллов) - Q3 |
| WEEPING | Плачущая | Weeping | оставить* |  |  | ступени PoE «как есть»; ложатся на лор «голосов» (шёпот печатей, пение кристаллов) - Q3 |
| WHISPERING | Шепчущая | Whispering | оставить* |  |  | ступени PoE «как есть»; ложатся на лор «голосов» (шёпот печатей, пение кристаллов) - Q3 |
| EXALTED | Возвышенная | Exalted | оставить* |  |  | ступени PoE «как есть»; ложатся на лор «голосов» (шёпот печатей, пение кристаллов) - Q3 |

## Материалы, руны, зелья, скарабеи (78)

Вердикты: оставить - 78

| Код | Сейчас ru | Сейчас en | Подкат. | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|---|
| WOOD_LOG | Деревянное бревно | Wooden Log | LOG | оставить |  |  |  |
| STONE_ROUGH | Грубый камень | Rough Stone | STONE | оставить |  |  |  |
| STONE_POLISHED | Шлифованный камень | Polished Stone | STONE | оставить |  |  |  |
| COPPER_ORE | Медная руда | Copper Ore | MINING | оставить |  |  |  |
| IRON_ORE | Железная руда | Iron Ore | MINING | оставить |  |  |  |
| SILVER_ORE | Серебряная руда | Silver Ore | MINING | оставить |  |  |  |
| MITHRIL_ORE | Мифриловая руда | Mithril Ore | MINING | оставить |  |  |  |
| ADAMANT_ORE | Адамантовая руда | Adamant Ore | MINING | оставить |  |  |  |
| STARMETAL_ORE | Звёздная руда | Starmetal Ore | MINING | оставить |  |  |  |
| TOPAZ | Топаз | Topaz | MINING | оставить |  |  |  |
| SAPPHIRE | Сапфир | Sapphire | MINING | оставить |  |  |  |
| RUBY | Рубин | Ruby | MINING | оставить |  |  |  |
| ONYX | Оникс | Onyx | MINING | оставить |  |  |  |
| WORMWOOD | Полынь | Wormwood | HERBALISM | оставить |  |  |  |
| CAVE_MUSHROOM | Пещерный гриб | Cave Mushroom | HERBALISM | оставить |  |  |  |
| BLOODWORT | Кровяница | Bloodwort | HERBALISM | оставить |  |  |  |
| AMBER_RESIN | Янтарная смола | Amber Resin | HERBALISM | оставить |  |  |  |
| SHADEBLOOM | Теневой цвет | Shadebloom | HERBALISM | оставить |  |  |  |
| GLOWCAP | Светошляпка | Glowcap | HERBALISM | оставить |  |  |  |
| MOON_LOTUS | Лунный лотос | Moon Lotus | HERBALISM | оставить |  |  |  |
| VOID_ROOT | Корень пустоты | Void Root | HERBALISM | оставить |  |  |  |
| WORLDROOT | Корень мира | Worldroot | HERBALISM | оставить |  |  |  |
| STARBLOOM | Звёздный цветок | Starbloom | HERBALISM | оставить |  |  |  |
| BIRCH_LOG | Берёзовое бревно | Birch Log | WOODCUTTING | оставить |  |  |  |
| OAK_LOG | Дубовое бревно | Oak Log | WOODCUTTING | оставить |  |  |  |
| YEW_LOG | Тисовое бревно | Yew Log | WOODCUTTING | оставить |  |  |  |
| IRONWOOD_LOG | Бревно железного дерева | Ironwood Log | WOODCUTTING | оставить |  |  |  |
| GHOST_ASH_LOG | Бревно призрачного ясеня | Ghost Ash Log | WOODCUTTING | оставить |  |  |  |
| WORLDTREE_LOG | Бревно мирового древа | Worldtree Log | WOODCUTTING | оставить |  |  |  |
| ELDERHEART_LOG | Бревно древнего сердца | Elderheart Log | WOODCUTTING | оставить |  |  |  |
| BARK | Кора | Bark | WOODCUTTING | оставить |  |  |  |
| SEEDS | Семена | Seeds | HERBALISM | оставить |  |  |  |
| BLOOD_FLUX | Кровавый флюс | Blood Flux | ALCHEMY | оставить |  |  |  |
| STAR_FLUX | Звёздный флюс | Star Flux | ALCHEMY | оставить |  |  |  |
| STONE_FLUX | Каменный флюс | Stone Flux | ALCHEMY | оставить |  |  |  |
| FIRE_FLUX | Огненный флюс | Fire Flux | ALCHEMY | оставить |  |  |  |
| FROST_FLUX | Морозный флюс | Frost Flux | ALCHEMY | оставить |  |  |  |
| STORM_FLUX | Грозовой флюс | Storm Flux | ALCHEMY | оставить |  |  |  |
| QUICK_FLUX | Быстрый флюс | Quick Flux | ALCHEMY | оставить |  |  |  |
| POTION_EXPERIENCE_T1 | Малое зелье опыта | Minor Experience Potion | POTION | оставить |  |  |  |
| POTION_EXPERIENCE_T2 | Зелье опыта | Experience Potion | POTION | оставить |  |  |  |
| POTION_EXPERIENCE_T3 | Великое зелье опыта | Greater Experience Potion | POTION | оставить |  |  |  |
| POTION_GOLD_T1 | Малое зелье золота | Minor Gold Potion | POTION | оставить |  |  |  |
| POTION_GOLD_T2 | Зелье золота | Gold Potion | POTION | оставить |  |  |  |
| POTION_GOLD_T3 | Великое зелье золота | Greater Gold Potion | POTION | оставить |  |  |  |
| POTION_RARITY_T1 | Малое зелье удачи | Minor Fortune Potion | POTION | оставить |  |  |  |
| POTION_RARITY_T2 | Зелье удачи | Fortune Potion | POTION | оставить |  |  |  |
| POTION_RARITY_T3 | Великое зелье удачи | Greater Fortune Potion | POTION | оставить |  |  |  |
| POTION_RESIST_T1 | Малое зелье стойкости | Minor Warding Potion | POTION | оставить |  |  |  |
| POTION_RESIST_T2 | Зелье стойкости | Warding Potion | POTION | оставить |  |  |  |
| POTION_RESIST_T3 | Великое зелье стойкости | Greater Warding Potion | POTION | оставить |  |  |  |
| POTION_DAMAGE_T1 | Малое зелье ярости | Minor Fury Potion | POTION | оставить |  |  |  |
| POTION_DAMAGE_T2 | Зелье ярости | Fury Potion | POTION | оставить |  |  |  |
| POTION_DAMAGE_T3 | Великое зелье ярости | Greater Fury Potion | POTION | оставить |  |  |  |
| POTION_LIFE_T1 | Малое зелье жизни | Minor Vitality Potion | POTION | оставить |  |  |  |
| POTION_LIFE_T2 | Зелье жизни | Vitality Potion | POTION | оставить |  |  |  |
| POTION_LIFE_T3 | Великое зелье жизни | Greater Vitality Potion | POTION | оставить |  |  |  |
| SCARAB_CHESTS | Скарабей тайников | Cache Scarab | SCARAB | оставить |  |  |  |
| SCARAB_PACK | Скарабей стаи | Pack Scarab | SCARAB | оставить |  |  |  |
| SCARAB_RARITY | Скарабей удачи | Fortune Scarab | SCARAB | оставить |  |  |  |
| RUNE_SPLIT | Руна раскола | Splitting Rune | SPREAD | оставить |  |  |  |
| RUNE_CHAIN | Руна цепи | Chain Rune | SPREAD | оставить |  |  |  |
| RUNE_ECHO | Руна эха | Echo Rune | SPREAD | оставить |  |  |  |
| RUNE_FORK | Руна развилки | Fork Rune | SPREAD | оставить |  |  |  |
| RUNE_EMBER | Руна углей | Ember Rune | ELEMENT | оставить |  |  |  |
| RUNE_RIME | Руна инея | Rime Rune | ELEMENT | оставить |  |  |  |
| RUNE_SURGE | Руна разряда | Surge Rune | ELEMENT | оставить |  |  |  |
| RUNE_BLIGHT | Руна яда | Venom Rune | ELEMENT | оставить |  |  |  |
| RUNE_HEMORRHAGE | Руна кровотечения | Hemorrhage Rune | ELEMENT | оставить |  |  |  |
| RUNE_HASTE | Руна спешки | Haste Rune | TEMPO | оставить |  |  |  |
| RUNE_THRIFT | Руна бережливости | Thrift Rune | TEMPO | оставить |  |  |  |
| RUNE_READINESS | Руна готовности | Readiness Rune | TEMPO | оставить |  |  |  |
| RUNE_FERVOR | Руна пыла | Fervor Rune | TEMPO | оставить |  |  |  |
| RUNE_LEECH | Руна вампиризма | Leech Rune | SURVIVAL | оставить |  |  |  |
| RUNE_AEGIS | Руна эгиды | Aegis Rune | SURVIVAL | оставить |  |  |  |
| RUNE_CHARGE | Руна заряда | Charge Rune | SURVIVAL | оставить |  |  |  |
| RUNE_DOMINION | Руна власти | Dominion Rune | SURVIVAL | оставить |  |  |  |
| RUNE_RALLY | Руна сплочения | Rally Rune | SURVIVAL | оставить |  |  |  |

## Сундуки (по семействам) (15)

Вердикты: оставить - 14, решение по стилю - 1

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| WOODEN (10 тиров) | Деревянный сундук · тир N | Wooden Chest · Tier N | оставить |  |  |  |
| ARTISAN (10 тиров) | Сундук ремесленника · тир N | Artisan's Chest · Tier N | оставить |  |  |  |
| TREASURY (10 тиров) | Казна · тир N | Treasury · Tier N | оставить |  |  |  |
| IRONBOUND (10 тиров) | Окованный сундук · тир N | Ironbound Chest · Tier N | оставить |  |  |  |
| ORBS (10 тиров) | Сундук сфер · тир N | Chest of Orbs · Tier N | оставить |  |  |  |
| ESSENCE (9 тиров) | Сундук эссенций · тир N | Chest of Essences · Tier N | оставить |  |  |  |
| VAAL (8 тиров) | Сундук Ваал · тир N | Vaal Chest · Tier N | решение по стилю | Сундук Изнанки · тир N | Underside Chest · Tier N | следует решению по Ваал (Q10) |
| ANCIENT (9 тиров) | Древний сундук · тир N | Ancient Chest · Tier N | оставить |  |  |  |
| RELIC (9 тиров) | Сундук реликвий · тир N | Relic Chest · Tier N | оставить |  |  |  |
| WARLORD (10 тиров) | Сундук полководца · тир N | Warlord's Chest · Tier N | оставить |  |  |  |
| TRIAL (10 тиров) | Сундук испытаний · тир N | Trial Chest · Tier N | оставить |  |  |  |
| CARTOGRAPHER (10 тиров) | Сундук картографа · тир N | Cartographer's Chest · Tier N | оставить |  |  |  |
| JEWELLER (9 тиров) | Сундук ювелира · тир N | Jeweller's Chest · Tier N | оставить |  |  |  |
| SEER (9 тиров) | Сундук провидца · тир N | Seer's Chest · Tier N | оставить |  |  |  |
| CLASS (9 тиров) | Сундук наследия · тир N | Legacy Chest · Tier N | оставить |  |  |  |

## Книги умений (104)

Вердикты: оставить - 104

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| BOOK_HEAVY_STRIKE | Книга: Тяжёлый удар | Book: Heavy Strike | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ENDURING_CRY | Книга: Клич стойкости | Book: Enduring Cry | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_GROUND_SLAM | Книга: Сотрясение земли | Book: Ground Slam | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_INFERNAL_BLOW | Книга: Адский удар | Book: Infernal Blow | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_VULNERABILITY | Книга: Уязвимость | Book: Vulnerability | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_BLOOD_RAGE | Книга: Кровавая ярость | Book: Blood Rage | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_DETERMINATION | Книга: Решимость | Book: Determination | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_IRON_SKIN | Книга: Железная кожа | Book: Iron Skin | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_BLOODLETTING | Книга: Кровопролитие | Book: Bloodletting | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ANGER | Книга: Гнев | Book: Anger | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_BERSERKER_FURY | Книга: Ярость берсерка | Book: Berserker Fury | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_UNBREAKABLE | Книга: Несокрушимость | Book: Unbreakable | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_THICK_SKULL | Книга: Крепкий лоб | Book: Thick Skull | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_RETALIATION | Книга: Отмщение | Book: Retaliation | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ENDURING_ROAR | Книга: Рёв стойкости | Book: Enduring Roar | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_SPLIT_ARROW | Книга: Расщеплённая стрела | Book: Split Arrow | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_SNIPE | Книга: Прицельный выстрел | Book: Snipe | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ICE_SHOT | Книга: Ледяной выстрел | Book: Ice Shot | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_RAIN_OF_ARROWS | Книга: Дождь стрел | Book: Rain of Arrows | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_SNIPERS_MARK | Книга: Метка снайпера | Book: Sniper’s Mark | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_WIND_VEIL | Книга: Покров ветра | Book: Wind Veil | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_GRACE | Книга: Грация | Book: Grace | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_EAGLE_EYE | Книга: Зоркий глаз | Book: Eagle Eye | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_HUNTERS_THRILL | Книга: Охотничий азарт | Book: Hunter’s Thrill | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_HASTE | Книга: Спешка | Book: Haste | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_LIGHT_FOOT | Книга: Лёгкая стопа | Book: Light Foot | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_GUST | Книга: Порыв ветра | Book: Gust | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_POISONED_TIPS | Книга: Ядовитые наконечники | Book: Poisoned Tips | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_FROST_BURST | Книга: Ледяная вспышка | Book: Frost Burst | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_FRENZY_STRIKE | Книга: Яростный выстрел | Book: Frenzy Strike | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_FIREBALL | Книга: Огненный шар | Book: Fireball | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_FREEZING_PULSE | Книга: Ледяной импульс | Book: Freezing Pulse | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_SPARK | Книга: Искра | Book: Spark | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ELEMENTAL_WEAKNESS | Книга: Стихийная слабость | Book: Elemental Weakness | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ESSENCE_DRAIN | Книга: Иссушение | Book: Essence Drain | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ARCANE_WARD | Книга: Магический барьер | Book: Arcane Ward | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_DISCIPLINE | Книга: Дисциплина | Book: Discipline | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ELEMENTAL_MASTERY | Книга: Мастер стихий | Book: Elemental Mastery | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_MANA_HARVEST | Книга: Жатва маны | Book: Mana Harvest | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_CLARITY | Книга: Ясность | Book: Clarity | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_DEEP_RESERVE | Книга: Глубокий резерв | Book: Deep Reserve | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ELEMENTAL_SURGE | Книга: Раскол стихий | Book: Elemental Surge | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_SWIFT_CASTING | Книга: Беглые чары | Book: Swift Casting | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_LAST_WARD | Книга: Последний оплот | Book: Last Ward | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_POWER_SIPHON | Книга: Вытягивание силы | Book: Power Siphon | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_DOUBLE_STRIKE | Книга: Двойной удар | Book: Double Strike | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_LUNGE | Книга: Молниеносный выпад | Book: Lunge | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_STEEL_WHIRL | Книга: Вихрь стали | Book: Steel Whirl | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_CHALLENGE | Книга: Вызов | Book: Challenge | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ENFEEBLE | Книга: Бессилие | Book: Enfeeble | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_RIPOSTE | Книга: Рипост | Book: Riposte | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_PRECISION | Книга: Точность | Book: Precision | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_BLADE_MASTER | Книга: Мастер клинка | Book: Blade Master | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ONSLAUGHT | Книга: Натиск | Book: Onslaught | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_BLOODLUST | Книга: Жажда крови | Book: Bloodlust | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_SWORDSMAN | Книга: Фехтовальщик | Book: Swordsman | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_SECOND_WIND | Книга: Второе дыхание | Book: Second Wind | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_RUTHLESS | Книга: Беспощадность | Book: Ruthless | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_COUP_DE_GRACE | Книга: Смертельный удар | Book: Coup de Grace | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_CHARGED_DASH | Книга: Заряженный рывок | Book: Charged Dash | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_SMITE | Книга: Кара | Book: Smite | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_PURIFYING_FLAME | Книга: Очищающее пламя | Book: Purifying Flame | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_STORM_CALL | Книга: Грозовой зов | Book: Storm Call | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_PRAYER | Книга: Молитва | Book: Prayer | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_CONDUCTIVITY | Книга: Проводимость | Book: Conductivity | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_DIVINE_SHIELD | Книга: Божественный щит | Book: Divine Shield | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_PURITY_OF_ELEMENTS | Книга: Чистота стихий | Book: Purity of Elements | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_FAITH | Книга: Вера | Book: Faith | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_BLESSING | Книга: Благословение | Book: Blessing | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_WRATH | Книга: Гнев небес | Book: Wrath | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_HOLY_ARMS | Книга: Святое оружие | Book: Holy Arms | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_RIGHTEOUSNESS | Книга: Праведность | Book: Righteousness | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_FORTITUDE | Книга: Стойкость | Book: Fortitude | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_MARTYR | Книга: Мученик | Book: Martyr | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_EARTHEN_RELEASE | Книга: Земной выброс | Book: Earthen Release | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_VIPER_STRIKE | Книга: Змеиный укус | Book: Viper Strike | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_BLADE_FLURRY | Книга: Шквал клинков | Book: Blade Flurry | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_SHADOW_CLOAK | Книга: Покров теней | Book: Shadow Cloak | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_TOXIC_CLOUD | Книга: Ядовитое облако | Book: Toxic Cloud | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_DESPAIR | Книга: Отчаяние | Book: Despair | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_BACKSTAB | Книга: Удар в спину | Book: Backstab | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_MALEVOLENCE | Книга: Злоба | Book: Malevolence | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ASSASSIN | Книга: Убийца | Book: Assassin | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_VANISH | Книга: Исчезновение | Book: Vanish | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_SWIFTNESS | Книга: Проворство | Book: Swiftness | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_TOXICOLOGIST | Книга: Ядовед | Book: Toxicologist | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_LETHAL_VENOM | Книга: Смертельный яд | Book: Lethal Venom | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_STEALTH | Книга: Незаметность | Book: Stealth | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_SIPHON | Книга: Кража сил | Book: Siphon | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_DISCHARGE | Книга: Разрядка | Book: Discharge | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ELEMENTAL_HIT | Книга: Стихийный удар | Book: Elemental Hit | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_SPECTRAL_THROW | Книга: Спектральный бросок | Book: Spectral Throw | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ARC | Книга: Дуга | Book: Arc | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_RALLYING_CRY | Книга: Клич единства | Book: Rallying Cry | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_TEMPORAL_CHAINS | Книга: Путы времени | Book: Temporal Chains | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_RENEWAL | Книга: Возрождение | Book: Renewal | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_HARMONY | Книга: Гармония | Book: Harmony | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_LEGACY | Книга: Наследие | Book: Legacy | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ECHO | Книга: Отзвук | Book: Echo | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_RESONANCE | Книга: Резонанс | Book: Resonance | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_ADEPT | Книга: Универсал | Book: Adept | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_FLASK_ECHO | Книга: Эхо фляг | Book: Flask Echo | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_HUNGER_FOR_POWER | Книга: Жажда силы | Book: Hunger for Power | оставить |  |  | имя = «Книга: <умение>», следует за умением |
| BOOK_WILL_TO_LIVE | Книга: Воля к жизни | Book: Will to Live | оставить |  |  | имя = «Книга: <умение>», следует за умением |

## Клиентские копии имён (ui_*.json) (35)

Вердикты: копия - 35

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| BLESSED_ORB | Благодатная сфера | Blessed Orb | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| CHAOS_ORB | Сфера хаоса | Chaos Orb | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| DIVINE_ORB | Божественная сфера | Divine Orb | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| ENGRAVERS_ORB | Сфера гравёра | Engraver's Orb | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| ELDER_ORB | Сфера Древнего | Elder Orb | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| EXALTED_ORB | Сфера возвышения | Exalted Orb | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| FRACTURING_ORB | Сфера раскола | Fracturing Orb | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| MIRROR_OF_KALANDRA | Зеркало Каландры | Mirror of Kalandra | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| ORB_OF_ALCHEMY | Сфера алхимии | Orb of Alchemy | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| ORB_OF_ALTERATION | Сфера перемен | Orb of Alteration | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| ORB_OF_ANNULMENT | Сфера отмены | Orb of Annulment | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| ORB_OF_AUGMENTATION | Сфера усовершенствования | Orb of Augmentation | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| ORB_OF_CHANCE | Сфера удачи | Orb of Chance | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| ORB_OF_REGRET | Сфера сожаления | Orb of Regret | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| ORB_OF_SCOURING | Сфера очищения | Orb of Scouring | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| ORB_OF_TRANSMUTATION | Сфера превращения | Orb of Transmutation | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| REGAL_ORB | Сфера царей | Regal Orb | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| SHAPERS_ORB | Сфера Создателя | Shaper's Orb | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| VAAL_ORB | Сфера Ваал | Vaal Orb | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| ABYSS_ORB | Сфера Бездны | Abyss Orb | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| SEAL_OF_ALCHEMY | Печать алхимии | Seal of Alchemy | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| SEAL_OF_THE_SHAPER | Печать Создателя | Seal of the Shaper | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| SEAL_OF_THE_ELDER | Печать Древнего | Seal of the Elder | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| SEAL_OF_THE_ABYSS | Печать Бездны | Seal of the Abyss | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| UNVEILING_ORB | Сфера раскрытия | Unveiling Orb | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| QUALITY_ORB | Сфера качества | Orb of Quality | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| INSIGHT_ORB | Сфера прозрения | Orb of Insight | копия |  |  | клиентская копия имени сферы - менять вместе с сервером |
| LIFE | Живительный | Vivid | копия |  |  | клиентская копия имени катализатора |
| DEFENCE | Защитный | Fertile | копия |  |  | клиентская копия имени катализатора |
| ELEMENTAL | Призматический | Prismatic | копия |  |  | клиентская копия имени катализатора |
| PHYSICAL | Абразивный | Abrasive | копия |  |  | клиентская копия имени катализатора |
| CHAOS | Ядовитый | Noxious | копия |  |  | клиентская копия имени катализатора |
| SPEED | Бурный | Turbulent | копия |  |  | клиентская копия имени катализатора |
| ATTRIBUTE | Врождённый | Intrinsic | копия |  |  | клиентская копия имени катализатора |
| CASTER | Насыщенный | Imbued | копия |  |  | клиентская копия имени катализатора |
