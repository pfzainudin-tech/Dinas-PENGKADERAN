package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.model.AppNotificationItem
import com.example.model.ScheduleItem
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ScheduleNotificationHelper(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_desc)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun processScheduleUpdates(
        schedules: List<ScheduleItem>,
        currentNotifications: List<AppNotificationItem>
    ): List<AppNotificationItem> {
        val seenMap = prefs.all
        val isFirstBaseline = !prefs.getBoolean(KEY_BASELINE_INITIALIZED, false)
        val editor = prefs.edit()
        val newItems = mutableListOf<AppNotificationItem>()
        val today = LocalDate.now()

        if (isFirstBaseline) {
            editor.putBoolean(KEY_BASELINE_INITIALIZED, true)
            schedules.forEach { schedule ->
                val stamp = schedule.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
                editor.putLong("sched_${schedule.id}", stamp)

                if (!schedule.completed) {
                    val start = runCatching { LocalDate.parse(schedule.startDate) }.getOrNull()
                    if (start != null) {
                        val daysUntil = ChronoUnit.DAYS.between(today, start)
                        if (daysUntil in 0..3) {
                            val reminderKey = "rem_${schedule.id}_${today}"
                            if (!prefs.getBoolean(reminderKey, false)) {
                                editor.putBoolean(reminderKey, true)
                                val dayLabel = when (daysUntil) {
                                    0L -> "Hari ini"
                                    1L -> "Besok"
                                    else -> "$daysUntil hari lagi"
                                }
                                val msg = "${schedule.title} (${schedule.executorName}) • $dayLabel di ${schedule.location}"
                                val item = AppNotificationItem(
                                    id = reminderKey,
                                    scheduleId = schedule.id,
                                    title = "Pengingat Jadwal Pengkaderan",
                                    message = msg,
                                    timestampMillis = System.currentTimeMillis(),
                                    unread = true,
                                    category = "PENGINGAT"
                                )
                                newItems.add(item)
                                postSystemNotification(
                                    notifId = schedule.id.hashCode(),
                                    title = item.title,
                                    body = msg
                                )
                            }
                        }
                    }
                }
            }
            editor.apply()
            return (newItems + currentNotifications)
                .distinctBy { it.id }
                .sortedByDescending { it.timestampMillis }
        }

        schedules.forEach { schedule ->
            val key = "sched_${schedule.id}"
            val updatedMillis = schedule.updatedAt?.toDate()?.time ?: 0L
            val previousMillis = (seenMap[key] as? Long) ?: -1L

            if (previousMillis == -1L) {
                editor.putLong(key, updatedMillis)
                val title = "Jadwal Pengkaderan Baru"
                val body = "${schedule.parsedProgramType.label}: ${schedule.title} (${schedule.startDate}) di ${schedule.location}"
                val item = AppNotificationItem(
                    id = "new_${schedule.id}_$updatedMillis",
                    scheduleId = schedule.id,
                    title = title,
                    message = body,
                    timestampMillis = System.currentTimeMillis(),
                    unread = true,
                    category = "BARU"
                )
                newItems.add(item)
                postSystemNotification(schedule.id.hashCode(), title, body)
            } else if (updatedMillis > previousMillis + 1500L) {
                editor.putLong(key, updatedMillis)
                val title = if (schedule.completed) {
                    "Jadwal Pengkaderan Selesai"
                } else {
                    "Perubahan Jadwal / Deskripsi"
                }
                val body = if (schedule.completed) {
                    "${schedule.title} telah ditandai selesai dan masuk Rekapitulasi Pengkaderan."
                } else {
                    "${schedule.title} diperbarui oleh Admin (${schedule.startDate})."
                }
                val item = AppNotificationItem(
                    id = "upd_${schedule.id}_$updatedMillis",
                    scheduleId = schedule.id,
                    title = title,
                    message = body,
                    timestampMillis = System.currentTimeMillis(),
                    unread = true,
                    category = if (schedule.completed) "SELESAI" else "UPDATE"
                )
                newItems.add(item)
                postSystemNotification(schedule.id.hashCode() + 17, title, body)
            }
        }
        editor.apply()

        return (newItems + currentNotifications)
            .distinctBy { it.id }
            .sortedByDescending { it.timestampMillis }
    }

    private fun postSystemNotification(notifId: Int, title: String, body: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notifId, notification)
        }
    }

    companion object {
        private const val PREFS_NAME = "dinas_kader_notif_prefs"
        private const val KEY_BASELINE_INITIALIZED = "baseline_initialized"
        private const val CHANNEL_ID = "jadwal_pengkaderan_channel"
    }
}
