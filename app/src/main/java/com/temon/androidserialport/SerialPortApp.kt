package com.temon.androidserialport

import android.app.Application
import com.temon.androidserialport.ScreenAdaptationUtil

class SerialPortApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ScreenAdaptationUtil.init(this, 360f)
    }
}
