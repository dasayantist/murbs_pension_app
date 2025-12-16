package com.appkings.murbs

import android.app.Notification
import android.app.NotificationManager
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import com.google.android.gms.ads.AdView
import kotlin.reflect.KProperty

object SmartWebView {
    init {
        // smart webview constructor here
    }

    // Configuration flags
    var ASWP_JSCRIPT = true
    var ASWP_FUPLOAD = true
    var ASWP_CAMUPLOAD = true
    var ASWP_ONLYCAM = false
    var ASWP_MULFILE = true
    var ASWP_LOCATION = true
    var ASWP_COPYPASTE = false
    var ASWP_RATINGS = true
    var ASWP_PULLFRESH = true
    var ASWP_PBAR = true
    var ASWP_ZOOM = false
    var ASWP_SFORM = false
    var ASWP_OFFLINE = false
    var ASWP_EXTURL = true
    var ASWP_TAB = true
    var ASWP_ADMOB = true
    var ASWP_EXITDIAL = true
    var ASWP_CERT_VERI = false

    // Orientation and layout
    var ASWV_ORIENTATION = 0
    var ASWV_LAYOUT = 0

    // URLs
    var ASWV_URL_ONLINE = "https://member.masenorbs.or.ke/?android=true"
    var ASWV_URL_OFFLINE = "file:///android_asset/offline.html"
    val ASWV_URL: String
        get() = if (ASWP_OFFLINE || ASWV_URL_ONLINE.isEmpty()) ASWV_URL_OFFLINE else ASWV_URL_ONLINE

    var ASWV_SEARCH = "https://www.google.com/search?q="
    val ASWV_SHARE_URL: String
        get() = "$ASWV_URL?share="
    var ASWV_EXC_LIST = "github.com"

    // User agent settings
    var POSTFIX_USER_AGENT = true
    var OVERRIDE_USER_AGENT = false
    var USER_AGENT_POSTFIX = "SWVAndroid"
    var CUSTOM_USER_AGENT = "Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Mobile Safari/537.36"

    var ASWV_F_TYPE = "*/*"
    var ASWV_ADMOB = ""

    // Rating settings
    var ASWR_DAYS = 3
    var ASWR_TIMES = 10
    var ASWR_INTERVAL = 2

    val TAG = MainActivity::class.java.simpleName
    val ASWV_HOST: String
        get() = Functions.aswmHost(ASWV_URL)

    // FCM and notifications
    var aswFcmChannel = "1"
    var CURR_URL = ASWV_URL
    var fcmToken: String? = null
    var aswPcamMessage: String? = null
    var aswVcamMessage: String? = null
    val ASWV_FCM_ID: Int
        get() = Functions.aswmFcmId()

    var aswErrorCounter = 0
    var aswFileReq = 1
    var locPerm = 1
    var filePerm = 2
    val trueOnline: Boolean
        get() = !ASWP_OFFLINE

    // Views and managers
    lateinit var aswView: WebView
    lateinit var printView: WebView
    lateinit var aswAdView: AdView
    lateinit var cookieManager: CookieManager
    lateinit var aswProgress: ProgressBar
    lateinit var aswLoadingText: TextView
    lateinit var aswNotification: NotificationManager
    lateinit var aswNotificationNew: Notification

    var aswFileMessage: ValueCallback<Uri>? = null
    var aswFilePath: ValueCallback<Array<Uri>>? = null

    // Reflection-based getter and setter
    @Throws(NoSuchFieldException::class, IllegalAccessException::class)
    fun swvGet(fieldName: String): Any {
        val field = this::class.java.getDeclaredField(fieldName)
        field.isAccessible = true
        return field.get(this)
    }

    fun swvSet(fieldName: String, value: Any): Boolean {
        return try {
            val field = this::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(this, value)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Optional: Kotlin delegate for property access
    operator fun getValue(thisRef: Any?, property: KProperty<*>): Any? {
        return try {
            swvGet(property.name)
        } catch (e: Exception) {
            null
        }
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: Any?) {
        if (value != null) {
            swvSet(property.name, value)
        }
    }
}