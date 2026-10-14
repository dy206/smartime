package com.example.worktimer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

object Notifier {
    private const val CH = "timer"
    fun sync(c: Context, s: AppState) {
        val nm = c.getSystemService(NotificationManager::class.java)
        val p = s.projects.firstOrNull { it.id == s.runningId }
        if (p == null) { nm.cancel(1); return }
        nm.createNotificationChannel(NotificationChannel(CH, "טיימר", NotificationManager.IMPORTANCE_LOW))
        val fl = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), fl)
        val stop = PendingIntent.getBroadcast(c, 1, Intent(c, StopReceiver::class.java), fl)
        val n = Notification.Builder(c, CH)
            .setSmallIcon(R.drawable.ic_timer).setContentTitle(p.name).setContentText("הטיימר רץ")
            .setUsesChronometer(true).setWhen(s.runningStart).setOngoing(true)
            .setContentIntent(open).addAction(R.drawable.ic_timer, "עצור", stop).build()
        nm.notify(1, n)
    }
}

class StopReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) { Store.stop() }
}

class TimerTile : TileService() {
    override fun onStartListening() { refresh() }
    override fun onClick() {
        if (Store.state.value.runningId != null) Store.stop() else Store.resumeLast()
        refresh()
    }
    private fun refresh() {
        qsTile?.apply {
            state = if (Store.state.value.runningId != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = "טיימר"; updateTile()
        }
    }
}
