package com.example.shrinkpdf.utils

import android.util.Log
import com.example.shrinkpdf.BuildConfig

object AppLogger {
    private const val TAG = "PDFchemy"

    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.e(TAG, message, throwable)
        }
    }
}
