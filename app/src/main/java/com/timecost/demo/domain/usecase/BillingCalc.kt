package com.timecost.demo.domain.usecase

import com.timecost.demo.domain.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToLong

/** PRD 7장 "핵심 계산 규칙"의 순수 함수 모음. 안드로이드 의존성이 없어 JUnit으로 바로 검증합니다. */
object BillingCalc {
    /** 한 달 구독료 = 결제액 ÷ 주기(개월), 원 단위 반올림 */
    fun monthlyPrice(price: Long, cycleMonths: Int): Long =
        if (price == 0L || cycleMonths <= 0) 0L else (price.toDouble() / cycleMonths).roundToLong()

    /** 연 환산 = 한 달 구독료 합계 × 12 */
    fun yearly(monthlyTotal: Long) = monthlyTotal * 12

    /**
     * 다음 결제일 = 시작일에 주기를 더해 기준일 이후 가장 가까운 날(기준일 당일 포함).
     * 항상 '시작일 기준'으로 더해야 1/31 → 2/28 → 3/31처럼 원래 날짜가 복원됩니다.
     */
    fun nextPaymentDate(start: LocalDate, cycleMonths: Int, today: LocalDate): LocalDate {
        val cycle = cycleMonths.coerceAtLeast(1)
        var n = 1L
        var next = start.plusMonths(cycle * n)
        while (next.isBefore(today)) { n++; next = start.plusMonths(cycle * n) }
        return next
    }

    /** 현재 결제 기간의 시작일 (직전 결제일) */
    fun currentPeriodStart(start: LocalDate, cycleMonths: Int, today: LocalDate): LocalDate {
        val cycle = cycleMonths.coerceAtLeast(1)
        var n = 0L
        while (!start.plusMonths(cycle * (n + 1)).isAfter(today)) n++
        return start.plusMonths(cycle * n)
    }

    /** 현재 결제 기간이 시작일부터 몇 번째인지(0부터). 시작 전이면 0 */
    fun periodIndex(start: LocalDate, cycleMonths: Int, today: LocalDate): Long {
        val cycle = cycleMonths.coerceAtLeast(1)
        var n = 0L
        while (!start.plusMonths(cycle * (n + 1)).isAfter(today)) n++
        return n
    }

    /** D-n = 기준일과의 날짜 차이 */
    fun daysUntil(target: LocalDate, today: LocalDate): Long = ChronoUnit.DAYS.between(today, target)

    /** 1시간당 비용 = 낸 구독료 ÷ 사용 시간(시간), 원 단위 반올림. 사용 시간 0이면 null(안내 문구) */
    fun costPerHour(price: Long, minutes: Int): Long? =
        if (minutes <= 0) null else (price * 60.0 / minutes).roundToLong()

    /** 비중 = 구독별 한 달 구독료 ÷ 합계, 정수 % 반올림 */
    fun share(monthly: Long, total: Long): Int =
        if (total == 0L) 0 else (monthly * 100.0 / total).roundToLong().toInt()

    /** 하루 사용 시간 = 자동 기록 ± 수정. 기록 없는 날은 null (0으로 더하지 않음). SUB는 0분 아래 불가 */
    fun dayMinutes(usage: DailyUsage?, adj: UsageAdjustment?): Int? {
        val auto = usage?.autoMinutes
        if (adj == null) return auto
        return when (adj.type) {
            AdjustType.SET -> adj.minutes
            AdjustType.ADD -> (auto ?: return null) + adj.minutes
            AdjustType.SUB -> ((auto ?: return null) - adj.minutes).coerceAtLeast(0)
        }
    }

    data class PeriodUsage(val minutes: Int, val noRecordDays: Int)

    fun periodUsage(
        from: LocalDate, toInclusive: LocalDate,
        usages: Map<LocalDate, DailyUsage>, adjs: Map<LocalDate, UsageAdjustment>,
    ): PeriodUsage {
        var sum = 0; var none = 0; var d = from
        while (!d.isAfter(toInclusive)) {
            val m = dayMinutes(usages[d], adjs[d])
            if (m == null) { if (usages[d]?.status == UsageStatus.NO_RECORD) none++ } else sum += m
            d = d.plusDays(1)
        }
        return PeriodUsage(sum, none)
    }
}
