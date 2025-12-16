package com.appkings.murbs

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Activity
import android.app.ActivityManager
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.drawerlayout.widget.DrawerLayout
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.gms.ads.MobileAds
import com.google.android.material.navigation.NavigationView
import com.google.firebase.FirebaseApp
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {
    private lateinit var actResultLauncher: ActivityResultLauncher<Intent>
    private val fns = Functions()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }

    @SuppressLint("SetJavaScriptEnabled", "WrongViewCast", "JavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        actResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = resources.getColor(R.color.colorPrimary)
            var results: Array<Uri>? = null

            if (result.resultCode == Activity.RESULT_CANCELED) {
                SmartWebView.aswFilePath?.onReceiveValue(null)
                return@registerForActivityResult
            } else if (result.resultCode == Activity.RESULT_OK) {
                if (SmartWebView.aswFilePath == null) {
                    return@registerForActivityResult
                }

                var clipData: ClipData? = null
                var stringData: String? = null
                try {
                    clipData = result.data?.clipData
                    stringData = result.data?.dataString
                } catch (e: Exception) {
                    clipData = null
                    stringData = null
                }

                if (clipData == null && stringData == null && (SmartWebView.aswPcamMessage != null || SmartWebView.aswVcamMessage != null)) {
                    val message = SmartWebView.aswPcamMessage ?: SmartWebView.aswVcamMessage
                    results = arrayOf(Uri.parse(message))
                } else {
                    if (clipData != null) {
                        val numSelectedFiles = clipData.itemCount
                        results = Array(numSelectedFiles) { i ->
                            clipData.getItemAt(i).uri
                        }
                    } else {
                        try {
                            val camPhoto = result.data?.extras?.get("data") as? Bitmap
                            camPhoto?.let {
                                val bytes = ByteArrayOutputStream()
                                it.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
                                stringData = MediaStore.Images.Media.insertImage(contentResolver, it, null, null)
                            }
                        } catch (ignored: Exception) {
                        }
                        results = arrayOf(Uri.parse(stringData))
                    }
                }
            }
            SmartWebView.aswFilePath?.onReceiveValue(results)
            SmartWebView.aswFilePath = null


        }

        // setting port view
        val cookieOrientation = if (!SmartWebView.ASWP_OFFLINE) fns.getCookies("ORIENT") else ""
        fns.setOrientation(
            if (!cookieOrientation.isNullOrEmpty()) cookieOrientation.toInt() else SmartWebView.ASWV_ORIENTATION,
            false,
            applicationContext
        )

        // use Service Worker
        if (Build.VERSION.SDK_INT >= 24) {
            val swController = ServiceWorkerController.getInstance()
            swController.setServiceWorkerClient(object : ServiceWorkerClient() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    return null
                }
            })
        }

        // prevent app from being started again when it is still alive in the background
        if (!isTaskRoot) {
            finish()
            return
        }

        if (SmartWebView.ASWV_LAYOUT == 1) {
            setContentView(R.layout.drawer_main)
            findViewById<View>(R.id.app_bar).visibility = View.VISIBLE

            val toolbar = findViewById<Toolbar>(R.id.toolbar)
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayShowTitleEnabled(false)

            val drawer = findViewById<DrawerLayout>(R.id.drawer_layout)
            val toggle = ActionBarDrawerToggle(this, drawer, toolbar, R.string.open, R.string.close)
            drawer.addDrawerListener(toggle)
            toggle.syncState()

            val navigationView = findViewById<NavigationView>(R.id.nav_view)
            navigationView.setNavigationItemSelectedListener(this as NavigationView.OnNavigationItemSelectedListener)
        } else {
            setContentView(R.layout.activity_main)
        }

        SmartWebView.aswView = findViewById(R.id.msw_view)
        SmartWebView.printView = findViewById<WebView>(R.id.print_view)
        //fns.fcmToken()

        // notification manager
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= 26) {
            val notificationChannel = NotificationChannel(
                SmartWebView.aswFcmChannel,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.description = getString(R.string.notification_channel_desc)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.setShowBadge(true)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        // swipe refresh
        val pullfresh = findViewById<SwipeRefreshLayout>(R.id.pullfresh)
        if (SmartWebView.ASWP_PULLFRESH) {
            pullfresh.setOnRefreshListener {
                fns.pullFresh(applicationContext)
                pullfresh.isRefreshing = false
            }
            SmartWebView.aswView.viewTreeObserver.addOnScrollChangedListener {
                pullfresh.isEnabled = SmartWebView.aswView.scrollY == 0
            }
        } else {
            pullfresh.isRefreshing = false
            pullfresh.isEnabled = false
        }

        if (SmartWebView.ASWP_PBAR) {
            SmartWebView.aswProgress = findViewById(R.id.msw_progress)
        } else {
            findViewById<View>(R.id.msw_progress).visibility = View.GONE
        }
        SmartWebView.aswLoadingText = findViewById(R.id.msw_loading_text)
        val handler = Handler()

        // Launching app rating request
        if (SmartWebView.ASWP_RATINGS) {
            val ratingRunnable = fns.getRating(applicationContext)
            if (ratingRunnable != null) {
                handler.postDelayed(ratingRunnable, 1000 * 60) // running request after few moments
            }
        }

        // Webview settings; defaults are customized for best performance
        val webSettings = SmartWebView.aswView.settings

        // setting custom user agent
        if (SmartWebView.OVERRIDE_USER_AGENT || SmartWebView.POSTFIX_USER_AGENT) {
            var userAgent = webSettings.userAgentString
            if (SmartWebView.OVERRIDE_USER_AGENT) {
                userAgent = SmartWebView.CUSTOM_USER_AGENT
            }
            if (SmartWebView.POSTFIX_USER_AGENT) {
                userAgent = "$userAgent ${SmartWebView.USER_AGENT_POSTFIX}"
            }
            webSettings.userAgentString = userAgent
        }

        if (!SmartWebView.ASWP_OFFLINE) {
            webSettings.javaScriptEnabled = SmartWebView.ASWP_JSCRIPT
        }
        webSettings.saveFormData = SmartWebView.ASWP_SFORM
        webSettings.setSupportZoom(SmartWebView.ASWP_ZOOM)
        webSettings.setGeolocationEnabled(SmartWebView.ASWP_LOCATION)
        webSettings.allowFileAccess = true
        webSettings.allowFileAccessFromFileURLs = true
        webSettings.allowUniversalAccessFromFileURLs = true
        webSettings.useWideViewPort = true
        webSettings.domStorageEnabled = true

        if (!SmartWebView.ASWP_COPYPASTE) {
            SmartWebView.aswView.setOnLongClickListener { true }
        }
        SmartWebView.aswView.isHapticFeedbackEnabled = false

        // download listener
        SmartWebView.aswView.setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            if (!fns.checkPermission(2, applicationContext)) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    SmartWebView.filePerm
                )
            } else {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("cookie", fns.getCookies(""))
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription(getString(R.string.dl_downloading))
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
                request.allowScanningByMediaScanner()
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(url, contentDisposition, mimeType)
                )
                val dm = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                Toast.makeText(applicationContext, getString(R.string.dl_downloading2), Toast.LENGTH_LONG).show()
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = resources.getColor(R.color.colorPrimaryDark)
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        SmartWebView.aswView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        SmartWebView.aswView.isVerticalScrollBarEnabled = false
        SmartWebView.aswView.webViewClient = Callback()

        // Reading incoming intents
        val readInt = intent
        Log.d("SLOG_INTENT", readInt.toUri(0))
        val uri = readInt.getStringExtra("uri")
        val share = readInt.getStringExtra("s_uri")
        val shareImg = readInt.getStringExtra("s_img")

        when {
            share != null -> {
                // Processing shared content
                Log.d("SLOG_SHARE_INTENT", share)
                val matcher = Functions.urlPattern().matcher(share)
                var urlStr = ""
                if (matcher.find()) {
                    urlStr = matcher.group()
                    if (urlStr.startsWith("(") && urlStr.endsWith(")")) {
                        urlStr = urlStr.substring(1, urlStr.length - 1)
                    }
                }
                val redUrl = "${SmartWebView.ASWV_SHARE_URL}?text=$share&link=$urlStr&image_url="
                fns.aswmView(redUrl, false, SmartWebView.aswErrorCounter, applicationContext)
            }
            shareImg != null -> {
                // Processing shared content
                Log.d("SLOG_SHARE_INTENT", shareImg)
                Toast.makeText(this, shareImg, Toast.LENGTH_LONG).show()
                fns.aswmView(SmartWebView.ASWV_URL, false, SmartWebView.aswErrorCounter, applicationContext)
            }
            uri != null -> {
                // Opening notification
                Log.d("SLOG_NOTIFI_INTENT", uri)
                fns.aswmView(uri, false, SmartWebView.aswErrorCounter, applicationContext)
            }
            else -> {
                // Rendering the default URL
                Log.d("SLOG_MAIN_INTENT", SmartWebView.ASWV_URL)
                fns.aswmView(SmartWebView.ASWV_URL, false, SmartWebView.aswErrorCounter, applicationContext)
            }
        }

        if (SmartWebView.ASWP_ADMOB) {
            MobileAds.initialize(this) { }
            SmartWebView.aswAdView = findViewById(R.id.msw_ad_view)
        }

        SmartWebView.aswView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                if (ContextCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    if (SmartWebView.ASWP_FUPLOAD) {
                        SmartWebView.aswFilePath = filePathCallback
                        var takePictureIntent: Intent? = null
                        var takeVideoIntent: Intent? = null

                        if (SmartWebView.ASWP_CAMUPLOAD) {
                            var includeVideo = false
                            var includePhoto = false

                            // Check the accept parameter to determine which intent(s) to include.
                            paramCheck@ for (acceptTypes in fileChooserParams.acceptTypes) {
                                // Although it's an array, it still seems to be the whole value.
                                // Split it out into chunks so that we can detect multiple values.
                                val splitTypes = acceptTypes.split(", ?+".toRegex())
                                for (acceptType in splitTypes) {
                                    when (acceptType) {
                                        "*/*" -> {
                                            includePhoto = true
                                            includeVideo = true
                                            break@paramCheck
                                        }
                                        "image/*" -> includePhoto = true
                                        "video/*" -> includeVideo = true
                                    }
                                }
                            }

                            // If no `accept` parameter was specified, allow both photo and video.
                            if (fileChooserParams.acceptTypes.isEmpty()) {
                                includePhoto = true
                                includeVideo = true
                            }

                            if (includePhoto) {
                                takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                                if (takePictureIntent.resolveActivity(applicationContext.packageManager) != null) {
                                    var photoFile: File? = null
                                    try {
                                        photoFile = fns.createImage(applicationContext)
                                        takePictureIntent.putExtra("PhotoPath", SmartWebView.aswPcamMessage)
                                    } catch (ex: IOException) {
                                        Log.e("SLOG_ERROR", "Image file creation failed", ex)
                                    }
                                    photoFile?.let {
                                        SmartWebView.aswPcamMessage = "file:${it.absolutePath}"
                                        takePictureIntent.putExtra(
                                            MediaStore.EXTRA_OUTPUT,
                                            FileProvider.getUriForFile(
                                                applicationContext,
                                                "${packageName}.provider",
                                                it
                                            )
                                        )
                                    } ?: run {
                                        takePictureIntent = null
                                    }
                                }
                            }

                            if (includeVideo) {
                                takeVideoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                                if (takeVideoIntent.resolveActivity(applicationContext.packageManager) != null) {
                                    var videoFile: File? = null
                                    try {
                                        videoFile = fns.createVideo(applicationContext)
                                    } catch (ex: IOException) {
                                        Log.e("SLOG_ERROR", "Video file creation failed", ex)
                                    }
                                    videoFile?.let {
                                        SmartWebView.aswVcamMessage = "file:${it.absolutePath}"
                                        takeVideoIntent.putExtra(
                                            MediaStore.EXTRA_OUTPUT,
                                            FileProvider.getUriForFile(
                                                applicationContext,
                                                "${packageName}.provider",
                                                it
                                            )
                                        )
                                    } ?: run {
                                        takeVideoIntent = null
                                    }
                                }
                            }
                        }

                        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
                        if (!SmartWebView.ASWP_ONLYCAM) {
                            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
                            contentSelectionIntent.type = SmartWebView.ASWV_F_TYPE
                            if (SmartWebView.ASWP_MULFILE) {
                                contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                            }
                        }

                        val intentArray = mutableListOf<Intent>().apply {
                            takePictureIntent?.let { add(it) }
                            takeVideoIntent?.let { add(it) }
                        }.toTypedArray()

                        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
                        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
                        chooserIntent.putExtra(Intent.EXTRA_TITLE, getString(R.string.fl_chooser))
                        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
                        actResultLauncher.launch(chooserIntent)
                    }
                    return true
                } else {
                    fns.getFilePerm(this@MainActivity)
                    return false
                }
            }

            // getting webview content rendering progress
            override fun onProgressChanged(view: WebView, p: Int) {
                if (SmartWebView.ASWP_PBAR) {
                    SmartWebView.aswProgress?.progress = p
                    if (p == 100) {
                        SmartWebView.aswProgress?.progress = 0
                    }
                }
            }

            // overload the geoLocations permissions prompt to always allow instantly as app permission was granted previously
            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback
            ) {
                if (Build.VERSION.SDK_INT < 23 || fns.checkPermission(1, applicationContext)) {
                    // location permissions were granted previously so auto-approve
                    callback.invoke(origin, true, false)
                } else {
                    // location permissions not granted so request them
                    ActivityCompat.requestPermissions(
                        this@MainActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        SmartWebView.locPerm
                    )
                }
            }
        }

        intent.data?.let { data ->
            val path = data.toString()
            fns.aswmView(path, false, SmartWebView.aswErrorCounter, applicationContext)
        }
    }

    override fun onPause() {
        super.onPause()
        SmartWebView.aswView.onPause()
    }

    override fun onResume() {
        super.onResume()
        SmartWebView.aswView.onResume()
        // Coloring the "recent apps" tab header; doing it onResume, as an insurance
        if (Build.VERSION.SDK_INT >= 23) {
            val bm = BitmapFactory.decodeResource(resources, R.mipmap.ic_launcher)
            val taskDesc = ActivityManager.TaskDescription(
                getString(R.string.app_name),
                bm,
                getColor(R.color.colorPrimary)
            )
            this.setTaskDescription(taskDesc)
        }
        fns.getLocation(applicationContext)
    }

    // Checking if users allowed the requested permissions or not
    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fns.getLocation(applicationContext)
            }
        }
    }

    // Action on back key tap/click
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (SmartWebView.aswView.canGoBack()) {
                    SmartWebView.aswView.goBack()
                } else {
                    if (SmartWebView.ASWP_EXITDIAL) {
                        fns.askExit(applicationContext)
                    } else {
                        finish()
                    }
                }
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        SmartWebView.aswView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        SmartWebView.aswView.restoreState(savedInstanceState)
    }

    private inner class Callback : WebViewClient() {
        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            this@MainActivity.fns.getLocation(applicationContext)
        }

        override fun onPageFinished(view: WebView, url: String) {
            findViewById<View>(R.id.msw_welcome).visibility = View.GONE
            findViewById<View>(R.id.msw_view).visibility = View.VISIBLE
        }

        override fun onReceivedError(
            view: WebView,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            Toast.makeText(applicationContext, getString(R.string.went_wrong), Toast.LENGTH_SHORT).show()
            fns.aswmView("file:///android_asset/error.html", false, SmartWebView.aswErrorCounter, applicationContext)
        }

        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            SmartWebView.CURR_URL = url
            return fns.urlActions(view, url, applicationContext)
        }

        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            SmartWebView.CURR_URL = request.url.toString()
            return fns.urlActions(view, request.url.toString(), applicationContext)
        }

        @SuppressLint("WebViewClientOnReceivedSslError")
        override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
            if (SmartWebView.ASWP_CERT_VERI) {
                super.onReceivedSslError(view, handler, error)
            } else {
                // to ignore SSL certificate errors; can cause security issues
                handler.proceed()
            }
        }
    }
}