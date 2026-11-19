package com.timecost.demo.data.settings

import android.content.Context
import com.timecost.demo.data.demo.DemoFlags
import com.timecost.demo.data.notify.Notifications
import com.timecost.demo.data.usage.RealUsageSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/** 홈·상세의 사용 시간 영역에만 보여주는 안내 (PRD 10장) */
enum class UsageNotice { NO_PERMISSION, READ_FAILED }

@Singleton
class PermissionChecker @Inject constructor(@ApplicationContext private val c: Context, private val flags: DemoFlags) {
    private val tick = MutableStateFlow(0)
    /** 시스템 설정에 다녀온 뒤 다시 읽도록 */
    fun refresh() { tick.value++ }

    /** 데모 DB에서는 가상 데이터를 쓰므로 기본 '허용됨', 시연 도구로 '권한 없음'을 재현 */
    fun usageAccessGranted(): Boolean = if (flags.active) !flags.noPermission.value else RealUsageSource(c).hasAccess()
    fun notificationsEnabled(): Boolean = Notifications.enabled(c)

    fun noticeFlow(): Flow<UsageNotice?> = combine(flags.noPermission, flags.failUsage, tick) { _, fail, _ ->
        when {
            !usageAccessGranted() -> UsageNotice.NO_PERMISSION
            flags.active && fail -> UsageNotice.READ_FAILED
            else -> null
        }
    }
}
