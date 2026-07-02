package com.nacho.minimaltime

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.LinearLayout

class AppsActivity : Activity() {

    private lateinit var listBox: LinearLayout
    private lateinit var search: EditText
    private var all: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = Ui.column(this, 24)
        root.addView(Ui.text(this, "aplicaciones", 22f))
        root.addView(Ui.space(this, 12))

        search = EditText(this)
        search.hint = "buscar…"
        search.setTextColor(Ui.WHITE)
        search.setHintTextColor(Ui.GRAY)
        search.textSize = 16f
        search.isSingleLine = true
        search.background?.setTint(Ui.DARK)
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = render()
        })
        root.addView(search)
        root.addView(Ui.space(this, 12))

        listBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val scroll = Ui.scroll(this, listBox)
        root.addView(
            scroll,
            LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
        )

        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        Thread {
            val apps = Apps.launchable(this)
            runOnUiThread {
                all = apps
                render()
            }
        }.start()
    }

    private fun render() {
        listBox.removeAllViews()
        val q = search.text.toString().trim().lowercase()
        val favs = Prefs.favorites(this)
        val dist = Prefs.distracting(this)
        val blocked = Prefs.blocked(this)
        val limits = Prefs.limits(this)

        val shown = if (q.isEmpty()) all else all.filter { q in it.label.lowercase() }
        for (app in shown) {
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            row.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10))

            row.addView(Ui.text(this, app.label, 19f))

            val meta = ArrayList<String>()
            if (app.pkg in favs) meta.add("favorita")
            if (app.pkg in dist) meta.add("distractora")
            if (app.pkg in blocked) meta.add("bloqueada")
            limits[app.pkg]?.let { meta.add("límite $it min") }
            if (meta.isNotEmpty()) {
                row.addView(Ui.text(this, meta.joinToString(" · "), 12f, Ui.GRAY))
            }

            row.setOnClickListener { Apps.open(this, app.pkg) }
            row.setOnLongClickListener {
                Dialogs.appOptions(this, app) { render() }
                true
            }
            listBox.addView(row)
        }
    }
}
