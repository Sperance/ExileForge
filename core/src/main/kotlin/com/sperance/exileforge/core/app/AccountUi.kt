package com.sperance.exileforge.core.app

import com.sperance.exileforge.core.admin.Admin
import com.sperance.exileforge.core.hero.HeroHolding
import com.sperance.exileforge.core.i18n.Lang
import com.sperance.exileforge.core.i18n.uiLanguage
import com.sperance.exileforge.core.network.Link
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.Session
import com.sperance.exileforge.core.world.World
import com.sperance.exileforge.rules.content.ContentIndex

/**
 * Что читают экраны аккаунта и настроек: сессия, команда в полёте, язык, мир, связь, данные администратора,
 * герой на экране и режим. Срез источников вместо общего состояния целиком.
 */
data class AccountUi(
    val session: Session,
    val activity: Activity = Activity(),
    val lang: Lang = uiLanguage,
    val world: World = World(),
    val link: Link = Link(),
    val admin: Admin = Admin(),
    val hero: HeroHolding = HeroHolding(),
    val mode: AppMode = AppMode.PLAYER,
) {
    val busy: Boolean get() = activity.held
    val isAdmin: Boolean get() = session.isAdmin

    /** Выбранный герой - тестировщик: окно тестирования - ему. */
    val isTester: Boolean get() = session.tester(hero)

    /** Окно тестирования: герою-тестировщику и любому герою администратора. */
    val grants: Boolean get() = session.grants(hero)

    /** Модерация и отчёты: модератор и администратор. */
    val isModerator: Boolean get() = session.isModerator

    /** Инструменты администратора: роль модерации аккаунта и режим вместе; права проверяет сервер. */
    val adminTools: Boolean get() = isModerator && mode == AppMode.ADMIN
    val index: ContentIndex? get() = world.content
    val heroId: String get() = hero.heroId

    private val row get() = session.characters.firstOrNull { it.id == hero.heroId }
    private val info get() = hero.hero?.takeIf { it.id == hero.heroId }?.info
    val heroName: String get() = info?.name ?: row?.name.orEmpty()
    val heroLevel: Int get() = info?.level ?: row?.level ?: 1
    val heroClass: String? get() = info?.heroClass ?: row?.heroClass
}
