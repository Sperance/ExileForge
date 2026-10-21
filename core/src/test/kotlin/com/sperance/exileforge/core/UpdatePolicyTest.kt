package com.sperance.exileforge.core

import com.sperance.exileforge.core.update.AppBuild
import com.sperance.exileforge.core.update.UpdatePolicy
import com.sperance.exileforge.core.update.Wire
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UpdatePolicyTest {
    private val own = Wire(45, 41)
    private val next = Wire(46, 41)

    private fun build(code: Int, wire: Wire, mandatory: Boolean = false) = AppBuild(code, "3.$code", wire.api, wire.rules, "a.apk", 1, "x", mandatory = mandatory)

    @Test
    fun an_older_or_same_build_is_never_required() {
        assertFalse(UpdatePolicy.required(build(10, own), 10, own, own))
        assertFalse(UpdatePolicy.required(build(9, next), 10, own, next))
    }

    /** Сервер не ответил: провод этой сборки. */
    @Test
    fun a_newer_build_on_the_same_wire_is_required() {
        assertTrue(UpdatePolicy.required(build(11, own), 10, own, own))
        assertTrue(UpdatePolicy.required(build(11, own), 10, own, null))
    }

    @Test
    fun a_server_on_another_wire_requires_its_build() {
        assertTrue(UpdatePolicy.required(build(11, next), 10, own, next))
    }

    @Test
    fun a_build_ahead_of_the_server_is_not_offered() {
        assertFalse(UpdatePolicy.required(build(11, next), 10, own, own))
        assertFalse(UpdatePolicy.required(build(11, next), 10, own, null))
    }

    @Test
    fun a_server_moved_on_without_a_matching_build_offers_nothing() {
        assertFalse(UpdatePolicy.required(build(11, own), 10, own, next))
    }

    /** 426 `CL_001`: сборка новее нужна всё равно. */
    @Test
    fun a_server_that_called_the_build_outdated_requires_any_newer_one() {
        assertTrue(UpdatePolicy.required(build(11, next), 10, own, null, outdated = true))
        assertTrue(UpdatePolicy.required(build(11, next), 10, own, own, outdated = true))
        assertFalse(UpdatePolicy.required(build(10, next), 10, own, null, outdated = true))
    }

    /** Тот же провод - мягкое; другой провод - жёсткое. */
    @Test
    fun a_build_on_the_own_wire_is_soft_and_on_another_wire_hard() {
        assertFalse(UpdatePolicy.hard(build(11, own), own))
        assertTrue(UpdatePolicy.hard(build(11, next), own))
    }

    /** 426: сборка на своём проводе всё равно жёсткая. */
    @Test
    fun a_server_that_called_the_build_outdated_makes_the_update_hard() {
        assertTrue(UpdatePolicy.hard(build(11, own), own, outdated = true))
    }

    /** Релиз объявлен обязательным (`Update-Mandatory: yes`): жёсткое и на том же проводе. */
    @Test
    fun a_release_declared_mandatory_is_hard() {
        assertTrue(UpdatePolicy.hard(build(11, own, mandatory = true), own))
    }

    @Test
    fun the_headline_is_the_first_note_line_without_markup() {
        assertEquals("Аукцион стал Рынком", build(11, own).copy(notes = "\n- **Аукцион стал Рынком**\n- Второе").headline)
        assertEquals("", build(11, own).copy(notes = "").headline)
    }
}
