package com.timecost.demo.data.db

import androidx.room.*
import com.timecost.demo.domain.model.*
import java.time.LocalDate

class Converters {
    @TypeConverter fun toDate(v: String?): LocalDate? = v?.let(LocalDate::parse)
    @TypeConverter fun fromDate(d: LocalDate?): String? = d?.toString()
}

@Entity(tableName = "subscription")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val type: SubType, val price: Long, val cycleMonths: Int,
    val startDate: LocalDate, val endDate: LocalDate?, val afterPrice: Long?, val afterCycleMonths: Int?,
    val trackingMode: TrackingMode, val cancelMemo: String, val notifyEnabled: Boolean,
    val status: SubStatus, val deletedAt: LocalDate?, val deleteReason: String?,
)

@Entity(tableName = "linked_app", primaryKeys = ["subscriptionId", "packageName"])
data class LinkedAppEntity(val subscriptionId: Long, val packageName: String, val appLabel: String)

@Entity(tableName = "daily_usage", primaryKeys = ["subscriptionId", "date", "source"])
data class DailyUsageEntity(
    val subscriptionId: Long, val date: LocalDate, val autoMinutes: Int?,
    val status: UsageStatus, val source: UsageSourceKind,
)

@Entity(tableName = "usage_adjustment", primaryKeys = ["subscriptionId", "date"])
data class UsageAdjustmentEntity(val subscriptionId: Long, val date: LocalDate, val type: AdjustType, val minutes: Int)

@Entity(tableName = "calc_record")
data class CalcRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String?, val price: Long, val minutes: Int, val createdAt: Long,
)

fun SubscriptionEntity.toDomain() = Subscription(id, name, type, price, cycleMonths, startDate, endDate,
    afterPrice, afterCycleMonths, trackingMode, cancelMemo, notifyEnabled, status, deletedAt, deleteReason)
fun DailyUsageEntity.toDomain() = DailyUsage(subscriptionId, date, autoMinutes, status, source)
fun UsageAdjustmentEntity.toDomain() = UsageAdjustment(subscriptionId, date, type, minutes)
fun CalcRecordEntity.toDomain() = CalcRecord(id, name, price, minutes, createdAt)
