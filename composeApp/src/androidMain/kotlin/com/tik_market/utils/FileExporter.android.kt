package com.tik_market.utils

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.io.File

@Composable
actual fun rememberFileExporter(): (filename: String, content: String) -> Boolean {
    val context = LocalContext.current
    return { filename, content ->
        try {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadDir.exists()) downloadDir.mkdirs()
            val file = File(downloadDir, filename)
            file.writeText(content, Charsets.UTF_8)
            Toast.makeText(context, "✅ Fichier enregistré dans Téléchargements :\n$filename", Toast.LENGTH_LONG).show()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            try {
                // Fallback to internal files dir
                val file = File(context.filesDir, filename)
                file.writeText(content, Charsets.UTF_8)
                Toast.makeText(context, "✅ Fichier enregistré :\n$filename", Toast.LENGTH_LONG).show()
                true
            } catch (ex: Exception) {
                Toast.makeText(context, "❌ Erreur d'enregistrement : ${ex.message}", Toast.LENGTH_SHORT).show()
                false
            }
        }
    }
}
