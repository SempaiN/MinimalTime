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

        // --- Servicio de bloqueo ---
        val on = Prefs.blockerOn(this)
        row(
            "servicio de bloqueo  ·  " + if (on) "activado" else "desactivado",
            "necesario para límites diarios, apps bloqueadas y modo monje"
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

        // --- Acceso de uso ---
        row(
            "acceso a datos de uso  ·  " + estado(UsageRepo.hasAccess(this)),
            "permite leer el tiempo de pantalla real del sistema"
        ) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }

        // --- Superposición ---
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

        // --- Notificaciones (Android 13+) ---
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

        // --- Launcher predeterminado ---
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
            "acerca de",
            "réplica educativa de un launcher minimalista con control " +
                    "de tiempo de pantalla. hecha en local, sin conexión, " +
                    "sin anuncios y sin recopilar datos."
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
