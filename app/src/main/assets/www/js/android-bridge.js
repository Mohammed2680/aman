/* =========================================================
   android-bridge.js
   يربط دوال الويب (تنزيل/مشاركة/طباعة/نسخ) بواجهة Android
   الأصلية (AndroidBridge المُضافة عبر addJavascriptInterface)
   عند تشغيل الصفحة داخل تطبيق أندرويد. في حال فتح نفس الملفات
   من متصفح عادي (بدون AndroidBridge) يعمل كل شيء كما هو تمامًا
   بالطريقة القديمة (تنزيل/مشاركة عبر المتصفح) دون أي تغيير.
   ========================================================= */
(function () {
  var isAndroid = typeof window.AndroidBridge !== "undefined";
  window.IS_ANDROID_APP = isAndroid;
  if (!isAndroid) return;

  /* تحويل Blob/DataURL إلى base64 خام (بدون البادئة data:...;base64,) */
  function dataUrlToBase64(dataUrl) {
    var comma = dataUrl.indexOf(",");
    return comma > -1 ? dataUrl.slice(comma + 1) : dataUrl;
  }
  function blobToBase64(blob) {
    return new Promise(function (resolve, reject) {
      var reader = new FileReader();
      reader.onloadend = function () { resolve(dataUrlToBase64(reader.result)); };
      reader.onerror = reject;
      reader.readAsDataURL(blob);
    });
  }

  /* حفظ ملف عبر Android (يعيد true عند النجاح) */
  window.androidSaveFile = async function (data, filename, mime) {
    var b64 = data instanceof Blob ? await blobToBase64(data) : dataUrlToBase64(data);
    return JSON.parse(window.AndroidBridge.saveFile(b64, filename, mime));
  };

  /* مشاركة ملف عبر قائمة المشاركة الأصلية في أندرويد */
  window.androidShareFile = async function (data, filename, mime) {
    var b64 = data instanceof Blob ? await blobToBase64(data) : dataUrlToBase64(data);
    window.AndroidBridge.shareFile(b64, filename, mime);
  };

  /* نسخ نص للحافظة (بديل موثوق عن navigator.clipboard) */
  window.androidCopyText = function (text) {
    window.AndroidBridge.copyText(text);
    return true;
  };

  /* مشاركة نص عام (تفتح قائمة مشاركة النظام) */
  window.androidShareText = function (text) {
    window.AndroidBridge.shareText(text);
  };

  /* فتح واتساب/تيليجرام/الإيميل عبر تطبيقات النظام مباشرة */
  window.androidOpenWhatsapp = function (text) { window.AndroidBridge.openWhatsapp(text); };
  window.androidOpenTelegram = function (text) { window.AndroidBridge.openTelegram(text); };
  window.androidSendEmail = function (subject, body) { window.AndroidBridge.sendEmail(subject, body); };

  /* طباعة أصلية عبر خدمة الطباعة في أندرويد بدل window.print() */
  var originalPrint = window.print ? window.print.bind(window) : function () {};
  window.print = function () {
    try { window.AndroidBridge.printPage(); } catch (e) { originalPrint(); }
  };

  /* navigator.clipboard.writeText قد لا يعمل من أصل محلي غير آمن، لذا نوفر بديلاً */
  if (!navigator.clipboard) navigator.clipboard = {};
  var originalWriteText = navigator.clipboard.writeText ? navigator.clipboard.writeText.bind(navigator.clipboard) : null;
  navigator.clipboard.writeText = function (text) {
    try {
      window.androidCopyText(text);
      return Promise.resolve();
    } catch (e) {
      return originalWriteText ? originalWriteText(text) : Promise.reject(e);
    }
  };
})();
