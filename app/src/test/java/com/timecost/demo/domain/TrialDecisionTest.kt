package com.timecost.demo.domain

import com.timecost.demo.core.Format
import com.timecost.demo.data.db.toDomain
import com.timecost.demo.data.repo.DemoSeed
import com.timecost.demo.domain.model.*
import com.timecost.demo.domain.usecase.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class TrialDecisionTest {
    private val today = DemoSeed.BASE_DATE
    private val disney get() = DemoSeed.subscriptions[5].copy(id = 6).toDomain()

    @Test fun baseName() {
        assertEquals("Disney+", TrialDecision.baseName("Disney+ 무료 체험"))
        assertEquals("Wavve", TrialDecision.baseName("Wavve 할인"))
        assertEquals("무료 체험", TrialDecision.baseName("무료 체험"))   // 이름이 전부 접미사면 그대로
    }

    @Test fun keepDraft_matchesMock() {
        val usage = DemoSeed.usage.filter { it.subscriptionId == 6L }.map { it.toDomain() }
        val d = TrialDecision.keepDraft(disney, emptyList(), usage, emptyList())
        assertEquals(LocalDate.of(2026, 11, 8), d.start)                 // 체험 종료 다음 날
        assertEquals(9_500L, d.price)
        assertEquals(390, d.carried.minutes)                              // 6시간 30분
        assertEquals(1, d.carried.noRecordDays)                           // 기록 없는 날 1일
        val next = BillingCalc.nextPaymentDate(d.start, d.cycleMonths, today)
        assertEquals(28L, BillingCalc.daysUntil(next, today))             // 12월 8일 · D-28
    }

    @Test fun keepDraft_withoutAfterPrice_goesToEdit() {
        val apple = DemoSeed.subscriptions[6].copy(id = 7).toDomain()
        assertNull(TrialDecision.keepDraft(apple, emptyList(), emptyList(), emptyList()).price)
    }

    @Test fun adjustment_rules() {
        val u = DailyUsage(1, today, 50, UsageStatus.RECORDED)
        assertEquals(80, BillingCalc.dayMinutes(u, UsageAdjustment(1, today, AdjustType.ADD, 30)))
        assertEquals(20, BillingCalc.dayMinutes(u, UsageAdjustment(1, today, AdjustType.SUB, 30)))
        assertEquals(0, BillingCalc.dayMinutes(u, UsageAdjustment(1, today, AdjustType.SUB, 90)))   // 0분 아래로 내려가지 않음
        assertEquals(45, BillingCalc.dayMinutes(u, UsageAdjustment(1, today, AdjustType.SET, 45)))
        val none = DailyUsage(1, today, null, UsageStatus.NO_RECORD)
        assertEquals(60, BillingCalc.dayMinutes(none, UsageAdjustment(1, today, AdjustType.SET, 60)))
        assertNull(BillingCalc.dayMinutes(none, null))                    // 기록 없는 날은 추정하지 않음
    }

    @Test fun alerts_skipPastDates() {
        val apple = DemoSeed.subscriptions[6].copy(id = 7).toDomain()      // 종료 11월 12일, 알림 켜짐
        val plan = AlertPlanner.plan(listOf(apple), today)
        assertEquals(listOf(1, 0), plan.map { it.daysLeft })              // D-3(11/9)은 이미 지남
        assertEquals(LocalDate.of(2026, 11, 11), plan.first().date)
    }

    @Test fun eulReul() {
        assertEquals("를", Format.eulReul("Disney+"))
        assertEquals("을", Format.eulReul("밀리의 서재".dropLast(1) + "삼"))   // 받침 있음
        assertEquals("를", Format.eulReul("티빙".dropLast(1) + "비"))          // 받침 없음
    }
}
