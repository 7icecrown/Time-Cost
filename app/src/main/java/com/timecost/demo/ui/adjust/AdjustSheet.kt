package com.timecost.demo.ui.adjust

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecost.demo.core.Format
import com.timecost.demo.domain.model.AdjustType
import com.timecost.demo.ui.common.*
import com.timecost.demo.ui.theme.TC

/** F5 사용 시간 수정 시트 (시안 Adj1~3) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustSheet(vm: AdjustViewModel, onDismiss: () -> Unit) {
    val header by vm.header.collectAsState()
    val form by vm.form.collectAsState()
    val result by vm.result.collectAsState()
    val h = header ?: return
    val noRecord = h.autoMinutes == null

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
            Text(h.title, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text(h.name, fontSize = 13.sp, color = TC.Sub, modifier = Modifier.padding(top = 2.dp))
            Spacer(Modifier.height(14.dp))
            InfoCard { InfoRow("자동 기록", h.autoMinutes?.let(Format::duration) ?: "기록 없음", divider = false) }

            Spacer(Modifier.height(16.dp))
            SectionLabel("수정 유형")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(AdjustType.ADD to "추가", AdjustType.SUB to "감소", AdjustType.SET to "변경").forEach { (t, label) ->
                    // 기록 없는 날은 '변경'만 고를 수 있음 (F5-2)
                    val enabled = !noRecord || t == AdjustType.SET
                    Box(Modifier.alpha(if (enabled) 1f else 0.4f)) {
                        PillChip(label, form.type == t) { if (enabled) vm.setType(t) }
                    }
                }
            }
            if (noRecord) HelpText("자동 기록이 없는 날은 변경만 고를 수 있어요. 입력한 시간이 그날의 사용 시간이 돼요.")

            Spacer(Modifier.height(16.dp))
            SectionLabel("시간")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(form.hours, vm::setHours, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), suffix = { Text("시간") })
                OutlinedTextField(form.minutes, vm::setMinutes, singleLine = true, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), suffix = { Text("분") })
            }

            Spacer(Modifier.height(16.dp))
            Column(Modifier.fillMaxWidth().background(TC.Highlight, RoundedCornerShape(12.dp)).padding(14.dp)) {
                Text("최종 사용 시간", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TC.Sub)
                Text(result.finalText ?: "시간을 입력하면 보여드려요", fontSize = if (result.finalText != null) 24.sp else 14.sp,
                    fontWeight = if (result.finalText != null) FontWeight.Bold else FontWeight.Normal,
                    color = if (result.finalText != null) TC.Text else TC.Sub, modifier = Modifier.padding(top = 2.dp))
                result.subText?.let { Text(it, fontSize = 12.sp, color = TC.Sub) }
            }
            result.error?.let { Text(it, fontSize = 13.sp, color = Color(0xFFB3261E), modifier = Modifier.padding(top = 8.dp)) }
            if (h.isToday) HelpText("오늘 기록은 자동값이 계속 갱신돼요. 추가·감소는 그대로 두고 최종 시간이 따라 바뀌어요.")

            Spacer(Modifier.height(20.dp))
            PrimaryButton("저장", enabled = result.valid) { vm.save(onDismiss) }
            if (h.hasExisting) {
                TextButton(onClick = { vm.cancelAdjustment(onDismiss) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("수정 취소", color = TC.Sub) }
                Text("취소하면 자동 기록값으로 돌아가요.", fontSize = 12.sp, color = TC.Sub, modifier = Modifier.align(Alignment.CenterHorizontally))
            }
        }
    }
}
