package com.nacho.minimaltime

import android.content.Context
import org.json.JSONObject

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("minimal", Context.MODE_PRIVATE)

    private fun getSet(c: Context, key: String): Set<String> =
        sp(c).getStringSet(key, emptySet())?.toSet() ?: emptySet()

    private fun toggle(c: Context, key: String, pkg: String) {
        val s = getSet(c, key).toMutableSet()
        if (!s.add(pkg)) s.remove(pkg)
        sp(c).edit().putStringSet(key, s).apply()
    }

    fun favorites(c: Context): Set<String> = getSet(c, "favs")
    fun toggleFavorite(c: Context, pkg: String) = toggle(c, "favs", pkg)

    fun distracting(c: Context): Set<String> = getSet(c, "dist")
    fun toggleDistracting(c: Context, pkg: String) = toggle(c, "dist", pkg)

    fun blocked(c: Context): Set<String> = getSet(c, "blocked")
    fun toggleBlocked(c: Context, pkg: String) = toggle(c, "blocked", pkg)

    fun limits(c: Context): Map<String, Int> {
        val j = JSONObject(sp(c).getString("limits", "{}") ?: "{}")
        val m = HashMap<String, Int>()
        for (k in j.keys()) m[k] = j.getInt(k)
        return m
    }

    fun setLimit(c: Context, pkg: String, minutes: Int) {
        val j = JSONObject(sp(c).getString("limits", "{}") ?: "{}")
        if (minutes <= 0) j.remove(pkg) else j.put(pkg, minutes)
        sp(c).edit().putString("limits", j.toString()).apply()
    }

    fun monk(c: Context) = sp(c).getBoolean("monk", false)
    fun setMonk(c: Context, on: Boolean) = sp(c).edit().putBoolean("monk", on).apply()

    fun blockerOn(c: Context) = sp(c).getBoolean("blocker_on", false)
    fun setBlockerOn(c: Context, on: Boolean) =
        sp(c).edit().putBoolean("blocker_on", on).apply()
}
