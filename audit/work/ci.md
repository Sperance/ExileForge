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
- [ ] клиент p4-cli (без LocaleAudit в UiStringsTest - после закрепления сервера)
