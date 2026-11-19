package com.timecost.demo.data.notify

import android.content.Context
import androidx.work.*
import com.timecost.demo.BuildConfig
import com.timecost.demo.core.AppClock
import com.timecost.demo.data.db.AppDatabase
import com.timecost.demo.data.db.toDomain
import com.timecost.demo.data.settings.SettingsStore
import com.timecost.demo.domain.usecase.AlertPlanner
import com.timecost.demo.domain.usecase.PlannedAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 종료 알림 예약. WorkManager는 몇 분 오차가 날 수 있어 정확 알람 권한은 쓰지 않습니다.
 * (PRD 14장 열린 질문 5: 화면 문구는 '오전 9시경'처럼 쓰는 안으로 정함)
 */
@Singleton
class AlertScheduler @Inject constructor(
    @ApplicationContext private val context: Context, private val db: AppDatabase,
    private val settings: SettingsStore, private val clock: AppClock,
) {
    /** 예약 대상 목록 (데모의 '예약된 알림' 가상 목록에도 사용: F12-5) */
    suspend fun planned(): List<PlannedAlert> =
        AlertPlanner.plan(db.subscriptions().alertTrials().map { it.toDomain() }, clock.today())

    suspend fun reschedule() {
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWorkByTag(TAG)
        // 데모는 기준일이 가상이라 실제로 예약하지 않고, 시연 도구가 가상 목록과 '테스트 알림'을 보여줍니다.
        if (clock.demo) return
        val time = settings.notifyTime.first()
        val now = LocalDateTime.now()
        planned().forEach { a ->
            val at = a.date.atTime(time)
            if (at.isAfter(now)) {
                wm.enqueueUniqueWork("alert-${a.subscriptionId}-${a.date}", ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<TrialAlertWorker>().setInitialDelay(Duration.between(now, at)).addTag(TAG)
                        .setInputData(workDataOf("name" to a.name, "daysLeft" to a.daysLeft, "subId" to a.subscriptionId)).build())
            }
        }
    }

    companion object { const val TAG = "trial-alert" }
}
