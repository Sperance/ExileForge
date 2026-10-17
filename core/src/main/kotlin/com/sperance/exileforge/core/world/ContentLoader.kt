package com.sperance.exileforge.core.world

/**
 * Контент сервера до чтения, которому он нужен: манифест, и при новом хэше - файлы мира. Сама загрузка пока
 * живёт в `ForgeRuntime` и подставляется делегатом; модели экранов зовут только [ensure].
 */
class ContentLoader {
    var delegate: (suspend (fresh: Boolean) -> Unit)? = null

    suspend fun ensure(fresh: Boolean = false) {
        delegate?.invoke(fresh)
    }
}
