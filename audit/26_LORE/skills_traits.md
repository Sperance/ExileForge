# Умения героев и монстров, черты, тотемы, фазы боссов

Ключи `skill.*.name` (умения героев и `MOB_*` монстров), `trait.*.name`, `totem.*.name`, `phase.*.name`. Имена умений героев повторяются в книгах (`item.BOOK_*`, «Книга: …»).

## Умения (122)

Вердикты: оставить* - 66, оставить - 46, переименовать - 6, развести - 4

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| ADEPT | Универсал | Adept | оставить |  |  |  |
| ANGER | Гнев | Anger | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| ARC | Дуга | Arc | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| ARCANE_WARD | Магический барьер | Arcane Ward | оставить |  |  |  |
| ASSASSIN | Убийца | Assassin | оставить |  |  |  |
| BACKSTAB | Удар в спину | Backstab | оставить* |  |  | совпадает с: skilltree.DUE_C5_3.name |
| BERSERKER_FURY | Ярость берсерка | Berserker Fury | оставить |  |  |  |
| BLADE_FLURRY | Шквал клинков | Blade Flurry | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| BLADE_MASTER | Мастер клинка | Blade Master | оставить |  |  |  |
| BLESSING | Благословение | Blessing | оставить* |  |  | совпадает с: skilltree.SCN_TM_14.name, skilltree.TEM_C3_1.name |
| BLOODLETTING | Кровопролитие | Bloodletting | оставить |  |  |  |
| BLOODLUST | Жажда крови | Bloodlust | оставить* |  |  | совпадает с: trait.TRAIT_BLOODLUST.name |
| BLOOD_RAGE | Кровавая ярость | Blood Rage | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| CHALLENGE | Вызов | Challenge | оставить |  |  |  |
| CHARGED_DASH | Заряженный рывок | Charged Dash | оставить |  |  |  |
| CLARITY | Ясность | Clarity | оставить* |  |  | совпадает с: skilltree.RAN_C4_2.name |
| CONDUCTIVITY | Проводимость | Conductivity | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| COUP_DE_GRACE | Смертельный удар | Coup de Grace | оставить |  |  |  |
| DEEP_RESERVE | Глубокий резерв | Deep Reserve | оставить |  |  |  |
| DESPAIR | Отчаяние | Despair | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| DETERMINATION | Решимость | Determination | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| DISCHARGE | Разрядка | Discharge | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| DISCIPLINE | Дисциплина | Discipline | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| DIVINE_SHIELD | Божественный щит | Divine Shield | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| DOUBLE_STRIKE | Двойной удар | Double Strike | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| EAGLE_EYE | Зоркий глаз | Eagle Eye | оставить* |  |  | совпадает с: trait.TRAIT_KEEN_EYE.name |
| EARTHEN_RELEASE | Земной выброс | Earthen Release | оставить |  |  |  |
| ECHO | Отзвук | Echo | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_ECHOING_GRIPS |
| ELEMENTAL_HIT | Стихийный удар | Elemental Hit | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| ELEMENTAL_MASTERY | Мастер стихий | Elemental Mastery | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| ELEMENTAL_SURGE | Раскол стихий | Elemental Surge | переименовать | Всплеск стихий | Elemental Surge | ru «Раскол стихий» ≠ en «Surge». |
| ELEMENTAL_WEAKNESS | Стихийная слабость | Elemental Weakness | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| ENDURING_CRY | Клич стойкости | Enduring Cry | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| ENDURING_ROAR | Рёв стойкости | Enduring Roar | оставить |  |  |  |
| ENFEEBLE | Бессилие | Enfeeble | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| ESSENCE_DRAIN | Иссушение | Essence Drain | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| FAITH | Вера | Faith | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_CARDINAL_FAITH |
| FIREBALL | Огненный шар | Fireball | развести |  |  | дубль внутри категории: skill.MOB_FIREBALL.name; совпадает с: trait.TRAIT_SKILL_FIREBALL.name |
| FLASK_ECHO | Эхо фляг | Flask Echo | оставить |  |  |  |
| FORTITUDE | Стойкость | Fortitude | оставить* |  |  | совпадает с: skilltree.MAR_C1_1.name, skilltree.TEM_C1_2.name, skilltree.TEM_T1_1.name, rift.boon.RIFT_RESOLVE.name |
| FREEZING_PULSE | Ледяной импульс | Freezing Pulse | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| FRENZY_STRIKE | Яростный выстрел | Frenzy Strike | переименовать | Яростный выстрел | Frenzy Shot | ru «выстрел» ≠ en «Strike» (умение лука Охотницы). |
| FROST_BURST | Ледяная вспышка | Frost Burst | оставить |  |  |  |
| GRACE | Грация | Grace | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| GROUND_SLAM | Сотрясение земли | Ground Slam | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| GUST | Порыв ветра | Gust | оставить |  |  |  |
| HARMONY | Гармония | Harmony | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_PRISM_HARMONY, skilltree.SCN_MP_3.name |
| HASTE | Спешка | Haste | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| HEAVY_STRIKE | Тяжёлый удар | Heavy Strike | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| HOLY_ARMS | Святое оружие | Holy Arms | оставить |  |  |  |
| HUNGER_FOR_POWER | Жажда силы | Hunger for Power | оставить |  |  |  |
| HUNTERS_THRILL | Охотничий азарт | Hunter’s Thrill | оставить |  |  |  |
| ICE_SHOT | Ледяной выстрел | Ice Shot | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| INFERNAL_BLOW | Адский удар | Infernal Blow | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| IRON_SKIN | Железная кожа | Iron Skin | оставить* |  |  | совпадает с: rift.boon.RIFT_IRON.name |
| LAST_WARD | Последний оплот | Last Ward | оставить |  |  |  |
| LEGACY | Наследие | Legacy | оставить* |  |  | совпадает с: skilltree.FIL_6.name, skilltree.FIL_8.name, skilltree.FIL_9.name, skilltree.FIL_11.name … |
| LETHAL_VENOM | Смертельный яд | Lethal Venom | оставить |  |  |  |
| LIGHT_FOOT | Лёгкая стопа | Light Foot | оставить |  |  |  |
| LUNGE | Молниеносный выпад | Lunge | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| MALEVOLENCE | Злоба | Malevolence | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| MANA_HARVEST | Жатва маны | Mana Harvest | оставить* |  |  | совпадает с: skilltree.RAN_C4_0.name, skilltree.RAN_C4_4.name, skilltree.SCN_A_0.name, skilltree.WIT_C2_3.name |
| MARTYR | Мученик | Martyr | оставить |  |  |  |
| MOB_CHAOS_RING | Кольцо хаоса | Chaos Ring | оставить* |  |  | совпадает с: trait.TRAIT_SKILL_CHAOS_RING.name |
| MOB_ENFEEBLE | Слабость | Weakness | переименовать | Бессилие | Enfeeble | Одно и то же проклятие: умение героя «Бессилие / Enfeeble», умение монстра «Слабость / Weakness», черта «Слабость / Enfeeble». |
| MOB_FIREBALL | Огненный шар | Fireball | развести |  |  | дубль внутри категории: skill.FIREBALL.name; совпадает с: trait.TRAIT_SKILL_FIREBALL.name |
| MOB_HEAL | Исцеление | Mending | переименовать | Исцеление | Heal | en «Mending» vs черта «Heal». |
| MOB_ICE_BOLT | Ледяная стрела | Ice Bolt | оставить* |  |  | совпадает с: trait.TRAIT_SKILL_ICE_BOLT.name |
| MOB_MANA_DRAIN | Кража маны | Mana Drain | оставить* |  |  | совпадает с: trait.TRAIT_SKILL_MANA_DRAIN.name |
| MOB_SLAM | Удар по земле | Ground Slam | оставить* |  |  | совпадает с: trait.TRAIT_SKILL_SLAM.name |
| MOB_SPIT | Ядовитый плевок | Venom Spit | оставить* |  |  | совпадает с: trait.TRAIT_SKILL_SPIT.name |
| MOB_STONESKIN | Каменная кожа | Stoneskin | оставить* |  |  | совпадает с: skilltree.MAR_C1_2.name, trait.TRAIT_SKILL_STONESKIN.name, fate.STONE_SKIN.name |
| MOB_VULNERABILITY | Уязвимость | Vulnerability | развести |  |  | дубль внутри категории: skill.VULNERABILITY.name; совпадает с: trait.TRAIT_SKILL_VULNERABILITY.name |
| MOB_WARCRY | Клич ярости | Rage Cry | переименовать | Боевой клич | Warcry | Клич монстра «Клич ярости / Rage Cry», черта «Боевой крик / Warcry», черта TRAIT_HOWL «Боевой клич / War Howl» - три имени одного. |
| MOB_ZAP | Разряд | Zap | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_COIL_DISCHARGE, trait.TRAIT_SKILL_ZAP.name |
| ONSLAUGHT | Натиск | Onslaught | оставить* |  |  | совпадает с: skilltree.SCN_WA_12.name, skilltree.SCN_WA_5.name, skilltree.SCN_N3_0.name, skilltree.SCN_N3_1.name … |
| POISONED_TIPS | Ядовитые наконечники | Poisoned Tips | оставить |  |  |  |
| POWER_SIPHON | Вытягивание силы | Power Siphon | оставить |  |  |  |
| PRAYER | Молитва | Prayer | оставить |  |  |  |
| PRECISION | Точность | Precision | оставить* |  |  | совпадает с: skilltree.DUE_B5_1.name, skilltree.DUE_B5_2.name, skilltree.RAN_C3_0.name, skilltree.SCN_EE_2.name … |
| PURIFYING_FLAME | Очищающее пламя | Purifying Flame | оставить |  |  |  |
| PURITY_OF_ELEMENTS | Чистота стихий | Purity of Elements | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| RAIN_OF_ARROWS | Дождь стрел | Rain of Arrows | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| RALLYING_CRY | Клич единства | Rallying Cry | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| RENEWAL | Возрождение | Renewal | оставить |  |  |  |
| RESONANCE | Резонанс | Resonance | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_RUNE_RESONANCE, fate.RESONANCE.name |
| RETALIATION | Отмщение | Retaliation | оставить |  |  |  |
| RIGHTEOUSNESS | Праведность | Righteousness | оставить |  |  |  |
| RIPOSTE | Рипост | Riposte | оставить |  |  |  |
| RUTHLESS | Беспощадность | Ruthless | оставить |  |  |  |
| SECOND_WIND | Второе дыхание | Second Wind | оставить* |  |  | совпадает с: skilltree.MAR_C0_3.name |
| SHADOW_CLOAK | Покров теней | Shadow Cloak | оставить |  |  |  |
| SIPHON | Кража сил | Siphon | оставить |  |  |  |
| SMITE | Кара | Smite | оставить* |  |  | совпадает с: skilltree.RAN_C5_2.name |
| SNIPE | Прицельный выстрел | Snipe | оставить |  |  |  |
| SNIPERS_MARK | Метка снайпера | Sniper’s Mark | оставить |  |  |  |
| SPARK | Искра | Spark | оставить* |  |  | совпадает с: skilltree.RAN_C4_1.name |
| SPECTRAL_THROW | Спектральный бросок | Spectral Throw | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| SPLIT_ARROW | Расщеплённая стрела | Split Arrow | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| STEALTH | Незаметность | Stealth | оставить |  |  |  |
| STEEL_WHIRL | Вихрь стали | Steel Whirl | оставить* |  |  | совпадает с: skilltree.DUE_N1_5.name |
| STORM_CALL | Грозовой зов | Storm Call | оставить |  |  |  |
| SWIFTNESS | Проворство | Swiftness | оставить |  |  |  |
| SWIFT_CASTING | Беглые чары | Swift Casting | оставить |  |  |  |
| SWORDSMAN | Фехтовальщик | Swordsman | оставить |  |  |  |
| TEMPORAL_CHAINS | Путы времени | Temporal Chains | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| THICK_SKULL | Крепкий лоб | Thick Skull | оставить |  |  |  |
| TOXICOLOGIST | Ядовед | Toxicologist | оставить |  |  |  |
| TOXIC_CLOUD | Ядовитое облако | Toxic Cloud | оставить |  |  |  |
| UNBREAKABLE | Несокрушимость | Unbreakable | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| VANISH | Исчезновение | Vanish | оставить |  |  |  |
| VIPER_STRIKE | Змеиный укус | Viper Strike | оставить* |  |  | имя PoE «как есть» (общее слово; вопрос Q3) |
| VULNERABILITY | Уязвимость | Vulnerability | развести |  |  | дубль внутри категории: skill.MOB_VULNERABILITY.name; совпадает с: trait.TRAIT_SKILL_VULNERABILITY.name |
| WILL_TO_LIVE | Воля к жизни | Will to Live | оставить |  |  |  |
| WIND_VEIL | Покров ветра | Wind Veil | оставить |  |  |  |
| WRATH | Гнев небес | Wrath | переименовать | Гнев небес | Heaven's Wrath | en - PoE «Wrath»; ru уже своё. |
| MOB_REND | Разрыв | Rend | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_DISFAVOUR_REND, trait.TRAIT_SKILL_REND.name |
| MOB_WEB | Паутина | Web | оставить* |  |  | совпадает с: trait.TRAIT_SKILL_WEB.name |
| MOB_FROST_BREATH | Ледяное дыхание | Frost Breath | оставить* |  |  | совпадает с: trait.TRAIT_SKILL_FROST_BREATH.name |
| MOB_FLAME_BREATH | Огненное дыхание | Flame Breath | оставить* |  |  | совпадает с: trait.TRAIT_SKILL_FLAME_BREATH.name |
| MOB_THUNDERCLAP | Раскат грома | Thunderclap | оставить* |  |  | совпадает с: trait.TRAIT_SKILL_THUNDERCLAP.name |
| MOB_BONE_ARMOUR | Костяная броня | Bone Armour | оставить* |  |  | совпадает с: trait.TRAIT_SKILL_BONE_ARMOUR.name |

## Черты монстров (73)

Вердикты: оставить - 36, оставить* - 32, переименовать - 5

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| TRAIT_DISCIPLINE | Выучка | Discipline | оставить* |  |  | совпадает с: skilltree.DUE_B3_2.name, skilltree.DUE_C3_4.name, skilltree.DUE_C3_5.name, skilltree.SHA_B5_2.name … |
| TRAIT_CARAPACE | Панцирь | Carapace | оставить* |  |  | совпадает с: skilltree.TEM_C1_3.name |
| TRAIT_FLUTTER | Порхание | Flutter | оставить |  |  |  |
| TRAIT_COILED_STRIKE | Бросок из засады | Coiled Strike | оставить |  |  |  |
| TRAIT_SLIME | Вязкая слизь | Viscous Slime | оставить |  |  |  |
| TRAIT_PACK_HUNTER | Стайный охотник | Pack Hunter | оставить |  |  |  |
| TRAIT_VENOM | Яд | Venom | оставить* |  |  | совпадает с: skilltree.MAR_C4_3.name, skilltree.SHA_C1_0.name, skilltree.WIT_C3_5.name, skilltree.WIT_N2_0.name … |
| TRAIT_UNDYING | Нежить | Undying | переименовать | Неумирающий | Undying | ru «Нежить» (существо) ≠ en «Undying» (свойство). |
| TRAIT_STONE_BODY | Каменное тело | Stone Body | оставить |  |  |  |
| TRAIT_ETHEREAL | Бесплотность | Ethereal | оставить |  |  |  |
| TRAIT_BLOODLUST | Жажда крови | Bloodlust | оставить* |  |  | совпадает с: skill.BLOODLUST.name |
| TRAIT_STINGER | Жало | Stinger | оставить |  |  |  |
| TRAIT_SPORE_BURST | Споровый взрыв | Spore Burst | оставить |  |  |  |
| TRAIT_SWARM | Рой | Swarm | оставить* |  |  | совпадает с: rift.boon.RIFT_SWARM.name |
| TRAIT_HELLFIRE | Адское пламя | Hellfire | оставить |  |  |  |
| TRAIT_SHATTER | Осколки | Shatter | оставить |  |  |  |
| TRAIT_PLATING | Обшивка | Plating | оставить |  |  |  |
| TRAIT_DRAGON_SCALES | Драконья чешуя | Dragon Scales | оставить |  |  |  |
| TRAIT_RADIANCE | Сияние | Radiance | оставить* |  |  | совпадает с: skilltree.TEM_B5_2.name, skilltree.WIT_IN_2.name |
| TRAIT_PROVOKE | Провокация | Provocation | оставить |  |  |  |
| TRAIT_RENDING | Рваные раны | Rending | оставить |  |  |  |
| TRAIT_FROSTBITE | Обморожение | Frostbite | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_HUSH_FROSTBITE, skilltree.RAN_N3_0.name, skilltree.RAN_N3_1.name, skilltree.RAN_N3_3.name … |
| TRAIT_STATIC | Статика | Static | оставить |  |  |  |
| TRAIT_SMOULDER | Выжженная земля | Scorched Earth | оставить |  |  |  |
| TRAIT_LEECHING | Кровопийца | Leeching | оставить* |  |  | совпадает с: phase.VAMPIRE.name |
| TRAIT_THORNS | Шипы | Thorns | оставить |  |  |  |
| TRAIT_REGROWTH | Отрастание | Regrowth | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_ROOTS_REGROWTH |
| TRAIT_KEEN_EYE | Зоркий глаз | Keen Eye | оставить* |  |  | совпадает с: skill.EAGLE_EYE.name |
| TRAIT_SHIELD_WALL | Стена щитов | Shield Wall | оставить* |  |  | совпадает с: skilltree.DUE_C0_3.name, skilltree.TEM_N3_2.name |
| TRAIT_NIMBLE | Увёртка | Sidestep | оставить |  |  |  |
| TRAIT_IRONHIDE | Железная шкура | Ironhide | оставить |  |  |  |
| TRAIT_FRENZIED | Неистовство | Frenzied | оставить* |  |  | совпадает с: rift.mutator.MUT_FRENZIED.name |
| TRAIT_WARDED | Обереги | Warded | оставить* |  |  | совпадает с: guild.node.C_RESIST.name |
| TRAIT_PENETRATING | Пронзающая стихия | Piercing Elements | оставить |  |  |  |
| TRAIT_ENERGY_WEAVE | Энергетический покров | Energy Weave | оставить |  |  |  |
| TRAIT_CHILLING_TOUCH | Ледяное касание | Chilling Touch | оставить |  |  |  |
| TRAIT_BURNING_BLOOD | Горящая кровь | Burning Blood | оставить |  |  |  |
| TRAIT_STORMCHARGED | Заряд бури | Stormcharged | оставить |  |  |  |
| TRAIT_VOIDTOUCHED | Касание бездны | Voidtouched | переименовать | Касание Пустоты | Voidtouched | ru «Касание бездны» ≠ en «Void»; выбрать Пустоту (R6/R9) или Бездну по монстру. |
| TRAIT_FIRE_BURST | Огненная смерть | Fiery Death | оставить |  |  |  |
| TRAIT_VOLATILE | Нестабильность | Volatile | оставить |  |  |  |
| TRAIT_STORM_BURST | Грозовой разряд | Storm Discharge | оставить |  |  |  |
| TRAIT_LAST_STAND | Последний рубеж | Last Stand | оставить* |  |  | совпадает с: phase.LAST_STAND.name, enum.EnumStatStock.POWER_LAST_GROUND, enum.EnumStatStock.POWER_LAST_STAND |
| TRAIT_BERSERK | Берсерк | Berserk | оставить |  |  |  |
| TRAIT_AMBUSHER | Засада | Ambusher | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_AMBUSH, rift.boon.RIFT_AMBUSH.name, rift.curse.CURSE_AMBUSH.name |
| TRAIT_HOWL | Боевой клич | War Howl | переименовать | Вой стаи | Pack Howl | Разводим с «Боевым кличем» (умение). |
| TRAIT_DEATH_PRAYER | Предсмертная молитва | Dying Prayer | оставить |  |  |  |
| TRAIT_SKILL_FIREBALL | Огненный шар | Fireball | оставить* |  |  | совпадает с: skill.FIREBALL.name, skill.MOB_FIREBALL.name |
| TRAIT_SKILL_ICE_BOLT | Ледяная стрела | Ice Bolt | оставить* |  |  | совпадает с: skill.MOB_ICE_BOLT.name |
| TRAIT_SKILL_ZAP | Разряд | Zap | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_COIL_DISCHARGE, skill.MOB_ZAP.name |
| TRAIT_SKILL_SPIT | Ядовитый плевок | Venom Spit | оставить* |  |  | совпадает с: skill.MOB_SPIT.name |
| TRAIT_SKILL_WARCRY | Боевой крик | Warcry | переименовать | Боевой клич | Warcry | См. skill.MOB_WARCRY. |
| TRAIT_SKILL_ENFEEBLE | Слабость | Enfeeble | переименовать | Бессилие | Enfeeble | См. skill.MOB_ENFEEBLE. |
| TRAIT_SKILL_VULNERABILITY | Уязвимость | Vulnerability | оставить* |  |  | совпадает с: skill.MOB_VULNERABILITY.name, skill.VULNERABILITY.name |
| TRAIT_SKILL_SLAM | Удар по земле | Ground Slam | оставить* |  |  | совпадает с: skill.MOB_SLAM.name |
| TRAIT_SKILL_HEAL | Исцеление | Heal | оставить* |  |  | совпадает с: skill.MOB_HEAL.name, skilltree.DUE_C2_0.name, skilltree.MAR_C4_2.name, skilltree.TEM_C2_1.name |
| TRAIT_SKILL_STONESKIN | Каменная кожа | Stoneskin | оставить* |  |  | совпадает с: skill.MOB_STONESKIN.name, skilltree.MAR_C1_2.name, fate.STONE_SKIN.name |
| TRAIT_SKILL_MANA_DRAIN | Кража маны | Mana Drain | оставить* |  |  | совпадает с: skill.MOB_MANA_DRAIN.name |
| TRAIT_SKILL_CHAOS_RING | Кольцо хаоса | Chaos Ring | оставить* |  |  | совпадает с: skill.MOB_CHAOS_RING.name |
| TRAIT_SKILL_REND | Разрыв | Rend | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_DISFAVOUR_REND, skill.MOB_REND.name |
| TRAIT_SKILL_WEB | Паутина | Web | оставить* |  |  | совпадает с: skill.MOB_WEB.name |
| TRAIT_SKILL_FROST_BREATH | Ледяное дыхание | Frost Breath | оставить* |  |  | совпадает с: skill.MOB_FROST_BREATH.name |
| TRAIT_SKILL_FLAME_BREATH | Огненное дыхание | Flame Breath | оставить* |  |  | совпадает с: skill.MOB_FLAME_BREATH.name |
| TRAIT_SKILL_THUNDERCLAP | Раскат грома | Thunderclap | оставить* |  |  | совпадает с: skill.MOB_THUNDERCLAP.name |
| TRAIT_SKILL_BONE_ARMOUR | Костяная броня | Bone Armour | оставить* |  |  | совпадает с: skill.MOB_BONE_ARMOUR.name |
| TRAIT_VENOM_STING | Ядовитое жало | Venomous Sting | оставить |  |  |  |
| TRAIT_UNSHAKEN | Непоколебимый | Unshaken | оставить |  |  |  |
| TRAIT_ICE_SHELL | Ледяной панцирь | Ice Shell | оставить |  |  |  |
| TRAIT_GROUNDED | Заземлённый | Grounded | оставить |  |  |  |
| TRAIT_CRUSHER | Сокрушитель | Crusher | оставить |  |  |  |
| TRAIT_GORGE | Обжорство | Gorging | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_FEAST_GORGE |
| TRAIT_BILE_SERMON | Желчная проповедь | Bile Sermon | оставить |  |  |  |
| TRAIT_ROTSTALK | Гнилая охота | Rotstalk | оставить |  |  |  |

## Тотемы (12)

Вердикты: оставить - 11, оставить* - 1

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| TOTEM_RAGE | Тотем ярости | Rage Totem | оставить |  |  |  |
| TOTEM_BULWARK | Тотем оплота | Bulwark Totem | оставить |  |  |  |
| TOTEM_MENDING | Тотем исцеления | Mending Totem | оставить |  |  |  |
| TOTEM_HASTE | Тотем спешки | Haste Totem | оставить |  |  |  |
| TOTEM_ENFEEBLE | Тотем слабости | Weakness Totem | оставить |  |  |  |
| TOTEM_VULNERABILITY | Тотем уязвимости | Vulnerability Totem | оставить |  |  |  |
| TOTEM_WEB | Тотем паутины | Web Totem | оставить |  |  |  |
| TOTEM_WARBANNER | Боевое знамя | War Banner | оставить |  |  |  |
| TOTEM_FLAME | Тотем пламени | Flame Totem | оставить |  |  |  |
| TOTEM_FROST | Тотем стужи | Frost Totem | оставить |  |  |  |
| TOTEM_STORM | Тотем бури | Storm Totem | оставить |  |  |  |
| TOTEM_RIFT_IDOL | Каменный идол | Stone Idol | оставить* | Каменный идол | Stone Idol | Оставить: совпадает с обликом Стража Врат (RULES: два каменных идола). |

## Фазы боссов (10)

Вердикты: оставить* - 5, оставить - 5

| Код | Сейчас ru | Сейчас en | Вердикт | Предложение ru | Предложение en | Обоснование |
|---|---|---|---|---|---|---|
| RAGE | Ярость | Rage | оставить* |  |  | совпадает с: skilltree.DUE_N1_0.name, skilltree.DUE_N1_1.name, skilltree.DUE_N1_3.name, rift.boon.RIFT_FURY.name |
| SUMMON | Свита | Retinue | оставить |  |  |  |
| AEGIS | Эгида | Aegis | оставить* |  |  | совпадает с: skilltree.SCN_TM_13.name, skilltree.SCN_TM_6.name |
| HEXER | Чародей | Hexer | оставить |  |  |  |
| VAMPIRE | Кровопийца | Bloodsucker | оставить* |  |  | совпадает с: trait.TRAIT_LEECHING.name |
| STORM | Буря | Storm | оставить* |  |  | совпадает с: atlas.node.ATLAS_POWER_B01.name, atlas.node.ATLAS_POWER_B02.name, atlas.node.ATLAS_POWER_B04.name, atlas.node.ATLAS_POWER_B05.name … |
| VENOM | Отравитель | Poisoner | оставить |  |  |  |
| LAST_STAND | Последний рубеж | Last Stand | оставить* |  |  | совпадает с: enum.EnumStatStock.POWER_LAST_GROUND, enum.EnumStatStock.POWER_LAST_STAND, trait.TRAIT_LAST_STAND.name |
| TOTEM | Шаман | Shaman | оставить |  |  |  |
| BLIGHT_CORDS | Сердце Скверны | Heart of Blight | оставить |  |  |  |
