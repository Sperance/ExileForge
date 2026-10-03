# Семь профессий и все работы

Профессия использует подходящий инструмент и доступную работу. Работа задаёт минимальный уровень, базовую длительность цикла, результат, опыт и дополнительную добычу; изготовление также требует входных материалов. Итог изменяется свойствами героя, инструмента и помощника.

Максимальный уровень профессии — **50**. Офлайн-учёт ограничен **12 часами**, минимальное время режима отсутствия — **5 минут**. Бонусы скорости и находок от уровня, удача и опыт профессии задаются отдельно. Потолок удачи — **90** в правилах этой системы.

## Инструменты и изготовление

Качество инструмента усиливает его строки труда. Инструмент другого вида не заменяет требуемый инструмент только из-за большей редкости. Перед долгой работой проверяйте остаток расходных материалов и выбранный рецепт.

Виды работ включают добычу, переработку и изготовление. Переработка может конвертировать ресурсы по указанному отношению, а не просто давать бесплатный следующий тир. Дополнительные результаты имеют собственные шансы; базовое время не равно гарантированной длительности с учётом всех бонусов.

Ниже перечислены все работы из контента, включая затраты, уровни, время, опыт и особые параметры. Коды сохранены для однозначного поиска. Общие правила и специальные настройки изготовления — в [полном справочнике профессий](Parameters-professions.md).

## MINING

### Горное дело


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_MINING` |


## COPPER_VEIN

### Медная жила


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 71 |
| nothing | 0 |
| Результат · `output` | [Copper Ore](Items-MATERIAL.md#copper_ore) |
| Опыт · `experience` | 4 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `ore` |


## MINING_REFINE

### Переплавка


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 5 |
| Длительность, с · `seconds` | 240 |
| nothing | 0 |
| Результат · `output` | `` |
| Опыт · `experience` | 5 |
| Вид · `kind` | `REFINE` |
| ratio | 5 |


## IRON_VEIN

### Железная жила


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 147 |
| nothing | 0 |
| Результат · `output` | [Iron Ore](Items-MATERIAL.md#iron_ore) |
| Опыт · `experience` | 9 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `ore` |


## TOPAZ_DEPOSIT

### Залежь топаза


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 15 |
| Длительность, с · `seconds` | 667 |
| nothing | 0 |
| Результат · `output` | [Topaz](Items-MATERIAL.md#topaz) |
| Опыт · `experience` | 18 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `gem` |


## SILVER_VEIN

### Серебряная жила


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 277 |
| nothing | 0 |
| Результат · `output` | [Silver Ore](Items-MATERIAL.md#silver_ore) |
| Опыт · `experience` | 16 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `ore` |


## SAPPHIRE_DEPOSIT

### Залежь сапфира


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 25 |
| Длительность, с · `seconds` | 1125 |
| nothing | 0 |
| Результат · `output` | [Sapphire](Items-MATERIAL.md#sapphire) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `gem` |


## MITHRIL_VEIN

### Мифриловая жила


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 452 |
| nothing | 0 |
| Результат · `output` | [Mithril Ore](Items-MATERIAL.md#mithril_ore) |
| Опыт · `experience` | 26 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `ore` |


## RUBY_DEPOSIT

### Залежь рубина


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 35 |
| Длительность, с · `seconds` | 1714 |
| nothing | 0 |
| Результат · `output` | [Ruby](Items-MATERIAL.md#ruby) |
| Опыт · `experience` | 45 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `gem` |


## ADAMANT_VEIN

### Адамантовая жила


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 667 |
| nothing | 0 |
| Результат · `output` | [Adamant Ore](Items-MATERIAL.md#adamant_ore) |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `ore` |


## STARMETAL_VEIN

### Звёздная жила


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 900 |
| nothing | 0 |
| Результат · `output` | [Starmetal Ore](Items-MATERIAL.md#starmetal_ore) |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `ore` |


## ONYX_DEPOSIT

### Залежь оникса


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 2200 |
| nothing | 0 |
| Результат · `output` | [Onyx](Items-MATERIAL.md#onyx) |
| Опыт · `experience` | 60 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `gem` |


## HERBALISM

### Травничество


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_HERBALISM` |


## WORMWOOD_PATCH

### Заросли полыни


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 71 |
| nothing | 0 |
| Результат · `output` | [Wormwood](Items-MATERIAL.md#wormwood) |
| Опыт · `experience` | 4 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Greed](Items-ESSENCE.md#essence_greed_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## CAVE_MUSHROOMS

### Пещерные грибы


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 5 |
| Длительность, с · `seconds` | 118 |
| nothing | 0 |
| Результат · `output` | [Cave Mushroom](Items-MATERIAL.md#cave_mushroom) |
| Опыт · `experience` | 6 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Contempt](Items-ESSENCE.md#essence_contempt_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## HERBALISM_REFINE

### Перегонка трав


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 5 |
| Длительность, с · `seconds` | 240 |
| nothing | 0 |
| Результат · `output` | `` |
| Опыт · `experience` | 5 |
| Вид · `kind` | `REFINE` |
| ratio | 5 |


## BLOODWORT_PATCH

### Заросли кровавника


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 149 |
| nothing | 0 |
| Результат · `output` | [Bloodwort](Items-MATERIAL.md#bloodwort) |
| Опыт · `experience` | 9 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Hatred](Items-ESSENCE.md#essence_hatred_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## AMBER_TAPPING

### Подсочка смолы


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 12 |
| Длительность, с · `seconds` | 250 |
| nothing | 0 |
| Результат · `output` | [Amber Resin](Items-MATERIAL.md#amber_resin) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Woe](Items-ESSENCE.md#essence_woe_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## SHADEBLOOM_PATCH

### Поляна теневцвета


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 277 |
| nothing | 0 |
| Результат · `output` | [Shadebloom](Items-MATERIAL.md#shadebloom) |
| Опыт · `experience` | 16 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Fear](Items-ESSENCE.md#essence_fear_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## GLOWCAP_GROTTO

### Грот светляков


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 28 |
| Длительность, с · `seconds` | 436 |
| nothing | 0 |
| Результат · `output` | [Glowcap](Items-MATERIAL.md#glowcap) |
| Опыт · `experience` | 22 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Anger](Items-ESSENCE.md#essence_anger_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## MOON_LOTUS_POND

### Пруд лунного лотоса


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 452 |
| nothing | 0 |
| Результат · `output` | [Moon Lotus](Items-MATERIAL.md#moon_lotus) |
| Опыт · `experience` | 26 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Torment](Items-ESSENCE.md#essence_torment_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## VOID_ROOT_BED

### Ложе корня пустоты


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 667 |
| nothing | 0 |
| Результат · `output` | [Void Root](Items-MATERIAL.md#void_root) |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Sorrow](Items-ESSENCE.md#essence_sorrow_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## STARBLOOM_PATCH

### Поляна звёздных цветов


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 900 |
| nothing | 0 |
| Результат · `output` | [Starbloom](Items-MATERIAL.md#starbloom) |
| Опыт · `experience` | 40 |
| chain | `herb` |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Rage](Items-ESSENCE.md#essence_rage_1); Шанс (единица по правилам подсистемы): 5 |


## WORLDROOT_GROVE

### Лощина корня мира


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 1300 |
| nothing | 0 |
| Результат · `output` | [Worldroot](Items-MATERIAL.md#worldroot) |
| Опыт · `experience` | 55 |
| chain | `herb` |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Suffering](Items-ESSENCE.md#essence_suffering_1); Шанс (единица по правилам подсистемы): 5 |


## WOODCUTTING

### Лесоруб


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_WOODCUTTING` |


## BIRCH_GROVE

### Березняк


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 71 |
| nothing | 0 |
| Результат · `output` | [Birch Log](Items-MATERIAL.md#birch_log) |
| Опыт · `experience` | 4 |
| Дополнительная добыча · `extra` | item: [Bark](Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Jungle Egg](Items-PET.md#pet_egg_jungle); Шанс (единица по правилам подсистемы): 2 |
| chain | `wood` |


## WOODCUTTING_REFINE

### Выдержка древесины


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 5 |
| Длительность, с · `seconds` | 240 |
| nothing | 0 |
| Результат · `output` | `` |
| Опыт · `experience` | 5 |
| Вид · `kind` | `REFINE` |
| ratio | 5 |


## OAK_GROVE

### Дубрава


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 147 |
| nothing | 0 |
| Результат · `output` | [Oak Log](Items-MATERIAL.md#oak_log) |
| Опыт · `experience` | 9 |
| Дополнительная добыча · `extra` | item: [Bark](Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Abyss Egg](Items-PET.md#pet_egg_abyss); Шанс (единица по правилам подсистемы): 2 |
| chain | `wood` |


## YEW_GROVE

### Тисовая роща


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 277 |
| nothing | 0 |
| Результат · `output` | [Yew Log](Items-MATERIAL.md#yew_log) |
| Опыт · `experience` | 16 |
| Дополнительная добыча · `extra` | item: [Bark](Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Crypt Egg](Items-PET.md#pet_egg_crypt); Шанс (единица по правилам подсистемы): 2 |
| chain | `wood` |


## IRONWOOD_GROVE

### Роща железного дерева


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 452 |
| nothing | 0 |
| Результат · `output` | [Ironwood Log](Items-MATERIAL.md#ironwood_log) |
| Опыт · `experience` | 26 |
| Дополнительная добыча · `extra` | item: [Bark](Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Ash Egg](Items-PET.md#pet_egg_ash); Шанс (единица по правилам подсистемы): 2 |
| chain | `wood` |


## GHOST_ASH_GROVE

### Призрачный ясеневник


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 667 |
| nothing | 0 |
| Результат · `output` | [Ghost Ash Log](Items-MATERIAL.md#ghost_ash_log) |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | item: [Bark](Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Temple Egg](Items-PET.md#pet_egg_temple); Шанс (единица по правилам подсистемы): 2 |
| chain | `wood` |


## ELDERHEART_GROVE

### Роща древнего сердца


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 900 |
| nothing | 0 |
| Результат · `output` | [Elderheart Log](Items-MATERIAL.md#elderheart_log) |
| Опыт · `experience` | 40 |
| chain | `wood` |
| Дополнительная добыча · `extra` | item: [Bark](Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Desert Egg](Items-PET.md#pet_egg_desert); Шанс (единица по правилам подсистемы): 2 |


## WORLDTREE_GROVE

### Роща мирового древа


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 1300 |
| nothing | 0 |
| Результат · `output` | [Worldtree Log](Items-MATERIAL.md#worldtree_log) |
| Опыт · `experience` | 55 |
| chain | `wood` |
| Дополнительная добыча · `extra` | item: [Bark](Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Volcano Egg](Items-PET.md#pet_egg_volcano); Шанс (единица по правилам подсистемы): 2 |


## SMITHING

### Кузнечное дело


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_SMITHING` |


## COPPER_FORGING

### Медная ковка


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 300 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Copper Ore](Items-MATERIAL.md#copper_ore); Количество: 5 |
| band | 1; 12 |
| additives | Да |


## IRON_FORGING

### Железная ковка


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 400 |
| nothing | 27 |
| Результат · `output` | `` |
| Опыт · `experience` | 24 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Iron Ore](Items-MATERIAL.md#iron_ore); Количество: 5; item: [Oak Log](Items-MATERIAL.md#oak_log); Количество: 1 |
| band | 10; 25 |
| additives | Да |


## SILVER_FORGING

### Серебряная ковка


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 500 |
| nothing | 30 |
| Результат · `output` | `` |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Silver Ore](Items-MATERIAL.md#silver_ore); Количество: 5; item: [Yew Log](Items-MATERIAL.md#yew_log); Количество: 1 |
| band | 20; 40 |
| additives | Да |


## MITHRIL_FORGING

### Мифриловая ковка


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 600 |
| nothing | 32 |
| Результат · `output` | `` |
| Опыт · `experience` | 60 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Mithril Ore](Items-MATERIAL.md#mithril_ore); Количество: 5; item: [Ironwood Log](Items-MATERIAL.md#ironwood_log); Количество: 1 |
| band | 35; 60 |
| additives | Да |


## ADAMANT_FORGING

### Адамантовая ковка


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 600 |
| nothing | 35 |
| Результат · `output` | `` |
| Опыт · `experience` | 85 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Adamant Ore](Items-MATERIAL.md#adamant_ore); Количество: 5; item: [Ghost Ash Log](Items-MATERIAL.md#ghost_ash_log); Количество: 1 |
| band | 55; 100 |
| additives | Да |


## STARMETAL_FORGING

### Ковка звёздного металла


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 700 |
| nothing | 35 |
| Результат · `output` | `` |
| Опыт · `experience` | 102 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Starmetal Ore](Items-MATERIAL.md#starmetal_ore); Количество: 5; item: [Elderheart Log](Items-MATERIAL.md#elderheart_log); Количество: 1 |
| band | 75; 90 |
| additives | Да |


## WORLD_FORGING

### Ковка мирового горна


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 800 |
| nothing | 35 |
| Результат · `output` | `` |
| Опыт · `experience` | 119 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Starmetal Ore](Items-MATERIAL.md#starmetal_ore); Количество: 5; item: [Worldtree Log](Items-MATERIAL.md#worldtree_log); Количество: 1; item: [Onyx](Items-MATERIAL.md#onyx); Количество: 1 |
| band | 85; 100 |
| additives | Да |


## ALCHEMY

### Алхимия


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_ALCHEMY` |


## BLOOD_FLUX_BREW

### Варка: примесь крови


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 200 |
| nothing | 25 |
| Результат · `output` | [Blood Flux](Items-MATERIAL.md#blood_flux) |
| Опыт · `experience` | 6 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Wormwood](Items-MATERIAL.md#wormwood); Количество: 3; item: [Cave Mushroom](Items-MATERIAL.md#cave_mushroom); Количество: 1 |


## TRANSMUTATION_BREW

### Варка: Orb of Transmutation


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 150 |
| nothing | 25 |
| Результат · `output` | [Orb of Transmutation](Items-CURRENCY.md#orb_of_transmutation) |
| Опыт · `experience` | 4 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Wormwood](Items-MATERIAL.md#wormwood); Количество: 2 |


## SMALL_LIFE_FLASK_BREW

### Сварить: малая фляга жизни


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 244 |
| nothing | 25 |
| Результат · `output` | [Small Life Flask](Equipment-FLASK.md#flask_small_life) |
| Опыт · `experience` | 8 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 1; item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 2 |


## SMALL_MANA_FLASK_BREW

### Сварить: малая фляга маны


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 244 |
| nothing | 25 |
| Результат · `output` | [Small Mana Flask](Equipment-FLASK.md#flask_small_mana) |
| Опыт · `experience` | 8 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 1; item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 2 |


## QUICKSILVER_FLASK_BREW

### Сварить: ртутная фляга


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 4 |
| Длительность, с · `seconds` | 256 |
| nothing | 25 |
| Результат · `output` | [Quicksilver Flask](Equipment-FLASK.md#flask_quicksilver) |
| Опыт · `experience` | 10 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Silver Ore](Items-MATERIAL.md#silver_ore); Количество: 2 |


## STONE_FLUX_BREW

### Варка: каменная примесь


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 5 |
| Длительность, с · `seconds` | 200 |
| nothing | 25 |
| Результат · `output` | [Stone Flux](Items-MATERIAL.md#stone_flux) |
| Опыт · `experience` | 8 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Cave Mushroom](Items-MATERIAL.md#cave_mushroom); Количество: 2; item: [Copper Ore](Items-MATERIAL.md#copper_ore); Количество: 2 |


## AUGMENTATION_BREW

### Варка: Orb of Augmentation


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 5 |
| Длительность, с · `seconds` | 150 |
| nothing | 25 |
| Результат · `output` | [Orb of Augmentation](Items-CURRENCY.md#orb_of_augmentation) |
| Опыт · `experience` | 5 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Wormwood](Items-MATERIAL.md#wormwood); Количество: 3 |


## MEDIUM_LIFE_FLASK_BREW

### Сварить: средняя фляга жизни


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 7 |
| Длительность, с · `seconds` | 272 |
| nothing | 26 |
| Результат · `output` | [Medium Life Flask](Equipment-FLASK.md#flask_medium_life) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 1; item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 3 |


## MEDIUM_MANA_FLASK_BREW

### Сварить: средняя фляга маны


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 7 |
| Длительность, с · `seconds` | 272 |
| nothing | 26 |
| Результат · `output` | [Medium Mana Flask](Equipment-FLASK.md#flask_medium_mana) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 1; item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 3 |


## RUBY_FLASK_BREW

### Сварить: рубиновая фляга


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 7 |
| Длительность, с · `seconds` | 272 |
| nothing | 26 |
| Результат · `output` | [Ruby Flask](Equipment-FLASK.md#flask_ruby) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Ruby](Items-MATERIAL.md#ruby); Количество: 2 |


## SAPPHIRE_FLASK_BREW

### Сварить: сапфировая фляга


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 7 |
| Длительность, с · `seconds` | 272 |
| nothing | 26 |
| Результат · `output` | [Sapphire Flask](Equipment-FLASK.md#flask_sapphire) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Sapphire](Items-MATERIAL.md#sapphire); Количество: 2 |


## TOPAZ_FLASK_BREW

### Сварить: топазовая фляга


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 7 |
| Длительность, с · `seconds` | 272 |
| nothing | 26 |
| Результат · `output` | [Topaz Flask](Equipment-FLASK.md#flask_topaz) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Topaz](Items-MATERIAL.md#topaz); Количество: 2 |


## ALTERATION_BREW

### Варка: Orb of Alteration


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 200 |
| nothing | 28 |
| Результат · `output` | [Orb of Alteration](Items-CURRENCY.md#orb_of_alteration) |
| Опыт · `experience` | 7 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 2; item: [Copper Ore](Items-MATERIAL.md#copper_ore); Количество: 1 |


## CONDENSE_ESSENCE

### Сгустить эссенцию


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 300 |
| nothing | 10 |
| Результат · `output` | `` |
| Опыт · `experience` | 18 |
| Вид · `kind` | `CONDENSE` |
| step | 4 |


## FIRE_FLUX_BREW

### Варка: огненная примесь


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 250 |
| nothing | 27 |
| Результат · `output` | [Fire Flux](Items-MATERIAL.md#fire_flux) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 3; item: [Topaz](Items-MATERIAL.md#topaz); Количество: 1 |


## HORDE_BREW

### Варка: Сфера удачи


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 300 |
| nothing | 30 |
| Результат · `output` | [Orb of Chance](Items-CURRENCY.md#orb_of_chance) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 2; item: [Bark](Items-MATERIAL.md#bark); Количество: 1 |


## GRANITE_FLASK_BREW

### Сварить: гранитная фляга


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 288 |
| nothing | 27 |
| Результат · `output` | [Granite Flask](Equipment-FLASK.md#flask_granite) |
| Опыт · `experience` | 14 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Rough Stone](Items-STONE_STOCK.md#stone_rough); Количество: 2 |


## JADE_FLASK_BREW

### Сварить: нефритовая фляга


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 288 |
| nothing | 27 |
| Результат · `output` | [Jade Flask](Equipment-FLASK.md#flask_jade) |
| Опыт · `experience` | 14 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Wormwood](Items-MATERIAL.md#wormwood); Количество: 2 |


## SCOURING_BREW

### Варка: Orb of Scouring


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 12 |
| Длительность, с · `seconds` | 200 |
| nothing | 30 |
| Результат · `output` | [Orb of Scouring](Items-CURRENCY.md#orb_of_scouring) |
| Опыт · `experience` | 9 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 2 |


## BAUBLE_BREW

### Варка: Сфера качества


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 12 |
| Длительность, с · `seconds` | 300 |
| nothing | 30 |
| Результат · `output` | [Orb of Quality](Items-CURRENCY.md#quality_orb) |
| Опыт · `experience` | 14 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 1 |


## LARGE_LIFE_FLASK_BREW

### Сварить: большая фляга жизни


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 14 |
| Длительность, с · `seconds` | 304 |
| nothing | 28 |
| Результат · `output` | [Large Life Flask](Equipment-FLASK.md#flask_large_life) |
| Опыт · `experience` | 16 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 1; item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 4 |


## LARGE_MANA_FLASK_BREW

### Сварить: большая фляга маны


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 14 |
| Длительность, с · `seconds` | 304 |
| nothing | 28 |
| Результат · `output` | [Large Mana Flask](Equipment-FLASK.md#flask_large_mana) |
| Опыт · `experience` | 16 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 1; item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 4 |


## FROST_FLUX_BREW

### Варка: ледяная примесь


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 15 |
| Длительность, с · `seconds` | 250 |
| nothing | 28 |
| Результат · `output` | [Frost Flux](Items-MATERIAL.md#frost_flux) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Shadebloom](Items-MATERIAL.md#shadebloom); Количество: 3; item: [Sapphire](Items-MATERIAL.md#sapphire); Количество: 1 |


## ALCHEMY_BREW

### Варка: Orb of Alchemy


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 15 |
| Длительность, с · `seconds` | 300 |
| nothing | 32 |
| Результат · `output` | [Orb of Alchemy](Items-CURRENCY.md#orb_of_alchemy) |
| Опыт · `experience` | 14 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Shadebloom](Items-MATERIAL.md#shadebloom); Количество: 2; item: [Topaz](Items-MATERIAL.md#topaz); Количество: 1 |


## MAGUS_BREW

### Варка: Сфера сожаления


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 15 |
| Длительность, с · `seconds` | 360 |
| nothing | 30 |
| Результат · `output` | [Orb of Regret](Items-CURRENCY.md#orb_of_regret) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 1; item: [Bark](Items-MATERIAL.md#bark); Количество: 2; item: [Orb of Transmutation](Items-CURRENCY.md#orb_of_transmutation); Количество: 4 |


## SILVER_FLASK_BREW

### Сварить: серебряная фляга


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 17 |
| Длительность, с · `seconds` | 320 |
| nothing | 29 |
| Результат · `output` | [Silver Flask](Equipment-FLASK.md#flask_silver) |
| Опыт · `experience` | 18 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Silver Ore](Items-MATERIAL.md#silver_ore); Количество: 2 |


## PERIL_BREW

### Варка: Благодатная сфера


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 450 |
| nothing | 32 |
| Результат · `output` | [Blessed Orb](Items-CURRENCY.md#blessed_orb) |
| Опыт · `experience` | 17 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 3; item: [Bark](Items-MATERIAL.md#bark); Количество: 2; item: [Orb of Alteration](Items-CURRENCY.md#orb_of_alteration); Количество: 6 |


## STORM_FLUX_BREW

### Варка: грозовая примесь


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 300 |
| nothing | 30 |
| Результат · `output` | [Storm Flux](Items-MATERIAL.md#storm_flux) |
| Опыт · `experience` | 18 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 3; item: [Topaz](Items-MATERIAL.md#topaz); Количество: 1 |


## MERCY_BREW

### Варка: Сфера ваал


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 600 |
| nothing | 35 |
| Результат · `output` | [Vaal Orb](Items-CURRENCY.md#vaal_orb) |
| Опыт · `experience` | 20 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Shadebloom](Items-MATERIAL.md#shadebloom); Количество: 2; item: [Bark](Items-MATERIAL.md#bark); Количество: 2; item: [Orb of Alchemy](Items-CURRENCY.md#orb_of_alchemy); Количество: 3 |


## BASALT_FLASK_BREW

### Сварить: базальтовая фляга


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 336 |
| nothing | 29 |
| Результат · `output` | [Basalt Flask](Equipment-FLASK.md#flask_basalt) |
| Опыт · `experience` | 20 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Iron Ore](Items-MATERIAL.md#iron_ore); Количество: 2 |


## GREATER_LIFE_FLASK_BREW

### Сварить: великая фляга жизни


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 22 |
| Длительность, с · `seconds` | 344 |
| nothing | 30 |
| Результат · `output` | [Greater Life Flask](Equipment-FLASK.md#flask_greater_life) |
| Опыт · `experience` | 21 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 5 |


## GREATER_MANA_FLASK_BREW

### Сварить: великая фляга маны


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 22 |
| Длительность, с · `seconds` | 344 |
| nothing | 30 |
| Результат · `output` | [Greater Mana Flask](Equipment-FLASK.md#flask_greater_mana) |
| Опыт · `experience` | 21 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 5 |


## ESSENCE_BREW

### Варка: Сфера раскрытия


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 22 |
| Длительность, с · `seconds` | 480 |
| nothing | 36 |
| Результат · `output` | [Unveiling Orb](Items-CURRENCY.md#unveiling_orb) |
| Опыт · `experience` | 26 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Shadebloom](Items-MATERIAL.md#shadebloom); Количество: 2; item: [Sapphire](Items-MATERIAL.md#sapphire); Количество: 1; item: [Orb of Alteration](Items-CURRENCY.md#orb_of_alteration); Количество: 4 |


## DIAMOND_FLASK_BREW

### Сварить: алмазная фляга


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 352 |
| nothing | 30 |
| Результат · `output` | [Diamond Flask](Equipment-FLASK.md#flask_diamond) |
| Опыт · `experience` | 22 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Mithril Ore](Items-MATERIAL.md#mithril_ore); Количество: 2 |


## QUICK_FLUX_BREW

### Варка: ртутная примесь


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 25 |
| Длительность, с · `seconds` | 300 |
| nothing | 32 |
| Результат · `output` | [Quick Flux](Items-MATERIAL.md#quick_flux) |
| Опыт · `experience` | 22 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 3; item: [Ruby](Items-MATERIAL.md#ruby); Количество: 1 |


## CHAOS_BREW

### Варка: Chaos Orb


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 25 |
| Длительность, с · `seconds` | 450 |
| nothing | 38 |
| Результат · `output` | [Chaos Orb](Items-CURRENCY.md#chaos_orb) |
| Опыт · `experience` | 24 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 2; item: [Sapphire](Items-MATERIAL.md#sapphire); Количество: 1 |


## ELITE_BREW

### Варка: Знамение порчи


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 28 |
| Длительность, с · `seconds` | 600 |
| nothing | 35 |
| Результат · `output` | [Omen of Corruption](Items-OMEN.md#omen_corruption) |
| Опыт · `experience` | 24 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 2; item: [Sapphire](Items-MATERIAL.md#sapphire); Количество: 1; item: [Bark](Items-MATERIAL.md#bark); Количество: 1; item: [Orb of Alchemy](Items-CURRENCY.md#orb_of_alchemy); Количество: 3 |


## QUARTZ_FLASK_BREW

### Сварить: кварцевая фляга


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 25 |
| Длительность, с · `seconds` | 360 |
| nothing | 31 |
| Результат · `output` | [Quartz Flask](Equipment-FLASK.md#flask_quartz) |
| Опыт · `experience` | 23 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2 |


## AMETHYST_FLASK_BREW

### Сварить: аметистовая фляга


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 25 |
| Длительность, с · `seconds` | 360 |
| nothing | 31 |
| Результат · `output` | [Amethyst Flask](Equipment-FLASK.md#flask_amethyst) |
| Опыт · `experience` | 23 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Void Root](Items-MATERIAL.md#void_root); Количество: 2 |


## SCRIBE_BREW

### Варка: Знамение выбора


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 36 |
| Длительность, с · `seconds` | 900 |
| nothing | 36 |
| Результат · `output` | [Omen of Choice](Items-OMEN.md#omen_choice) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 3; item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 2; item: [Chaos Orb](Items-CURRENCY.md#chaos_orb); Количество: 3 |


## REGAL_BREW

### Варка: Regal Orb


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 450 |
| nothing | 40 |
| Результат · `output` | [Regal Orb](Items-CURRENCY.md#regal_orb) |
| Опыт · `experience` | 28 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Moon Lotus](Items-MATERIAL.md#moon_lotus); Количество: 2; item: [Silver Ore](Items-MATERIAL.md#silver_ore); Количество: 2 |


## BOUNTY_BREW

### Варка: Сфера отмены


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 38 |
| Длительность, с · `seconds` | 1200 |
| nothing | 38 |
| Результат · `output` | [Orb of Annulment](Items-CURRENCY.md#orb_of_annulment) |
| Опыт · `experience` | 28 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Moon Lotus](Items-MATERIAL.md#moon_lotus); Количество: 2; item: [Topaz](Items-MATERIAL.md#topaz); Количество: 1; item: [Bark](Items-MATERIAL.md#bark); Количество: 2; item: [Chaos Orb](Items-CURRENCY.md#chaos_orb); Количество: 7 |


## TREASURE_BREW

### Варка: Знамение света


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 32 |
| Длительность, с · `seconds` | 600 |
| nothing | 38 |
| Результат · `output` | [Omen of Light](Items-OMEN.md#omen_light) |
| Опыт · `experience` | 32 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Shadebloom](Items-MATERIAL.md#shadebloom); Количество: 3; item: [Silver Ore](Items-MATERIAL.md#silver_ore); Количество: 2; item: [Orb of Alchemy](Items-CURRENCY.md#orb_of_alchemy); Количество: 3 |


## GRAND_LIFE_FLASK_BREW

### Сварить: грандиозная фляга жизни


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 384 |
| nothing | 32 |
| Результат · `output` | [Grand Life Flask](Equipment-FLASK.md#flask_grand_life) |
| Опыт · `experience` | 26 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 6 |


## GRAND_MANA_FLASK_BREW

### Сварить: грандиозная фляга маны


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 384 |
| nothing | 32 |
| Результат · `output` | [Grand Mana Flask](Equipment-FLASK.md#flask_grand_mana) |
| Опыт · `experience` | 26 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Moon Lotus](Items-MATERIAL.md#moon_lotus); Количество: 6 |


## SULPHUR_FLASK_BREW

### Сварить: сернистая фляга


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 380 |
| nothing | 32 |
| Результат · `output` | [Sulphur Flask](Equipment-FLASK.md#flask_sulphur) |
| Опыт · `experience` | 25 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Fire Flux](Items-MATERIAL.md#fire_flux); Количество: 2 |


## GILDED_BREW

### Варка: Знамение великого возвышения


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 35 |
| Длительность, с · `seconds` | 600 |
| nothing | 38 |
| Результат · `output` | [Omen of Greater Exaltation](Items-OMEN.md#omen_greater_exaltation) |
| Опыт · `experience` | 32 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 3; item: [Topaz](Items-MATERIAL.md#topaz); Количество: 1; item: [Regal Orb](Items-CURRENCY.md#regal_orb); Количество: 1 |


## EMPOWERING_BREW

### Варка: Знамение тира


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1200 |
| nothing | 45 |
| Результат · `output` | [Omen of Tiers](Items-OMEN.md#omen_tier) |
| Опыт · `experience` | 35 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Moon Lotus](Items-MATERIAL.md#moon_lotus); Количество: 2; item: [Ruby](Items-MATERIAL.md#ruby); Количество: 1; item: [Bark](Items-MATERIAL.md#bark); Количество: 2; item: [Regal Orb](Items-CURRENCY.md#regal_orb); Количество: 3 |


## DIVINE_BREW

### Варка: Divine Orb


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1800 |
| nothing | 55 |
| Результат · `output` | [Divine Orb](Items-CURRENCY.md#divine_orb) |
| Опыт · `experience` | 45 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items-MATERIAL.md#void_root); Количество: 4; item: [Mithril Ore](Items-MATERIAL.md#mithril_ore); Количество: 4; item: [Chaos Orb](Items-CURRENCY.md#chaos_orb); Количество: 5 |


## GIANT_LIFE_FLASK_BREW

### Сварить: гигантская фляга жизни


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 432 |
| nothing | 34 |
| Результат · `output` | [Giant Life Flask](Equipment-FLASK.md#flask_giant_life) |
| Опыт · `experience` | 32 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 7 |


## GIANT_MANA_FLASK_BREW

### Сварить: гигантская фляга маны


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 432 |
| nothing | 34 |
| Результат · `output` | [Giant Mana Flask](Equipment-FLASK.md#flask_giant_mana) |
| Опыт · `experience` | 32 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Moon Lotus](Items-MATERIAL.md#moon_lotus); Количество: 7 |


## WARDEN_BREW

### Варка: Сфера раскола


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 42 |
| Длительность, с · `seconds` | 1800 |
| nothing | 38 |
| Результат · `output` | [Fracturing Orb](Items-CURRENCY.md#fracturing_orb) |
| Опыт · `experience` | 32 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items-MATERIAL.md#void_root); Количество: 2; item: [Ruby](Items-MATERIAL.md#ruby); Количество: 1; item: [Divine Orb](Items-CURRENCY.md#divine_orb); Количество: 1; item: [Chaos Orb](Items-CURRENCY.md#chaos_orb); Количество: 10 |


## EXALTED_BREW

### Варка: Exalted Orb


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 1800 |
| nothing | 60 |
| Результат · `output` | [Exalted Orb](Items-CURRENCY.md#exalted_orb) |
| Опыт · `experience` | 55 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items-MATERIAL.md#void_root); Количество: 6; item: [Ruby](Items-MATERIAL.md#ruby); Количество: 4; item: [Regal Orb](Items-CURRENCY.md#regal_orb); Количество: 5 |


## STAR_FLUX_BREW

### Сварить звёздный флюс


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 300 |
| nothing | 25 |
| Результат · `output` | [Star Flux](Items-MATERIAL.md#star_flux) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Starbloom](Items-MATERIAL.md#starbloom); Количество: 2; item: [Worldroot](Items-MATERIAL.md#worldroot); Количество: 1 |


## DIVINE_LIFE_FLASK_BREW

### Сварить: божественная фляга жизни


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 480 |
| nothing | 37 |
| Результат · `output` | [Divine Life Flask](Equipment-FLASK.md#flask_divine_life) |
| Опыт · `experience` | 38 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 3; item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 8 |


## DIVINE_MANA_FLASK_BREW

### Сварить: божественная фляга маны


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 480 |
| nothing | 37 |
| Результат · `output` | [Divine Mana Flask](Equipment-FLASK.md#flask_divine_mana) |
| Опыт · `experience` | 38 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items-STONE_STOCK.md#stone_polished); Количество: 3; item: [Moon Lotus](Items-MATERIAL.md#moon_lotus); Количество: 8 |


## POTION_EXPERIENCE_T1_BREW

### Варка: малое зелье опыта


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 400 |
| nothing | 25 |
| Результат · `output` | [Minor Experience Potion](Items-MATERIAL.md#potion_experience_t1) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 3; item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 2 |


## POTION_EXPERIENCE_T2_BREW

### Варка: зелье опыта


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 700 |
| nothing | 25 |
| Результат · `output` | [Experience Potion](Items-MATERIAL.md#potion_experience_t2) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 3; item: [Moon Lotus](Items-MATERIAL.md#moon_lotus); Количество: 2 |


## POTION_EXPERIENCE_T3_BREW

### Варка: великое зелье опыта


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1000 |
| nothing | 25 |
| Результат · `output` | [Greater Experience Potion](Items-MATERIAL.md#potion_experience_t3) |
| Опыт · `experience` | 50 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items-MATERIAL.md#void_root); Количество: 3; item: [Starbloom](Items-MATERIAL.md#starbloom); Количество: 2 |


## POTION_GOLD_T1_BREW

### Варка: малое зелье золота


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 400 |
| nothing | 25 |
| Результат · `output` | [Minor Gold Potion](Items-MATERIAL.md#potion_gold_t1) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 3; item: [Copper Ore](Items-MATERIAL.md#copper_ore); Количество: 3 |


## POTION_GOLD_T2_BREW

### Варка: зелье золота


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 700 |
| nothing | 25 |
| Результат · `output` | [Gold Potion](Items-MATERIAL.md#potion_gold_t2) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 3; item: [Silver Ore](Items-MATERIAL.md#silver_ore); Количество: 3 |


## POTION_GOLD_T3_BREW

### Варка: великое зелье золота


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1000 |
| nothing | 25 |
| Результат · `output` | [Greater Gold Potion](Items-MATERIAL.md#potion_gold_t3) |
| Опыт · `experience` | 50 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items-MATERIAL.md#void_root); Количество: 3; item: [Adamant Ore](Items-MATERIAL.md#adamant_ore); Количество: 3 |


## POTION_RARITY_T1_BREW

### Варка: малое зелье удачи


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 400 |
| nothing | 25 |
| Результат · `output` | [Minor Fortune Potion](Items-MATERIAL.md#potion_rarity_t1) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 3; item: [Topaz](Items-MATERIAL.md#topaz); Количество: 1 |


## POTION_RARITY_T2_BREW

### Варка: зелье удачи


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 700 |
| nothing | 25 |
| Результат · `output` | [Fortune Potion](Items-MATERIAL.md#potion_rarity_t2) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 3; item: [Sapphire](Items-MATERIAL.md#sapphire); Количество: 1 |


## POTION_RARITY_T3_BREW

### Варка: великое зелье удачи


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1000 |
| nothing | 25 |
| Результат · `output` | [Greater Fortune Potion](Items-MATERIAL.md#potion_rarity_t3) |
| Опыт · `experience` | 50 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items-MATERIAL.md#void_root); Количество: 3; item: [Ruby](Items-MATERIAL.md#ruby); Количество: 1 |


## POTION_RESIST_T1_BREW

### Варка: малое зелье стойкости


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 400 |
| nothing | 25 |
| Результат · `output` | [Minor Warding Potion](Items-MATERIAL.md#potion_resist_t1) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 3; item: [Stone Flux](Items-MATERIAL.md#stone_flux); Количество: 1 |


## POTION_RESIST_T2_BREW

### Варка: зелье стойкости


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 700 |
| nothing | 25 |
| Результат · `output` | [Warding Potion](Items-MATERIAL.md#potion_resist_t2) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 3; item: [Stone Flux](Items-MATERIAL.md#stone_flux); Количество: 2 |


## POTION_RESIST_T3_BREW

### Варка: великое зелье стойкости


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1000 |
| nothing | 25 |
| Результат · `output` | [Greater Warding Potion](Items-MATERIAL.md#potion_resist_t3) |
| Опыт · `experience` | 50 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items-MATERIAL.md#void_root); Количество: 3; item: [Stone Flux](Items-MATERIAL.md#stone_flux); Количество: 3 |


## POTION_DAMAGE_T1_BREW

### Варка: малое зелье ярости


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 400 |
| nothing | 25 |
| Результат · `output` | [Minor Fury Potion](Items-MATERIAL.md#potion_damage_t1) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 3; item: [Fire Flux](Items-MATERIAL.md#fire_flux); Количество: 1 |


## POTION_DAMAGE_T2_BREW

### Варка: зелье ярости


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 700 |
| nothing | 25 |
| Результат · `output` | [Fury Potion](Items-MATERIAL.md#potion_damage_t2) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 3; item: [Fire Flux](Items-MATERIAL.md#fire_flux); Количество: 2 |


## POTION_DAMAGE_T3_BREW

### Варка: великое зелье ярости


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1000 |
| nothing | 25 |
| Результат · `output` | [Greater Fury Potion](Items-MATERIAL.md#potion_damage_t3) |
| Опыт · `experience` | 50 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items-MATERIAL.md#void_root); Количество: 3; item: [Fire Flux](Items-MATERIAL.md#fire_flux); Количество: 3 |


## POTION_LIFE_T1_BREW

### Варка: малое зелье жизни


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 400 |
| nothing | 25 |
| Результат · `output` | [Minor Vitality Potion](Items-MATERIAL.md#potion_life_t1) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 3; item: [Blood Flux](Items-MATERIAL.md#blood_flux); Количество: 1 |


## POTION_LIFE_T2_BREW

### Варка: зелье жизни


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 700 |
| nothing | 25 |
| Результат · `output` | [Vitality Potion](Items-MATERIAL.md#potion_life_t2) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 3; item: [Blood Flux](Items-MATERIAL.md#blood_flux); Количество: 2 |


## POTION_LIFE_T3_BREW

### Варка: великое зелье жизни


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1000 |
| nothing | 25 |
| Результат · `output` | [Greater Vitality Potion](Items-MATERIAL.md#potion_life_t3) |
| Опыт · `experience` | 50 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items-MATERIAL.md#void_root); Количество: 3; item: [Blood Flux](Items-MATERIAL.md#blood_flux); Количество: 3 |


## PET_ORB_BREEDING_BREW

### Варка: сфера скрещивания


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 25 |
| Длительность, с · `seconds` | 900 |
| nothing | 25 |
| Результат · `output` | [Orb of Breeding](Items-PET.md#pet_orb_breeding) |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 4; item: [Ruby](Items-MATERIAL.md#ruby); Количество: 1; item: [Blood Flux](Items-MATERIAL.md#blood_flux); Количество: 2 |


## CARTOGRAPHY

### Картография


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_CARTOGRAPHY` |


## CHART_REGION_1

### Карта: Берег изгнанников


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 324 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 14 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 1; item: [Birch Log](Items-MATERIAL.md#birch_log); Количество: 2 |
| Регион · `region` | `REGION_1` |


## CHART_REGION_2

### Карта: Кристальный хребет


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 382 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 24 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 1; item: [Oak Log](Items-MATERIAL.md#oak_log); Количество: 2 |
| Регион · `region` | `REGION_2` |


## CHART_REGION_3

### Карта: Выжженные пески


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 18 |
| Длительность, с · `seconds` | 440 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 35 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 2; item: [Yew Log](Items-MATERIAL.md#yew_log); Количество: 2; item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 1 |
| Регион · `region` | `REGION_3` |


## CHART_REGION_4

### Карта: Изумрудные дебри


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 27 |
| Длительность, с · `seconds` | 501 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 46 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 3; item: [Ironwood Log](Items-MATERIAL.md#ironwood_log); Количество: 2; item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 1 |
| Регион · `region` | `REGION_4` |


## CHART_REGION_5

### Карта: Огненный рубеж


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 35 |
| Длительность, с · `seconds` | 561 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 56 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 3; item: [Ghost Ash Log](Items-MATERIAL.md#ghost_ash_log); Количество: 2; item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 1 |
| Регион · `region` | `REGION_5` |


## CHART_REGION_6

### Карта: Бездна


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 44 |
| Длительность, с · `seconds` | 619 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 67 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 4; item: [Ghost Ash Log](Items-MATERIAL.md#ghost_ash_log); Количество: 2; item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 1 |
| Регион · `region` | `REGION_6` |


## CHART_REGION_7

### Карта: Расколотые небеса


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 678 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 73 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 5; item: [Ghost Ash Log](Items-MATERIAL.md#ghost_ash_log); Количество: 3; item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 2 |
| Регион · `region` | `REGION_7` |


## CHART_REGION_8

### Карта: Утонувшая империя


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 738 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 76 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 5; item: [Ghost Ash Log](Items-MATERIAL.md#ghost_ash_log); Количество: 3; item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 2 |
| Регион · `region` | `REGION_8` |


## CHART_REGION_9

### Карта: Чертоги богов


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 798 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 79 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 5; item: [Ghost Ash Log](Items-MATERIAL.md#ghost_ash_log); Количество: 3; item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 2 |
| Регион · `region` | `REGION_9` |


## CHART_TIERED

### Карта с тиром


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 900 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 90 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Worldtree Log](Items-MATERIAL.md#worldtree_log); Количество: 2; item: [Starbloom](Items-MATERIAL.md#starbloom); Количество: 2; item: [Onyx](Items-MATERIAL.md#onyx); Количество: 1 |
| Регион · `region` | `REGION_9` |
| tier | 3 |


## SCARAB_CHESTS_CARVE

### Резьба: скарабей тайников


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 600 |
| nothing | 25 |
| Результат · `output` | [Cache Scarab](Items-MATERIAL.md#scarab_chests) |
| Опыт · `experience` | 25 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 2; item: [Oak Log](Items-MATERIAL.md#oak_log); Количество: 3; item: [Amber Resin](Items-MATERIAL.md#amber_resin); Количество: 1 |


## SCARAB_PACK_CARVE

### Резьба: скарабей стаи


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 18 |
| Длительность, с · `seconds` | 600 |
| nothing | 25 |
| Результат · `output` | [Pack Scarab](Items-MATERIAL.md#scarab_pack) |
| Опыт · `experience` | 25 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Yew Log](Items-MATERIAL.md#yew_log); Количество: 3; item: [Bloodwort](Items-MATERIAL.md#bloodwort); Количество: 2; item: [Iron Ore](Items-MATERIAL.md#iron_ore); Количество: 2 |


## SCARAB_RARITY_CARVE

### Резьба: скарабей удачи


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 26 |
| Длительность, с · `seconds` | 600 |
| nothing | 25 |
| Результат · `output` | [Fortune Scarab](Items-MATERIAL.md#scarab_rarity) |
| Опыт · `experience` | 25 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Ironwood Log](Items-MATERIAL.md#ironwood_log); Количество: 2; item: [Topaz](Items-MATERIAL.md#topaz); Количество: 1; item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 2 |


## ENCHANTING

### Зачарование


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_ENCHANTING` |


## SCRIBE_BOOK_1

### Переписать книгу умений I


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 600 |
| nothing | 35 |
| Результат · `output` | `` |
| Опыт · `experience` | 20 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `BOOK` |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 3; item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 2 |
| band | 12 |


## CUT_JEWEL_1

### Огранить самоцвет I


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 15 |
| Длительность, с · `seconds` | 700 |
| nothing | 30 |
| Результат · `output` | `` |
| Опыт · `experience` | 25 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `JEWEL` |
| Затраты · `inputs` | item: [Sapphire](Items-MATERIAL.md#sapphire); Количество: 1; item: [Glowcap](Items-MATERIAL.md#glowcap); Количество: 2; item: [Bark](Items-MATERIAL.md#bark); Количество: 2 |
| band | 1; 20 |


## SCRIBE_BOOK_2

### Переписать книгу умений II


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 800 |
| nothing | 35 |
| Результат · `output` | `` |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `BOOK` |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 4; item: [Moon Lotus](Items-MATERIAL.md#moon_lotus); Количество: 2; item: [Sapphire](Items-MATERIAL.md#sapphire); Количество: 1 |
| band | 30 |


## CUT_JEWEL_2

### Огранить самоцвет II


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 1000 |
| nothing | 30 |
| Результат · `output` | `` |
| Опыт · `experience` | 55 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `JEWEL` |
| Затраты · `inputs` | item: [Onyx](Items-MATERIAL.md#onyx); Количество: 1; item: [Starbloom](Items-MATERIAL.md#starbloom); Количество: 2; item: [Elderheart Log](Items-MATERIAL.md#elderheart_log); Количество: 1 |
| band | 20; 45 |


## SCRIBE_BOOK_3

### Переписать книгу умений III


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 1000 |
| nothing | 35 |
| Результат · `output` | `` |
| Опыт · `experience` | 60 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `BOOK` |
| Затраты · `inputs` | item: [Bark](Items-MATERIAL.md#bark); Количество: 5; item: [Void Root](Items-MATERIAL.md#void_root); Количество: 2; item: [Ruby](Items-MATERIAL.md#ruby); Количество: 1 |
| band | 70 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/professions.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
