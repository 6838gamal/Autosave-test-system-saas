package gamalsolutions.autosavetestsystem.export

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import gamalsolutions.autosavetestsystem.database.SavedContact
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportManager {

    /**
     * Exports saved contacts to a beautifully formatted UTF-8 CSV with BOM (for Arabic Excel compatibility).
     * Saves directly inside the shared public "Downloads" directory across Android 10-14, with safe older fallbacks.
     * @return Uri pointing to the generated file or null on failure.
     */
    fun exportContactsToCsv(context: Context, contacts: List<SavedContact>): String? {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "autosave_contacts_$timestamp.csv"
        
        val header = "الاسم,رقم الهاتف,مصدر الالتقاط,تاريخ الحفظ,آخر نشاط,عدد مرات التواصل\n"
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ar", "AE"))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ MediaStore approach (No storage permissions needed)
            val contentResolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues) ?: return null
            try {
                contentResolver.openOutputStream(uri)?.use { os ->
                    // Write UTF-8 BOM (0xEF, 0xBB, 0xBF) so Excel reads Arabic correctly
                    os.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    os.writer(Charsets.UTF_8).use { writer ->
                        writer.write(header)
                        for (item in contacts) {
                            val row = escapeCsv(item.name) + "," +
                                    escapeCsv(item.rawNumber) + "," +
                                    escapeCsv(getLocalizedSource(item.source)) + "," +
                                    escapeCsv(df.format(Date(item.timestamp))) + "," +
                                    escapeCsv(df.format(Date(item.lastActive))) + "," +
                                    item.interactionsCount + "\n"
                            writer.write(row)
                        }
                    }
                }
                return fileName
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        } else {
            // Legacy Android 9 and below file writing (using Downloads directory)
            try {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val outputFile = File(downloadsDir, fileName)
                FileOutputStream(outputFile).use { os ->
                    os.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    os.writer(Charsets.UTF_8).use { writer ->
                        writer.write(header)
                        for (item in contacts) {
                            val row = escapeCsv(item.name) + "," +
                                    escapeCsv(item.rawNumber) + "," +
                                    escapeCsv(getLocalizedSource(item.source)) + "," +
                                    escapeCsv(df.format(Date(item.timestamp))) + "," +
                                    escapeCsv(df.format(Date(item.lastActive))) + "," +
                                    item.interactionsCount + "\n"
                            writer.write(row)
                        }
                    }
                }
                return fileName
            } catch (e: Exception) {
                e.printStackTrace()
                // Fail-safe: write to app private directory
                try {
                    val fallbackFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                    FileOutputStream(fallbackFile).use { os ->
                        os.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                        os.writer(Charsets.UTF_8).use { writer ->
                            writer.write(header)
                            for (item in contacts) {
                                val row = escapeCsv(item.name) + "," +
                                        escapeCsv(item.rawNumber) + "," +
                                        escapeCsv(getLocalizedSource(item.source)) + "," +
                                        escapeCsv(df.format(Date(item.timestamp))) + "," +
                                        escapeCsv(df.format(Date(item.lastActive))) + "," +
                                        item.interactionsCount + "\n"
                                writer.write(row)
                            }
                        }
                    }
                    return fallbackFile.absolutePath
                } catch (e2: Exception) {
                    e2.printStackTrace()
                    return null
                }
            }
        }
    }

    private fun escapeCsv(value: String): String {
        var str = value.replace("\"", "\"\"")
        if (str.contains(",") || str.contains("\n") || str.contains("\"")) {
            str = "\"$str\""
        }
        return str
    }

    private fun getLocalizedSource(source: String): String {
        return when (source) {
            "incoming_call" -> "مكالمة واردة"
            "outgoing_call" -> "مكالمة صادرة"
            "sms" -> "رسالة SMS"
            "whatsapp" -> "إشعار واتساب"
            "accessibility" -> "إمكانية الوصول"
            else -> source
        }
    }
}
