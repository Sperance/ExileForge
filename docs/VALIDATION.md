# Проверка версии 1.2

Добавлены 7 тестов: все варианты форм эффектов/условий/выражений, поля и числовые границы персонажа, генерация, связь выбранных модификаторов с определениями и HTTP-контракт `/api/v1/character`.

[GitHub Actions: BUILD SUCCESSFUL](https://github.com/Sperance/ExileForge/actions/runs/34400501968), проверенный коммит `e8c89b44683b533076f3ff998339fbe0402064a1`.

- 30 JVM-тестов пройдены: 16 HTTP/contract, 5 modifier contract, 3 CRUD scenario, 6 editor/generation.
- `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest` — успешно.
- [APK 1.2.0, versionCode 3](https://github.com/Sperance/ExileForge/actions/runs/34400501968/artifacts/10123319583).
- На Android-устройстве и против работающего пользовательского сервера тесты не запускались. Instrumentation-тест собран, но не исполнялся.
- Следующий коммит меняет только документацию. Результаты ниже относятся к предыдущей версии.

---

# Проверка обновления от 2026-09-09

Сервер: `5fb037f3ba6a60f5e45da9da35832e2165339432`.

- Контракт сопоставлен с исходниками маршрутов, моделей и репозиториев сервера.
- Добавлены проверки нового формата, вложенных определений и передачи JSON через HTTP. CRUD-сценарий расширен до добавления, изменения и удаления модификаторов.
- `git diff --check` пройден.
- Локально `:core:test`: 23 теста, 0 failures, 0 errors, 0 skipped (15 HTTP/contract + 5 modifier contract + 3 CRUD scenario). Gradle 9.7.1, JDK 17.
- [GitHub Actions, run 34332742008](https://github.com/Sperance/ExileForge/actions/runs/34332742008): **BUILD SUCCESSFUL** для коммита `a2df7259c614fcabae13db1edb7f6eca7a5d9244`.
- `:core:test`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest` завершились успешно. Instrumentation-тест скомпилирован, но не запускался на устройстве.
- [Новый debug APK 1.1.0 (versionCode 2)](https://github.com/Sperance/ExileForge/actions/runs/34332742008/artifacts/10096546106).
- Работа с запущенным Ktor/MongoDB и запуск на Android-устройстве не проверены: адрес API не предоставлен.
- APK в корне репозитория и `test-results.json` — предыдущие артефакты, они не содержат это исправление. Новый APK получается из успешной сборки текущего коммита.

---

## Архивный отчёт предыдущей поставки (не относится к текущему коду)

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
