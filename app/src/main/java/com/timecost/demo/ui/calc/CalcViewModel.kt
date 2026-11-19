package com.timecost.demo.ui.calc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timecost.demo.core.AppClock
import com.timecost.demo.core.Format
import com.timecost.demo.data.repo.SubscriptionRepository
import com.timecost.demo.domain.model.CalcRecord
import com.timecost.demo.domain.model.Subscription
import com.timecost.demo.domain.model.SubType
import com.timecost.demo.domain.model.TrackingMode
import com.timecost.demo.domain.usecase.BillingCalc
import com.timecost.demo.ui.add.formatDigits
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject

data class CalcForm(
    val name: String = "", val priceText: String = "", val free: Boolean = false,
    val hours: String = "", val minutes: String = "",
)

/** F7-1 · F7-4 결과 상태 */
sealed interface CalcResult {
    /** 입력이 모자랄 때 안내 문구 (사용 시간 0 포함) */
    data class Hint(val text: String) : CalcResult
    data class Cost(val won: String, val detail: String) : CalcResult
    /** 무료: 비용 대신 사용 시간만 표시 */
    data class FreeOnly(val detail: String) : CalcResult
}

data class CalcRecordUi(val record: CalcRecord, val title: String, val result: String, val detail: String, val dateLabel: String)

@HiltViewModel
class CalcViewModel @Inject constructor(private val repo: SubscriptionRepository, private val clock: AppClock) : ViewModel() {
    private val _form = MutableStateFlow(CalcForm())
    val form: StateFlow<CalcForm> = _form

    val result: StateFlow<CalcResult> = _form.map(::compute)
        .stateIn(viewModelScope, SharingStarted.Eagerly, CalcResult.Hint("사용 시간을 입력하면 보여드려요"))

    val records: StateFlow<List<CalcRecordUi>> = repo.calcRecords.map { l -> l.map(::toUi) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** F7-2 구독 불러오기 대상: 사용 시간 안 재요로 등록한 정기 구독 */
    val loadable: StateFlow<List<Subscription>> = repo.snapshot
        .map { s -> s.subscriptions.filter { it.type == SubType.RECURRING && it.trackingMode == TrackingMode.OFF } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun update(f: (CalcForm) -> CalcForm) = _form.update(f)
    fun setPrice(v: String) = update { it.copy(priceText = formatDigits(v)) }
    fun setHours(v: String) = update { it.copy(hours = v.filter(Char::isDigit).take(4)) }
    fun setMinutes(v: String) = update { it.copy(minutes = v.filter(Char::isDigit).take(2)) }

    fun load(s: Subscription) = update {
        it.copy(name = s.name, priceText = if (s.price == 0L) "" else formatDigits(s.price.toString()), free = s.price == 0L)
    }

    /** F7-3 기록을 누르면 값이 입력 칸에 다시 채워짐 */
    fun refill(r: CalcRecord) {
        _form.value = CalcForm(name = r.name.orEmpty(), priceText = if (r.price == 0L) "" else formatDigits(r.price.toString()),
            free = r.price == 0L, hours = (r.minutes / 60).takeIf { it > 0 }?.toString() ?: "",
            minutes = (r.minutes % 60).takeIf { it > 0 || r.minutes < 60 }?.toString() ?: "")
    }

    private fun mins(f: CalcForm) = (f.hours.toIntOrNull() ?: 0) * 60 + (f.minutes.toIntOrNull() ?: 0).coerceAtMost(59)
    private fun price(f: CalcForm): Long = if (f.free) 0 else f.priceText.filter(Char::isDigit).toLongOrNull() ?: 0

    private fun compute(f: CalcForm): CalcResult {
        val m = mins(f)
        if (m == 0) return CalcResult.Hint("사용 시간을 입력하면 보여드려요")   // F7-4
        if (f.free) return CalcResult.FreeOnly("무료 · ${Format.duration(m)}")
        val p = price(f)
        if (p == 0L) return CalcResult.Hint("낸 구독료를 입력하면 보여드려요")
        return CalcResult.Cost(Format.won(BillingCalc.costPerHour(p, m)!!), "${Format.won(p)} · ${Format.duration(m)}")
    }

    val canSave: StateFlow<Boolean> = result.map { it !is CalcResult.Hint }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun saveRecord() {
        val f = _form.value; if (!canSave.value) return
        // 기준일(AppClock)의 날짜로 기록해 데모 날짜 이동과 맞춤
        val createdAt = clock.today().atTime(LocalTime.now()).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        viewModelScope.launch { repo.addCalcRecord(f.name, price(f), mins(f), createdAt) }
    }

    private fun toUi(r: CalcRecord): CalcRecordUi {
        val date = Instant.ofEpochMilli(r.createdAt).atZone(ZoneId.systemDefault()).toLocalDate()
        val free = r.price == 0L
        return CalcRecordUi(r, r.name ?: "직접 입력",
            result = if (free) "사용 시간만 표시" else Format.won(BillingCalc.costPerHour(r.price, r.minutes) ?: 0),
            detail = (if (free) "무료" else Format.won(r.price)) + " · ${Format.duration(r.minutes)}",
            dateLabel = Format.monthDay(date))
    }
}
