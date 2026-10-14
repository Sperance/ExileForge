# Только для `-PuiTests` (4.12.x): все UI-тесты идут на сборке minifiedTest, а они зовут экраны, модели и правила приложения
# напрямую - код приложения и сами тесты не ужимаются. Строгий смоук R8 - сборка без флага.
-keep class com.sperance.exileforge.** { *; }
-keepclassmembers class com.sperance.exileforge.** { *; }

# Тесты сами рисуют Compose (фон, текст, иконки) и зовут библиотеки приложения: APK тестов ссылается на них по именам, а R8
# ужимал и переименовывал их в APK приложения (0.13.0: NoClassDefFoundError `BackgroundKt`, NoSuchMethodError `Text`).
# Библиотеки, на которые ссылаются тесты, - целиком и без переименования.
-dontobfuscate
-keep class androidx.** { *; }
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-keep class org.koin.** { *; }
-keep class okhttp3.** { *; }
-keep class okio.** { *; }
