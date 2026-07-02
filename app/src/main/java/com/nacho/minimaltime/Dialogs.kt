package com.nacho.minimaltime

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.widget.EditText
import android.widget.Toast

object Dialogs {

    private const val THEME = android.R.style.Theme_DeviceDefault_Dialog_Alert

    fun appOptions(a: Activity, app: AppInfo, onChanged: () -> Unit) {
        val fav = app.pkg in Prefs.favorites(a)
        val dist = app.pkg in Prefs.distracting(a)
        val blk = app.pkg in Prefs.blocked(a)
        val hid = app.pkg in Prefs.hidden(a)
        val lim = Prefs.limits(a)[app.pkg]
        val items = arrayOf(
            if (fav) "Quitar de favoritas" else "Añadir a favoritas",
            if (dist) "Quitar de distractoras" else "Marcar como distractora",
            if (blk) "Desbloquear" else "Bloquear ahora",
            "Límite diario" + if (lim != null) " (actual: $lim min)" else " (sin límite)",
            if (hid) "Mostrar app" else "Ocultar app",
            "Abrir"
        )
        AlertDialog.Builder(a, THEME)
            .setTitle(app.label)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> { Prefs.toggleFavorite(a, app.pkg); onChanged() }
                    1 -> { Prefs.toggleDistracting(a, app.pkg); onChanged() }
                    2 -> { Prefs.toggleBlocked(a, app.pkg); onChanged() }
                    3 -> limitPicker(a, app, onChanged)
                    4 -> { Prefs.toggleHidden(a, app.pkg); onChanged() }
                    5 -> Apps.open(a, app.pkg)
                }
            }
            .show()
    }

    fun limitPicker(a: Activity, app: AppInfo, onChanged: () -> Unit) {
        val labels = arrayOf("Sin límite", "5 min", "15 min", "30 min", "1 hora", "2 horas")
        val values = intArrayOf(0, 5, 15, 30, 60, 120)
        AlertDialog.Builder(a, THEME)
            .setTitle("Límite diario · ${app.label}")
            .setItems(labels) { _, i ->
                Prefs.setLimit(a, app.pkg, values[i])
                onChanged()
            }
            .show()
    }

    fun focusPicker(a: Activity, onChanged: () -> Unit) {
        if (Prefs.focusActive(a)) {
            val left = (Prefs.focusUntil(a) - System.currentTimeMillis()) / 60000 + 1
            AlertDialog.Builder(a, THEME)
                .setTitle("Modo concentración")
                .setMessage("Quedan $left min. Las apps distractoras están bloqueadas.")
                .setPositiveButton("Terminar ahora") { _, _ ->
                    Prefs.setFocusUntil(a, 0)
                    onChanged()
                }
                .setNegativeButton("Seguir concentrado", null)
                .show()
            return
        }
        val labels = arrayOf("15 min", "25 min", "50 min", "90 min")
        val values = intArrayOf(15, 25, 50, 90)
        AlertDialog.Builder(a, THEME)
            .setTitle("Concentrarse durante…")
            .setItems(labels) { _, i ->
                if (!UsageRepo.hasAccess(a)) {
                    Toast.makeText(a, "concede el acceso de uso en ajustes", Toast.LENGTH_LONG).show()
                    return@setItems
                }
                Prefs.setFocusUntil(a, System.currentTimeMillis() + values[i] * 60_000L)
                try {
                    a.startForegroundService(Intent(a, BlockerService::class.java))
                } catch (_: Exception) {
                }
                onChanged()
            }
            .show()
    }

    fun nameInput(a: Activity, onChanged: () -> Unit) {
        val et = EditText(a)
        et.setText(Prefs.userName(a))
        et.isSingleLine = true
        AlertDialog.Builder(a, THEME)
            .setTitle("Tu nombre")
            .setView(et)
            .setPositiveButton("Guardar") { _, _ ->
                Prefs.setUserName(a, et.text.toString().trim())
                onChanged()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    fun goalPicker(a: Activity, onChanged: () -> Unit) {
        val labels = arrayOf("Sin objetivo", "1 hora", "2 horas", "3 horas", "4 horas", "5 horas", "6 horas")
        val values = intArrayOf(0, 60, 120, 180, 240, 300, 360)
        AlertDialog.Builder(a, THEME)
            .setTitle("Objetivo diario de tiempo de pantalla")
            .setItems(labels) { _, i ->
                Prefs.setDailyGoalMin(a, values[i])
                onChanged()
            }
            .show()
    }

    fun hourPicker(a: Activity, title: String, onPick: (Int) -> Unit) {
        val labels = Array(24) { String.format("%02d:00", it) }
        AlertDialog.Builder(a, THEME)
            .setTitle(title)
            .setItems(labels) { _, i -> onPick(i) }
            .show()
    }
}
