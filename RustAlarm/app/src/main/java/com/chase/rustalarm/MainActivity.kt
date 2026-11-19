package com.chase.rustalarm

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.graphics.Typeface
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var soundLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(24), dp(16), dp(24))
        }

        root.addView(label("Rust+ Alarm Blaster", 24f, bold = true))
        status = label("", 14f)
        root.addView(status)

        root.addView(label("Setup (do each once)", 18f, bold = true, topMargin = 20))
        root.addView(button("1. Grant notification access") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        })
        root.addView(button("2. Allow notifications (Android 13+)") {
            if (Build.VERSION.SDK_INT >= 33) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            } else {
                toast("Not needed on this Android version")
            }
        })
        root.addView(button("3. Allow Do Not Disturb override") {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        })
        root.addView(button("4. Allow full-screen alerts (Android 14+)") {
            if (Build.VERSION.SDK_INT >= 34) {
                try {
                    startActivity(
                        Intent(
                            Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                            Uri.parse("package:$packageName")
                        )
                    )
                } catch (e: Exception) {
                    toast("Not available on this phone")
                }
            } else {
                toast("Not needed on this Android version")
            }
        })
        root.addView(button("5. Disable battery optimization") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        })

        root.addView(label("Alarm sound", 18f, bold = true, topMargin = 20))
        soundLabel = label("", 14f)
        root.addView(soundLabel)
        root.addView(button("Choose ringtone file") {
            val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "audio/*"
                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                )
            }
            @Suppress("DEPRECATION")
            startActivityForResult(i, REQ_SOUND)
        })
        root.addView(button("Use default alarm sound") {
            Prefs.setSound(this, null)
            refresh()
        })

        root.addView(label("Filter (optional)", 18f, bold = true, topMargin = 20))
        root.addView(label("Only ring if the Rust+ notification contains one of these words (comma-separated). Leave empty to ring on every Rust+ notification.", 13f))
        root.addView(input("e.g. alarm, raid", Prefs.getKeywords(this)) { Prefs.setKeywords(this, it) })

        root.addView(label("Rust+ app package name", 14f, topMargin = 12))
        root.addView(input(Prefs.DEFAULT_PACKAGE, Prefs.getPackage(this)) { Prefs.setPackage(this, it) })

        root.addView(label("Test", 18f, bold = true, topMargin = 20))
        root.addView(button("Test alarm now") { AlarmPlayer.start(this) })
        root.addView(button("Stop alarm") { AlarmPlayer.stop(this) })

        setContentView(ScrollView(this).apply { addView(root) })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_SOUND && resultCode == RESULT_OK) {
            val uri = data?.data ?: return
            try {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (_: Exception) {
            }
            Prefs.setSound(this, uri)
            refresh()
        }
    }

    private fun refresh() {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.contains(packageName) == true
        val dnd = getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
        status.text = "Notification access: ${if (enabled) "ON" else "OFF - do step 1"}\n" +
            "Do Not Disturb override: ${if (dnd) "ON" else "off (optional, step 3)"}"
        soundLabel.text = Prefs.getSound(this)?.lastPathSegment ?: "Default alarm sound"
    }

    // --- tiny UI helpers ---

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun label(text: String, size: Float, bold: Boolean = false, topMargin: Int = 0) =
        TextView(this).apply {
            this.text = text
            textSize = size
            if (bold) setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, dp(topMargin), 0, dp(4)) }
        }

    private fun button(text: String, onClick: () -> Unit) = Button(this).apply {
        this.text = text
        isAllCaps = false
        setOnClickListener { onClick() }
    }

    private fun input(hint: String, initial: String, onChange: (String) -> Unit) = EditText(this).apply {
        this.hint = hint
        setText(initial)
        isSingleLine = true
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = onChange(s?.toString().orEmpty())
        })
    }

    companion object {
        private const val REQ_SOUND = 100
    }
}
