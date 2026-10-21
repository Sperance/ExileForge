# Пакет 4: проверки и CI (без Issue)
Статус: в работе; сервер: да (deploy.yml, build.gradle.kts, тесты); дизайн: нет

## Решения владельца
- Все безрисковые пункты 106-115 из обзора: сервер - `test` вместо белого списка `--tests`, убрать Dokka, configuration cache + maxParallelForks, IntegrityTest на виртуальное время, check.sh; клиент - входы `:core:test`, убрать setup-android, fetch-depth 1 + unshallow сабмодуля, гейт на job app, check.sh, маркеры @OutputFile у сторожей.
- Снимки KeyScreensShotTest - только по ручному запуску `shots: true` (RULES.md:290 поправить).
- Слияние minified+minified-strict и image после checks - не трогать (скорость важнее минут).
- Тесты: словарные проверки в rules (LocaleAudit) для UiStringsTest и LocalizationTest; переименовать клиентский CombatRulesTest; объединить кластер «сводка героя» (5 классов); разбить RulesInvariantsTest по областям.
- CLAUDE.md не правим (только владелец); ссылки на check.sh - в RULES.md.

## План и коммиты
- [ ] сервер p4-srv
- [x] клиент p4-cli (без LocaleAudit в UiStringsTest - после закрепления сервера):
  - c6164253 `ci: declare the inputs core tests read outside the classpath` - `core/build.gradle.kts`: `:core:test` знает исходники app, контент и локаль сабмодуля.
  - d04aad81 `ci: let the :app source guards be UP-TO-DATE` - `app/build.gradle.kts`: маркер `build/<task>.ok` у `androidOnly`/`forgeButtonsOnly`, общий `guard<T>()`.
  - b5f2227c `ci: shallow checkouts, no setup-android, app gate, shots on demand` - `android.yml`: без `setup-android` и `fetch-depth: 0` (сабмодуль дотягивается `fetch --unshallow`), гейт job `app`, `KeyScreensShotTest` и превью в логе только при `shots: true`, версия эмулятора в ключе кэша AVD.
  - 0f731e17 `ci: one check script for the workflow and the pre-push check` - `scripts/check.sh core|app|all [--no-compile]`, job `core`/`app` зовут его; RULES.md «CI».
  - a68ba2bf `test: rename the client CombatRulesTest to ClientCombatTest`.
  - 05db1ca5 `test: merge the hero sheet guards into HeroSheetTest` - пять классов в один на общих фикстурах; RULES.md. В `backend/RULES.md`:10 остались имена `SheetCompletenessTest`/`EquippedModifiersTest` клиента - поправить на сервере.
