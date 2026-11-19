package com.timecost.demo.domain.usecase

import com.timecost.demo.domain.model.*
import java.time.LocalDate

data class PlannedAlert(val subscriptionId: Long, val name: String, val date: LocalDate, val daysLeft: Int)

/** F3-5: 종료 D-3, D-1, 종료 당일에 알립니다. 이미 지난 날짜는 제외. */
object AlertPlanner {
    fun plan(subs: List<Subscription>, today: LocalDate): List<PlannedAlert> = subs
        .filter { it.type == SubType.TRIAL_OR_PROMO && it.notifyEnabled && it.endDate != null && it.deletedAt == null && it.status != SubStatus.ENDED }
        .flatMap { s -> s.endDate?.let { end -> listOf(3, 1, 0).map { n -> PlannedAlert(s.id, s.name, end.minusDays(n.toLong()), n) } } ?: emptyList() }
        .filter { !it.date.isBefore(today) }
        .sortedWith(compareBy({ it.date }, { it.subscriptionId }))
}
