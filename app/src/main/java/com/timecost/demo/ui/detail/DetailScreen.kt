package com.timecost.demo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecost.demo.ui.common.*
import com.timecost.demo.ui.theme.TC

@Composable
fun DetailScreen(
    ui: DetailUi,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onDecide: (Long) -> Unit,
    onDayClick: (Long, String) -> Unit = { _, _ -> }, // (구독 id, 날짜 ISO)
    onOpenSettings: () -> Unit = {}
) {
    var confirmDelete by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(TC.Bg)) {
        TopBarBack(if (ui.found) ui.name else "구독 상세", onBack)
        if (!ui.found) { Text("구독을 찾을 수 없어요.", color = TC.Sub, modifier = Modifier.padding(20.dp)); return@Column }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {

            InfoCard { ui.rows.forEachIndexed { i, (k, v) -> InfoRow(k, v, divider = i > 0) } }

            if (!ui.isTrial) {   // F4-1
                Column(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(12.dp)).padding(14.dp)) {
                    Text(ui.usageTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TC.Sub)
                    if (ui.trackingOff) Text("사용 시간 안 재요", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                    else if (ui.usageNotice != null) Box(Modifier.padding(top = 4.dp)) { UsageNoticeView(ui.usageNotice, onOpenSettings) }
                    else Text(ui.usageHeadline ?: "", fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                    ui.usageSub?.let { Text(it, fontSize = 12.sp, color = TC.Sub) }
                }
                ui.carriedText?.let { Text(it, fontSize = 12.sp, color = TC.Sub) }
                Column {
                    SectionLabel("결제 기간별 사용 시간")
                    ui.periods.forEach { PeriodCard(it) { day -> onDayClick(ui.id, day) } }
                }
            } else if (ui.memo.isNotBlank()) {
                Column { SectionLabel("해지 방법 메모"); InfoCard { Text(ui.memo, fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp)) } }
            }

            if (ui.linkedApps.isNotEmpty()) Column {
                SectionLabel("연결된 앱")
                InfoCard { Text(ui.linkedApps.joinToString(", "), fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp)) }
            }

            TextButton(onClick = { confirmDelete = true }, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text("삭제 (휴지통으로 이동)", color = TC.Sub)
            }
        }
        if (ui.needsDecision) Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {   // F4-3
            PrimaryButton("결정하기") { onDecide(ui.id) }
        }
    }
    if (confirmDelete) AlertDialog(
        onDismissRequest = { confirmDelete = false },
        title = { Text("휴지통으로 옮길까요?") },
        text = { Text("30일 동안은 휴지통에서 복구할 수 있어요.") },
        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("삭제") } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("취소") } },
    )
}

@Composable
private fun PeriodCard(p: PeriodItem, onDay: (String) -> Unit) {
    var open by remember { mutableStateOf(p.isCurrent) }
    Column(Modifier.fillMaxWidth().padding(bottom = 8.dp).background(TC.Card, RoundedCornerShape(12.dp))) {
        Row(Modifier.fillMaxWidth().clickable(enabled = p.days.isNotEmpty()) { open = !open }.heightIn(min = 56.dp).padding(horizontal = 14.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.label + if (p.isCurrent) " (이번)" else "", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(p.usageText, fontSize = 13.sp, color = TC.Sub)
            }
            if (p.days.isNotEmpty()) Text(if (open) "접기" else "펼치기", fontSize = 12.sp, color = TC.Sub)
        }
        if (open) p.days.forEach { d ->
            HorizontalDivider(color = Color(0xFFEEEEEA))
            Row(Modifier.fillMaxWidth().clickable { onDay(d.iso) }.heightIn(min = 48.dp).padding(horizontal = 14.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text(d.dateLabel, fontSize = 14.sp, modifier = Modifier.weight(1f))
                d.badge?.let { Text(it, fontSize = 12.sp, color = TC.Sub, modifier = Modifier.padding(end = 10.dp)) }
                Text(d.timeText, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
