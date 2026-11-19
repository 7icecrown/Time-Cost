package com.timecost.demo.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecost.demo.data.settings.UsageNotice
import com.timecost.demo.ui.theme.TC
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun TopBarBack(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextButton(onClick = onBack, modifier = Modifier.defaultMinSize(48.dp, 48.dp).semantics { contentDescription = "뒤로" }) { Text("←", color = TC.Text, fontSize = 20.sp) }
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
fun SectionLabel(text: String) =
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TC.Sub, modifier = Modifier.padding(bottom = 8.dp))

@Composable
fun HelpText(text: String) = Text(text, fontSize = 12.sp, color = TC.Sub, modifier = Modifier.padding(top = 6.dp))

/** 알약 모양 선택 칩 (최소 높이 44dp 이상, 시안과 동일) */
@Composable
fun PillChip(text: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick, shape = RoundedCornerShape(22.dp),
        modifier = Modifier.heightIn(min = 48.dp).semantics { this.selected = selected; role = Role.RadioButton },
        contentPadding = PaddingValues(horizontal = 16.dp),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) TC.Primary else Color.White,
            contentColor = if (selected) Color.White else TC.Text),
        border = BorderStroke(1.dp, if (selected) TC.Primary else Color(0xFFD4D4CF)),
    ) { Text(text, fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal) }
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled, shape = RoundedCornerShape(TC.Radius),
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(containerColor = TC.Primary, disabledContainerColor = Color(0xFFC9C9C4),
            disabledContentColor = Color.White)) { Text(text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun InfoRow(label: String, value: String, divider: Boolean = true) {
    if (divider) HorizontalDivider(color = Color(0xFFEEEEEA))
    Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, fontSize = 14.sp, color = TC.Sub)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

@Composable
fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(TC.Radius))
        .border(1.dp, TC.Border, RoundedCornerShape(TC.Radius)).padding(horizontal = 14.dp, vertical = 2.dp), content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerDialogCompat(initial: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("확인") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    ) { DatePicker(state = state) }
}

/** F1-6 데모 빌드 상단 배너. 문구가 null이면(실사용 빌드) 아무것도 그리지 않습니다. */
@Composable
fun DemoBanner(text: String?) {
    if (text == null) return
    Box(Modifier.fillMaxWidth().heightIn(min = 28.dp).background(TC.Primary), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 시스템의 '사용 정보 접근' 설정 화면으로 이동 (PRD 10장) */
fun android.content.Context.openUsageAccessSettings() {
    try { startActivity(android.content.Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS)) } catch (_: Exception) { }
}

/** 권한이 없거나 조회에 실패했을 때 사용 시간 영역에만 보여주는 안내 (PRD 10장) */
@Composable
fun UsageNoticeView(notice: UsageNotice, onOpenSettings: () -> Unit) {
    Column {
        Text(
            if (notice == UsageNotice.NO_PERMISSION) "사용 정보 접근이 없어서 사용 시간을 보여줄 수 없어요."
            else "사용 시간을 읽지 못했어요. 잠시 뒤에 다시 확인해 주세요.",
            fontSize = 14.sp, color = TC.Text,
        )
        if (notice == UsageNotice.NO_PERMISSION) TextButton(onClick = onOpenSettings, modifier = Modifier.defaultMinSize(48.dp, 48.dp)) {
            Text("설정 열기", color = TC.Accent, fontWeight = FontWeight.SemiBold)
        }
    }
}
