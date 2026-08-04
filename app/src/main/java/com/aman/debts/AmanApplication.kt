package com.aman.debts

import android.app.Application

/**
 * نقطة انطلاق التطبيق. لا يوجد إعداد خاص مطلوب حالياً؛
 * تُركت هذه الفئة موجودة عمداً لتسهيل إضافة تهيئة عامة
 * لاحقاً (مثل Firebase عند ربطه فعلياً) دون تعديل البيان (Manifest).
 */
class AmanApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
