# E: интерфейс (клиент, сервер не нужен)
#538 баг: кэш ItemViews (core sheet/SheetView.kt:45,242-266) сбрасывается только по ContentIndex, не по serverLocale; ItemView.kt:312-409 lazy тексты. С fe2e1748 (0.17.0). Фикс: ключ кэша + serverLocale. Тест. 0 мин/1 ч.
#531 WearPreview.kt:91-94 wearing/Comparison/LostLines в главном потоке, пересчёт по game.hero (LootSheet.kt:72 flushRun). Фикс: produceState на Default как GearVerdictView.kt:37. 2-3 ч. Вопрос: сравнение на долю секунды позже - ок?
#535 zoomTools CanvasToolbar.kt:76-79: ExpeditionScreen.kt:129, AtlasScreen.kt:148, RunMap.kt:93-100 (у мини-карты щипка нет). RULES.md:21. 0.5 ч. Вопрос: мини-карта?
#537 ItemCard.kt:190 chipPrice, RelicChips :267-280; RULES.md:101. Цену убрать, качество (QualityBadge) - а) правая колонка под RollRing б) угол гнезда в) строка тип·редкость. Макет. 1.5 ч.
#549 Toasts.kt:58-66 сверху, BANNER_TOAST_TOP 60dp ForgeApp.kt:241,324, перекрывает HeroChrome, 4 c, кликабелен. A вниз над навигацией; B сквозь касания; C компактнее. 1-2 ч.
#555 TrialScreen.kt:134-161 LootSheet: «Надеть» только в походе (LootSheet.kt:69,85-89). Сервер не запрещает (GearChange.kt:14-29 requireOpen, CH_044 только поход по карте и Разлом). Hero.kt:132 inRun включает trials.open - проверить. A испытания+Разлом+Бездна. 1.5-2 ч.
#556 RewardLines.kt:41-83 (испытания, Разлом, Бездна, поражение) и ChestOpenedSheet:62 vs Spoils ReportScreen:295-332 (LootRow+StackCell сетка). A общий LootPanel; B + письма/ремёсла/задания; C вещи плитками (RULES.md:108). 3-4 ч / 6-8 ч.
