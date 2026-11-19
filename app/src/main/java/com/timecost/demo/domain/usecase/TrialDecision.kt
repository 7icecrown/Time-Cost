package com.timecost.demo.domain.usecase

import com.timecost.demo.domain.model.*
import java.time.LocalDate

/** 유지하기를 눌렀을 때 만들어질 정기 구독의 초안 (F6-2 '저장될 내용') */
data class KeepDraft(
    val name: String,
    val price: Long?,            // 종료 후 구독료. 비워 둔 체험이면 null → 수정 화면으로 바로 이동(PRD 14장 열린 질문 3)
    val cycleMonths: Int,
    val start: LocalDate,        // 체험 종료 다음 날 = 첫 결제일
    val tracking: TrackingMode,
    val apps: List<InstalledApp>,
    val carried: BillingCalc.PeriodUsage,   // 체험 때 가져올 기록
)

object TrialDecision {
    private val suffix = Regex("\\s*(무료 체험|체험|할인)$")

    /** 'Disney+ 무료 체험' → 'Disney+' */
    fun baseName(name: String): String = name.replace(suffix, "").trim().ifEmpty { name }

    fun keepDraft(
        sub: Subscription, linked: List<LinkedApp>, usage: List<DailyUsage>, adj: List<UsageAdjustment>,
    ): KeepDraft {
        val end = sub.endDate ?: sub.startDate
        val carried = BillingCalc.periodUsage(sub.startDate, end,
            usage.associateBy { it.date }, adj.associateBy { it.date })
        return KeepDraft(
            name = baseName(sub.name), price = sub.afterPrice, cycleMonths = sub.afterCycleMonths ?: 1,
            start = end.plusDays(1), tracking = sub.trackingMode,
            apps = linked.map { InstalledApp(it.packageName, it.appLabel) }, carried = carried,
        )
    }
}
