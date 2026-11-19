package com.chase.rustalarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator

/** Plays the alarm sound at max volume on the alarm stream until stop() is called. */
object AlarmPlayer {
    private const val CHANNEL_ID = "rust_alarm"
    private const val NOTIF_ID = 4242

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var savedAlarmVolume = -1

    /** Set by AlarmActivity so it can close itself when the alarm is stopped. */
    @Volatile
    var onStopped: (() -> Unit)? = null

    @Synchronized
    fun isRinging(): Boolean = player != null

    @Synchronized
    fun start(ctx: Context) {
        if (player != null) return // already ringing
        val app = ctx.applicationContext
        val nm = app.getSystemService(NotificationManager::class.java)
        val am = app.getSystemService(AudioManager::class.java)

        // Lift Do Not Disturb if the user granted that access.
        try {
            if (nm.isNotificationPolicyAccessGranted &&
                nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
            ) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (_: SecurityException) {
        }

        // Max alarm volume (remember the old one to restore later).
        savedAlarmVolume = am.getStreamVolume(AudioManager.STREAM_ALARM)
        am.setStreamVolume(
            AudioManager.STREAM_ALARM,
            am.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0
        )

        val candidates = listOfNotNull(
            Prefs.getSound(app),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        )
        for (uri in candidates) {
            val mp = build(app, uri)
            if (mp != null) {
                player = mp
                mp.start()
                break
            }
        }

        val vib = app.getSystemService(Vibrator::class.java)
        vibrator = vib
        vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 300), 0))

        showNotification(app, nm)
    }

    private fun build(app: Context, uri: Uri): MediaPlayer? = try {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            setDataSource(app, uri)
            isLooping = true
            setVolume(1f, 1f)
            prepare()
        }
    } catch (e: Exception) {
        null
    }

    private fun showNotification(app: Context, nm: NotificationManager) {
        val channel = NotificationChannel(CHANNEL_ID, "Rust alarm", NotificationManager.IMPORTANCE_HIGH).apply {
            setSound(null, null)
            enableVibration(false)
            setBypassDnd(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        nm.createNotificationChannel(channel)

        val open = PendingIntent.getActivity(
            app, 0,
            Intent(app, AlarmActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val stop = PendingIntent.getBroadcast(
            app, 1,
            Intent(app, StopReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        @Suppress("DEPRECATION")
        val n = Notification.Builder(app, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("RUST ALARM")
            .setContentText("Your base alarm went off. Tap to stop.")
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setContentIntent(open)
            .setFullScreenIntent(open, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stop)
            .build()
        nm.notify(NOTIF_ID, n)
    }

    @Synchronized
    fun stop(ctx: Context) {
        val app = ctx.applicationContext
        player?.let {
            try {
                it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null

        if (savedAlarmVolume >= 0) {
            try {
                app.getSystemService(AudioManager::class.java)
                    .setStreamVolume(AudioManager.STREAM_ALARM, savedAlarmVolume, 0)
            } catch (_: Exception) {
            }
            savedAlarmVolume = -1
        }
        app.getSystemService(NotificationManager::class.java).cancel(NOTIF_ID)
        onStopped?.invoke()
    }
}
