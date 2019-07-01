package com.veliz99.eta.service

import android.app.IntentService
import android.content.Intent
import android.os.IBinder

class MyService : IntentService("ETA") {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onHandleIntent(p0: Intent?) {

    }
}
