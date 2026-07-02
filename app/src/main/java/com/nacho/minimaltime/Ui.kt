package com.nacho.minimaltime

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object Ui {
    const val WHITE = Color.WHITE
    val GRAY = Color.parseColor("#9E9E9E")
    val DARK = Color.parseColor("#333333")

    fun dp(c: Context, v: Int): Int = (v * c.resources.displayMetrics.density).toInt()

    fun text(c: Context, s: String, sizeSp: Float, color: Int = WHITE, light: Boolean = true): TextView {
        val t = TextView(c)
        t.text = s
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
        t.setTextColor(color)
        t.typeface = if (light) Typeface.create("sans-serif-light", Typeface.NORMAL)
        else Typeface.create("sans-serif-medium", Typeface.NORMAL)
        return t
    }

    fun column(c: Context, padDp: Int = 24): LinearLayout {
        val l = LinearLayout(c)
        l.orientation = LinearLayout.VERTICAL
        val p = dp(c, padDp)
        l.setPadding(p, p, p, p)
        l.setBackgroundColor(Color.BLACK)
        return l
    }

    fun scroll(c: Context, inner: View): ScrollView {
        val s = ScrollView(c)
        s.isVerticalScrollBarEnabled = false
        s.setBackgroundColor(Color.BLACK)
        s.addView(inner)
        return s
    }

    fun divider(c: Context): View {
        val v = View(c)
        v.setBackgroundColor(DARK)
        v.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(c, 1)
        )
        return v
    }

    fun space(c: Context, hDp: Int): View {
        val v = View(c)
        v.layoutParams = LinearLayout.LayoutParams(1, dp(c, hDp))
        return v
    }

    fun fmt(ms: Long): String {
        val m = ms / 60000
        if (m < 1) return if (ms > 0) "menos de 1 min" else "0 min"
        val h = m / 60
        val r = m % 60
        return if (h > 0) "$h h $r min" else "$r min"
    }
}
