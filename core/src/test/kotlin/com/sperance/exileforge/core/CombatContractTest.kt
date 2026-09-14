package com.sperance.exileforge.core

import com.sperance.exileforge.core.contract.WireJson
import com.sperance.exileforge.core.model.combat.*
import com.sperance.exileforge.core.network.GameApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Test
import kotlin.test.*

class CombatContractTest {
    @Test fun combatRequestsPreserveVersionBattleAndIdempotencyKey(): Unit = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"success":true,"data":{"token":"fixture-token"}}"""))
            val response = """{"success":true,"data":{"characterVersion":8,"battle":null,"zoneKills":{"coast":2}}}"""
            server.enqueue(MockResponse().setBody(response))
            server.enqueue(MockResponse().setBody(response))
            val api = GameApi(server.url("/").toString())
            api.login("tester", "password")
            server.takeRequest()
            val id = "0123456789abcdef01234567"
            val command = BattleActionCommand(7, "same-request-key", "battle-123", BattleAction.GUARD)
            assertEquals(8L, api.actBattle(id, command).characterVersion)
            api.actBattle(id, command)
            val first = server.takeRequest(); val second = server.takeRequest()
            assertEquals("/api/v1/combat/characters/$id/act", first.path)
            assertEquals("Bearer fixture-token", first.getHeader("Authorization"))
            assertEquals(first.body.readUtf8(), second.body.readUtf8())
            assertEquals("POST", first.method)
        }
    }
    @Test fun serializedBattleCommandsNeverContainClientSuppliedDamageOrRewards() {
        val json = WireJson.encodeToString(StartBattleCommand(0, "request-123", "coast"))
        assertTrue(json.contains("expectedVersion"))
        assertFalse(json.contains("damage"))
        assertFalse(json.contains("rewards"))
        assertEquals(BattleAction.POWER, WireJson.decodeFromString<BattleActionCommand>(
            WireJson.encodeToString(BattleActionCommand(3, "request-456", "battle-123", BattleAction.POWER))).action)
    }
}
