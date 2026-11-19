package com.timecost.demo

import android.content.Context
import androidx.room.Room
import com.timecost.demo.core.AppClock
import com.timecost.demo.data.db.AppDatabase
import com.timecost.demo.data.demo.DemoFlags
import com.timecost.demo.data.demo.DemoPrefs
import com.timecost.demo.data.repo.DemoSeed
import com.timecost.demo.data.usage.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /** 데모 DB는 실사용 DB와 파일을 분리합니다 (PRD 8장). 데모 모드를 끄면 실사용 DB를 씁니다. */
    @Provides @Singleton
    fun db(@ApplicationContext c: Context, flags: DemoFlags): AppDatabase =
        Room.databaseBuilder(c, AppDatabase::class.java, if (flags.active) "timecost_demo.db" else "timecost.db").build()

    /** 데모 시드가 2026-11-10 기준이라 데모 DB에서는 기준일을 그날로 고정하고, 저장해 둔 이동 일수를 복원합니다. */
    @Provides @Singleton
    fun clock(flags: DemoFlags, prefs: DemoPrefs): AppClock =
        if (flags.active) AppClock(demo = true) { DemoSeed.BASE_DATE }.also { it.offsetDays = prefs.offsetDays } else AppClock()

    /** 데모 DB와 에뮬레이터는 Fake, 실기기는 Real (PRD 9장) */
    @Provides @Singleton
    fun usageSource(@ApplicationContext c: Context, clock: AppClock, flags: DemoFlags): UsageSource =
        if (flags.active) FakeUsageSource(clock, flags) else RealUsageSource(c)

    @Provides @Singleton
    fun installedApps(@ApplicationContext c: Context, flags: DemoFlags): InstalledAppsSource =
        if (flags.active) FakeInstalledAppsSource() else RealInstalledAppsSource(c)
}
