package com.estebanmiretti.gps

import android.content.Context
import android.media.MediaPlayer
import android.os.PowerManager

object GestorSonido {
    private var mediaPlayer: MediaPlayer? = null

    fun reproducirAlerta(context: Context) {
        mediaPlayer?.release()

        mediaPlayer = MediaPlayer.create(context, R.raw.tormenta).apply {
            // Permite al MediaPlayer usar el procesador el tiempo suficiente
            // para terminar el efecto corto aunque la pantalla esté apagada
            setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)

            setOnCompletionListener {
                it.release()
                mediaPlayer = null
            }
            start()
        }
    }
}
