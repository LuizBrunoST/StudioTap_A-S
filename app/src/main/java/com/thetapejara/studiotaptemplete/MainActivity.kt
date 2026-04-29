package com.thetapejara.studiotaptemplete

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    lateinit var web: WebView




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)



        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    2
                )
            }
        }

        web = findViewById(R.id.web)

        val ws = web.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true
        ws.allowFileAccess = true
        ws.setSupportZoom(false)
        ws.builtInZoomControls = false
        ws.displayZoomControls = false

        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()


        web.addJavascriptInterface(TapBridge(this), "TapNative")

        web.loadUrl("file:///android_asset/index.html")
    }

    fun handleBackAction(title: String, message: String, checkHistory: Boolean) {
        runOnUiThread {
            if (checkHistory && web.canGoBack()) {
                web.goBack()
            } else {
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Sim") { _, _ ->
                        finish()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    override fun onBackPressed() {
        web.evaluateJavascript("tap.triggerBack()", null)
    }
}