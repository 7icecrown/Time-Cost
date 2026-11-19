package com.timecost.demo.data.notify

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.timecost.demo.MainActivity

object Notifications {
    const val CHANNEL = "trial_end"

    fun ensureChannel(c: Context) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CHANNEL, "체험·할인 종료", NotificationManager.IMPORTANCE_DEFAULT))
    }

    /** 알림 권한이 꺼져 있어도 앱은 정상 동작합니다 (PRD 10장). */
    fun enabled(c: Context) = NotificationManagerCompat.from(c).areNotificationsEnabled()

    fun endText(daysLeft: Int) = if (daysLeft == 0) "오늘 끝나요. 유지할지 정해 주세요." else "${daysLeft}일 뒤 끝나요. 유지할지 정해 주세요."

    @SuppressLint("MissingPermission")
    fun show(c: Context, notificationId: Int, title: String, text: String) {
        if (!enabled(c)) return
        val open = PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val n = NotificationCompat.Builder(c, CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title).setContentText(text).setContentIntent(open).setAutoCancel(true).build()
        NotificationManagerCompat.from(c).notify(notificationId, n)
    }
}

class TrialAlertWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val name = inputData.getString("name") ?: return Result.success()
        val days = inputData.getInt("daysLeft", 0)
        val subId = inputData.getLong("subId", 0)
        Notifications.ensureChannel(applicationContext)
        Notifications.show(applicationContext, (subId * 10 + days).toInt(), "$name 사용 기간", Notifications.endText(days))
        return Result.success()
    }
}
