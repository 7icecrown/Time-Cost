package com.timecost.demo.data.repo

import com.timecost.demo.data.db.*
import com.timecost.demo.domain.model.*
import java.time.LocalDate

/** PRD 11장 시드 데이터 (기준일 2026-11-10). */
object DemoSeed {
    val BASE_DATE: LocalDate = LocalDate.of(2026, 11, 10)
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)

    private fun sub(
        name: String, type: SubType, price: Long, cycle: Int, start: LocalDate,
        end: LocalDate? = null, afterPrice: Long? = null, afterCycle: Int? = null,
        tracking: TrackingMode = TrackingMode.AUTO, deletedAt: LocalDate? = null, reason: String? = null,
    ) = SubscriptionEntity(
        name = name, type = type, price = price, cycleMonths = cycle, startDate = start, endDate = end,
        afterPrice = afterPrice, afterCycleMonths = afterCycle, trackingMode = tracking, cancelMemo = "",
        notifyEnabled = name.startsWith("Apple") || name.startsWith("Disney"), status = SubStatus.ACTIVE,
        deletedAt = deletedAt, deleteReason = reason,
    )

    // id는 1부터 삽입 순서대로 부여됩니다.
    val subscriptions = listOf(
        sub("YouTube Premium", SubType.RECURRING, 14_900, 1, d(10, 9)),                 // 1
        sub("Netflix", SubType.RECURRING, 13_500, 1, d(10, 13)),                        // 2
        sub("밀리의 서재", SubType.RECURRING, 29_700, 3, d(9, 25)),                       // 3
        sub("Notion", SubType.RECURRING, 99_000, 12, d(2, 14), tracking = TrackingMode.OFF), // 4
        sub("Duolingo", SubType.RECURRING, 0, 1, d(10, 20)),                            // 5
        sub("Disney+ 무료 체험", SubType.TRIAL_OR_PROMO, 0, 1, d(11, 1), d(11, 7), 9_500, 1), // 6
        sub("Apple Music 무료 체험", SubType.TRIAL_OR_PROMO, 0, 1, d(10, 29), d(11, 12)),      // 7
        sub("Wavve 할인", SubType.TRIAL_OR_PROMO, 2_900, 1, d(10, 15), d(11, 15)),          // 8
        sub("Spotify", SubType.RECURRING, 10_900, 1, d(8, 1),
            deletedAt = BASE_DATE.minusDays(7), reason = "user"),                       // 9 (휴지통, 23일 남음)
        sub("Coupang Play 체험", SubType.TRIAL_OR_PROMO, 0, 1, d(10, 1), d(10, 31),
            deletedAt = BASE_DATE.minusDays(27), reason = "user"),                      // 10 (휴지통, 3일 남음)
    )

    private fun u(id: Long, date: LocalDate, min: Int?) = DailyUsageEntity(
        id, date, min, if (min == null) UsageStatus.NO_RECORD else UsageStatus.RECORDED, UsageSourceKind.AUTO)

    val usage = listOf(
        u(1, d(10, 20), 90), u(1, d(11, 7), null), u(1, d(11, 8), 50),               // 지난 기간: 11/7 기록 없음, 11/8 자동 50분 (시안 Adj1·Adj2)
        u(1, d(11, 9), 140), u(1, d(11, 10), 80),                                    // 이번 기간 3시간 40분, 오늘 자동 1시간 20분 (시안 Adj3)
        u(2, d(10, 14), 120), u(2, d(10, 20), null), u(2, d(10, 21), 120),
        u(2, d(10, 27), null), u(2, d(10, 28), 120), u(2, d(11, 5), 120),            // 8시간, 기록 없는 날 2일
        u(3, d(10, 5), 120), u(3, d(10, 20), 130), u(3, d(11, 8), 120),              // 6시간 10분
        u(5, d(10, 25), 190), u(5, d(11, 2), 190), u(5, d(11, 9), 190),              // 9시간 30분
        u(6, d(11, 1), 60), u(6, d(11, 2), 60), u(6, d(11, 3), 60), u(6, d(11, 4), 60),
        u(6, d(11, 5), 60), u(6, d(11, 6), 90), u(6, d(11, 7), null),                // 6시간 30분, 기록 없는 날 1일
    )

    /** 시드 구독 이름과 가상 앱 연결 (F11-4 목록과 같은 패키지명) */
    fun appFor(subName: String): com.timecost.demo.domain.model.InstalledApp? = when {
        subName.startsWith("YouTube") -> com.timecost.demo.domain.model.InstalledApp("com.google.android.youtube", "YouTube")
        subName.startsWith("Netflix") -> com.timecost.demo.domain.model.InstalledApp("com.netflix.mediaclient", "Netflix")
        subName.startsWith("Disney") -> com.timecost.demo.domain.model.InstalledApp("com.disney.disneyplus", "Disney+")
        subName.startsWith("Wavve") -> com.timecost.demo.domain.model.InstalledApp("kr.co.captv.pooq", "Wavve")
        subName.startsWith("Spotify") -> com.timecost.demo.domain.model.InstalledApp("com.spotify.music", "Spotify")
        else -> null
    }

    private fun at(m: Int, day: Int) = LocalDate.of(2026, m, day).atTime(12, 0)
        .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    /** 시안 '계산 기록' 4건: Notion 2,475원 / 직접 입력 2,000원 / 헬스장 4,583원 / Disney+ 체험(무료) */
    val calcRecords = listOf(
        CalcRecordEntity(name = "Notion", price = 99_000, minutes = 40 * 60, createdAt = at(11, 8)),
        CalcRecordEntity(name = null, price = 17_000, minutes = 8 * 60 + 30, createdAt = at(11, 3)),
        CalcRecordEntity(name = "헬스장", price = 55_000, minutes = 12 * 60, createdAt = at(10, 28)),
        CalcRecordEntity(name = "Disney+ 체험", price = 0, minutes = 3 * 60, createdAt = at(10, 20)),
    )
}
