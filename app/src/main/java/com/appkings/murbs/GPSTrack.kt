package com.appkings.murbs

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat

class GPSTrack(context: Context) : Service(), LocationListener {
    private val mContext: Context = context

    // Use different names for properties vs methods to avoid conflicts
    private var _currentLocation: Location? = null
    private var _latitude: Double = 0.0
    private var _longitude: Double = 0.0

    // Public properties with custom getters
    val currentLocation: Location?
        get() = _currentLocation

    val currentLatitude: Double
        get() = _latitude

    val currentLongitude: Double
        get() = _longitude

    // Status flags
    var isGPSEnabled = false
    var isNetworkEnabled = false
    var canGetLocation = false

    // Configuration
    private val minDistanceChangeForUpdates: Float = 1f // 1 meter as Float
    private val minTimeBwUpdates: Long = 1000 * 5 // 5 seconds
    private var locationManager: LocationManager? = null

    init {
        fetchLocation()
    }

    @SuppressLint("MissingPermission")
    fun fetchLocation(): Location? {
        try {
            locationManager = mContext.getSystemService(LOCATION_SERVICE) as LocationManager

            isGPSEnabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
            isNetworkEnabled = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

            if (!isGPSEnabled && !isNetworkEnabled) {
                Log.w("Location GPS:", "DEAD")
            } else {
                this.canGetLocation = true
                if (isGPSEnabled) {
                    if (_currentLocation == null) {
                        locationManager?.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            minTimeBwUpdates,
                            minDistanceChangeForUpdates,
                            this
                        )
                        Log.d("LOC-TP", "GPS")
                        _currentLocation = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                        if (_currentLocation != null) {
                            _latitude = _currentLocation!!.latitude
                            _longitude = _currentLocation!!.longitude
                        } else {
                            if (isNetworkEnabled) {
                                locationManager?.requestLocationUpdates(
                                    LocationManager.NETWORK_PROVIDER,
                                    minTimeBwUpdates,
                                    minDistanceChangeForUpdates,
                                    this
                                )
                                Log.d("LOC-TP", "Network")
                                _currentLocation = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                if (_currentLocation != null) {
                                    _latitude = _currentLocation!!.latitude
                                    _longitude = _currentLocation!!.longitude
                                }
                                if (ActivityCompat.checkSelfPermission(
                                        this,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                                        this,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    return _currentLocation
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return _currentLocation
    }

    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     */
    fun stopUsingGPS() {
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            locationManager?.removeUpdates(this)
        }
    }

    /**
     * Function to get latitude (if you still need method form)
     */
    fun getLatitudeValue(): Double {
        _currentLocation?.let {
            _latitude = it.latitude
        }
        return _latitude
    }

    /**
     * Function to get longitude (if you still need method form)
     */
    fun getLongitudeValue(): Double {
        _currentLocation?.let {
            _longitude = it.longitude
        }
        return _longitude
    }

    /**
     * Function to show settings alert dialog
     * On pressing Settings button will launch Settings Options
     */
    fun showSettingsAlert() {
        val alertDialog = AlertDialog.Builder(mContext)

        alertDialog.setTitle("GPS is disabled")
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?")

        alertDialog.setPositiveButton("Settings") { dialog, which ->
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            mContext.startActivity(intent)
        }

        alertDialog.setNegativeButton("Cancel") { dialog, which -> dialog.cancel() }

        alertDialog.show()
    }

    override fun onLocationChanged(loc: Location) {
        // Update location when it changes
        _currentLocation = loc
        _latitude = loc.latitude
        _longitude = loc.longitude
    }

    override fun onProviderDisabled(provider: String) {}

    override fun onProviderEnabled(provider: String) {}

    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

    override fun onBind(arg0: Intent): IBinder? {
        return null
    }
}