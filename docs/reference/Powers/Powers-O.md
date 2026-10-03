# Особые силы · O

## POWER_OFFCUT_LUCK <a href="#power_offcut_luck" id="power_offcut_luck"></a>

### Удача обрезков <a href="#удача-обрезков" id="удача-обрезков"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% увеличения количества предметов с волшебных монстров |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Удача обрезков](../Stats/Stats-POWER.md#power_offcut_luck) |
| world | gain: `QUANTITY`; against: `MAGIC` |


## POWER_ORACLE_CURSE <a href="#power_oracle_curse" id="power_oracle_curse"></a>

### Нежное касание <a href="#нежное-касание" id="нежное-касание"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | {v}% шанс проклясть цель при ударе |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Нежное касание](../Stats/Stats-POWER.md#power_oracle_curse) |
| Тип броска · `roll` | `CHANCE` |
| Событие · `on` | `HIT` |
| Эффекты · `effects` | Действие: `CURSE` |


## POWER_ORACLE_DOOM <a href="#power_oracle_doom" id="power_oracle_doom"></a>

### Назначенная смерть <a href="#назначенная-смерть" id="назначенная-смерть"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Убитые проклятые враги взрываются: {v}% их макс. здоровья уроном хаосом по остальным |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Назначенная смерть](../Stats/Stats-POWER.md#power_oracle_doom) |
| Событие · `on` | `KILL` |
| checks | check: `TARGET_CURSED` |
| Эффекты · `effects` | Действие: `DAMAGE`; Тип: `CHAOS`; of: `TARGET_LIFE`; to: `OTHERS` |


## POWER_OAK_BARK <a href="#power_oak_bark" id="power_oak_bark"></a>

### Обветренная кора <a href="#обветренная-кора" id="обветренная-кора"></a>


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Обветренная кора](../Stats/Stats-POWER.md#power_oak_bark) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Урон стихии наибольшего сопротивления](../Stats/Stats-HERO.md#stock_highest_resist_element_taken) |


## POWER_OAK_ROOT <a href="#power_oak_root" id="power_oak_root"></a>

### Глубокий корень <a href="#глубокий-корень" id="глубокий-корень"></a>

**Чтение свойства** (`{v}` — значение):

| Операция | Игровой текст |
| --- | --- |
| ADD | Во время действия ваш урон стихией вашего наименьшего сопротивления пробивает {v}% сопротивления |


| Параметр | Значение |
| --- | --- |
| Характеристика · `stat` | [Глубокий корень](../Stats/Stats-POWER.md#power_oak_root) |
| Событие · `on` | `STANDING` |
| Эффекты · `effects` | Действие: `BUFF`; Свойства: Характеристика: [Пробивание стихии наименьшего сопротивления](../Stats/Stats-HERO.md#stock_lowest_resist_penetrate) |

---
Основание: [актуальный исходник](https://github.com/Sperance/ktor-bestgame/blob/fa8c3991681e20ef4d62548bf954d0b031a905a5/src/main/resources/content/powers.json), срез `fa8c399` / клиент `9faca17`, 03.10.2026.

[↑ На главную](../../README.md) · [Все страницы](../../Catalogs.md)
