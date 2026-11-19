package com.chase.rustalarm

import android.content.Context
import android.net.Uri

object Prefs {
    const val DEFAULT_PACKAGE = "com.facepunch.rust.companion"
    private const val FILE = "rustalarm"

    private fun sp(c: Context) = c.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getPackage(c: Context): String =
        sp(c).getString("pkg", DEFAULT_PACKAGE)?.trim().orEmpty().ifEmpty { DEFAULT_PACKAGE }

    fun setPackage(c: Context, v: String) {
        sp(c).edit().putString("pkg", v.trim()).apply()
    }

    /** Comma-separated keywords. Empty = any Rust+ notification triggers the alarm. */
    fun getKeywords(c: Context): String = sp(c).getString("kw", "")?.trim().orEmpty()

    fun setKeywords(c: Context, v: String) {
        sp(c).edit().putString("kw", v.trim()).apply()
    }

    fun getSound(c: Context): Uri? = sp(c).getString("sound", null)?.let { Uri.parse(it) }

    fun setSound(c: Context, uri: Uri?) {
        sp(c).edit().putString("sound", uri?.toString()).apply()
    }
}
