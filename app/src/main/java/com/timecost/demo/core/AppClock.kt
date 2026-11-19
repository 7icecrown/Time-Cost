package com.timecost.demo.core

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/**
 * 모든 날짜 계산은 이 시계만 씁니다. 데모의 기준일 이동은 offset만 바꾸고, 화면은 offsetFlow를 구독해 다시 계산합니다. (PRD 9장)
 * [demo]는 지금 데모 DB로 동작 중인지(데모 빌드에서 데모 모드를 켠 상태)를 뜻합니다.
 */
class AppClock(val demo: Boolean = false, private val base: () -> LocalDate = { LocalDate.now() }) {
    private val offset = MutableStateFlow(0)
    val offsetFlow: StateFlow<Int> get() = offset
    var offsetDays: Int
        get() = offset.value
        set(v) { offset.value = v }
    fun today(): LocalDate = base().plusDays(offset.value.toLong())
}

/** 'F1-6 데모 · 기준일 …' 배너 문구. 데모 DB가 아니면 null */
fun AppClock.demoBanner(): String? {
    if (!demo) return null
    val d = today()
    return "데모 · 기준일 ${d.year}년 ${d.monthValue}월 ${d.dayOfMonth}일 (+${offsetDays}일)"
}
