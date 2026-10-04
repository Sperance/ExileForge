package com.sperance.exileforge.presentation.guild

import androidx.lifecycle.ViewModel
import com.sperance.exileforge.core.guild.GuildRepository
import com.sperance.exileforge.core.guild.GuildTab
import com.sperance.exileforge.core.guild.Guilds
import com.sperance.exileforge.core.model.guild.GuildCard
import com.sperance.exileforge.core.network.MemberCommand
import com.sperance.exileforge.core.session.Activity
import com.sperance.exileforge.core.session.CommandRunner
import com.sperance.exileforge.presentation.state.GameSlice
import com.sperance.exileforge.presentation.state.GameUi
import com.sperance.exileforge.rules.content.GuildMode
import kotlinx.coroutines.flow.StateFlow

/** Экран гильдии (3.80.13): гильдия из репозитория, действия общие с возвратом в приложение. */
class GuildViewModel(
    private val actions: GuildActions,
    repository: GuildRepository,
    commands: CommandRunner,
    slice: GameSlice,
) : ViewModel() {
    /** Срез «игра» для экранов этой модели (3.80.33). */
    val game: StateFlow<GameUi> = slice.ui
    val guilds: StateFlow<Guilds> = repository.state
    val activity: StateFlow<Activity> = commands.state

    fun tab(tab: GuildTab?) = actions.tab(tab)
    fun query(text: String) = actions.query(text)
    fun filterFaction(code: String) = actions.filterFaction(code)
    fun load() = actions.load()
    fun search(page: Int = 0) = actions.search(page)
    fun create(name: String, tag: String, faction: String, emblem: String, color: String, mode: GuildMode, minLevel: Int) = actions.create(name, tag, faction, emblem, color, mode, minLevel)
    fun join(card: GuildCard) = actions.join(card)
    fun acceptInvite(guildId: String) = actions.acceptInvite(guildId)
    fun declineInvite(guildId: String) = actions.declineInvite(guildId)
    fun leave() = actions.leave()
    fun disband() = actions.disband()
    fun acceptApplicant(applicantId: String) = actions.acceptApplicant(applicantId)
    fun declineApplicant(applicantId: String) = actions.declineApplicant(applicantId)
    fun invite(name: String) = actions.invite(name)
    fun member(command: MemberCommand, memberId: String) = actions.member(command, memberId)
    fun settings(mode: GuildMode, minLevel: Int, emblem: String, color: String, announcement: String) = actions.settings(mode, minLevel, emblem, color, announcement)
    fun takeNode(node: String) = actions.takeNode(node)
    fun resetTree() = actions.resetTree()
    fun loadStash() = actions.loadStash()
    fun deposit(tab: Int, itemId: String?, code: String?, amount: Long) = actions.deposit(tab, itemId, code, amount)
    fun take(entryId: String) = actions.take(entryId)
    fun tabRank(tab: Int, minRank: Int) = actions.tabRank(tab, minRank)
    fun contribute(item: String, amount: Long) = actions.contribute(item, amount)
    fun loadLog(more: Boolean = false) = actions.loadLog(more)
}
