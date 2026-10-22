# Проект #434/#435 (черновик для согласования)
Модель: BASE_PHYSICAL_DAMAGE = средний; ширина семейства rules.json weapons.spread (+ ItemTemplate.spread у уникалок); концы мин=ср(1-w), макс=ср(1+w) в foldItem (WeaponSpread.ends).
Статы: STOCK_ATTACK_<T>_MIN/_MAX (ADD, partOf ATTACK_T, вес ½ в ATTACK_T), STOCK_ATTACK_<T>_SPREAD (полуширина %, считает HitRanges.settle), STOCK_WEAPON_MAXIMUM/MINIMUM_DAMAGE (локальные MORE), STOCK_LUCKY_DAMAGE (шанс %, потолок 100), STOCK_MAXIMUM_DAMAGE_ROLLS (флаг силы уникалки).
StatSpread.parts(stat,op) с весом. ATTACK_T остаётся средним - Мощь/монстры/дерево как были.
ADD_<T>_DAMAGE: два эффекта MIN/MAX, одна доля Tier.at; число тиров не менять (10/6/2). h по стихии: физ/хаос ⅓, огонь 0.3, холод 0.25, молния 0.8. Таблица сеток - в отчёте (PHYSICAL NATURAL top [13,15];[27,31] ... LIGHTNING LOCAL [9,11];[79,95]).
UniqueLine.local; плоские ADD уникалок (~102, 57 оружейных local) → пары; новые строки уникалок только в конец.
Дерево: 11 ADD_CHAOS_DAMAGE → PASSIVE_ADD_CHAOS_DAMAGE (одно число).
Тексты: шаблон .range «Добавляет {0}–{1} ...», хвост .weapon «этого оружия» (#435).
Вайп не нужен (Roll без чисел, коды/тиры те же); RULES_VERSION 102; API_REVISION 101.
Ширина ±%: WAND 10, BLADE 12, SWORD 22, LONGSWORD 25, BOW 25, DOUBLESWORD 30, SCEPTRE 30, STAFF 30, AXE 35, DOUBLEAXE 40, без оружия 20.
Моды: WEAPON_MAXIMUM_DAMAGE@LOCAL PREFIX MORE вес 500 top 40-45 apex 46-52; WEAPON_MINIMUM_DAMAGE@LOCAL SUFFIX MORE вес 600 top 60-70; LUCKY_DAMAGE SUFFIX глоб. веса weapon150 quiver200 gloves120 amulet100 ring80 top 36-42; PASSIVE_LUCKY_DAMAGE; «урон всегда наибольший» - только сила уникалки.
Уникалки: HEADSMANS_LOT «Жребий палача» 2р топор ур68 spread 60, лок. ADD (20-26)-(48-58), +140-170% лок., +25-35 силы, нет критов, сила: урон всегда наибольший, -20% скорости атаки; LOADED_DIE «Шулерская кость» кольцо ур55: молния (1-2)-(30-38), +20-30 ловк., -30-40 множ. крита, сила: урон атак всегда удачный; GAMBLERS_QUIVER «Колчан игрока» ур62: молния (2-3)-(26-32), 30-40% удачного урона, +8-12% скор. атаки, -15-20% меткости, сила POWER_LUCKY_BREAK.
Дерево: DUE_K5 «Мясник» 35%→25% больше физ + 100% удачного урона; SHA_K0 «Совершенный удар» -30→-40 множ. крита + 50% удачного; MAR_N1_5 «Чистый срез» +20% по уникальным → 25% удачного. Двойник DUE_N0_5 - вопрос.
Шаги: 1 сервер ядро 14-18 ч; 2 сервер содержание 8-10 ч; 3 клиент core 10-12 ч; 4 клиент app+баланс 8-12 ч. Итого 40-52 ч.
Карточка: MedallionHead (до 3 чисел базы, гнездо, RollRing), BaseFigures, RelicLines; сводка HeroSummary StatSheet/StatRow. Направления макетов: A числа «мин–макс», B полоса диапазона, C среднее + чип ±%.
Риски/вопросы: 1 глобальные ADD на старом оружии → rehome в LOCAL (сетки ×2, бафф); 2 h по стихиям; 3 удача - шанс или флаг; 4 +макс/мин - MORE или INCREASED; 5 префикс/суффикс; 6 DUE_N0_5; 7 минус «Жребия»; 8 дробные/округление; 13 API_REVISION обязателен; 15 баланс MobBal (страж 55-60%, темп ≤+5%).
