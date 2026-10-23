# B: предметы и крафт (сервер, пути от backend/, R=rules/src/main/kotlin/com/sperance/exileforge/rules, C=src/main/resources/content)
#529 Требование R/sheet/Sheet.kt:597 Requirements.level = max(база, 0.8*ур.высшего тира) для волш/редк (5e5b7df, 0.17.3); тиры по ур. предмета уже (Rolls.kt:133). A требование=ур.предмета; B ур.-3; C только одно число в карточке. Крайние: надетые гаснут, закалка, уникалки. 3-4 ч.
#530 Месторождения TOPAZ/SAPPHIRE/RUBY/ONYX_DEPOSIT C/professions.json:117-230; находки extra Work.kt:413; CraftLadder.kt:23. A новая группа «камень» у жил + осколки 10→1, убрать месторождения (8-12 ч, иконки, миграция hero.work); B строки extra; C оставить + находки. Баланс камней в ~25 рецептах.
#532 STONE_POLISHING в MINING professions.json:79 → SMITHING, миграция hero.work. 0.5-1 ч.
#533 Orbs.kt:461 chance 5% уникалка, иначе tables.json:8837. Гипотеза: волшебная ветка на служебной фляге падает отказом CR_023 Orbs.kt:105, откат → видны только уникалки. Замер-тест 10к. 1-2 ч.
#551 CHART_BAND_1..6, уровень от зоны Loot.kt:334-338, CraftsService.kt:385. A выбор уровня до героя+2, вход ×2^(floor((ур-1)/10)) (или ×1.5); B одна работа; C ур.=герою. ActiveWork.choice. 4-6 ч, строка выбора.
#552 база вес 100 tables.json:3021, пояса 6 баз → ~3%. A сначала слот по весам (rules.json), потом база; B вес поясов 200-250; C новые базы.
#560 heroClass Equipment.kt:237, barred Sheet.kt:610; 14 классовых (по 2). +2-3 на класс, POWER_ строки, спрайты, локаль. 12-20 ч, владельцу до часа.
