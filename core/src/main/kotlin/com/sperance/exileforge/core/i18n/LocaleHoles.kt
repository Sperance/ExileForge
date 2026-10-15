package com.sperance.exileforge.core.i18n

/**
 * Дыры словаря сервера (0.14.5, #48): имя, которого нет в словаре на устройстве, игрок не видит ни кодом, ни ключом - на его месте
 * нейтральная заглушка ([missing], `locale.missing`). Дыра сообщается слушателю [listener] один раз на ключ в редакции словаря
 * (его отпечаток [LocaleBundle.hash]): приложение пишет её в последние действия отчёта, а первая дыра редакции перечитывает
 * словарь - сервер мог уже дописать строку. Пока словаря нет вовсе (первый запуск без связи), дыры не сообщаются.
 */
object LocaleHoles {
    /** Кто слушает дыры: ключ и надо ли перечитать словарь (первая дыра этой редакции). Ставит приложение. */
    @Volatile var listener: (key: String, refetch: Boolean) -> Unit = { _, _ -> }

    private var edition: String? = null
    private val told = HashSet<String>()

    /** Заглушка на месте строки [key], которой нет в словаре; дыра сообщается слушателю. */
    fun missing(key: String): String {
        report(key)
        return ui("locale.missing")
    }

    @Synchronized private fun report(key: String) {
        val bundle = serverLocale
        if (bundle.isEmpty) return
        if (edition != bundle.hash) {
            edition = bundle.hash
            told.clear()
        }
        if (told.add(key)) listener(key, told.size == 1)
    }
}
