package com.timecost.demo.core

import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

object Format {
    private val nf = NumberFormat.getIntegerInstance(Locale.KOREA)
    fun won(v: Long) = "${nf.format(v)}원"
    fun duration(minutes: Int): String {
        val h = minutes / 60; val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}시간 ${m}분"
            h > 0 -> "${h}시간"
            else -> "${m}분"
        }
    }
    fun dDay(days: Long) = if (days == 0L) "D-day" else "D-$days"
    fun monthDay(d: LocalDate) = "${d.monthValue}월 ${d.dayOfMonth}일"
    /** 받침에 따라 을/를. 한글이 아니면 '를' */
    fun eulReul(name: String): String {
        val c = name.trim().lastOrNull() ?: return "를"
        return if (c in '가'..'힣' && (c - '가') % 28 != 0) "을" else "를"
    }
    fun weekday(d: LocalDate): String = d.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.KOREAN)
}

/** '오전 9:00' / '오후 12:00' (시안 문구) */
fun Format.timeLabel(t: java.time.LocalTime): String {
    val h = if (t.hour % 12 == 0) 12 else t.hour % 12
    return "${if (t.hour < 12) "오전" else "오후"} $h:${"%02d".format(t.minute)}"
}
