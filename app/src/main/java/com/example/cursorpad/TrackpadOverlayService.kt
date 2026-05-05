package com.example.cursorpad

import android.app.Service
import android.content.Intent
import android.os.IBinder

class TrackpadOverlayService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
}