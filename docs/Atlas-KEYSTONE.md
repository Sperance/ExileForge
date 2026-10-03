# KEYSTONE

| Название | Код |
| --- | --- |
| [Сердце Сокровищницы](Atlas-KEYSTONE.md#atlas_loot_c10) | ATLAS_LOOT_C10 |
| [Венец Сокровищницы](Atlas-KEYSTONE.md#atlas_loot_c13) | ATLAS_LOOT_C13 |
| [Сердце Картографа](Atlas-KEYSTONE.md#atlas_maps_c14) | ATLAS_MAPS_C14 |
| [Венец Картографа](Atlas-KEYSTONE.md#atlas_maps_c15) | ATLAS_MAPS_C15 |
| [Сердце Вершины](Atlas-KEYSTONE.md#atlas_tiers_c02) | ATLAS_TIERS_C02 |
| [Венец Вершины](Atlas-KEYSTONE.md#atlas_tiers_c09) | ATLAS_TIERS_C09 |
| [Сердце Палача](Atlas-KEYSTONE.md#atlas_bosses_c02) | ATLAS_BOSSES_C02 |
| [Венец Палача](Atlas-KEYSTONE.md#atlas_bosses_c03) | ATLAS_BOSSES_C03 |
| [Сердце Зева](Atlas-KEYSTONE.md#atlas_abyss_c05) | ATLAS_ABYSS_C05 |
| [Венец Зева](Atlas-KEYSTONE.md#atlas_abyss_c16) | ATLAS_ABYSS_C16 |
| [Сердце Порчи](Atlas-KEYSTONE.md#atlas_vaal_c05) | ATLAS_VAAL_C05 |
| [Венец Порчи](Atlas-KEYSTONE.md#atlas_vaal_c16) | ATLAS_VAAL_C16 |
| [Сердце Призмы](Atlas-KEYSTONE.md#atlas_crystals_c02) | ATLAS_CRYSTALS_C02 |
| [Венец Призмы](Atlas-KEYSTONE.md#atlas_crystals_c08) | ATLAS_CRYSTALS_C08 |
| [Сердце Фонаря](Atlas-KEYSTONE.md#atlas_expedition_c01) | ATLAS_EXPEDITION_C01 |
| [Венец Фонаря](Atlas-KEYSTONE.md#atlas_expedition_c15) | ATLAS_EXPEDITION_C15 |
| [Сердце Горна](Atlas-KEYSTONE.md#atlas_craft_c02) | ATLAS_CRAFT_C02 |
| [Венец Горна](Atlas-KEYSTONE.md#atlas_craft_c10) | ATLAS_CRAFT_C10 |
| [Сердце Титана](Atlas-KEYSTONE.md#atlas_power_c07) | ATLAS_POWER_C07 |
| [Венец Титана](Atlas-KEYSTONE.md#atlas_power_c08) | ATLAS_POWER_C08 |
| [Сердце Двуликого](Atlas-KEYSTONE.md#atlas_influence_c01) | ATLAS_INFLUENCE_C01 |
| [Венец Двуликого](Atlas-KEYSTONE.md#atlas_influence_c17) | ATLAS_INFLUENCE_C17 |


## ATLAS_LOOT_C10

### Сердце Сокровищницы

**Свойства**

- 60% больше золота на картах · [Атлас: золото](Modifiers-ATLAS.md#atlas_gold)
- 10% к опыту на картах · [Атлас: опыт на картах](Modifiers-ATLAS.md#atlas_experience)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Сокровищница](Atlas-SMALL.md#atlas_loot_c16) |


## ATLAS_LOOT_C13

### Венец Сокровищницы

**Свойства**

- 12% к количеству добычи на картах · [Атлас: количество предметов на картах](Modifiers-ATLAS.md#atlas_quantity)
- 20% к редкости добычи на картах · [Атлас: редкость предметов на картах](Modifiers-ATLAS.md#atlas_rarity)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Сокровищница](Atlas-SMALL.md#atlas_loot_c03) |


## ATLAS_MAPS_C14

### Сердце Картографа

**Свойства**

- 50% шанс лишнего модификатора у выпавшей карты · [Атлас: лишний аффикс карты](Modifiers-ATLAS.md#atlas_map_affix)
- 60% больше шанс, что выпавшая карта редкая · [Атлас: редкие карты](Modifiers-ATLAS.md#atlas_map_rare)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Гамма Картографа](Atlas-NOTABLE.md#atlas_maps_c10) |


## ATLAS_MAPS_C15

### Венец Картографа

**Свойства**

- 30% больше шанс выпадения карт · [Атлас: шанс выпадения карт](Modifiers-ATLAS.md#atlas_map_drop)
- 40% больше шанс, что выпадет карта уровнем выше · [Атлас: карты выше](Modifiers-ATLAS.md#atlas_map_next)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Сердце Картографа](Atlas-KEYSTONE.md#atlas_maps_c14) |


## ATLAS_TIERS_C02

### Сердце Вершины

**Свойства**

- 30% к действию модификаторов карт · [Атлас: эффект модификаторов карты](Modifiers-ATLAS.md#atlas_map_effect)
- 8% к количеству добычи на картах · [Атлас: количество предметов на картах](Modifiers-ATLAS.md#atlas_quantity)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Венец Вершины](Atlas-KEYSTONE.md#atlas_tiers_c09) |


## ATLAS_TIERS_C09

### Венец Вершины

**Свойства**

- 50% больше шанс, что выпавшая карта поднимется на тир · [Атлас: тир карт](Modifiers-ATLAS.md#atlas_map_tier)
- 15% к действию модификаторов карт · [Атлас: эффект модификаторов карты](Modifiers-ATLAS.md#atlas_map_effect)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Вершина](Atlas-SMALL.md#atlas_tiers_c08) |


## ATLAS_BOSSES_C02

### Сердце Палача

**Свойства**

- Босс карты возвращается на 40% быстрее · [Атлас: скорость возвращения босса](Modifiers-ATLAS.md#atlas_boss_respawn)
- 25% больше добычи с боссов карт · [Атлас: добыча с боссов](Modifiers-ATLAS.md#atlas_boss_loot)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Палач](Atlas-SMALL.md#atlas_bosses_c01) |


## ATLAS_BOSSES_C03

### Венец Палача

**Свойства**

- 60% больше добычи с боссов карт · [Атлас: добыча с боссов](Modifiers-ATLAS.md#atlas_boss_loot)
- 40% больше шанс уникалки с босса карты · [Атлас: уникальные предметы с босса](Modifiers-ATLAS.md#atlas_boss_unique)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Альфа Палача](Atlas-NOTABLE.md#atlas_bosses_c05) |


## ATLAS_ABYSS_C05

### Сердце Зева

**Свойства**

- 80% больше шанс уникалки Бездны · [Атлас: уникалки Бездны](Modifiers-ATLAS.md#atlas_abyss_unique)
- +20% к доле редких вещей в копилке Бездны · [Атлас: редкие вещи Бездны](Modifiers-ATLAS.md#atlas_abyss_rare)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Зев](Atlas-SMALL.md#atlas_abyss_c09) |


## ATLAS_ABYSS_C16

### Венец Зева

**Свойства**

- +2 к глубине расщелин Бездны · [Атлас: глубина Бездны](Modifiers-ATLAS.md#atlas_abyss_depth)
- 40% больше вещей в копилке Бездны · [Атлас: копилка Бездны](Modifiers-ATLAS.md#atlas_abyss_hoard)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Сердце Зева](Atlas-KEYSTONE.md#atlas_abyss_c05) |


## ATLAS_VAAL_C05

### Сердце Порчи

**Свойства**

- 40% к шансу портала Ваал · [Атлас: шанс портала Ваал](Modifiers-ATLAS.md#atlas_vaal_chance)
- 60% больше шанс уникалки Ваал · [Атлас: уникалки Ваал](Modifiers-ATLAS.md#atlas_vaal_unique)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Гамма Порчи](Atlas-NOTABLE.md#atlas_vaal_c17); [Венец Порчи](Atlas-KEYSTONE.md#atlas_vaal_c16) |


## ATLAS_VAAL_C16

### Венец Порчи

**Свойства**

- 45% больше награда Ваал-зон · [Атлас: награда Ваал-зон](Modifiers-ATLAS.md#atlas_vaal_reward)
- +2 к минимуму модификаторов Ваал-зоны · [Атлас: минимум модификаторов Ваал-зоны](Modifiers-ATLAS.md#atlas_vaal_min_mods)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Порча](Atlas-SMALL.md#atlas_vaal_c01); [Порча](Atlas-SMALL.md#atlas_vaal_c04) |


## ATLAS_CRYSTALS_C02

### Сердце Призмы

**Свойства**

- 40% шанс эссенции уровнем выше · [Атлас: эссенции выше](Modifiers-ATLAS.md#atlas_crystal_tier)
- 40% шанс лишней эссенции в кристалле · [Атлас: лишняя эссенция](Modifiers-ATLAS.md#atlas_crystal_essences)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Призма](Atlas-SMALL.md#atlas_crystals_c01) |


## ATLAS_CRYSTALS_C08

### Венец Призмы

**Свойства**

- 100% больше кристаллов эссенций · [Атлас: больше кристаллов](Modifiers-ATLAS.md#atlas_crystals_more)
- 20% шанс лишней эссенции в кристалле · [Атлас: лишняя эссенция](Modifiers-ATLAS.md#atlas_crystal_essences)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Призма](Atlas-SMALL.md#atlas_crystals_c03) |


## ATLAS_EXPEDITION_C01

### Сердце Фонаря

**Свойства**

- +2 источника на картах · [Атлас: источники на картах](Modifiers-ATLAS.md#atlas_fountains)
- Герой на картах: 30% больше длительность фляг · [Атлас: длительность фляг](Modifiers-ATLAS.md#atlas_flask_duration)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Венец Фонаря](Atlas-KEYSTONE.md#atlas_expedition_c15) |


## ATLAS_EXPEDITION_C15

### Венец Фонаря

**Свойства**

- +2 сундука на картах · [Атлас: сундуки на картах](Modifiers-ATLAS.md#atlas_chests)
- 40% больше добычи из сундуков · [Атлас: добыча из сундуков](Modifiers-ATLAS.md#atlas_chest_loot)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Бета Фонаря](Atlas-NOTABLE.md#atlas_expedition_c13) |


## ATLAS_CRAFT_C02

### Сердце Горна

**Свойства**

- 15% шанс скрытой строки на добыче волшебных и редких монстров · [Атлас: скрытые строки](Modifiers-ATLAS.md#atlas_veiled)
- 50% больше сфер качества на картах · [Атлас: сферы качества](Modifiers-ATLAS.md#atlas_quality_orbs)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Горн](Atlas-SMALL.md#atlas_craft_c14) |


## ATLAS_CRAFT_C10

### Венец Горна

**Свойства**

- 100% больше знамений на картах · [Атлас: знамения](Modifiers-ATLAS.md#atlas_omens)
- 50% больше катализаторов на картах · [Атлас: катализаторы](Modifiers-ATLAS.md#atlas_catalysts)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Сердце Горна](Atlas-KEYSTONE.md#atlas_craft_c02) |


## ATLAS_POWER_C07

### Сердце Титана

**Свойства**

- 25% больше монстров на картах · [Атлас: число монстров](Modifiers-ATLAS.md#atlas_pack_size)
- Редкие монстры: +1 модификатора · [Атлас: модификаторы редких монстров](Modifiers-ATLAS.md#atlas_monster_mods)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Венец Титана](Atlas-KEYSTONE.md#atlas_power_c08) |


## ATLAS_POWER_C08

### Венец Титана

**Свойства**

- Монстры на картах: 30% больше здоровья (награда за риск) · [Атлас: здоровье монстров](Modifiers-ATLAS.md#atlas_monster_life)
- Монстры на картах: 20% больше урона (награда за риск) · [Атлас: урон монстров](Modifiers-ATLAS.md#atlas_monster_damage)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Титан](Atlas-SMALL.md#atlas_power_c17) |


## ATLAS_INFLUENCE_C01

### Сердце Двуликого

**Свойства**

- Случайно захваченные карты - только Древним · [Атлас: только Древний](Modifiers-ATLAS.md#atlas_influence_elder)
- +6% к шансу, что выпавшая карта захвачена влиянием · [Атлас: захваченные карты](Modifiers-ATLAS.md#atlas_map_influence)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Бета Двуликого](Atlas-NOTABLE.md#atlas_influence_c09) |


## ATLAS_INFLUENCE_C17

### Венец Двуликого

**Свойства**

- Случайно захваченные карты - только Создателем · [Атлас: только Создатель](Modifiers-ATLAS.md#atlas_influence_shaper)
- +6% к шансу, что выпавшая карта захвачена влиянием · [Атлас: захваченные карты](Modifiers-ATLAS.md#atlas_map_influence)


| Параметр | Значение |
| --- | --- |
| Вид · `kind` | `KEYSTONE` |
| Предшествующие узлы · `parents` | [Двуликий](Atlas-SMALL.md#atlas_influence_c11) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/atlas.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
