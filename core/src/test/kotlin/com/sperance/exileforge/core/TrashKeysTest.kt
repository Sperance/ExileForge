package com.sperance.exileforge.core

import com.sperance.exileforge.core.model.paging.Page
import com.sperance.exileforge.core.network.AccountRole
import com.sperance.exileforge.core.network.ModerationAccount
import com.sperance.exileforge.core.network.ModerationHero
import com.sperance.exileforge.core.network.SanctionCategory
import com.sperance.exileforge.core.network.SanctionKind
import com.sperance.exileforge.core.network.SanctionTarget
import com.sperance.exileforge.core.network.SanctionView
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Корзина модерации падала на повторе ключа строки. Ключ карточки списка - аккаунт: два удаления одного героя - строки одной карточки, а
 * повтор карточки внутри страницы и на стыке страниц (список сдвинулся между чтениями) остаётся одной карточкой.
 */
class TrashKeysTest {
    @Test
    fun twoDeletionsOfOneHeroAndRepeatedCardsKeepKeysUnique() {
        val first = card("user-1", "del-1", "del-2")
        val page = Page(listOf(first, card("user-2", "del-3"), first), more = true).distinctBy(ModerationAccount::userId)
        assertEquals(listOf("user-1", "user-2"), page.items.map { it.userId })
        assertEquals(2, page.items.first().heroes.size)

        val next = Page(listOf(card("user-2", "del-3"), card("user-3", "del-4")), page = 1)
        val shown = page.followedBy(next, ModerationAccount::userId)
        assertEquals(listOf("user-1", "user-2", "user-3"), shown.items.map { it.userId })
    }

    private fun card(user: String, vararg deletions: String) = ModerationAccount(
        userId = user,
        heroes = deletions.map { id ->
            ModerationHero(
                heroId = "hero",
                name = "Hero",
                deleted = true,
                sanction = SanctionView(id, 1, SanctionKind.DELETION, SanctionTarget.HERO, "Hero", SanctionCategory.CHEATS, byLogin = "admin", byRole = AccountRole.ADMIN, at = "2026-10-05T00:00"),
            )
        },
    )
}
