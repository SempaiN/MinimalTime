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

    private val handler = Handler(Looper.getMainLooper())
    private var overlay: View? = null

    private var cachedToday: Map<String, Long> = emptyMap()
    private var cacheAt = 0L
    @Volatile
    private var refreshing = false

    private var currentPkg: String? = null
    private var currentSince = 0L

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
        if (reason != null) {
            showOverlay(Apps.label(this, fg), reason)
        } else {
            hideOverlay()
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
        if (Prefs.monk(this) && pkg in Prefs.distracting(this)) return "Modo monje activo"
        val lim = Prefs.limits(this)[pkg] ?: return null
        var used = cachedToday[pkg] ?: 0L
        if (pkg == currentPkg) {
            used += (System.currentTimeMillis() - maxOf(currentSince, cacheAt)).coerceAtLeast(0)
        }
        if (used >= lim * 60_000L) return "Límite diario alcanzado ($lim min)"
        return null
    }

    private fun showOverlay(label: String, reason: String) {
        if (overlay != null) return
        if (!Settings.canDrawOverlays(this)) return

        val box = Ui.column(this, 32)
        box.gravity = Gravity.CENTER
        box.setBackgroundColor(Color.BLACK)
        box.addView(Ui.text(this, label, 28f).apply { gravity = Gravity.CENTER })
        box.addView(Ui.space(this, 10))
        box.addView(Ui.text(this, reason, 15f, Ui.GRAY).apply { gravity = Gravity.CENTER })
        box.addView(Ui.space(this, 6))
        box.addView(
            Ui.text(this, "tu tiempo vale más", 13f, Ui.GRAY).apply { gravity = Gravity.CENTER }
        )
        box.addView(Ui.space(this, 48))
        val back = Ui.text(this, "‹  volver al inicio", 18f)
        back.setPadding(Ui.dp(this, 20), Ui.dp(this, 14), Ui.dp(this, 20), Ui.dp(this, 14))
        back.setOnClickListener {
            hideOverlay()
            val home = Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(home)
        }
        box.addView(back)

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
        } catch (_: Exception) {
        }
    }

    private fun hideOverlay() {
        val v = overlay ?: return
        try {
            (getSystemService(Context.WINDOW_SERVICE) as WindowManager).removeView(v)
        } catch (_: Exception) {
        }
        overlay = null
    }
}
