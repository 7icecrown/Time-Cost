package com.timecost.demo.ui.add

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecost.demo.core.Format
import com.timecost.demo.domain.model.SubType
import com.timecost.demo.domain.model.TrackingMode
import com.timecost.demo.ui.common.*
import com.timecost.demo.ui.theme.TC
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

@Composable
fun AddScreen(vm: AddViewModel, onBack: () -> Unit, onPickApps: () -> Unit, onSaved: () -> Unit, keepMode: Boolean = false) {
    val form by vm.form.collectAsState()
    val canSave by vm.canSave.collectAsState()
    val preview = vm.preview(form)
    val trial = form.type == SubType.TRIAL_OR_PROMO
    var datePicker by remember { mutableStateOf<String?>(null) }   // "start" | "end"

    Column(Modifier.fillMaxSize().background(TC.Bg)) {
        TopBarBack(if (keepMode) "정기 구독으로 등록" else "구독제 추가", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {

            // F2-1 유형 선택 (유지하기 → 수정에서는 정기 구독으로 고정)
            if (!keepMode) {
            Row(Modifier.fillMaxWidth().background(Color(0xFFECECE8), RoundedCornerShape(12.dp)).padding(3.dp)) {
                listOf(SubType.RECURRING to "정기 구독", SubType.TRIAL_OR_PROMO to "무료 체험·할인").forEach { (t, label) ->
                    val sel = form.type == t
                    Button(onClick = { vm.update { it.copy(type = t) } }, shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = if (sel) TC.Primary else Color.Transparent,
                            contentColor = if (sel) Color.White else TC.Text), elevation = null) {
                        Text(label, fontSize = 14.sp, fontWeight = if (sel) FontWeight.SemiBold else FontWeight.Medium)
                    }
                }
            }
            }

            // F2-2 서비스 타일
            Column {
                if (!keepMode) SectionLabel("어떤 구독인가요?")
                if (!keepMode) SERVICE_TILES.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { t -> ServiceTileButton(t, form.tileKey == t.key, Modifier.weight(1f)) { vm.pickTile(t) } }
                    }
                }
                if (form.tileKey == "custom" || trial || keepMode) {
                    OutlinedTextField(form.name, { v -> vm.update { it.copy(name = v) } }, singleLine = true,
                        label = { Text("이름") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            }

            // F2-3 구독료 (필수) — 무료 체험·할인은 '기간 중 구독료'
            Column {
                SectionLabel(if (trial) "기간 중 구독료와 결제 주기" else "구독료 (결제할 때마다 내는 금액)")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = form.priceText, onValueChange = { v -> vm.update { it.copy(priceText = formatDigits(v)) } },
                        enabled = !form.free, singleLine = true, suffix = { Text("원") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                        label = { Text(if (trial) "구독료 (없으면 비워 두세요)" else "구독료") })
                    Text("무료", fontSize = 14.sp)
                    Switch(checked = form.free, onCheckedChange = { c -> vm.update { it.copy(free = c) } },
                        modifier = Modifier.semantics { contentDescription = "무료" })
                }
                if (!trial) HelpText("요금제에 맞게 바꿔 주세요.")
            }

            // F2-4 결제 주기
            Column {
                SectionLabel(if (trial) "기간 중 결제 주기" else "얼마나 자주 결제되나요?")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "매월", 3 to "3개월", 12 to "매년").forEach { (m, label) ->
                        PillChip(label, !form.customCycle && form.cycleMonths == m) { vm.update { it.copy(cycleMonths = m, customCycle = false) } }
                    }
                    PillChip("직접 입력", form.customCycle) { vm.update { it.copy(customCycle = true) } }
                }
                if (form.customCycle) {
                    OutlinedTextField(form.customCycleText, { v -> vm.update { it.copy(customCycleText = v.filter(Char::isDigit).take(3)) } },
                        singleLine = true, suffix = { Text("개월마다") }, label = { Text("개월 수 (1~120)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.padding(top = 8.dp).fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
            }

            // F2-5 시작일
            Column {
                SectionLabel("언제부터 쓰나요?")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    PillChip("오늘부터", form.startOption == StartOption.TODAY) { vm.update { it.copy(startOption = StartOption.TODAY) } }
                    PillChip(if (form.startOption == StartOption.OTHER && form.startDate != null) formatFull(form.startDate!!) else "다른 날짜",
                        form.startOption == StartOption.OTHER) {
                        vm.update { it.copy(startOption = StartOption.OTHER, startDate = it.startDate ?: vm.today()) }
                        datePicker = "start"
                    }
                }
                HelpText("예전부터 쓰던 구독이면 ‘다른 날짜’를 눌러 주세요. 그 전 사용 시간은 기록이 없어요.")
            }

            // F3-1 종료일 / F3-2 종료 후 조건
            if (trial) {
                Column {
                    SectionLabel("언제 끝나나요?")
                    PillChip(form.endDate?.let(::formatFull) ?: "종료일 선택", form.endDate != null) { datePicker = "end" }
                    if (form.endDate != null && form.endDate!!.isBefore(vm.start())) HelpText("종료일은 시작일 이후여야 해요.")
                }
                Column {
                    SectionLabel("종료 후 조건 (선택)")
                    OutlinedTextField(form.afterPriceText, { v -> vm.update { it.copy(afterPriceText = formatDigits(v)) } },
                        singleLine = true, suffix = { Text("원") }, label = { Text("종료 후 구독료") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "매월", 3 to "3개월", 12 to "매년").forEach { (m, label) ->
                            PillChip(label, form.afterCycleMonths == m) { vm.update { it.copy(afterCycleMonths = m) } }
                        }
                    }
                    HelpText("비워도 저장돼요. 입력하면 결정할 때 한 달 구독료를 보여드려요.")
                }
                Column {
                    SectionLabel("해지 방법 메모 (선택)")
                    OutlinedTextField(form.cancelMemo, { v -> vm.update { it.copy(cancelMemo = v.take(500)) } },
                        minLines = 3, label = { Text("해지 방법이나 주소(URL)") }, supportingText = { Text("${form.cancelMemo.length}/500") },
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("종료 알림 받기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("종료 3일 전 · 1일 전 · 당일에 알려드려요.", fontSize = 12.sp, color = TC.Sub)
                    }
                    Switch(checked = form.notify, onCheckedChange = { c -> vm.update { it.copy(notify = c) } },
                        modifier = Modifier.semantics { contentDescription = "종료 알림 받기" })
                }
            }

            // F2-6 더 설정하기 (접힘). 접힌 상태에서도 현재 설정을 한 줄로 요약.
            Column(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(12.dp))
                .padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val summary = if (form.tracking == TrackingMode.OFF) "사용 시간 안 재요"
                else "자동으로 재요 · " + if (form.apps.isEmpty()) "연결된 앱 없음" else form.apps.joinToString(", ") { it.label }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("더 설정하기", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(summary, fontSize = 12.sp, color = TC.Sub, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    TextButton(onClick = { vm.update { it.copy(moreOpen = !it.moreOpen) } }, modifier = Modifier.defaultMinSize(48.dp, 48.dp)) {
                        Text(if (form.moreOpen) "접기" else "열기", color = TC.Text)
                    }
                }
                if (form.moreOpen) {
                    SectionLabel("사용 시간 기록")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillChip("자동으로 재요", form.tracking == TrackingMode.AUTO) { vm.update { it.copy(tracking = TrackingMode.AUTO) } }
                        PillChip("사용 시간 안 재요", form.tracking == TrackingMode.OFF) { vm.update { it.copy(tracking = TrackingMode.OFF) } }
                    }
                    if (form.tracking == TrackingMode.AUTO) {
                        OutlinedButton(onClick = onPickApps, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFD4D4CF))) {
                            Text(if (form.apps.isEmpty()) "연결된 앱 고르기" else "연결된 앱 바꾸기", color = TC.Text)
                        }
                    }
                }
            }

            // F2-7 · F3-6 미리보기
            preview?.let {
                Column(Modifier.fillMaxWidth().background(TC.Highlight, RoundedCornerShape(12.dp)).padding(14.dp)) {
                    Text(it.line1, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    it.line2?.let { l -> Text(l, fontSize = 14.sp, modifier = Modifier.padding(top = 2.dp)) }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
        Box(Modifier.fillMaxWidth().background(TC.Bg).padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryButton("저장", enabled = canSave) { vm.save(onSaved) }
        }
    }

    datePicker?.let { which ->
        val initial = if (which == "end") form.endDate ?: vm.start() else form.startDate ?: vm.today()
        DatePickerDialogCompat(initial, onPick = { d ->
            vm.update { if (which == "end") it.copy(endDate = d) else it.copy(startOption = StartOption.OTHER, startDate = d) }
        }, onDismiss = { datePicker = null })
    }
}

@Composable
private fun ServiceTileButton(t: ServiceTile, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val dashed = t.key == "custom"
    OutlinedButton(onClick = onClick, modifier = modifier.heightIn(min = 68.dp).semantics { contentDescription = t.label },
        shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (dashed) Color.Transparent else Color.White, contentColor = TC.Text),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) TC.Primary else if (dashed) Color(0xFF9A9EA5) else Color(0xFFD4D4CF))) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(Modifier.size(28.dp).background(Color(0xFFECECE8), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(t.initial, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TC.Sub)
            }
            Text(t.label + if (selected) " ✓" else "", fontSize = 13.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1)
        }
    }
}

private fun formatFull(d: LocalDate) = "${d.year}년 ${d.monthValue}월 ${d.dayOfMonth}일"
