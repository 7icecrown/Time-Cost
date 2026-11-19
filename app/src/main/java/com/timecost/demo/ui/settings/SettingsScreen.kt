package com.timecost.demo.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.timecost.demo.BuildConfig
import com.timecost.demo.core.Format
import com.timecost.demo.core.timeLabel
import com.timecost.demo.ui.common.*
import com.timecost.demo.ui.theme.TC
import java.time.LocalTime

/** F10 설정 (Settings · SettingsTime) */
@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit, onDemo: () -> Unit, onDebug: () -> Unit) {
    val ui by vm.ui.collectAsState()
    val ctx = LocalContext.current
    var timeOpen by remember { mutableStateOf(false) }

    // 시스템 설정에 다녀온 뒤 권한 상태 갱신
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) vm.refreshPermissions() }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }

    Column(Modifier.fillMaxSize().background(TC.Bg)) {
        DemoBanner(ui.banner)
        TopBarBack("설정", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)) {

            Section("알림") {
                Row(Modifier.fillMaxWidth().clickable { timeOpen = true }.heightIn(min = 64.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("알림 시각", fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        Text("체험·할인 종료 알림을 보내는 시각이에요.", fontSize = 12.sp, color = TC.Sub)
                    }
                    Text(Format.timeLabel(ui.notifyTime), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Section("권한") {   // F10-2
                PermissionRow("사용 정보 접근", "연결한 앱의 사용 시간을 읽을 수 있어요.", ui.usageAccess, "허용됨", "허용 안 됨") {
                    ctx.safeStart(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                }
                HorizontalDivider(color = Color(0xFFEEEEEA))
                PermissionRow("알림", "꺼져 있으면 종료 알림을 받을 수 없어요.", ui.notifications, "켜짐", "꺼짐") {
                    ctx.safeStart(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, ctx.packageName))
                }
            }

            Section("앱 정보") {   // F10-3
                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("버전", fontSize = 15.sp, modifier = Modifier.weight(1f)); Text(BuildConfig.VERSION_NAME, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = Color(0xFFEEEEEA))
                Text("데이터는 이 기기에만 저장돼요. 앱을 삭제하면 사라져요.", fontSize = 13.sp, color = TC.Sub, modifier = Modifier.padding(vertical = 12.dp))
            }

            // F10-4 빌드별 진입 항목
            if (BuildConfig.DEMO) Section("시연") {
                EntryRow("시연 도구", "시나리오, 기준일, 알림을 시연용으로 조작해요.", onDemo)
            }
            if (BuildConfig.DEBUG) Section("개발자용") {
                EntryRow("디버그 메뉴", "디버그 빌드에서만 보여요.", onDebug)
            }
        }
    }

    if (timeOpen) TimeSheet(current = ui.notifyTime, onSave = { vm.saveTime(it); timeOpen = false }, onDismiss = { timeOpen = false })
}

private fun Context.safeStart(i: Intent) { try { startActivity(i) } catch (_: Exception) { } }

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        SectionLabel(title)
        Column(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(12.dp)).border(1.dp, TC.Border, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp), content = content)
    }
}

@Composable
private fun PermissionRow(title: String, desc: String, on: Boolean, onText: String, offText: String, onOpen: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(desc, fontSize = 12.sp, color = TC.Sub)
        }
        Spacer(Modifier.width(8.dp))
        if (on) Text(onText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        else Column(horizontalAlignment = Alignment.End) {
            Text(offText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TC.Accent)
            TextButton(onClick = onOpen, modifier = Modifier.defaultMinSize(48.dp, 48.dp)) { Text("설정 열기", color = TC.Text, fontSize = 13.sp) }   // 꺼져 있을 때만 (F10-2)
        }
    }
}

@Composable
private fun EntryRow(title: String, desc: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 64.dp).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold); Text(desc, fontSize = 12.sp, color = TC.Sub) }
        Text("›", fontSize = 20.sp, color = TC.Sub)
    }
}

/** F10-1 알림 시각: 오전 8·9·10시, 오후 12·8시, 직접 선택 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSheet(current: LocalTime, onSave: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val presets = listOf(8, 9, 10, 12, 20).map { LocalTime.of(it, 0) }
    var selected by remember { mutableStateOf(current) }
    var custom by remember { mutableStateOf(current !in presets) }
    var picker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("알림 시각", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Text("체험·할인 종료 알림을 이 시각에 보내요.", fontSize = 13.sp, color = TC.Sub, modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
            presets.forEach { t -> TimeOption(Format.timeLabel(t), !custom && selected == t) { selected = t; custom = false } }
            TimeOption(if (custom) "직접 선택 · ${Format.timeLabel(selected)}" else "직접 선택", custom) { picker = true }
            HelpText("저장하면 예약된 알림이 모두 새 시각으로 다시 맞춰져요.")
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp)) { Text("취소", color = TC.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                Button(onClick = { onSave(selected) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TC.Primary)) { Text("저장", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }

    if (picker) {
        val state = rememberTimePickerState(selected.hour, selected.minute, is24Hour = false)
        AlertDialog(onDismissRequest = { picker = false },
            text = { TimePicker(state = state) },
            confirmButton = { TextButton(onClick = { selected = LocalTime.of(state.hour, state.minute); custom = true; picker = false }) { Text("확인") } },
            dismissButton = { TextButton(onClick = { picker = false }) { Text("취소") } })
    }
}

@Composable
private fun TimeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).background(if (selected) TC.Highlight else Color.Transparent, RoundedCornerShape(12.dp))
        .border(1.dp, if (selected) TC.Primary else TC.Border, RoundedCornerShape(12.dp)).clickable(onClick = onClick)
        .heightIn(min = 48.dp).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
        if (selected) Text("✓", fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
