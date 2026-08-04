#!/usr/bin/env bash
# =============================================================================
# fix-gradle-wrapper.sh
#
# يقوم هذا السكربت بجلب ملف gradle-wrapper.jar الرسمي المتوافق تمامًا مع
# الإصدار المحدد في gradle/wrapper/gradle-wrapper.properties (Gradle 8.7)
# ويضعه في المكان الصحيح: gradle/wrapper/gradle-wrapper.jar
#
# شغّله مرة واحدة فقط من جذر المشروع (نفس مجلد gradlew):
#   chmod +x fix-gradle-wrapper.sh
#   ./fix-gradle-wrapper.sh
#
# بعدها استخدم ./gradlew assembleDebug بشكل طبيعي.
# =============================================================================

set -euo pipefail

PROP_FILE="gradle/wrapper/gradle-wrapper.properties"
TARGET="gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$PROP_FILE" ]; then
  echo "خطأ: لم أجد $PROP_FILE. تأكد أنك تشغّل السكربت من جذر المشروع."
  exit 1
fi

# استخراج رقم إصدار Gradle من distributionUrl (مثال: gradle-8.7-bin.zip -> 8.7)
GRADLE_VERSION=$(grep -oE 'gradle-[0-9]+\.[0-9]+(\.[0-9]+)?-(bin|all)' "$PROP_FILE" | head -1 | sed -E 's/gradle-([0-9.]+)-(bin|all)/\1/')

if [ -z "$GRADLE_VERSION" ]; then
  echo "تعذر استخراج إصدار Gradle من $PROP_FILE، سيتم استخدام 8.7 كافتراضي."
  GRADLE_VERSION="8.7"
fi

echo "إصدار Gradle المكتشف: $GRADLE_VERSION"
echo "جارٍ تحميل gradle-wrapper.jar المطابق..."

mkdir -p "$(dirname "$TARGET")"

URL="https://raw.githubusercontent.com/gradle/gradle/v${GRADLE_VERSION}.0/gradle/wrapper/gradle-wrapper.jar"

if command -v curl >/dev/null 2>&1; then
  curl -fL -o "$TARGET" "$URL" || {
    echo "تعذر التحميل من GitHub، سيتم تجربة الطريقة البديلة (gradle wrapper)...";
    USE_FALLBACK=1
  }
elif command -v wget >/dev/null 2>&1; then
  wget -O "$TARGET" "$URL" || {
    echo "تعذر التحميل من GitHub، سيتم تجربة الطريقة البديلة (gradle wrapper)...";
    USE_FALLBACK=1
  }
else
  echo "لا يوجد curl أو wget على هذا الجهاز."
  USE_FALLBACK=1
fi

if [ "${USE_FALLBACK:-0}" = "1" ]; then
  if command -v gradle >/dev/null 2>&1; then
    gradle wrapper --gradle-version "$GRADLE_VERSION"
  else
    echo "فشل التحميل التلقائي. الرجاء تحميل الملف يدويًا من:"
    echo "  $URL"
    echo "وحفظه في: $TARGET"
    exit 1
  fi
fi

chmod +x gradlew 2>/dev/null || true

echo ""
echo "تم بنجاح ✅  الملف موجود الآن في: $TARGET"
echo "يمكنك الآن تشغيل: ./gradlew assembleDebug"
