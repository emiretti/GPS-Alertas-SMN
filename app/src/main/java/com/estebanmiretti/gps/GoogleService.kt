package com.estebanmiretti.gps

import android.Manifest
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.Locale

class GoogleService : Service(), LocationListener {
    private val maxResultados = 1
    private val minTimeMsGps: Long = 60 * 1000
    private var isGPSEnable = false
    private var isNetworkEnable = false
    private var latitude = 0.0
    private var longitude = 0.0
    private var provincia = ""
    private var departamento = ""
    private var localidad = ""
    private var direccion = ""
    private var locationManager: LocationManager? = null
    private var location: Location? = null
    private val broadcastIntent = Intent(str_receiver)
    private val mBinder: IBinder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): GoogleService = this@GoogleService
    }

    override fun onBind(intent: Intent?): IBinder = mBinder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        fn_getlocation()
        return START_STICKY
    }

    override fun onLocationChanged(p0: Location) {
        location = p0
        latitude = p0.latitude
        longitude = p0.longitude
        fn_update()
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}

    fun fn_getlocation() {
        locationManager = applicationContext.getSystemService(LOCATION_SERVICE) as LocationManager
        isGPSEnable = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true
        isNetworkEnable = locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true

        if (!isGPSEnable && !isNetworkEnable) return

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        location = null

        if (isNetworkEnable) {
            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, minTimeMsGps, 0f, this)
            location = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        }

        if (location == null && isGPSEnable) {
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTimeMsGps, 0f, this)
            location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        }

        if (location != null) {
            latitude = location!!.latitude
            longitude = location!!.longitude
            fn_update()
        }
    }

    /*fun fn_update() {
        val lat = location?.latitude
        val lon = location?.longitude
        if (lat != null && lon != null) {
            devuelveDepartamentoNombre(lat, lon)
        }
        broadcastIntent.apply {
            putExtra("latutide", lat)
            putExtra("longitude", lon)
            putExtra("localidad", localidad)
            putExtra("provincia", provincia)
            putExtra("departamento", departamento)
            putExtra("direccion", direccion)
        }
        sendBroadcast(broadcastIntent)
    }*/

    fun devuelveDepartamento(lat: Double, lng: Double): String {
        val geoCoder = Geocoder(applicationContext, Locale.getDefault())
        var str = ""
        try {
            val addresses = geoCoder.getFromLocation(lat, lng, maxResultados)
            if (!addresses.isNullOrEmpty()) {
                val loc = addresses[0].locality
                val sub = addresses[0].subAdminArea
                if (!loc.isNullOrEmpty()) str = "$loc, "
                if (!sub.isNullOrEmpty()) str += sub
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return str
    }

    fun devuelveDepartamentoNombre(lat: Double, lng: Double) {
        val geoCoder = Geocoder(applicationContext, Locale.getDefault())
        try {
            val addresses = geoCoder.getFromLocation(lat, lng, maxResultados)
            if (!addresses.isNullOrEmpty()) {
                provincia = addresses[0].adminArea ?: ""
                departamento = addresses[0].subAdminArea ?: ""
                localidad = addresses[0].locality ?: ""
                direccion = addresses[0].getAddressLine(0) ?: ""
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        locationManager?.removeUpdates(this)
        super.onDestroy()
    }

    companion object {
        const val str_receiver = "com.estebanmiretti.gps.receiver"
    }

    fun fn_update() {
        val lat = location?.latitude
        val lon = location?.longitude
        if (lat != null && lon != null) {
            devuelveDepartamentoNombre(lat, lon)

            // Guardamos la ubicación fresca para que el Worker la lea cada 15 minutos
            val mPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            mPref.edit().apply {
                putFloat("lati", lat.toFloat())
                putFloat("longi", lon.toFloat())
                putString("dire", direccion)
                apply()
            }
        }
        broadcastIntent.apply {
            putExtra("latutide", lat)
            putExtra("longitude", lon)
            putExtra("localidad", localidad)
            putExtra("provincia", provincia)
            putExtra("departamento", departamento)
            putExtra("direccion", direccion)
        }
        sendBroadcast(broadcastIntent)
    }

}
