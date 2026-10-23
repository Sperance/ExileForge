# C: развитие и поход (сервер 727ae5d)
#548 Метка «!N» MapMarks.kt:144 только число; причины тост только тестерам (SessionRepository.kt:61); ENGAGE-отказы вырезаны из тоста но считаются в метку (ExpeditionActions.kt:460-464, RunJournal.kt:97). Вероятно FIGHT по ENGAGE в Ваал-зоне CampaignService.kt:731; в логе [KILL] no such monster vaal (CampaignService.kt:528,681) - рассинхрон Ваал-стаи. A хранить причину по событию, ENGAGE не считать, тост всем (3 ч); B + серверный WARN; C убрать метку игрокам.
#547 TOXIC_CLOUD дефолт ENEMIES_3 (skills.json:2253, BattleSkills.kt:56), 3 врага ~21% боёв (rules.json:366). Слоты по порядку, casting блокирует (BattleSkills.kt:68-82). 8 умений с ENEMIES_3. A ENEMIES_2; B дефолт READY; C пояснение.
#545 SHA_C0_5 tree.json:8872 -20% урона ударами, единственный NOTABLE с чистым штрафом; задумано backend/RULES.md:65. Реальная потеря ~-8% при +150%. Убрать / -10% / в ключевой / подсветить.
#546 яд = хаос по RULES.md:65, ошибки нет. A текст «Яд: урон хаосом..» + журнал; B отдельная стихия (8+ ч).
#553 TreeCanvas.kt:332-335 гнездо одинаково с/без самоцвета; hero.jewels Hero.kt:135. Облик пустого гнезда (+счётчик). Макет.
#540 StatsSheet.kt:37-85 TabRow (нарушает RULES.md:54 StoneTabs), вкладки Герой/На карте/В бою. A один итоговый список с источниками. Макет.
