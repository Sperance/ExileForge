# Модификаторы · IMPLICIT

Тир 1 — верхний. Уровень — порог **уровня предмета**, не героя. Вес — относительный вес тира среди разрешённых; он не является процентом выпадения предмета.

## BASE_ARMOUR


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit` |
| Локальный эффект · `local` | Да |


- +{v} к броне · [Броня](Stats-HERO.md#stock_armor)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к броне · [Броня](Stats-HERO.md#stock_armor) |


## BASE_EVASION


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit` |
| Локальный эффект · `local` | Да |


- +{v} к уклонению · [Уклонение](Stats-HERO.md#stock_evasion)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к уклонению · [Уклонение](Stats-HERO.md#stock_evasion) |


## BASE_ENERGY_SHIELD


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit` |
| Локальный эффект · `local` | Да |


- +{v} к максимуму энергетического щита · [Энергетический щит](Stats-HERO.md#stock_energy_shield)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к максимуму энергетического щита · [Энергетический щит](Stats-HERO.md#stock_energy_shield) |


## BASE_PHYSICAL_DAMAGE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit` |
| Локальный эффект · `local` | Да |


- +{v} к физическому урону · [Физический урон](Stats-HERO.md#stock_attack_physical)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к физическому урону · [Физический урон](Stats-HERO.md#stock_attack_physical) |


## BASE_ATTACK_SPEED


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit` |
| Локальный эффект · `local` | Да |


- {v} атак в секунду · [Скорость атаки](Stats-HERO.md#stock_attack_speed)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1 атак в секунду · [Скорость атаки](Stats-HERO.md#stock_attack_speed) |


## BASE_BLOCK_CHANCE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit` |
| Локальный эффект · `local` | Да |


- +{v}% к шансу блока · [Шанс блока](Stats-HERO.md#stock_block_chance)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к шансу блока · [Шанс блока](Stats-HERO.md#stock_block_chance) |


## IMPLICIT_ADD_MAXIMUM_MANA


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `implicit`; `mana` |


- +{v} к максимуму маны · [Мана](Stats-HERO.md#stock_mana)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 45 | 518 | +30–40 к максимуму маны · [Мана](Stats-HERO.md#stock_mana) |
| 2 | 20 | 720 | +20–28 к максимуму маны · [Мана](Stats-HERO.md#stock_mana) |
| 3 | 1 | 1000 | +10–15 к максимуму маны · [Мана](Stats-HERO.md#stock_mana) |


## IMPLICIT_INCREASED_SPELL_DAMAGE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `implicit`; `caster` |


- {v}% увеличение урона чар · [Урон чар](Stats-HERO.md#stock_spell_damage)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 45 | 518 | 20–25% увеличение урона чар · [Урон чар](Stats-HERO.md#stock_spell_damage) |
| 2 | 20 | 720 | 14–18% увеличение урона чар · [Урон чар](Stats-HERO.md#stock_spell_damage) |
| 3 | 1 | 1000 | 8–12% увеличение урона чар · [Урон чар](Stats-HERO.md#stock_spell_damage) |


## IMPLICIT_STAFF_SPELL_DAMAGE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `implicit`; `caster` |


- {v}% увеличение урона чар · [Урон чар](Stats-HERO.md#stock_spell_damage)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 45 | 518 | 36–45% увеличение урона чар · [Урон чар](Stats-HERO.md#stock_spell_damage) |
| 2 | 20 | 720 | 25–34% увеличение урона чар · [Урон чар](Stats-HERO.md#stock_spell_damage) |
| 3 | 1 | 1000 | 14–22% увеличение урона чар · [Урон чар](Stats-HERO.md#stock_spell_damage) |


## IMPLICIT_INCREASED_ELEMENTAL_DAMAGE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `implicit`; `damage`; `elemental` |


- {v}% увеличение урона от стихий · [Урон от стихий](Stats-HERO.md#stock_elemental_damage)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 45 | 518 | 20–25% увеличение урона от стихий · [Урон от стихий](Stats-HERO.md#stock_elemental_damage) |
| 2 | 20 | 720 | 14–18% увеличение урона от стихий · [Урон от стихий](Stats-HERO.md#stock_elemental_damage) |
| 3 | 1 | 1000 | 8–12% увеличение урона от стихий · [Урон от стихий](Stats-HERO.md#stock_elemental_damage) |


## BASE_FLASK_CHARGES


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- +{v} к максимуму зарядов · [Заряды](Stats-FLASK.md#flask_charges)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 к максимуму зарядов · [Заряды](Stats-FLASK.md#flask_charges) |


## BASE_FLASK_CHARGES_PER_USE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- Тратит {v} зарядов за глоток · [Зарядов за глоток](Stats-FLASK.md#flask_charges_per_use)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Тратит 1 зарядов за глоток · [Зарядов за глоток](Stats-FLASK.md#flask_charges_per_use) |


## BASE_FLASK_DURATION


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- +{v} с к длительности действия · [Длительность](Stats-FLASK.md#flask_duration)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1 с к длительности действия · [Длительность](Stats-FLASK.md#flask_duration) |


## BASE_FLASK_LIFE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- Восстанавливает {v} здоровья · [Восстанавливает здоровья](Stats-FLASK.md#flask_life)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Восстанавливает 1 здоровья · [Восстанавливает здоровья](Stats-FLASK.md#flask_life) |


## BASE_FLASK_MANA


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- Восстанавливает {v} маны · [Восстанавливает маны](Stats-FLASK.md#flask_mana)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | Восстанавливает 1 маны · [Восстанавливает маны](Stats-FLASK.md#flask_mana) |


## BASE_FLASK_MOVEMENT


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- {v}% увеличение скорости передвижения · [Скорость передвижения](Stats-HERO.md#stock_movement_speed)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение скорости передвижения · [Скорость передвижения](Stats-HERO.md#stock_movement_speed) |


## BASE_FLASK_ARMOUR


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- {v}% увеличение брони · [Броня](Stats-HERO.md#stock_armor)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение брони · [Броня](Stats-HERO.md#stock_armor) |


## BASE_FLASK_EVASION


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- {v}% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение уклонения · [Уклонение](Stats-HERO.md#stock_evasion) |


## BASE_FLASK_ATTACK_SPEED


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- {v}% увеличение скорости атаки · [Скорость атаки](Stats-HERO.md#stock_attack_speed)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение скорости атаки · [Скорость атаки](Stats-HERO.md#stock_attack_speed) |


## BASE_FLASK_FIRE_RESISTANCE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- +{v}% к сопротивлению огню · [Сопротивление огню](Stats-HERO.md#stock_resist_fire)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к сопротивлению огню · [Сопротивление огню](Stats-HERO.md#stock_resist_fire) |


## BASE_FLASK_COLD_RESISTANCE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- +{v}% к сопротивлению холоду · [Сопротивление холоду](Stats-HERO.md#stock_resist_cold)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к сопротивлению холоду · [Сопротивление холоду](Stats-HERO.md#stock_resist_cold) |


## BASE_FLASK_LIGHTNING_RESISTANCE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- +{v}% к сопротивлению молнии · [Сопротивление молнии](Stats-HERO.md#stock_resist_lightning)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к сопротивлению молнии · [Сопротивление молнии](Stats-HERO.md#stock_resist_lightning) |


## BASE_FLASK_CHAOS_RESISTANCE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- +{v}% к сопротивлению хаосу · [Сопротивление хаосу](Stats-HERO.md#stock_resist_chaos)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к сопротивлению хаосу · [Сопротивление хаосу](Stats-HERO.md#stock_resist_chaos) |


## BASE_FLASK_MAXIMUM_FIRE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- +{v}% к максимуму сопротивления огню · [Максимум сопротивления огню](Stats-HERO.md#stock_resist_max_fire)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к максимуму сопротивления огню · [Максимум сопротивления огню](Stats-HERO.md#stock_resist_max_fire) |


## BASE_FLASK_MAXIMUM_COLD


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- +{v}% к максимуму сопротивления холоду · [Максимум сопротивления холоду](Stats-HERO.md#stock_resist_max_cold)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к максимуму сопротивления холоду · [Максимум сопротивления холоду](Stats-HERO.md#stock_resist_max_cold) |


## BASE_FLASK_MAXIMUM_LIGHTNING


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- +{v}% к максимуму сопротивления молнии · [Максимум сопротивления молнии](Stats-HERO.md#stock_resist_max_lightning)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к максимуму сопротивления молнии · [Максимум сопротивления молнии](Stats-HERO.md#stock_resist_max_lightning) |


## BASE_FLASK_MAXIMUM_CHAOS


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- +{v}% к максимуму сопротивления хаосу · [Максимум сопротивления хаосу](Stats-HERO.md#stock_resist_max_chaos)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к максимуму сопротивления хаосу · [Максимум сопротивления хаосу](Stats-HERO.md#stock_resist_max_chaos) |


## BASE_FLASK_PHYSICAL_REDUCTION


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- {v}% дополнительного снижения физического урона · [Снижение физического урона](Stats-HERO.md#stock_physical_reduction)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% дополнительного снижения физического урона · [Снижение физического урона](Stats-HERO.md#stock_physical_reduction) |


## BASE_FLASK_STUN_THRESHOLD


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- {v}% увеличение порога оглушения · [Порог оглушения](Stats-HERO.md#stock_stun_threshold)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение порога оглушения · [Порог оглушения](Stats-HERO.md#stock_stun_threshold) |


## BASE_FLASK_CRITICAL


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- {v}% увеличение шанса критического удара · [Шанс критического удара](Stats-HERO.md#stock_critical_chance)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение шанса критического удара · [Шанс критического удара](Stats-HERO.md#stock_critical_chance) |


## BASE_FLASK_BLOCK


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- +{v}% к шансу блока · [Шанс блока](Stats-HERO.md#stock_block_chance)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | +1% к шансу блока · [Шанс блока](Stats-HERO.md#stock_block_chance) |


## BASE_FLASK_DAMAGE


| Параметр | Значение |
| --- | --- |
| Источник · `source` | `IMPLICIT` |
| Теги · `tags` | `base`; `implicit`; [flask](Tables-MODIFIER.md#flask) |
| Локальный эффект · `local` | Да |


- {v}% увеличение урона · [Урон](Stats-HERO.md#stock_damage)

### NATURAL


| Тир | Мин. ilvl | Вес | Диапазоны эффектов |
| --- | --- | --- | --- |
| 1 | 1 | 1000 | 1% увеличение урона · [Урон](Stats-HERO.md#stock_damage) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/modifiers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
