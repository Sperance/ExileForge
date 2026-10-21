package com.sperance.exileforge.core

import com.sperance.exileforge.core.update.AppBuild
import com.sperance.exileforge.core.update.UpdateKind
import com.sperance.exileforge.core.update.UpdatePolicy
import com.sperance.exileforge.core.update.Wire
import com.sperance.exileforge.core.update.notesHeadline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UpdatePolicyTest {
    private val own = Wire(45, 41)
    private val next = Wire(46, 41)

    private fun build(code: Int, wire: Wire, forced: Boolean = false) = AppBuild(code, "3.$code", wire.api, wire.rules, "a.apk", 1, "x", forced = forced)

    @Test
    fun an_older_or_same_build_is_never_offered() {
        assertNull(UpdatePolicy.verdict(build(10, own), 10, own, own))
        assertNull(UpdatePolicy.verdict(build(9, next), 10, own, next))
    }

    /** Сервер не ответил: провод этой сборки. Тот же провод - мягкое. */
    @Test
    fun a_newer_build_on_the_same_wire_is_soft() {
        assertEquals(UpdateKind.SOFT, UpdatePolicy.verdict(build(11, own), 10, own, own))
        assertEquals(UpdateKind.SOFT, UpdatePolicy.verdict(build(11, own), 10, own, null))
    }

    /** Другой провод - жёсткое. */
    @Test
    fun a_server_on_another_wire_requires_its_build() {
        assertEquals(UpdateKind.HARD, UpdatePolicy.verdict(build(11, next), 10, own, next))
    }

    @Test
    fun a_build_ahead_of_the_server_is_not_offered() {
        assertNull(UpdatePolicy.verdict(build(11, next), 10, own, own))
        assertNull(UpdatePolicy.verdict(build(11, next), 10, own, null))
    }

    @Test
    fun a_server_moved_on_without_a_matching_build_offers_nothing() {
        assertNull(UpdatePolicy.verdict(build(11, own), 10, own, next))
    }

    /** 426 `CL_001`: сборка новее нужна всё равно и всегда жёстко, на своём проводе тоже. */
    @Test
    fun a_server_that_called_the_build_outdated_requires_any_newer_one_hard() {
        assertEquals(UpdateKind.HARD, UpdatePolicy.verdict(build(11, next), 10, own, null, outdated = true))
        assertEquals(UpdateKind.HARD, UpdatePolicy.verdict(build(11, next), 10, own, own, outdated = true))
        assertEquals(UpdateKind.HARD, UpdatePolicy.verdict(build(11, own), 10, own, own, outdated = true))
        assertNull(UpdatePolicy.verdict(build(10, next), 10, own, null, outdated = true))
    }

    /** Релиз объявлен обязательным (`Update-Mandatory: yes`): жёсткое и на том же проводе. */
    @Test
    fun a_release_declared_mandatory_is_hard() {
        assertEquals(UpdateKind.HARD, UpdatePolicy.verdict(build(11, own, forced = true), 10, own, own))
    }

    @Test
    fun the_headline_is_the_first_note_line_without_markup() {
        assertEquals("Аукцион стал Рынком", notesHeadline("\n- **Аукцион стал Рынком**\n- Второе"))
        assertEquals("", notesHeadline(""))
    }
}
