package com.sperance.exileforge.core

import com.sperance.exileforge.core.model.paging.Page
import com.sperance.exileforge.core.network.AccountRole
import com.sperance.exileforge.core.network.ModerationRow
import com.sperance.exileforge.core.network.SanctionCategory
import com.sperance.exileforge.core.network.SanctionKind
import com.sperance.exileforge.core.network.SanctionTarget
import com.sperance.exileforge.core.network.SanctionView
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Корзина модерации падала на повторе ключа строки: у одного героя оказалось два действующих удаления, а строка корзины
 * ключевалась героем. Ключ строки корзины - санкция, повтор строки внутри страницы и на стыке страниц остаётся одной строкой.
 */
class TrashKeysTest {
    @Test
    fun twoDeletionsOfOneHeroAndRepeatedRowsKeepKeysUnique() {
        val first = row("del-1")
        val second = row("del-2")
        val page = Page(listOf(first, second, first), more = true).distinctBy(ModerationRow::key)
        assertEquals(listOf("del-1", "del-2"), page.items.map { it.key })

        val next = Page(listOf(second, row("del-3"), row("del-3")), page = 1)
        val shown = page.followedBy(next, ModerationRow::key)
        assertEquals(listOf("del-1", "del-2", "del-3"), shown.items.map { it.key })
    }

    private fun row(sanction: String) = ModerationRow(
        heroId = "hero",
        heroName = "Hero",
        userId = "user",
        sanction = SanctionView(sanction, 1, SanctionKind.DELETION, SanctionTarget.HERO, "Hero", SanctionCategory.CHEATS, byLogin = "admin", byRole = AccountRole.ADMIN, at = "2026-10-05T00:00"),
    )
}
