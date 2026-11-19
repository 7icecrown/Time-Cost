package com.timecost.demo.ui

import androidx.lifecycle.ViewModel
import com.timecost.demo.data.settings.PermissionChecker
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** 앱 전체에서 쓰는 가벼운 상태. 시스템 설정에 다녀온 뒤 권한 안내를 다시 계산합니다. */
@HiltViewModel
class AppViewModel @Inject constructor(private val perms: PermissionChecker) : ViewModel() {
    fun onResume() = perms.refresh()
}
