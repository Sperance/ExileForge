package com.sperance.exileforge.core

import com.sperance.exileforge.core.model.paging.Page
import com.sperance.exileforge.core.network.AccountRole
import com.sperance.exileforge.core.network.ModerationAccount
import com.sperance.exileforge.core.network.ModerationHero
import com.sperance.exileforge.core.network.ModerationRow
import com.sperance.exileforge.core.network.SanctionCategory
import com.sperance.exileforge.core.network.SanctionKind
import com.sperance.exileforge.core.network.SanctionTarget
import com.sperance.exileforge.core.network.SanctionView
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Корзина модерации падала на повторе ключа строки. Ключ строки корзины - удаление: два удаления одного героя - две строки, а повтор
 * строки внутри страницы и на стыке страниц (список сдвинулся между чтениями) остаётся одной строкой.
 */
class TrashKeysTest {
    @Test
    fun twoDeletionsOfOneHeroAndRepeatedRowsKeepKeysUnique() {
        val first = row("user-1", "del-1")
        val page = Page(listOf(first, row("user-1", "del-2"), first), more = true).distinctBy(ModerationRow::key)
        assertEquals(listOf("del-1", "del-2"), page.items.map { it.key })

        val next = Page(listOf(row("user-1", "del-2"), row("user-3", "del-4")), page = 1)
        val shown = page.followedBy(next, ModerationRow::key)
        assertEquals(listOf("del-1", "del-2", "del-4"), shown.items.map { it.key })
    }

    private fun row(user: String, deletion: String) = ModerationRow(
        ModerationAccount(userId = user),
        ModerationHero(
            heroId = "hero",
            name = "Hero",
            deleted = true,
            sanction = SanctionView(deletion, 1, SanctionKind.DELETION, SanctionTarget.HERO, "Hero", SanctionCategory.CHEATS, byLogin = "admin", byRole = AccountRole.ADMIN, at = "2026-10-05T00:00"),
        ),
    )
}
