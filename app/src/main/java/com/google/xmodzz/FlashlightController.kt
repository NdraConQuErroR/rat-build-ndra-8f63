package com.google.xmodzz

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

object FlashlightController {
    var m: CameraManager? = null
    var id: String? = null
    private var blinkHandler: Handler? = null
    private var isBlinking = false

    private fun init(ctx: Context) {
        if (m == null) {
            m = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            id = m?.cameraIdList?.firstOrNull()
        }
    }

    fun set(ctx: Context, on: Boolean) {
        if (!on) {
            isBlinking = false
            blinkHandler?.removeCallbacksAndMessages(null)
        }
        
        init(ctx)
        try {
            m?.setTorchMode(id ?: return, on)
        } catch (e: Exception) {}
    }
    
    fun blink(ctx: Context, ms: Int) {
        if (isBlinking) return
        
        isBlinking = true
        blinkHandler = Handler(Looper.getMainLooper())
        init(ctx)
        
        var state = false
        
        val runnable = object : Runnable {
            override fun run() {
                if (!isBlinking) return
                
                state = !state
                try {
                    m?.setTorchMode(id ?: return, state)
                } catch (e: Exception) {}
                
                blinkHandler?.postDelayed(this, ms.toLong())
            }
        }
        
        blinkHandler?.post(runnable)
    }
    
    fun stopBlink(ctx: Context) {
        isBlinking = false
        blinkHandler?.removeCallbacksAndMessages(null)
        
        init(ctx)
        try {
            m?.setTorchMode(id ?: return, false)
        } catch (e: Exception) {}
    }
}
