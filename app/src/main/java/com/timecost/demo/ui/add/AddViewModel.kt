package com.timecost.demo.ui.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timecost.demo.core.AppClock
import com.timecost.demo.data.repo.HomeEvents
import com.timecost.demo.data.repo.SubscriptionRepository
import com.timecost.demo.domain.usecase.TrialDecision
import com.timecost.demo.core.Format
import com.timecost.demo.data.usage.InstalledAppsSource
import com.timecost.demo.domain.model.*
import com.timecost.demo.domain.usecase.BillingCalc
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** F2-2 서비스 타일. 누르면 이름과 연결 앱이 채워집니다. */
data class ServiceTile(val key: String, val label: String, val initial: String, val app: InstalledApp?)

val SERVICE_TILES = listOf(
    ServiceTile("netflix", "Netflix", "N", InstalledApp("com.netflix.mediaclient", "Netflix")),
    ServiceTile("youtube", "YouTube", "Y", InstalledApp("com.google.android.youtube", "YouTube")),
    ServiceTile("wavve", "Wavve", "W", InstalledApp("kr.co.captv.pooq", "Wavve")),
    ServiceTile("disney", "Disney+", "D", InstalledApp("com.disney.disneyplus", "Disney+")),
    ServiceTile("tving", "티빙", "T", InstalledApp("net.cj.cjhv.gs.tving", "티빙")),
    ServiceTile("custom", "직접 입력", "+", null),
)

enum class StartOption { TODAY, OTHER }

data class AddForm(
    val type: SubType = SubType.RECURRING,
    val tileKey: String? = null,
    val name: String = "",
    val priceText: String = "",
    val free: Boolean = false,
    val cycleMonths: Int = 1,
    val customCycle: Boolean = false,
    val customCycleText: String = "",
    val startOption: StartOption = StartOption.TODAY,
    val startDate: LocalDate? = null,          // OTHER일 때 선택한 날짜
    val endDate: LocalDate? = null,            // 무료 체험·할인
    val afterPriceText: String = "",
    val afterCycleMonths: Int = 1,
    val tracking: TrackingMode = TrackingMode.AUTO,
    val apps: List<InstalledApp> = emptyList(),
    val cancelMemo: String = "",
    val notify: Boolean = true,
    val moreOpen: Boolean = false,
)

data class AddPreview(val line1: String, val line2: String?)

@HiltViewModel
class AddViewModel @Inject constructor(
    private val repo: SubscriptionRepository, private val clock: AppClock, private val installed: InstalledAppsSource,
    private val events: HomeEvents,
) : ViewModel() {
    private val _form = MutableStateFlow(AddForm())
    val form: StateFlow<AddForm> = _form
    val installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    /** 이미 다른 구독에 연결된 앱 (F11-2: 중복 연결은 허용하되 표시) */
    val trackedElsewhere: StateFlow<Set<String>> = repo.snapshot.map { s -> s.linkedApps.map { it.packageName }.toSet() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    init { viewModelScope.launch { installedApps.value = installed.list() } }

    /** 유지하기 → 수정: 체험·할인 id. 있으면 저장 시 체험 기록을 복사하며 정기 구독으로 등록 (F6-4) */
    var keepTrialId: Long? = null
        private set

    /** F6-4 값이 채워진 정기 구독 입력 폼. 뒤로 가면 아무것도 바뀌지 않습니다. */
    fun prefillFromTrial(id: Long) {
        keepTrialId = id
        viewModelScope.launch {
            val ctx = repo.loadTrialContext(id) ?: return@launch
            val d = TrialDecision.keepDraft(ctx.sub, ctx.apps, ctx.usage, ctx.adjustments)
            val preset = d.cycleMonths in listOf(1, 3, 12)
            _form.value = AddForm(
                type = SubType.RECURRING, tileKey = "custom", name = d.name,
                priceText = d.price?.let { formatDigits(it.toString()) } ?: "",
                cycleMonths = if (preset) d.cycleMonths else 1, customCycle = !preset,
                customCycleText = if (preset) "" else d.cycleMonths.toString(),
                startOption = StartOption.OTHER, startDate = d.start, tracking = d.tracking, apps = d.apps)
        }
    }

    fun today(): LocalDate = clock.today()
    fun update(f: (AddForm) -> AddForm) = _form.update(f)

    fun pickTile(t: ServiceTile) = update {
        if (t.key == "custom") it.copy(tileKey = t.key, name = if (it.tileKey == "custom") it.name else "")
        else it.copy(tileKey = t.key, name = t.label, apps = listOfNotNull(t.app))
    }

    private fun parseWon(s: String): Long? = s.filter(Char::isDigit).takeIf { it.isNotEmpty() }?.toLongOrNull()
    private fun f() = _form.value
    private fun price(): Long = if (f().free) 0 else parseWon(f().priceText) ?: 0
    private fun cycle(): Int = if (f().customCycle) f().customCycleText.toIntOrNull()?.coerceIn(1, 120) ?: 0 else f().cycleMonths
    fun start(): LocalDate = if (f().startOption == StartOption.TODAY) today() else f().startDate ?: today()

    val canSave: StateFlow<Boolean> = _form.map { form ->
        val nameOk = form.name.isNotBlank()
        val cycleOk = (if (form.customCycle) form.customCycleText.toIntOrNull()?.let { it in 1..120 } == true else true)
        when (form.type) {
            SubType.RECURRING -> nameOk && cycleOk && (form.free || (parseWon(form.priceText) ?: 0) > 0)
            SubType.TRIAL_OR_PROMO -> nameOk && cycleOk && form.endDate != null && !form.endDate.isBefore(start())
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    /** F2-7·F3-6 미리보기 */
    fun preview(form: AddForm): AddPreview? {
        val today = clock.today()
        val c = if (form.customCycle) form.customCycleText.toIntOrNull()?.coerceIn(1, 120) ?: return null else form.cycleMonths
        val p = if (form.free) 0L else parseWon(form.priceText) ?: 0L
        val start = if (form.startOption == StartOption.TODAY) today else form.startDate ?: today
        return when (form.type) {
            SubType.RECURRING -> {
                val next = BillingCalc.nextPaymentDate(start, c, today)
                AddPreview("다음 결제일 ${com.timecost.demo.core.Format.monthDay(next)} (${com.timecost.demo.core.Format.dDay(BillingCalc.daysUntil(next, today))})",
                    "한 달 구독료 ${com.timecost.demo.core.Format.won(BillingCalc.monthlyPrice(p, c))}")
            }
            SubType.TRIAL_OR_PROMO -> {
                val end = form.endDate ?: return null
                val ap = parseWon(form.afterPriceText)
                AddPreview("종료까지 ${com.timecost.demo.core.Format.dDay(BillingCalc.daysUntil(end, today))}",
                    ap?.let { "종료 후 한 달 구독료 ${com.timecost.demo.core.Format.won(BillingCalc.monthlyPrice(it, form.afterCycleMonths))}" })
            }
        }
    }

    fun save(onDone: () -> Unit) {
        val form = f(); if (!canSave.value) return
        viewModelScope.launch {
            val trial = form.type == SubType.TRIAL_OR_PROMO
            val n = NewSubscription(
                name = form.name.trim(), type = form.type, price = price(), cycleMonths = cycle(), startDate = start(),
                endDate = if (trial) form.endDate else null,
                afterPrice = if (trial) parseWon(form.afterPriceText) else null,
                afterCycleMonths = if (trial && parseWon(form.afterPriceText) != null) form.afterCycleMonths else null,
                trackingMode = form.tracking, cancelMemo = if (trial) form.cancelMemo.take(500) else "",
                notifyEnabled = trial && form.notify, apps = if (form.tracking == TrackingMode.AUTO) form.apps else emptyList())
            val id = keepTrialId?.let { repo.keepTrial(it, n, clock.today()) } ?: repo.add(n, clock.today())
            // 등록 완료 홈: 새 구독 하이라이트와 안내 문구
            events.highlightId.value = id
            events.message.value = "${n.name}${Format.eulReul(n.name)} " + if (trial) "무료 체험·할인으로 등록했어요" else "정기 구독으로 등록했어요"
            onDone()
        }
    }
}

internal fun formatDigits(raw: String): String {
    val d = raw.filter(Char::isDigit).take(9).trimStart('0')
    return if (d.isEmpty()) "" else java.text.NumberFormat.getIntegerInstance(java.util.Locale.KOREA).format(d.toLong())
}
