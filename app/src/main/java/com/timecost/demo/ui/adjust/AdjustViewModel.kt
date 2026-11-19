package com.timecost.demo.ui.adjust

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timecost.demo.core.AppClock
import com.timecost.demo.core.Format
import com.timecost.demo.data.repo.SubscriptionRepository
import com.timecost.demo.domain.model.*
import com.timecost.demo.domain.usecase.BillingCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class AdjustHeader(
    val name: String, val title: String, val autoMinutes: Int?, val isToday: Boolean, val hasExisting: Boolean,
)
data class AdjustForm(val type: AdjustType = AdjustType.ADD, val hours: String = "", val minutes: String = "")
data class AdjustResult(val finalText: String?, val subText: String?, val error: String?, val valid: Boolean)

@HiltViewModel
class AdjustViewModel @Inject constructor(private val repo: SubscriptionRepository, private val clock: AppClock) : ViewModel() {
    private var key: String? = null
    private var subId = 0L
    private var date: LocalDate = clock.today()

    private val _header = MutableStateFlow<AdjustHeader?>(null)
    val header: StateFlow<AdjustHeader?> = _header
    private val _form = MutableStateFlow(AdjustForm())
    val form: StateFlow<AdjustForm> = _form

    val result: StateFlow<AdjustResult> = combine(_header, _form) { h, f -> compute(h, f) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, AdjustResult(null, null, null, false))

    fun load(id: Long, iso: String) {
        subId = id; date = LocalDate.parse(iso)
        viewModelScope.launch {
            val sub = repo.subscription(id) ?: return@launch
            val usage = repo.usageOn(id, date)
            val existing = repo.adjustmentOn(id, date)
            val auto = usage?.autoMinutes
            val isToday = date == clock.today()
            val md = Format.monthDay(date)
            _header.value = AdjustHeader(
                name = sub.name, autoMinutes = auto, isToday = isToday, hasExisting = existing != null,
                title = when {
                    isToday -> "오늘($md) 사용 시간 수정"
                    auto == null && existing == null -> "$md (${Format.weekday(date)}) 사용 시간 입력"
                    else -> "$md (${Format.weekday(date)}) 사용 시간 수정"
                })
            // 기록 없는 날은 '변경'만 가능 (F5-2)
            _form.value = if (existing != null) AdjustForm(existing.type, (existing.minutes / 60).toString().takeIf { existing.minutes >= 60 } ?: "",
                (existing.minutes % 60).toString().takeIf { existing.minutes % 60 > 0 || existing.minutes < 60 } ?: "")
            else AdjustForm(if (auto == null) AdjustType.SET else AdjustType.ADD)
        }
    }

    fun setType(t: AdjustType) = _form.update { it.copy(type = t) }
    fun setHours(v: String) = _form.update { it.copy(hours = v.filter(Char::isDigit).take(2)) }
    fun setMinutes(v: String) = _form.update { it.copy(minutes = v.filter(Char::isDigit).take(2)) }

    private fun input(f: AdjustForm) = (f.hours.toIntOrNull() ?: 0) * 60 + (f.minutes.toIntOrNull() ?: 0).coerceAtMost(59)

    /** F5-1 최종 사용 시간 = 자동 기록 ± 입력값, 바로 보여줌 / F5-4 0분 아래가 되는 감소는 막음 */
    private fun compute(h: AdjustHeader?, f: AdjustForm): AdjustResult {
        if (h == null) return AdjustResult(null, null, null, false)
        val entered = f.hours.isNotBlank() || f.minutes.isNotBlank()
        if (!entered) return AdjustResult(null, null, null, false)
        val mins = input(f)
        val auto = h.autoMinutes
        val now = if (h.isToday) "지금 자동 " else "자동 "
        return when (f.type) {
            AdjustType.SET -> AdjustResult(Format.duration(mins), "입력한 시간이 그날의 사용 시간이에요", null, true)
            AdjustType.ADD -> if (auto == null || mins == 0) AdjustResult(null, null, null, false)
                else AdjustResult(Format.duration(auto + mins), "$now${Format.duration(auto)} + ${Format.duration(mins)}", null, true)
            AdjustType.SUB -> when {
                auto == null || mins == 0 -> AdjustResult(null, null, null, false)
                mins > auto -> AdjustResult(null, null, "자동 기록(${Format.duration(auto)})보다 많이 줄일 수 없어요", false)
                else -> AdjustResult(Format.duration(auto - mins), "$now${Format.duration(auto)} − ${Format.duration(mins)}", null, true)
            }
        }
    }

    fun save(onDone: () -> Unit) {
        if (!result.value.valid) return
        val f = _form.value
        viewModelScope.launch { repo.saveAdjustment(UsageAdjustment(subId, date, f.type, input(f))); onDone() }
    }

    /** 수정 취소: 자동 기록값으로 돌아감 (F5-4) */
    fun cancelAdjustment(onDone: () -> Unit) = viewModelScope.launch { repo.cancelAdjustment(subId, date); onDone() }
}
