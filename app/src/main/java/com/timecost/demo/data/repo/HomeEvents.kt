package com.timecost.demo.data.repo

import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** 등록 완료 후 홈에서 새 구독 하이라이트와 안내 문구를 보여주기 위한 일회성 이벤트 */
@Singleton
class HomeEvents @Inject constructor() {
    val highlightId = MutableStateFlow<Long?>(null)
    val message = MutableStateFlow<String?>(null)
}
