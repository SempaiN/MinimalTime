package com.nacho.minimaltime

import android.content.Context
import android.content.Intent

data class AppInfo(val label: String, val pkg: String)

object Apps {

    fun launchable(c: Context): List<AppInfo> {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = c.packageManager
        return pm.queryIntentActivities(i, 0)
            .mapNotNull { r ->
                val pkg = r.activityInfo.packageName
                if (pkg == c.packageName) null
                else AppInfo(r.loadLabel(pm).toString(), pkg)
            }
            .distinctBy { it.pkg }
            .sortedBy { it.label.lowercase() }
    }

    fun open(c: Context, pkg: String) {
        val i = c.packageManager.getLaunchIntentForPackage(pkg) ?: return
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        c.startActivity(i)
    }

    fun label(c: Context, pkg: String): String = try {
        val pm = c.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }
}
