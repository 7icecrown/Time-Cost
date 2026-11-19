package com.timecost.demo.data.demo

import android.content.Context
import com.timecost.demo.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 시연 설정. DB와 시계를 앱 시작 때 동기로 정해야 해서 SharedPreferences를 씁니다
 * (PRD 8장은 DataStore를 적었지만, 시작 시점 동기 읽기가 필요한 이 세 값만 예외로 둡니다).
 */
@Singleton
class DemoPrefs @Inject constructor(@ApplicationContext c: Context) {
    private val sp = c.getSharedPreferences("demo", Context.MODE_PRIVATE)
    /** demo 빌드의 기본값은 켜짐. 끄면 실사용 DB와 실제 날짜를 씁니다. */
    var demoMode: Boolean
        get() = sp.getBoolean("demoMode", BuildConfig.DEMO)
        set(v) { sp.edit().putBoolean("demoMode", v).commit() }
    var offsetDays: Int
        get() = sp.getInt("offsetDays", 0)
        set(v) { sp.edit().putInt("offsetDays", v).apply() }
    var scenario: String
        get() = sp.getString("scenario", "A") ?: "A"
        set(v) { sp.edit().putString("scenario", v).apply() }
}

/** 앱이 지금 '데모 DB'로 동작 중인지, 그리고 상태 재현 스위치(F12-4). 재현 스위치는 저장하지 않습니다. */
@Singleton
class DemoFlags @Inject constructor(prefs: DemoPrefs) {
    val active: Boolean = BuildConfig.DEMO && prefs.demoMode
    val failUsage = MutableStateFlow(false)       // 사용 시간 조회 실패
    val noPermission = MutableStateFlow(false)    // 사용 정보 접근 없음
}
