# Модификаторы · PASSIVE

Тир 1 — верхний. Уровень — порог **уровня предмета**, не героя. Вес — относительный вес тира среди разрешённых; он не является процентом выпадения предмета.

## PASSIVE_INCREASED_ARMOUR_AND_EVASION


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | [armour](Tables-MODIFIER.md#armour); `evasion`; `defences`; `passive` |


- {v}% увеличение брони · [Броня](Stats-HERO.md#stock_armor)
- {v}% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение брони · [Броня](Stats-HERO.md#stock_armor); 1% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion) |


## PASSIVE_INCREASED_EVASION_AND_ENERGY_SHIELD


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `evasion`; `energy_shield`; `defences`; `passive` |


- {v}% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion)
- {v}% увеличение энергетического щита · [Энергетический щит](Stats-HERO.md#stock_energy_shield)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion); 1% увеличение энергетического щита · [Энергетический щит](Stats-HERO.md#stock_energy_shield) |


## INCREASED_STUN_THRESHOLD


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `stun`; `passive` |


- {v}% увеличение порога оглушения · [Порог оглушения](Stats-HERO.md#stock_stun_threshold)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение порога оглушения · [Порог оглушения](Stats-HERO.md#stock_stun_threshold) |


## PASSIVE_ADD_MAXIMUM_BLOCK


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `block`; `passive` |


- +{v}% к максимуму шанса блока · [Максимум блока](Stats-HERO.md#stock_block_max)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к максимуму шанса блока · [Максимум блока](Stats-HERO.md#stock_block_max) |


## PASSIVE_ADD_MAXIMUM_EVASION


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `evasion`; `passive` |


- +{v}% к максимуму шанса уклонения · [Максимум уклонения](Stats-HERO.md#stock_evasion_max)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к максимуму шанса уклонения · [Максимум уклонения](Stats-HERO.md#stock_evasion_max) |


## PASSIVE_ADD_MAXIMUM_PHYSICAL_REDUCTION


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `physical`; `passive` |


- +{v}% к максимуму снижения физического урона · [Максимум физического снижения](Stats-HERO.md#stock_physical_reduction_max)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к максимуму снижения физического урона · [Максимум физического снижения](Stats-HERO.md#stock_physical_reduction_max) |


## PASSIVE_ADD_MAXIMUM_CRITICAL_CHANCE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `critical`; `passive` |


- +{v}% к максимуму шанса критического удара · [Максимум шанса крита](Stats-HERO.md#stock_critical_max)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к максимуму шанса критического удара · [Максимум шанса крита](Stats-HERO.md#stock_critical_max) |


## PASSIVE_REDUCE_MAXIMUM_BLOCK


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `block`; `passive` |


- +{v}% к максимуму шанса блока · [Максимум блока](Stats-HERO.md#stock_block_max)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +-1% к максимуму шанса блока · [Максимум блока](Stats-HERO.md#stock_block_max) |


## PASSIVE_REDUCE_MAXIMUM_EVASION


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `evasion`; `passive` |


- +{v}% к максимуму шанса уклонения · [Максимум уклонения](Stats-HERO.md#stock_evasion_max)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +-1% к максимуму шанса уклонения · [Максимум уклонения](Stats-HERO.md#stock_evasion_max) |


## PASSIVE_ADD_STRENGTH_AND_DEXTERITY


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `attribute`; `strength`; `dexterity`; `passive` |


- +{v} к силе · [Сила](Stats-HERO.md#stock_strength)
- +{v} к ловкости · [Ловкость](Stats-HERO.md#stock_agility)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к силе · [Сила](Stats-HERO.md#stock_strength); +1 к ловкости · [Ловкость](Stats-HERO.md#stock_agility) |


## PASSIVE_ADD_STRENGTH_AND_INTELLIGENCE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `attribute`; `strength`; `intelligence`; `passive` |


- +{v} к силе · [Сила](Stats-HERO.md#stock_strength)
- +{v} к интеллекту · [Интеллект](Stats-HERO.md#stock_intellect)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к силе · [Сила](Stats-HERO.md#stock_strength); +1 к интеллекту · [Интеллект](Stats-HERO.md#stock_intellect) |


## PASSIVE_ADD_DEXTERITY_AND_INTELLIGENCE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `attribute`; `dexterity`; `intelligence`; `passive` |


- +{v} к ловкости · [Ловкость](Stats-HERO.md#stock_agility)
- +{v} к интеллекту · [Интеллект](Stats-HERO.md#stock_intellect)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к ловкости · [Ловкость](Stats-HERO.md#stock_agility); +1 к интеллекту · [Интеллект](Stats-HERO.md#stock_intellect) |


## PASSIVE_INCREASED_ENERGY_REGENERATION


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `regen`; `energy_shield`; `passive` |


- {v}% увеличение скорости восстановления энергетического щита · [Восстановление энергии](Stats-HERO.md#stock_energy_regen)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение скорости восстановления энергетического щита · [Восстановление энергии](Stats-HERO.md#stock_energy_regen) |


## CONVERT_STRENGTH_TO_LIFE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive`; `conversion` |


- +{v} к максимуму здоровья за каждые 2 Сила · [Здоровье](Stats-HERO.md#stock_health)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к максимуму здоровья за каждые 2 Сила · [Здоровье](Stats-HERO.md#stock_health) |


## CONVERT_DEXTERITY_TO_EVASION


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive`; `conversion` |


- +{v} к уклонению за каждые 1 Ловкость · [Уклонение](Stats-HERO.md#stock_evasion)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к уклонению за каждые 1 Ловкость · [Уклонение](Stats-HERO.md#stock_evasion) |


## CONVERT_INTELLIGENCE_TO_ENERGY_SHIELD


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive`; `conversion` |


- {v}% увеличение энергетического щита за каждые 5 Интеллект · [Энергетический щит](Stats-HERO.md#stock_energy_shield)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение энергетического щита за каждые 5 Интеллект · [Энергетический щит](Stats-HERO.md#stock_energy_shield) |


## CONVERT_INTELLIGENCE_TO_MANA


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive`; `conversion` |


- +{v} к максимуму маны за каждые 2 Интеллект · [Мана](Stats-HERO.md#stock_mana)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к максимуму маны за каждые 2 Интеллект · [Мана](Stats-HERO.md#stock_mana) |


## CONVERT_STRENGTH_TO_PHYSICAL_DAMAGE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive`; `conversion` |


- {v}% увеличение физического урона за каждые 5 Сила · [Физический урон](Stats-HERO.md#stock_attack_physical)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение физического урона за каждые 5 Сила · [Физический урон](Stats-HERO.md#stock_attack_physical) |


## CONVERT_INTELLIGENCE_TO_LIFE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive`; `conversion` |


- +{v} к максимуму здоровья за каждые 2 Интеллект · [Здоровье](Stats-HERO.md#stock_health)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к максимуму здоровья за каждые 2 Интеллект · [Здоровье](Stats-HERO.md#stock_health) |


## PASSIVE_SET_CRITICAL_STRIKE_CHANCE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- Шанс критического удара: {v} · [Шанс критического удара](Stats-HERO.md#stock_critical_chance)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Шанс критического удара: 1 · [Шанс критического удара](Stats-HERO.md#stock_critical_chance) |


## PASSIVE_SET_MAXIMUM_LIFE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- Максимум здоровья: {v} · [Здоровье](Stats-HERO.md#stock_health)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Максимум здоровья: 1 · [Здоровье](Stats-HERO.md#stock_health) |


## PASSIVE_SET_EVASION_RATING


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- Уклонение: {v} · [Уклонение](Stats-HERO.md#stock_evasion)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Уклонение: 1 · [Уклонение](Stats-HERO.md#stock_evasion) |


## PASSIVE_SET_ARMOUR


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- Броня: {v} · [Броня](Stats-HERO.md#stock_armor)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Броня: 1 · [Броня](Stats-HERO.md#stock_armor) |


## PASSIVE_SET_CHAOS_RESISTANCE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- Сопротивление хаосу: {v} · [Сопротивление хаосу](Stats-HERO.md#stock_resist_chaos)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Сопротивление хаосу: 1 · [Сопротивление хаосу](Stats-HERO.md#stock_resist_chaos) |


## PASSIVE_CHAOS_IMMUNITY


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `chaos`; `passive` |


- Иммунитет к урону хаосом · [Иммунитет к хаосу](Stats-HERO.md#stock_chaos_immune)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Иммунитет к урону хаосом · [Иммунитет к хаосу](Stats-HERO.md#stock_chaos_immune) |


## PASSIVE_SET_LIFE_REGENERATION


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- Восстановление здоровья: {v} · [Восстановление здоровья](Stats-HERO.md#stock_health_regen)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Восстановление здоровья: 1 · [Восстановление здоровья](Stats-HERO.md#stock_health_regen) |


## PASSIVE_SET_CRITICAL_DAMAGE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- Урон критических ударов: {v} · [Урон критического удара](Stats-HERO.md#stock_critical_damage)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Урон критических ударов: 1 · [Урон критического удара](Stats-HERO.md#stock_critical_damage) |


## PASSIVE_MORE_ARMOUR


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- {v}% больше брони · [Броня](Stats-HERO.md#stock_armor)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% больше брони · [Броня](Stats-HERO.md#stock_armor) |


## PASSIVE_MORE_EVASION_RATING


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- {v}% больше уклонения · [Уклонение](Stats-HERO.md#stock_evasion)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% больше уклонения · [Уклонение](Stats-HERO.md#stock_evasion) |


## PASSIVE_MORE_MAXIMUM_LIFE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- {v}% больше максимума здоровья · [Здоровье](Stats-HERO.md#stock_health)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% больше максимума здоровья · [Здоровье](Stats-HERO.md#stock_health) |


## PASSIVE_MORE_PHYSICAL_DAMAGE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- {v}% больше физического урона · [Физический урон](Stats-HERO.md#stock_attack_physical)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% больше физического урона · [Физический урон](Stats-HERO.md#stock_attack_physical) |


## PASSIVE_MORE_STUN_THRESHOLD


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- {v}% больше порога оглушения · [Порог оглушения](Stats-HERO.md#stock_stun_threshold)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% больше порога оглушения · [Порог оглушения](Stats-HERO.md#stock_stun_threshold) |


## PASSIVE_MORE_LIFE_LEECH


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- {v}% больше кражи здоровья · [Физический вампиризм](Stats-HERO.md#stock_leech_physical)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% больше кражи здоровья · [Физический вампиризм](Stats-HERO.md#stock_leech_physical) |


## PASSIVE_MORE_ELEMENTAL_DAMAGE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- {v}% больше урона от огня · [Урон огнём](Stats-HERO.md#stock_attack_fire)
- {v}% больше урона от холода · [Урон холодом](Stats-HERO.md#stock_attack_cold)
- {v}% больше урона от молнии · [Урон молнией](Stats-HERO.md#stock_attack_lightning)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% больше урона от огня · [Урон огнём](Stats-HERO.md#stock_attack_fire); 1% больше урона от холода · [Урон холодом](Stats-HERO.md#stock_attack_cold); 1% больше урона от молнии · [Урон молнией](Stats-HERO.md#stock_attack_lightning) |


## PASSIVE_MORE_DAMAGE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `damage`; `passive` |


- Наносит на {v}% больше урона · [Урон](Stats-HERO.md#stock_damage)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Наносит на 1% больше урона · [Урон](Stats-HERO.md#stock_damage) |


## PASSIVE_LESS_PHYSICAL_DAMAGE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `damage`; `physical`; `passive` |


- {v}% больше физического урона · [Физический урон](Stats-HERO.md#stock_attack_physical)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | -1% больше физического урона · [Физический урон](Stats-HERO.md#stock_attack_physical) |


## PASSIVE_LESS_MAXIMUM_MANA


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `mana`; `passive` |


- Мана · MORE · {v} · [Мана](Stats-HERO.md#stock_mana)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Мана · MORE · -1 · [Мана](Stats-HERO.md#stock_mana) |


## FRENZY_CHARGE_ON_HIT


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `charge`; `passive` |


- {v}% шанс получить заряд ярости при попадании (откат 0,5 с) · [Яростные удары](Stats-POWER.md#power_frenzy_on_hit)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% шанс получить заряд ярости при попадании (откат 0,5 с) · [Яростные удары](Stats-POWER.md#power_frenzy_on_hit) |


## POWER_CHARGE_ON_CRIT


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `charge`; `passive` |


- {v}% шанс получить заряд силы при критическом ударе · [Сила крита](Stats-POWER.md#power_power_on_crit)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% шанс получить заряд силы при критическом ударе · [Сила крита](Stats-POWER.md#power_power_on_crit) |


## ENDURANCE_CHARGE_WHEN_HIT


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `charge`; `passive` |


- {v}% шанс получить заряд выносливости, когда вас бьют · [Закалка ударами](Stats-POWER.md#power_endurance_when_hit)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% шанс получить заряд выносливости, когда вас бьют · [Закалка ударами](Stats-POWER.md#power_endurance_when_hit) |


## PASSIVE_SET_SPELL_CRITICAL_STRIKE_CHANCE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- Шанс критического удара заклинаний: {v} · [Шанс крита заклинаний](Stats-HERO.md#stock_spell_critical_chance)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Шанс критического удара заклинаний: 1 · [Шанс крита заклинаний](Stats-HERO.md#stock_spell_critical_chance) |


## PASSIVE_POWER_ARCANE_TIDE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Каждые 6 с восстанавливает {v}% маны · [Волшебный прилив](Stats-POWER.md#power_arcane_tide)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Каждые 6 с восстанавливает 1% маны · [Волшебный прилив](Stats-POWER.md#power_arcane_tide) |


## PASSIVE_POWER_BLACK_SAP


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Убийства дают {v}% физического урона как доп. хаос на 4 с · [Чёрный сок](Stats-POWER.md#power_black_sap)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Убийства дают 1% физического урона как доп. хаос на 4 с · [Чёрный сок](Stats-POWER.md#power_black_sap) |


## PASSIVE_POWER_BLADE_DANCE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- +{v}% отклонения за каждые 500 уклонения, до 30%; блока нет · [Танец клинков](Stats-POWER.md#power_blade_dance)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% отклонения за каждые 500 уклонения, до 30%; блока нет · [Танец клинков](Stats-POWER.md#power_blade_dance) |


## PASSIVE_POWER_BLOOD_HEX


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Удары по вам с шансом {v}% проклинают атакующего · [Кровавый сглаз](Stats-POWER.md#power_blood_hex)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Удары по вам с шансом 1% проклинают атакующего · [Кровавый сглаз](Stats-POWER.md#power_blood_hex) |


## PASSIVE_POWER_CANTORS_BREATH


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Низкое здоровье даёт {v}% увеличения скорости сотворения на 4 с · [Дыхание кантора](Stats-POWER.md#power_cantors_breath)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Низкое здоровье даёт 1% увеличения скорости сотворения на 4 с · [Дыхание кантора](Stats-POWER.md#power_cantors_breath) |


## PASSIVE_POWER_COLD_CALCULUS


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- +{v}% шанса двойного урона за каждые 200 меткости, до 40% · [Холодный расчёт](Stats-POWER.md#power_cold_calculus)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% шанса двойного урона за каждые 200 меткости, до 40% · [Холодный расчёт](Stats-POWER.md#power_cold_calculus) |


## PASSIVE_POWER_CRYSTAL_SIGHT


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Удар по шокированному врагу даёт {v}% шанса крита на 3 с · [Кристальный взор](Stats-POWER.md#power_crystal_sight)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Удар по шокированному врагу даёт 1% шанса крита на 3 с · [Кристальный взор](Stats-POWER.md#power_crystal_sight) |


## PASSIVE_POWER_DEEP_FREEZE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Каждый 3-й удар с шансом {v}% замораживает · [Глубокий холод](Stats-POWER.md#power_deep_freeze)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Каждый 3-й удар с шансом 1% замораживает · [Глубокий холод](Stats-POWER.md#power_deep_freeze) |


## PASSIVE_POWER_ENDURING_BLOCK


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- {v}% шанс получить заряд выносливости при блоке · [Стойкий блок](Stats-POWER.md#power_enduring_block)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% шанс получить заряд выносливости при блоке · [Стойкий блок](Stats-POWER.md#power_enduring_block) |


## PASSIVE_POWER_FLARE_UP


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Шок врага даёт {v}% усиления шока на 4 с · [Вспышка](Stats-POWER.md#power_flare_up)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Шок врага даёт 1% усиления шока на 4 с · [Вспышка](Stats-POWER.md#power_flare_up) |


## PASSIVE_POWER_FURNACE_CORE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Поджог врага даёт {v}% физ. урона как доп. огонь на 4 с · [Сердце горна](Stats-POWER.md#power_furnace_core)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Поджог врага даёт 1% физ. урона как доп. огонь на 4 с · [Сердце горна](Stats-POWER.md#power_furnace_core) |


## PASSIVE_POWER_GRANITE_HIDE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- {v}% к урону от ударов при полном здоровье · [Гранитная шкура](Stats-POWER.md#power_granite_hide)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% к урону от ударов при полном здоровье · [Гранитная шкура](Stats-POWER.md#power_granite_hide) |


## PASSIVE_POWER_GUARD_DOG


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Удар по вам лечит питомца на {v}% · [Сторожевой пёс](Stats-POWER.md#power_guard_dog)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Удар по вам лечит питомца на 1% · [Сторожевой пёс](Stats-POWER.md#power_guard_dog) |


## PASSIVE_POWER_HEADSMAN


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Удары добивают редких врагов ниже {v}% здоровья · [Плата палача](Stats-POWER.md#power_headsman)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Удары добивают редких врагов ниже 1% здоровья · [Плата палача](Stats-POWER.md#power_headsman) |


## PASSIVE_POWER_IRON_BLOOD


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- +{v}% возврата здоровья за каждые 1000 брони, до 25% · [Железная кровь](Stats-POWER.md#power_iron_blood)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% возврата здоровья за каждые 1000 брони, до 25% · [Железная кровь](Stats-POWER.md#power_iron_blood) |


## PASSIVE_POWER_KILLING_FEVER


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- {v}% шанс получить заряд ярости при убийстве · [Горячка убийства](Stats-POWER.md#power_killing_fever)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% шанс получить заряд ярости при убийстве · [Горячка убийства](Stats-POWER.md#power_killing_fever) |


## PASSIVE_POWER_LONE_QUARRY


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- +{v} к меткости, пока стоит один враг · [Одинокая добыча](Stats-POWER.md#power_lone_quarry)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к меткости, пока стоит один враг · [Одинокая добыча](Stats-POWER.md#power_lone_quarry) |


## PASSIVE_POWER_MARKSMANS_FOCUS


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- {v}% увеличение меткости в первые 6 с боя · [Сосредоточение стрелка](Stats-POWER.md#power_marksmans_focus)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение меткости в первые 6 с боя · [Сосредоточение стрелка](Stats-POWER.md#power_marksmans_focus) |


## PASSIVE_POWER_MOURNING_BELL


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Удары по врагам ниже 30% здоровья наносят на {v}% больше урона оружия · [Погребальный колокол](Stats-POWER.md#power_mourning_bell)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Удары по врагам ниже 30% здоровья наносят на 1% больше урона оружия · [Погребальный колокол](Stats-POWER.md#power_mourning_bell) |


## PASSIVE_POWER_PACK_RALLY


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Удар питомца даёт вам {v}% увеличения урона на 3 с, до 5 раз · [Сбор стаи](Stats-POWER.md#power_pack_rally)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Удар питомца даёт вам 1% увеличения урона на 3 с, до 5 раз · [Сбор стаи](Stats-POWER.md#power_pack_rally) |


## PASSIVE_POWER_PRISM_CLEAVE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Криты наносят {v}% урона оружия холодом другому врагу · [Призменный разруб](Stats-POWER.md#power_prism_cleave)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Криты наносят 1% урона оружия холодом другому врагу · [Призменный разруб](Stats-POWER.md#power_prism_cleave) |


## PASSIVE_POWER_QUICK_ROT


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Отравление врага ускоряет урон состояний на {v}% на 4 с · [Быстрая гниль](Stats-POWER.md#power_quick_rot)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Отравление врага ускоряет урон состояний на 1% на 4 с · [Быстрая гниль](Stats-POWER.md#power_quick_rot) |


## PASSIVE_POWER_RAIDERS_TOLL


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Каждый 2-й удар с шансом {v}% вызывает кровотечение · [Дань налётчика](Stats-POWER.md#power_raiders_toll)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Каждый 2-й удар с шансом 1% вызывает кровотечение · [Дань налётчика](Stats-POWER.md#power_raiders_toll) |


## PASSIVE_POWER_RECOUP_WARD


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Когда щит пробит, возвращается {v}% его · [Отскок щита](Stats-POWER.md#power_recoup_ward)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Когда щит пробит, возвращается 1% его · [Отскок щита](Stats-POWER.md#power_recoup_ward) |


## PASSIVE_POWER_SHARED_SOUL


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Питомец получает {v}% вашего максимума здоровья; максимум здоровья на 15% меньше · [Общая душа](Stats-POWER.md#power_shared_soul)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Питомец получает 1% вашего максимума здоровья; максимум здоровья на 15% меньше · [Общая душа](Stats-POWER.md#power_shared_soul) |


## PASSIVE_POWER_SHROUDED_STEP


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Уклонение даёт {v}% увеличения уклонения на 3 с, до 3 раз · [Сокрытый шаг](Stats-POWER.md#power_shrouded_step)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Уклонение даёт 1% увеличения уклонения на 3 с, до 3 раз · [Сокрытый шаг](Stats-POWER.md#power_shrouded_step) |


## PASSIVE_POWER_SHRUG_OFF


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Критический удар по вам даёт барьер в 15% здоровья на {v} с · [Отмахнуться](Stats-POWER.md#power_shrug_off)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Критический удар по вам даёт барьер в 15% здоровья на 1 с · [Отмахнуться](Stats-POWER.md#power_shrug_off) |


## PASSIVE_POWER_SKYFORGE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- {v}% физического урона атак обращается в молнию · [Небесная кузня](Stats-POWER.md#power_skyforge)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% физического урона атак обращается в молнию · [Небесная кузня](Stats-POWER.md#power_skyforge) |


## PASSIVE_POWER_SPELLSTORM


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Критические заклинания дают +{v} урона от молнии к заклинаниям на 4 с · [Буря чар](Stats-POWER.md#power_spellstorm)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Критические заклинания дают +1 урона от молнии к заклинаниям на 4 с · [Буря чар](Stats-POWER.md#power_spellstorm) |


## PASSIVE_POWER_STAR_HEART


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Каждые 5 с восстанавливает {v}% энергощита · [Звёздное сердце](Stats-POWER.md#power_star_heart)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Каждые 5 с восстанавливает 1% энергощита · [Звёздное сердце](Stats-POWER.md#power_star_heart) |


## PASSIVE_POWER_TEMPERED_SOUL


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Блок даёт {v}% увеличения брони на 4 с · [Закалённая душа](Stats-POWER.md#power_tempered_soul)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Блок даёт 1% увеличения брони на 4 с · [Закалённая душа](Stats-POWER.md#power_tempered_soul) |


## PASSIVE_POWER_TURNING_STEEL


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Блок даёт +{v}% к отклонению на 3 с, до 3 раз · [Отводящая сталь](Stats-POWER.md#power_turning_steel)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Блок даёт +1% к отклонению на 3 с, до 3 раз · [Отводящая сталь](Stats-POWER.md#power_turning_steel) |


## PASSIVE_POWER_TWIN_FANGS


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Удары с шансом {v}% повторяются через 0,3 с за половину урона · [Двойные клыки](Stats-POWER.md#power_twin_fangs)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Удары с шансом 1% повторяются через 0,3 с за половину урона · [Двойные клыки](Stats-POWER.md#power_twin_fangs) |


## PASSIVE_POWER_UNDERTOW_PULL


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- {v}% шанс получить заряд силы при крите · [Тяга течения](Stats-POWER.md#power_undertow_pull)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% шанс получить заряд силы при крите · [Тяга течения](Stats-POWER.md#power_undertow_pull) |


## PASSIVE_POWER_VAAL_DEBT


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Низкое здоровье восстанавливает {v}% здоровья, раз за бой · [Долг Ваал](Stats-POWER.md#power_vaal_debt)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Низкое здоровье восстанавливает 1% здоровья, раз за бой · [Долг Ваал](Stats-POWER.md#power_vaal_debt) |


## PASSIVE_POWER_VENOM_HEART


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- {v}% физического урона атак обращается в хаос · [Ядовитое сердце](Stats-POWER.md#power_venom_heart)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% физического урона атак обращается в хаос · [Ядовитое сердце](Stats-POWER.md#power_venom_heart) |


## PASSIVE_POWER_WARDING_MANTRA


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- +{v}% к подавлению чар на 5 с в начале боя · [Охранная мантра](Stats-POWER.md#power_warding_mantra)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к подавлению чар на 5 с в начале боя · [Охранная мантра](Stats-POWER.md#power_warding_mantra) |


## PASSIVE_POWER_WARDSTONE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- {v}% уклонения достаётся энергощитом; уклонения нет · [Камень-оберег](Stats-POWER.md#power_wardstone)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% уклонения достаётся энергощитом; уклонения нет · [Камень-оберег](Stats-POWER.md#power_wardstone) |


## PASSIVE_POWER_WHISPERED_DOOM


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- С шансом {v}% проклинает врага в начале боя · [Шёпот гибели](Stats-POWER.md#power_whispered_doom)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | С шансом 1% проклинает врага в начале боя · [Шёпот гибели](Stats-POWER.md#power_whispered_doom) |


## PASSIVE_POWER_WIND_AT_BACK


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `power`; `passive` |


- Убийства дают {v}% увеличения скорости атаки на 4 с, до 3 раз · [Попутный ветер](Stats-POWER.md#power_wind_at_back)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Убийства дают 1% увеличения скорости атаки на 4 с, до 3 раз · [Попутный ветер](Stats-POWER.md#power_wind_at_back) |


## PASSIVE_FORTIFY_ALWAYS


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- Укреплён · [Укреплён](Stats-HERO.md#stock_fortify_always)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Укреплён · [Укреплён](Stats-HERO.md#stock_fortify_always) |


## PASSIVE_ONSLAUGHT_ALWAYS


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- Под Натиском · [Натиск](Stats-HERO.md#stock_onslaught_always)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Под Натиском · [Натиск](Stats-HERO.md#stock_onslaught_always) |


## PASSIVE_ARCANE_SURGE_ALWAYS


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- Под Всплеском магии · [Всплеск магии](Stats-HERO.md#stock_arcane_surge_always)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Под Всплеском магии · [Всплеск магии](Stats-HERO.md#stock_arcane_surge_always) |


## PASSIVE_UNHOLY_MIGHT_ALWAYS


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `PASSIVE` |
| Теги · `tags` | `passive` |


- Под Нечестивой мощью · [Нечестивая мощь](Stats-HERO.md#stock_unholy_might_always)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Под Нечестивой мощью · [Нечестивая мощь](Stats-HERO.md#stock_unholy_might_always) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/modifiers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
