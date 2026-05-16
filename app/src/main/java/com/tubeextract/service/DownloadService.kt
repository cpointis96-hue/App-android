package com.tubeextract.service

import android.app.*
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tubeextract.MainActivity
import com.tubeextract.R

class DownloadService : Service() {

    companion object {
        const val CHANNEL_ID = "tubeextract_download"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification("Téléchargement en cours...", 0)
        startForeground(NOTIFICATION_ID, notification)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    fun updateProgress(message: String, progress: Int) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(message, progress))
    }

    private fun buildNotification(message: String, progress: Int): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TubeExtract")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pi)
            .setProgress(100, progress, progress == 0)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "Téléchargements", NotificationManager.IMPORTANCE_LOW)
        channel.description = "Notifications de téléchargement TubeExtract"
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
