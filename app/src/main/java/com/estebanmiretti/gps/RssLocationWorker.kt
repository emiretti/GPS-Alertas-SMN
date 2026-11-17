package com.estebanmiretti.gps

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.estebanmiretti.gps.polygon.Point
import com.estebanmiretti.gps.polygon.Polygon
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

class RssLocationWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private var wvTodos = StringBuilder()
    private val notificationId = 15
    private val context = appContext
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    override suspend fun doWork(): Result {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                readXmlFeedSincrono()
                Result.success()
            } catch (e: Exception) {
                Log.e("RssLocationWorker", "Error en Worker: ${e.message}", e)
                mostrarToast("Error SMN: ${e.message}")
                Result.failure()
            }
        }
    }


    private fun readXmlFeedSincrono() {
        val mPref = PreferenceManager.getDefaultSharedPreferences(context)
        val latitud = mPref.getFloat("lati", inputData.getDouble("latitud", 0.0).toFloat()).toDouble()
        val longitud = mPref.getFloat("longi", inputData.getDouble("longitud", 0.0).toFloat()).toDouble()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://ssl.smn.gob.ar/feeds/")
            .client(OkHttpClient())
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()

        val reader = retrofit.create(ApiDataInterface::class.java)

        // .execute() obliga al Worker a esperar hasta que el SMN responda
        val response = reader.getData().execute()

        if (!response.isSuccessful) return
        val rssMessage = response.body() ?: return
        val items = rssMessage.getChannel()?.getItem() ?: return

        val editor = mPref.edit()
        val arrayAvisos = ArrayList<String>()
        val strArrDeptos = mPref.getString("deptos", null)
        val arrayDeptos = if (!strArrDeptos.isNullOrBlank()) {
            strArrDeptos.replace("[", "").replace("]", "").replace("\"", "").split(",")
        } else {
            emptyList()
        }

        var alarmas = ""
        val gpss = StringBuilder()
        wvTodos.clear()

        for (aviso in items) {
            if (aviso == null) continue

            val polygonStr = aviso.getPolygon()
            gpss.append(polygonStr).append("  ")

            val descripcion = aviso.getDescription() ?: continue
            arrayAvisos.add(descripcion.replace("\n", "").replace("\t", "").replace("\r", ""))

            for (depto in arrayDeptos) {
                if (depto.isNotBlank() && descripcion.contains(depto.trim(), ignoreCase = true)) {
                    alarmas = if (alarmas.isEmpty()) depto.trim() else "$alarmas, ${depto.trim()}"
                    if (wvTodos.isNotEmpty()) wvTodos.append("<br><hr>")
                    wvTodos.append(descripcion)
                    break
                }
            }

            if (!polygonStr.isNullOrBlank()) {
                val arrayLatLng = polygonStr.trim().split(" ")
                val polyBuilder = Polygon.Builder()
                var i = 0
                while (i + 1 < arrayLatLng.size) {
                    val lat = arrayLatLng[i].toDoubleOrNull()
                    val lng = arrayLatLng[i + 1].toDoubleOrNull()
                    if (lat != null && lng != null) {
                        polyBuilder.addVertex(Point(lat, lng))
                    }
                    i += 2
                }

                val polygon = polyBuilder.build()
                if (polygon.contains(Point(latitud, longitud))) {
                    alarmas = if (alarmas.isEmpty()) "Locación Actual" else "$alarmas, Locación Actual"
                    if (wvTodos.isNotEmpty()) wvTodos.append("<br><hr>")
                    wvTodos.append(descripcion)
                }
            }
        }

        editor.putString("gpss", gpss.toString())

        if (alarmas.isNotEmpty()) {
            notificationManager.notify(notificationId, createNotification("Alertas: $alarmas"))
            Handler(Looper.getMainLooper()).post {
                GestorSonido.reproducirAlerta(context)
            }
        }

        if (arrayAvisos.isNotEmpty()) {
            editor.putString("alertas", arrayAvisos.joinToString("$"))
            mostrarToast("Actualizando: ${arrayAvisos.size} avisos")
        }

        editor.apply()
    }

    private fun readXmlFeed() {
        val mPref = PreferenceManager.getDefaultSharedPreferences(context)

        // Leemos la última ubicación real guardada por el servicio (o inputData como respaldo inicial)
        val latitud = mPref.getFloat("lati", inputData.getDouble("latitud", 0.0).toFloat()).toDouble()
        val longitud = mPref.getFloat("longi", inputData.getDouble("longitud", 0.0).toFloat()).toDouble()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://smn.gob.ar")
            .client(OkHttpClient())
            .addConverterFactory(SimpleXmlConverterFactory.create())
            .build()

        val reader = retrofit.create(ApiDataInterface::class.java)
        reader.getData().enqueue(object : Callback<Rss> {
            override fun onResponse(call: Call<Rss>, response: Response<Rss>) {
                if (!response.isSuccessful) return
                val rssMessage = response.body() ?: return
                val items = rssMessage.getChannel()?.getItem() ?: return

                val editor = mPref.edit()
                val arrayAvisos = ArrayList<String>()
                val strArrDeptos = mPref.getString("deptos", null)
                val arrayDeptos = if (!strArrDeptos.isNullOrBlank()) {
                    strArrDeptos.replace("[", "").replace("]", "").replace("\"", "").split(",")
                } else {
                    emptyList()
                }

                var alarmas = ""
                val gpss = StringBuilder()
                wvTodos.clear()

                for (aviso in items) {
                    if (aviso == null) continue

                    val polygonStr = aviso.getPolygon()
                    gpss.append(polygonStr).append("  ")

                    val descripcion = aviso.getDescription() ?: continue
                    arrayAvisos.add(descripcion.replace("\n", "").replace("\t", "").replace("\r", ""))

                    // 1. Verificar coincidencia por nombre de departamento guardado
                    for (depto in arrayDeptos) {
                        if (depto.isNotBlank() && descripcion.contains(depto.trim(), ignoreCase = true)) {
                            alarmas = if (alarmas.isEmpty()) depto.trim() else "$alarmas, ${depto.trim()}"
                            if (wvTodos.isNotEmpty()) wvTodos.append("<br><hr>")
                            wvTodos.append(descripcion)
                            break
                        }
                    }

                    // 2. Verificar intersección matemática (Point-in-Polygon) con GPS actual
                    if (!polygonStr.isNullOrBlank()) {
                        val arrayLatLng = polygonStr.trim().split(" ")
                        val polyBuilder = Polygon.Builder()
                        var i = 0
                        while (i + 1 < arrayLatLng.size) {
                            val lat = arrayLatLng[i].toDoubleOrNull()
                            val lng = arrayLatLng[i + 1].toDoubleOrNull()
                            if (lat != null && lng != null) {
                                polyBuilder.addVertex(Point(lat, lng))
                            }
                            i += 2
                        }

                        val polygon = polyBuilder.build()
                        if (polygon.contains(Point(latitud, longitud))) {
                            alarmas = if (alarmas.isEmpty()) "Locación Actual" else "$alarmas, Locación Actual"
                            if (wvTodos.isNotEmpty()) wvTodos.append("<br><hr>")
                            wvTodos.append(descripcion)
                        }
                    }
                }

                ////editor.putString("gpss", gpss.toString())
                val gpssLimpio = gpss.toString().replace("null", "").trim()
                if (gpssLimpio.isNotEmpty()) {
                    editor.putString("gpss", gpssLimpio)
                } else {
                    editor.remove("gpss")
                }

                // Disparar notificación y alerta sonora tormenta.mp3 si hay coincidencias
                if (alarmas.isNotEmpty()) {
                    notificationManager.notify(notificationId, createNotification("Alertas: $alarmas"))
                    Handler(Looper.getMainLooper()).post {
                        GestorSonido.reproducirAlerta(context)
                    }
                }

                if (arrayAvisos.isNotEmpty()) {
                    editor.putString("alertas", arrayAvisos.joinToString("$"))
                    mostrarToast("Actualizando: ${arrayAvisos.size} avisos SMN")
                } else {
                    editor.remove("alertas")
                }

                editor.apply()
            }

            override fun onFailure(call: Call<Rss>, t: Throwable) {
                Log.e("RssLocationWorker", "Error obteniendo feed SMN: ${t.message}")
               //// mostrarToast("Error actualizando: ${t.message}")
            }
        })
    }

    private fun mostrarToast(mensaje: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
    }

    private fun createNotification(textoAlerta: String): Notification {
        val intent = Intent(context, Alarma::class.java).apply {
            putExtra("wv", wvTodos.toString())
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val channelId = context.getString(R.string.notification_channel_id)
        val channelName = context.getString(R.string.channel_name)
        createNotificationChannel(channelId, channelName)

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Aviso de Corto Plazo")
            .setContentText(textoAlerta)
            .setSmallIcon(android.R.drawable.alert_dark_frame)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel(channelId: String, name: String): NotificationChannel {
        return NotificationChannel(
            channelId, name, NotificationManager.IMPORTANCE_HIGH
        ).also { channel ->
            notificationManager.createNotificationChannel(channel)
        }
    }
}
