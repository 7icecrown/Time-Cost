package com.timecost.demo.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecost.demo.core.Format
import com.timecost.demo.ui.common.*
import com.timecost.demo.ui.theme.TC

/** F9 휴지통 (Trash · TrashConfirm · TrashEmpty) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(vm: TrashViewModel, onBack: () -> Unit) {
    val items by vm.items.collectAsState()
    var confirm by remember { mutableStateOf<TrashItem?>(null) }

    Column(Modifier.fillMaxSize().background(TC.Bg)) {
        TopBarBack("휴지통", onBack)
        Text("삭제한 구독은 30일 동안 복구할 수 있어요. 기간이 지나면 완전히 삭제돼요.", fontSize = 13.sp, color = TC.Sub,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp))
        if (items.isEmpty()) {   // F9-4 빈 상태
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("휴지통이 비어 있어요", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("삭제한 구독이 있으면 여기에 보여요.", fontSize = 13.sp, color = TC.Sub, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { t ->
                Column(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(12.dp)).padding(14.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(Modifier.weight(1f)) {
                            Text(t.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.kind, fontSize = 12.sp, color = TC.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(t.remaining, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                            Text(t.deletedLabel, fontSize = 12.sp, color = TC.Sub, maxLines = 1, softWrap = false)
                        }
                    }
                    t.restoreNote?.let { Text(it, fontSize = 12.sp, color = TC.Accent, modifier = Modifier.padding(top = 8.dp)) }
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { vm.restore(t.id) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) { Text("복구", color = TC.Text, fontWeight = FontWeight.SemiBold) }
                        OutlinedButton(onClick = { confirm = t }, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) { Text("완전 삭제", color = Color(0xFFB3261E), fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        }
    }

    // F9-2 완전 삭제 확인 시트
    confirm?.let { t ->
        ModalBottomSheet(onDismissRequest = { confirm = null }, containerColor = Color.White, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
                Text("${t.name}${Format.eulReul(t.name)} 완전히\n삭제할까요?", fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
                Text("사용 기록과 수정 내용도 함께 사라지고, 다시 복구할 수 없어요.", fontSize = 14.sp, color = TC.Sub, modifier = Modifier.padding(top = 10.dp))
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { confirm = null }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) { Text("취소", color = TC.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                    Button(onClick = { vm.deletePermanently(t.id); confirm = null }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))) { Text("완전 삭제", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                }
            }
        }
    }
}
