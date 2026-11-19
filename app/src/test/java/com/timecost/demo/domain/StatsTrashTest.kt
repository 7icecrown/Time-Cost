package com.timecost.demo.domain

import com.timecost.demo.data.db.toDomain
import com.timecost.demo.data.repo.DemoSeed
import com.timecost.demo.domain.model.*
import com.timecost.demo.domain.usecase.StatsCalc
import com.timecost.demo.domain.usecase.StatsCalc.CostState
import com.timecost.demo.domain.usecase.TrashCalc
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class StatsTrashTest {
    private val today = DemoSeed.BASE_DATE
    private val subs = DemoSeed.subscriptions.mapIndexed { i, e -> e.copy(id = i + 1L).toDomain() }
    private val recurring = subs.filter { it.type == SubType.RECURRING && it.deletedAt == null }
    private val usage = DemoSeed.usage.map { it.toDomain() }

    @Test fun monthlyTotalAndShares_matchMock() {
        assertEquals(46_550L, StatsCalc.monthlyTotal(recurring))
        val s = StatsCalc.shares(recurring)
        assertEquals(listOf("YouTube Premium", "Netflix", "밀리의 서재", "Notion", "Duolingo"), s.map { it.name })
        assertEquals(listOf(32, 29, 21, 18, 0), s.map { it.percent })
        assertTrue(s.last().free)
    }

    @Test fun nextPayment_isNetflixNov13() {
        val n = StatsCalc.nextPayment(recurring, today)!!
        assertEquals("Netflix", n.sub.name)
        assertEquals(LocalDate.of(2026, 11, 13), n.date)
        assertEquals(3L, n.daysLeft)
        assertEquals(13_500L, n.sub.price)
    }

    @Test fun hourCosts_current() {
        val c = StatsCalc.hourCosts(recurring, usage, emptyList(), today, StatsCalc.Which.CURRENT).associateBy { it.name }
        assertEquals(4_064L, c.getValue("YouTube Premium").cost)               // 14,900 ÷ 3시간 40분
        assertEquals(1_688L, c.getValue("Netflix").cost)                       // 13,500 ÷ 8시간 (반올림)
        assertEquals(2, c.getValue("Netflix").noRecordDays)
        assertEquals(CostState.NO_TRACKING, c.getValue("Notion").state)        // 사용 시간 기록 없음
        assertEquals(CostState.FREE, c.getValue("Duolingo").state)
    }

    @Test fun hourCosts_previousPeriod_noneYet_thenAvailableAfter30Days() {
        val prev = StatsCalc.hourCosts(recurring, usage, emptyList(), today, StatsCalc.Which.PREVIOUS).associateBy { it.name }
        assertEquals(CostState.NO_PERIOD, prev.getValue("Netflix").state)      // 첫 결제 기간이라 지난 기간 없음
        val later = StatsCalc.hourCosts(recurring, usage, emptyList(), today.plusDays(30), StatsCalc.Which.PREVIOUS).associateBy { it.name }
        assertEquals(1_688L, later.getValue("Netflix").cost)                   // 10/13~11/12 기간이 '지난 기간'이 됨
    }

    @Test fun trash_remainingDays_matchMock() {
        val spotify = subs.first { it.name == "Spotify" }
        val coupang = subs.first { it.name.startsWith("Coupang") }
        assertEquals(23L, TrashCalc.remainingDays(spotify.deletedAt!!, today))
        assertEquals(3L, TrashCalc.remainingDays(coupang.deletedAt!!, today))
        assertTrue(TrashCalc.restoreGoesToDecision(coupang, today))            // 종료일(10/31)이 지남
        assertFalse(TrashCalc.restoreGoesToDecision(spotify, today))
    }

    @Test fun trash_expiry() {
        val d = LocalDate.of(2026, 10, 11)
        assertFalse(TrashCalc.isExpired(d, d.plusDays(29)))
        assertTrue(TrashCalc.isExpired(d, d.plusDays(30)))
        assertEquals(0L, TrashCalc.remainingDays(d, d.plusDays(45)))
    }
}
