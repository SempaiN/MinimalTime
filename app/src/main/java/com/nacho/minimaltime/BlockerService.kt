package com.nacho.minimaltime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager

class BlockerService : Service() {

    companion object {
        private const val KIND_NONE = 0
        private const val KIND_BLOCK = 1
        private const val KIND_BREATH = 2
        private const val BREATH_SECONDS = 10
        private const val BREATH_VALID_MS = 5 * 60_000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null
    private var overlayKind = KIND_NONE
    private var overlayPkg: String? = null

    /** Última vez que el usuario completó el respiro por app. */
    private val breathOk = HashMap<String, Long>()

    private var cachedToday: Map<String, Long> = emptyMap()
    private var cacheAt = 0L
    @Volatile
    private var refreshing = false

    private var currentPkg: String? = null
    private var currentSince = 0L

    private val phrases = listOf(
        "tu tiempo vale más",
        "¿era esto lo que querías hacer hoy?",
        "la app seguirá ahí mañana",
        "respira. vuelve a lo importante",
        "cada minuto aquí es un minuto menos allí"
    )

    private val loop = object : Runnable {
        override fun run() {
            try {
                tick()
            } catch (_: Exception) {
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        startInForeground()
        handler.post(loop)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        hideOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startInForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("blocker", "Control de tiempo", NotificationManager.IMPORTANCE_MIN)
        )
        val notif = Notification.Builder(this, "blocker")
            .setSmallIcon(R.drawable.ic_notif)
            .setContentTitle("Control de tiempo activo")
            .setContentText("Vigilando límites y apps bloqueadas")
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(1, notif)
        }
    }

    private fun tick() {
        if (!UsageRepo.hasAccess(this)) return
        val fg = UsageRepo.foregroundPackage(this) ?: return
        if (fg != currentPkg) {
            currentPkg = fg
            currentSince = System.currentTimeMillis()
        }
        refreshCacheIfStale()
        val reason = blockReason(fg)
        when {
            reason != null -> showBlock(fg, Apps.label(this, fg), reason)
            needsBreath(fg) -> showBreath(fg, Apps.label(this, fg))
            else -> hideOverlay()
        }
    }

    private fun refreshCacheIfStale() {
        val now = System.currentTimeMillis()
        if (now - cacheAt < 30_000 || refreshing) return
        refreshing = true
        Thread {
            try {
                val m = UsageRepo.todayUsage(this)
                handler.post {
                    cachedToday = m
                    cacheAt = System.currentTimeMillis()
                    refreshing = false
                }
            } catch (_: Exception) {
                refreshing = false
            }
        }.start()
    }

    private fun blockReason(pkg: String): String? {
        if (pkg == packageName || pkg == "com.android.systemui") return null
        if (pkg in Prefs.blocked(this)) return "Has bloqueado esta aplicación"
        val dist = pkg in Prefs.distracting(this)
        if (dist && Prefs.monkActiveNow(this)) return "Modo monje activo"
        if (dist && Prefs.focusActive(this)) {
            val left = (Prefs.focusUntil(this) - System.currentTimeMillis()) / 60_000 + 1
            return "Modo concentración · $left min restantes"
        }
        val lim = Prefs.limits(this)[pkg] ?: return null
        var used = cachedToday[pkg] ?: 0L
        if (pkg == currentPkg) {
            used += (System.currentTimeMillis() - maxOf(currentSince, cacheAt)).coerceAtLeast(0)
        }
        if (used >= lim * 60_000L) return "Límite diario alcanzado ($lim min)"
        return null
    }

    private fun needsBreath(pkg: String): Boolean {
        if (!Prefs.breathing(this)) return false
        if (pkg == packageName || pkg == "com.android.systemui") return false
        if (pkg !in Prefs.distracting(this)) return false
        return System.currentTimeMillis() - (breathOk[pkg] ?: 0L) > BREATH_VALID_MS
    }

    // --- Pantalla de bloqueo ---

    private fun showBlock(pkg: String, label: String, reason: String) {
        if (overlayKind == KIND_BLOCK && overlayPkg == pkg) return
        hideOverlay()
        if (!Settings.canDrawOverlays(this)) return

        val box = Ui.column(this, 32)
        box.gravity = Gravity.CENTER
        box.setBackgroundColor(Color.BLACK)
        box.addView(Ui.text(this, label, 28f).apply { gravity = Gravity.CENTER })
        box.addView(Ui.space(this, 10))
        box.addView(Ui.text(this, reason, 15f, Ui.GRAY).apply { gravity = Gravity.CENTER })
        box.addView(Ui.space(this, 6))
        box.addView(
            Ui.text(this, phrases.random(), 13f, Ui.GRAY).apply { gravity = Gravity.CENTER }
        )
        box.addView(Ui.space(this, 48))
        val back = Ui.text(this, "‹  volver al inicio", 18f)
        back.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 14))
        back.setOnClickListener {
            hideOverlay()
            goHome()
        }
        box.addView(back)

        attach(box, KIND_BLOCK, pkg)
    }

    // --- Respiro consciente ---

    private fun showBreath(pkg: String, label: String) {
        if (overlayKind == KIND_BREATH && overlayPkg == pkg) return
        hideOverlay()
        if (!Settings.canDrawOverlays(this)) return

        val box = Ui.column(this, 32)
        box.gravity = Gravity.CENTER
        box.setBackgroundColor(Color.BLACK)
        box.addView(Ui.text(this, label, 24f).apply { gravity = Gravity.CENTER })
        box.addView(Ui.space(this, 8))
        box.addView(
            Ui.text(this, "un momento de pausa · respira hondo", 14f, Ui.GRAY)
                .apply { gravity = Gravity.CENTER }
        )
        box.addView(Ui.space(this, 36))
        val count = Ui.text(this, BREATH_SECONDS.toString(), 56f)
        count.gravity = Gravity.CENTER
        box.addView(count)
        box.addView(Ui.space(this, 36))

        var ready = false
        val cont = Ui.text(this, "continuar", 18f, Ui.DARK)
        cont.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 12))
        cont.setOnClickListener {
            if (!ready) return@setOnClickListener
            breathOk[pkg] = System.currentTimeMillis()
            hideOverlay()
        }
        box.addView(cont)

        val exit = Ui.text(this, "‹  mejor no, volver al inicio", 15f, Ui.GRAY)
        exit.setPadding(Ui.dp(this, 20), Ui.dp(this, 12), Ui.dp(this, 20), Ui.dp(this, 12))
        exit.setOnClickListener {
            hideOverlay()
            goHome()
        }
        box.addView(exit)

        attach(box, KIND_BREATH, pkg)

        var left = BREATH_SECONDS
        val countdown = object : Runnable {
            override fun run() {
                if (overlay !== box) return
                if (left > 0) {
                    count.text = left.toString()
                    left--
                    handler.postDelayed(this, 1000)
                } else {
                    count.text = "·"
                    cont.setTextColor(Ui.WHITE)
                    ready = true
                }
            }
        }
        handler.post(countdown)
    }

    private fun attach(box: View, kind: Int, pkg: String) {
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.OPAQUE
        )
        try {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).addView(box, lp)
            overlay = box
            overlayKind = kind
            overlayPkg = pkg
        } catch (_: Exception) {
        }
    }

    private fun goHome() {
        val home = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(home)
    }

    private fun hideOverlay() {
        val v = overlay ?: return
        try {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v)
        } catch (_: Exception) {
        }
        overlay = null
        overlayKind = KIND_NONE
        overlayPkg = null
    }
}
