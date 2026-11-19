package com.timecost.demo.domain.usecase

import com.timecost.demo.domain.model.*
import java.time.LocalDate

/** F8 통계 계산 (순수 함수). 정기 구독만 대상이고 체험·할인은 제외합니다. */
object StatsCalc {
    data class Share(val id: Long, val name: String, val monthly: Long, val percent: Int, val free: Boolean)
    data class Next(val sub: Subscription, val date: LocalDate, val daysLeft: Long)
    enum class Which { CURRENT, PREVIOUS }
    enum class CostState { VALUE, NO_TRACKING, NO_PERIOD, FREE, NO_USAGE }
    data class HourCost(
        val id: Long, val name: String, val state: CostState,
        val cost: Long? = null, val price: Long = 0, val minutes: Int = 0, val noRecordDays: Int = 0,
    )

    fun monthlyTotal(subs: List<Subscription>): Long = subs.sumOf { BillingCalc.monthlyPrice(it.price, it.cycleMonths) }

    /** F8-3 구독별 한 달 구독료와 비중. 비싼 순, 무료는 맨 아래 */
    fun shares(subs: List<Subscription>): List<Share> {
        val total = monthlyTotal(subs)
        return subs.map {
            val m = BillingCalc.monthlyPrice(it.price, it.cycleMonths)
            Share(it.id, it.name, m, BillingCalc.share(m, total), free = it.price == 0L)
        }.sortedByDescending { it.monthly }
    }

    /** F8-2 가장 가까운 결제일. 무료 구독은 결제가 없어 제외. 같은 날이면 금액이 큰 쪽 */
    fun nextPayment(subs: List<Subscription>, today: LocalDate): Next? = subs.filter { it.price > 0 }
        .map { Next(it, BillingCalc.nextPaymentDate(it.startDate, it.cycleMonths, today), 0) }
        .minWithOrNull(compareBy<Next>({ it.date }, { -it.sub.price }))
        ?.let { it.copy(daysLeft = BillingCalc.daysUntil(it.date, today)) }

    /**
     * F8-4 1시간 쓸 때 드는 돈 = 낸 구독료 ÷ 사용 시간. 이번 결제 기간은 아직 진행 중이라
     * 사용 시간이 늘면 값이 줄어듭니다(화면에 안내). 사용 시간 안 재요는 따로 표시합니다.
     */
    fun hourCosts(
        subs: List<Subscription>, usage: List<DailyUsage>, adj: List<UsageAdjustment>, today: LocalDate, which: Which,
    ): List<HourCost> = subs.map { s ->
        if (s.trackingMode == TrackingMode.OFF) return@map HourCost(s.id, s.name, CostState.NO_TRACKING)
        if (s.price == 0L) return@map HourCost(s.id, s.name, CostState.FREE)
        val n = BillingCalc.periodIndex(s.startDate, s.cycleMonths, today)
        val k = if (which == Which.CURRENT) n else n - 1
        if (k < 0) return@map HourCost(s.id, s.name, CostState.NO_PERIOD, price = s.price)
        val from = s.startDate.plusMonths(s.cycleMonths * k)
        val to = minOf(s.startDate.plusMonths(s.cycleMonths * (k + 1)).minusDays(1), today)
        val u = usage.filter { it.subscriptionId == s.id && it.source == UsageSourceKind.AUTO }.associateBy { it.date }
        val a = adj.filter { it.subscriptionId == s.id }.associateBy { it.date }
        val pu = BillingCalc.periodUsage(from, to, u, a)
        val cost = BillingCalc.costPerHour(s.price, pu.minutes)
        if (cost == null) HourCost(s.id, s.name, CostState.NO_USAGE, price = s.price, noRecordDays = pu.noRecordDays)
        else HourCost(s.id, s.name, CostState.VALUE, cost, s.price, pu.minutes, pu.noRecordDays)
    }
}
