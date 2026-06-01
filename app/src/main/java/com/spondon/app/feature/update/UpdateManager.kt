package com.spondon.app.feature.update

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.spondon.app.BuildConfig
import com.spondon.app.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages APK download, showing a notification with real-time progress,
 * and then offering Install / Cancel actions.
 */
class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"
        private const val CHANNEL_ID_PROGRESS = "spondon_update_progress"
        private const val CHANNEL_ID_COMPLETE = "spondon_update_complete"
        private const val CHANNEL_NAME = "App Updates"
        private const val NOTIFICATION_ID = 9001
    }

    /** Download progress exposed for optional in-app UI. 0–100, or -1 for indeterminate. */
    private val _progress = MutableStateFlow(-1)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    /** True while a download is in flight. */
    private val _isDownloading = MutableStateFlow(false)
    val isDownloading: StateFlow<Boolean> = _isDownloading.asStateFlow()

    private val notificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    init {
        createNotificationChannel()
    }

    // ── Public API ────────────────────────────────────────────────

    /**
     * Downloads the APK from the given URL.
     *
     * For **private** GitHub repos the `browser_download_url` redirects through
     * a GitHub auth gate.  `DownloadManager` cannot add the required
     * `Authorization` header on the redirect, so the download silently fails.
     *
     * This implementation therefore does two things:
     * 1. If a GitHub token is available, it downloads the APK manually with
     *    an authenticated `HttpURLConnection`, following redirects.
     * 2. If no token is available (public repo), it falls back to the system
     *    `DownloadManager` for a nicer notification-based UX.
     */
    fun downloadUpdate(apkUrl: String) {
        if (_isDownloading.value) return // prevent duplicate downloads
        val token = BuildConfig.GITHUB_TOKEN
        if (token.isNotBlank() && apkUrl.contains("github.com")) {
            // Private repo: manual authenticated download with progress
            downloadWithAuth(apkUrl, token)
        } else {
            // Public repo: use system DownloadManager
            downloadWithDownloadManager(apkUrl)
        }
    }

    /** Cancels an in-progress download and dismisses the notification. */
    fun cancelDownload() {
        _isDownloading.value = false
        _progress.value = -1
        notificationManager.cancel(NOTIFICATION_ID)
        // Delete partial file
        val file = getApkFile()
        if (file.exists()) file.delete()
    }

    // ── Notification channel ──────────────────────────────────────

    private fun createNotificationChannel() {
        // Low importance channel for silent progress updates
        val progressChannel = NotificationChannel(
            CHANNEL_ID_PROGRESS,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Download progress for app updates"
            setShowBadge(false)
        }
        notificationManager.createNotificationChannel(progressChannel)

        // High importance channel for the final Install prompt
        val completeChannel = NotificationChannel(
            CHANNEL_ID_COMPLETE,
            "App Update Ready",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications when an app update is ready to install"
            setShowBadge(true)
        }
        notificationManager.createNotificationChannel(completeChannel)
    }

    // ── Notification builders ─────────────────────────────────────

    private fun buildProgressNotification(percent: Int): android.app.Notification {
        val cancelIntent = Intent(context, UpdateCancelReceiver::class.java)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_PROGRESS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Downloading update…")
            .setContentText(if (percent >= 0) "$percent%" else "Preparing…")
            .setProgress(100, percent.coerceAtLeast(0), percent < 0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(R.mipmap.ic_launcher, "Cancel", cancelPendingIntent)
            .build()
    }

    private fun buildCompleteNotification(): android.app.Notification {
        val installIntent = Intent(context, UpdateInstallReceiver::class.java)
        val installPendingIntent = PendingIntent.getBroadcast(
            context, 1, installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val cancelIntent = Intent(context, UpdateCancelReceiver::class.java)
        val cancelPendingIntent = PendingIntent.getBroadcast(
            context, 0, cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(context, CHANNEL_ID_COMPLETE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Update ready to install")
            .setContentText("Tap Install to apply the update")
            .setAutoCancel(false)
            .setOngoing(false)
            .setProgress(0, 0, false) // Clear any leftover progress bar
            .setContentIntent(installPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Install", installPendingIntent)
            .addAction(R.mipmap.ic_launcher, "Cancel", cancelPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Ensure visibility
            .build()
    }

    private fun buildFailedNotification(reason: String): android.app.Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID_COMPLETE)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Update failed")
            .setContentText(reason)
            .setAutoCancel(true)
            .setProgress(0, 0, false) // Clear any leftover progress bar
            .build()
    }

    // ── Authenticated download (private repos) ────────────────────

    private fun downloadWithAuth(apkUrl: String, token: String) {
        _isDownloading.value = true
        _progress.value = 0

        // Show initial progress notification
        notificationManager.notify(NOTIFICATION_ID, buildProgressNotification(0))

        Thread {
            try {
                val file = getApkFile()
                if (file.exists()) file.delete()

                // GitHub asset URLs need Accept: application/octet-stream
                // and Authorization header to get the actual binary.
                val downloadUrl = convertToApiUrl(apkUrl)
                Log.d(TAG, "Authenticated download from: $downloadUrl")

                val connection = URL(downloadUrl).openConnection() as HttpURLConnection
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Accept", "application/octet-stream")
                connection.setRequestProperty("User-Agent", "Spondon-Android-App")
                connection.instanceFollowRedirects = true
                connection.connectTimeout = 30_000
                connection.readTimeout = 60_000
                connection.connect()

                // GitHub may redirect — follow manually if needed
                var conn = connection
                var responseCode = conn.responseCode
                var redirectCount = 0

                while (responseCode in 301..303 || responseCode == 307 || responseCode == 308) {
                    if (++redirectCount > 5) {
                        Log.e(TAG, "Too many redirects")
                        onDownloadFailed("Too many redirects")
                        return@Thread
                    }
                    val redirectUrl = conn.getHeaderField("Location")
                    Log.d(TAG, "Redirecting to: $redirectUrl")
                    conn.disconnect()

                    conn = URL(redirectUrl).openConnection() as HttpURLConnection
                    // Don't send auth header to S3 redirect — it will fail
                    conn.setRequestProperty("User-Agent", "Spondon-Android-App")
                    conn.instanceFollowRedirects = true
                    conn.connectTimeout = 30_000
                    conn.readTimeout = 60_000
                    conn.connect()
                    responseCode = conn.responseCode
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errBody = try {
                        conn.errorStream?.bufferedReader()?.readText() ?: "no body"
                    } catch (_: Exception) { "unreadable" }
                    Log.e(TAG, "Download failed with $responseCode: $errBody")
                    conn.disconnect()
                    onDownloadFailed("Server returned $responseCode")
                    return@Thread
                }

                val totalBytes = conn.contentLength.toLong()
                var downloadedBytes = 0L
                var lastNotifiedPercent = -1

                // Stream to file with progress tracking
                conn.inputStream.use { input ->
                    FileOutputStream(file).use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (!_isDownloading.value) {
                                // User cancelled
                                conn.disconnect()
                                if (file.exists()) file.delete()
                                return@Thread
                            }
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            // Update progress
                            val percent = if (totalBytes > 0) {
                                ((downloadedBytes * 100) / totalBytes).toInt()
                            } else {
                                -1 // indeterminate
                            }

                            if (percent != lastNotifiedPercent) {
                                lastNotifiedPercent = percent
                                _progress.value = percent
                                notificationManager.notify(
                                    NOTIFICATION_ID,
                                    buildProgressNotification(percent),
                                )
                            }
                        }
                    }
                }
                conn.disconnect()

                Log.d(TAG, "Download complete: ${file.absolutePath} (${file.length()} bytes)")

                _isDownloading.value = false
                _progress.value = 100

                // Cancel the ongoing progress notification first, then show the
                // complete notification as a fresh non-ongoing notification.
                // This prevents the "stuck at 100%" issue where the ongoing
                // progress notification lingers and blocks the complete one.
                notificationManager.cancel(NOTIFICATION_ID)
                notificationManager.notify(NOTIFICATION_ID, buildCompleteNotification())

            } catch (e: Exception) {
                Log.e(TAG, "Authenticated download failed", e)
                onDownloadFailed(e.message ?: "Unknown error")
            }
        }.start()
    }

    /**
     * Converts a `browser_download_url` to the GitHub API asset URL that
     * accepts `Accept: application/octet-stream` for binary download.
     *
     * Since we don't have the asset ID handy, we fall back to using the
     * browser URL directly with auth headers — GitHub's server will redirect
     * properly when Authorization is present.
     */
    private fun convertToApiUrl(browserUrl: String): String {
        // The browser_download_url already works with Authorization header,
        // so we just return it as-is. GitHub will authenticate and serve the file.
        return browserUrl
    }

    // ── DownloadManager fallback (public repos) ───────────────────

    private var downloadId: Long = -1

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (downloadId == id) {
                _isDownloading.value = false
                _progress.value = 100
                // Cancel any ongoing progress notification, then show complete
                notificationManager.cancel(NOTIFICATION_ID)
                notificationManager.notify(NOTIFICATION_ID, buildCompleteNotification())
                try {
                    ctx.unregisterReceiver(this)
                } catch (_: Exception) { }
            }
        }
    }

    private fun downloadWithDownloadManager(apkUrl: String) {
        _isDownloading.value = true
        _progress.value = 0

        val file = getApkFile()
        if (file.exists()) file.delete()

        val request = DownloadManager.Request(apkUrl.toUri())
            .setTitle("Spondon Update")
            .setDescription("Downloading latest version…")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "update.apk")

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadId = downloadManager.enqueue(request)

        ContextCompat.registerReceiver(
            context,
            onDownloadComplete,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    // ── Failure handler ───────────────────────────────────────────

    private fun onDownloadFailed(reason: String) {
        _isDownloading.value = false
        _progress.value = -1
        notificationManager.notify(NOTIFICATION_ID, buildFailedNotification(reason))
    }

    // ── APK installation ──────────────────────────────────────────

    fun installApk(ctx: Context = context) {
        val file = getApkFile(ctx)

        if (!file.exists() || file.length() == 0L) {
            Log.e(TAG, "APK file missing or empty: ${file.absolutePath}")
            return
        }

        if (!ctx.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${ctx.packageName}".toUri()
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(settingsIntent)
            return
        }

        val uri = FileProvider.getUriForFile(
            ctx,
            "${ctx.packageName}.provider",
            file
        )

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        ctx.startActivity(installIntent)

        // Dismiss the notification after launching installer
        (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .cancel(NOTIFICATION_ID)
    }

    private fun getApkFile(ctx: Context = context): File {
        return File(ctx.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
    }

    // ── Broadcast Receivers for notification actions ──────────────

    /** Receiver that triggers APK installation from the notification "Install" action. */
    class UpdateInstallReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            UpdateManager(context).installApk(context)
        }
    }

    /** Receiver that cancels/dismisses the update notification and deletes the APK. */
    class UpdateCancelReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(NOTIFICATION_ID)
            // Delete downloaded APK
            val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
            if (file.exists()) file.delete()
        }
    }
}
