package com.timecost.demo.domain.usecase

import com.timecost.demo.domain.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** F9 휴지통 규칙: 30일 보관, 남은 일수, 복구 안내 */
object TrashCalc {
    const val KEEP_DAYS = 30L

    fun elapsed(deletedAt: LocalDate, today: LocalDate) = ChronoUnit.DAYS.between(deletedAt, today)
    fun remainingDays(deletedAt: LocalDate, today: LocalDate): Long = (KEEP_DAYS - elapsed(deletedAt, today)).coerceAtLeast(0)
    /** F9-5 보관 기간(30일)이 지난 항목은 앱을 실행할 때 완전 삭제 */
    fun isExpired(deletedAt: LocalDate, today: LocalDate) = elapsed(deletedAt, today) >= KEEP_DAYS
    /** F9-3 종료일이 이미 지난 체험·할인은 복구하면 바로 결정 대기 */
    fun restoreGoesToDecision(sub: Subscription, today: LocalDate) =
        sub.type == SubType.TRIAL_OR_PROMO && sub.endDate != null && sub.endDate.isBefore(today)
}
