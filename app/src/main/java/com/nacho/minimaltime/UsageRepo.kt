package com.nacho.minimaltime

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.util.Calendar

object UsageRepo {

    fun hasAccess(c: Context): Boolean {
        val aom = c.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = aom.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), c.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun startOfDay(offsetDays: Int = 0): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -offsetDays)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun usm(c: Context) =
        c.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /** Tiempo en primer plano por paquete entre dos instantes, calculado a partir de eventos. */
    fun usageBetween(c: Context, start: Long, end: Long): MutableMap<String, Long> {
        val map = HashMap<String, Long>()
        val events = usm(c).queryEvents(start, end)
        val e = UsageEvents.Event()
        var currentPkg: String? = null
        var currentStart = 0L
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    val p = currentPkg
                    if (p != null) {
                        map[p] = (map[p] ?: 0L) + (e.timeStamp - currentStart).coerceAtLeast(0)
                    }
                    currentPkg = e.packageName
                    currentStart = e.timeStamp
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND,
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    val p = currentPkg
                    if (p != null) {
                        map[p] = (map[p] ?: 0L) + (e.timeStamp - currentStart).coerceAtLeast(0)
                        currentPkg = null
                    }
                }
            }
        }
        val p = currentPkg
        if (p != null) {
            val until = minOf(end, System.currentTimeMillis())
            map[p] = (map[p] ?: 0L) + (until - currentStart).coerceAtLeast(0)
        }
        return map
    }

    fun todayUsage(c: Context): MutableMap<String, Long> =
        usageBetween(c, startOfDay(0), System.currentTimeMillis())

    /** Número de desbloqueos de hoy. Devuelve -1 si la versión de Android no lo soporta. */
    fun unlocksToday(c: Context): Int {
        if (Build.VERSION.SDK_INT < 28) return -1
        val events = usm(c).queryEvents(startOfDay(0), System.currentTimeMillis())
        val e = UsageEvents.Event()
        var n = 0
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) n++
        }
        return n
    }

    /** Totales de los últimos 7 días (índice 0 = hace 6 días, 6 = hoy), solo apps de la lista. */
    fun weekTotals(c: Context, launchable: Set<String>): LongArray {
        val out = LongArray(7)
        for (i in 0..6) {
            val off = 6 - i
            val start = startOfDay(off)
            val end = if (off == 0) System.currentTimeMillis() else startOfDay(off - 1)
            val m = usageBetween(c, start, end)
            var sum = 0L
            for ((k, v) in m) if (k in launchable) sum += v
            out[i] = sum
        }
        return out
    }

    /** Último paquete que pasó a primer plano en la ventana indicada. */
    fun foregroundPackage(c: Context, lookbackMs: Long = 60_000): String? {
        val now = System.currentTimeMillis()
        val events = usm(c).queryEvents(now - lookbackMs, now)
        val e = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) last = e.packageName
        }
        return last
    }
}
