package com.nacho.minimaltime

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsActivity : Activity() {

    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = Ui.column(this, 24)
        setContentView(Ui.scroll(this, root))
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        root.removeAllViews()
        root.addView(Ui.text(this, "tiempo de pantalla", 22f))
        root.addView(Ui.space(this, 16))

        if (!UsageRepo.hasAccess(this)) {
            root.addView(
                Ui.text(
                    this,
                    "para ver tus estadísticas reales, esta app\nnecesita el permiso de acceso al uso.",
                    15f, Ui.GRAY
                )
            )
            root.addView(Ui.space(this, 20))
            val btn = Ui.text(this, "conceder acceso de uso  ›", 17f)
            btn.setOnClickListener {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            root.addView(btn)
            return
        }

        val loading = Ui.text(this, "cargando…", 14f, Ui.GRAY)
        root.addView(loading)

        Thread {
            try {
                val apps = Apps.launchable(this)
                val labels = HashMap<String, String>()
                for (a in apps) labels[a.pkg] = a.label
                val set = labels.keys

                val today = UsageRepo.todayUsage(this).filterKeys { it in set }
                val total = today.values.sum()
                val unlocks = UsageRepo.unlocksToday(this)
                val opens = UsageRepo.opensToday(this)
                val week = UsageRepo.weekTotals(this, set)

                runOnUiThread { render(total, unlocks, week, today, opens, labels) }
            } catch (_: Exception) {
                runOnUiThread { loading.text = "no se pudieron leer los datos de uso" }
            }
        }.start()
    }

    private fun render(
        total: Long,
        unlocks: Int,
        week: LongArray,
        today: Map<String, Long>,
        opens: Map<String, Int>,
        labels: Map<String, String>
    ) {
        root.removeAllViews()
        root.addView(Ui.text(this, "tiempo de pantalla", 22f))
        root.addView(Ui.space(this, 24))

        root.addView(Ui.text(this, Ui.fmt(total), 46f))
        root.addView(Ui.text(this, "hoy", 14f, Ui.GRAY))

        val goal = Prefs.dailyGoalMin(this)
        if (goal > 0) {
            val pct = (100L * total / (goal * 60_000L)).toInt()
            val txt = if (pct <= 100) "objetivo: ${Ui.fmt(goal * 60_000L)} · $pct %"
            else "objetivo de ${Ui.fmt(goal * 60_000L)} superado ($pct %)"
            root.addView(Ui.space(this, 6))
            root.addView(Ui.text(this, txt, 13f, Ui.GRAY))
        }

        val yesterday = week[5]
        if (yesterday > 0) {
            val diff = total - yesterday
            val pct = (100.0 * kotlin.math.abs(diff) / yesterday).toInt()
            val txt = if (diff <= 0) "↓ $pct % menos que ayer" else "↑ $pct % más que ayer"
            root.addView(Ui.space(this, 6))
            root.addView(Ui.text(this, txt, 13f, Ui.GRAY))
        }

        if (unlocks >= 0) {
            root.addView(Ui.space(this, 10))
            root.addView(Ui.text(this, "$unlocks desbloqueos hoy", 13f, Ui.GRAY))
        }

        root.addView(Ui.space(this, 28))
        root.addView(Ui.text(this, "últimos 7 días", 14f, Ui.GRAY))
        root.addView(Ui.space(this, 10))

        val chart = BarChartView(this, week, dayLabels())
        root.addView(
            chart,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Ui.dp(this, 140))
        )

        root.addView(Ui.space(this, 28))
        root.addView(Ui.divider(this))
        root.addView(Ui.space(this, 16))
        root.addView(Ui.text(this, "por aplicación", 14f, Ui.GRAY))
        root.addView(Ui.space(this, 8))

        val sorted = today.entries.filter { it.value > 30_000 }.sortedByDescending { it.value }
        val max = sorted.firstOrNull()?.value ?: 1L
        val screenW = resources.displayMetrics.widthPixels - Ui.dp(this, 48)

        for ((pkg, ms) in sorted) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            row.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 4))

            val line = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            val name = Ui.text(this, labels[pkg] ?: pkg, 16f)
            line.addView(name, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
            val n = opens[pkg] ?: 0
            val time = Ui.text(this, Ui.fmt(ms) + if (n > 0) "  ·  $n×" else "", 13f, Ui.GRAY)
            time.gravity = Gravity.END
            line.addView(time)
            row.addView(line)

            val bar = View(this)
            bar.setBackgroundColor(Ui.WHITE)
            val w = ((screenW.toLong() * ms) / max).toInt().coerceAtLeast(Ui.dp(this, 2))
            val lp = LinearLayout.LayoutParams(w, Ui.dp(this, 2))
            lp.topMargin = Ui.dp(this, 6)
            row.addView(bar, lp)

            root.addView(row)
        }
        root.addView(Ui.space(this, 24))
    }

    private fun dayLabels(): List<String> {
        val fmt = SimpleDateFormat("EEE", Locale("es", "ES"))
        val out = ArrayList<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        repeat(7) {
            out.add(fmt.format(cal.time).take(2).lowercase())
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return out
    }

    private class BarChartView(
        c: Context,
        private val values: LongArray,
        private val labels: List<String>
    ) : View(c) {

        private val barPaint = Paint().apply { color = Color.parseColor("#3A3A3A") }
        private val todayPaint = Paint().apply { color = Color.WHITE }
        private val textPaint = Paint().apply {
            color = Color.parseColor("#9E9E9E")
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            textSize = 10 * c.resources.displayMetrics.scaledDensity
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val max = (values.maxOrNull() ?: 1L).coerceAtLeast(1L)
            val n = values.size
            val labelH = 22 * resources.displayMetrics.density
            val chartH = height - labelH
            val slot = width.toFloat() / n
            val barW = slot * 0.38f

            for (i in 0 until n) {
                val h = chartH * 0.92f * values[i] / max
                val cx = slot * i + slot / 2
                val paint = if (i == n - 1) todayPaint else barPaint
                canvas.drawRect(cx - barW / 2, chartH - h, cx + barW / 2, chartH, paint)
                canvas.drawText(labels[i], cx, height - 6 * resources.displayMetrics.density, textPaint)
            }
        }
    }
}
