# Семь профессий и все работы

Профессия использует подходящий инструмент и доступную работу. Работа задаёт минимальный уровень, базовую длительность цикла, результат, опыт и дополнительную добычу; изготовление также требует входных материалов. Итог изменяется свойствами героя, инструмента и помощника.

Максимальный уровень профессии — **50**. Офлайн-учёт ограничен **12 часами**, минимальное время режима отсутствия — **5 минут**. Бонусы скорости и находок от уровня, удача и опыт профессии задаются отдельно. Потолок удачи — **90** в правилах этой системы.

## Инструменты и изготовление <a href="#инструменты-и-изготовление" id="инструменты-и-изготовление"></a>

Качество инструмента усиливает его строки труда. Инструмент другого вида не заменяет требуемый инструмент только из-за большей редкости. Перед долгой работой проверяйте остаток расходных материалов и выбранный рецепт.

Виды работ включают добычу, переработку и изготовление. Переработка может конвертировать ресурсы по указанному отношению, а не просто давать бесплатный следующий тир. Дополнительные результаты имеют собственные шансы; базовое время не равно гарантированной длительности с учётом всех бонусов.

Ниже перечислены все работы из контента, включая затраты, уровни, время, опыт и особые параметры. Коды сохранены для однозначного поиска. Общие правила и специальные настройки изготовления — в [полном справочнике профессий](reference/Parameters-professions.md).

## MINING <a href="#mining" id="mining"></a>

### Горное дело <a href="#горное-дело" id="горное-дело"></a>


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_MINING` |


## COPPER_VEIN <a href="#copper_vein" id="copper_vein"></a>

### Медная жила <a href="#медная-жила" id="медная-жила"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 71 |
| nothing | 0 |
| Результат · `output` | [Copper Ore](Items/Items-MATERIAL.md#copper_ore) |
| Опыт · `experience` | 4 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `ore` |


## MINING_REFINE <a href="#mining_refine" id="mining_refine"></a>

### Переплавка <a href="#переплавка" id="переплавка"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 5 |
| Длительность, с · `seconds` | 240 |
| nothing | 0 |
| Результат · `output` | `` |
| Опыт · `experience` | 5 |
| Вид · `kind` | `REFINE` |
| ratio | 5 |


## IRON_VEIN <a href="#iron_vein" id="iron_vein"></a>

### Железная жила <a href="#железная-жила" id="железная-жила"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 147 |
| nothing | 0 |
| Результат · `output` | [Iron Ore](Items/Items-MATERIAL.md#iron_ore) |
| Опыт · `experience` | 9 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `ore` |


## TOPAZ_DEPOSIT <a href="#topaz_deposit" id="topaz_deposit"></a>

### Залежь топаза <a href="#залежь-топаза" id="залежь-топаза"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 15 |
| Длительность, с · `seconds` | 667 |
| nothing | 0 |
| Результат · `output` | [Topaz](Items/Items-MATERIAL.md#topaz) |
| Опыт · `experience` | 18 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `gem` |


## SILVER_VEIN <a href="#silver_vein" id="silver_vein"></a>

### Серебряная жила <a href="#серебряная-жила" id="серебряная-жила"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 277 |
| nothing | 0 |
| Результат · `output` | [Silver Ore](Items/Items-MATERIAL.md#silver_ore) |
| Опыт · `experience` | 16 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `ore` |


## SAPPHIRE_DEPOSIT <a href="#sapphire_deposit" id="sapphire_deposit"></a>

### Залежь сапфира <a href="#залежь-сапфира" id="залежь-сапфира"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 25 |
| Длительность, с · `seconds` | 1125 |
| nothing | 0 |
| Результат · `output` | [Sapphire](Items/Items-MATERIAL.md#sapphire) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `gem` |


## MITHRIL_VEIN <a href="#mithril_vein" id="mithril_vein"></a>

### Мифриловая жила <a href="#мифриловая-жила" id="мифриловая-жила"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 452 |
| nothing | 0 |
| Результат · `output` | [Mithril Ore](Items/Items-MATERIAL.md#mithril_ore) |
| Опыт · `experience` | 26 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `ore` |


## RUBY_DEPOSIT <a href="#ruby_deposit" id="ruby_deposit"></a>

### Залежь рубина <a href="#залежь-рубина" id="залежь-рубина"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 35 |
| Длительность, с · `seconds` | 1714 |
| nothing | 0 |
| Результат · `output` | [Ruby](Items/Items-MATERIAL.md#ruby) |
| Опыт · `experience` | 45 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `gem` |


## ADAMANT_VEIN <a href="#adamant_vein" id="adamant_vein"></a>

### Адамантовая жила <a href="#адамантовая-жила" id="адамантовая-жила"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 667 |
| nothing | 0 |
| Результат · `output` | [Adamant Ore](Items/Items-MATERIAL.md#adamant_ore) |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `ore` |


## STARMETAL_VEIN <a href="#starmetal_vein" id="starmetal_vein"></a>

### Звёздная жила <a href="#звёздная-жила" id="звёздная-жила"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 900 |
| nothing | 0 |
| Результат · `output` | [Starmetal Ore](Items/Items-MATERIAL.md#starmetal_ore) |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `ore` |


## ONYX_DEPOSIT <a href="#onyx_deposit" id="onyx_deposit"></a>

### Залежь оникса <a href="#залежь-оникса" id="залежь-оникса"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 2200 |
| nothing | 0 |
| Результат · `output` | [Onyx](Items/Items-MATERIAL.md#onyx) |
| Опыт · `experience` | 60 |
| Дополнительная добыча · `extra` | item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 5 |
| chain | `gem` |


## HERBALISM <a href="#herbalism" id="herbalism"></a>

### Травничество <a href="#травничество" id="травничество"></a>


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_HERBALISM` |


## WORMWOOD_PATCH <a href="#wormwood_patch" id="wormwood_patch"></a>

### Заросли полыни <a href="#заросли-полыни" id="заросли-полыни"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 71 |
| nothing | 0 |
| Результат · `output` | [Wormwood](Items/Items-MATERIAL.md#wormwood) |
| Опыт · `experience` | 4 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Greed](Items/Items-ESSENCE.md#essence_greed_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## CAVE_MUSHROOMS <a href="#cave_mushrooms" id="cave_mushrooms"></a>

### Пещерные грибы <a href="#пещерные-грибы" id="пещерные-грибы"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 5 |
| Длительность, с · `seconds` | 118 |
| nothing | 0 |
| Результат · `output` | [Cave Mushroom](Items/Items-MATERIAL.md#cave_mushroom) |
| Опыт · `experience` | 6 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Contempt](Items/Items-ESSENCE.md#essence_contempt_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## HERBALISM_REFINE <a href="#herbalism_refine" id="herbalism_refine"></a>

### Перегонка трав <a href="#перегонка-трав" id="перегонка-трав"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 5 |
| Длительность, с · `seconds` | 240 |
| nothing | 0 |
| Результат · `output` | `` |
| Опыт · `experience` | 5 |
| Вид · `kind` | `REFINE` |
| ratio | 5 |


## BLOODWORT_PATCH <a href="#bloodwort_patch" id="bloodwort_patch"></a>

### Заросли кровавника <a href="#заросли-кровавника" id="заросли-кровавника"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 149 |
| nothing | 0 |
| Результат · `output` | [Bloodwort](Items/Items-MATERIAL.md#bloodwort) |
| Опыт · `experience` | 9 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Hatred](Items/Items-ESSENCE.md#essence_hatred_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## AMBER_TAPPING <a href="#amber_tapping" id="amber_tapping"></a>

### Подсочка смолы <a href="#подсочка-смолы" id="подсочка-смолы"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 12 |
| Длительность, с · `seconds` | 250 |
| nothing | 0 |
| Результат · `output` | [Amber Resin](Items/Items-MATERIAL.md#amber_resin) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Woe](Items/Items-ESSENCE.md#essence_woe_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## SHADEBLOOM_PATCH <a href="#shadebloom_patch" id="shadebloom_patch"></a>

### Поляна теневцвета <a href="#поляна-теневцвета" id="поляна-теневцвета"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 277 |
| nothing | 0 |
| Результат · `output` | [Shadebloom](Items/Items-MATERIAL.md#shadebloom) |
| Опыт · `experience` | 16 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Fear](Items/Items-ESSENCE.md#essence_fear_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## GLOWCAP_GROTTO <a href="#glowcap_grotto" id="glowcap_grotto"></a>

### Грот светляков <a href="#грот-светляков" id="грот-светляков"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 28 |
| Длительность, с · `seconds` | 436 |
| nothing | 0 |
| Результат · `output` | [Glowcap](Items/Items-MATERIAL.md#glowcap) |
| Опыт · `experience` | 22 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Anger](Items/Items-ESSENCE.md#essence_anger_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## MOON_LOTUS_POND <a href="#moon_lotus_pond" id="moon_lotus_pond"></a>

### Пруд лунного лотоса <a href="#пруд-лунного-лотоса" id="пруд-лунного-лотоса"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 452 |
| nothing | 0 |
| Результат · `output` | [Moon Lotus](Items/Items-MATERIAL.md#moon_lotus) |
| Опыт · `experience` | 26 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Torment](Items/Items-ESSENCE.md#essence_torment_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## VOID_ROOT_BED <a href="#void_root_bed" id="void_root_bed"></a>

### Ложе корня пустоты <a href="#ложе-корня-пустоты" id="ложе-корня-пустоты"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 667 |
| nothing | 0 |
| Результат · `output` | [Void Root](Items/Items-MATERIAL.md#void_root) |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Sorrow](Items/Items-ESSENCE.md#essence_sorrow_1); Шанс (единица по правилам подсистемы): 5 |
| chain | `herb` |


## STARBLOOM_PATCH <a href="#starbloom_patch" id="starbloom_patch"></a>

### Поляна звёздных цветов <a href="#поляна-звёздных-цветов" id="поляна-звёздных-цветов"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 900 |
| nothing | 0 |
| Результат · `output` | [Starbloom](Items/Items-MATERIAL.md#starbloom) |
| Опыт · `experience` | 40 |
| chain | `herb` |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Rage](Items/Items-ESSENCE.md#essence_rage_1); Шанс (единица по правилам подсистемы): 5 |


## WORLDROOT_GROVE <a href="#worldroot_grove" id="worldroot_grove"></a>

### Лощина корня мира <a href="#лощина-корня-мира" id="лощина-корня-мира"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 1300 |
| nothing | 0 |
| Результат · `output` | [Worldroot](Items/Items-MATERIAL.md#worldroot) |
| Опыт · `experience` | 55 |
| chain | `herb` |
| Дополнительная добыча · `extra` | item: [Whispering Essence of Suffering](Items/Items-ESSENCE.md#essence_suffering_1); Шанс (единица по правилам подсистемы): 5 |


## WOODCUTTING <a href="#woodcutting" id="woodcutting"></a>

### Лесоруб <a href="#лесоруб" id="лесоруб"></a>


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_WOODCUTTING` |


## BIRCH_GROVE <a href="#birch_grove" id="birch_grove"></a>

### Березняк <a href="#березняк" id="березняк"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 71 |
| nothing | 0 |
| Результат · `output` | [Birch Log](Items/Items-MATERIAL.md#birch_log) |
| Опыт · `experience` | 4 |
| Дополнительная добыча · `extra` | item: [Bark](Items/Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Jungle Egg](Items/Items-PET.md#pet_egg_jungle); Шанс (единица по правилам подсистемы): 2 |
| chain | `wood` |


## WOODCUTTING_REFINE <a href="#woodcutting_refine" id="woodcutting_refine"></a>

### Выдержка древесины <a href="#выдержка-древесины" id="выдержка-древесины"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 5 |
| Длительность, с · `seconds` | 240 |
| nothing | 0 |
| Результат · `output` | `` |
| Опыт · `experience` | 5 |
| Вид · `kind` | `REFINE` |
| ratio | 5 |


## OAK_GROVE <a href="#oak_grove" id="oak_grove"></a>

### Дубрава <a href="#дубрава" id="дубрава"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 147 |
| nothing | 0 |
| Результат · `output` | [Oak Log](Items/Items-MATERIAL.md#oak_log) |
| Опыт · `experience` | 9 |
| Дополнительная добыча · `extra` | item: [Bark](Items/Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Abyss Egg](Items/Items-PET.md#pet_egg_abyss); Шанс (единица по правилам подсистемы): 2 |
| chain | `wood` |


## YEW_GROVE <a href="#yew_grove" id="yew_grove"></a>

### Тисовая роща <a href="#тисовая-роща" id="тисовая-роща"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 277 |
| nothing | 0 |
| Результат · `output` | [Yew Log](Items/Items-MATERIAL.md#yew_log) |
| Опыт · `experience` | 16 |
| Дополнительная добыча · `extra` | item: [Bark](Items/Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Crypt Egg](Items/Items-PET.md#pet_egg_crypt); Шанс (единица по правилам подсистемы): 2 |
| chain | `wood` |


## IRONWOOD_GROVE <a href="#ironwood_grove" id="ironwood_grove"></a>

### Роща железного дерева <a href="#роща-железного-дерева" id="роща-железного-дерева"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 452 |
| nothing | 0 |
| Результат · `output` | [Ironwood Log](Items/Items-MATERIAL.md#ironwood_log) |
| Опыт · `experience` | 26 |
| Дополнительная добыча · `extra` | item: [Bark](Items/Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Ash Egg](Items/Items-PET.md#pet_egg_ash); Шанс (единица по правилам подсистемы): 2 |
| chain | `wood` |


## GHOST_ASH_GROVE <a href="#ghost_ash_grove" id="ghost_ash_grove"></a>

### Призрачный ясеневник <a href="#призрачный-ясеневник" id="призрачный-ясеневник"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 667 |
| nothing | 0 |
| Результат · `output` | [Ghost Ash Log](Items/Items-MATERIAL.md#ghost_ash_log) |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | item: [Bark](Items/Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Temple Egg](Items/Items-PET.md#pet_egg_temple); Шанс (единица по правилам подсистемы): 2 |
| chain | `wood` |


## ELDERHEART_GROVE <a href="#elderheart_grove" id="elderheart_grove"></a>

### Роща древнего сердца <a href="#роща-древнего-сердца" id="роща-древнего-сердца"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 900 |
| nothing | 0 |
| Результат · `output` | [Elderheart Log](Items/Items-MATERIAL.md#elderheart_log) |
| Опыт · `experience` | 40 |
| chain | `wood` |
| Дополнительная добыча · `extra` | item: [Bark](Items/Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Desert Egg](Items/Items-PET.md#pet_egg_desert); Шанс (единица по правилам подсистемы): 2 |


## WORLDTREE_GROVE <a href="#worldtree_grove" id="worldtree_grove"></a>

### Роща мирового древа <a href="#роща-мирового-древа" id="роща-мирового-древа"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 1300 |
| nothing | 0 |
| Результат · `output` | [Worldtree Log](Items/Items-MATERIAL.md#worldtree_log) |
| Опыт · `experience` | 55 |
| chain | `wood` |
| Дополнительная добыча · `extra` | item: [Bark](Items/Items-MATERIAL.md#bark); Шанс (единица по правилам подсистемы): 8; item: [Orb of Quality](Items/Items-CURRENCY.md#quality_orb); Шанс (единица по правилам подсистемы): 3; item: [Volcano Egg](Items/Items-PET.md#pet_egg_volcano); Шанс (единица по правилам подсистемы): 2 |


## SMITHING <a href="#smithing" id="smithing"></a>

### Кузнечное дело <a href="#кузнечное-дело" id="кузнечное-дело"></a>


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_SMITHING` |


## COPPER_FORGING <a href="#copper_forging" id="copper_forging"></a>

### Медная ковка <a href="#медная-ковка" id="медная-ковка"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 300 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Copper Ore](Items/Items-MATERIAL.md#copper_ore); Количество: 5 |
| band | 1; 12 |
| additives | Да |


## IRON_FORGING <a href="#iron_forging" id="iron_forging"></a>

### Железная ковка <a href="#железная-ковка" id="железная-ковка"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 400 |
| nothing | 27 |
| Результат · `output` | `` |
| Опыт · `experience` | 24 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Iron Ore](Items/Items-MATERIAL.md#iron_ore); Количество: 5; item: [Oak Log](Items/Items-MATERIAL.md#oak_log); Количество: 1 |
| band | 10; 25 |
| additives | Да |


## SILVER_FORGING <a href="#silver_forging" id="silver_forging"></a>

### Серебряная ковка <a href="#серебряная-ковка" id="серебряная-ковка"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 500 |
| nothing | 30 |
| Результат · `output` | `` |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Silver Ore](Items/Items-MATERIAL.md#silver_ore); Количество: 5; item: [Yew Log](Items/Items-MATERIAL.md#yew_log); Количество: 1 |
| band | 20; 40 |
| additives | Да |


## MITHRIL_FORGING <a href="#mithril_forging" id="mithril_forging"></a>

### Мифриловая ковка <a href="#мифриловая-ковка" id="мифриловая-ковка"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 600 |
| nothing | 32 |
| Результат · `output` | `` |
| Опыт · `experience` | 60 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Mithril Ore](Items/Items-MATERIAL.md#mithril_ore); Количество: 5; item: [Ironwood Log](Items/Items-MATERIAL.md#ironwood_log); Количество: 1 |
| band | 35; 60 |
| additives | Да |


## ADAMANT_FORGING <a href="#adamant_forging" id="adamant_forging"></a>

### Адамантовая ковка <a href="#адамантовая-ковка" id="адамантовая-ковка"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 600 |
| nothing | 35 |
| Результат · `output` | `` |
| Опыт · `experience` | 85 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Adamant Ore](Items/Items-MATERIAL.md#adamant_ore); Количество: 5; item: [Ghost Ash Log](Items/Items-MATERIAL.md#ghost_ash_log); Количество: 1 |
| band | 55; 100 |
| additives | Да |


## STARMETAL_FORGING <a href="#starmetal_forging" id="starmetal_forging"></a>

### Ковка звёздного металла <a href="#ковка-звёздного-металла" id="ковка-звёздного-металла"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 700 |
| nothing | 35 |
| Результат · `output` | `` |
| Опыт · `experience` | 102 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Starmetal Ore](Items/Items-MATERIAL.md#starmetal_ore); Количество: 5; item: [Elderheart Log](Items/Items-MATERIAL.md#elderheart_log); Количество: 1 |
| band | 75; 90 |
| additives | Да |


## WORLD_FORGING <a href="#world_forging" id="world_forging"></a>

### Ковка мирового горна <a href="#ковка-мирового-горна" id="ковка-мирового-горна"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 800 |
| nothing | 35 |
| Результат · `output` | `` |
| Опыт · `experience` | 119 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `EQUIPMENT` |
| Затраты · `inputs` | item: [Starmetal Ore](Items/Items-MATERIAL.md#starmetal_ore); Количество: 5; item: [Worldtree Log](Items/Items-MATERIAL.md#worldtree_log); Количество: 1; item: [Onyx](Items/Items-MATERIAL.md#onyx); Количество: 1 |
| band | 85; 100 |
| additives | Да |


## ALCHEMY <a href="#alchemy" id="alchemy"></a>

### Алхимия <a href="#алхимия" id="алхимия"></a>


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_ALCHEMY` |


## BLOOD_FLUX_BREW <a href="#blood_flux_brew" id="blood_flux_brew"></a>

### Варка: примесь крови <a href="#варка-примесь-крови" id="варка-примесь-крови"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 200 |
| nothing | 25 |
| Результат · `output` | [Blood Flux](Items/Items-MATERIAL.md#blood_flux) |
| Опыт · `experience` | 6 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Wormwood](Items/Items-MATERIAL.md#wormwood); Количество: 3; item: [Cave Mushroom](Items/Items-MATERIAL.md#cave_mushroom); Количество: 1 |


## TRANSMUTATION_BREW <a href="#transmutation_brew" id="transmutation_brew"></a>

### Варка: Orb of Transmutation <a href="#варка-orb-of-transmutation" id="варка-orb-of-transmutation"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 150 |
| nothing | 25 |
| Результат · `output` | [Orb of Transmutation](Items/Items-CURRENCY.md#orb_of_transmutation) |
| Опыт · `experience` | 4 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Wormwood](Items/Items-MATERIAL.md#wormwood); Количество: 2 |


## SMALL_LIFE_FLASK_BREW <a href="#small_life_flask_brew" id="small_life_flask_brew"></a>

### Сварить: малая фляга жизни <a href="#сварить-малая-фляга-жизни" id="сварить-малая-фляга-жизни"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 244 |
| nothing | 25 |
| Результат · `output` | [Small Life Flask](Equipment/Equipment-FLASK.md#flask_small_life) |
| Опыт · `experience` | 8 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 1; item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 2 |


## SMALL_MANA_FLASK_BREW <a href="#small_mana_flask_brew" id="small_mana_flask_brew"></a>

### Сварить: малая фляга маны <a href="#сварить-малая-фляга-маны" id="сварить-малая-фляга-маны"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 244 |
| nothing | 25 |
| Результат · `output` | [Small Mana Flask](Equipment/Equipment-FLASK.md#flask_small_mana) |
| Опыт · `experience` | 8 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 1; item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 2 |


## QUICKSILVER_FLASK_BREW <a href="#quicksilver_flask_brew" id="quicksilver_flask_brew"></a>

### Сварить: ртутная фляга <a href="#сварить-ртутная-фляга" id="сварить-ртутная-фляга"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 4 |
| Длительность, с · `seconds` | 256 |
| nothing | 25 |
| Результат · `output` | [Quicksilver Flask](Equipment/Equipment-FLASK.md#flask_quicksilver) |
| Опыт · `experience` | 10 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Silver Ore](Items/Items-MATERIAL.md#silver_ore); Количество: 2 |


## STONE_FLUX_BREW <a href="#stone_flux_brew" id="stone_flux_brew"></a>

### Варка: каменная примесь <a href="#варка-каменная-примесь" id="варка-каменная-примесь"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 5 |
| Длительность, с · `seconds` | 200 |
| nothing | 25 |
| Результат · `output` | [Stone Flux](Items/Items-MATERIAL.md#stone_flux) |
| Опыт · `experience` | 8 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Cave Mushroom](Items/Items-MATERIAL.md#cave_mushroom); Количество: 2; item: [Copper Ore](Items/Items-MATERIAL.md#copper_ore); Количество: 2 |


## AUGMENTATION_BREW <a href="#augmentation_brew" id="augmentation_brew"></a>

### Варка: Orb of Augmentation <a href="#варка-orb-of-augmentation" id="варка-orb-of-augmentation"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 5 |
| Длительность, с · `seconds` | 150 |
| nothing | 25 |
| Результат · `output` | [Orb of Augmentation](Items/Items-CURRENCY.md#orb_of_augmentation) |
| Опыт · `experience` | 5 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Wormwood](Items/Items-MATERIAL.md#wormwood); Количество: 3 |


## MEDIUM_LIFE_FLASK_BREW <a href="#medium_life_flask_brew" id="medium_life_flask_brew"></a>

### Сварить: средняя фляга жизни <a href="#сварить-средняя-фляга-жизни" id="сварить-средняя-фляга-жизни"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 7 |
| Длительность, с · `seconds` | 272 |
| nothing | 26 |
| Результат · `output` | [Medium Life Flask](Equipment/Equipment-FLASK.md#flask_medium_life) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 1; item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 3 |


## MEDIUM_MANA_FLASK_BREW <a href="#medium_mana_flask_brew" id="medium_mana_flask_brew"></a>

### Сварить: средняя фляга маны <a href="#сварить-средняя-фляга-маны" id="сварить-средняя-фляга-маны"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 7 |
| Длительность, с · `seconds` | 272 |
| nothing | 26 |
| Результат · `output` | [Medium Mana Flask](Equipment/Equipment-FLASK.md#flask_medium_mana) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 1; item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 3 |


## RUBY_FLASK_BREW <a href="#ruby_flask_brew" id="ruby_flask_brew"></a>

### Сварить: рубиновая фляга <a href="#сварить-рубиновая-фляга" id="сварить-рубиновая-фляга"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 7 |
| Длительность, с · `seconds` | 272 |
| nothing | 26 |
| Результат · `output` | [Ruby Flask](Equipment/Equipment-FLASK.md#flask_ruby) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Ruby](Items/Items-MATERIAL.md#ruby); Количество: 2 |


## SAPPHIRE_FLASK_BREW <a href="#sapphire_flask_brew" id="sapphire_flask_brew"></a>

### Сварить: сапфировая фляга <a href="#сварить-сапфировая-фляга" id="сварить-сапфировая-фляга"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 7 |
| Длительность, с · `seconds` | 272 |
| nothing | 26 |
| Результат · `output` | [Sapphire Flask](Equipment/Equipment-FLASK.md#flask_sapphire) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Sapphire](Items/Items-MATERIAL.md#sapphire); Количество: 2 |


## TOPAZ_FLASK_BREW <a href="#topaz_flask_brew" id="topaz_flask_brew"></a>

### Сварить: топазовая фляга <a href="#сварить-топазовая-фляга" id="сварить-топазовая-фляга"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 7 |
| Длительность, с · `seconds` | 272 |
| nothing | 26 |
| Результат · `output` | [Topaz Flask](Equipment/Equipment-FLASK.md#flask_topaz) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Topaz](Items/Items-MATERIAL.md#topaz); Количество: 2 |


## ALTERATION_BREW <a href="#alteration_brew" id="alteration_brew"></a>

### Варка: Orb of Alteration <a href="#варка-orb-of-alteration" id="варка-orb-of-alteration"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 200 |
| nothing | 28 |
| Результат · `output` | [Orb of Alteration](Items/Items-CURRENCY.md#orb_of_alteration) |
| Опыт · `experience` | 7 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 2; item: [Copper Ore](Items/Items-MATERIAL.md#copper_ore); Количество: 1 |


## CONDENSE_ESSENCE <a href="#condense_essence" id="condense_essence"></a>

### Сгустить эссенцию <a href="#сгустить-эссенцию" id="сгустить-эссенцию"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 300 |
| nothing | 10 |
| Результат · `output` | `` |
| Опыт · `experience` | 18 |
| Вид · `kind` | `CONDENSE` |
| step | 4 |


## FIRE_FLUX_BREW <a href="#fire_flux_brew" id="fire_flux_brew"></a>

### Варка: огненная примесь <a href="#варка-огненная-примесь" id="варка-огненная-примесь"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 250 |
| nothing | 27 |
| Результат · `output` | [Fire Flux](Items/Items-MATERIAL.md#fire_flux) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 3; item: [Topaz](Items/Items-MATERIAL.md#topaz); Количество: 1 |


## HORDE_BREW <a href="#horde_brew" id="horde_brew"></a>

### Варка: Сфера удачи <a href="#варка-сфера-удачи" id="варка-сфера-удачи"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 300 |
| nothing | 30 |
| Результат · `output` | [Orb of Chance](Items/Items-CURRENCY.md#orb_of_chance) |
| Опыт · `experience` | 12 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 2; item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 1 |


## GRANITE_FLASK_BREW <a href="#granite_flask_brew" id="granite_flask_brew"></a>

### Сварить: гранитная фляга <a href="#сварить-гранитная-фляга" id="сварить-гранитная-фляга"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 288 |
| nothing | 27 |
| Результат · `output` | [Granite Flask](Equipment/Equipment-FLASK.md#flask_granite) |
| Опыт · `experience` | 14 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Rough Stone](Items/Items-STONE_STOCK.md#stone_rough); Количество: 2 |


## JADE_FLASK_BREW <a href="#jade_flask_brew" id="jade_flask_brew"></a>

### Сварить: нефритовая фляга <a href="#сварить-нефритовая-фляга" id="сварить-нефритовая-фляга"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 288 |
| nothing | 27 |
| Результат · `output` | [Jade Flask](Equipment/Equipment-FLASK.md#flask_jade) |
| Опыт · `experience` | 14 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Wormwood](Items/Items-MATERIAL.md#wormwood); Количество: 2 |


## SCOURING_BREW <a href="#scouring_brew" id="scouring_brew"></a>

### Варка: Orb of Scouring <a href="#варка-orb-of-scouring" id="варка-orb-of-scouring"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 12 |
| Длительность, с · `seconds` | 200 |
| nothing | 30 |
| Результат · `output` | [Orb of Scouring](Items/Items-CURRENCY.md#orb_of_scouring) |
| Опыт · `experience` | 9 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 2 |


## BAUBLE_BREW <a href="#bauble_brew" id="bauble_brew"></a>

### Варка: Сфера качества <a href="#варка-сфера-качества" id="варка-сфера-качества"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 12 |
| Длительность, с · `seconds` | 300 |
| nothing | 30 |
| Результат · `output` | [Orb of Quality](Items/Items-CURRENCY.md#quality_orb) |
| Опыт · `experience` | 14 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 1 |


## LARGE_LIFE_FLASK_BREW <a href="#large_life_flask_brew" id="large_life_flask_brew"></a>

### Сварить: большая фляга жизни <a href="#сварить-большая-фляга-жизни" id="сварить-большая-фляга-жизни"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 14 |
| Длительность, с · `seconds` | 304 |
| nothing | 28 |
| Результат · `output` | [Large Life Flask](Equipment/Equipment-FLASK.md#flask_large_life) |
| Опыт · `experience` | 16 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 1; item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 4 |


## LARGE_MANA_FLASK_BREW <a href="#large_mana_flask_brew" id="large_mana_flask_brew"></a>

### Сварить: большая фляга маны <a href="#сварить-большая-фляга-маны" id="сварить-большая-фляга-маны"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 14 |
| Длительность, с · `seconds` | 304 |
| nothing | 28 |
| Результат · `output` | [Large Mana Flask](Equipment/Equipment-FLASK.md#flask_large_mana) |
| Опыт · `experience` | 16 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 1; item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 4 |


## FROST_FLUX_BREW <a href="#frost_flux_brew" id="frost_flux_brew"></a>

### Варка: ледяная примесь <a href="#варка-ледяная-примесь" id="варка-ледяная-примесь"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 15 |
| Длительность, с · `seconds` | 250 |
| nothing | 28 |
| Результат · `output` | [Frost Flux](Items/Items-MATERIAL.md#frost_flux) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Shadebloom](Items/Items-MATERIAL.md#shadebloom); Количество: 3; item: [Sapphire](Items/Items-MATERIAL.md#sapphire); Количество: 1 |


## ALCHEMY_BREW <a href="#alchemy_brew" id="alchemy_brew"></a>

### Варка: Orb of Alchemy <a href="#варка-orb-of-alchemy" id="варка-orb-of-alchemy"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 15 |
| Длительность, с · `seconds` | 300 |
| nothing | 32 |
| Результат · `output` | [Orb of Alchemy](Items/Items-CURRENCY.md#orb_of_alchemy) |
| Опыт · `experience` | 14 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Shadebloom](Items/Items-MATERIAL.md#shadebloom); Количество: 2; item: [Topaz](Items/Items-MATERIAL.md#topaz); Количество: 1 |


## MAGUS_BREW <a href="#magus_brew" id="magus_brew"></a>

### Варка: Сфера сожаления <a href="#варка-сфера-сожаления" id="варка-сфера-сожаления"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 15 |
| Длительность, с · `seconds` | 360 |
| nothing | 30 |
| Результат · `output` | [Orb of Regret](Items/Items-CURRENCY.md#orb_of_regret) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 1; item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 2; item: [Orb of Transmutation](Items/Items-CURRENCY.md#orb_of_transmutation); Количество: 4 |


## SILVER_FLASK_BREW <a href="#silver_flask_brew" id="silver_flask_brew"></a>

### Сварить: серебряная фляга <a href="#сварить-серебряная-фляга" id="сварить-серебряная-фляга"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 17 |
| Длительность, с · `seconds` | 320 |
| nothing | 29 |
| Результат · `output` | [Silver Flask](Equipment/Equipment-FLASK.md#flask_silver) |
| Опыт · `experience` | 18 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Silver Ore](Items/Items-MATERIAL.md#silver_ore); Количество: 2 |


## PERIL_BREW <a href="#peril_brew" id="peril_brew"></a>

### Варка: Благодатная сфера <a href="#варка-благодатная-сфера" id="варка-благодатная-сфера"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 450 |
| nothing | 32 |
| Результат · `output` | [Blessed Orb](Items/Items-CURRENCY.md#blessed_orb) |
| Опыт · `experience` | 17 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 3; item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 2; item: [Orb of Alteration](Items/Items-CURRENCY.md#orb_of_alteration); Количество: 6 |


## STORM_FLUX_BREW <a href="#storm_flux_brew" id="storm_flux_brew"></a>

### Варка: грозовая примесь <a href="#варка-грозовая-примесь" id="варка-грозовая-примесь"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 300 |
| nothing | 30 |
| Результат · `output` | [Storm Flux](Items/Items-MATERIAL.md#storm_flux) |
| Опыт · `experience` | 18 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 3; item: [Topaz](Items/Items-MATERIAL.md#topaz); Количество: 1 |


## MERCY_BREW <a href="#mercy_brew" id="mercy_brew"></a>

### Варка: Сфера ваал <a href="#варка-сфера-ваал" id="варка-сфера-ваал"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 600 |
| nothing | 35 |
| Результат · `output` | [Vaal Orb](Items/Items-CURRENCY.md#vaal_orb) |
| Опыт · `experience` | 20 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Shadebloom](Items/Items-MATERIAL.md#shadebloom); Количество: 2; item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 2; item: [Orb of Alchemy](Items/Items-CURRENCY.md#orb_of_alchemy); Количество: 3 |


## BASALT_FLASK_BREW <a href="#basalt_flask_brew" id="basalt_flask_brew"></a>

### Сварить: базальтовая фляга <a href="#сварить-базальтовая-фляга" id="сварить-базальтовая-фляга"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 20 |
| Длительность, с · `seconds` | 336 |
| nothing | 29 |
| Результат · `output` | [Basalt Flask](Equipment/Equipment-FLASK.md#flask_basalt) |
| Опыт · `experience` | 20 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Iron Ore](Items/Items-MATERIAL.md#iron_ore); Количество: 2 |


## GREATER_LIFE_FLASK_BREW <a href="#greater_life_flask_brew" id="greater_life_flask_brew"></a>

### Сварить: великая фляга жизни <a href="#сварить-великая-фляга-жизни" id="сварить-великая-фляга-жизни"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 22 |
| Длительность, с · `seconds` | 344 |
| nothing | 30 |
| Результат · `output` | [Greater Life Flask](Equipment/Equipment-FLASK.md#flask_greater_life) |
| Опыт · `experience` | 21 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 5 |


## GREATER_MANA_FLASK_BREW <a href="#greater_mana_flask_brew" id="greater_mana_flask_brew"></a>

### Сварить: великая фляга маны <a href="#сварить-великая-фляга-маны" id="сварить-великая-фляга-маны"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 22 |
| Длительность, с · `seconds` | 344 |
| nothing | 30 |
| Результат · `output` | [Greater Mana Flask](Equipment/Equipment-FLASK.md#flask_greater_mana) |
| Опыт · `experience` | 21 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 5 |


## ESSENCE_BREW <a href="#essence_brew" id="essence_brew"></a>

### Варка: Сфера раскрытия <a href="#варка-сфера-раскрытия" id="варка-сфера-раскрытия"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 22 |
| Длительность, с · `seconds` | 480 |
| nothing | 36 |
| Результат · `output` | [Unveiling Orb](Items/Items-CURRENCY.md#unveiling_orb) |
| Опыт · `experience` | 26 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Shadebloom](Items/Items-MATERIAL.md#shadebloom); Количество: 2; item: [Sapphire](Items/Items-MATERIAL.md#sapphire); Количество: 1; item: [Orb of Alteration](Items/Items-CURRENCY.md#orb_of_alteration); Количество: 4 |


## DIAMOND_FLASK_BREW <a href="#diamond_flask_brew" id="diamond_flask_brew"></a>

### Сварить: алмазная фляга <a href="#сварить-алмазная-фляга" id="сварить-алмазная-фляга"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 352 |
| nothing | 30 |
| Результат · `output` | [Diamond Flask](Equipment/Equipment-FLASK.md#flask_diamond) |
| Опыт · `experience` | 22 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Mithril Ore](Items/Items-MATERIAL.md#mithril_ore); Количество: 2 |


## QUICK_FLUX_BREW <a href="#quick_flux_brew" id="quick_flux_brew"></a>

### Варка: ртутная примесь <a href="#варка-ртутная-примесь" id="варка-ртутная-примесь"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 25 |
| Длительность, с · `seconds` | 300 |
| nothing | 32 |
| Результат · `output` | [Quick Flux](Items/Items-MATERIAL.md#quick_flux) |
| Опыт · `experience` | 22 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 3; item: [Ruby](Items/Items-MATERIAL.md#ruby); Количество: 1 |


## CHAOS_BREW <a href="#chaos_brew" id="chaos_brew"></a>

### Варка: Chaos Orb <a href="#варка-chaos-orb" id="варка-chaos-orb"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 25 |
| Длительность, с · `seconds` | 450 |
| nothing | 38 |
| Результат · `output` | [Chaos Orb](Items/Items-CURRENCY.md#chaos_orb) |
| Опыт · `experience` | 24 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 2; item: [Sapphire](Items/Items-MATERIAL.md#sapphire); Количество: 1 |


## ELITE_BREW <a href="#elite_brew" id="elite_brew"></a>

### Варка: Знамение порчи <a href="#варка-знамение-порчи" id="варка-знамение-порчи"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 28 |
| Длительность, с · `seconds` | 600 |
| nothing | 35 |
| Результат · `output` | [Omen of Corruption](Items/Items-OMEN.md#omen_corruption) |
| Опыт · `experience` | 24 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 2; item: [Sapphire](Items/Items-MATERIAL.md#sapphire); Количество: 1; item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 1; item: [Orb of Alchemy](Items/Items-CURRENCY.md#orb_of_alchemy); Количество: 3 |


## QUARTZ_FLASK_BREW <a href="#quartz_flask_brew" id="quartz_flask_brew"></a>

### Сварить: кварцевая фляга <a href="#сварить-кварцевая-фляга" id="сварить-кварцевая-фляга"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 25 |
| Длительность, с · `seconds` | 360 |
| nothing | 31 |
| Результат · `output` | [Quartz Flask](Equipment/Equipment-FLASK.md#flask_quartz) |
| Опыт · `experience` | 23 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2 |


## AMETHYST_FLASK_BREW <a href="#amethyst_flask_brew" id="amethyst_flask_brew"></a>

### Сварить: аметистовая фляга <a href="#сварить-аметистовая-фляга" id="сварить-аметистовая-фляга"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 25 |
| Длительность, с · `seconds` | 360 |
| nothing | 31 |
| Результат · `output` | [Amethyst Flask](Equipment/Equipment-FLASK.md#flask_amethyst) |
| Опыт · `experience` | 23 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Void Root](Items/Items-MATERIAL.md#void_root); Количество: 2 |


## SCRIBE_BREW <a href="#scribe_brew" id="scribe_brew"></a>

### Варка: Знамение выбора <a href="#варка-знамение-выбора" id="варка-знамение-выбора"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 36 |
| Длительность, с · `seconds` | 900 |
| nothing | 36 |
| Результат · `output` | [Omen of Choice](Items/Items-OMEN.md#omen_choice) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 3; item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 2; item: [Chaos Orb](Items/Items-CURRENCY.md#chaos_orb); Количество: 3 |


## REGAL_BREW <a href="#regal_brew" id="regal_brew"></a>

### Варка: Regal Orb <a href="#варка-regal-orb" id="варка-regal-orb"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 450 |
| nothing | 40 |
| Результат · `output` | [Regal Orb](Items/Items-CURRENCY.md#regal_orb) |
| Опыт · `experience` | 28 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Moon Lotus](Items/Items-MATERIAL.md#moon_lotus); Количество: 2; item: [Silver Ore](Items/Items-MATERIAL.md#silver_ore); Количество: 2 |


## BOUNTY_BREW <a href="#bounty_brew" id="bounty_brew"></a>

### Варка: Сфера отмены <a href="#варка-сфера-отмены" id="варка-сфера-отмены"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 38 |
| Длительность, с · `seconds` | 1200 |
| nothing | 38 |
| Результат · `output` | [Orb of Annulment](Items/Items-CURRENCY.md#orb_of_annulment) |
| Опыт · `experience` | 28 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Moon Lotus](Items/Items-MATERIAL.md#moon_lotus); Количество: 2; item: [Topaz](Items/Items-MATERIAL.md#topaz); Количество: 1; item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 2; item: [Chaos Orb](Items/Items-CURRENCY.md#chaos_orb); Количество: 7 |


## TREASURE_BREW <a href="#treasure_brew" id="treasure_brew"></a>

### Варка: Знамение света <a href="#варка-знамение-света" id="варка-знамение-света"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 32 |
| Длительность, с · `seconds` | 600 |
| nothing | 38 |
| Результат · `output` | [Omen of Light](Items/Items-OMEN.md#omen_light) |
| Опыт · `experience` | 32 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Shadebloom](Items/Items-MATERIAL.md#shadebloom); Количество: 3; item: [Silver Ore](Items/Items-MATERIAL.md#silver_ore); Количество: 2; item: [Orb of Alchemy](Items/Items-CURRENCY.md#orb_of_alchemy); Количество: 3 |


## GRAND_LIFE_FLASK_BREW <a href="#grand_life_flask_brew" id="grand_life_flask_brew"></a>

### Сварить: грандиозная фляга жизни <a href="#сварить-грандиозная-фляга-жизни" id="сварить-грандиозная-фляга-жизни"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 384 |
| nothing | 32 |
| Результат · `output` | [Grand Life Flask](Equipment/Equipment-FLASK.md#flask_grand_life) |
| Опыт · `experience` | 26 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 6 |


## GRAND_MANA_FLASK_BREW <a href="#grand_mana_flask_brew" id="grand_mana_flask_brew"></a>

### Сварить: грандиозная фляга маны <a href="#сварить-грандиозная-фляга-маны" id="сварить-грандиозная-фляга-маны"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 384 |
| nothing | 32 |
| Результат · `output` | [Grand Mana Flask](Equipment/Equipment-FLASK.md#flask_grand_mana) |
| Опыт · `experience` | 26 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Moon Lotus](Items/Items-MATERIAL.md#moon_lotus); Количество: 6 |


## SULPHUR_FLASK_BREW <a href="#sulphur_flask_brew" id="sulphur_flask_brew"></a>

### Сварить: сернистая фляга <a href="#сварить-сернистая-фляга" id="сварить-сернистая-фляга"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 380 |
| nothing | 32 |
| Результат · `output` | [Sulphur Flask](Equipment/Equipment-FLASK.md#flask_sulphur) |
| Опыт · `experience` | 25 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Fire Flux](Items/Items-MATERIAL.md#fire_flux); Количество: 2 |


## GILDED_BREW <a href="#gilded_brew" id="gilded_brew"></a>

### Варка: Знамение великого возвышения <a href="#варка-знамение-великого-возвышения" id="варка-знамение-великого-возвышения"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 35 |
| Длительность, с · `seconds` | 600 |
| nothing | 38 |
| Результат · `output` | [Omen of Greater Exaltation](Items/Items-OMEN.md#omen_greater_exaltation) |
| Опыт · `experience` | 32 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 3; item: [Topaz](Items/Items-MATERIAL.md#topaz); Количество: 1; item: [Regal Orb](Items/Items-CURRENCY.md#regal_orb); Количество: 1 |


## EMPOWERING_BREW <a href="#empowering_brew" id="empowering_brew"></a>

### Варка: Знамение тира <a href="#варка-знамение-тира" id="варка-знамение-тира"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1200 |
| nothing | 45 |
| Результат · `output` | [Omen of Tiers](Items/Items-OMEN.md#omen_tier) |
| Опыт · `experience` | 35 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Moon Lotus](Items/Items-MATERIAL.md#moon_lotus); Количество: 2; item: [Ruby](Items/Items-MATERIAL.md#ruby); Количество: 1; item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 2; item: [Regal Orb](Items/Items-CURRENCY.md#regal_orb); Количество: 3 |


## DIVINE_BREW <a href="#divine_brew" id="divine_brew"></a>

### Варка: Divine Orb <a href="#варка-divine-orb" id="варка-divine-orb"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1800 |
| nothing | 55 |
| Результат · `output` | [Divine Orb](Items/Items-CURRENCY.md#divine_orb) |
| Опыт · `experience` | 45 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items/Items-MATERIAL.md#void_root); Количество: 4; item: [Mithril Ore](Items/Items-MATERIAL.md#mithril_ore); Количество: 4; item: [Chaos Orb](Items/Items-CURRENCY.md#chaos_orb); Количество: 5 |


## GIANT_LIFE_FLASK_BREW <a href="#giant_life_flask_brew" id="giant_life_flask_brew"></a>

### Сварить: гигантская фляга жизни <a href="#сварить-гигантская-фляга-жизни" id="сварить-гигантская-фляга-жизни"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 432 |
| nothing | 34 |
| Результат · `output` | [Giant Life Flask](Equipment/Equipment-FLASK.md#flask_giant_life) |
| Опыт · `experience` | 32 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 7 |


## GIANT_MANA_FLASK_BREW <a href="#giant_mana_flask_brew" id="giant_mana_flask_brew"></a>

### Сварить: гигантская фляга маны <a href="#сварить-гигантская-фляга-маны" id="сварить-гигантская-фляга-маны"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 432 |
| nothing | 34 |
| Результат · `output` | [Giant Mana Flask](Equipment/Equipment-FLASK.md#flask_giant_mana) |
| Опыт · `experience` | 32 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 2; item: [Moon Lotus](Items/Items-MATERIAL.md#moon_lotus); Количество: 7 |


## WARDEN_BREW <a href="#warden_brew" id="warden_brew"></a>

### Варка: Сфера раскола <a href="#варка-сфера-раскола" id="варка-сфера-раскола"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 42 |
| Длительность, с · `seconds` | 1800 |
| nothing | 38 |
| Результат · `output` | [Fracturing Orb](Items/Items-CURRENCY.md#fracturing_orb) |
| Опыт · `experience` | 32 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items/Items-MATERIAL.md#void_root); Количество: 2; item: [Ruby](Items/Items-MATERIAL.md#ruby); Количество: 1; item: [Divine Orb](Items/Items-CURRENCY.md#divine_orb); Количество: 1; item: [Chaos Orb](Items/Items-CURRENCY.md#chaos_orb); Количество: 10 |


## EXALTED_BREW <a href="#exalted_brew" id="exalted_brew"></a>

### Варка: Exalted Orb <a href="#варка-exalted-orb" id="варка-exalted-orb"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 1800 |
| nothing | 60 |
| Результат · `output` | [Exalted Orb](Items/Items-CURRENCY.md#exalted_orb) |
| Опыт · `experience` | 55 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items/Items-MATERIAL.md#void_root); Количество: 6; item: [Ruby](Items/Items-MATERIAL.md#ruby); Количество: 4; item: [Regal Orb](Items/Items-CURRENCY.md#regal_orb); Количество: 5 |


## STAR_FLUX_BREW <a href="#star_flux_brew" id="star_flux_brew"></a>

### Сварить звёздный флюс <a href="#сварить-звёздный-флюс" id="сварить-звёздный-флюс"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 300 |
| nothing | 25 |
| Результат · `output` | [Star Flux](Items/Items-MATERIAL.md#star_flux) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Starbloom](Items/Items-MATERIAL.md#starbloom); Количество: 2; item: [Worldroot](Items/Items-MATERIAL.md#worldroot); Количество: 1 |


## DIVINE_LIFE_FLASK_BREW <a href="#divine_life_flask_brew" id="divine_life_flask_brew"></a>

### Сварить: божественная фляга жизни <a href="#сварить-божественная-фляга-жизни" id="сварить-божественная-фляга-жизни"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 480 |
| nothing | 37 |
| Результат · `output` | [Divine Life Flask](Equipment/Equipment-FLASK.md#flask_divine_life) |
| Опыт · `experience` | 38 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 3; item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 8 |


## DIVINE_MANA_FLASK_BREW <a href="#divine_mana_flask_brew" id="divine_mana_flask_brew"></a>

### Сварить: божественная фляга маны <a href="#сварить-божественная-фляга-маны" id="сварить-божественная-фляга-маны"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 480 |
| nothing | 37 |
| Результат · `output` | [Divine Mana Flask](Equipment/Equipment-FLASK.md#flask_divine_mana) |
| Опыт · `experience` | 38 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `FLASK` |
| Затраты · `inputs` | item: [Polished Stone](Items/Items-STONE_STOCK.md#stone_polished); Количество: 3; item: [Moon Lotus](Items/Items-MATERIAL.md#moon_lotus); Количество: 8 |


## POTION_EXPERIENCE_T1_BREW <a href="#potion_experience_t1_brew" id="potion_experience_t1_brew"></a>

### Варка: малое зелье опыта <a href="#варка-малое-зелье-опыта" id="варка-малое-зелье-опыта"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 400 |
| nothing | 25 |
| Результат · `output` | [Minor Experience Potion](Items/Items-MATERIAL.md#potion_experience_t1) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 3; item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 2 |


## POTION_EXPERIENCE_T2_BREW <a href="#potion_experience_t2_brew" id="potion_experience_t2_brew"></a>

### Варка: зелье опыта <a href="#варка-зелье-опыта" id="варка-зелье-опыта"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 700 |
| nothing | 25 |
| Результат · `output` | [Experience Potion](Items/Items-MATERIAL.md#potion_experience_t2) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 3; item: [Moon Lotus](Items/Items-MATERIAL.md#moon_lotus); Количество: 2 |


## POTION_EXPERIENCE_T3_BREW <a href="#potion_experience_t3_brew" id="potion_experience_t3_brew"></a>

### Варка: великое зелье опыта <a href="#варка-великое-зелье-опыта" id="варка-великое-зелье-опыта"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1000 |
| nothing | 25 |
| Результат · `output` | [Greater Experience Potion](Items/Items-MATERIAL.md#potion_experience_t3) |
| Опыт · `experience` | 50 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items/Items-MATERIAL.md#void_root); Количество: 3; item: [Starbloom](Items/Items-MATERIAL.md#starbloom); Количество: 2 |


## POTION_GOLD_T1_BREW <a href="#potion_gold_t1_brew" id="potion_gold_t1_brew"></a>

### Варка: малое зелье золота <a href="#варка-малое-зелье-золота" id="варка-малое-зелье-золота"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 400 |
| nothing | 25 |
| Результат · `output` | [Minor Gold Potion](Items/Items-MATERIAL.md#potion_gold_t1) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 3; item: [Copper Ore](Items/Items-MATERIAL.md#copper_ore); Количество: 3 |


## POTION_GOLD_T2_BREW <a href="#potion_gold_t2_brew" id="potion_gold_t2_brew"></a>

### Варка: зелье золота <a href="#варка-зелье-золота" id="варка-зелье-золота"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 700 |
| nothing | 25 |
| Результат · `output` | [Gold Potion](Items/Items-MATERIAL.md#potion_gold_t2) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 3; item: [Silver Ore](Items/Items-MATERIAL.md#silver_ore); Количество: 3 |


## POTION_GOLD_T3_BREW <a href="#potion_gold_t3_brew" id="potion_gold_t3_brew"></a>

### Варка: великое зелье золота <a href="#варка-великое-зелье-золота" id="варка-великое-зелье-золота"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1000 |
| nothing | 25 |
| Результат · `output` | [Greater Gold Potion](Items/Items-MATERIAL.md#potion_gold_t3) |
| Опыт · `experience` | 50 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items/Items-MATERIAL.md#void_root); Количество: 3; item: [Adamant Ore](Items/Items-MATERIAL.md#adamant_ore); Количество: 3 |


## POTION_RARITY_T1_BREW <a href="#potion_rarity_t1_brew" id="potion_rarity_t1_brew"></a>

### Варка: малое зелье удачи <a href="#варка-малое-зелье-удачи" id="варка-малое-зелье-удачи"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 400 |
| nothing | 25 |
| Результат · `output` | [Minor Fortune Potion](Items/Items-MATERIAL.md#potion_rarity_t1) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 3; item: [Topaz](Items/Items-MATERIAL.md#topaz); Количество: 1 |


## POTION_RARITY_T2_BREW <a href="#potion_rarity_t2_brew" id="potion_rarity_t2_brew"></a>

### Варка: зелье удачи <a href="#варка-зелье-удачи" id="варка-зелье-удачи"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 700 |
| nothing | 25 |
| Результат · `output` | [Fortune Potion](Items/Items-MATERIAL.md#potion_rarity_t2) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 3; item: [Sapphire](Items/Items-MATERIAL.md#sapphire); Количество: 1 |


## POTION_RARITY_T3_BREW <a href="#potion_rarity_t3_brew" id="potion_rarity_t3_brew"></a>

### Варка: великое зелье удачи <a href="#варка-великое-зелье-удачи" id="варка-великое-зелье-удачи"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1000 |
| nothing | 25 |
| Результат · `output` | [Greater Fortune Potion](Items/Items-MATERIAL.md#potion_rarity_t3) |
| Опыт · `experience` | 50 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items/Items-MATERIAL.md#void_root); Количество: 3; item: [Ruby](Items/Items-MATERIAL.md#ruby); Количество: 1 |


## POTION_RESIST_T1_BREW <a href="#potion_resist_t1_brew" id="potion_resist_t1_brew"></a>

### Варка: малое зелье стойкости <a href="#варка-малое-зелье-стойкости" id="варка-малое-зелье-стойкости"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 400 |
| nothing | 25 |
| Результат · `output` | [Minor Warding Potion](Items/Items-MATERIAL.md#potion_resist_t1) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 3; item: [Stone Flux](Items/Items-MATERIAL.md#stone_flux); Количество: 1 |


## POTION_RESIST_T2_BREW <a href="#potion_resist_t2_brew" id="potion_resist_t2_brew"></a>

### Варка: зелье стойкости <a href="#варка-зелье-стойкости" id="варка-зелье-стойкости"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 700 |
| nothing | 25 |
| Результат · `output` | [Warding Potion](Items/Items-MATERIAL.md#potion_resist_t2) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 3; item: [Stone Flux](Items/Items-MATERIAL.md#stone_flux); Количество: 2 |


## POTION_RESIST_T3_BREW <a href="#potion_resist_t3_brew" id="potion_resist_t3_brew"></a>

### Варка: великое зелье стойкости <a href="#варка-великое-зелье-стойкости" id="варка-великое-зелье-стойкости"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1000 |
| nothing | 25 |
| Результат · `output` | [Greater Warding Potion](Items/Items-MATERIAL.md#potion_resist_t3) |
| Опыт · `experience` | 50 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items/Items-MATERIAL.md#void_root); Количество: 3; item: [Stone Flux](Items/Items-MATERIAL.md#stone_flux); Количество: 3 |


## POTION_DAMAGE_T1_BREW <a href="#potion_damage_t1_brew" id="potion_damage_t1_brew"></a>

### Варка: малое зелье ярости <a href="#варка-малое-зелье-ярости" id="варка-малое-зелье-ярости"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 400 |
| nothing | 25 |
| Результат · `output` | [Minor Fury Potion](Items/Items-MATERIAL.md#potion_damage_t1) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 3; item: [Fire Flux](Items/Items-MATERIAL.md#fire_flux); Количество: 1 |


## POTION_DAMAGE_T2_BREW <a href="#potion_damage_t2_brew" id="potion_damage_t2_brew"></a>

### Варка: зелье ярости <a href="#варка-зелье-ярости" id="варка-зелье-ярости"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 700 |
| nothing | 25 |
| Результат · `output` | [Fury Potion](Items/Items-MATERIAL.md#potion_damage_t2) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 3; item: [Fire Flux](Items/Items-MATERIAL.md#fire_flux); Количество: 2 |


## POTION_DAMAGE_T3_BREW <a href="#potion_damage_t3_brew" id="potion_damage_t3_brew"></a>

### Варка: великое зелье ярости <a href="#варка-великое-зелье-ярости" id="варка-великое-зелье-ярости"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1000 |
| nothing | 25 |
| Результат · `output` | [Greater Fury Potion](Items/Items-MATERIAL.md#potion_damage_t3) |
| Опыт · `experience` | 50 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items/Items-MATERIAL.md#void_root); Количество: 3; item: [Fire Flux](Items/Items-MATERIAL.md#fire_flux); Количество: 3 |


## POTION_LIFE_T1_BREW <a href="#potion_life_t1_brew" id="potion_life_t1_brew"></a>

### Варка: малое зелье жизни <a href="#варка-малое-зелье-жизни" id="варка-малое-зелье-жизни"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 8 |
| Длительность, с · `seconds` | 400 |
| nothing | 25 |
| Результат · `output` | [Minor Vitality Potion](Items/Items-MATERIAL.md#potion_life_t1) |
| Опыт · `experience` | 15 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 3; item: [Blood Flux](Items/Items-MATERIAL.md#blood_flux); Количество: 1 |


## POTION_LIFE_T2_BREW <a href="#potion_life_t2_brew" id="potion_life_t2_brew"></a>

### Варка: зелье жизни <a href="#варка-зелье-жизни" id="варка-зелье-жизни"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 24 |
| Длительность, с · `seconds` | 700 |
| nothing | 25 |
| Результат · `output` | [Vitality Potion](Items/Items-MATERIAL.md#potion_life_t2) |
| Опыт · `experience` | 30 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 3; item: [Blood Flux](Items/Items-MATERIAL.md#blood_flux); Количество: 2 |


## POTION_LIFE_T3_BREW <a href="#potion_life_t3_brew" id="potion_life_t3_brew"></a>

### Варка: великое зелье жизни <a href="#варка-великое-зелье-жизни" id="варка-великое-зелье-жизни"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 40 |
| Длительность, с · `seconds` | 1000 |
| nothing | 25 |
| Результат · `output` | [Greater Vitality Potion](Items/Items-MATERIAL.md#potion_life_t3) |
| Опыт · `experience` | 50 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Void Root](Items/Items-MATERIAL.md#void_root); Количество: 3; item: [Blood Flux](Items/Items-MATERIAL.md#blood_flux); Количество: 3 |


## PET_ORB_BREEDING_BREW <a href="#pet_orb_breeding_brew" id="pet_orb_breeding_brew"></a>

### Варка: сфера скрещивания <a href="#варка-сфера-скрещивания" id="варка-сфера-скрещивания"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 25 |
| Длительность, с · `seconds` | 900 |
| nothing | 25 |
| Результат · `output` | [Orb of Breeding](Items/Items-PET.md#pet_orb_breeding) |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 4; item: [Ruby](Items/Items-MATERIAL.md#ruby); Количество: 1; item: [Blood Flux](Items/Items-MATERIAL.md#blood_flux); Количество: 2 |


## CARTOGRAPHY <a href="#cartography" id="cartography"></a>

### Картография <a href="#картография" id="картография"></a>


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_CARTOGRAPHY` |


## CHART_REGION_1 <a href="#chart_region_1" id="chart_region_1"></a>

### Карта: Берег изгнанников <a href="#карта-берег-изгнанников" id="карта-берег-изгнанников"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 324 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 14 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment/Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 1; item: [Birch Log](Items/Items-MATERIAL.md#birch_log); Количество: 2 |
| Регион · `region` | `REGION_1` |


## CHART_REGION_2 <a href="#chart_region_2" id="chart_region_2"></a>

### Карта: Кристальный хребет <a href="#карта-кристальный-хребет" id="карта-кристальный-хребет"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 382 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 24 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment/Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 1; item: [Oak Log](Items/Items-MATERIAL.md#oak_log); Количество: 2 |
| Регион · `region` | `REGION_2` |


## CHART_REGION_3 <a href="#chart_region_3" id="chart_region_3"></a>

### Карта: Выжженные пески <a href="#карта-выжженные-пески" id="карта-выжженные-пески"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 18 |
| Длительность, с · `seconds` | 440 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 35 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment/Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 2; item: [Yew Log](Items/Items-MATERIAL.md#yew_log); Количество: 2; item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 1 |
| Регион · `region` | `REGION_3` |


## CHART_REGION_4 <a href="#chart_region_4" id="chart_region_4"></a>

### Карта: Изумрудные дебри <a href="#карта-изумрудные-дебри" id="карта-изумрудные-дебри"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 27 |
| Длительность, с · `seconds` | 501 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 46 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment/Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 3; item: [Ironwood Log](Items/Items-MATERIAL.md#ironwood_log); Количество: 2; item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 1 |
| Регион · `region` | `REGION_4` |


## CHART_REGION_5 <a href="#chart_region_5" id="chart_region_5"></a>

### Карта: Огненный рубеж <a href="#карта-огненный-рубеж" id="карта-огненный-рубеж"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 35 |
| Длительность, с · `seconds` | 561 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 56 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment/Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 3; item: [Ghost Ash Log](Items/Items-MATERIAL.md#ghost_ash_log); Количество: 2; item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 1 |
| Регион · `region` | `REGION_5` |


## CHART_REGION_6 <a href="#chart_region_6" id="chart_region_6"></a>

### Карта: Бездна <a href="#карта-бездна" id="карта-бездна"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 44 |
| Длительность, с · `seconds` | 619 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 67 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment/Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 4; item: [Ghost Ash Log](Items/Items-MATERIAL.md#ghost_ash_log); Количество: 2; item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 1 |
| Регион · `region` | `REGION_6` |


## CHART_REGION_7 <a href="#chart_region_7" id="chart_region_7"></a>

### Карта: Расколотые небеса <a href="#карта-расколотые-небеса" id="карта-расколотые-небеса"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 678 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 73 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment/Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 5; item: [Ghost Ash Log](Items/Items-MATERIAL.md#ghost_ash_log); Количество: 3; item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 2 |
| Регион · `region` | `REGION_7` |


## CHART_REGION_8 <a href="#chart_region_8" id="chart_region_8"></a>

### Карта: Утонувшая империя <a href="#карта-утонувшая-империя" id="карта-утонувшая-империя"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 738 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 76 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment/Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 5; item: [Ghost Ash Log](Items/Items-MATERIAL.md#ghost_ash_log); Количество: 3; item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 2 |
| Регион · `region` | `REGION_8` |


## CHART_REGION_9 <a href="#chart_region_9" id="chart_region_9"></a>

### Карта: Чертоги богов <a href="#карта-чертоги-богов" id="карта-чертоги-богов"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 798 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 79 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment/Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 5; item: [Ghost Ash Log](Items/Items-MATERIAL.md#ghost_ash_log); Количество: 3; item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 2 |
| Регион · `region` | `REGION_9` |


## CHART_TIERED <a href="#chart_tiered" id="chart_tiered"></a>

### Карта с тиром <a href="#карта-с-тиром" id="карта-с-тиром"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 900 |
| nothing | 25 |
| Результат · `output` | `` |
| Опыт · `experience` | 90 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | [Map](Equipment/Equipment-MAP.md#map) |
| Затраты · `inputs` | item: [Worldtree Log](Items/Items-MATERIAL.md#worldtree_log); Количество: 2; item: [Starbloom](Items/Items-MATERIAL.md#starbloom); Количество: 2; item: [Onyx](Items/Items-MATERIAL.md#onyx); Количество: 1 |
| Регион · `region` | `REGION_9` |
| tier | 3 |


## SCARAB_CHESTS_CARVE <a href="#scarab_chests_carve" id="scarab_chests_carve"></a>

### Резьба: скарабей тайников <a href="#резьба-скарабей-тайников" id="резьба-скарабей-тайников"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 10 |
| Длительность, с · `seconds` | 600 |
| nothing | 25 |
| Результат · `output` | [Cache Scarab](Items/Items-MATERIAL.md#scarab_chests) |
| Опыт · `experience` | 25 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 2; item: [Oak Log](Items/Items-MATERIAL.md#oak_log); Количество: 3; item: [Amber Resin](Items/Items-MATERIAL.md#amber_resin); Количество: 1 |


## SCARAB_PACK_CARVE <a href="#scarab_pack_carve" id="scarab_pack_carve"></a>

### Резьба: скарабей стаи <a href="#резьба-скарабей-стаи" id="резьба-скарабей-стаи"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 18 |
| Длительность, с · `seconds` | 600 |
| nothing | 25 |
| Результат · `output` | [Pack Scarab](Items/Items-MATERIAL.md#scarab_pack) |
| Опыт · `experience` | 25 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Yew Log](Items/Items-MATERIAL.md#yew_log); Количество: 3; item: [Bloodwort](Items/Items-MATERIAL.md#bloodwort); Количество: 2; item: [Iron Ore](Items/Items-MATERIAL.md#iron_ore); Количество: 2 |


## SCARAB_RARITY_CARVE <a href="#scarab_rarity_carve" id="scarab_rarity_carve"></a>

### Резьба: скарабей удачи <a href="#резьба-скарабей-удачи" id="резьба-скарабей-удачи"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 26 |
| Длительность, с · `seconds` | 600 |
| nothing | 25 |
| Результат · `output` | [Fortune Scarab](Items/Items-MATERIAL.md#scarab_rarity) |
| Опыт · `experience` | 25 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `ITEM` |
| Затраты · `inputs` | item: [Ironwood Log](Items/Items-MATERIAL.md#ironwood_log); Количество: 2; item: [Topaz](Items/Items-MATERIAL.md#topaz); Количество: 1; item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 2 |


## ENCHANTING <a href="#enchanting" id="enchanting"></a>

### Зачарование <a href="#зачарование" id="зачарование"></a>


| Параметр | Значение |
| --- | --- |
| tool | `TOOL_ENCHANTING` |


## SCRIBE_BOOK_1 <a href="#scribe_book_1" id="scribe_book_1"></a>

### Переписать книгу умений I <a href="#переписать-книгу-умений-i" id="переписать-книгу-умений-i"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 1 |
| Длительность, с · `seconds` | 600 |
| nothing | 35 |
| Результат · `output` | `` |
| Опыт · `experience` | 20 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `BOOK` |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 3; item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 2 |
| band | 12 |


## CUT_JEWEL_1 <a href="#cut_jewel_1" id="cut_jewel_1"></a>

### Огранить самоцвет I <a href="#огранить-самоцвет-i" id="огранить-самоцвет-i"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 15 |
| Длительность, с · `seconds` | 700 |
| nothing | 30 |
| Результат · `output` | `` |
| Опыт · `experience` | 25 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `JEWEL` |
| Затраты · `inputs` | item: [Sapphire](Items/Items-MATERIAL.md#sapphire); Количество: 1; item: [Glowcap](Items/Items-MATERIAL.md#glowcap); Количество: 2; item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 2 |
| band | 1; 20 |


## SCRIBE_BOOK_2 <a href="#scribe_book_2" id="scribe_book_2"></a>

### Переписать книгу умений II <a href="#переписать-книгу-умений-ii" id="переписать-книгу-умений-ii"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 30 |
| Длительность, с · `seconds` | 800 |
| nothing | 35 |
| Результат · `output` | `` |
| Опыт · `experience` | 40 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `BOOK` |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 4; item: [Moon Lotus](Items/Items-MATERIAL.md#moon_lotus); Количество: 2; item: [Sapphire](Items/Items-MATERIAL.md#sapphire); Количество: 1 |
| band | 30 |


## CUT_JEWEL_2 <a href="#cut_jewel_2" id="cut_jewel_2"></a>

### Огранить самоцвет II <a href="#огранить-самоцвет-ii" id="огранить-самоцвет-ii"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 45 |
| Длительность, с · `seconds` | 1000 |
| nothing | 30 |
| Результат · `output` | `` |
| Опыт · `experience` | 55 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `JEWEL` |
| Затраты · `inputs` | item: [Onyx](Items/Items-MATERIAL.md#onyx); Количество: 1; item: [Starbloom](Items/Items-MATERIAL.md#starbloom); Количество: 2; item: [Elderheart Log](Items/Items-MATERIAL.md#elderheart_log); Количество: 1 |
| band | 20; 45 |


## SCRIBE_BOOK_3 <a href="#scribe_book_3" id="scribe_book_3"></a>

### Переписать книгу умений III <a href="#переписать-книгу-умений-iii" id="переписать-книгу-умений-iii"></a>


| Параметр | Значение |
| --- | --- |
| Уровень · `level` | 50 |
| Длительность, с · `seconds` | 1000 |
| nothing | 35 |
| Результат · `output` | `` |
| Опыт · `experience` | 60 |
| Дополнительная добыча · `extra` | — |
| Вид · `kind` | `BOOK` |
| Затраты · `inputs` | item: [Bark](Items/Items-MATERIAL.md#bark); Количество: 5; item: [Void Root](Items/Items-MATERIAL.md#void_root); Количество: 2; item: [Ruby](Items/Items-MATERIAL.md#ruby); Количество: 1 |
| band | 70 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/professions.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
