package com.example

import android.app.Application
import leakcanary.LeakCanary

class CleanerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            // Configurar LeakCanary para que notifique activamente desde el primer objeto retenido
            LeakCanary.config = LeakCanary.config.copy(
                retainedVisibleThreshold = 1,
                dumpHeap = true
            )
        } catch (_: Throwable) {
            // En caso de que se ejecute en entorno sin soporte de Heap Dumper
        }
    }
}
