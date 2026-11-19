package com.timecost.demo.ui.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecost.demo.data.demo.SCENARIOS
import com.timecost.demo.ui.common.*
import com.timecost.demo.ui.theme.TC

/** F12 시연 도구 (Demo) */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DemoScreen(vm: DemoViewModel, onBack: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.messageShown() } }
    var confirmMode by remember { mutableStateOf<Boolean?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    var scenarioOpen by remember { mutableStateOf(false) }
    var plannedOpen by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(TC.Bg)) {
        Column(Modifier.fillMaxSize()) {
            DemoBanner(ui.banner)
            TopBarBack("시연 도구", onBack)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)) {

                Group("데이터") {
                    ToggleRow("데모 모드", "시연용 데이터베이스를 사용해요. 전환하면 앱이 다시 시작돼요.", ui.active, if (ui.active) "켜짐" else "꺼짐") { confirmMode = it }
                    HorizontalDivider(color = Color(0xFFEEEEEA))
                    ActionRow("시나리오 불러오기", ui.scenarioLabel, enabled = ui.active) { scenarioOpen = true }
                    HorizontalDivider(color = Color(0xFFEEEEEA))
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("데모 DB 초기화", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("시연 데이터를 처음 상태로 되돌려요.", fontSize = 12.sp, color = TC.Sub)
                        }
                        OutlinedButton(onClick = { confirmReset = true }, enabled = ui.active, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) { Text("초기화", color = TC.Text) }
                    }
                }

                Group("기준일") {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(ui.dateLabel, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(ui.offsetLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TC.Accent)
                    }
                    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 7, 30).forEach { n ->
                            OutlinedButton(onClick = { vm.move(n) }, enabled = ui.active, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)) { Text("+${n}일", color = TC.Text, fontSize = 14.sp, maxLines = 1) }
                        }
                        OutlinedButton(onClick = { vm.resetDate() }, enabled = ui.active, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)) { Text("초기화", color = TC.Text, fontSize = 14.sp, maxLines = 1) }
                    }
                    Text("날짜 이동은 데모 DB에서만 작동해요.", fontSize = 12.sp, color = TC.Sub, modifier = Modifier.padding(bottom = 12.dp))
                }

                Group("상태 재현") {
                    ToggleRow("사용 시간 조회 실패", "조회 실패 상태의 화면을 보여 줘요.", ui.failUsage, if (ui.failUsage) "켜짐" else "꺼짐", enabled = ui.active) { vm.setFail(it) }
                    HorizontalDivider(color = Color(0xFFEEEEEA))
                    ToggleRow("권한 없음", "사용 정보 접근이 없는 상태를 보여 줘요.", ui.noPermission, if (ui.noPermission) "켜짐" else "꺼짐", enabled = ui.active) { vm.setNoPermission(it) }
                }

                Group("알림") {
                    Row(Modifier.fillMaxWidth().clickable { plannedOpen = !plannedOpen }.heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("예약 알림 목록", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("가상으로 예약된 알림 ${ui.planned.size}건", fontSize = 12.sp, color = TC.Sub)
                        }
                        Text(if (plannedOpen) "접기" else "펼치기", fontSize = 12.sp, color = TC.Sub)
                    }
                    if (plannedOpen) ui.planned.forEach { Text(it, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)) }
                    HorizontalDivider(color = Color(0xFFEEEEEA))
                    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("테스트 알림", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("시연용 알림을 지금 보내요.", fontSize = 12.sp, color = TC.Sub)
                        }
                        OutlinedButton(onClick = { vm.sendTest() }, modifier = Modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp)) { Text("보내기", color = TC.Text) }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }

    confirmMode?.let { on ->
        AlertDialog(onDismissRequest = { confirmMode = null },
            title = { Text(if (on) "데모 모드를 켤까요?" else "데모 모드를 끌까요?") },
            text = { Text("전환하면 앱이 다시 시작돼요. 데모 모드를 끄면 이 기기의 실제 데이터와 오늘 날짜를 써요.") },
            confirmButton = { TextButton(onClick = { confirmMode = null; vm.switchDemoMode(on) }) { Text("전환") } },
            dismissButton = { TextButton(onClick = { confirmMode = null }) { Text("취소") } })
    }
    if (confirmReset) AlertDialog(onDismissRequest = { confirmReset = false },
        title = { Text("데모 DB를 초기화할까요?") }, text = { Text("시연 데이터를 처음 상태로 되돌리고, 기준일도 처음으로 돌아가요.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; vm.resetDb() }) { Text("초기화") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("취소") } })

    if (scenarioOpen) ModalBottomSheet(onDismissRequest = { scenarioOpen = false }, containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
            Text("시나리오 불러오기", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("고르면 데이터를 처음 상태로 되돌리고 불러와요.", fontSize = 13.sp, color = TC.Sub, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
            SCENARIOS.forEach { s ->
                Column(Modifier.fillMaxWidth().padding(vertical = 3.dp).border(1.dp, TC.Border, RoundedCornerShape(12.dp))
                    .clickable { scenarioOpen = false; vm.loadScenario(s.id) }.heightIn(min = 56.dp).padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text("${s.id}. ${s.title}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(s.steps, fontSize = 12.sp, color = TC.Sub)
                }
            }
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SectionLabel(title)
        Column(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(12.dp)).border(1.dp, TC.Border, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp), content = content)
    }
}

@Composable
private fun ToggleRow(title: String, desc: String, checked: Boolean, stateText: String, enabled: Boolean = true, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, fontSize = 12.sp, color = TC.Sub)
        }
        Text(stateText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp))
        Switch(checked, onChange, enabled = enabled, modifier = Modifier.semantics { contentDescription = title })
    }
}

@Composable
private fun ActionRow(title: String, value: String, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).heightIn(min = 64.dp).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 12.sp, color = TC.Sub)
        }
        Text("›", fontSize = 20.sp, color = TC.Sub)
    }
}
