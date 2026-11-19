package com.timecost.demo.ui.calc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecost.demo.core.Format
import com.timecost.demo.ui.common.*
import com.timecost.demo.ui.theme.TC

/** F7 구독 계산기 */
@Composable
fun CalcScreen(vm: CalcViewModel) {
    val form by vm.form.collectAsState()
    val result by vm.result.collectAsState()
    val records by vm.records.collectAsState()
    val loadable by vm.loadable.collectAsState()
    val canSave by vm.canSave.collectAsState()
    var pickOpen by remember { mutableStateOf(false) }

    LazyColumn(Modifier.fillMaxSize().background(TC.Bg), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Text("구독 계산기", fontSize = 22.sp, fontWeight = FontWeight.Bold) }

        item {   // F7-2 구독 불러오기
            Column(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(12.dp)).padding(14.dp)) {
                OutlinedButton(onClick = { pickOpen = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) {
                    Text("구독 불러오기", color = TC.Text, fontWeight = FontWeight.SemiBold)
                }
                HelpText("시간을 안 재는 것으로 등록한 구독의 이름과 구독료를 채울 수 있어요.")
            }
        }

        item {   // F7-1 입력
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(form.name, { v -> vm.update { it.copy(name = v.take(30)) } }, singleLine = true,
                    label = { Text("이름 (선택)") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(form.priceText, vm::setPrice, enabled = !form.free, singleLine = true, suffix = { Text("원") },
                        label = { Text("낸 구독료") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp))
                    Text("무료", fontSize = 14.sp)
                    Switch(form.free, { c -> vm.update { it.copy(free = c) } }, modifier = Modifier.semantics { contentDescription = "무료" })
                }
                SectionLabel("사용 시간")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(form.hours, vm::setHours, singleLine = true, suffix = { Text("시간") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                    OutlinedTextField(form.minutes, vm::setMinutes, singleLine = true, suffix = { Text("분") }, modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), shape = RoundedCornerShape(12.dp))
                }
            }
        }

        item {   // 결과 (F7-1 · F7-4 · F7-5)
            Column(Modifier.fillMaxWidth().background(TC.Highlight, RoundedCornerShape(12.dp)).padding(16.dp)) {
                Text("1시간 쓸 때 드는 돈", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TC.Sub)
                when (val r = result) {
                    is CalcResult.Cost -> { Text(r.won, fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp)); Text(r.detail, fontSize = 13.sp, color = TC.Sub) }
                    is CalcResult.FreeOnly -> { Text("사용 시간만 표시", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)); Text(r.detail, fontSize = 13.sp, color = TC.Sub) }
                    is CalcResult.Hint -> Text(r.text, fontSize = 14.sp, color = TC.Sub, modifier = Modifier.padding(top = 6.dp))
                }
                if (result !is CalcResult.Hint) Text("입력하신 값 기준의 계산 결과예요", fontSize = 12.sp, color = TC.Sub, modifier = Modifier.padding(top = 8.dp))
            }
        }

        item { PrimaryButton("계산하고 기록하기", enabled = canSave) { vm.saveRecord() } }

        item {   // F7-3 계산 기록
            Column {
                Text("계산 기록", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("탭하면 값이 입력 칸에 채워져요", fontSize = 12.sp, color = TC.Sub, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
            }
        }
        items(records, key = { it.record.id }) { r -> RecordRow(r) { vm.refill(r.record) } }
    }

    if (pickOpen) AlertDialog(
        onDismissRequest = { pickOpen = false },
        title = { Text("구독 불러오기") },
        text = {
            if (loadable.isEmpty()) Text("사용 시간 안 재요로 등록한 구독이 없어요.")
            else Column { loadable.forEach { s ->
                Row(Modifier.fillMaxWidth().clickable { vm.load(s); pickOpen = false }.heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text(if (s.price == 0L) "무료" else Format.won(s.price), fontSize = 12.sp, color = TC.Sub)
                    }
                }
            } }
        },
        confirmButton = { TextButton(onClick = { pickOpen = false }) { Text("닫기") } },
    )
}

@Composable
private fun RecordRow(r: CalcRecordUi, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 10.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(r.title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(r.detail, fontSize = 12.sp, color = TC.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(r.result, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
            Text(r.dateLabel, fontSize = 12.sp, color = TC.Sub, textAlign = TextAlign.End)
        }
    }
}
