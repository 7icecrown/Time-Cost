package com.timecost.demo.domain.model

import java.time.LocalDate

enum class SubType { RECURRING, TRIAL_OR_PROMO }
enum class TrackingMode { AUTO, OFF }
enum class SubStatus { ACTIVE, DECISION_PENDING, ENDED }
enum class UsageStatus { RECORDED, NO_RECORD, UNCERTAIN }
enum class UsageSourceKind { AUTO, TRIAL_CARRIED }
enum class AdjustType { ADD, SUB, SET }

data class Subscription(
    val id: Long,
    val name: String,
    val type: SubType,
    val price: Long,
    val cycleMonths: Int,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val afterPrice: Long? = null,
    val afterCycleMonths: Int? = null,
    val trackingMode: TrackingMode = TrackingMode.AUTO,
    val cancelMemo: String = "",
    val notifyEnabled: Boolean = false,
    val status: SubStatus = SubStatus.ACTIVE,
    val deletedAt: LocalDate? = null,
    val deleteReason: String? = null,
)

data class DailyUsage(
    val subscriptionId: Long, val date: LocalDate, val autoMinutes: Int?,
    val status: UsageStatus, val source: UsageSourceKind = UsageSourceKind.AUTO,
)

data class UsageAdjustment(val subscriptionId: Long, val date: LocalDate, val type: AdjustType, val minutes: Int)

data class LinkedApp(val subscriptionId: Long, val packageName: String, val appLabel: String)

/** 구독 추가 화면에서 저장 시 넘기는 값 */
data class NewSubscription(
    val name: String, val type: SubType, val price: Long, val cycleMonths: Int,
    val startDate: java.time.LocalDate, val endDate: java.time.LocalDate? = null,
    val afterPrice: Long? = null, val afterCycleMonths: Int? = null,
    val trackingMode: TrackingMode = TrackingMode.AUTO, val cancelMemo: String = "",
    val notifyEnabled: Boolean = false, val apps: List<InstalledApp> = emptyList(),
)

data class InstalledApp(val packageName: String, val label: String)

/** F7 계산 기록. createdAt은 기준일(AppClock) 날짜의 epoch millis */
data class CalcRecord(val id: Long, val name: String?, val price: Long, val minutes: Int, val createdAt: Long)
