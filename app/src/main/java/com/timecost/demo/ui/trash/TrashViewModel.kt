package com.timecost.demo.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timecost.demo.core.AppClock
import com.timecost.demo.core.Format
import com.timecost.demo.data.repo.SubscriptionRepository
import com.timecost.demo.domain.model.SubType
import com.timecost.demo.domain.model.Subscription
import com.timecost.demo.domain.usecase.TrashCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TrashItem(
    val id: Long, val name: String, val kind: String, val remaining: String, val deletedLabel: String,
    /** F9-3 종료일이 지난 체험·할인은 복구하면 바로 결정 대기 */
    val restoreNote: String?,
)

@HiltViewModel
class TrashViewModel @Inject constructor(private val repo: SubscriptionRepository, private val clock: AppClock) : ViewModel() {
    val items: StateFlow<List<TrashItem>> = combine(repo.trash, clock.offsetFlow) { l, _ -> l.map(::toItem) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init { viewModelScope.launch { repo.purgeExpired(clock.today()) } }   // F9-5

    fun restore(id: Long) = viewModelScope.launch { repo.restore(id) }
    fun deletePermanently(id: Long) = viewModelScope.launch { repo.deletePermanently(id) }

    private fun toItem(s: Subscription): TrashItem {
        val today = clock.today(); val del = s.deletedAt ?: today
        val cancelled = s.deleteReason == "CANCELLED"
        return TrashItem(
            id = s.id, name = s.name,
            kind = if (s.type == SubType.RECURRING) "정기 구독" else "체험·할인" + if (cancelled) " · 해지 완료로 표시" else "",
            remaining = "${TrashCalc.remainingDays(del, today)}일 남음", deletedLabel = "${Format.monthDay(del)} 삭제",
            restoreNote = if (TrashCalc.restoreGoesToDecision(s, today)) "복구하면 종료일이 지나 바로 결정 대기가 돼요." else null)
    }
}
