package com.timecost.demo.data.repo

import com.timecost.demo.data.db.*
import com.timecost.demo.data.notify.AlertScheduler
import com.timecost.demo.data.usage.UsageSource
import com.timecost.demo.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

data class Snapshot(
    val subscriptions: List<Subscription>,
    val usage: List<DailyUsage>,
    val adjustments: List<UsageAdjustment>,
    val trashCount: Int,
    val linkedApps: List<LinkedApp> = emptyList(),
)

/** 결정 흐름에서 쓰는 체험·할인 한 건의 전체 정보 (결정 후 목록에서 빠져도 읽을 수 있게 DB에서 직접 읽음) */
data class TrialContext(
    val sub: Subscription, val usage: List<DailyUsage>, val adjustments: List<UsageAdjustment>, val apps: List<LinkedApp>,
)

@Singleton
class SubscriptionRepository @Inject constructor(
    private val db: AppDatabase, private val usageSource: UsageSource, private val scheduler: AlertScheduler,
) {
    val snapshot: Flow<Snapshot> = combine(
        db.subscriptions().observeActive(), db.usage().observeUsage(),
        db.usage().observeAdjustments(), db.subscriptions().observeTrashCount(),
        db.linkedApps().observeAll(),
    ) { subs, usage, adj, trash, apps ->
        Snapshot(subs.map { it.toDomain() }, usage.map { it.toDomain() }, adj.map { it.toDomain() }, trash,
            apps.map { LinkedApp(it.subscriptionId, it.packageName, it.appLabel) })
    }

    suspend fun seedIfEmpty() {
        if (db.subscriptions().count() > 0) return
        val ids = db.subscriptions().insertAll(DemoSeed.subscriptions)
        db.usage().insertUsage(DemoSeed.usage)
        val apps = ids.zip(DemoSeed.subscriptions).mapNotNull { (id, s) ->
            DemoSeed.appFor(s.name)?.let { LinkedAppEntity(id, it.packageName, it.label) }
        }
        db.linkedApps().insertAll(apps)
        db.calc().insertAll(DemoSeed.calcRecords)
        scheduler.reschedule()
    }

    /** 새 구독 저장 + 연결 앱 저장 + 시작일~오늘 사용 시간 읽기. 반환: 새 id */
    suspend fun add(n: NewSubscription, today: LocalDate): Long {
        val id = db.subscriptions().insert(SubscriptionEntity(
            name = n.name, type = n.type, price = n.price, cycleMonths = n.cycleMonths, startDate = n.startDate,
            endDate = n.endDate, afterPrice = n.afterPrice, afterCycleMonths = n.afterCycleMonths,
            trackingMode = n.trackingMode, cancelMemo = n.cancelMemo, notifyEnabled = n.notifyEnabled,
            status = SubStatus.ACTIVE, deletedAt = null, deleteReason = null))
        if (n.trackingMode == TrackingMode.AUTO && n.apps.isNotEmpty()) {
            db.linkedApps().insertAll(n.apps.map { LinkedAppEntity(id, it.packageName, it.label) })
            if (!n.startDate.isAfter(today)) refreshUsage(id, n.apps.map { it.packageName }.toSet(), n.startDate, today)
        }
        scheduler.reschedule()
        return id
    }

    /** 일별 사용 시간을 읽어 저장. 읽지 못한 날은 NO_RECORD (추정하지 않음). 최대 400일. */
    suspend fun refreshUsage(id: Long, packages: Set<String>, from: LocalDate, to: LocalDate) {
        val begin = maxOf(from, to.minusDays(400))
        val dates = generateSequence(begin) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
        val rows = dates.map { d ->
            val m = usageSource.minutesFor(packages, d)
            DailyUsageEntity(id, d, m, if (m == null) UsageStatus.NO_RECORD else UsageStatus.RECORDED, UsageSourceKind.AUTO)
        }
        db.usage().insertUsage(rows)
    }

    suspend fun softDelete(id: Long, today: LocalDate) { db.subscriptions().softDelete(id, today); scheduler.reschedule() }

    // ---- 계산기 (F7) ----
    val calcRecords: Flow<List<CalcRecord>> = db.calc().observeAll().map { l -> l.map { it.toDomain() } }
    suspend fun addCalcRecord(name: String?, price: Long, minutes: Int, createdAt: Long) {
        db.calc().insert(CalcRecordEntity(name = name?.takeIf { it.isNotBlank() }, price = price, minutes = minutes, createdAt = createdAt))
    }

    // ---- 시연 (F12) ----
    /** 기준일을 앞으로 옮길 때 그 사이 날들의 가상 사용 기록을 만듭니다. 자동 기록 구독 + 연결된 앱이 있는 활성 구독만. */
    suspend fun simulateUsage(from: LocalDate, to: LocalDate) {
        db.subscriptions().activeList().filter { it.trackingMode == TrackingMode.AUTO }.forEach { s ->
            val pkgs = db.linkedApps().of(s.id).map { it.packageName }.toSet()
            if (pkgs.isEmpty()) return@forEach
            val begin = maxOf(from, s.startDate)
            val dates = generateSequence(begin) { it.plusDays(1) }.takeWhile { !it.isAfter(to) }.toList()
            val rows = dates.map { d ->
                val m = usageSource.simulate(pkgs, d)
                DailyUsageEntity(s.id, d, m, if (m == null) UsageStatus.NO_RECORD else UsageStatus.RECORDED, UsageSourceKind.AUTO)
            }
            db.usage().insertUsage(rows)
        }
    }

    /** 데모 DB를 처음 상태로 되돌립니다 (F12-2) */
    suspend fun resetToSeed() {
        withContext(Dispatchers.IO) { db.clearAllTables() }
        seedIfEmpty()
    }

    // ---- 휴지통 (F9) ----
    val trash: Flow<List<Subscription>> = db.subscriptions().observeTrash().map { l -> l.map { it.toDomain() } }
    suspend fun restore(id: Long) { db.subscriptions().restore(id); scheduler.reschedule() }

    /** 완전 삭제: 사용 기록·수정 내용·연결 앱까지 함께 지웁니다 (F9-2) */
    suspend fun deletePermanently(id: Long) {
        db.usage().deleteUsageOf(id); db.usage().deleteAdjustmentsOf(id); db.linkedApps().deleteOf(id)
        db.subscriptions().deleteById(id)
        scheduler.reschedule()
    }

    /** F9-5 30일이 지난 항목 자동 완전 삭제 (앱 실행 때) */
    suspend fun purgeExpired(today: LocalDate) {
        db.subscriptions().expiredIds(today.minusDays(com.timecost.demo.domain.usecase.TrashCalc.KEEP_DAYS)).forEach { deletePermanently(it) }
    }

    // ---- 결정 흐름 (F6) ----
    suspend fun loadTrialContext(id: Long): TrialContext? {
        val sub = db.subscriptions().byId(id)?.toDomain() ?: return null
        return TrialContext(sub, db.usage().usageOf(id).map { it.toDomain() }, db.usage().adjustmentsOf(id).map { it.toDomain() },
            db.linkedApps().of(id).map { LinkedApp(it.subscriptionId, it.packageName, it.appLabel) })
    }

    /**
     * F6-3 유지하기 확인: 정기 구독을 만들고, 체험 기록을 복사하며(원본은 그대로, source=TRIAL_CARRIED로 구분),
     * 체험 항목은 목록에서 뺍니다. 복사한 기록은 결제 기간 통계에 넣지 않습니다(상세에서 따로 표시).
     */
    suspend fun keepTrial(trialId: Long, n: NewSubscription, today: LocalDate): Long {
        val ctx = loadTrialContext(trialId)
        val newId = add(n, today)
        if (ctx != null) {
            db.usage().insertUsage(ctx.usage.map { DailyUsageEntity(newId, it.date, it.autoMinutes, it.status, UsageSourceKind.TRIAL_CARRIED) })
            db.usage().insertAdjustments(ctx.adjustments.map { UsageAdjustmentEntity(newId, it.date, it.type, it.minutes) })
        }
        db.subscriptions().markEnded(trialId)
        scheduler.reschedule()
        return newId
    }

    /** F6-5 해지 완료로 표시: 홈에서 사라지고 휴지통으로 이동 */
    suspend fun markCancelled(trialId: Long, today: LocalDate) { db.subscriptions().markCancelled(trialId, today); scheduler.reschedule() }

    // ---- 사용 시간 수정 (F5) ----
    suspend fun usageOn(id: Long, date: LocalDate): DailyUsage? = db.usage().usageOn(id, date)?.toDomain()
    suspend fun adjustmentOn(id: Long, date: LocalDate): UsageAdjustment? = db.usage().adjustmentsOf(id).firstOrNull { it.date == date }?.toDomain()
    suspend fun saveAdjustment(a: UsageAdjustment) = db.usage().insertAdjustment(UsageAdjustmentEntity(a.subscriptionId, a.date, a.type, a.minutes))
    suspend fun cancelAdjustment(id: Long, date: LocalDate) = db.usage().deleteAdjustment(id, date)
    suspend fun subscription(id: Long): Subscription? = db.subscriptions().byId(id)?.toDomain()
}
