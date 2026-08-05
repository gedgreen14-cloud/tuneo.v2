package com.tuneo.app

import android.app.Application
import coil.Coil
import coil.ImageLoader
import coil.decode.VideoFrameDecoder

/**
 * Configure un ImageLoader Coil global capable de générer des vignettes
 * à partir des fichiers vidéo locaux (nécessaire pour l'onglet Vidéos).
 */
class TuneoApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val imageLoader = ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()

        Coil.setImageLoader(imageLoader)
    }
}
