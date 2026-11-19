package com.timecost.demo.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecost.demo.domain.usecase.StatsCalc
import com.timecost.demo.ui.common.*
import com.timecost.demo.ui.theme.TC

/** F8 통계 */
@Composable
fun StatsScreen(vm: StatsViewModel) {
    val ui by vm.ui.collectAsState()
    Column(Modifier.fillMaxSize().background(TC.Bg)) {
        DemoBanner(ui.banner)
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { Text("통계", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            item {   // F8-1
                Column(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Text("한 달 구독료", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TC.Sub)
                    Text(ui.monthly, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Text("1년이면 ${ui.yearly}이에요.", fontSize = 14.sp, color = TC.Sub)
                }
            }
            ui.next?.let { n -> item {   // F8-2
                Column(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(12.dp)).padding(16.dp)) {
                    Text("다음 결제", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TC.Sub)
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("${n.name} · ${n.dateLabel}", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(n.amount, fontSize = 14.sp)
                        }
                        Text(n.daysLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TC.Accent, maxLines = 1, softWrap = false)
                    }
                }
            } }
            item {   // F8-3
                Column {
                    Text("어디에 쓰고 있나요", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("한 달 기준 구독료예요.", fontSize = 12.sp, color = TC.Sub, modifier = Modifier.padding(top = 2.dp))
                }
            }
            items(ui.shares) { r ->
                Column(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(r.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(r.amount + (r.percent?.let { " · $it%" } ?: ""), fontSize = 14.sp, maxLines = 1, softWrap = false)
                    }
                    if (r.percent != null) Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(6.dp).background(Color(0xFFECECE8), RoundedCornerShape(3.dp))) {
                        Box(Modifier.fillMaxWidth(r.fraction.coerceIn(0.02f, 1f)).fillMaxHeight().background(TC.Primary, RoundedCornerShape(3.dp)))
                    }
                }
            }
            item {   // F8-4
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1시간 쓸 때 드는 돈", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillChip("이번 결제 기간", ui.which == StatsCalc.Which.CURRENT) { vm.setWhich(StatsCalc.Which.CURRENT) }
                        PillChip("지난 결제 기간", ui.which == StatsCalc.Which.PREVIOUS) { vm.setWhich(StatsCalc.Which.PREVIOUS) }
                    }
                    if (ui.which == StatsCalc.Which.CURRENT) Text("이번 결제 기간은 아직 진행 중이라, 사용 시간이 늘면 값이 줄어들어요.", fontSize = 12.sp, color = TC.Sub)
                }
            }
            items(ui.costs) { c ->
                Row(Modifier.fillMaxWidth().background(TC.Card, RoundedCornerShape(12.dp)).padding(horizontal = 14.dp, vertical = 12.dp).heightIn(min = 56.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        c.detail?.let { Text(it, fontSize = 12.sp, color = TC.Sub, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(c.headline, fontSize = if (c.muted) 13.sp else 16.sp, fontWeight = if (c.muted) FontWeight.Normal else FontWeight.Bold,
                        color = if (c.muted) TC.Sub else TC.Text, maxLines = 1)
                }
            }
        }
    }
}
