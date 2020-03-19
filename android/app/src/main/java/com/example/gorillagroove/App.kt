package com.example.gorillagroove

import android.app.Application
import com.example.gorillagroove.db.GroovinDB

// This is the entry point of the application, as defined in the AndroidManifest.xml
@Suppress("unused")
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        GroovinDB.initialize(this.applicationContext)
    }

    companion object {
        const val VERSION = "1.2.1"
    }
}
