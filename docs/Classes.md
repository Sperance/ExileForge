# Семь классов

Семь классов используют общий мир и общее дерево, но начинают из разных узлов и изучают свои книги умений. Сложность в таблице — авторская оценка контента 1–3, а не множитель входящего урона.

Общая база на первом уровне: **38 здоровья**, радиус света **5**. Общий прирост за уровень выше первого: **12 здоровья**, **5 маны**. Собственные значения класса накладываются на общую базу. Свойства конверсии атрибутов затем участвуют в расчёте листа героя.

## MARAUDER

### Мародёр (Marauder)

Сила: больше здоровья и физического урона

Танк ближнего боя

Изгнанник, выживший за счёт упрямства и тяжёлого топора. Стоит в гуще врагов и не падает.

Медленные мощные удары, много здоровья и брони, лечение от урона. Прощает ошибки.


| Параметр | Значение |
| --- | --- |
| Код · `code` | [Мародёр (Marauder)](Classes.md#marauder) |
| difficulty | 1 |
| startNode | [Мародёр](Tree-START.md#str_start) |
| Базовые свойства · `base` | STOCK_STRENGTH: 32; STOCK_AGILITY: 14; STOCK_INTELLECT: 14; STOCK_MANA: 35 |
| Урон оружия · `weapon` | [Stone Axe](Equipment-WEAPON_2H.md#stone_axe) |
| armour | [Iron Hat](Equipment-HELMET.md#iron_hat); [Plate Vest](Equipment-BODY.md#plate_vest); [Iron Greaves](Equipment-BOOTS.md#iron_greaves) |


[Умения класса](Skills-MARAUDER.md)

## RANGER

### Охотница (Ranger)

Ловкость: уклонение и скорость

Стрелок на уклонении

Следопыт диких земель. Бьёт издалека и не даёт себя поймать.

Быстрые выстрелы из лука, высокое уклонение и скорость передвижения. Хороша в фарме.


| Параметр | Значение |
| --- | --- |
| Код · `code` | [Охотница (Ranger)](Classes.md#ranger) |
| difficulty | 2 |
| startNode | [Следопыт](Tree-START.md#dex_start) |
| Базовые свойства · `base` | STOCK_STRENGTH: 14; STOCK_AGILITY: 32; STOCK_INTELLECT: 14; STOCK_MANA: 45 |
| Урон оружия · `weapon` | [Crude Bow](Equipment-WEAPON_1H.md#crude_bow) |
| armour | [Leather Cap](Equipment-HELMET.md#leather_cap); [Scale Vest](Equipment-BODY.md#scale_vest); [Rawhide Boots](Equipment-BOOTS.md#rawhide_boots) |


[Умения класса](Skills-RANGER.md)

## WITCH

### Ведьма (Witch)

Интеллект: энергощит и стихии

Маг стихий

Изгнана за запретные чары. Сжигает, замораживает и бьёт молнией.

Заклинания стихий, энергощит вместо брони, много маны. Сильна, но хрупка.


| Параметр | Значение |
| --- | --- |
| Код · `code` | [Ведьма (Witch)](Classes.md#witch) |
| difficulty | 3 |
| startNode | [Ведьма](Tree-START.md#int_start) |
| Базовые свойства · `base` | STOCK_STRENGTH: 14; STOCK_AGILITY: 14; STOCK_INTELLECT: 32; STOCK_MANA: 65 |
| Урон оружия · `weapon` | [Driftwood Wand](Equipment-WEAPON_1H.md#driftwood_wand) |
| armour | [Vine Circlet](Equipment-HELMET.md#vine_circlet); [Simple Robe](Equipment-BODY.md#simple_robe); [Wool Shoes](Equipment-BOOTS.md#wool_shoes) |


[Умения класса](Skills-WITCH.md)

## DUELIST

### Дуэлянт (Duelist)

Сила и ловкость: ближний бой, блок и скорость атаки

Боец-фехтовальщик

Гладиатор арены. Быстрый клинок, блок и жажда крови.

Частые удары мечом, блок щитом и вампиризм. Держится за счёт темпа.


| Параметр | Значение |
| --- | --- |
| Код · `code` | [Дуэлянт (Duelist)](Classes.md#duelist) |
| difficulty | 2 |
| startNode | [Дуэлянт](Tree-START.md#str_dex_start) |
| Базовые свойства · `base` | STOCK_STRENGTH: 23; STOCK_AGILITY: 23; STOCK_INTELLECT: 14; STOCK_MANA: 40 |
| Урон оружия · `weapon` | [Rusted Sword](Equipment-WEAPON_1H.md#rusted_sword) |
| armour | [Battered Helm](Equipment-HELMET.md#battered_helm); [Scale Vest](Equipment-BODY.md#scale_vest); [Leatherscale Boots](Equipment-BOOTS.md#leatherscale_boots) |


[Умения класса](Skills-DUELIST.md)

## TEMPLAR

### Храмовник (Templar)

Сила и интеллект: броня, энергощит и стихии

Паладин стихий

Бывший инквизитор. Молот в одной руке, молитва в другой.

Смесь атак и заклинаний, броня плюс энергощит, лечение. Надёжный универсал.


| Параметр | Значение |
| --- | --- |
| Код · `code` | [Храмовник (Templar)](Classes.md#templar) |
| difficulty | 2 |
| startNode | [Храмовник](Tree-START.md#str_int_start) |
| Базовые свойства · `base` | STOCK_STRENGTH: 23; STOCK_AGILITY: 14; STOCK_INTELLECT: 23; STOCK_MANA: 55 |
| Урон оружия · `weapon` | [Rusted Hatchet](Equipment-WEAPON_1H.md#rusted_hatchet) |
| armour | [Iron Hat](Equipment-HELMET.md#iron_hat); [Chainmail Vest](Equipment-BODY.md#chainmail_vest); [Iron Greaves](Equipment-BOOTS.md#iron_greaves) |


[Умения класса](Skills-TEMPLAR.md)

## SHADOW

### Тень (Shadow)

Ловкость и интеллект: криты, уклонение и хаос

Убийца: криты и яды

Наёмный убийца. Бьёт первым и исчезает.

Критические удары, яды и хаос, уклонение. Огромный урон, мало права на ошибку.


| Параметр | Значение |
| --- | --- |
| Код · `code` | [Тень (Shadow)](Classes.md#shadow) |
| difficulty | 3 |
| startNode | [Тень](Tree-START.md#dex_int_start) |
| Базовые свойства · `base` | STOCK_STRENGTH: 14; STOCK_AGILITY: 23; STOCK_INTELLECT: 23; STOCK_MANA: 55 |
| Урон оружия · `weapon` | [Rusted Spike](Equipment-WEAPON_1H.md#rusted_spike) |
| armour | [Scare Mask](Equipment-HELMET.md#scare_mask); [Padded Vest](Equipment-BODY.md#padded_vest); [Rawhide Boots](Equipment-BOOTS.md#rawhide_boots) |


[Умения класса](Skills-SHADOW.md)

## SCION

### Скион (Scion)

Универсал: ровно по двадцать каждого атрибута и выход в любую ветку дерева

Универсал

Наследница знатного рода. Может стать кем угодно.

Ровные атрибуты и выход в любую ветку дерева. Сильна, если знать, что собирать.


| Параметр | Значение |
| --- | --- |
| Код · `code` | [Скион (Scion)](Classes.md#scion) |
| difficulty | 3 |
| startNode | [Потомок](Tree-START.md#scion_start) |
| Базовые свойства · `base` | STOCK_STRENGTH: 20; STOCK_AGILITY: 20; STOCK_INTELLECT: 20; STOCK_MANA: 50 |
| Урон оружия · `weapon` | [Corroded Blade](Equipment-WEAPON_2H.md#corroded_blade) |
| armour | [Battered Helm](Equipment-HELMET.md#battered_helm); [Chainmail Vest](Equipment-BODY.md#chainmail_vest); [Leatherscale Boots](Equipment-BOOTS.md#leatherscale_boots) |


[Умения класса](Skills-SCION.md)

## Общие конверсии

- +1 к максимуму здоровья за каждые 2 Сила · [Здоровье](Stats-HERO.md#stock_health)
- +1 к уклонению за каждые 1 Ловкость · [Уклонение](Stats-HERO.md#stock_evasion)
- 1% увеличение энергетического щита за каждые 5 Интеллект · [Энергетический щит](Stats-HERO.md#stock_energy_shield)
- 1% увеличение физического урона за каждые 5 Сила · [Физический урон](Stats-HERO.md#stock_attack_physical)
- +1 к максимуму маны за каждые 2 Интеллект · [Мана](Stats-HERO.md#stock_mana)

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/classes.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](README.md) · [Все страницы](Catalogs.md)
