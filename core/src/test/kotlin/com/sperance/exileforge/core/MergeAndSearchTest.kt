package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.editor.conflict.ThreeWayMerge
import com.sperance.exileforge.core.model.CatalogFilter
import com.sperance.exileforge.core.network.FailureState
import kotlinx.serialization.json.*
import org.junit.Test
import kotlin.test.*

class MergeAndSearchTest {
    private fun obj(value: String) = WireJson.parseToJsonElement(value).jsonObject
    @Test fun unrelatedChangesMergeWhileOverlappingFieldsRequireChoice() {
        val base = obj("""{"name":"old","description":"old","version":1}""")
        val mine = obj("""{"name":"mine","description":"old","version":1}""")
        val remote = obj("""{"name":"theirs","description":"remote","version":2}""")
        val review = ThreeWayMerge.review(base, mine, remote)
        assertEquals(listOf("name"), review.conflicts.map { it.field })
        assertFailsWith<IllegalArgumentException> { ThreeWayMerge.resolve(review, emptyMap()) }
        assertEquals(obj("""{"name":"mine","description":"remote","version":2}"""), ThreeWayMerge.resolve(review, mapOf("name" to true)))
        assertEquals(remote, ThreeWayMerge.resolve(review, mapOf("name" to false)))
    }
    @Test fun arraysAreAtomicAndIdenticalEditsDoNotConflict() {
        val base = obj("""{"refs":[1,2],"name":"base"}""")
        val mine = obj("""{"refs":[2,3],"name":"same"}""")
        val remote = obj("""{"refs":[4],"name":"same"}""")
        val review = ThreeWayMerge.review(base, mine, remote)
        assertEquals(listOf("refs"), review.conflicts.map { it.field })
        assertEquals(mine, ThreeWayMerge.resolve(review, mapOf("refs" to true)))
    }
    @Test fun filtersRoundTripAndUnknownWriteOutcomeIsExplicit() {
        val filter = CatalogFilter("life & mana", "RING", "RARE", "10", "80", "defense", "5")
        assertEquals(filter, WireJson.decodeFromString(CatalogFilter.serializer(), WireJson.encodeToString(CatalogFilter.serializer(), filter)))
        assertEquals("life & mana", filter.parameters()["q"])
        assertEquals(FailureState.UncertainWrite, FailureState.from(java.io.IOException(), true))
        assertEquals(FailureState.Offline, FailureState.from(java.io.IOException(), false))
    }
}
