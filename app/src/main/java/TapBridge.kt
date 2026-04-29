package com.thetapejara.studiotaptemplete

import androidx.appcompat.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Vibrator
import android.webkit.JavascriptInterface
import android.widget.Toast
import android.Manifest
import android.R
import android.annotation.SuppressLint
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.app.Activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

import android.content.pm.ActivityInfo
import android.view.View


import android.provider.MediaStore
import android.content.ContentValues
import android.util.Base64
import java.io.OutputStream


class TapBridge(private val activity: Activity) {
    private val ctx = activity.applicationContext
    //Toast
    @JavascriptInterface
    fun toast(msg: String) {
        Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
    }

    //Vibrar
    @JavascriptInterface
    fun vibrate(ms: Int) {
        val v = ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        v.vibrate(ms.toLong())
    }

    //Clipboard
    @JavascriptInterface
    fun copy(text: String) {
        val clipboard = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("tap", text)
        clipboard.setPrimaryClip(clip)
    }

    @JavascriptInterface
    @SuppressLint("MissingPermission")
    fun getLocation() {

        val lm = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1
            )
            return
        }

        val location: Location? = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if (location != null) {

            val lat = location.latitude
            val lon = location.longitude

            activity.runOnUiThread {
                val js = "window.onTapLocation && onTapLocation($lat, $lon)"
                activity.findViewById<android.webkit.WebView>(android.R.id.content)
                    ?.evaluateJavascript(js, null)
            }

        } else {
            Toast.makeText(activity, "GPS não disponível", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    @JavascriptInterface
    fun notify(title: String, message: String) {

        val channelId = "tap_channel"

        // 🔥 cria canal (Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Tap Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            val manager = activity.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(activity, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            activity,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(activity, channelId)
            .setSmallIcon(R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        NotificationManagerCompat.from(activity)
            .notify(System.currentTimeMillis().toInt(), builder.build())
    }

    @JavascriptInterface
    fun back(title: String, message: String, checkHistory: Boolean) {
        if (activity is MainActivity) {
            activity.handleBackAction(title, message, checkHistory)
        }
    }

    @JavascriptInterface
    fun setOrientation(mode: String) {
        activity.runOnUiThread {
            when (mode.lowercase()) {
                "portrait" -> {
                    activity.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                }

                "landscape" -> {
                    activity.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }

                "auto" -> {
                    activity.requestedOrientation =
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
        }
    }

    @JavascriptInterface
    fun saveImage(base64: String) {

        try {

            val pureBase64 = base64.substringAfter(",")
            val bytes = Base64.decode(pureBase64, Base64.DEFAULT)

            val filename = "tap_${System.currentTimeMillis()}.png"

            val resolver = activity.contentResolver

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
            }

            val imageUri = resolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )

            if (imageUri != null) {
                val stream: OutputStream? = resolver.openOutputStream(imageUri)
                stream?.write(bytes)
                stream?.flush()
                stream?.close()

                Toast.makeText(activity, "Imagem salva em Download", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @JavascriptInterface
    fun printHtml(html: String) {

        activity.runOnUiThread {

            val webView = android.webkit.WebView(activity)

            webView.settings.javaScriptEnabled = true

            webView.loadDataWithBaseURL(
                null,
                html,
                "text/html",
                "UTF-8",
                null
            )

            webView.postDelayed({

                val printManager = activity.getSystemService(Context.PRINT_SERVICE) as android.print.PrintManager

                val adapter = webView.createPrintDocumentAdapter("TapPrint")

                printManager.print(
                    "Tap_Print",
                    adapter,
                    android.print.PrintAttributes.Builder().build()
                )

            }, 500) // tempo pra renderizar
        }
    }

}