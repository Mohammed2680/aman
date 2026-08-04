# أمان للديون - Android (مشروع Android Studio)

تطبيق أندرويد أصلي (Kotlin) يستضيف نفس تطبيق الويب الأصلي بالكامل (HTML/CSS/JS، localStorage)
داخل WebView محلي، مع جسر Kotlin↔JavaScript (`AndroidBridge`) يضيف: حفظ الملفات في مجلد
التنزيلات، المشاركة عبر قائمة النظام (واتساب/تيليجرام/بريد/أي تطبيق)، طباعة أصلية عبر خدمة
طباعة أندرويد، ونسخ للحافظة. هذا يحافظ على **كل ميزة موجودة في نسخة الويب دون أي حذف أو تبسيط**.

## ⚠️ خطوة واحدة مطلوبة قبل أول Build: توليد `gradle-wrapper.jar`

هذا الملف ثنائي (bytecode مُوقَّع من Gradle نفسه) ولا يمكن توليده يدوياً كنص. لتوليده:

**الخيار الأول (الأسهل - داخل Android Studio):**
1. افتح المشروع في Android Studio.
2. عند ظهور تنبيه بخصوص ملف الـ wrapper الناقص، اختر "Use Gradle from: 'gradle-wrapper.properties' file" ثم دع Android Studio يعيد بناء الـ wrapper تلقائياً، أو
3. من Terminal داخل Android Studio نفّذ الأمر أدناه (الخيار الثاني).

**الخيار الثاني (من أي جهاز عليه Gradle مثبت مسبقاً):**
```bash
gradle wrapper --gradle-version 8.7 --distribution-type bin
```
هذا الأمر يُعيد توليد `gradlew` و`gradlew.bat` و`gradle/wrapper/gradle-wrapper.jar`
و`gradle-wrapper.properties` تلقائياً ومطابقة لبعضها — نفّذه مرة واحدة فقط من داخل مجلد المشروع.

بعد ذلك: `./gradlew assembleDebug` أو ببساطة زر Run في Android Studio.

## اسم الحزمة (Package Name)

المشروع الأصلي كان تطبيق ويب وليس له اسم حزمة أندرويد سابق، لذا تم استخدام:
`com.aman.debts`
عدّله من `app/build.gradle.kts` (`applicationId` و `namespace`) وفي `AndroidManifest.xml` إذا رغبت باسم آخر.

## البنية

```
AmanDebts/
├── app/
│   ├── src/main/
│   │   ├── java/com/aman/debts/
│   │   │   ├── MainActivity.kt        # يستضيف WebView ويُعدّه (WebViewAssetLoader, ملفات، صلاحيات...)
│   │   │   ├── AndroidBridge.kt       # الجسر الأصلي المستدعى من JavaScript (window.AndroidBridge)
│   │   │   └── AmanApplication.kt
│   │   ├── assets/www/                # نفس ملفات الويب الأصلية بالكامل (HTML/CSS/JS) + android-bridge.js
│   │   ├── res/                       # الأيقونات، الألوان، الثيم، التخطيط (activity_main.xml)
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew / gradlew.bat
└── gradle/wrapper/gradle-wrapper.properties
```

## ملاحظات مهمة

- **الإنترنت مطلوب عند أول تشغيل**: نفس ملفات الويب الأصلية تحمّل Bootstrap وFont Awesome
  وSweetAlert2 وQRCode.js وJsBarcode وhtml2canvas وjsPDF من CDN (كما كانت بالضبط في نسخة
  الويب) — لم يتم تغيير هذا حتى لا تُحذف أي وظيفة. إن رغبت بنسخة تعمل بدون إنترنت بالكامل،
  حمّل هذه المكتبات محلياً وضعها داخل `assets/www` وعدّل روابط `<script>`/`<link>` في ملفات HTML.
- **البيانات (العملاء/الديون/الإعدادات)** ما زالت مخزّنة عبر `localStorage` داخل WebView (كما في
  نسخة الويب تماماً)، وتُصدَّر أيضاً ضمن نسخ أندرويد الاحتياطية (`android:allowBackup`).
- **مزامنة Firebase**: كما في نسخة الويب، `js/firebase.js` جاهز بنفس النمط لكنه يحتاج بيانات
  مشروع Firebase حقيقية لتفعيل المزامنة الفعلية بين الأجهزة.
- تم اختبار جميع ملفات Kotlin وXML يدوياً للتأكد من صحة الصياغة والربط بين المعرّفات (IDs)،
  لكن لا تتوفر في بيئة التوليد حزمة Android SDK/Gradle كاملة لتنفيذ Build فعلي والتحقق النهائي؛
  الخطوة الوحيدة المطلوبة منك هي توليد `gradle-wrapper.jar` أعلاه ثم فتح المشروع في Android Studio.
