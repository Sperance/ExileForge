package com.sperance.exileforge.presentation.state

import com.sperance.exileforge.rules.content.EngineRules

/**
 * What opens with the hero's level (3.76.0): not everything at once, a new place every few levels. The client alone keeps
 * the gate; testers and staff pass it. [tab] or [building] is the way into the feature that the gate closes. Уровни - в
 * правилах `unlocks` (3.88.8), не в коде: раздела там нет - он открыт с первого уровня.
 */
enum class Feature(val title: String, val tab: Int? = null, val building: Building? = null) {
    GRIMOIRE("nav.skills", tab = TAB_SKILLS),
    CRAFTS("nav.crafts", tab = TAB_CRAFTS),
    CHRONICLE("chronicle.title", tab = TAB_CHRONICLE),
    MERCHANT("merchant.title", building = Building.MERCHANT),
    FORGE("nav.forge", tab = TAB_CRAFT),
    QUESTS("quest.title", building = Building.QUESTS),
    AUCTION("nav.auction", building = Building.AUCTION),
    TRIALS("trials.title", tab = TAB_TRIALS),
    PETS("progress.pets", tab = TAB_PETS),
    GUILD("guild.title", building = Building.GUILD),
    ;

    companion object {
        fun ofTab(tab: Int): Feature? = entries.firstOrNull { it.tab == tab }
        fun ofBuilding(building: Building?): Feature? = building?.let { b -> entries.firstOrNull { it.building == b } }

        /** The features a hero gains between [from] and [to], both levels inclusive of [to] only. */
        fun gained(from: Int, to: Int, rules: EngineRules?): List<Feature> = entries.filter { it.level(rules) in (from + 1)..to }
    }
}

/** Whether the hero being played has [feature] open. */
fun GameUi.unlocked(feature: Feature?): Boolean = feature == null || isTester || heroLevel >= feature.level(index?.rules)

/** Уровень, с которого раздел открыт, по правилам `unlocks`; без правил или без строки раздела - первый. */
fun Feature.level(rules: EngineRules?): Int = rules?.unlocks?.get(name) ?: 1
