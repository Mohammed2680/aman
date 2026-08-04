# أمان للديون - قواعد ProGuard / R8

# احتفظ بواجهة الجافاسكربت (AndroidBridge) وأسماء دوالها كما هي،
# لأن WebView يستدعيها بالاسم من كود JS مباشرة ولا يمكن تعتيمها.
-keepclassmembers class com.aman.debts.AndroidBridge {
   public *;
}
-keepclassmembers class * {
   @android.webkit.JavascriptInterface <methods>;
}

# مكوّنات WebView الأساسية
-keep class android.webkit.** { *; }
-dontwarn android.webkit.**

# AndroidX WebKit (WebViewAssetLoader)
-keep class androidx.webkit.** { *; }
-dontwarn androidx.webkit.**

# دعم عام لملفات AndroidManifest components (Activities تُستدعى بالاسم عبر Intent)
-keep public class * extends android.app.Activity
-keep public class * extends androidx.appcompat.app.AppCompatActivity

# الحفاظ على أسماء الأصناف المستخدمة عبر Reflection (Parcelable وما شابه)
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
