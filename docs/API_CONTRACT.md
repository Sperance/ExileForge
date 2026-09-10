# Контракт Exile Forge 1.3

Сервер: `feature/poe-catalog-crafting`, коммит `34ca93a099d1eb2f56f5ab53114c27bf1b005f49`.
Успех: `{"success":true,"data":...}`. Ошибка может быть ApiMongoResponse либо пустым HTTP 4xx/5xx; статус сохраняется клиентом.

| Операция | Запрос |
|---|---|
| Вход | `POST /api/v1/poe/token`, `{login,password}` |
| Поиск определений | `GET /api/v1/poe/modifier-definitions?q=life&page=0&size=50` |
| Точная версия | `GET /api/v1/poe/modifier-definition?id=...&revision=1` |
| Публикация ADMIN | `POST /api/v1/poe/modifier-definitions`, `{definition,expectedRevision}` |
| Инвентарь владельца | `GET /api/v1/poe/characters/{id}/inventory` → `{version,equipment}` |
| Сферы | `GET /api/v1/poe/currencies` → `[{id,name,itemId}]` |
| Тестовое выпадение ADMIN | `POST /api/v1/poe/characters/{id}/drop`, `{requestId,expectedVersion}` |
| Крафт | `POST /api/v1/poe/characters/{id}/craft`, `{requestId,expectedVersion,equipmentUuid,currency}` |

Защищённые операции отправляют `Authorization: Bearer <token>`. JWT не добавляется к публичному каталогу и legacy CRUD. В журнале вход полностью скрыт.

Пример ссылок шаблона:
```json
{"modifierDefinitionRefs":[{"definitionId":"custom/life","revision":2}],"stockModifierDefinitionRefs":[]}
```
Выпавший legacy-совместимый модификатор содержит `definitionId`, `definitionRevision`, `values:[{value:42}]`, `tier`, `source`, `tags`.
В PoE-состоянии экземпляра `implicits`/`explicits` содержат `{id,revision,values:[42],fractured}`. Клиент сохраняет и отображает эти версии; не пересчитывает PoE-роллы локально.

Публикация создаёт новую версию. Для нового ID `expectedRevision=0`; для существующего — последняя версия. Неизменяемые старые версии остаются доступными. Пользовательский редактор не переписывает raw PoE-каталог.

Результат крафта/выпадения: `{requestId,characterVersion,equipment,currencyRemaining?}`. Mirror может вернуть новый UUID; клиент сохраняет исходный экземпляр и добавляет копию. После успешной операции инвентарь перечитывается. При сетевой ошибке/5xx сохраняется исходное тело для повтора, включая ожидаемую версию. Оно сохраняется в DataStore до отправки. После рестарта JWT нужно получить заново тем же пользователем. При явном отклонении 400/409/422 необходимо перечитать инвентарь.

Legacy каталоги: `items`, `equipment`, `character`; `GET /api/v1/{catalog}/paged?page=0&size=20`, `GET/PUT/DELETE /api/v1/{catalog}?id=...`, создание `POST` JSON-массивом. PUT отправляет только изменённые поля, исключая служебные. Определения запрещены внутри новых equipment-записей; API проверяет ссылочную структуру до отправки.
