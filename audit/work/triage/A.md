# A: бой и испытания
#534 RunFights.kt:224-227 после волны Бездны report=null, фаза ABYSS; lastFight есть. A кнопка «Итоги волны» в AbyssSheet → FightStatsSheet + журнал (как RiftScreen.kt:1388). 1.5-2 ч, малый макет.
#543 9a5faac (+30% боссам, фаза сразу, #521) + tower.leaders campaign.json:11163 (этаж 5 урон ×1.304) + CHASM_PIT_MASTER power 1.929 → удар REND 865 при HP 769. A пересчитать tower.leaders 5-15 по mobBal trials; B без мгновенной фазы у вождей; C откат +30% у вождей. 2-3 ч.
#544 ui_ru.json:1333 fight.feed_invite. 10 мин.
#547 очередь слотов BattleSkills.kt:72-82; A подсказка; B условные слоты раньше «как готово»; C считать подкрепление.
#554 BossStage.kt:108-113,123 win Animatable без ключа босса, гашение 900 мс обрывается при aftermath 0.8 с на ×2/×3. remember(boss.index)+snapTo. 0.5 ч.
#557 campaign.json:11150-11156 ступени -22,-17,-13,-3,0; Trials.kt:26 RushTarget. A 0/+20/+45/+75/+110 (+ RushTarget, reward); B поднять верх; C ступени VI-VIII. 2-3 ч.
#558 у 107/166 боссов есть сопротивления; без силы 5 боссов. Метки сил / чипы. Объединить с #561. Где видел «3%»?
#559 Run.kt:1029-1050 сферы [1,2]×reward(1..4.5), orbs:abyss богатая. A orbs [0,1], reward 1/1.3/1.7/2.2/3; B таблица orbs:rush; C потолок. V ~40 → ~15.
#561 86 мобов: 4 без силы, 21 одна, 38 две, 20 три. Правило RULES.md + поле strengths + проверка; пройти всех. 8-14 ч.
