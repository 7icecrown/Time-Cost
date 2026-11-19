package com.timecost.demo.domain.usecase

import com.timecost.demo.domain.model.*
import java.time.LocalDate

/** 체험·할인은 종료일이 기준일보다 앞서면 '결정 대기' (F1-1) */
fun Subscription.effectiveStatus(today: LocalDate): SubStatus = when {
    status == SubStatus.ENDED -> SubStatus.ENDED
    type == SubType.TRIAL_OR_PROMO && endDate != null && endDate.isBefore(today) -> SubStatus.DECISION_PENDING
    else -> status
}
