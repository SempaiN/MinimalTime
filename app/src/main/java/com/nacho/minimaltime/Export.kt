package com.nacho.minimaltime

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Export {

    /** Exporta el uso de hoy a un CSV en Descargas. Devuelve la ruta o null si falla. */
    fun todayCsv(c: Context): String? {
        if (!UsageRepo.hasAccess(c)) return null
        val labels = HashMap<String, String>()
        for (a in Apps.launchable(c)) labels[a.pkg] = a.label
        val usage = UsageRepo.todayUsage(c).filterKeys { it in labels.keys }
        val opens = UsageRepo.opensToday(c)

        val sb = StringBuilder("app;paquete;minutos;aperturas\n")
        for ((pkg, ms) in usage.entries.sortedByDescending { it.value }) {
            sb.append('"').append(labels[pkg]).append('"').append(';')
                .append(pkg).append(';')
                .append(ms / 60000).append(';')
                .append(opens[pkg] ?: 0).append('\n')
        }

        val name = "minimal-time-" +
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) + ".csv"
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                val cv = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, name)
                    put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = c.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv
                ) ?: return null
                c.contentResolver.openOutputStream(uri)?.use {
                    it.write(sb.toString().toByteArray(Charsets.UTF_8))
                }
                "Descargas/$name"
            } else {
                val f = File(c.getExternalFilesDir(null), name)
                f.writeText(sb.toString())
                f.absolutePath
            }
        } catch (e: Exception) {
            null
        }
    }
}
