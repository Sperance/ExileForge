# Ошейники

[Правила экипировки](../Gear.md) · [Крафт](../Crafting.md) · [Все слоты](README.md)

Указаны свойства **шаблона**; конкретная копия получает собственный уровень предмета, качество, редкость и случайные роллы. Диапазоны уникальных свойств — минимальный и максимальный ролл.

| Предмет | Редкость | Уровень |
| --- | --- | --- |
| [Leather Collar](#leather_collar) | Обычный | 1 |
| [Studded Collar](#studded_collar) | Обычный | 15 |
| [Brass Collar](#brass_collar) | Обычный | 30 |
| [Runed Collar](#runed_collar) | Обычный | 45 |
| [Alpha's Collar](#alpha_collar) | Обычный | 60 |
| [Collar of the First Hound](#collar_of_the_first_hound) | Уникальный | 12 |
| [Packleader's Band](#packleaders_band) | Уникальный | 30 |
| [Ironjaw Collar](#ironjaw_collar) | Уникальный | 45 |
| [Beastsoul Choker](#beastsoul_choker) | Уникальный | 58 |
| [Wildmother's Garland](#wildmothers_garland) | Уникальный | 66 |
| [The Nest](#the_nest) | Уникальный | 35 |
| [Warm Scales](#warm_scales) | Уникальный | 50 |


## LEATHER_COLLAR <a href="#leather_collar" id="leather_collar"></a>

### Leather Collar <a href="#leather-collar" id="leather-collar"></a>

**Русское название:** Кожаный ошейник

**Обычный · Ошейники**


| Уровень базы | Уровень героя | Сила | Ловкость | Интеллект |
| --- | --- | --- | --- | --- |
| 1 | 1 | 0 | 0 | 0 |

**Встроенные модификаторы**

- `PET_INCREASED_LIFE@IMPLICIT`


| Параметр | Значение |
| --- | --- |
| Слот · `slot` | `COLLAR` |
| Таблицы · `tables` | [collar](../reference/Tables/Tables-MODIFIER.md#collar) |


## STUDDED_COLLAR <a href="#studded_collar" id="studded_collar"></a>

### Studded Collar <a href="#studded-collar" id="studded-collar"></a>

**Русское название:** Ошейник с шипами

**Обычный · Ошейники**


| Уровень базы | Уровень героя | Сила | Ловкость | Интеллект |
| --- | --- | --- | --- | --- |
| 15 | 15 | 0 | 0 | 0 |

**Встроенные модификаторы**

- `PET_ADD_ARMOUR@IMPLICIT`


| Параметр | Значение |
| --- | --- |
| Слот · `slot` | `COLLAR` |
| Таблицы · `tables` | [collar](../reference/Tables/Tables-MODIFIER.md#collar) |


## BRASS_COLLAR <a href="#brass_collar" id="brass_collar"></a>

### Brass Collar <a href="#brass-collar" id="brass-collar"></a>

**Русское название:** Латунный ошейник

**Обычный · Ошейники**


| Уровень базы | Уровень героя | Сила | Ловкость | Интеллект |
| --- | --- | --- | --- | --- |
| 30 | 30 | 0 | 0 | 0 |

**Встроенные модификаторы**

- `PET_ATTACK_SPEED@IMPLICIT`


| Параметр | Значение |
| --- | --- |
| Слот · `slot` | `COLLAR` |
| Таблицы · `tables` | [collar](../reference/Tables/Tables-MODIFIER.md#collar) |


## RUNED_COLLAR <a href="#runed_collar" id="runed_collar"></a>

### Runed Collar <a href="#runed-collar" id="runed-collar"></a>

**Русское название:** Рунный ошейник

**Обычный · Ошейники**


| Уровень базы | Уровень героя | Сила | Ловкость | Интеллект |
| --- | --- | --- | --- | --- |
| 45 | 45 | 0 | 0 | 0 |

**Встроенные модификаторы**

- `PET_RESISTANCES@IMPLICIT`


| Параметр | Значение |
| --- | --- |
| Слот · `slot` | `COLLAR` |
| Таблицы · `tables` | [collar](../reference/Tables/Tables-MODIFIER.md#collar) |


## ALPHA_COLLAR <a href="#alpha_collar" id="alpha_collar"></a>

### Alpha's Collar <a href="#alphas-collar" id="alphas-collar"></a>

**Русское название:** Ошейник вожака

**Обычный · Ошейники**


| Уровень базы | Уровень героя | Сила | Ловкость | Интеллект |
| --- | --- | --- | --- | --- |
| 60 | 60 | 0 | 0 | 0 |

**Встроенные модификаторы**

- `PET_INCREASED_DAMAGE@IMPLICIT`


| Параметр | Значение |
| --- | --- |
| Слот · `slot` | `COLLAR` |
| Таблицы · `tables` | [collar](../reference/Tables/Tables-MODIFIER.md#collar) |


## COLLAR_OF_THE_FIRST_HOUND <a href="#collar_of_the_first_hound" id="collar_of_the_first_hound"></a>

### Collar of the First Hound <a href="#collar-of-the-first-hound" id="collar-of-the-first-hound"></a>

Хороший пёс помнит, кто кормил его первым.

**Русское название:** Ошейник первого пса

**Уникальный · Ошейники**


| Уровень базы | Уровень героя | Сила | Ловкость | Интеллект |
| --- | --- | --- | --- | --- |
| 12 | 12 | 0 | 0 | 0 |

**Свойства**

- Питомец получает +20–30% к максимуму здоровья · [Здоровье питомца](../reference/Stats/Stats-HERO.md#stock_pet_health)
- Питомец наносит на 15–20% больше урона · [Урон питомца](../reference/Stats/Stats-HERO.md#stock_pet_damage)
- Удар по вам лечит питомца на 5–8% · [Сторожевой пёс](../reference/Stats/Stats-POWER.md#power_guard_dog)
- Вой вожака · ADD · -20–-12 · [Вой вожака](../reference/Stats/Stats-POWER.md#power_alpha_howl)

**Встроенные модификаторы**

- `PET_ADD_ARMOUR@IMPLICIT`


| Параметр | Значение |
| --- | --- |
| Слот · `slot` | `COLLAR` |
| Редкость · `rarity` | `UNIQUE` |


## PACKLEADERS_BAND <a href="#packleaders_band" id="packleaders_band"></a>

### Packleader's Band <a href="#packleaders-band" id="packleaders-band"></a>

Ведёт не ошейник. Ведёт тот, кто его носит.

**Русское название:** Обруч вожака стаи

**Уникальный · Ошейники**


| Уровень базы | Уровень героя | Сила | Ловкость | Интеллект |
| --- | --- | --- | --- | --- |
| 30 | 30 | 0 | 0 | 0 |

**Свойства**

- Питомец получает +10–15% к скорости атаки · [Скорость атаки питомца](../reference/Stats/Stats-HERO.md#stock_pet_attack_speed)
- Питомец наносит на 20–30% больше урона · [Урон питомца](../reference/Stats/Stats-HERO.md#stock_pet_damage)
- 15–20% увеличение урона (условие: PET_ALIVE) · [Урон](../reference/Stats/Stats-HERO.md#stock_damage)
- Убийство питомцем даёт ему 15–25% увеличения скорости атаки на 4 с · [Кровь вожака](../reference/Stats/Stats-POWER.md#power_alpha_blood)
- Удар питомца даёт вам 3–5% увеличения урона на 3 с, до 5 раз · [Сбор стаи](../reference/Stats/Stats-POWER.md#power_pack_rally)

**Встроенные модификаторы**

- `PET_ATTACK_SPEED@IMPLICIT`


| Параметр | Значение |
| --- | --- |
| Слот · `slot` | `COLLAR` |
| Редкость · `rarity` | `UNIQUE` |


## IRONJAW_COLLAR <a href="#ironjaw_collar" id="ironjaw_collar"></a>

### Ironjaw Collar <a href="#ironjaw-collar" id="ironjaw-collar"></a>

Шипы наружу. Верность внутрь.

**Русское название:** Ошейник железной челюсти

**Уникальный · Ошейники**


| Уровень базы | Уровень героя | Сила | Ловкость | Интеллект |
| --- | --- | --- | --- | --- |
| 45 | 45 | 0 | 0 | 0 |

**Свойства**

- +150–220 к броне питомца · [Прибавка брони питомца](../reference/Stats/Stats-HERO.md#stock_pet_armor)
- +20–30% к сопротивлению стихиям питомца · [Стихийные сопротивления питомца](../reference/Stats/Stats-HERO.md#stock_pet_resist)
- Питомец получает +25–35% к максимуму здоровья · [Здоровье питомца](../reference/Stats/Stats-HERO.md#stock_pet_health)
- Когда питомец падает, получаете 30–50% увеличения урона на 6 с · [Последний вой](../reference/Stats/Stats-POWER.md#power_last_howl)
- Удар по вам лечит питомца на 6–10% · [Сторожевой пёс](../reference/Stats/Stats-POWER.md#power_guard_dog)

**Встроенные модификаторы**

- `PET_RESISTANCES@IMPLICIT`


| Параметр | Значение |
| --- | --- |
| Слот · `slot` | `COLLAR` |
| Редкость · `rarity` | `UNIQUE` |


## BEASTSOUL_CHOKER <a href="#beastsoul_choker" id="beastsoul_choker"></a>

### Beastsoul Choker <a href="#beastsoul-choker" id="beastsoul-choker"></a>

Два сердца — один голод.

**Русское название:** Удавка звериной души

**Уникальный · Ошейники**


| Уровень базы | Уровень героя | Сила | Ловкость | Интеллект |
| --- | --- | --- | --- | --- |
| 58 | 58 | 0 | 0 | 0 |

**Свойства**

- +2 к уровню питомца · [Уровень питомца](../reference/Stats/Stats-HERO.md#stock_pet_level)
- Питомец наносит на 25–35% больше урона · [Урон питомца](../reference/Stats/Stats-HERO.md#stock_pet_damage)
- 8–12% увеличение скорости атаки (условие: PET_ALIVE) · [Скорость атаки](../reference/Stats/Stats-HERO.md#stock_attack_speed)
- Ваши убийства лечат питомца на 10–15% его здоровья · [Узы стаи](../reference/Stats/Stats-POWER.md#power_pack_bond)
- Убийство питомцем даёт ему 20–30% увеличения скорости атаки на 4 с · [Кровь вожака](../reference/Stats/Stats-POWER.md#power_alpha_blood)
- -10–-6% увеличение максимума здоровья · [Здоровье](../reference/Stats/Stats-HERO.md#stock_health)

**Встроенные модификаторы**

- `PET_INCREASED_DAMAGE@IMPLICIT`


| Параметр | Значение |
| --- | --- |
| Слот · `slot` | `COLLAR` |
| Редкость · `rarity` | `UNIQUE` |


## WILDMOTHERS_GARLAND <a href="#wildmothers_garland" id="wildmothers_garland"></a>

### Wildmother's Garland <a href="#wildmothers-garland" id="wildmothers-garland"></a>

Сплетён из листьев, что так и не опали.

**Русское название:** Венок дикой матери

**Уникальный · Ошейники**


| Уровень базы | Уровень героя | Сила | Ловкость | Интеллект |
| --- | --- | --- | --- | --- |
| 66 | 66 | 0 | 0 | 0 |

**Свойства**

- Питомец получает +35–45% к максимуму здоровья · [Здоровье питомца](../reference/Stats/Stats-HERO.md#stock_pet_health)
- +25–35% к сопротивлению стихиям питомца · [Стихийные сопротивления питомца](../reference/Stats/Stats-HERO.md#stock_pet_resist)
- Восстанавливает 1–1.5% здоровья в секунду (условие: PET_ALIVE) · [Регенерация здоровья, %](../reference/Stats/Stats-HERO.md#stock_life_regen_percent)
- Удар по вам лечит питомца на 8–12% · [Сторожевой пёс](../reference/Stats/Stats-POWER.md#power_guard_dog)
- Удар питомца даёт вам 4–6% увеличения урона на 3 с, до 5 раз · [Сбор стаи](../reference/Stats/Stats-POWER.md#power_pack_rally)

**Встроенные модификаторы**

- `PET_INCREASED_DAMAGE@IMPLICIT`


| Параметр | Значение |
| --- | --- |
| Слот · `slot` | `COLLAR` |
| Редкость · `rarity` | `UNIQUE` |


## THE_NEST <a href="#the_nest" id="the_nest"></a>

### The Nest <a href="#the-nest" id="the-nest"></a>

Каждое яйцо, что оно согревало, оно помнит до сих пор.

**Русское название:** Гнездо

**Уникальный · Ошейники**


| Уровень базы | Уровень героя | Сила | Ловкость | Интеллект |
| --- | --- | --- | --- | --- |
| 35 | 35 | 0 | 0 | 0 |

**Свойства**

- +2 к местам инкубатора · [Места инкубатора](../reference/Stats/Stats-HERO.md#stock_incubator_slots)
- Время вылупления дольше на 50% · [Время вылупления](../reference/Stats/Stats-HERO.md#stock_hatch_time)
- Питомец получает +20–30% к максимуму здоровья · [Здоровье питомца](../reference/Stats/Stats-HERO.md#stock_pet_health)
- Ваши убийства лечат питомца на 8–12% его здоровья · [Узы стаи](../reference/Stats/Stats-POWER.md#power_pack_bond)
- Удар по вам лечит питомца на 5–8% · [Сторожевой пёс](../reference/Stats/Stats-POWER.md#power_guard_dog)

**Встроенные модификаторы**

- `PET_INCREASED_LIFE@IMPLICIT`


| Параметр | Значение |
| --- | --- |
| Слот · `slot` | `COLLAR` |
| Редкость · `rarity` | `UNIQUE` |


## WARM_SCALES <a href="#warm_scales" id="warm_scales"></a>

### Warm Scales <a href="#warm-scales" id="warm-scales"></a>

Сброшена матерью, что не дала остыть ни одной кладке.

**Русское название:** Тёплая чешуя

**Уникальный · Ошейники**


| Уровень базы | Уровень героя | Сила | Ловкость | Интеллект |
| --- | --- | --- | --- | --- |
| 50 | 50 | 0 | 0 | 0 |

**Свойства**

- Время вылупления дольше на -40% · [Время вылупления](../reference/Stats/Stats-HERO.md#stock_hatch_time)
- 25% шанс, что питомец вылупится на ступень редкости выше · [Шанс повысить редкость вылупившегося](../reference/Stats/Stats-HERO.md#stock_hatch_rarity_up)
- +15–25% к сопротивлению стихиям питомца · [Стихийные сопротивления питомца](../reference/Stats/Stats-HERO.md#stock_pet_resist)
- Убийство питомцем даёт ему 15–25% увеличения скорости атаки на 4 с · [Кровь вожака](../reference/Stats/Stats-POWER.md#power_alpha_blood)
- Удар питомца даёт вам 3–5% увеличения урона на 3 с, до 5 раз · [Сбор стаи](../reference/Stats/Stats-POWER.md#power_pack_rally)

**Встроенные модификаторы**

- `PET_RESISTANCES@IMPLICIT`


| Параметр | Значение |
| --- | --- |
| Слот · `slot` | `COLLAR` |
| Редкость · `rarity` | `UNIQUE` |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/equipment.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
