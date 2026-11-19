package com.timecost.demo

import android.Manifest
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.rule.GrantPermissionRule
import com.timecost.demo.data.demo.DemoController
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * PRD 11장 시연 시나리오 A~F 리허설. 데모 빌드(demoDebug)에서만 의미가 있습니다.
 * 각 테스트는 시나리오를 불러와 데이터를 처음 상태로 맞춘 뒤, 화면의 글자로 따라갑니다.
 */
@HiltAndroidTest
class ScenarioTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val perm: GrantPermissionRule =
        if (Build.VERSION.SDK_INT >= 33) GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS) else GrantPermissionRule.grant()
    @get:Rule(order = 2) val compose = createAndroidComposeRule<MainActivity>()
    @Inject lateinit var demo: DemoController

    private fun load(id: String) { hilt.inject(); runBlocking { demo.loadScenario(id) } }
    private fun waitFor(text: String, substring: Boolean = false) =
        compose.waitUntil(10_000) { compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }
    private fun tap(text: String, index: Int = 0) { waitFor(text); compose.onAllNodesWithText(text)[index].performClick() }
    private fun back() { compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() } }

    @Before fun setUp() { load("A") }

    @Test fun A_overview_stats_calc() {
        waitFor("46,550원")                                  // 한 달 구독료
        tap("통계"); waitFor("558,600원", substring = true)   // 1년이면 558,600원
        tap("계산기")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("99000")   // 낸 구독료
        compose.onAllNodes(hasSetTextAction())[2].performTextInput("40")      // 40시간
        waitFor("2,475원")
    }

    @Test fun B_keep_trial_in_three_taps() {
        load("B")
        tap("결정"); tap("유지하기"); tap("확인")              // 3탭
        waitFor("Disney+를 정기 구독으로 등록했어요")
    }

    @Test fun C_cancel_trial() {
        load("C")
        tap("설정"); tap("시연 도구")
        repeat(3) { tap("+1일") }                            // 기준일 +3일 → Apple Music 종료
        back(); back()
        waitFor("Apple Music 무료 체험")
        tap("결정", index = 1)                               // Disney+ 다음에 Apple Music
        tap("해지하기"); tap("해지 완료로 표시")
        waitFor("해지 완료로 표시했어요", substring = true)
        compose.onNodeWithContentDescription("휴지통", substring = true).performClick()
        waitFor("Apple Music 무료 체험")
    }

    @Test fun D_add_subscription_in_four_taps_or_fewer() {
        load("D")
        tap("+ 구독 추가"); tap("티빙")                       // 2탭
        compose.onAllNodes(hasSetTextAction())[0].performScrollTo().performTextInput("9500")
        tap("저장")                                          // 3탭
        waitFor("티빙을 정기 구독으로 등록했어요")
    }

    @Test fun E_adjust_usage_time() {
        load("E")
        tap("YouTube Premium")
        tap("10월 9일 – 11월 8일", index = 0)                // 지난 결제 기간 펼치기
        tap("11월 8일")
        waitFor("11월 8일 (일) 사용 시간 수정")
        compose.onAllNodes(hasSetTextAction())[1].performTextInput("30")     // +30분
        waitFor("1시간 20분")                                // 자동 50분 + 30분
        tap("저장")
    }

    @Test fun F_move_date_and_test_notification() {
        load("F")
        tap("설정"); tap("시연 도구")
        tap("+7일"); waitFor("2026년 11월 17일", substring = true)
        tap("보내기")
        waitFor("가상으로 예약된 알림", substring = true)
    }
}
