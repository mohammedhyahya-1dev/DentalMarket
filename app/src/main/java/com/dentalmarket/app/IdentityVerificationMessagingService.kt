package com.dentalmarket.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.dentalmarket.app.data.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "admin_alerts"

// Receive-side only — the send side (Admin SDK, the only thing that can
// safely target a specific device) lives in functions/index.js. This class
// has two jobs: keep AuthRepository's stored token current (onNewToken),
// and — since a manually-built notification is what lets tapping it deep-
// link to the right screen — construct and show the notification ourselves
// rather than relying on FCM's own auto-display (onMessageReceived).
class IdentityVerificationMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val authRepository = AuthRepository()

    // Fires once per real token (fresh install, or a rotation) — but only
    // ever meaningful for the admin account, since nothing else reads any
    // other uid's token. Not the only place this gets saved: authGate also
    // saves it speculatively on every admin login (same "onNewToken alone
    // isn't enough" reasoning as ensureUsername's own comment — a device
    // could get its token before ever being signed in as admin).
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if (!authRepository.isAdmin) return
        scope.launch { authRepository.updateFcmToken(token) }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: return
        val body = message.notification?.body ?: ""
        val uid = message.data["uid"]
        showNotification(title, body, uid)
    }

    private fun showNotification(title: String, body: String, uid: String?) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Admin alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            manager.createNotificationChannel(channel)
        }

        // Same deep-link mechanism as every other cross-screen link in this
        // app (see MainActivity.kt's adminVerificationRouteFor) — this is
        // an internal PendingIntent targeting MainActivity directly, not a
        // real clickable web URL, but shaped identically to the seller-
        // share-link/reset-password ones so it resolves through the exact
        // same authGate-centralized flow rather than a separate one.
        val intent = Intent(this, MainActivity::class.java).apply {
            // uid is always present in practice — the Cloud Function always
            // includes it (see functions/index.js). Falling back to no
            // .data at all (a plain app-open, no deep link) rather than
            // inventing a second URI shape nothing else recognizes.
            if (uid != null) {
                action = Intent.ACTION_VIEW
                data = Uri.parse("https://dentalmarket-abdf6.firebaseapp.com/adminVerification/$uid")
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(uid?.hashCode() ?: 0, notification)
    }
}
