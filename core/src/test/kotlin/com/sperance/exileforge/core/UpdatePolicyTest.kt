package com.sperance.exileforge.core

import com.sperance.exileforge.core.update.AppBuild
import com.sperance.exileforge.core.update.UpdatePolicy
import com.sperance.exileforge.core.update.UpdatePolicy.Kind
import com.sperance.exileforge.core.update.Wire
import kotlin.test.Test
import kotlin.test.assertEquals

class UpdatePolicyTest {
    private val own = Wire(45, 41)
    private val next = Wire(46, 41)

    private fun build(code: Int, wire: Wire) = AppBuild(code, "3.$code", wire.api, wire.rules, "a.apk", 1, "x")

    @Test
    fun an_older_or_same_build_is_never_offered() {
        assertEquals(Kind.NONE, UpdatePolicy.decide(build(10, own), 10, own, own))
        assertEquals(Kind.NONE, UpdatePolicy.decide(build(9, next), 10, own, next))
    }

    @Test
    fun a_newer_build_on_the_same_wire_is_optional() {
        assertEquals(Kind.OPTIONAL, UpdatePolicy.decide(build(11, own), 10, own, own))
        // Сервер не ответил: провод этой сборки.
        assertEquals(Kind.OPTIONAL, UpdatePolicy.decide(build(11, own), 10, own, null))
    }

    @Test
    fun a_server_on_another_wire_makes_its_build_mandatory() {
        assertEquals(Kind.MANDATORY, UpdatePolicy.decide(build(11, next), 10, own, next))
    }

    @Test
    fun a_build_ahead_of_the_server_is_not_offered() {
        assertEquals(Kind.NONE, UpdatePolicy.decide(build(11, next), 10, own, own))
        assertEquals(Kind.NONE, UpdatePolicy.decide(build(11, next), 10, own, null))
    }

    @Test
    fun a_server_moved_on_without_a_matching_build_offers_nothing() {
        assertEquals(Kind.NONE, UpdatePolicy.decide(build(11, own), 10, own, next))
    }
}
