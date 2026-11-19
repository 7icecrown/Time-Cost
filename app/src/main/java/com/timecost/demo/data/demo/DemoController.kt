package com.timecost.demo.data.demo

import android.content.Context
import android.content.Intent
import android.os.Process
import com.timecost.demo.core.AppClock
import com.timecost.demo.data.notify.AlertScheduler
import com.timecost.demo.data.notify.Notifications
import com.timecost.demo.data.repo.HomeEvents
import com.timecost.demo.data.repo.SubscriptionRepository
import com.timecost.demo.data.settings.SettingsStore
import com.timecost.demo.domain.usecase.PlannedAlert
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class Scenario(val id: String, val title: String, val steps: String)

/** PRD 11장 시연 시나리오. A는 시연 도구 시안에 있고 B~F는 제안안입니다. */
val SCENARIOS = listOf(
    Scenario("A", "구독 현황 확인", "홈 → 통계 → 계산기에서 Notion 99,000원 ÷ 40시간 = 2,475원 확인"),
    Scenario("B", "체험 유지", "Disney+ 결정 → 유지하기 → 확인 → 홈에서 하이라이트 확인"),
    Scenario("C", "체험 해지", "기준일 +3일로 Apple Music 종료 → 결정 → 해지하기 → 해지 완료로 표시 → 휴지통에서 확인"),
    Scenario("D", "구독 추가", "+ 구독 추가 → 티빙 선택 → 구독료 입력 → 저장"),
    Scenario("E", "시간 수정", "YouTube Premium 상세 → 지난 결제 기간 펼치기 → 11월 8일 → +30분 추가"),
    Scenario("F", "기준일 이동", "+7일로 D-n과 결정 대기 항목이 바뀌는 것을 보고, 테스트 알림 보내기"),
)

/** 시연 도구 동작 (F12). 데모 DB에서만 데이터를 건드립니다. */
@Singleton
class DemoController @Inject constructor(
    @ApplicationContext private val context: Context, private val repo: SubscriptionRepository, private val clock: AppClock,
    private val prefs: DemoPrefs, private val flags: DemoFlags, private val scheduler: AlertScheduler,
    private val settings: SettingsStore, private val events: HomeEvents,
) {
    val active: Boolean get() = flags.active
    val scenarioId: String get() = prefs.scenario

    /** F12-3 기준일 이동: 앞으로 옮기면 그 사이 날들의 가상 사용 기록도 만들어 통계가 날짜를 따라 바뀌게 함 */
    suspend fun moveDays(n: Int) {
        if (!active || n <= 0) return
        val old = clock.today()
        clock.offsetDays += n; prefs.offsetDays = clock.offsetDays
        repo.simulateUsage(old.plusDays(1), clock.today())
        repo.purgeExpired(clock.today())
        scheduler.reschedule()
    }

    suspend fun resetDate() {
        if (!active) return
        clock.offsetDays = 0; prefs.offsetDays = 0
        scheduler.reschedule()
    }

    /** F12-2 시나리오 불러오기 = 데모 DB를 처음 상태로 되돌리고 날짜·재현 스위치도 초기화 */
    suspend fun loadScenario(id: String) {
        if (!active) return
        repo.resetToSeed()
        clock.offsetDays = 0; prefs.offsetDays = 0
        flags.failUsage.value = false; flags.noPermission.value = false
        events.highlightId.value = null; events.message.value = null
        prefs.scenario = id
        scheduler.reschedule()
    }

    fun setFailUsage(on: Boolean) { flags.failUsage.value = on }
    fun setNoPermission(on: Boolean) { flags.noPermission.value = on }

    /** F12-5 예약 알림 목록(가상)과 테스트 알림 */
    suspend fun planned(): List<PlannedAlert> = scheduler.planned()
    suspend fun notifyTime() = settings.notifyTime.first()

    /** 알림이 꺼져 있으면 false */
    fun sendTestNotification(): Boolean {
        Notifications.ensureChannel(context)
        if (!Notifications.enabled(context)) return false
        Notifications.show(context, 99_001, "테스트 알림", "시연용 알림이에요. 종료 알림은 이렇게 보여요.")
        return true
    }

    /** F12-1 데모 모드 전환. 저장 후 앱을 다시 시작합니다 (DB·시계를 앱 시작 때 정하므로). */
    fun switchDemoMode(on: Boolean) {
        prefs.demoMode = on
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(launch)
        Process.killProcess(Process.myPid())
    }
}
