package com.tubeextract

import android.app.Application
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TubeExtractApp : Application() {

    override fun onCreate() {
        super.onCreate()
        initYoutubeDL()
    }

    private fun initYoutubeDL() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                YoutubeDL.getInstance().init(applicationContext)
                Log.d("TubeExtract", "yt-dlp initialized successfully")
            } catch (e: Exception) {
                Log.e("TubeExtract", "Failed to initialize yt-dlp", e)
            }
        }
    }
}
