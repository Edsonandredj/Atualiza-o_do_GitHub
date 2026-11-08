package com.edson.ghwatch

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

object Notifier {
    private const val CHANNEL = "updates"

    fun notifyUpdate(c: Context, repo: Repo, release: Release) {
        if (Build.VERSION.SDK_INT >= 33 &&
            c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Atualizações de repositórios", NotificationManager.IMPORTANCE_HIGH)
        )
        val pi = PendingIntent.getActivity(
            c, repo.slug.hashCode(),
            Intent(Intent.ACTION_VIEW, Uri.parse(release.url)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = Notification.Builder(c, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle("Nova versão: ${repo.slug}")
            .setContentText(release.tag)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(repo.slug.hashCode(), n)
    }
}
