package com.timecost.demo.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timecost.demo.core.AppClock
import com.timecost.demo.core.demoBanner
import com.timecost.demo.data.notify.AlertScheduler
import com.timecost.demo.data.settings.PermissionChecker
import com.timecost.demo.data.settings.SettingsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

data class SettingsUi(
    val banner: String? = null, val notifyTime: LocalTime = LocalTime.of(9, 0),
    val usageAccess: Boolean = true, val notifications: Boolean = true,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsStore, private val scheduler: AlertScheduler,
    private val perms: PermissionChecker, private val clock: AppClock,
) : ViewModel() {
    private val permState = MutableStateFlow(perms.usageAccessGranted() to perms.notificationsEnabled())

    val ui: StateFlow<SettingsUi> = combine(settings.notifyTime, permState, clock.offsetFlow) { t, p, _ ->
        SettingsUi(clock.demoBanner(), t, p.first, p.second)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUi())

    /** 시스템 설정에서 돌아오면 권한 상태를 다시 읽습니다 */
    fun refreshPermissions() { permState.value = perms.usageAccessGranted() to perms.notificationsEnabled() }

    /** F10-1 저장하면 예약된 알림을 모두 새 시각으로 다시 맞춥니다 */
    fun saveTime(t: LocalTime) = viewModelScope.launch { settings.setNotifyTime(t); scheduler.reschedule() }
}
