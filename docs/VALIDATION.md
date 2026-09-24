# Проверка Exile Forge

GitHub Actions выполняет `:core:test`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`
и `:app:assembleRelease` (R8 не должен выкинуть сериализаторы). Затем эмулятор Android 35 запускает
UI-тесты и сохраняет снимки экранов.

С 2.54.0 остались только критически важные тесты (решение владельца):

- `UiStringsTest` — словарь клиента `ui_{ru,en}.json` и `ui_common.json`: одинаковые наборы ключей,
  нет пустых строк, плейсхолдеры не теряются, каждый `ui("...")` из исходников есть в словаре, у
  каждого значения перечислений есть строка.
- `SheetTest` — лист персонажа, сложенный на клиенте, совпадает с формулой сервера.
- `CraftCycleTest` — цикл ремесла бросается тем же генератором и в том же порядке, что на сервере
  (пара к серверному `CraftsTest`).
- `ServerIntegrationTest` — только в задании `client-server`: закреплённый бэкенд, изолированный
  MongoDB и настоящий `GameApi` против живого сервера.
- UI: `DesignPreviewTest` и `HeroPanelTest` снимают `design/arsenal.jpg` и `design/hero.jpg` —
  workflow без этих файлов падает.

Артефакты: `exile-forge-debug` (APK), `exile-forge-ui` (результат UI-тестов и снимки),
`client-server-reports` (отчёты живого теста и лог сервера).
