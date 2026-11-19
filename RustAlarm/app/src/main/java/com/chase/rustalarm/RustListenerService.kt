package com.chase.rustalarm

import android.app.Notification
import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class RustListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName != Prefs.getPackage(this)) return

        val n = sbn.notification
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = n.extras
        val haystack = listOf(
            extras.getCharSequence(Notification.EXTRA_TITLE),
            extras.getCharSequence(Notification.EXTRA_TEXT),
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
            extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        ).joinToString(" ") { it?.toString().orEmpty() }

        val keywords = Prefs.getKeywords(this)
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (keywords.isNotEmpty() && keywords.none { haystack.contains(it, ignoreCase = true) }) return

        AlarmPlayer.start(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (Build.VERSION.SDK_INT >= 24) {
            requestRebind(ComponentName(this, RustListenerService::class.java))
        }
    }
}
