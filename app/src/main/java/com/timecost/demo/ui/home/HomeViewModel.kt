package com.timecost.demo.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timecost.demo.BuildConfig
import com.timecost.demo.core.AppClock
import com.timecost.demo.core.Format
import com.timecost.demo.core.demoBanner
import com.timecost.demo.data.demo.DemoFlags
import com.timecost.demo.data.settings.PermissionChecker
import com.timecost.demo.data.settings.UsageNotice
import com.timecost.demo.data.repo.HomeEvents
import kotlinx.coroutines.delay
import com.timecost.demo.data.repo.Snapshot
import com.timecost.demo.data.repo.SubscriptionRepository
import com.timecost.demo.domain.model.*
import com.timecost.demo.domain.usecase.BillingCalc
import com.timecost.demo.domain.usecase.effectiveStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DecisionItem(val id: Long, val name: String, val sub: String, val needsDecision: Boolean, val dDay: String?, val urgent: Boolean)
data class CardItem(
    val id: Long, val name: String, val monthly: String, val payLine: String?,
    val usageLine: String?, val noRecordLine: String?, val dueLine: String?, val dueUrgent: Boolean,
    val tags: List<String>, val highlighted: Boolean = false,
)
data class HomeUiState(
    val demoBanner: String? = null,
    val decisions: List<DecisionItem> = emptyList(),
    val count: Int = 0, val monthlyTotal: String = "0원",
    val cards: List<CardItem> = emptyList(), val trashCount: Int = 0,
    /** 권한이 없거나 조회에 실패했을 때, 사용 시간 영역에만 보이는 안내 (PRD 10장) */
    val usageNotice: UsageNotice? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: SubscriptionRepository, private val clock: AppClock, private val events: HomeEvents,
    private val flags: DemoFlags, perms: PermissionChecker,
) : ViewModel() {
    val state: StateFlow<HomeUiState> = combine(repo.snapshot, events.highlightId, clock.offsetFlow, perms.noticeFlow()) { snap, hl, _, notice -> toUi(snap, hl, notice) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeUiState())
    val message: StateFlow<String?> = events.message
    fun messageShown() { events.message.value = null }

    init {
        viewModelScope.launch { if (flags.active) repo.seedIfEmpty() }
        // 등록 완료 하이라이트는 3초 동안만 (PRD 14장 열린 질문 4: 새 구독은 평소 정렬 위치에서 하이라이트하고 그 위치로 스크롤)
        viewModelScope.launch { events.highlightId.filterNotNull().collectLatest { delay(3000); events.highlightId.value = null } }
    }

    private fun toUi(s: Snapshot, highlight: Long?, notice: UsageNotice?): HomeUiState {
        val today = clock.today()
        val recurring = s.subscriptions.filter { it.type == SubType.RECURRING }
        val trials = s.subscriptions.filter { it.type == SubType.TRIAL_OR_PROMO }
        // 체험 때 가져온 기록은 결제 기간 통계에 넣지 않습니다 (F6-3)
        val usageBySub = s.usage.filter { it.source == UsageSourceKind.AUTO }.groupBy { it.subscriptionId }
        val adjBySub = s.adjustments.groupBy { it.subscriptionId }

        val decisions = trials.sortedBy { it.endDate }.map { t ->
            val pending = t.effectiveStatus(today) == SubStatus.DECISION_PENDING
            val left = t.endDate?.let { BillingCalc.daysUntil(it, today) }
            val promo = if (t.price > 0) "${Format.won(t.price)} · " else ""
            DecisionItem(t.id, t.name,
                sub = if (pending) "결정이 필요해요" else promo + "${Format.monthDay(t.endDate!!)} 종료",
                needsDecision = pending, dDay = if (pending) null else left?.let(Format::dDay),
                urgent = left != null && left <= 3)
        }.sortedByDescending { it.needsDecision }

        val cards = recurring.map { r ->
            val monthly = BillingCalc.monthlyPrice(r.price, r.cycleMonths)
            val next = BillingCalc.nextPaymentDate(r.startDate, r.cycleMonths, today)
            val left = BillingCalc.daysUntil(next, today)
            val off = r.trackingMode == TrackingMode.OFF
            val period = BillingCalc.periodUsage(
                BillingCalc.currentPeriodStart(r.startDate, r.cycleMonths, today), today,
                usageBySub[r.id].orEmpty().associateBy { it.date }, adjBySub[r.id].orEmpty().associateBy { it.date })
            val free = r.price == 0L
            CardItem(
                id = r.id, name = r.name, monthly = Format.won(monthly),
                payLine = if (r.cycleMonths == 1) null else
                    "${Format.won(r.price)} · " + if (r.cycleMonths == 12) "매년" else "${r.cycleMonths}개월마다",
                usageLine = if (off) null else when (notice) {
                    UsageNotice.NO_PERMISSION -> "사용 시간을 보여줄 수 없어요"
                    UsageNotice.READ_FAILED -> "사용 시간을 읽지 못했어요"
                    null -> "${Format.duration(period.minutes)} 사용"
                },
                noRecordLine = if (!off && notice == null && period.noRecordDays > 0) "(기록 없는 날 ${period.noRecordDays}일)" else null,
                dueLine = if (free) null else "결제 ${Format.dDay(left)}", dueUrgent = left <= 3,
                tags = buildList { if (off) add("사용 시간 안 재요"); if (free) add("무료") },
                highlighted = r.id == highlight)
        }

        return HomeUiState(
            demoBanner = clock.demoBanner(),
            decisions = decisions, count = recurring.size,
            monthlyTotal = Format.won(recurring.sumOf { BillingCalc.monthlyPrice(it.price, it.cycleMonths) }),
            cards = cards, trashCount = s.trashCount, usageNotice = notice)
    }
}
