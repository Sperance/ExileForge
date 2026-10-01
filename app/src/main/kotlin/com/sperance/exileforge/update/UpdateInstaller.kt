package com.sperance.exileforge.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

/**
 * The downloaded APK into the system installer (3.72.0): a [PackageInstaller] session, so no file is shared with another
 * app. The system asks the player to confirm; what it answers comes back through [UpdateReceiver] as [results]. The
 * release is signed with the same key as the build on the device, so it installs over it, the hero data kept.
 */
object UpdateInstaller {
    private val mutableResults = MutableSharedFlow<InstallResult>(extraBufferCapacity = 4)
    val results: SharedFlow<InstallResult> = mutableResults.asSharedFlow()

    /** May this app install packages at all ("Install unknown apps", Android 8+). */
    fun allowed(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /** The system screen that lets this app install packages. */
    fun permissionScreen(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Hands [apk] to the system installer; the answer comes as [results]. */
    suspend fun install(context: Context, apk: File) = withContext(Dispatchers.IO) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply { setAppPackageName(context.packageName) }
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("base.apk", 0, apk.length()).use { output ->
                apk.inputStream().use { it.copyTo(output) }
                session.fsync(output)
            }
            val intent = Intent(context, UpdateReceiver::class.java).setPackage(context.packageName)
            // The installer writes its status into the intent: it must stay mutable.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            session.commit(PendingIntent.getBroadcast(context, id, intent, flags).intentSender)
        }
    }

    internal fun post(result: InstallResult) { mutableResults.tryEmit(result) }
}

/** What the system installer answered. */
sealed interface InstallResult {
    /** Installed: the process is about to be replaced. */
    data object Done : InstallResult
    /** The player closed the confirmation, or the system refused; [message] is the system's own words. */
    data class Failed(val message: String) : InstallResult
}

/** The installer's answers: the confirmation screen is opened, the end is passed on to [UpdateInstaller.results]. */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                    else @Suppress("DEPRECATION") intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                // The receiver is not exported: only the system installer's own confirmation reaches it.
                if (confirm != null) context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                else UpdateInstaller.post(InstallResult.Failed("no confirmation"))
            }
            PackageInstaller.STATUS_SUCCESS -> UpdateInstaller.post(InstallResult.Done)
            else -> UpdateInstaller.post(InstallResult.Failed(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status $status"))
        }
    }
}
