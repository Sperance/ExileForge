# Проверенный контракт сервера

Источник: [commit ed33cab6f215d7d0d7ff48aec025f842c98796ac](https://github.com/Sperance/ktor-bestgame/tree/ed33cab6f215d7d0d7ff48aec025f842c98796ac).
Исследованы Constants.kt, BaseRoute.kt, BaseRepository.kt, Items/Equipment Route и Repository, модели Items/Weapon/Armor/Accessory/Equipment, Modifier, EnumModifierDefinitions, Serialization, StatusPage, Routing, Security.

| Операция | Метод и путь | Тело / ответ data |
|---|---|---|
| Получить все | GET `/api/v1/items` или `/api/v1/equipment` | Массив |
| Получить один | GET `<base>?id=<24 hex>` | Объект либо null |
| Страница | GET `<base>/paged?page=0&size=20` | items, page, pageSize, totalItems, totalPages |
| Количество | GET `<base>/count` | `{"count":123}` |
| Создать | POST `<base>` | **Массив** объектов → массив созданных |
| Изменить | PUT `<base>?id=<24 hex>` | Частичный JSON-объект → объект |
| Удалить | DELETE `<base>?id=<24 hex>` | `"Deleted"` |
| Здоровье | GET `/system/health` | Строка с состоянием |

`<base>` — один из двух каталогов. Приложение использует paged для списка.

Обёртка: `{"success":true,"data":...,"error":null}`. Ошибка: `{"success":false,"data":null,"error":{"message":"...","errorCode":"...",...}}`. Недостаточно проверить только HTTP-статус: `success=false` обрабатывается как ошибка даже при HTTP 200.

## Создание обычного предмета

POST `/api/v1/items`

```json
[{"name":"Осколок древних","category":"Currency","subCategory":"Shard","description":"Тест","price":1}]
```

## Создание оружия

POST `/api/v1/equipment`

```json
[{
  "type":"features.data.equipment.equipment_data.Weapon",
  "name":"Наследие изгнанника",
  "slot":"WEAPON_1H",
  "weaponType":"SWORD",
  "damage_min":10.0,
  "damage_max":20.0,
  "attackSpeed":1.2,
  "durability":100,
  "rarity":"RARE",
  "itemLevel":30,
  "description":"Тестовый меч",
  "modifiers":[{"type":"PREFIX_ADD_HEALTH","value":42.0,"tier":2}],
  "modifierDefinitions":["PREFIX_ADD_HEALTH","SUFFIX_ADD_STRENGTH"],
  "modifierDefinitionsStock":[]
}]
```

Для брони discriminator заканчивается `.Armor`, необходимо поле `defense`; у `.Accessory` дополнительных обязательных полей кроме slot нет. `type` — стандартный discriminator kotlinx.serialization для sealed Equipment, поэтому нужны полные серверные package names. Для редкости используются серверные COMMON, UNCOMMON, RARE, EPIC, LEGENDARY, MYTHICAL.

## Изменение модификаторов

PUT `/api/v1/equipment?id=0123456789abcdef01234567`

```json
{"modifiers":[{"type":"PREFIX_ADD_HEALTH","value":42.0,"tier":2}]}
```

Удаление всех модификаторов: `{"modifiers":[]}`. Изменение имени: `{"name":"Новое имя"}`. Не отправляются `_id`, `id`, `version`, `deleted`, `createdAt`, `updatedAt`, а также `type`: серверный BaseRoute фильтрует системные поля, но сам discriminator не фильтрует. Клиент исключает его, чтобы не записывать его как обычное поле MongoDB.

Серверная валидация `id` проверяет длину; клиент дополнительно проверяет шестнадцатеричные символы. Клиент валидирует числовые типы, конечность значений, tier 1..8, известные enum-имена и обязательные поля. Это не замена серверной валидации. Редкости, типы, формулы и ограничения текущего сервера отличаются от оригинальной Path of Exile.

`image` сохраняется как URL/nullable поле. В карточках используются собственные векторные символы; загрузка удалённых картинок в этой версии не реализована.
