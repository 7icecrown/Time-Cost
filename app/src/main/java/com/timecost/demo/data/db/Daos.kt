package com.timecost.demo.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscription WHERE deletedAt IS NULL AND status != 'ENDED' ORDER BY id")
    fun observeActive(): Flow<List<SubscriptionEntity>>
    @Query("SELECT COUNT(*) FROM subscription WHERE deletedAt IS NOT NULL")
    fun observeTrashCount(): Flow<Int>
    @Query("SELECT COUNT(*) FROM subscription") suspend fun count(): Int
    @Query("SELECT * FROM subscription WHERE id = :id") suspend fun byId(id: Long): SubscriptionEntity?
    @Query("SELECT * FROM subscription WHERE deletedAt IS NULL AND status != 'ENDED'") suspend fun activeList(): List<SubscriptionEntity>
    @Query("SELECT * FROM subscription WHERE type = 'TRIAL_OR_PROMO' AND notifyEnabled = 1 AND deletedAt IS NULL AND status != 'ENDED'")
    suspend fun alertTrials(): List<SubscriptionEntity>

    @Query("UPDATE subscription SET deletedAt = :date, deleteReason = 'user' WHERE id = :id")
    suspend fun softDelete(id: Long, date: LocalDate)
    /** 유지하기로 결정한 체험: 목록에서만 빠지고 휴지통에는 가지 않습니다. 원본 기록은 그대로 남습니다. */
    @Query("UPDATE subscription SET status = 'ENDED' WHERE id = :id") suspend fun markEnded(id: Long)
    /** '해지 완료로 표시': 홈에서 사라지고 휴지통으로 이동 (F6-5) */
    @Query("UPDATE subscription SET status = 'ENDED', deletedAt = :date, deleteReason = 'CANCELLED' WHERE id = :id")
    suspend fun markCancelled(id: Long, date: LocalDate)

    @Query("SELECT * FROM subscription WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<SubscriptionEntity>>
    /** 복구: 해지 완료로 표시했던 항목도 다시 활성으로. 종료일이 지난 체험은 effectiveStatus가 '결정 대기'로 보여 줌 (F9-3) */
    @Query("UPDATE subscription SET deletedAt = NULL, deleteReason = NULL, status = 'ACTIVE' WHERE id = :id")
    suspend fun restore(id: Long)
    @Query("DELETE FROM subscription WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("SELECT id FROM subscription WHERE deletedAt IS NOT NULL AND deletedAt <= :cutoff")
    suspend fun expiredIds(cutoff: LocalDate): List<Long>

    @Insert suspend fun insert(e: SubscriptionEntity): Long
    @Insert suspend fun insertAll(list: List<SubscriptionEntity>): List<Long>
}

@Dao
interface LinkedAppDao {
    @Query("SELECT * FROM linked_app") fun observeAll(): Flow<List<LinkedAppEntity>>
    @Query("SELECT * FROM linked_app WHERE subscriptionId = :id") suspend fun of(id: Long): List<LinkedAppEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<LinkedAppEntity>)
    @Query("DELETE FROM linked_app WHERE subscriptionId = :id") suspend fun deleteOf(id: Long)
}

@Dao
interface UsageDao {
    @Query("SELECT * FROM daily_usage") fun observeUsage(): Flow<List<DailyUsageEntity>>
    @Query("SELECT * FROM usage_adjustment") fun observeAdjustments(): Flow<List<UsageAdjustmentEntity>>
    @Query("SELECT * FROM daily_usage WHERE subscriptionId = :id AND source = 'AUTO'") suspend fun usageOf(id: Long): List<DailyUsageEntity>
    @Query("SELECT * FROM daily_usage WHERE subscriptionId = :id AND date = :date AND source = 'AUTO'")
    suspend fun usageOn(id: Long, date: LocalDate): DailyUsageEntity?
    @Query("SELECT * FROM usage_adjustment WHERE subscriptionId = :id") suspend fun adjustmentsOf(id: Long): List<UsageAdjustmentEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertUsage(list: List<DailyUsageEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAdjustments(list: List<UsageAdjustmentEntity>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAdjustment(e: UsageAdjustmentEntity)
    @Query("DELETE FROM usage_adjustment WHERE subscriptionId = :id AND date = :date") suspend fun deleteAdjustment(id: Long, date: LocalDate)
    @Query("DELETE FROM daily_usage WHERE subscriptionId = :id") suspend fun deleteUsageOf(id: Long)
    @Query("DELETE FROM usage_adjustment WHERE subscriptionId = :id") suspend fun deleteAdjustmentsOf(id: Long)
}

@Dao
interface CalcDao {
    @Query("SELECT * FROM calc_record ORDER BY createdAt DESC LIMIT 50") fun observeAll(): Flow<List<CalcRecordEntity>>
    @Insert suspend fun insert(e: CalcRecordEntity): Long
    @Insert suspend fun insertAll(list: List<CalcRecordEntity>)
}

@Database(
    entities = [SubscriptionEntity::class, LinkedAppEntity::class, DailyUsageEntity::class,
        UsageAdjustmentEntity::class, CalcRecordEntity::class],
    version = 1, exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun subscriptions(): SubscriptionDao
    abstract fun linkedApps(): LinkedAppDao
    abstract fun usage(): UsageDao
    abstract fun calc(): CalcDao
}
