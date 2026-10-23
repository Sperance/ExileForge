package com.sperance.exileforge.ui.screens.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sperance.exileforge.BuildConfig
import com.sperance.exileforge.core.app.AccountSeal
import com.sperance.exileforge.core.app.SealRole
import com.sperance.exileforge.core.display.dateText
import com.sperance.exileforge.core.display.dayTimeText
import com.sperance.exileforge.core.fate.fateTitle
import com.sperance.exileforge.core.i18n.ui
import com.sperance.exileforge.core.model.command.AccountNotice
import com.sperance.exileforge.core.network.SanctionKind
import com.sperance.exileforge.core.network.epochOfStamp
import com.sperance.exileforge.core.session.AccountBinding
import com.sperance.exileforge.rules.content.InputLimits
import com.sperance.exileforge.ui.components.ButtonSize
import com.sperance.exileforge.ui.components.ButtonState
import com.sperance.exileforge.ui.components.ConfirmSheet
import com.sperance.exileforge.ui.components.ForgeButton
import com.sperance.exileforge.ui.components.ForgeSheet
import com.sperance.exileforge.ui.components.InfoMark
import com.sperance.exileforge.ui.components.LengthCounter
import com.sperance.exileforge.ui.components.MutedText
import com.sperance.exileforge.ui.components.SheetBody
import com.sperance.exileforge.ui.components.Tip
import com.sperance.exileforge.ui.components.relicName
import com.sperance.exileforge.ui.icons.ForgeGlyphs
import com.sperance.exileforge.ui.theme.Bronze
import com.sperance.exileforge.ui.theme.Gold
import com.sperance.exileforge.ui.theme.GoldBright
import com.sperance.exileforge.ui.theme.LifeRed
import com.sperance.exileforge.ui.theme.Muted
import com.sperance.exileforge.ui.theme.Panel
import com.sperance.exileforge.ui.theme.Parchment
import com.sperance.exileforge.ui.theme.Rune

/**
 * Карточка-печать аккаунта: значок роли, имя (длинное переносится) и плашка роли - «Гость», «Игрок», «Модератор» со щитом,
 * «Администратор»; ниже строки «название - значение»: логин (у гостя - «Вход: по устройству»), героев из мест, Предначертание, «В игре
 * с» (если сервер прислал); действующие санкции - полосой внизу печати (немота, бан героя), [about] - ⓘ с пояснением входа.
 */
@Composable internal fun AccountSealCard(seal: AccountSeal, about: String) {
    val shape = RoundedCornerShape(14.dp)
    val tint = roleTint(seal.role)
    Column(
        Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Gold.copy(alpha = .14f).compositeOver(Panel), Panel)), shape).border(1.dp, Bronze, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(roleIcon(seal.role), null, tint = tint, modifier = Modifier.size(26.dp))
            Column(Modifier.weight(1f)) {
                Text(seal.name, style = relicName(18).copy(color = GoldBright))
                Text(
                    ui("signin.role.${seal.role.name}"),
                    color = tint,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 2.dp).border(1.dp, tint.copy(alpha = .6f), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
            InfoMark(ui("signin.about"), tip = { Tip(ui("account.signin_section"), about, Muted) })
        }
        HorizontalDivider(color = Bronze.copy(alpha = .47f), thickness = 1.dp)
        if (seal.guest) SealRow(ui("signin.device"), ui("signin.by_device")) else SealRow(ui("account.login"), seal.login)
        SealRow(ui("signin.heroes"), ui("signin.heroes_value", seal.heroes, seal.slots))
        SealRow(ui("signin.fate"), seal.fate?.let(::fateTitle) ?: ui("signin.fate_none"), muted = seal.fate == null)
        seal.since?.let { SealRow(ui("signin.since"), dateText(it)) }
        seal.sanctions.forEach { SanctionStripe(it) }
    }
}

/** Строка печати: название слева, значение справа; [muted] - значение тихим цветом («не выбрано»). */
@Composable private fun SealRow(label: String, value: String, muted: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
        Text(value, color = if (muted) Muted else Parchment, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

/** Полоса санкции в печати: что и до когда (местное время), что из-за неё нельзя и причина для игрока. */
@Composable private fun SanctionStripe(sanction: AccountNotice) {
    val notice = sanction.notice
    val until = epochOfStamp(notice.until)?.let { dayTimeText(it) }
    val mute = notice.kind == SanctionKind.MUTE
    val head = when {
        mute && sanction.hero.isBlank() -> until?.let { ui("signin.mute_until", it) } ?: ui("signin.mute_forever")
        mute -> until?.let { ui("signin.hero_mute_until", sanction.hero, it) } ?: ui("signin.hero_mute_forever", sanction.hero)
        else -> until?.let { ui("signin.hero_ban_until", sanction.hero, it) } ?: ui("signin.hero_ban_forever", sanction.hero)
    }
    val note = listOfNotNull(ui(if (mute) "signin.mute_note" else "signin.ban_note"), notice.reason.takeIf { it.isNotBlank() }?.let { ui("signin.reason", it) }).joinToString(" ")
    val shape = RoundedCornerShape(8.dp)
    Column(Modifier.fillMaxWidth().background(LifeRed.copy(alpha = .12f), shape).border(1.dp, LifeRed.copy(alpha = .5f), shape).padding(horizontal = 10.dp, vertical = 6.dp)) {
        Text(head, color = LifeRed, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Text(note, color = Parchment, style = MaterialTheme.typography.bodySmall)
    }
}

/** Знак роли печати: гость и игрок - знак изгнанника, модератор - щит, администратор - печать администратора. */
private fun roleIcon(role: SealRole): ImageVector = when (role) {
    SealRole.GUEST, SealRole.PLAYER -> ForgeGlyphs.Exile
    SealRole.MODERATOR -> Icons.Outlined.Shield
    SealRole.ADMIN -> Icons.Outlined.WorkspacePremium
}

/** Цвет роли - те же, что у значков роли ([com.sperance.exileforge.ui.components.RoleMark]); гость - тихий. */
private fun roleTint(role: SealRole): Color = when (role) {
    SealRole.GUEST -> Muted
    SealRole.PLAYER -> Gold
    SealRole.MODERATOR -> Rune
    SealRole.ADMIN -> LifeRed
}

/** Действие страницы входа строкой: знак, название, пояснение второй строкой (необязательно); каждое открывает свой лист. */
internal enum class SignInAction(val icon: ImageVector, val title: String, val note: String?) {
    CHANGE_PASSWORD(Icons.Outlined.Key, "signin.change_password", null),
    REGISTER(Icons.Outlined.PersonAdd, "signin.register", "signin.register_note"),
    SIGN_IN(Icons.AutoMirrored.Outlined.Login, "signin.sign_in", "signin.sign_in_note"),
    SIGN_OUT(Icons.AutoMirrored.Outlined.Logout, "signin.sign_out", null),
    ;

    companion object {
        /** Действия аккаунта [seal]: гость - зарегистрироваться, войти и выйти; с логином - сменить пароль и выйти. */
        fun of(seal: AccountSeal): List<SignInAction> = if (seal.guest) listOf(REGISTER, SIGN_IN, SIGN_OUT) else listOf(CHANGE_PASSWORD, SIGN_OUT)
    }
}

/** Строка действия [action] в группе строк ([RowGroup]): название, пояснение мелко под ним, шеврон. */
@Composable internal fun SignInActionRow(action: SignInAction, enabled: Boolean, onClick: () -> Unit) {
    AccountRow(action.icon, ui(action.title), note = action.note?.let { ui(it) }, danger = action == SignInAction.SIGN_OUT, enabled = enabled, onClick = onClick)
}

/** Подвал страниц входа: версия и сервер - игроку «основной сервер», адрес - только администратору. */
@Composable internal fun SignInFooter(server: String, admin: Boolean) {
    val version = ui("app.version", BuildConfig.VERSION_NAME)
    val line = if (admin) ui("signin.footer_server", version, server) else ui("signin.footer_main", version)
    Text(line, color = Muted.copy(alpha = .7f), style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
}

/**
 * Лист формы входа - общий облик: заголовок, пояснение [note], поля [fields], под полями - отказ сервера [refusal] (только после своей
 * отправки), внизу - кнопка [submit]. Отправка без отказа закрывает лист сама, когда запрос кончился.
 */
@Composable private fun FormSheet(
    title: String,
    submit: String,
    ready: Boolean,
    busy: Boolean,
    refusal: String?,
    note: String?,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit,
    fields: @Composable () -> Unit,
) {
    var sent by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(busy) {
        if (sent && !busy) {
            sent = false
            if (refusal == null) onDismiss() else failed = true
        }
    }
    ForgeSheet(
        onDismissRequest = onDismiss,
        body = SheetBody.Scroll(),
        footer = {
            ForgeButton(
                state = ButtonState.of(!busy && ready),
                onClick = {
                    failed = false
                    sent = true
                    onSubmit()
                },
                modifier = Modifier.weight(1f),
                size = ButtonSize.Wide,
            ) { Text(submit) }
        },
    ) {
        Text(title, style = relicName(19).copy(color = GoldBright))
        note?.let { MutedText(it) }
        fields()
        if (failed && refusal != null) Text(refusal, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

/** Лист «Войти по логину»: логин со счётчиком, пароль; неверный логин или пароль - под полями. */
@Composable internal fun LoginSheet(limits: InputLimits, busy: Boolean, refusal: String?, onLogin: (String, String) -> Unit, onDismiss: () -> Unit) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    FormSheet(ui("auth.by_login"), ui("account.sign_in"), login.isNotBlank() && password.isNotEmpty(), busy, refusal, null, onSubmit = { onLogin(login, password) }, onDismiss = onDismiss) {
        OutlinedTextField(login, { login = it.take(limits.login) }, enabled = !busy, label = { Text(ui("account.login")) }, supportingText = { LengthCounter(login, limits.login) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it.take(limits.password) }, enabled = !busy, label = { Text(ui("account.password")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    }
}

/** Лист «Сменить пароль»: текущий и новый пароль; текущий не подошёл - под полями. */
@Composable internal fun PasswordSheet(limits: InputLimits, busy: Boolean, refusal: String?, onChange: (String, String) -> Unit, onDismiss: () -> Unit) {
    var current by remember { mutableStateOf("") }
    var next by remember { mutableStateOf("") }
    FormSheet(ui("signin.change_password"), ui("account.do_change_password"), current.isNotEmpty() && next.isNotEmpty(), busy, refusal, null, onSubmit = { onChange(current, next) }, onDismiss = onDismiss) {
        OutlinedTextField(current, { current = it.take(limits.password) }, enabled = !busy, label = { Text(ui("account.current_password")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(next, { next = it.take(limits.password) }, enabled = !busy, label = { Text(ui("account.new_password")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
    }
}

/** Лист «Зарегистрироваться» гостя: логин, пароль дважды; беда формы - до запроса, когда что-то введено; отказ сервера - под полями. */
@Composable internal fun RegisterSheet(limits: InputLimits, busy: Boolean, refusal: String?, onRegister: (String, String) -> Unit, onDismiss: () -> Unit) {
    var login by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val problem = AccountBinding.problem(login, password, repeat, limits)
    FormSheet(ui("signin.register"), ui("signin.register"), problem == null, busy, refusal, ui("account.bind_note"), onSubmit = { onRegister(login, password) }, onDismiss = onDismiss) {
        OutlinedTextField(login, { login = it.take(limits.accountLogin) }, enabled = !busy, label = { Text(ui("account.login")) }, supportingText = { LengthCounter(login, limits.accountLogin) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(password, { password = it.take(limits.password) }, enabled = !busy, label = { Text(ui("account.password")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        OutlinedTextField(repeat, { repeat = it.take(limits.password) }, enabled = !busy, label = { Text(ui("account.bind_repeat")) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
        if (problem != null && (login.isNotEmpty() || password.isNotEmpty() || repeat.isNotEmpty())) {
            Text(ui(problem.key, AccountBinding.argument(problem, limits)), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Лист «Выйти из аккаунта?»: гостя предупреждает, что без логина в героев с другого устройства не войти. */
@Composable internal fun SignOutSheet(guest: Boolean, busy: Boolean, onSignOut: () -> Unit, onDismiss: () -> Unit) {
    ConfirmSheet(
        title = ui("signin.sign_out_q"),
        confirm = ui("signin.sign_out"),
        onDismiss = onDismiss,
        note = ui(if (guest) "signin.sign_out_guest" else "signin.sign_out_note"),
        danger = guest,
        busy = busy,
    ) {
        onSignOut()
        onDismiss()
    }
}
