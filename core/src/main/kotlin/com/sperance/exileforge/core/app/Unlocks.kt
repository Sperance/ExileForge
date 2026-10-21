package com.sperance.exileforge.core.app

import com.sperance.exileforge.rules.content.EngineRules

/**
 * What opens with the hero's level: not everything at once, a new place every few levels. The client alone keeps
 * the gate; a tester with free progress passes it. [tab] or [building] is the way into the feature that the gate closes. Уровни - в
 * правилах `unlocks`, не в коде: раздела там нет - он открыт с первого уровня.
 */
enum class Feature(val title: String, val tab: Int? = null, val building: Building? = null) {
    GRIMOIRE("nav.skills", tab = TAB_SKILLS),

    /** Ремёсла: плитка «Развития», до уровня открытия - с замком. */
    CRAFTS("nav.crafts", tab = TAB_CRAFTS),

    /** Летопись: своего входа нет - вторая плитка «Истории», до уровня открытия она закрыта замком. */
    CHRONICLE("chronicle.title"),
    MERCHANT("merchant.title", building = Building.MERCHANT),
    FORGE("nav.forge", tab = TAB_CRAFT),
    QUESTS("quest.title", building = Building.QUESTS),
    AUCTION("nav.auction", building = Building.AUCTION),
    TRIALS("trials.title", tab = TAB_TRIALS),

    /** Гильдия: вкладка панели [TAB_GUILD]; до уровня открытия на вкладке замок, касание открывает экран замка. */
    GUILD("guild.title", tab = TAB_GUILD),

    /** Разлом недели: своего входа нет - он внутри «Испытаний»; кнопка входа на доске испытаний и у Врат. */
    RIFT("rift.title"),
    ;

    companion object {
        fun ofTab(tab: Int): Feature? = entries.firstOrNull { it.tab == tab }
        fun ofBuilding(building: Building?): Feature? = building?.let { b -> entries.firstOrNull { it.building == b } }

        /** The features a hero gains between [from] and [to], both levels inclusive of [to] only. */
        fun gained(from: Int, to: Int, rules: EngineRules?): List<Feature> = entries.filter { it.level(rules) in (from + 1)..to }

        /** Ближайший раздел, что откроется после уровня [level]; всё открыто - ничего. */
        fun next(level: Int, rules: EngineRules?): Feature? = entries.filter { it.level(rules) > level }.minByOrNull { it.level(rules) }
    }
}

/** Whether the hero being played has [feature] open. */
fun GameUi.unlocked(feature: Feature?): Boolean = feature == null || freeProgress || heroLevel >= feature.level(index?.rules)

/**
 * Открыт ли раздел [feature] по прочитанным правилам: пока правил нет, уровень открытия неизвестен - раздел закрыт, его запросы ждут
 * правил. Раздела нет - открыт.
 */
fun GameUi.reachable(feature: Feature?): Boolean = feature == null || index != null && unlocked(feature)

/** Уровень, с которого раздел открыт, по таблице `unlocks` ([EngineRules.unlockLevel]); без правил - первый. */
fun Feature.level(rules: EngineRules?): Int = rules?.unlockLevel(name) ?: 1

/**
 * С какого уровня героя можно взять в гильдию: открытие гильдий (`unlocks` → `GUILD`) и порог самой гильдии
 * [guildMin]. Сервер держит то же на каждом пути вступления; клиент гасит кнопку заранее и называет уровень.
 */
fun guildEntryLevel(rules: EngineRules?, guildMin: Int = 1): Int = maxOf(Feature.GUILD.level(rules), guildMin)
