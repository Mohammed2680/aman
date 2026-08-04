package com.aman.debts

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Base64
import android.webkit.JavascriptInterface
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

/**
 * الجسر بين صفحات الويب (JavaScript) وواجهات أندرويد الأصلية.
 * يُستدعى من js/android-bridge.js عبر الكائن العام window.AndroidBridge.
 *
 * ملاحظة أمان: تُستدعى كل دالة معلّمة بـ @JavascriptInterface من صفحات
 * محمّلة محلياً فقط (assets عبر WebViewAssetLoader)، ولا تُحمَّل أي صفحة
 * خارجية بنفس WebView (راجع MainActivity.shouldOverrideUrlLoading)، وهو
 * الشرط الأساسي لأمان استخدام addJavascriptInterface.
 */
class AndroidBridge(private val activity: MainActivity) {

    private val mainHandler = Handler(Looper.getMainLooper())

    private fun runOnUi(block: () -> Unit) = mainHandler.post(block)

    private fun toast(msg: String) = runOnUi {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
    }

    /** حفظ ملف (PDF / صورة / Word) في مجلد التنزيلات العام على الجهاز. */
    @JavascriptInterface
    fun saveFile(base64Data: String, filename: String, mime: String): String {
        return try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val savedOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveViaMediaStore(bytes, filename, mime)
            } else {
                saveLegacy(bytes, filename)
            }
            if (savedOk) toast("تم حفظ \"$filename\" في مجلد التنزيلات")
            JSONObject().put("success", savedOk).toString()
        } catch (e: Exception) {
            JSONObject().put("success", false).put("error", e.message ?: "unknown_error").toString()
        }
    }

    private fun saveViaMediaStore(bytes: ByteArray, filename: String, mime: String): Boolean {
        val resolver = activity.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, filename)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: return false
        values.clear()
        values.put(MediaStore.Downloads.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return true
    }

    private fun saveLegacy(bytes: ByteArray, filename: String): Boolean {
        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, filename)
        FileOutputStream(file).use { it.write(bytes) }
        return true
    }

    /** مشاركة ملف عبر قائمة مشاركة النظام (واتساب/تيليجرام/إيميل/أي تطبيق آخر). */
    @JavascriptInterface
    fun shareFile(base64Data: String, filename: String, mime: String) {
        try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val shareDir = File(activity.cacheDir, "shared").apply { mkdirs() }
            val file = File(shareDir, filename)
            FileOutputStream(file).use { it.write(bytes) }
            val uri: Uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
            runOnUi {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = mime
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                activity.startActivity(Intent.createChooser(intent, "مشاركة الفاتورة"))
            }
        } catch (e: Exception) {
            toast("تعذر تجهيز الملف للمشاركة")
        }
    }

    /** نسخ نص إلى الحافظة (بديل موثوق عن navigator.clipboard في WebView). */
    @JavascriptInterface
    fun copyText(text: String) {
        runOnUi {
            val cm = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("aman_debts", text))
            toast("تم نسخ النص")
        }
    }

    /** مشاركة نص عام عبر قائمة مشاركة النظام. */
    @JavascriptInterface
    fun shareText(text: String) {
        runOnUi {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
            }
            activity.startActivity(Intent.createChooser(intent, "مشاركة"))
        }
    }

    /** فتح واتساب مباشرة (أو المتصفح إن لم يكن مثبتاً) مع نص جاهز. */
    @JavascriptInterface
    fun openWhatsapp(text: String) {
        runOnUi {
            openUrlExternally("https://wa.me/?text=${Uri.encode(text)}")
        }
    }

    /** فتح تيليجرام لمشاركة نص. */
    @JavascriptInterface
    fun openTelegram(text: String) {
        runOnUi {
            openUrlExternally("https://t.me/share/url?url=&text=${Uri.encode(text)}")
        }
    }

    /** فتح تطبيق البريد الإلكتروني الافتراضي مع عنوان ونص جاهزين. */
    @JavascriptInterface
    fun sendEmail(subject: String, body: String) {
        runOnUi {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            try {
                activity.startActivity(Intent.createChooser(intent, "إرسال عبر البريد الإلكتروني"))
            } catch (e: Exception) {
                toast("لا يوجد تطبيق بريد إلكتروني مثبّت")
            }
        }
    }

    /** طباعة الصفحة الحالية عبر خدمة الطباعة الأصلية في أندرويد. */
    @JavascriptInterface
    fun printPage() {
        runOnUi {
            val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
            val jobName = "${activity.getString(R.string.app_name)}-Invoice"
            val adapter = activity.webView.createPrintDocumentAdapter(jobName)
            printManager.print(jobName, adapter, PrintAttributes.Builder().build())
        }
    }

    private fun openUrlExternally(url: String) {
        try {
            activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            toast("تعذر فتح التطبيق المطلوب")
        }
    }
}
