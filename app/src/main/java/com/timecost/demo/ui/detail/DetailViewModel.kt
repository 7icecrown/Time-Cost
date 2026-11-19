package com.timecost.demo.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timecost.demo.core.AppClock
import com.timecost.demo.core.Format
import com.timecost.demo.data.settings.PermissionChecker
import com.timecost.demo.data.settings.UsageNotice
import com.timecost.demo.data.repo.Snapshot
import com.timecost.demo.data.repo.SubscriptionRepository
import com.timecost.demo.domain.model.*
import com.timecost.demo.domain.usecase.BillingCalc
import com.timecost.demo.domain.usecase.effectiveStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DayItem(val dateLabel: String, val iso: String, val timeText: String, val badge: String?)
data class PeriodItem(val label: String, val usageText: String, val isCurrent: Boolean, val days: List<DayItem>)
data class DetailUi(
    val id: Long = 0, val name: String = "", val isTrial: Boolean = false, val found: Boolean = false,
    val rows: List<Pair<String, String>> = emptyList(),      // 결제 정보 / 기간 정보
    val usageTitle: String = "", val usageHeadline: String? = null, val usageSub: String? = null,
    val periods: List<PeriodItem> = emptyList(),
    val memo: String = "", val linkedApps: List<String> = emptyList(), val trackingOff: Boolean = false,
    val needsDecision: Boolean = false,
    val carriedText: String? = null,
    val usageNotice: UsageNotice? = null,
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedState: SavedStateHandle, private val repo: SubscriptionRepository, private val clock: AppClock, perms: PermissionChecker,
) : ViewModel() {
    private val id: Long = checkNotNull(savedState["id"])
    val state: StateFlow<DetailUi> = combine(repo.snapshot, clock.offsetFlow, perms.noticeFlow()) { snap, _, notice -> build(snap, notice) }.stateIn(viewModelScope, SharingStarted.Eagerly, DetailUi())

    fun delete(onDone: () -> Unit) = viewModelScope.launch { repo.softDelete(id, clock.today()); onDone() }

    private fun build(s: Snapshot, notice: UsageNotice?): DetailUi {
        val sub = s.subscriptions.firstOrNull { it.id == id } ?: return DetailUi(id = id)
        val today = clock.today()
        val usage = s.usage.filter { it.subscriptionId == id && it.source == UsageSourceKind.AUTO }.associateBy { it.date }
        val adj = s.adjustments.filter { it.subscriptionId == id }.associateBy { it.date }
        val off = sub.trackingMode == TrackingMode.OFF
        val apps = s.linkedApps.filter { it.subscriptionId == id }.map { it.appLabel }

        fun days(from: LocalDate, to: LocalDate): List<DayItem> =
            generateSequence(to) { it.minusDays(1) }.takeWhile { !it.isBefore(from) }.map { d ->
                val m = BillingCalc.dayMinutes(usage[d], adj[d])
                val badge = when {
                    adj[d] != null -> "수정"
                    usage[d]?.status == UsageStatus.NO_RECORD -> "기록 없음"
                    usage[d]?.status == UsageStatus.UNCERTAIN -> "정확하지 않음"
                    else -> null
                }
                DayItem(Format.monthDay(d), d.toString(), m?.let(Format::duration) ?: "–", badge)
            }.toList()

        if (sub.type == SubType.RECURRING) {
            val next = BillingCalc.nextPaymentDate(sub.startDate, sub.cycleMonths, today)
            val cur = BillingCalc.currentPeriodStart(sub.startDate, sub.cycleMonths, today)
            val monthly = BillingCalc.monthlyPrice(sub.price, sub.cycleMonths)
            val cyc = if (sub.cycleMonths == 1) "매월" else if (sub.cycleMonths == 12) "매년" else "${sub.cycleMonths}개월마다"
            val rows = listOf("구독료" to Format.won(sub.price), "결제 주기" to cyc,
                "시작일" to Format.monthDay(sub.startDate),
                "다음 결제일" to "${Format.monthDay(next)} (${Format.dDay(BillingCalc.daysUntil(next, today))})",
                "한 달 구독료" to Format.won(monthly))
            // 결제 기간별 사용 시간 (최신 → 과거, 최대 12개)
            // 결제 기간별 사용 시간 (최신 → 과거, 최대 12개). 시작일 기준으로 n번째 결제일을 계산해 말일 보정이 누적되지 않게 합니다.
            val n = generateSequence(0L) { it + 1 }.takeWhile { !sub.startDate.plusMonths(sub.cycleMonths * it).isAfter(cur) }.last()
            val periods = (n downTo maxOf(0L, n - 11)).map { k ->
                val start = sub.startDate.plusMonths(sub.cycleMonths * k)
                val endEx = sub.startDate.plusMonths(sub.cycleMonths * (k + 1))
                val last = minOf(endEx.minusDays(1), today)
                val pu = BillingCalc.periodUsage(start, last, usage, adj)
                PeriodItem("${Format.monthDay(start)} – ${Format.monthDay(endEx.minusDays(1))}",
                    if (off) "사용 시간 기록 없음" else Format.duration(pu.minutes) + if (pu.noRecordDays > 0) " (기록 없는 날 ${pu.noRecordDays}일)" else "",
                    start == cur, if (off) emptyList() else days(start, last))
            }
            val curPu = BillingCalc.periodUsage(cur, today, usage, adj)
            val dayN = java.time.temporal.ChronoUnit.DAYS.between(cur, today) + 1
            // 체험 때 가져온 기록은 결제 기간 통계에 넣지 않고 따로 표시 (F6-3)
            val carriedAdj = adj
            val carried = s.usage.filter { it.subscriptionId == id && it.source == UsageSourceKind.TRIAL_CARRIED }
                .mapNotNull { BillingCalc.dayMinutes(it, carriedAdj[it.date]) }.sum()
            return DetailUi(id, sub.name, false, true, rows,
                carriedText = if (carried > 0 && !off) "체험 때 가져온 기록 ${Format.duration(carried)} · 결제 기간 통계에는 넣지 않아요" else null,
                usageTitle = "이번 결제 기간 사용 시간",
                usageHeadline = if (off) null else Format.duration(curPu.minutes), usageSub = "결제 후 ${dayN}일째",
                periods = periods, linkedApps = apps, trackingOff = off, usageNotice = if (off) null else notice)
        }

        // 무료 체험·할인 (F4-3)
        val status = sub.effectiveStatus(today)
        val end = sub.endDate!!
        val statusText = when (status) {
            SubStatus.DECISION_PENDING -> "결정 대기"
            SubStatus.ENDED -> "종료"
            else -> "진행 중"
        }
        val pu = BillingCalc.periodUsage(sub.startDate, minOf(end, today), usage, adj)
        val rows = buildList {
            add("사용 기간" to "${Format.monthDay(sub.startDate)} – ${Format.monthDay(end)}")
            add("상태" to statusText)
            add("기간 중 구독료" to Format.won(sub.price))
            sub.afterPrice?.let { add("종료 후 구독료" to Format.won(it)) }
            if (!off) {
                add("기간 내 사용 시간" to Format.duration(pu.minutes))
                add("기록 없는 날" to "${pu.noRecordDays}일")
            }
            add("종료 알림" to if (sub.notifyEnabled) "받는 중" else "받지 않음")
        }
        return DetailUi(id, sub.name, true, true, rows, memo = sub.cancelMemo, linkedApps = apps, trackingOff = off,
            needsDecision = status == SubStatus.DECISION_PENDING)
    }
}
