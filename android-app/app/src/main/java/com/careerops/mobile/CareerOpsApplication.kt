package com.careerops.mobile

import android.app.Application
import com.careerops.mobile.diagnostics.CrashLogWriter

class CareerOpsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashLogWriter.installUncaughtExceptionHandler(this)
    }
}
