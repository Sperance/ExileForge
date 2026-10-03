# Особые силы · N

## POWER_NECTAR_RUSH <a href="#power_nectar_rush" id="power_nectar_rush"></a>

### Прилив нектара <a href="#прилив-нектара" id="прилив-нектара"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия умение сокращает перезарядку ваших умений на {v} с |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Прилив нектара](../Stats/Stats-POWER.md#power_nectar_rush) |
| Событие · `on` | `SKILL_USE` |
| Эффекты · `effects` | Действие: `COOLDOWNS` |


## POWER_NOON_GLARE <a href="#power_noon_glare" id="power_noon_glare"></a>

### Полуденный блеск <a href="#полуденный-блеск" id="полуденный-блеск"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Критический удар с шансом {v}% поджигает |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Полуденный блеск](../Stats/Stats-POWER.md#power_noon_glare) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `CRIT` |
| Эффекты · `effects` | Действие: `AILMENT`; Состояние: `IGNITE` |


## POWER_NIGHTFALL <a href="#power_nightfall" id="power_nightfall"></a>

### Сумерки <a href="#сумерки" id="сумерки"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убийство даёт {v}% увеличения уклонения на 4 с, до 3 раз |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Сумерки](../Stats/Stats-POWER.md#power_nightfall) |
| Событие · `on` | `KILL` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Уклонение](../Stats/Stats-HERO.md#stock_evasion); Операция: `INCREASED`; Длительность: 4; stacks: 3 |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
