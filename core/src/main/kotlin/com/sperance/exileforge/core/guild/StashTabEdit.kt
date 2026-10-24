package com.sperance.exileforge.core.guild

/**
 * Правка вкладки хранилища из листа её настроек: новое имя [name] и новый порог ранга [minRank]; null - поле не меняется. Правку без
 * перемен ([changes] - нет) лист не отправляет.
 */
data class StashTabEdit(val name: String? = null, val minRank: Int? = null) {
    /** Правка что-то меняет. */
    val changes: Boolean get() = name != null || minRank != null

    companion object {
        /** Правка вкладки с именем [current] и порогом [currentRank] до имени [name] и порога [rank]: только разошедшееся. */
        fun of(current: String, currentRank: Int, name: String, rank: Int): StashTabEdit = StashTabEdit(name.trim().takeIf { it != current.trim() }, rank.takeIf { it != currentRank })
    }
}
