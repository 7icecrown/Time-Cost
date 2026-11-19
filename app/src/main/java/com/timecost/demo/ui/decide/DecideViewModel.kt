package com.timecost.demo.ui.decide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timecost.demo.core.AppClock
import com.timecost.demo.core.Format
import com.timecost.demo.data.repo.HomeEvents
import com.timecost.demo.data.repo.SubscriptionRepository
import com.timecost.demo.domain.model.*
import com.timecost.demo.domain.usecase.BillingCalc
import com.timecost.demo.domain.usecase.KeepDraft
import com.timecost.demo.domain.usecase.TrialDecision
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DecideUi(
    val name: String, val periodLabel: String, val usageText: String?, val afterMonthly: String?,
    val noRecordDays: Int, val memo: String,
    /** 종료 후 구독료가 비어 있으면 확인 시트 대신 수정 화면으로 바로 이동 */
    val goesToEdit: Boolean,
    // 유지 내용 확인 시트 (F6-2)
    val keepName: String, val keepPriceLine: String?, val keepStart: String, val keepNext: String?,
    val trackingLine: String, val carriedLine: String?,
)

@HiltViewModel
class DecideViewModel @Inject constructor(
    private val repo: SubscriptionRepository, private val clock: AppClock, private val events: HomeEvents,
) : ViewModel() {
    private var loadedId = -1L
    private var draft: KeepDraft? = null
    private val _ui = MutableStateFlow<DecideUi?>(null)
    val ui: StateFlow<DecideUi?> = _ui

    fun load(id: Long) {
        loadedId = id
        viewModelScope.launch {
            val ctx = repo.loadTrialContext(id) ?: return@launch
            val s = ctx.sub; val today = clock.today()
            val d = TrialDecision.keepDraft(s, ctx.apps, ctx.usage, ctx.adjustments).also { draft = it }
            val off = s.trackingMode == TrackingMode.OFF
            val cyc = d.cycleMonths.let { if (it == 1) "매월" else if (it == 12) "매년" else "${it}개월마다" }
            _ui.value = DecideUi(
                name = s.name,
                periodLabel = "${Format.monthDay(s.startDate)} – ${Format.monthDay(s.endDate!!)}",
                usageText = if (off) null else Format.duration(d.carried.minutes),
                afterMonthly = s.afterPrice?.let { Format.won(BillingCalc.monthlyPrice(it, d.cycleMonths)) },
                noRecordDays = if (off) 0 else d.carried.noRecordDays, memo = s.cancelMemo,
                goesToEdit = d.price == null,
                keepName = d.name,
                keepPriceLine = d.price?.let { "${Format.won(it)} · $cyc" },
                keepStart = Format.monthDay(d.start),
                keepNext = d.price?.let {
                    val next = BillingCalc.nextPaymentDate(d.start, d.cycleMonths, today)
                    "${Format.monthDay(next)} · ${Format.dDay(BillingCalc.daysUntil(next, today))}"
                },
                trackingLine = if (off) "사용 시간 안 재요" else "자동으로 재요" + if (d.apps.isEmpty()) "" else " · ${d.apps.joinToString(", ") { it.label }} 연결",
                carriedLine = if (off || d.carried.minutes == 0) null else "${Format.duration(d.carried.minutes)} 가져와요 · 원본은 그대로 남아요",
            )
        }
    }

    /** 확인: 정기 구독 생성 → 홈에서 하이라이트와 안내 문구 (F6-3) */
    fun confirmKeep(onDone: () -> Unit) {
        val d = draft ?: return; val price = d.price ?: return
        viewModelScope.launch {
            val id = repo.keepTrial(loadedId, NewSubscription(
                name = d.name, type = SubType.RECURRING, price = price, cycleMonths = d.cycleMonths, startDate = d.start,
                trackingMode = d.tracking, apps = d.apps), clock.today())
            events.highlightId.value = id
            events.message.value = "${d.name}${Format.eulReul(d.name)} 정기 구독으로 등록했어요"
            onDone()
        }
    }

    /** 해지 완료로 표시: 홈에서 사라지고 휴지통으로 (F6-5) */
    fun markCancelled(onDone: () -> Unit) {
        val name = _ui.value?.name ?: return
        viewModelScope.launch {
            repo.markCancelled(loadedId, clock.today())
            events.message.value = "$name${Format.eulReul(name)} 해지 완료로 표시했어요. 휴지통에서 볼 수 있어요"
            onDone()
        }
    }
}
