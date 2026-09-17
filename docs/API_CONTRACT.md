# Контракт Exile Forge 1.11

Сервер: `master`, коммит `f0d88446254b1f3d3ff1a06a6e609471ba99f97e` (0.13.0, API revision 4).
Успех: `{"success":true,"data":...}`. Ошибка может быть ApiMongoResponse либо пустым HTTP 4xx/5xx; статус сохраняется клиентом.

| Операция | Запрос |
|---|---|
| Вход | `POST /api/v1/poe/token`, `{login,password}` |
| Поиск определений | `GET /api/v1/poe/modifier-definitions?q=life&page=0&size=50` |
| Точная версия | `GET /api/v1/poe/modifier-definition?id=...&revision=1` |
| Публикация ADMIN | `POST /api/v1/poe/modifier-definitions`, `{definition,expectedRevision}` |
| Инвентарь владельца | `GET /api/v1/poe/characters/{id}/inventory` → `{version,equipment}` |
| Сферы | `GET /api/v1/poe/currencies` → `[{id,name,itemId,icon}]` |
| Тестовое выпадение ADMIN | `POST /api/v1/poe/characters/{id}/drop`, `{requestId,expectedVersion}` |
| Крафт | `POST /api/v1/poe/characters/{id}/craft`, `{requestId,expectedVersion,equipmentUuid,currency}` |
| Возможности | `GET /api/v1/poe/capabilities` → `{apiRevision:4,…,icons,iconSet,iconSetRevision,iconSetVersion,iconCount}` |
| Набор иконок | `GET /api/v1/icons`, `/api/v1/icons/bindings`, `/api/v1/icons/sprite.svg`, `/api/v1/icons/{id}.svg?variant=plain` |

Защищённые операции отправляют `Authorization: Bearer <token>`. JWT не добавляется к публичному каталогу, маршрутам иконок и legacy CRUD. В журнале вход полностью скрыт, а SVG записывается размером, а не телом.

Иконки отдаются как `image/svg+xml` с `ETag` и `Cache-Control: public, max-age=604800, immutable`; `If-None-Match` даёт `304`. Каталог, определения модификаторов, сферы, экипировка, предметы, узлы дерева, зоны и монстры содержат поле `icon` — идентификатор набора, а не ссылку на чужой хост. У документов, записанных до появления набора, поля нет: иконка выбирается по таблицам `/api/v1/icons/bindings`. `icon` разрешён к записи в equipment и items; неизвестный идентификатор отклоняется с `400`. [Подробности](ICONS.md).

Пример ссылок шаблона:
```json
{"modifierDefinitionRefs":[{"definitionId":"custom/life","revision":2}],"stockModifierDefinitionRefs":[]}
```
Выпавший legacy-совместимый модификатор содержит `definitionId`, `definitionRevision`, `values:[{value:42}]`, `tier`, `source`, `tags`.
В PoE-состоянии экземпляра `implicits`/`explicits` содержат `{id,revision,values:[42],fractured}`. Клиент сохраняет и отображает эти версии; не пересчитывает PoE-роллы локально.

Публикация создаёт новую версию. Для нового ID `expectedRevision=0`; для существующего — последняя версия. Неизменяемые старые версии остаются доступными. Пользовательский редактор не переписывает raw PoE-каталог.

Результат крафта/выпадения: `{requestId,characterVersion,equipment,currencyRemaining?}`. Mirror может вернуть новый UUID; клиент сохраняет исходный экземпляр и добавляет копию. После успешной операции инвентарь перечитывается. При сетевой ошибке/5xx сохраняется исходное тело для повтора, включая ожидаемую версию. Оно сохраняется в DataStore до отправки. После рестарта JWT нужно получить заново тем же пользователем. При явном отклонении 400/409/422 необходимо перечитать инвентарь.

Legacy каталоги: `items`, `equipment`, `character`; `GET /api/v1/{catalog}/paged?page=0&size=20`, `GET/PUT/DELETE /api/v1/{catalog}?id=...`, создание `POST` JSON-массивом. PUT отправляет только изменённые поля, исключая служебные. Определения запрещены внутри новых equipment-записей; API проверяет ссылочную структуру до отправки.
