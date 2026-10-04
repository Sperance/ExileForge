package com.sperance.exileforge.presentation.state

/**
 * What opens with the hero's level (3.76.0): not everything at once, a new place every few levels. The client alone keeps
 * the gate; testers and administrators pass it. [tab] or [building] is the way into the feature that the gate closes.
 */
enum class Feature(val level: Int, val title: String, val tab: Int? = null, val building: Building? = null) {
    GRIMOIRE(2, "nav.skills", tab = TAB_SKILLS),
    CRAFTS(3, "nav.crafts", tab = TAB_CRAFTS),
    CHRONICLE(4, "chronicle.title", tab = TAB_CHRONICLE),
    MERCHANT(3, "merchant.title", building = Building.MERCHANT),
    FORGE(2, "nav.forge", tab = TAB_CRAFT),
    QUESTS(5, "quest.title", building = Building.QUESTS),
    AUCTION(8, "nav.auction", building = Building.AUCTION),
    TRIALS(10, "trials.title", tab = TAB_TRIALS),
    PETS(10, "progress.pets", tab = TAB_PETS),
    GUILD(12, "guild.title", building = Building.GUILD),
    ;

    companion object {
        fun ofTab(tab: Int): Feature? = entries.firstOrNull { it.tab == tab }
        fun ofBuilding(building: Building?): Feature? = building?.let { b -> entries.firstOrNull { it.building == b } }

        /** The features a hero gains between [from] and [to], both levels inclusive of [to] only. */
        fun gained(from: Int, to: Int): List<Feature> = entries.filter { it.level in (from + 1)..to }
    }
}

/** Whether the hero being played has [feature] open. */
fun ForgeState.unlocked(feature: Feature?): Boolean = feature == null || isTester || heroLevel >= feature.level

fun GameUi.unlocked(feature: Feature?): Boolean = feature == null || isTester || heroLevel >= feature.level
