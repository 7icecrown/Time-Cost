package com.timecost.demo.ui.decide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timecost.demo.ui.common.*
import com.timecost.demo.ui.theme.TC

private val sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

/** F6-1 결정 시트: 유지하기 · 해지하기 · 나중에 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DecideSheet(ui: DecideUi, onKeep: () -> Unit, onCancel: () -> Unit, onLater: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onLater, containerColor = Color.White, shape = sheetShape) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp)) {
            Text("${ui.name}\n사용 기간이 끝났어요", fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
            Spacer(Modifier.height(14.dp))
            InfoCard {
                InfoRow("사용 기간", ui.periodLabel, divider = false)
                ui.usageText?.let { InfoRow("기간 내 사용 시간", it) }
                ui.afterMonthly?.let { InfoRow("종료 후 한 달 구독료", it) }
            }
            if (ui.noRecordDays > 0) HelpText("기록 없는 날 ${ui.noRecordDays}일이 있어요. 기록 없는 날의 시간은 추정하지 않아요.")
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onKeep, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, TC.Primary)) { Text("유지하기", color = TC.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, TC.Primary)) { Text("해지하기", color = TC.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
            TextButton(onClick = onLater, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("나중에", color = TC.Sub, fontSize = 14.sp) }
        }
    }
}

/** F6-2 유지 내용 확인 시트: 저장될 내용을 먼저 보여주고 '확인 · 수정 · 돌아가기' */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeepConfirmSheet(ui: DecideUi, onConfirm: () -> Unit, onEdit: () -> Unit, onBack: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onBack, containerColor = Color.White, shape = sheetShape) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp).verticalScroll(rememberScrollState())) {
            Text("이 내용으로\n정기 구독에 등록할게요", fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 26.sp)
            Spacer(Modifier.height(14.dp))
            InfoCard {
                InfoRow("이름", ui.keepName, divider = false)
                ui.keepPriceLine?.let { InfoRow("구독료", it) }
                InfoRow("시작일 (첫 결제일)", "${ui.keepStart}\n체험 종료 다음 날")
                ui.keepNext?.let { InfoRow("다음 결제일", it) }
                InfoRow("사용 시간 기록", ui.trackingLine)
                ui.carriedLine?.let { InfoRow("체험 때 기록", it) }
            }
            HelpText("다르면 ‘수정’을 눌러 바꿀 수 있어요.")
            Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, TC.Primary)) { Text("수정", color = TC.Text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f).heightIn(min = 52.dp), shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TC.Primary)) { Text("확인", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("돌아가기", color = TC.Sub, fontSize = 14.sp) }
        }
    }
}

/** F6-5 해지 확인 화면: 앱은 실제 해지 여부를 확인하지 않습니다. */
@Composable
fun CancelScreen(ui: DecideUi?, onMark: () -> Unit, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(TC.Bg)) {
        TopBarBack("해지 확인", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("해지 방법을 확인해 주세요", fontSize = 19.sp, fontWeight = FontWeight.Bold)
            Column {
                SectionLabel("해지 방법 메모")
                InfoCard {
                    val memo = ui?.memo.orEmpty()
                    if (memo.isBlank()) Text("적어 둔 해지 방법이 없어요.", fontSize = 14.sp, color = TC.Sub, modifier = Modifier.padding(vertical = 12.dp))
                    else Text(linkify(memo), fontSize = 14.sp, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
            Text("앱은 실제 해지 여부를 확인하지 않아요. 해지는 해당 서비스에서 직접 진행해 주세요.", fontSize = 13.sp, color = TC.Sub)
        }
        Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
            PrimaryButton("해지 완료로 표시", enabled = ui != null, onClick = onMark)
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("뒤로", color = TC.Sub) }
        }
    }
}

/** 메모 안의 주소(URL)를 눌러서 열 수 있게 합니다 (F3-4). 앱은 INTERNET 권한 없이 브라우저에 넘깁니다. */
private fun linkify(text: String): AnnotatedString = buildAnnotatedString {
    val re = Regex("https?://[^\\s]+")
    var last = 0
    re.findAll(text).forEach { m ->
        append(text.substring(last, m.range.first))
        withLink(LinkAnnotation.Url(m.value, TextLinkStyles(SpanStyle(color = TC.Accent, textDecoration = TextDecoration.Underline)))) { append(m.value) }
        last = m.range.last + 1
    }
    append(text.substring(last))
}
