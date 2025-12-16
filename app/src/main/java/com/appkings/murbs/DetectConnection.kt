package com.appkings.murbs

import android.content.Context
import android.net.ConnectivityManager

internal object DetectConnection {
    private val TAG: String = DetectConnection::class.java.getSimpleName()
    fun isInternetAvailable(context: Context?): Boolean {
        // Handle null context
        if (context == null) {
            return false
        }

        // Use safe call operator ?. and handle potential null
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val info = connectivityManager?.activeNetworkInfo

        return info?.isConnected == true
    }
}