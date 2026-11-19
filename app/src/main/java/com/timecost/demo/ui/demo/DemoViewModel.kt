package com.timecost.demo.ui.demo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timecost.demo.core.AppClock
import com.timecost.demo.core.Format
import com.timecost.demo.core.timeLabel
import com.timecost.demo.core.demoBanner
import com.timecost.demo.data.demo.DemoController
import com.timecost.demo.data.demo.DemoFlags
import com.timecost.demo.data.demo.DemoPrefs
import com.timecost.demo.data.demo.SCENARIOS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DemoUi(
    val banner: String? = null, val active: Boolean = true, val scenarioLabel: String = "", val dateLabel: String = "",
    val offsetLabel: String = "+0일", val failUsage: Boolean = false, val noPermission: Boolean = false,
    val planned: List<String> = emptyList(),
)

@HiltViewModel
class DemoViewModel @Inject constructor(
    private val ctl: DemoController, private val clock: AppClock, private val flags: DemoFlags, private val prefs: DemoPrefs,
) : ViewModel() {
    private val planned = MutableStateFlow<List<String>>(emptyList())
    private val scenario = MutableStateFlow(prefs.scenario)
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    fun messageShown() { _message.value = null }

    val ui: StateFlow<DemoUi> = combine(clock.offsetFlow, flags.failUsage, flags.noPermission, planned, scenario) { _, fail, noPerm, pl, sc ->
        val d = clock.today(); val s = SCENARIOS.firstOrNull { it.id == sc }
        DemoUi(clock.demoBanner(), flags.active, s?.let { "현재 · ${it.id}. ${it.title}" } ?: "현재 · 없음",
            "${d.year}년 ${d.monthValue}월 ${d.dayOfMonth}일", "+${clock.offsetDays}일", fail, noPerm, pl)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, DemoUi())

    init { refreshPlanned() }

    /** F12-5 가상 예약 알림 목록 */
    private fun refreshPlanned() = viewModelScope.launch {
        val t = ctl.notifyTime()
        planned.value = ctl.planned().map { a ->
            "${a.name} · ${Format.monthDay(a.date)} ${Format.timeLabel(t)} · " + if (a.daysLeft == 0) "종료 당일" else "종료 D-${a.daysLeft}"
        }
    }

    fun move(n: Int) = viewModelScope.launch { ctl.moveDays(n); refreshPlanned() }
    fun resetDate() = viewModelScope.launch { ctl.resetDate(); refreshPlanned() }
    fun loadScenario(id: String) = viewModelScope.launch {
        ctl.loadScenario(id); scenario.value = id; refreshPlanned()
        _message.value = "시나리오 $id 를 불러왔어요"
    }
    fun resetDb() = viewModelScope.launch { ctl.loadScenario(prefs.scenario); refreshPlanned(); _message.value = "시연 데이터를 처음 상태로 되돌렸어요" }
    fun setFail(on: Boolean) = ctl.setFailUsage(on)
    fun setNoPermission(on: Boolean) = ctl.setNoPermission(on)
    fun sendTest() { _message.value = if (ctl.sendTestNotification()) "테스트 알림을 보냈어요" else "알림이 꺼져 있어요. 설정에서 켜 주세요" }
    fun switchDemoMode(on: Boolean) = ctl.switchDemoMode(on)
}
