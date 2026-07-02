package com.nacho.minimaltime

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import android.widget.Toast

class SettingsActivity : Activity() {

    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = Ui.column(this, 24)
        setContentView(Ui.scroll(this, root))
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        root.removeAllViews()
        root.addView(Ui.text(this, "ajustes", 22f))
        root.addView(Ui.space(this, 20))

        // --- Personalización ---
        val name = Prefs.userName(this)
        row(
            "tu nombre  ·  " + if (name.isBlank()) "sin configurar" else name.lowercase(),
            "aparece en el saludo de la pantalla de inicio"
        ) {
            Dialogs.nameInput(this) { render() }
        }

        val goal = Prefs.dailyGoalMin(this)
        row(
            "objetivo diario  ·  " + if (goal > 0) Ui.fmt(goal * 60_000L) else "sin objetivo",
            "cuánto tiempo de pantalla quieres como máximo al día"
        ) {
            Dialogs.goalPicker(this) { render() }
        }

        root.addView(Ui.space(this, 12))
        root.addView(Ui.divider(this))
        root.addView(Ui.space(this, 12))
        root.addView(Ui.text(this, "protecciones", 14f, Ui.GRAY))
        root.addView(Ui.space(this, 4))

        // --- Modo monje ---
        val monk = Prefs.monk(this)
        row(
            "modo monje  ·  " + if (monk) "activado" else "desactivado",
            "bloquea todas las apps marcadas como distractoras"
        ) {
            Prefs.setMonk(this, !monk)
            ensureBlockerIfNeeded()
            render()
        }

        // --- Horario de modo monje ---
        val sched = Prefs.monkSchedOn(this)
        row(
            "horario de modo monje  ·  " + if (sched) "activado" else "desactivado",
            "activa el modo monje automáticamente cada día"
        ) {
            Prefs.setMonkSchedOn(this, !sched)
            ensureBlockerIfNeeded()
            render()
        }
        if (sched) {
            row(
                "    desde  ·  %02d:00".format(Prefs.monkStartHour(this)),
                "hora a la que empieza"
            ) {
                Dialogs.hourPicker(this, "Desde") { h ->
                    Prefs.setMonkStartHour(this, h)
                    render()
                }
            }
            row(
                "    hasta  ·  %02d:00".format(Prefs.monkEndHour(this)),
                "hora a la que termina"
            ) {
                Dialogs.hourPicker(this, "Hasta") { h ->
                    Prefs.setMonkEndHour(this, h)
                    render()
                }
            }
        }

        // --- Respiro consciente ---
        val breath = Prefs.breathing(this)
        row(
            "respiro consciente  ·  " + if (breath) "activado" else "desactivado",
            "pausa de 10 segundos antes de abrir una app distractora"
        ) {
            Prefs.setBreathing(this, !breath)
            ensureBlockerIfNeeded()
            render()
        }

        // --- Servicio de bloqueo ---
        val on = Prefs.blockerOn(this)
        row(
            "servicio de bloqueo  ·  " + if (on) "activado" else "desactivado",
            "necesario para límites, bloqueos, modo monje y respiro"
        ) {
            if (on) {
                Prefs.setBlockerOn(this, false)
                stopService(Intent(this, BlockerService::class.java))
            } else {
                if (!UsageRepo.hasAccess(this)) {
                    toast("primero concede el acceso de uso")
                } else if (!Settings.canDrawOverlays(this)) {
                    toast("primero permite mostrar sobre otras apps")
                } else {
                    Prefs.setBlockerOn(this, true)
                    startForegroundService(Intent(this, BlockerService::class.java))
                }
            }
            render()
        }

        root.addView(Ui.space(this, 12))
        root.addView(Ui.divider(this))
        root.addView(Ui.space(this, 12))
        root.addView(Ui.text(this, "permisos", 14f, Ui.GRAY))
        root.addView(Ui.space(this, 4))

        row(
            "acceso a datos de uso  ·  " + estado(UsageRepo.hasAccess(this)),
            "permite leer el tiempo de pantalla real del sistema"
        ) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        row(
            "mostrar sobre otras apps  ·  " + estado(Settings.canDrawOverlays(this)),
            "permite mostrar la pantalla de bloqueo encima de otras apps"
        ) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        if (Build.VERSION.SDK_INT >= 33) {
            val ok = checkSelfPermission("android.permission.POST_NOTIFICATIONS") ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            row(
                "notificaciones  ·  " + estado(ok),
                "para la notificación fija del servicio de bloqueo"
            ) {
                requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 1)
            }
        }

        root.addView(Ui.space(this, 12))
        root.addView(Ui.divider(this))
        root.addView(Ui.space(this, 12))

        row(
            "exportar uso de hoy (csv)",
            "guarda en Descargas un csv con minutos y aperturas por app"
        ) {
            Thread {
                val path = Export.todayCsv(this)
                runOnUiThread {
                    toast(if (path != null) "guardado en $path" else "no se pudo exportar")
                }
            }.start()
        }

        row(
            "usar como launcher",
            "elige Minimal Time como pantalla de inicio predeterminada"
        ) {
            try {
                startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
            } catch (e: Exception) {
                toast("abre Ajustes › Apps › Apps predeterminadas › App de inicio")
            }
        }

        row(
            "acerca de  ·  v2.0",
            "minimal time, hecha por y para nacho. launcher minimalista con " +
                    "control de tiempo de pantalla: sin anuncios, sin internet y " +
                    "sin recopilar datos. github.com/SempaiN/MinimalTime"
        ) { }

        root.addView(Ui.space(this, 24))
    }

    private fun estado(ok: Boolean) = if (ok) "concedido" else "pendiente"

    private fun ensureBlockerIfNeeded() {
        if (Prefs.blockerOn(this)) {
            try {
                startForegroundService(Intent(this, BlockerService::class.java))
            } catch (_: Exception) {
            }
        }
    }

    private fun row(title: String, subtitle: String, onClick: () -> Unit) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.setPadding(0, Ui.dp(this, 12), 0, Ui.dp(this, 12))
        box.addView(Ui.text(this, title, 17f))
        val sub = Ui.text(this, subtitle, 13f, Ui.GRAY)
        sub.setPadding(0, Ui.dp(this, 2), 0, 0)
        box.addView(sub)
        box.setOnClickListener { onClick() }
        root.addView(box)
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}
