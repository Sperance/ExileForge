# Только для `-PuiTests` (4.12.x): все UI-тесты идут на сборке minifiedTest, а они зовут экраны, модели и правила приложения
# напрямую - код приложения и сами тесты не ужимаются. Библиотеки R8 ужимает как в релизе; строгий смоук R8 - сборка без флага.
-keep class com.sperance.exileforge.** { *; }
-keepclassmembers class com.sperance.exileforge.** { *; }
