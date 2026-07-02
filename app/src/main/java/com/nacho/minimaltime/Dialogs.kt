package com.nacho.minimaltime

import android.app.Activity
import android.app.AlertDialog

object Dialogs {

    fun appOptions(a: Activity, app: AppInfo, onChanged: () -> Unit) {
        val fav = app.pkg in Prefs.favorites(a)
        val dist = app.pkg in Prefs.distracting(a)
        val blk = app.pkg in Prefs.blocked(a)
        val lim = Prefs.limits(a)[app.pkg]
        val items = arrayOf(
            if (fav) "Quitar de favoritas" else "Añadir a favoritas",
            if (dist) "Quitar de distractoras" else "Marcar como distractora",
            if (blk) "Desbloquear" else "Bloquear ahora",
            "Límite diario" + if (lim != null) " (actual: $lim min)" else " (sin límite)",
            "Abrir"
        )
        AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(app.label)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> { Prefs.toggleFavorite(a, app.pkg); onChanged() }
                    1 -> { Prefs.toggleDistracting(a, app.pkg); onChanged() }
                    2 -> { Prefs.toggleBlocked(a, app.pkg); onChanged() }
                    3 -> limitPicker(a, app, onChanged)
                    4 -> Apps.open(a, app.pkg)
                }
            }
            .show()
    }

    fun limitPicker(a: Activity, app: AppInfo, onChanged: () -> Unit) {
        val labels = arrayOf("Sin límite", "5 min", "15 min", "30 min", "1 hora", "2 horas")
        val values = intArrayOf(0, 5, 15, 30, 60, 120)
        AlertDialog.Builder(a, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("Límite diario · ${app.label}")
            .setItems(labels) { _, i ->
                Prefs.setLimit(a, app.pkg, values[i])
                onChanged()
            }
            .show()
    }
}
