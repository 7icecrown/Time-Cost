package com.timecost.demo.ui.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecost.demo.domain.model.InstalledApp
import com.timecost.demo.ui.add.AddViewModel
import com.timecost.demo.ui.common.PrimaryButton
import com.timecost.demo.ui.common.TopBarBack
import com.timecost.demo.ui.theme.TC

/** F11 앱 선택: 설치된 앱 검색 · 여러 개 선택 · '선택 완료 (n)' */
@Composable
fun AppsScreen(vm: AddViewModel, onBack: () -> Unit) {
    val all by vm.installedApps.collectAsState()
    val elsewhere by vm.trackedElsewhere.collectAsState()
    val committed = vm.form.collectAsState().value.apps
    var query by remember { mutableStateOf("") }
    var picked by remember { mutableStateOf(committed.map { it.packageName }.toSet()) }
    val shown = remember(all, query) { all.filter { it.label.contains(query.trim(), ignoreCase = true) } }

    Column(Modifier.fillMaxSize().background(TC.Bg)) {
        TopBarBack("앱 선택", onBack)
        OutlinedTextField(query, { query = it }, singleLine = true, label = { Text("앱 이름 검색") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp), shape = RoundedCornerShape(12.dp))
        if (shown.isEmpty()) {
            // F11-3 빈 상태
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("검색 결과가 없어요", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("다른 이름으로 찾아보세요.", fontSize = 13.sp, color = TC.Sub, modifier = Modifier.padding(top = 4.dp))
                }
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(shown, key = { it.packageName }) { app -> AppRow(app, app.packageName in picked, app.packageName in elsewhere) {
                    picked = if (app.packageName in picked) picked - app.packageName else picked + app.packageName
                } }
            }
        }
        Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryButton("선택 완료 (${picked.size})") {
                vm.update { f -> f.copy(apps = all.filter { it.packageName in picked }) }
                onBack()
            }
        }
    }
}

@Composable
private fun AppRow(app: InstalledApp, checked: Boolean, trackedElsewhere: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(12.dp)).clickable(onClick = onToggle)
        .padding(horizontal = 14.dp).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(app.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            if (trackedElsewhere) Text("다른 구독에서도 추적 중", fontSize = 12.sp, color = TC.Sub)
        }
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
    }
}
