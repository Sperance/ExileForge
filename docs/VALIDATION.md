# Результат проверки поставки

Проверка выполнена 2026-09-08 UTC. Серверный контракт: ed33cab6f215d7d0d7ff48aec025f842c98796ac (0.8.8).

## Выполнено

- Gradle 8.13 / JDK 17 / Android SDK 35.
- `:core:test`: **17 тестов, 0 failures, 0 errors** (14 HTTP/contract + 3 CRUD scenario).
- `:app:assembleDebug`: **успешно**, APK включён в корень архива.
- `:app:lintDebug`: **0 ошибок, 3 предупреждения**.
- `:app:assembleDebugAndroidTest`: **успешно**, instrumentation-тест скомпилирован.
- `apksigner verify`: подпись debug APK проверена успешно.
- Финальный запуск сборки выполнен с `--offline`, завершён `BUILD SUCCESSFUL`.

Предупреждения lint: OldTargetApi (targetSdk 35), GradleDependency (доступен compileSdk 36), DataExtractionRules (для Android 12+ можно явно настроить перенос данных). В этой поставке сохранена фиксированная конфигурация SDK 35; хранятся только настройки адреса сервера. Предупреждения не подавлялись.

## Не выполнялось

- Запуск приложения и Compose-теста на Android-устройстве/эмуляторе, визуальная проверка экранов на устройстве.
- Интеграционные запросы к развёрнутому серверу пользователя: рабочий URL не предоставлен.
- Проверка release APK, подпись релизным ключом или публикация.

Unit-тесты используют локальный MockWebServer и fake repository. Это проверяет клиентский контракт и сценарии, но не доказывает исправность развёрнутого Ktor/MongoDB. Встроенный экран «Проверки» предназначен для интеграционной проверки после подключения.

## APK

Имя в архиве: `ExileForge-debug.apk`.
Application ID: `com.sperance.exileforge`, version 1.0.0, minSdk 26, targetSdk 35.
Debug-сборка допускает HTTP для локальной разработки.

SHA-256: `cf44700e74bbe2909b23647e273e7a8b588acb8d7fb1d47cb022806711625529`
