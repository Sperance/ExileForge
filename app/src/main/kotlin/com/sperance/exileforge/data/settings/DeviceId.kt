package com.sperance.exileforge.data.settings

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.UUID

/**
 * The identifier the account is registered under.
 *
 * It reads as a hardware fingerprint, and mostly is one — but `Build.MANUFACTURER`, `MODEL`,
 * `DEVICE` and `HARDWARE` describe the *model*, not the handset: two identical phones on the same
 * firmware produce byte-for-byte the same string. Alone they would hand the second player the
 * first one's account, because `login/byDeviceId` would find it and hand it over. `ANDROID_ID` is
 * the only per-instance value a normal app can still read (`SERIAL` needs a privileged permission
 * since Android 8, the IMEI is gone since 10, and the MAC has been `02:00:00:00:00:00` since 6),
 * so it is what actually makes this unique; the rest keeps the id recognisable in a support log.
 *
 * The hash is folded into a UUID so the value is a fixed shape whatever the parts are worth, and
 * so the raw `ANDROID_ID` never travels to the server or into the request journal.
 *
 * It survives reinstalling the app and is lost on a factory reset — and losing it means losing the
 * account, because the server has no other way back to it.
 */
@SuppressLint("HardwareIds")
fun deviceId(context: Context): String {
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
    val parts = listOf(Build.MANUFACTURER, Build.MODEL, Build.DEVICE, Build.HARDWARE, androidId)
    return uuidFrom(parts.joinToString("|") { it.orEmpty().trim() })
}

/** A name-based UUID (RFC 4122 v5): the same text always yields the same identifier. */
private fun uuidFrom(text: String): String {
    val hash = MessageDigest.getInstance("SHA-1").digest(text.toByteArray())
    hash[6] = ((hash[6].toInt() and 0x0f) or 0x50).toByte()
    hash[8] = ((hash[8].toInt() and 0x3f) or 0x80).toByte()
    val buffer = ByteBuffer.wrap(hash, 0, 16)
    return UUID(buffer.long, buffer.long).toString()
}
