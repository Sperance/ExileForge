# KEYSTONE

| Название | Код |
| --- | --- |
| [Воитель](#scn_k0) | SCN_K0 |
| [Стихийный клинок](#scn_k1) | SCN_K1 |
| [Храмовый маг](#scn_k2) | SCN_K2 |
| [Путь посередине](#scn_k3) | SCN_K3 |
| [Несокрушимый](#mar_k0) | MAR_K0 |
| [Кровь титана](#mar_k5) | MAR_K5 |
| [Ненасытный](#due_k0) | DUE_K0 |
| [Мясник](#due_k5) | DUE_K5 |
| [Ускользающий](#ran_k0) | RAN_K0 |
| [Лёгкая поступь](#ran_k5) | RAN_K5 |
| [Совершенный удар](#sha_k0) | SHA_K0 |
| [Тысяча порезов](#sha_k5) | SHA_K5 |
| [Хрупкий разум](#wit_k0) | WIT_K0 |
| [Стихийная буря](#wit_k5) | WIT_K5 |
| [Пламенный суд](#tem_k0) | TEM_K0 |
| [Небесный щит](#tem_k5) | TEM_K5 |
| [Несгибаемый бастион](#mar_k6) | MAR_K6 |
| [Железная кровь](#mar_k7) | MAR_K7 |
| [Орлиный глаз](#ran_k6) | RAN_K6 |
| [Холодный расчёт](#ran_k7) | RAN_K7 |
| [Бесконечный прилив](#wit_k6) | WIT_K6 |
| [Камень-оберег](#wit_k7) | WIT_K7 |
| [Гордость дуэлянта](#due_k6) | DUE_K6 |
| [Танец клинков](#due_k7) | DUE_K7 |
| [Хранитель клятвы](#tem_k6) | TEM_K6 |
| [Небесная кузня](#tem_k7) | TEM_K7 |
| [Невидимый нож](#sha_k6) | SHA_K6 |
| [Ядовитое сердце](#sha_k7) | SHA_K7 |
| [Кровь змея](#sha_k8) | SHA_K8 |
| [Гамбит наследника](#scn_k6) | SCN_K6 |
| [Общая душа](#scn_k7) | SCN_K7 |


## SCN_K0 <a href="#scn_k0" id="scn_k0"></a>

### Воитель <a href="#воитель" id="воитель"></a>

Сила и ловкость окупаются вдвойне: вдвое больше физического урона от силы и уклонения от ловкости, но маны меньше.

**Цена:** 1 очк.

**Свойства**

- 1% увеличение физического урона за каждые 5 Сила · [Физический урон](../reference/Stats/Stats-HERO.md#stock_attack_physical)
- +1 к уклонению за каждые 1 Ловкость · [Уклонение](../reference/Stats/Stats-HERO.md#stock_evasion)
- Мана · MORE · -30 · [Мана](../reference/Stats/Stats-HERO.md#stock_mana)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## SCN_K1 <a href="#scn_k1" id="scn_k1"></a>

### Стихийный клинок <a href="#стихийный-клинок" id="стихийный-клинок"></a>

Клинок несёт стихию: стихийного урона намного больше, а физического вдвое меньше.

**Цена:** 1 очк.

**Свойства**

- 40% больше урона от огня · [Урон огнём](../reference/Stats/Stats-HERO.md#stock_attack_fire); 40% больше урона от холода · [Урон холодом](../reference/Stats/Stats-HERO.md#stock_attack_cold); 40% больше урона от молнии · [Урон молнией](../reference/Stats/Stats-HERO.md#stock_attack_lightning)
- -50% больше физического урона · [Физический урон](../reference/Stats/Stats-HERO.md#stock_attack_physical)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## SCN_K2 <a href="#scn_k2" id="scn_k2"></a>

### Храмовый маг <a href="#храмовый-маг" id="храмовый-маг"></a>

Разум крепит тело: интеллект даёт здоровье, как сила, и пределы стихийных сопротивлений выше, но критических ударов не бывает.

**Цена:** 1 очк.

**Свойства**

- +1 к максимуму здоровья за каждые 2 Интеллект · [Здоровье](../reference/Stats/Stats-HERO.md#stock_health)
- +5% к максимуму всех сопротивлений · [Максимум сопротивлений стихиям](../reference/Stats/Stats-HERO.md#stock_resist_max_all)
- Шанс критического удара: 0 · [Шанс критического удара](../reference/Stats/Stats-HERO.md#stock_critical_chance)
- Шанс критического удара заклинаний: 0 · [Шанс крита заклинаний](../reference/Stats/Stats-HERO.md#stock_spell_critical_chance)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## SCN_K3 <a href="#scn_k3" id="scn_k3"></a>

### Путь посередине <a href="#путь-посередине" id="путь-посередине"></a>

Ни в чём не лучший, во всём достаточный: больше здоровья, урона и сопротивлений, но ниже пределы блока и уклонения.

**Цена:** 1 очк.

**Свойства**

- 20% больше максимума здоровья · [Здоровье](../reference/Stats/Stats-HERO.md#stock_health)
- Наносит на 20% больше урона · [Урон](../reference/Stats/Stats-HERO.md#stock_damage)
- +10% ко всем сопротивлениям · [Все сопротивления](../reference/Stats/Stats-HERO.md#stock_all_resistances)
- +-5% к максимуму шанса блока · [Максимум блока](../reference/Stats/Stats-HERO.md#stock_block_max)
- +-5% к максимуму шанса уклонения · [Максимум уклонения](../reference/Stats/Stats-HERO.md#stock_evasion_max)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## MAR_K0 <a href="#mar_k0" id="mar_k0"></a>

### Несокрушимый <a href="#несокрушимый" id="несокрушимый"></a>

Запас жизни больше на четверть сверх всего прочего.

**Цена:** 1 очк.

**Свойства**

- 25% больше максимума здоровья · [Здоровье](../reference/Stats/Stats-HERO.md#stock_health)
- Восстанавливает 6 здоровья в секунду · [Восстановление здоровья](../reference/Stats/Stats-HERO.md#stock_health_regen)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## MAR_K5 <a href="#mar_k5" id="mar_k5"></a>

### Кровь титана <a href="#кровь-титана" id="кровь-титана"></a>

Вдвое крепче против оглушения и на половину больше брони, предел физического снижения выше, но уклонения нет вовсе.

**Цена:** 1 очк.

**Свойства**

- 100% больше порога оглушения · [Порог оглушения](../reference/Stats/Stats-HERO.md#stock_stun_threshold)
- 50% больше брони · [Броня](../reference/Stats/Stats-HERO.md#stock_armor)
- Уклонение: 0 · [Уклонение](../reference/Stats/Stats-HERO.md#stock_evasion)
- +5% к максимуму снижения физического урона · [Максимум физического снижения](../reference/Stats/Stats-HERO.md#stock_physical_reduction_max)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## DUE_K0 <a href="#due_k0" id="due_k0"></a>

### Ненасытный <a href="#ненасытный" id="ненасытный"></a>

Вампиризм сильнее на шестьдесят процентов, а предел блока выше.

**Цена:** 1 очк.

**Свойства**

- 60% больше кражи здоровья · [Физический вампиризм](../reference/Stats/Stats-HERO.md#stock_leech_physical)
- 1% физического урона атак крадётся здоровьем · [Физический вампиризм](../reference/Stats/Stats-HERO.md#stock_leech_physical)
- +5% к максимуму шанса блока · [Максимум блока](../reference/Stats/Stats-HERO.md#stock_block_max)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## DUE_K5 <a href="#due_k5" id="due_k5"></a>

### Мясник <a href="#мясник" id="мясник"></a>

Физический урон больше на пятую часть, удары режут до крови, но жизнь не восстанавливается сама.

**Цена:** 1 очк.

**Свойства**

- 60% больше физического урона · [Физический урон](../reference/Stats/Stats-HERO.md#stock_attack_physical)
- 40% шанс вызвать кровотечение · [Шанс кровотечения](../reference/Stats/Stats-HERO.md#stock_bleed_chance)
- Восстановление здоровья: 0 · [Восстановление здоровья](../reference/Stats/Stats-HERO.md#stock_health_regen)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## RAN_K0 <a href="#ran_k0" id="ran_k0"></a>

### Ускользающий <a href="#ускользающий" id="ускользающий"></a>

Уклонение заметно выше, а шаг быстрее, но брони нет совсем.

**Цена:** 1 очк.

**Свойства**

- 60% увеличение уклонения · [Уклонение](../reference/Stats/Stats-HERO.md#stock_evasion)
- Броня: 0 · [Броня](../reference/Stats/Stats-HERO.md#stock_armor)
- 10% увеличение скорости передвижения · [Скорость передвижения](../reference/Stats/Stats-HERO.md#stock_movement_speed)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## RAN_K5 <a href="#ran_k5" id="ran_k5"></a>

### Лёгкая поступь <a href="#лёгкая-поступь" id="лёгкая-поступь"></a>

Уклонение больше на треть, его предел выше и шаг быстрее, но жизнь не восстанавливается сама.

**Цена:** 1 очк.

**Свойства**

- 60% больше уклонения · [Уклонение](../reference/Stats/Stats-HERO.md#stock_evasion)
- 20% увеличение скорости передвижения · [Скорость передвижения](../reference/Stats/Stats-HERO.md#stock_movement_speed)
- Восстановление здоровья: 0 · [Восстановление здоровья](../reference/Stats/Stats-HERO.md#stock_health_regen)
- +5% к максимуму шанса уклонения · [Максимум уклонения](../reference/Stats/Stats-HERO.md#stock_evasion_max)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## SHA_K0 <a href="#sha_k0" id="sha_k0"></a>

### Совершенный удар <a href="#совершенный-удар" id="совершенный-удар"></a>

Шанс критического удара всегда сорок процентов.

**Цена:** 1 очк.

**Свойства**

- Шанс критического удара: 64 · [Шанс критического удара](../reference/Stats/Stats-HERO.md#stock_critical_chance)
- +16% к множителю критического удара · [Множитель критического удара](../reference/Stats/Stats-HERO.md#stock_critical_multiplier)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## SHA_K5 <a href="#sha_k5" id="sha_k5"></a>

### Тысяча порезов <a href="#тысяча-порезов" id="тысяча-порезов"></a>

Атаки быстрее и отравляют, но критический удар не усиливается сверх базы.

**Цена:** 1 очк.

**Свойства**

- 48% увеличение скорости атаки · [Скорость атаки](../reference/Stats/Stats-HERO.md#stock_attack_speed)
- 50% шанс отравить · [Шанс отравления](../reference/Stats/Stats-HERO.md#stock_poison_chance)
- Урон критических ударов: 0 · [Урон критического удара](../reference/Stats/Stats-HERO.md#stock_critical_damage)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## WIT_K0 <a href="#wit_k0" id="wit_k0"></a>

### Хрупкий разум <a href="#хрупкий-разум" id="хрупкий-разум"></a>

Жизнь всегда одна единица, но урон хаосом вас не касается, а щит крепче.

**Цена:** 1 очк.

**Свойства**

- Максимум здоровья: 1 · [Здоровье](../reference/Stats/Stats-HERO.md#stock_health)
- Иммунитет к урону хаосом · [Иммунитет к хаосу](../reference/Stats/Stats-HERO.md#stock_chaos_immune)
- 20% увеличение энергетического щита · [Энергетический щит](../reference/Stats/Stats-HERO.md#stock_energy_shield)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## WIT_K5 <a href="#wit_k5" id="wit_k5"></a>

### Стихийная буря <a href="#стихийная-буря" id="стихийная-буря"></a>

Стихийный урон больше на пятую часть, но критических ударов не бывает.

**Цена:** 1 очк.

**Свойства**

- 64% больше урона от огня · [Урон огнём](../reference/Stats/Stats-HERO.md#stock_attack_fire); 64% больше урона от холода · [Урон холодом](../reference/Stats/Stats-HERO.md#stock_attack_cold); 64% больше урона от молнии · [Урон молнией](../reference/Stats/Stats-HERO.md#stock_attack_lightning)
- 20% шанс шокировать · [Шанс шока](../reference/Stats/Stats-HERO.md#stock_shock_chance)
- Шанс критического удара: 0 · [Шанс критического удара](../reference/Stats/Stats-HERO.md#stock_critical_chance)
- Шанс критического удара заклинаний: 0 · [Шанс крита заклинаний](../reference/Stats/Stats-HERO.md#stock_spell_critical_chance)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## TEM_K0 <a href="#tem_k0" id="tem_k0"></a>

### Пламенный суд <a href="#пламенный-суд" id="пламенный-суд"></a>

Стихийный урон больше на тридцать процентов.

**Цена:** 1 очк.

**Свойства**

- 48% больше урона от огня · [Урон огнём](../reference/Stats/Stats-HERO.md#stock_attack_fire); 48% больше урона от холода · [Урон холодом](../reference/Stats/Stats-HERO.md#stock_attack_cold); 48% больше урона от молнии · [Урон молнией](../reference/Stats/Stats-HERO.md#stock_attack_lightning)
- +12% к сопротивлению огню · [Сопротивление огню](../reference/Stats/Stats-HERO.md#stock_resist_fire)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## TEM_K5 <a href="#tem_k5" id="tem_k5"></a>

### Небесный щит <a href="#небесный-щит" id="небесный-щит"></a>

Пределы стихийных сопротивлений и блока выше и брони больше, но критический удар не усиливается сверх базы.

**Цена:** 1 очк.

**Свойства**

- +6% к максимуму всех сопротивлений · [Максимум сопротивлений стихиям](../reference/Stats/Stats-HERO.md#stock_resist_max_all)
- 60% больше брони · [Броня](../reference/Stats/Stats-HERO.md#stock_armor)
- Урон критических ударов: 0 · [Урон критического удара](../reference/Stats/Stats-HERO.md#stock_critical_damage)
- +5% к максимуму шанса блока · [Максимум блока](../reference/Stats/Stats-HERO.md#stock_block_max)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |


## MAR_K6 <a href="#mar_k6" id="mar_k6"></a>

### Несгибаемый бастион <a href="#несгибаемый-бастион" id="несгибаемый-бастион"></a>

Всегда укреплён и на заряд выносливости больше, но атакует медленнее.

**Цена:** 1 очк.

**Свойства**

- Укреплён · [Укреплён](../reference/Stats/Stats-HERO.md#stock_fortify_always)
- +1 к максимуму зарядов выносливости · [Максимум зарядов выносливости](../reference/Stats/Stats-HERO.md#stock_max_endurance_charges)
- -10% увеличение скорости атаки · [Скорость атаки](../reference/Stats/Stats-HERO.md#stock_attack_speed)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Закалённость](Tree-SMALL.md#mar_t6_1) |


## MAR_K7 <a href="#mar_k7" id="mar_k7"></a>

### Железная кровь <a href="#железная-кровь" id="железная-кровь"></a>

Здоровье не восстанавливается само — вместо этого броня становится возвратом здоровья.

**Цена:** 1 очк.

**Свойства**

- +2% возврата здоровья за каждые 1000 брони, до 25% · [Железная кровь](../reference/Stats/Stats-POWER.md#power_iron_blood)
- Восстановление здоровья: 0 · [Восстановление здоровья](../reference/Stats/Stats-HERO.md#stock_health_regen)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Выносливость](Tree-SMALL.md#mar_t7_1) |


## RAN_K6 <a href="#ran_k6" id="ran_k6"></a>

### Орлиный глаз <a href="#орлиный-глаз" id="орлиный-глаз"></a>

Постоянный натиск и точнее прицел ценой тонкой брони.

**Цена:** 1 очк.

**Свойства**

- Под Натиском · [Натиск](../reference/Stats/Stats-HERO.md#stock_onslaught_always)
- 30% увеличение меткости · [Меткость](../reference/Stats/Stats-HERO.md#stock_accuracy)
- -30% увеличение брони · [Броня](../reference/Stats/Stats-HERO.md#stock_armor)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Обморожение](Tree-SMALL.md#ran_t6_1) |


## RAN_K7 <a href="#ran_k7" id="ran_k7"></a>

### Холодный расчёт <a href="#холодный-расчёт" id="холодный-расчёт"></a>

Не наносит критических ударов — меткость становится шансом двойного урона.

**Цена:** 1 очк.

**Свойства**

- +1% шанса двойного урона за каждые 200 меткости, до 40% · [Холодный расчёт](../reference/Stats/Stats-POWER.md#power_cold_calculus)
- Шанс критического удара: 0 · [Шанс критического удара](../reference/Stats/Stats-HERO.md#stock_critical_chance)
- Шанс критического удара заклинаний: 0 · [Шанс крита заклинаний](../reference/Stats/Stats-HERO.md#stock_spell_critical_chance)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Отклонение](Tree-SMALL.md#ran_t7_1) |


## WIT_K6 <a href="#wit_k6" id="wit_k6"></a>

### Бесконечный прилив <a href="#бесконечный-прилив" id="бесконечный-прилив"></a>

Волшебный прилив не угасает и чары сильнее, но тело хрупче.

**Цена:** 1 очк.

**Свойства**

- Под Всплеском магии · [Всплеск магии](../reference/Stats/Stats-HERO.md#stock_arcane_surge_always)
- 30% увеличение урона чар · [Урон чар](../reference/Stats/Stats-HERO.md#stock_spell_damage)
- -15% увеличение максимума здоровья · [Здоровье](../reference/Stats/Stats-HERO.md#stock_health)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Подавление](Tree-SMALL.md#wit_t6_1) |


## WIT_K7 <a href="#wit_k7" id="wit_k7"></a>

### Камень-оберег <a href="#камень-оберег" id="камень-оберег"></a>

Уклонения нет — большая его часть становится энергощитом.

**Цена:** 1 очк.

**Свойства**

- 60% уклонения достаётся энергощитом; уклонения нет · [Камень-оберег](../reference/Stats/Stats-POWER.md#power_wardstone)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Порча](Tree-SMALL.md#wit_t7_1) |


## DUE_K6 <a href="#due_k6" id="due_k6"></a>

### Гордость дуэлянта <a href="#гордость-дуэлянта" id="гордость-дуэлянта"></a>

Добивает ослабших и часто бьёт вдвое, но хуже сопротивляется стихиям.

**Цена:** 1 очк.

**Свойства**

- Добивание: убивает врагов ниже 10% здоровья · [Добивание](../reference/Stats/Stats-HERO.md#stock_culling)
- 10% шанс нанести двойной урон · [Шанс двойного урона](../reference/Stats/Stats-HERO.md#stock_double_damage)
- +-10% к всем сопротивлениям стихиям · [Все сопротивления стихиям](../reference/Stats/Stats-HERO.md#stock_resist_all)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Угли](Tree-SMALL.md#due_t6_1) |


## DUE_K7 <a href="#due_k7" id="due_k7"></a>

### Танец клинков <a href="#танец-клинков" id="танец-клинков"></a>

Не блокирует — уклонение превращается в отклонение.

**Цена:** 1 очк.

**Свойства**

- +2% отклонения за каждые 500 уклонения, до 30%; блока нет · [Танец клинков](../reference/Stats/Stats-POWER.md#power_blade_dance)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Возврат](Tree-SMALL.md#due_t7_1) |


## TEM_K6 <a href="#tem_k6" id="tem_k6"></a>

### Хранитель клятвы <a href="#хранитель-клятвы" id="хранитель-клятвы"></a>

На заряд выносливости и силы больше, но шаг тяжелее.

**Цена:** 1 очк.

**Свойства**

- +1 к максимуму зарядов выносливости · [Максимум зарядов выносливости](../reference/Stats/Stats-HERO.md#stock_max_endurance_charges)
- +1 к максимуму зарядов силы · [Максимум зарядов силы](../reference/Stats/Stats-HERO.md#stock_max_power_charges)
- -10% увеличение скорости передвижения · [Скорость передвижения](../reference/Stats/Stats-HERO.md#stock_movement_speed)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Укрепление](Tree-SMALL.md#tem_t6_1) |


## TEM_K7 <a href="#tem_k7" id="tem_k7"></a>

### Небесная кузня <a href="#небесная-кузня" id="небесная-кузня"></a>

Весь физический урон атак становится молнией.

**Цена:** 1 очк.

**Свойства**

- 100% физического урона атак обращается в молнию · [Небесная кузня](../reference/Stats/Stats-POWER.md#power_skyforge)
- 20% увеличение урона от молнии · [Урон молнией](../reference/Stats/Stats-HERO.md#stock_attack_lightning)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Гроза](Tree-SMALL.md#tem_t7_1) |


## SHA_K6 <a href="#sha_k6" id="sha_k6"></a>

### Невидимый нож <a href="#невидимый-нож" id="невидимый-нож"></a>

Нечестивая мощь не покидает, но питается здоровьем.

**Цена:** 1 очк.

**Свойства**

- Под Нечестивой мощью · [Нечестивая мощь](../reference/Stats/Stats-HERO.md#stock_unholy_might_always)
- 8% физического урона добавляется как урон хаосом · [Физический как доп. хаос](../reference/Stats/Stats-HERO.md#stock_physical_as_extra_chaos)
- -12% увеличение максимума здоровья · [Здоровье](../reference/Stats/Stats-HERO.md#stock_health)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Порча](Tree-SMALL.md#sha_t6_1) |


## SHA_K7 <a href="#sha_k7" id="sha_k7"></a>

### Ядовитое сердце <a href="#ядовитое-сердце" id="ядовитое-сердце"></a>

Весь физический урон атак становится хаосом.

**Цена:** 1 очк.

**Свойства**

- 100% физического урона атак обращается в хаос · [Ядовитое сердце](../reference/Stats/Stats-POWER.md#power_venom_heart)
- Наносящие урон состояния действуют на 8% быстрее · [Ускорение урона состояний](../reference/Stats/Stats-HERO.md#stock_faster_ailments)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Отклонение](Tree-SMALL.md#sha_t7_1) |


## SHA_K8 <a href="#sha_k8" id="sha_k8"></a>

### Кровь змея <a href="#кровь-змея" id="кровь-змея"></a>

Сопротивление хаосу всегда 75%, но стихийные сопротивления слабее.

**Цена:** 1 очк.

**Свойства**

- Сопротивление хаосу: 75 · [Сопротивление хаосу](../reference/Stats/Stats-HERO.md#stock_resist_chaos)
- +-15% к всем сопротивлениям стихиям · [Все сопротивления стихиям](../reference/Stats/Stats-HERO.md#stock_resist_all)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Порченая кровь](Tree-SMALL.md#sha_t8_1) |


## SCN_K6 <a href="#scn_k6" id="scn_k6"></a>

### Гамбит наследника <a href="#гамбит-наследника" id="гамбит-наследника"></a>

На заряд каждого вида больше, ценой сопротивлений.

**Цена:** 1 очк.

**Свойства**

- +1 к максимуму зарядов ярости · [Максимум зарядов ярости](../reference/Stats/Stats-HERO.md#stock_max_frenzy_charges)
- +1 к максимуму зарядов силы · [Максимум зарядов силы](../reference/Stats/Stats-HERO.md#stock_max_power_charges)
- +1 к максимуму зарядов выносливости · [Максимум зарядов выносливости](../reference/Stats/Stats-HERO.md#stock_max_endurance_charges)
- +-10% к всем сопротивлениям стихиям · [Все сопротивления стихиям](../reference/Stats/Stats-HERO.md#stock_resist_all)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Натиск](Tree-SMALL.md#scn_t6_1) |


## SCN_K7 <a href="#scn_k7" id="scn_k7"></a>

### Общая душа <a href="#общая-душа" id="общая-душа"></a>

Питомец делит с вами жизнь: получает её часть, а вам остаётся меньше.

**Цена:** 1 очк.

**Свойства**

- Питомец получает 40% вашего максимума здоровья; максимум здоровья на 15% меньше · [Общая душа](../reference/Stats/Stats-POWER.md#power_shared_soul)


| Параметр | Значение |
| --- | --- |
| Тип · `type` | `KEYSTONE` |
| Связанные узлы · `connections` | [Ход боя](Tree-SMALL.md#scn_t7_1) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/tree.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../README.md) · [Все страницы](../Catalogs.md)
