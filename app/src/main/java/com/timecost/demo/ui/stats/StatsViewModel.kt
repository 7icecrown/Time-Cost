package com.timecost.demo.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timecost.demo.core.AppClock
import com.timecost.demo.core.Format
import com.timecost.demo.core.demoBanner
import com.timecost.demo.data.repo.Snapshot
import com.timecost.demo.data.repo.SubscriptionRepository
import com.timecost.demo.domain.model.SubType
import com.timecost.demo.domain.usecase.BillingCalc
import com.timecost.demo.domain.usecase.StatsCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ShareRow(val name: String, val amount: String, val percent: Int?, val fraction: Float)
data class CostRow(val name: String, val headline: String, val detail: String?, val muted: Boolean)
data class NextUi(val name: String, val dateLabel: String, val amount: String, val daysLabel: String)
data class StatsUi(
    val banner: String? = null, val monthly: String = "0원", val yearly: String = "0원",
    val next: NextUi? = null, val shares: List<ShareRow> = emptyList(), val costs: List<CostRow> = emptyList(),
    val which: StatsCalc.Which = StatsCalc.Which.CURRENT,
)

@HiltViewModel
class StatsViewModel @Inject constructor(repo: SubscriptionRepository, private val clock: AppClock) : ViewModel() {
    private val which = MutableStateFlow(StatsCalc.Which.CURRENT)

    /** 기준일 이동(F8-5)과 구독·기록 변경에 모두 반응 */
    val ui: StateFlow<StatsUi> = combine(repo.snapshot, which, clock.offsetFlow) { s, w, _ -> build(s, w) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, StatsUi())

    fun setWhich(w: StatsCalc.Which) { which.value = w }

    private fun build(s: Snapshot, w: StatsCalc.Which): StatsUi {
        val today = clock.today()
        val subs = s.subscriptions.filter { it.type == SubType.RECURRING }
        val total = StatsCalc.monthlyTotal(subs)
        val shares = StatsCalc.shares(subs)
        val maxMonthly = shares.maxOfOrNull { it.monthly }?.coerceAtLeast(1) ?: 1
        return StatsUi(
            banner = clock.demoBanner(), monthly = Format.won(total), yearly = Format.won(BillingCalc.yearly(total)), which = w,
            next = StatsCalc.nextPayment(subs, today)?.let {
                NextUi(it.sub.name, Format.monthDay(it.date), Format.won(it.sub.price), if (it.daysLeft == 0L) "오늘" else "${it.daysLeft}일 뒤")
            },
            shares = shares.map { ShareRow(it.name, if (it.free) "무료" else Format.won(it.monthly), if (it.free) null else it.percent, it.monthly.toFloat() / maxMonthly) },
            costs = StatsCalc.hourCosts(subs, s.usage, s.adjustments, today, w).map { c ->
                when (c.state) {
                    StatsCalc.CostState.VALUE -> CostRow(c.name, Format.won(c.cost!!),
                        "${Format.won(c.price)} · ${Format.duration(c.minutes)}" + if (c.noRecordDays > 0) " (기록 없는 날 ${c.noRecordDays}일)" else "", false)
                    StatsCalc.CostState.NO_TRACKING -> CostRow(c.name, "사용 시간 기록 없음", null, true)
                    StatsCalc.CostState.FREE -> CostRow(c.name, "무료", null, true)
                    StatsCalc.CostState.NO_PERIOD -> CostRow(c.name, "지난 결제 기간이 아직 없어요", null, true)
                    StatsCalc.CostState.NO_USAGE -> CostRow(c.name, "사용 시간이 없어요", "${Format.won(c.price)} 냈어요", true)
                }
            })
    }
}
