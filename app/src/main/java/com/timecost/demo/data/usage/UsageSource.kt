package com.timecost.demo.data.usage

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Process
import com.timecost.demo.domain.model.InstalledApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId

/** 일 단위 앱 사용 시간 읽기. 읽지 못한 날은 null → 호출부가 NO_RECORD로 저장 (추정 금지, PRD 2장). */
interface UsageSource {
    suspend fun minutesFor(packages: Set<String>, date: LocalDate): Int?
    /** 시연에서 기준일을 앞으로 옮길 때, 그 사이 날들의 가상 사용 기록을 만듭니다. 실제 소스는 읽기와 같습니다. */
    suspend fun simulate(packages: Set<String>, date: LocalDate): Int? = minutesFor(packages, date)
}

/**
 * 데모용: 날짜·패키지로 결정되는 값(0~80분)을 돌려주고, 시스템의 7일 보관 한계처럼 오래된 날은 읽지 못함(null) 처리.
 * 시연 도구의 '조회 실패'·'권한 없음'이 켜져 있으면 읽지 못함(null).
 */
class FakeUsageSource(
    private val clock: com.timecost.demo.core.AppClock, private val flags: com.timecost.demo.data.demo.DemoFlags,
) : UsageSource {
    private fun synth(packages: Set<String>, date: LocalDate): Int =
        packages.sumOf { (((it.hashCode() xor date.toEpochDay().toInt()) and 0x7fffffff) % 5) * 20 }

    override suspend fun minutesFor(packages: Set<String>, date: LocalDate): Int? {
        if (packages.isEmpty() || flags.failUsage.value || flags.noPermission.value) return null
        if (date.isBefore(clock.today().minusDays(7))) return null
        return synth(packages, date)
    }
    override suspend fun simulate(packages: Set<String>, date: LocalDate): Int? =
        if (packages.isEmpty()) null else synth(packages, date)
}

class RealUsageSource(private val context: Context) : UsageSource {
    @Suppress("DEPRECATION")
    fun hasAccess(): Boolean {
        val ops = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override suspend fun minutesFor(packages: Set<String>, date: LocalDate): Int? = withContext(Dispatchers.IO) {
        if (packages.isEmpty() || !hasAccess()) return@withContext null
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val stats = runCatching { usm.queryAndAggregateUsageStats(start, end) }.getOrNull() ?: return@withContext null
        val ms = packages.sumOf { stats[it]?.totalTimeInForeground ?: 0L }
        // 시스템은 과거 일 단위 기록을 약 7일만 보관합니다. 0이고 오래된 날이면 '읽지 못함'으로 봅니다.
        if (ms == 0L && date.isBefore(LocalDate.now().minusDays(7))) null else (ms / 60_000).toInt()
    }
}

/** 설치된 앱 목록 (런처에 보이는 앱). F11 */
interface InstalledAppsSource { suspend fun list(): List<InstalledApp> }

class RealInstalledAppsSource(private val context: Context) : InstalledAppsSource {
    override suspend fun list(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, 0)
            .map { InstalledApp(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }
}

/** 데모 모드의 가상 앱 목록 (F11-4) */
class FakeInstalledAppsSource : InstalledAppsSource {
    override suspend fun list() = listOf(
        "Coupang Play" to "com.coupang.mobile.play", "Disney+" to "com.disney.disneyplus",
        "Melon" to "com.iloen.melon", "Netflix" to "com.netflix.mediaclient", "Notion" to "notion.id",
        "Spotify" to "com.spotify.music", "Wavve" to "kr.co.captv.pooq",
        "YouTube" to "com.google.android.youtube", "YouTube Music" to "com.google.android.apps.youtube.music",
        "티빙" to "net.cj.cjhv.gs.tving",
    ).map { InstalledApp(it.second, it.first) }
}
