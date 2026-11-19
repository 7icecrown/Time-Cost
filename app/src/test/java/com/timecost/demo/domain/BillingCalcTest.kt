package com.timecost.demo.domain

import com.timecost.demo.domain.usecase.BillingCalc
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class BillingCalcTest {
    private val today = LocalDate.of(2026, 11, 10)

    @Test fun monthlyPrice_examples() {
        assertEquals(8250L, BillingCalc.monthlyPrice(99_000, 12))
        assertEquals(9900L, BillingCalc.monthlyPrice(29_700, 3))
        assertEquals(0L, BillingCalc.monthlyPrice(0, 1))
    }
    @Test fun yearly() = assertEquals(558_600L, BillingCalc.yearly(46_550))
    @Test fun costPerHour() {
        assertEquals(2475L, BillingCalc.costPerHour(99_000, 40 * 60))
        assertNull(BillingCalc.costPerHour(99_000, 0))
    }
    @Test fun share() = assertEquals(32, BillingCalc.share(14_900, 46_550))

    @Test fun nextPayment_endOfMonthFallback() {
        val start = LocalDate.of(2027, 1, 31)
        assertEquals(LocalDate.of(2027, 2, 28), BillingCalc.nextPaymentDate(start, 1, LocalDate.of(2027, 2, 1)))
        assertEquals(LocalDate.of(2027, 3, 31), BillingCalc.nextPaymentDate(start, 1, LocalDate.of(2027, 3, 1)))
    }
    @Test fun dDay_seedExamples() {
        fun d(start: LocalDate, cycle: Int) =
            BillingCalc.daysUntil(BillingCalc.nextPaymentDate(start, cycle, today), today)
        assertEquals(29L, d(LocalDate.of(2026, 10, 9), 1))   // YouTube Premium
        assertEquals(3L, d(LocalDate.of(2026, 10, 13), 1))   // Netflix
        assertEquals(45L, d(LocalDate.of(2026, 9, 25), 3))   // 밀리의 서재
        assertEquals(96L, d(LocalDate.of(2026, 2, 14), 12))  // Notion
    }
    @Test fun periodStart() =
        assertEquals(LocalDate.of(2026, 10, 13), BillingCalc.currentPeriodStart(LocalDate.of(2026, 10, 13), 1, today))
}
