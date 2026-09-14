package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.passives.*
import com.sperance.exileforge.core.network.GameApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import kotlin.test.*

class PassiveContractTest {
    @Test fun skillWritesSendOnlyCommandAndPreserveLongVersion(): Unit = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"success":true,"data":{"token":"fixture-token"}}"""))
            val response = """{"success":true,"data":{"characterVersion":9007199254740994,"treeRevision":1,"allocated":["origin"],"totalPoints":2,"spentPoints":1,"availablePoints":1,"allocatable":["vitality_1"],"refundable":["origin"],"stats":{"version":9007199254740994,"values":{"maximum_life":77}}}}"""
            server.enqueue(MockResponse().setBody(response)); server.enqueue(MockResponse().setBody(response))
            val api = GameApi(server.url("/").toString()); api.login("user", "password"); server.takeRequest()
            val id = "0123456789abcdef01234567"
            val command = PassiveCommand(9007199254740993L, 1, "request-123", PassiveAction.ALLOCATE, "origin")
            assertEquals(9007199254740994L, api.changePassives(id, command).characterVersion)
            api.changePassives(id, command)
            val first = server.takeRequest(); val second = server.takeRequest()
            assertEquals("/api/v1/passives/characters/$id", first.path)
            assertEquals("Bearer fixture-token", first.getHeader("Authorization"))
            val body = first.body.readUtf8()
            assertEquals(body, second.body.readUtf8())
            assertEquals(9007199254740993L, WireJson.parseToJsonElement(body).jsonObject.getValue("expectedVersion").jsonPrimitive.long)
            assertFalse(body.contains("effects")); assertFalse(body.contains("availablePoints"))
        }
    }
    @Test fun treeKeepsStableGraphCoordinatesAndLargeNodeKinds() {
        val node = PassiveNode("node", "Узел", "", PassiveNodeKind.KEYSTONE, 12.0, -24.0, listOf(PassiveEffect("maximum_life", PassiveOperation.MORE, .2)))
        val tree = PassiveTree(1, "Дерево", "node", listOf(node), emptyList())
        assertEquals(tree, WireJson.decodeFromString<PassiveTree>(WireJson.encodeToString(tree)))
    }
}
