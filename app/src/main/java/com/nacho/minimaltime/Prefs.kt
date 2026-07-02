package com.nacho.minimaltime

import android.content.Context
import org.json.JSONObject
import java.util.Calendar

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

    fun hidden(c: Context): Set<String> = getSet(c, "hidden")
    fun toggleHidden(c: Context, pkg: String) = toggle(c, "hidden", pkg)

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

    // --- Modo monje (manual y programado) ---

    fun monk(c: Context) = sp(c).getBoolean("monk", false)
    fun setMonk(c: Context, on: Boolean) = sp(c).edit().putBoolean("monk", on).apply()

    fun monkSchedOn(c: Context) = sp(c).getBoolean("monk_sched", false)
    fun setMonkSchedOn(c: Context, on: Boolean) =
        sp(c).edit().putBoolean("monk_sched", on).apply()

    fun monkStartHour(c: Context) = sp(c).getInt("monk_start", 22)
    fun setMonkStartHour(c: Context, h: Int) = sp(c).edit().putInt("monk_start", h).apply()

    fun monkEndHour(c: Context) = sp(c).getInt("monk_end", 7)
    fun setMonkEndHour(c: Context, h: Int) = sp(c).edit().putInt("monk_end", h).apply()

    /** ¿Está activo el modo monje ahora mismo (manual o por horario)? */
    fun monkActiveNow(c: Context): Boolean {
        if (monk(c)) return true
        if (!monkSchedOn(c)) return false
        val now = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val s = monkStartHour(c)
        val e = monkEndHour(c)
        return when {
            s == e -> true
            s < e -> now in s until e
            else -> now >= s || now < e
        }
    }

    // --- Modo concentración ---

    fun focusUntil(c: Context) = sp(c).getLong("focus_until", 0L)
    fun setFocusUntil(c: Context, ts: Long) = sp(c).edit().putLong("focus_until", ts).apply()
    fun focusActive(c: Context) = focusUntil(c) > System.currentTimeMillis()

    // --- Personalización y opciones ---

    fun userName(c: Context): String = sp(c).getString("user_name", "nacho") ?: "nacho"
    fun setUserName(c: Context, n: String) = sp(c).edit().putString("user_name", n).apply()

    fun dailyGoalMin(c: Context) = sp(c).getInt("daily_goal", 0)
    fun setDailyGoalMin(c: Context, m: Int) = sp(c).edit().putInt("daily_goal", m).apply()

    fun breathing(c: Context) = sp(c).getBoolean("breathing", false)
    fun setBreathing(c: Context, on: Boolean) =
        sp(c).edit().putBoolean("breathing", on).apply()

    fun blockerOn(c: Context) = sp(c).getBoolean("blocker_on", false)
    fun setBlockerOn(c: Context, on: Boolean) =
        sp(c).edit().putBoolean("blocker_on", on).apply()
}
