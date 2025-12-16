package com.appkings.murbs /*
 * Smart WebView is an Open Source project that integrates native features into webview to help create advanced hybrid applications. Available on GitHub (https://github.com/mgks/Android-SmartWebView).
 * Initially developed by Ghazi Khan (https://github.com/mgks) under MIT Open Source License.
 * This program is free to use for private and commercial purposes under MIT License (https://opensource.org/licenses/MIT).
 * Please mention project source or developer credits in your Application's License(s) Wiki.
 * Contribute to the project (https://github.com/mgks/Android-SmartWebView/discussions)
 * Sponsor the project (https://github.com/sponsors/mgks)
 * Giving right credits to developers encourages them to keep improving their projects :)
 */

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.print.PrintAttributes
import android.print.PrintManager
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.BuildConfig
import com.google.firebase.messaging.FirebaseMessaging
import java.io.File
import java.io.IOException
import java.math.BigInteger
import java.security.SecureRandom
import java.text.SimpleDateFormat
import java.util.Date
import java.util.regex.Pattern

class Functions : NavigationView.OnNavigationItemSelectedListener {
    private val random = SecureRandom()

    /* --- internal functions --- */ // random ID creation function to help get fresh cache every-time webview reloaded
    fun randomId(): String {
        return BigInteger(130, random).toString(32)
    }

    // opening URLs inside webview with request
    fun aswmView(url: String, tab: Boolean, errorCounter: Int, context: Context) {
        var url = url
        if (errorCounter > 2) {
            exitApp(context)
        } else {
            if (tab) {
                if (SmartWebView.ASWP_TAB) {
                    //val intentBuilder: CustomTabsIntent.Builder = Builder()
                    val intentBuilder = CustomTabsIntent.Builder()
                    intentBuilder.setStartAnimations(
                        context.getApplicationContext(),
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                    )
                    intentBuilder.setExitAnimations(
                        context.getApplicationContext(),
                        android.R.anim.slide_in_left,
                        android.R.anim.slide_out_right
                    )
                    val customTabsIntent: CustomTabsIntent = intentBuilder.build()
                    try {
                        customTabsIntent.launchUrl(context.getApplicationContext(), Uri.parse(url))
                    } catch (e: ActivityNotFoundException) {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setData(Uri.parse(url))
                        context.startActivity(intent)
                    }
                } else {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.setData(Uri.parse(url))
                    context.startActivity(intent)
                }
            } else {
                // check to see whether the url already has query parameters and handle appropriately
                url = url + (if (url.contains("?")) "&" else "?") + "rid=" + randomId()
                SmartWebView.aswView.loadUrl(url)
            }
        }
    }

    /*--- actions based on URL structure ---*/
    fun urlActions(view: WebView, url: String, context: Context): Boolean {
        var a = true
        // show toast error if not connected to the network
        if (!SmartWebView.ASWP_OFFLINE && !DetectConnection.isInternetAvailable(context)) {
            Toast.makeText(
                context,
                context.getString(R.string.check_connection),
                Toast.LENGTH_SHORT
            ).show()

            // use this in a hyperlink to redirect back to default URL :: href="refresh:android"
        } else if (url.startsWith("refresh:")) {
            val refSch = (Uri.parse(url).toString()).replace("refresh:", "")
            if (refSch.matches("URL".toRegex())) {
                SmartWebView.CURR_URL = SmartWebView.ASWV_URL
            }
            pullFresh(context)

            // use this in a hyperlink to launch default phone dialer for specific number :: href="tel:+919876543210"
        } else if (url.startsWith("tel:")) {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse(url))
            context.startActivity(intent)
        } else if (url.startsWith("print:")) {
            printPage(view, view.getTitle()!!, true, context)

            // use this to open your apps page on google play store app :: href="rate:android"
        } else if (url.startsWith("rate:")) {
            val appPackage =
                context.getPackageName() //requesting app package name from Context or Activity object
            try {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + appPackage)
                    )
                )
            } catch (anfe: ActivityNotFoundException) {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + appPackage)
                    )
                )
            }

            // sharing content from your webview to external apps :: href="share:URL" and remember to place the URL you want to share after share:___
        } else if (url.startsWith("share:")) {
            val intent = Intent(Intent.ACTION_SEND)
            intent.setType("text/plain")
            intent.putExtra(Intent.EXTRA_SUBJECT, view.getTitle())
            intent.putExtra(
                Intent.EXTRA_TEXT,
                view.getTitle() + "\nVisit: " + (Uri.parse(url).toString()).replace("share:", "")
            )
            context.startActivity(
                Intent.createChooser(
                    intent,
                    context.getString(R.string.share_w_friends)
                )
            )

            // use this in a hyperlink to exit your app :: href="exit:android"
        } else if (url.startsWith("exit:")) {
            exitApp(context)

            // getting location for offline files
        } else if (url.startsWith("offloc:")) {
            val offloc = SmartWebView.ASWV_URL + "?loc=" + getLocation(context)
            aswmView(offloc, false, SmartWebView.aswErrorCounter, context)
            Log.d("SLOG_OFFLINE_LOC_REQ", offloc)

            // creating firebase notification for offline files
        } else if (url.startsWith("fcm:")) {
            val fcm = SmartWebView.ASWV_URL + "?fcm=" + fcmToken()
            aswmView(fcm, false, SmartWebView.aswErrorCounter, context)
            Log.d("SLOG_OFFLINE_FCM_TOKEN", fcm)

            // opening external URLs in android default web browser
        } else if (SmartWebView.ASWP_EXTURL && (aswmHost(url) != SmartWebView.ASWV_HOST) && !SmartWebView.ASWV_EXC_LIST.contains(
                aswmHost(url)
            )
        ) {
            aswmView(url, true, SmartWebView.aswErrorCounter, context)

            // set the device orientation on request
        } else if (url.startsWith("orient:")) {
            setOrientation(5, true, context)

            // else return false for no special action
        } else {
            a = false
        }
        return a
    }

    // reloading current page
    fun pullFresh(context: Context) {
        aswmView(
            (if (SmartWebView.CURR_URL != "") SmartWebView.CURR_URL else SmartWebView.ASWV_URL),
            false,
            SmartWebView.aswErrorCounter,
            context
        )
    }

    // changing port view
    @SuppressLint("SourceLockedOrientationActivity")
    fun setOrientation(
        orientation: Int,
        cookie: Boolean,
        context: Context?
    ) { // setting the view port var
        if (context is Activity) {
            val activity = context
            if (orientation == 1) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            } else if (orientation == 2) {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            } else if (orientation == 5) { //experimental switch
                SmartWebView.ASWV_ORIENTATION = (if (SmartWebView.ASWV_ORIENTATION == 1) 2 else 1)
            } else {
                activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            }
            if (cookie) {
                setCookie("ORIENT=" + orientation)
            }
        }
    }

    // setting cookies
    fun setCookie(data: String?) {
        //boolean log = true;
        if (SmartWebView.trueOnline) {
            // cookie manager initialisation
            SmartWebView.cookieManager = CookieManager.getInstance()
            SmartWebView.cookieManager.setAcceptCookie(true)
            SmartWebView.cookieManager.setCookie(SmartWebView.ASWV_URL, data)
            Log.d("SLOG_COOKIES", SmartWebView.cookieManager.getCookie(SmartWebView.ASWV_URL))
        }
    }

    //Getting device basic information
    fun getInfo() {
        setCookie("DEVICE=android")
        val dv: DeviceDetails = DeviceDetails()
        setCookie("DEVICE_INFO=" + dv.pull())
        setCookie("DEV_API=" + Build.VERSION.SDK_INT)
        setCookie("APP_ID=" + com.google.firebase.BuildConfig.LIBRARY_PACKAGE_NAME)
        setCookie("APP_VER=" + com.google.firebase.BuildConfig.BUILD_TYPE + "/" + BuildConfig.VERSION_NAME)
    }

    // checking permission for storage and camera for writing and uploading images
    fun getFilePerm(activity: Activity) {
        val perms = arrayOf<String?>(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )

        //Checking for storage permission to write images for upload
        if (SmartWebView.ASWP_FUPLOAD && SmartWebView.ASWP_CAMUPLOAD && !checkPermission(
                2,
                activity.applicationContext
            ) && !checkPermission(3, activity.applicationContext)
        ) {
            ActivityCompat.requestPermissions(activity, perms, SmartWebView.filePerm)

            //Checking for WRITE_EXTERNAL_STORAGE permission
        } else if (SmartWebView.ASWP_FUPLOAD && !checkPermission(
                2,
                activity.getApplicationContext()
            )
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf<String>(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ),
                SmartWebView.filePerm
            )

            //Checking for CAMERA permissions
        } else if (SmartWebView.ASWP_CAMUPLOAD && !checkPermission(
                3,
                activity.getApplicationContext()
            )
        ) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf<String>(Manifest.permission.CAMERA),
                SmartWebView.filePerm
            )
        }
    }

    // using cookies to update user locations
    fun getLocation(context: Context): String {
        var newLoc = "0,0"
        //Checking for location permissions
        if (SmartWebView.ASWP_LOCATION && (Build.VERSION.SDK_INT < 23 || checkPermission(
                1,
                context
            ))
        ) {
            val gps = GPSTrack(context)
            val latitude: Double = gps.getLatitudeValue()
            val longitude: Double = gps.getLongitudeValue()
            if (gps.canGetLocation) {
                if (latitude != 0.0 || longitude != 0.0) {
                    if (SmartWebView.trueOnline) {
                        setCookie("lat=" + latitude)
                        setCookie("long=" + longitude)
                        setCookie("LATLANG=" + latitude + "x" + longitude)
                    }
                    //Log.d("SLOG_NEW_LOCATION", latitude + "," + longitude);  //enable to test dummy latitude and longitude
                    newLoc = latitude.toString() + "," + longitude
                } else {
                    Log.d("SLOG_UPDATED_LOCATION", "NULL")
                }
            } else {
                showNotification(1, 1, context)
                Log.d("SLOG_UPDATED_LOCATION", "FAIL")
            }
        }
        return newLoc
    }

    // get cookie value
    fun getCookies(cookie: String): String? {
        var value: String? = ""
        if (SmartWebView.trueOnline) {
            SmartWebView.cookieManager = CookieManager.getInstance()
            val cookies: String? = SmartWebView.cookieManager.getCookie(SmartWebView.ASWV_URL)
            if (cookies != null && !cookies.isEmpty()) {
                val temp =
                    cookies.split(";".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                for (ar1 in temp) {
                    if (ar1.contains(cookie)) {
                        val temp1 =
                            ar1.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        value = temp1[1]
                        break
                    }
                }
            } else {
                Log.d("SLOG_COOKIES", "Cookies either NULL or Empty")
                value = ""
            }
        } else {
            Log.w("SLOG_NETWORK", "DEVICE NOT ONLINE")
        }
        return value
    }

    @SuppressLint("ResourceAsColor")
    fun onCreateOptionsMenu(menu: Menu, context: Activity): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        context.getMenuInflater().inflate(R.menu.main, menu)
        val searchManager = context.getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchView = menu.findItem(R.id.menu_search).getActionView() as SearchView?
        searchView!!.setSearchableInfo(searchManager.getSearchableInfo(context.getComponentName()))
        searchView.setQueryHint(context.getString(R.string.search_hint))
        searchView.setIconified(true)
        searchView.setIconifiedByDefault(true)
        searchView.clearFocus()

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                aswmView(
                    SmartWebView.ASWV_SEARCH + query,
                    false,
                    SmartWebView.aswErrorCounter,
                    context.getApplicationContext()
                )
                searchView.setQuery(query, false)
                return false
            }

            override fun onQueryTextChange(query: String?): Boolean {
                return false
            }
        })
        //searchView.setQuery(SmartWebView.asw_view.getUrl(),false);
        return true
    }

    fun onOptionsItemSelected(item: MenuItem, context: Context): Boolean {
        val id = item.getItemId()
        if (id == R.id.action_exit) {
            exitApp(context)
            return true
        }
        return onOptionsItemSelected(item, context)
    }

    fun onNavigationItemSelected(item: MenuItem, context: Context): Boolean {
        val id = item.getItemId()
        if (id == R.id.nav_home) {
            aswmView(
                "file:///android_asset/offline.html",
                false,
                SmartWebView.aswErrorCounter,
                context
            )
        } else if (id == R.id.nav_doc) {
            aswmView(
                "https://github.com/mgks/Android-SmartWebView/tree/master/documentation",
                false,
                SmartWebView.aswErrorCounter,
                context
            )
        } else if (id == R.id.nav_fcm) {
            aswmView(
                "https://github.com/mgks/Android-SmartWebView/blob/master/documentation/fcm.md",
                false,
                SmartWebView.aswErrorCounter,
                context
            )
        } else if (id == R.id.nav_admob) {
            aswmView(
                "https://github.com/mgks/Android-SmartWebView/blob/master/documentation/admob.md",
                false,
                SmartWebView.aswErrorCounter,
                context
            )
        } else if (id == R.id.nav_gps) {
            aswmView(
                "https://github.com/mgks/Android-SmartWebView/blob/master/documentation/gps.md",
                false,
                SmartWebView.aswErrorCounter,
                context
            )
        } else if (id == R.id.nav_share) {
            aswmView(
                "https://github.com/mgks/Android-SmartWebView/blob/master/documentation/share.md",
                false,
                SmartWebView.aswErrorCounter,
                context
            )
        } else if (id == R.id.nav_lay) {
            aswmView(
                "https://github.com/mgks/Android-SmartWebView/blob/master/documentation/layout.md",
                false,
                SmartWebView.aswErrorCounter,
                context
            )
        } else if (id == R.id.nav_support) {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.setData(Uri.parse("mailto:hello@mgks.dev"))
            intent.putExtra(Intent.EXTRA_SUBJECT, "SWV Help")
            context.startActivity(Intent.createChooser(intent, "Send Email"))
        }

        val drawer = (context as Activity).findViewById<DrawerLayout?>(R.id.drawer_layout)
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    fun fcmToken(): String? {
        val fcmToken = arrayOf<String?>("")
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener({ instanceIdResult ->
            fcmToken[0] = FirebaseMessaging.getInstance().getToken().getResult()
            if (!SmartWebView.ASWP_OFFLINE) {
                setCookie("FCM_TOKEN=" + fcmToken[0])
                Log.d("SLOG_FCM_BAKED", "YES")
                //Log.d("SLOG_COOKIES", cookieManager.getCookie(ASWV_URL));
            }
            Log.d("SLOG_REQ_FCM_TOKEN", fcmToken[0]!!)
        }).addOnFailureListener({ e -> Log.d("SLOG_REQ_FCM_TOKEN", "FAILED") })
        return fcmToken[0]
    }

    //Checking if particular permission is given or not
    fun checkPermission(permission: Int, context: Context): Boolean {
        when (permission) {
            1 -> return ContextCompat.checkSelfPermission(
                context.getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            2 -> return Build.VERSION.SDK_INT >= 30 || ContextCompat.checkSelfPermission(
                context.getApplicationContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED

            3 -> return ContextCompat.checkSelfPermission(
                context.getApplicationContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED

        }
        return false
    }

    //Creating image file for upload
    @Throws(IOException::class)
    fun createImage(context: Context): File {
        @SuppressLint("SimpleDateFormat") val file_name =
            SimpleDateFormat("yyyy_mm_ss").format(Date())
        val newName = "file_" + file_name + "_"
        val sdDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(newName, ".jpg", sdDirectory)
    }

    //Creating video file for upload
    @Throws(IOException::class)
    fun createVideo(context: Context): File {
        @SuppressLint("SimpleDateFormat") val file_name =
            SimpleDateFormat("yyyy_mm_ss").format(Date())
        val newName = "file_" + file_name + "_"
        val sdDirectory = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(newName, ".3gp", sdDirectory)
    }

    //Launching app rating dialog [developed by github.com/hotchemi]
    fun getRating(context: Context?): Runnable? {
        if (DetectConnection.isInternetAvailable(context)) {
            AppRate.with(context!!)!!
                .setStoreType(StoreType.GOOGLEPLAY) //default is Google Play, other option is Amazon App Store
                .setInstallDays(SmartWebView.ASWR_DAYS)
                .setLaunchTimes(SmartWebView.ASWR_TIMES)
                .setRemindInterval()
                .setTitle(R.string.rate_dialog_title)
                .setMessage(R.string.rate_dialog_message)
                .setTextLater(R.string.rate_dialog_cancel)
                .setTextNever(R.string.rate_dialog_no)
                .setTextRateNow(R.string.rate_dialog_ok)
                .monitor()
            AppRate.showRateDialogIfMeetsConditions(context)
        }
        //for more customizations, look for AppRate and DialogManager
        return null
    }

    //Creating custom notifications with IDs
    fun showNotification(type: Int, id: Int, context: Context) {
        val `when` = System.currentTimeMillis()
        var contTitle: String? = ""
        var contText: String? = ""
        var contDesc: String? = ""

        SmartWebView.aswNotification =
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?)!!
        val i = Intent()
        if (type == 1) {
            i.setClass(context, MainActivity::class.java)
        } else if (type == 2) {
            i.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        } else {
            i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            i.addCategory(Intent.CATEGORY_DEFAULT)
            i.setData(Uri.parse("package:" + context.getPackageName()))
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent: PendingIntent?
        val flag =
            if (Build.VERSION.SDK_INT >= 23) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        pendingIntent = PendingIntent.getActivity(context, 0, i, flag)
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, "")
        builder.setTicker(context.getString(R.string.app_name))
        when (type) {
            1 -> {
                contTitle = context.getString(R.string.loc_fail)
                contText = context.getString(R.string.loc_fail_text)
                contDesc = context.getString(R.string.loc_fail_more)
            }

            2 -> {
                contTitle = context.getString(R.string.loc_perm)
                contText = context.getString(R.string.loc_perm_text)
                contDesc = context.getString(R.string.loc_perm_more)
                builder.setSound(alarmSound)
            }
        }
        builder.setContentTitle(contTitle)
        builder.setContentText(contText)
        builder.setStyle(NotificationCompat.BigTextStyle().bigText(contDesc))
        builder.setVibrate(longArrayOf(350, 700, 350, 700, 350))
        builder.setSmallIcon(R.mipmap.ic_launcher)
        builder.setOngoing(false)
        builder.setAutoCancel(true)
        builder.setWhen(`when`)
        builder.setContentIntent(pendingIntent)
        SmartWebView.aswNotificationNew = builder.build()
        SmartWebView.aswNotification.notify(id, SmartWebView.aswNotificationNew)
    }

    //Printing pages
    private fun printPage(view: WebView, printName: String, manual: Boolean, context: Context) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
        val printAdapter = view.createPrintDocumentAdapter(printName)
        val builder = PrintAttributes.Builder()
        builder.setMediaSize(PrintAttributes.MediaSize.ISO_A5)
        val printJob = printManager.print(printName, printAdapter, builder.build())

        if (printJob.isCompleted) {
            Toast.makeText(context, R.string.print_complete, Toast.LENGTH_LONG).show()
        } else if (printJob.isFailed) {
            Toast.makeText(context, R.string.print_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun doWebViewPrint(ss: String?, context: Context) {
        SmartWebView.printView.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }

            //use Service Worker
            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView, url: String?) {
                printPage(view, view.getTitle()!!, false, context)
                super.onPageFinished(view, url)
            }
        })
        // Generate an HTML document on the fly:
        SmartWebView.printView.loadDataWithBaseURL(null, ss.toString(), "text/html", "UTF-8", null)
    }

    fun exitApp(context: Context) {
        val intent = Intent(Intent.ACTION_MAIN)
        intent.addCategory(Intent.CATEGORY_HOME)
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    // Creating exit dialogue
    fun askExit(context: Context) {
        val builder = AlertDialog.Builder(context)

        builder.setTitle(context.getString(R.string.exit_title))
        builder.setMessage(context.getString(R.string.exit_subtitle))
        builder.setCancelable(true)

        // Action if user selects 'yes'
        builder.setPositiveButton(
            "Yes",
            DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int ->
                exitApp(context)
            })

        // Actions if user selects 'no'
        builder.setNegativeButton(
            "No",
            DialogInterface.OnClickListener { dialogInterface: DialogInterface?, i: Int -> })

        // Create the alert dialog using alert dialog builder
        val dialog = builder.create()

        // Finally, display the dialog when user press back button
        dialog.show()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        return false
    }

    companion object {
        //Getting host name
        fun aswmHost(url: String?): String {
            if (url == null || url.isEmpty()) {
                return ""
            }
            var dslash = url.indexOf("//")
            if (dslash == -1) {
                dslash = 0
            } else {
                dslash += 2
            }
            var end = url.indexOf('/', dslash)
            end = if (end >= 0) end else url.length
            val port = url.indexOf(':', dslash)
            end = if (port > 0 && port < end) port else end
            Log.i("SLOG_URL_HOST", url.substring(dslash, end))
            return url.substring(dslash, end)
        }

        fun urlPattern(): Pattern {
            return Pattern.compile(
                "(?:^|\\W)((ht|f)tp(s?)://|www\\.)" + "(([\\w\\-]+\\.)+([\\w\\-.~]+/?)*" + "[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*)",
                Pattern.CASE_INSENSITIVE or Pattern.MULTILINE or Pattern.DOTALL
            )
        }

        fun aswmFcmId(): Int {
            //Date now = new Date();
            //Integer.parseInt(new SimpleDateFormat("ddHHmmss",  Locale.US).format(now));
            return 1
        }
    }
}