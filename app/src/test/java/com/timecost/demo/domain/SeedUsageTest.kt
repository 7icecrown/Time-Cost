package com.timecost.demo.domain

import com.timecost.demo.data.db.toDomain
import com.timecost.demo.data.repo.DemoSeed
import com.timecost.demo.domain.usecase.BillingCalc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** 시드 데이터가 PRD 11장·시안의 홈 카드 값(사용 시간, 기록 없는 날)과 일치하는지 확인 */
class SeedUsageTest {
    private val today = DemoSeed.BASE_DATE
    private fun period(idx: Int): BillingCalc.PeriodUsage {
        val sub = DemoSeed.subscriptions[idx - 1].toDomainLike(idx.toLong())
        val from = BillingCalc.currentPeriodStart(sub.startDate, sub.cycleMonths, today)
        val u = DemoSeed.usage.filter { it.subscriptionId == idx.toLong() }.map { it.toDomain() }.associateBy { it.date }
        return BillingCalc.periodUsage(from, today, u, emptyMap())
    }
    private fun com.timecost.demo.data.db.SubscriptionEntity.toDomainLike(id: Long) = copy(id = id).toDomain()

    @Test fun youtube() = assertEquals(BillingCalc.PeriodUsage(220, 0), period(1))   // 3시간 40분

    /** 시나리오 E: 시안 Adj1·Adj2·Adj3의 자동 기록과 일치 */
    @Test fun youtube_dailyRecords_matchAdjMocks() {
        val u = DemoSeed.usage.filter { it.subscriptionId == 1L }.map { it.toDomain() }.associateBy { it.date }
        assertEquals(50, u.getValue(java.time.LocalDate.of(2026, 11, 8)).autoMinutes)          // Adj1 자동 기록 50분
        assertNull(u.getValue(java.time.LocalDate.of(2026, 11, 7)).autoMinutes)                // Adj2 기록 없음
        assertEquals(80, u.getValue(today).autoMinutes)                                        // Adj3 오늘 자동 1시간 20분
        val adj = com.timecost.demo.domain.model.UsageAdjustment(1, java.time.LocalDate.of(2026, 11, 8), com.timecost.demo.domain.model.AdjustType.ADD, 30)
        assertEquals(80, BillingCalc.dayMinutes(u[adj.date], adj))                             // 50분 + 30분 = 1시간 20분
        val todayAdj = com.timecost.demo.domain.model.UsageAdjustment(1, today, com.timecost.demo.domain.model.AdjustType.ADD, 15)
        assertEquals(95, BillingCalc.dayMinutes(u[today], todayAdj))                           // 1시간 20분 + 15분 = 1시간 35분
    }
    @Test fun netflix() = assertEquals(BillingCalc.PeriodUsage(480, 2), period(2))   // 8시간, 기록 없는 날 2일
    @Test fun milli() = assertEquals(BillingCalc.PeriodUsage(370, 0), period(3))     // 6시간 10분
    @Test fun duolingo() = assertEquals(BillingCalc.PeriodUsage(570, 0), period(5))  // 9시간 30분
}
