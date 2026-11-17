package com.estebanmiretti.gps

import android.app.Activity
import android.app.ActivityManager
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolygonOptions
import java.text.SimpleDateFormat
import java.util.Date


class MainActivity : AppCompatActivity(), /*GoogleMap.OnMapLongClickListener, */OnMapReadyCallback {
    private var mMap: GoogleMap? = null

    private var localidad: String? = null

    //private var provincia: String? = null
    private var direccion: String? = null
    private var departamento: String? = null
    var latitud: Double = -30.33
    var longitud: Double = -61.33
    var mBound: Boolean = false
    var mService: GoogleService? = null
    private val REQUEST_PERMISSIONS = 100
    var boolean_permission = false
    var tv_address: TextView? = null
    private lateinit var adapter: ArrayAdapter<String>
    private var array1: Array<String>? = arrayOf("")
    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "alertas" || key == "gpss") {
            runOnUiThread {
                cargarDatos()
            }
        }
    }



    private fun isMyServiceRunning(serviceClass: Class<*>): Boolean {
        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (serviceClass.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onStart() {
        super.onStart()
        tv_address!!.text = "chequeando..."
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fn_permission()
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        tv_address = findViewById<View>(R.id.tv_address) as TextView
        iniciarWorkerAlertas()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.setOnMapLongClickListener(object : GoogleMap.OnMapLongClickListener {
            override fun onMapLongClick(latlng: LatLng) {
                val location = LatLng(latlng.latitude, latlng.longitude)
                val departm = mService?.devuelveDepartamento(latlng.latitude, latlng.longitude)
                mMap?.addMarker(MarkerOptions().position(location).title(departm.toString()))
            }
        })
    }


    fun doBindService() {
        val intento = Intent(applicationContext, GoogleService::class.java)

        if (!mBound && !isMyServiceRunning(GoogleService::class.java)) {
            Toast.makeText(
                applicationContext, "Iniciando Avisos de Corto Plazo SMN", Toast.LENGTH_SHORT
            ).show()

            mService?.startService(intento)
            bindService(intento, mConnection, BIND_AUTO_CREATE)
        }
        //Service is active
        else if (!mBound/* && isMyServiceRunning(GoogleService::class.java)*/) {
            bindService(intento, mConnection, BIND_AUTO_CREATE)
        } else {
        }
    }

    private fun fn_permission() {
        // Solicitar permiso de notificaciones en Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
        if (ContextCompat.checkSelfPermission(
                applicationContext, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this@MainActivity, android.Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
            } else {
                ActivityCompat.requestPermissions(
                    this@MainActivity, arrayOf(
                        android.Manifest.permission.ACCESS_FINE_LOCATION
                    ), REQUEST_PERMISSIONS
                )
            }
        } else {
            boolean_permission = true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String?>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSIONS -> {
                if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    boolean_permission = true
                } else {
                    Toast.makeText(
                        applicationContext, "Por favor, habilite el permiso", Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val mPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)

            latitud = intent.getDoubleExtra("latutide", mPref.getFloat("lati", 0.0F).toDouble())
            longitud = intent.getDoubleExtra("longitude", mPref.getFloat("longi", 0.0F).toDouble())
            localidad = intent.getStringExtra("localidad")
            departamento = intent.getStringExtra("departamento")
            val s: SimpleDateFormat = SimpleDateFormat("EEE dd/MM/yy hh:mm:ss")
            val format: String = s.format(Date())

            if (!intent.getStringExtra("direccion").isNullOrBlank())
                direccion = format + ". " + intent.getStringExtra("direccion")
            else direccion = mPref.getString("dire", "")
            cargarDatos()

            tv_address!!.text = direccion + ", departamento " + departamento

        }
    }

    fun addPolygono(arg: ArrayList<LatLng>): PolygonOptions {
        val polygonOptions = PolygonOptions().strokeColor(Color.RED).strokeWidth(1f)
            .fillColor(Color.parseColor("#ea9999"))//.geodesic(true)//.fillColor(Color.parseColor("#51000000"))

        return polygonOptions.addAll(arg)
    }

    override fun onStop() {
        super.onStop()

    }

    private fun cargarDatos() {
        val mPref = PreferenceManager.getDefaultSharedPreferences(applicationContext)

        mMap?.clear()

        mMap?.addMarker(
            MarkerOptions().position(
                LatLng(
                    mPref.getFloat("lati", latitud.toFloat()).toDouble(),
                    mPref.getFloat("longi", longitud.toFloat()).toDouble()
                )
            ).title(localidad)
        )
        val arrayText: String? = mPref.getString("alertas", null)
        val arrayGpss: String? = mPref.getString(
            "gpss", null
        )//"-34.66 -59.62 -34.44 -59.69 -34.01 -58.50 -34.43 -58.25  -33.09 -65.30 -32.80 -65.27 -32.61 -64.29 -33.04 -64.31"//-32.53 -61.59 -32.10 -60.32 -32.82 -59.94 -32.88 -61.27%%-59.4506837 -32.4913363 -62.2521974 -32.5005975 -61.9116212 -31.0820505 -59.5166017 -31.0350195"
//
        var arrayLatLng: Array<String> = emptyArray()
        if (!arrayGpss.isNullOrBlank()) {
            arrayLatLng = arrayGpss.trim().split("\\s\\s".toRegex()).toTypedArray()
            for (i in 0 until arrayLatLng.size) {
                var j = 0
                // var options = PolygonOptions().strokeColor(Color.RED).strokeWidth(1f)
                // .fillColor(Color.RED)//.geodesic(true)
                val arrayL: Array<String> = arrayLatLng[i].split("\\s".toRegex()).toTypedArray()
                if (!arrayL[0].equals("null")) {
                    var options: ArrayList<LatLng> = ArrayList()
                    while (j < arrayL.size) {
                        val poin =
                            LatLng((arrayL.get(j).toDouble()), (arrayL.get(j + 1).toDouble()))
                        options.add(poin)
                        j = j + 2
                    }
                    mMap?.addPolygon(addPolygono(options))
                }

            }
        }


        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(-30.33, -61.33), 4.6f))

        val myListView = findViewById<ListView>(R.id.ListView01)
        myListView.layoutDirection



        myListView.onItemClickListener = AdapterView.OnItemClickListener { parent, _, position, _ ->
            val selectedFromList = parent.getItemAtPosition(position) as String
            //getAlerta("$selectedFromList")
        }
        if (!arrayText.isNullOrEmpty()) {
            array1 = arrayText.split("$").toTypedArray()
        } else {
            array1 = emptyArray()
        }
        myListView.adapter = null

        adapter = object : ArrayAdapter<String>(this, R.layout.new_list_view, array1!!) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val row: View
                row = convertView ?: layoutInflater.inflate(R.layout.new_list_view, null)
                val tv =
                    row.findViewById<View>(android.R.id.text1) as WebView//findViewById<WebView>(R.id.webview) as WebView
                tv.loadData(
                    "<html><head><style>body{margin-left: auto; margin-top: 20px; margin-right: auto; max-width: 85%;}img{display: inline; height: auto;display: block; margin-left: auto; margin-right: auto; max-width: 100%; }</style>\n<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"></head>\n<body>" + getItem(
                        position
                    ) + "</body></html>", "text/html", "UTF-8"
                )
                return row
            }
        }
        myListView.adapter = this.adapter
        adapter.notifyDataSetChanged()

    }


    override fun onResume() {
        super.onResume()
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
            .registerOnSharedPreferenceChangeListener(prefListener)
        doBindService()
        registerReceiver(broadcastReceiver, IntentFilter(GoogleService.str_receiver))
        mService?.fn_update()
        cargarDatos()
    }

    override fun onPause() {
        PreferenceManager.getDefaultSharedPreferences(applicationContext)
            .unregisterOnSharedPreferenceChangeListener(prefListener)
        unregisterReceiver(broadcastReceiver)
        if (mBound) {
            unbindService(mConnection)
            mBound = false
        }
        super.onPause()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Log.e("GPS", "adio")
        //unbind();
        mService?.stopSelf()
        finish()
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private val mConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            className: ComponentName, service: IBinder
        ) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            val binder: GoogleService.LocalBinder = service as GoogleService.LocalBinder
            mService = binder.getService()
            mBound = true
            cargarDatos()
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            mBound = false
            //Toast.makeText(this@MainActivity, "Servicio desconectado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSaveInstanceState(savedInstanceState: Bundle) {
        super.onSaveInstanceState(savedInstanceState)
        savedInstanceState.putBoolean("ServiceState", mBound)
        savedInstanceState.putDouble("latitud", latitud)
        savedInstanceState.putDouble("longitud", longitud)
        savedInstanceState.putCharSequence("direccion", tv_address!!.text)

    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        mBound = savedInstanceState.getBoolean("ServiceState")
        latitud = savedInstanceState.getDouble("latitud")
        longitud = savedInstanceState.getDouble("longitud")
        tv_address!!.text = savedInstanceState.getCharSequence("direccion")
        //array1 = savedInstanceState.getStringArray("alertas")

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val i = Intent(this, NuevoDepartamento::class.java)

        when (id) {
            R.id.AgregarDepartamento -> {
                launchSomeActivity.launch(i)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    var launchSomeActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                Toast.makeText(
                    applicationContext,
                    "Alerta agregada: " + data?.getStringExtra("variable").toString(),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    /*override fun onMapClick(p0: LatLng) {
        TODO("Not yet implemented")
    }*/
    private fun iniciarWorkerAlertas() {
        val locManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val lastNet = locManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            val lastGps = locManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            val mejorLoc = lastGps ?: lastNet
            if (mejorLoc != null) {
                latitud = mejorLoc.latitude
                longitud = mejorLoc.longitude
            }
        }

        val inputData = androidx.work.Data.Builder()
            .putDouble("latitud", latitud)
            .putDouble("longitud", longitud)
            .putString("direccion", direccion ?: "")
            .build()

        val workManager = androidx.work.WorkManager.getInstance(applicationContext)

        // 1. Tarea periódica cada 15 minutos (para segundo plano)
        val periodicWork = androidx.work.PeriodicWorkRequestBuilder<RssLocationWorker>(
            15, java.util.concurrent.TimeUnit.MINUTES
        ).setInputData(inputData).build()

        workManager.enqueueUniquePeriodicWork(
            "LectorAlertasGeoRSS",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            periodicWork
        )

        // 2. Tarea inmediata prioritaria (para actualizar apenas abres la app)
        val immediateWork = androidx.work.OneTimeWorkRequestBuilder<RssLocationWorker>()
            .setInputData(inputData)
            .setExpedited(androidx.work.OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()

        workManager.enqueueUniqueWork(
            "LectorAlertasInmediato",
            androidx.work.ExistingWorkPolicy.REPLACE,
            immediateWork
        )
    }


}