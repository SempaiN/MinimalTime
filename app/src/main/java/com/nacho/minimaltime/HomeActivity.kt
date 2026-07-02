package com.nacho.minimaltime

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HomeActivity : Activity() {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var greeting: TextView
    private lateinit var clock: TextView
    private lateinit var date: TextView
    private lateinit var favsBox: LinearLayout
    private lateinit var stateLabel: TextView
    private lateinit var summary: TextView
    private lateinit var goalLine: TextView

    private val es = Locale("es", "ES")
    private val hourFmt = SimpleDateFormat("HH:mm", es)
    private val dateFmt = SimpleDateFormat("EEEE d 'de' MMMM", es)

    private val tick = object : Runnable {
        override fun run() {
            updateClock()
            updateState()
            handler.postDelayed(this, 20_000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = Ui.column(this, 28)

        greeting = Ui.text(this, "", 15f, Ui.GRAY)
        clock = Ui.text(this, "", 64f)
        date = Ui.text(this, "", 16f, Ui.GRAY)
        favsBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        stateLabel = Ui.text(this, "", 14f)
        summary = Ui.text(this, "", 14f, Ui.GRAY)
        goalLine = Ui.text(this, "", 13f, Ui.GRAY)

        root.addView(greeting)
        root.addView(Ui.space(this, 4))
        root.addView(clock)
        root.addView(date)
        root.addView(Ui.space(this, 40))
        root.addView(favsBox)

        val spacer = View(this)
        root.addView(spacer, LinearLayout.LayoutParams(1, 0, 1f))

        root.addView(stateLabel)
        root.addView(summary)
        root.addView(goalLine)
        root.addView(Ui.space(this, 16))

        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        nav.addView(navItem("aplicaciones") { startActivity(Intent(this, AppsActivity::class.java)) })
        nav.addView(navItem("tiempo") { startActivity(Intent(this, StatsActivity::class.java)) })
        nav.addView(navItem("enfocar") { Dialogs.focusPicker(this) { updateState() } })
        nav.addView(navItem("ajustes") { startActivity(Intent(this, SettingsActivity::class.java)) })
        root.addView(nav)

        setContentView(root)
    }

    private fun navItem(label: String, onClick: () -> Unit): TextView {
        val t = Ui.text(this, label, 15f, Ui.GRAY)
        t.setPadding(0, Ui.dp(this, 8), Ui.dp(this, 22), Ui.dp(this, 8))
        t.setOnClickListener { onClick() }
        return t
    }

    override fun onResume() {
        super.onResume()
        updateClock()
        updateState()
        handler.postDelayed(tick, 20_000)
        val name = Prefs.userName(this)
        greeting.text = if (name.isBlank()) "" else "hola, ${name.lowercase()}"
        renderFavorites()
        loadSummary()
        // Reactiva el servicio de bloqueo si estaba activado
        if (Prefs.blockerOn(this) && UsageRepo.hasAccess(this)) {
            try {
                startForegroundService(Intent(this, BlockerService::class.java))
            } catch (_: Exception) {
            }
        }
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    private fun updateClock() {
        val now = Date()
        clock.text = hourFmt.format(now)
        date.text = dateFmt.format(now)
    }

    private fun updateState() {
        val states = ArrayList<String>()
        if (Prefs.monk(this)) {
            states.add("● modo monje activo")
        } else if (Prefs.monkSchedOn(this) && Prefs.monkActiveNow(this)) {
            states.add("● modo monje (horario)")
        }
        val fu = Prefs.focusUntil(this)
        val now = System.currentTimeMillis()
        if (fu > now) {
            states.add("● concentración: ${(fu - now) / 60_000 + 1} min restantes")
        }
        stateLabel.text = states.joinToString("\n")
    }

    private fun renderFavorites() {
        favsBox.removeAllViews()
        val favs = Prefs.favorites(this)
        if (favs.isEmpty()) {
            val hint = Ui.text(
                this,
                "sin favoritas todavía.\nen \"aplicaciones\", mantén pulsada una app\npara añadirla aquí.",
                14f, Ui.GRAY
            )
            favsBox.addView(hint)
            return
        }
        val apps = favs.map { AppInfo(Apps.label(this, it), it) }.sortedBy { it.label.lowercase() }
        for (app in apps) {
            val t = Ui.text(this, app.label.lowercase(), 26f)
            t.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10))
            t.setOnClickListener { Apps.open(this, app.pkg) }
            t.setOnLongClickListener {
                Dialogs.appOptions(this, app) { renderFavorites() }
                true
            }
            favsBox.addView(t)
        }
    }

    private fun loadSummary() {
        if (!UsageRepo.hasAccess(this)) {
            summary.text = "concede acceso de uso en ajustes para ver tu tiempo"
            goalLine.text = ""
            return
        }
        Thread {
            try {
                val set = Apps.launchable(this).map { it.pkg }.toSet()
                val today = UsageRepo.todayUsage(this)
                var total = 0L
                for ((k, v) in today) if (k in set) total += v
                val unlocks = UsageRepo.unlocksToday(this)
                val txt = buildString {
                    append("hoy: ").append(Ui.fmt(total))
                    if (unlocks >= 0) append("  ·  ").append(unlocks).append(" desbloqueos")
                }
                val goal = Prefs.dailyGoalMin(this)
                val goalTxt = if (goal > 0) {
                    val pct = (100L * total / (goal * 60_000L)).toInt()
                    if (pct <= 100) "objetivo ${Ui.fmt(goal * 60_000L)} · $pct %"
                    else "objetivo superado ($pct %)"
                } else ""
                runOnUiThread {
                    summary.text = txt
                    goalLine.text = goalTxt
                }
            } catch (_: Exception) {
            }
        }.start()
    }
}
