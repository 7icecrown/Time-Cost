package com.timecost.demo

import android.app.Application
import com.timecost.demo.core.AppClock
import com.timecost.demo.data.notify.AlertScheduler
import com.timecost.demo.data.notify.Notifications
import com.timecost.demo.data.repo.SubscriptionRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class TimeCostApp : Application() {
    @Inject lateinit var scheduler: AlertScheduler
    @Inject lateinit var repo: SubscriptionRepository
    @Inject lateinit var clock: AppClock

    override fun onCreate() {
        super.onCreate()
        Notifications.ensureChannel(this)
        CoroutineScope(Dispatchers.Default).launch {
            repo.purgeExpired(clock.today())   // F9-5
            scheduler.reschedule()             // 앱을 열 때마다 예약을 맞춰 둠
        }
    }
}
