package com.timecost.demo.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.timecost.demo.ui.common.UsageNoticeView
import com.timecost.demo.ui.common.openUsageAccessSettings
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecost.demo.ui.theme.TC

@Composable
fun HomeScreen(
    state: HomeUiState,
    onCardClick: (Long) -> Unit = {}, onDecide: (Long) -> Unit = {}, onAdd: () -> Unit = {},
    onTrash: () -> Unit = {}, onSettings: () -> Unit = {},
    message: String? = null, onMessageShown: () -> Unit = {},
) {
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    LaunchedEffect(message) { if (message != null) { snackbar.showSnackbar(message); onMessageShown() } }
    val highlightedIdx = state.cards.indexOfFirst { it.highlighted }
    // 등록 직후 새 구독이 화면 밖에 있어도 보이도록 그 위치로 이동
    LaunchedEffect(highlightedIdx) { if (highlightedIdx >= 0) listState.animateScrollToItem(state.decisions.size + 2 + highlightedIdx) }
    Box(Modifier.fillMaxSize().background(TC.Bg)) {
        Column(Modifier.fillMaxSize()) {
            state.demoBanner?.let {
                Box(Modifier.fillMaxWidth().heightIn(min = 28.dp).background(TC.Primary), contentAlignment = Alignment.Center) {
                    Text(it, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(start = 20.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Time-Cost", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onTrash, modifier = Modifier.defaultMinSize(48.dp, 48.dp)
                    .semantics { contentDescription = "휴지통, 삭제된 구독 ${state.trashCount}개" }) {
                    Text(if (state.trashCount > 0) "휴지통 ${state.trashCount}" else "휴지통", color = TC.Text, fontSize = 13.sp)
                }
                TextButton(onClick = onSettings, modifier = Modifier.defaultMinSize(48.dp, 48.dp)) { Text("설정", color = TC.Text, fontSize = 13.sp) }
            }
            LazyColumn(state = listState, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.decisions, key = { "d${it.id}" }) { DecisionRow(it, onDecide) }
                item { Summary(state) }
                item { Text("결제일부터 센 사용 시간", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TC.Sub, modifier = Modifier.padding(start = 2.dp, top = 6.dp)) }
                state.usageNotice?.let { n -> item {
                    Column(Modifier.fillMaxWidth().background(TC.Highlight, RoundedCornerShape(TC.Radius)).padding(horizontal = 14.dp, vertical = 10.dp)) {
                        UsageNoticeView(n) { ctx.openUsageAccessSettings() }
                    }
                } }
                items(state.cards, key = { "c${it.id}" }) { SubCard(it) { onCardClick(it.id) } }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd, containerColor = TC.Primary, contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 16.dp),
        ) { Text("+ 구독 추가", fontWeight = FontWeight.SemiBold) }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
}

@Composable
private fun DecisionRow(d: DecisionItem, onDecide: (Long) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(TC.Radius)).border(1.dp, TC.Border, RoundedCornerShape(TC.Radius))
            .padding(horizontal = 14.dp, vertical = 12.dp).heightIn(min = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(d.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(d.sub, fontSize = 12.sp, color = if (d.needsDecision) TC.Accent else TC.Sub,
                fontWeight = if (d.needsDecision) FontWeight.SemiBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (d.needsDecision) {
            Button(onClick = { onDecide(d.id) }, shape = RoundedCornerShape(10.dp), modifier = Modifier.heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = TC.Primary)) { Text("결정", fontWeight = FontWeight.SemiBold) }
        } else d.dDay?.let {
            Text(it, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = if (d.urgent) TC.Accent else TC.Text, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun Summary(s: HomeUiState) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp)) {
        HorizontalDivider(color = TC.Border)
        Row(Modifier.fillMaxWidth().padding(vertical = 16.dp), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Text("구독", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TC.Sub)
                Text("${s.count}개", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("한 달 구독료", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TC.Sub)
                Text(s.monthlyTotal, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        }
        HorizontalDivider(color = TC.Border)
    }
}

@Composable
private fun SubCard(c: CardItem, onClick: () -> Unit) {
    Card(onClick = onClick, shape = RoundedCornerShape(TC.Radius), colors = CardDefaults.cardColors(containerColor = if (c.highlighted) TC.Highlight else TC.Card),
        border = BorderStroke(1.dp, TC.Border), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    // 이름은 한 줄 말줄임 (F1-4). 태그는 줄지 않도록 weight 밖에 둡니다.
                    Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    c.tags.forEach { Tag(it) }
                }
                Spacer(Modifier.width(8.dp))
                Text(c.monthly, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false)
                Text(" /월", fontSize = 11.sp, color = TC.Sub, maxLines = 1, softWrap = false)
            }
            c.payLine?.let { Text(it, fontSize = 12.sp, color = TC.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp)) }
            Row(Modifier.fillMaxWidth().padding(top = 8.dp).heightIn(min = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f)) {
                    c.usageLine?.let { Text(it, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)) }
                    c.noRecordLine?.let { Text(" $it", fontSize = 14.sp, color = TC.Sub, maxLines = 1, softWrap = false) }
                }
                c.dueLine?.let {
                    Text(it, fontSize = 12.sp, color = if (c.dueUrgent) TC.Accent else TC.Sub,
                        fontWeight = if (c.dueUrgent) FontWeight.Bold else FontWeight.Normal, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun Tag(text: String) {
    Text(text, fontSize = 12.sp, color = TC.Sub, maxLines = 1, softWrap = false,
        modifier = Modifier.padding(start = 4.dp).border(1.dp, Color(0xFFD4D4CF), RoundedCornerShape(6.dp)).padding(horizontal = 6.dp, vertical = 1.dp))
}
