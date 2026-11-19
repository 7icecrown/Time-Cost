package com.timecost.demo

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.timecost.demo.data.notify.Notifications
import com.timecost.demo.ui.add.AddScreen
import com.timecost.demo.ui.add.AddViewModel
import com.timecost.demo.ui.calc.CalcScreen
import com.timecost.demo.ui.calc.CalcViewModel
import com.timecost.demo.ui.settings.SettingsScreen
import com.timecost.demo.ui.settings.SettingsViewModel
import com.timecost.demo.ui.stats.StatsScreen
import com.timecost.demo.ui.stats.StatsViewModel
import com.timecost.demo.ui.trash.TrashScreen
import com.timecost.demo.ui.trash.TrashViewModel
import com.timecost.demo.ui.common.TopBarBack
import com.timecost.demo.ui.common.openUsageAccessSettings
import com.timecost.demo.ui.demo.DemoScreen
import com.timecost.demo.ui.demo.DemoViewModel
import com.timecost.demo.ui.AppViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.timecost.demo.ui.adjust.AdjustSheet
import com.timecost.demo.ui.adjust.AdjustViewModel
import com.timecost.demo.ui.apps.AppsScreen
import com.timecost.demo.ui.decide.CancelScreen
import com.timecost.demo.ui.decide.DecideSheet
import com.timecost.demo.ui.decide.DecideViewModel
import com.timecost.demo.ui.decide.KeepConfirmSheet
import com.timecost.demo.ui.detail.DetailScreen
import com.timecost.demo.ui.detail.DetailViewModel
import com.timecost.demo.ui.home.HomeScreen
import com.timecost.demo.ui.home.HomeViewModel
import com.timecost.demo.ui.theme.TC
import com.timecost.demo.ui.theme.TimeCostTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TimeCostTheme { AppRoot() } }
    }
}

private val tabs = listOf("home" to "홈", "calc" to "계산기", "stats" to "통계")

/** 홈·상세 위에 올라오는 시트들. 화면 전환 없이 그 자리에서 결정·수정을 끝냅니다. */
private sealed interface Sheet {
    data class Decide(val id: Long) : Sheet
    data class KeepConfirm(val id: Long) : Sheet
    data class Adjust(val id: Long, val iso: String) : Sheet
}

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val ctx = LocalContext.current
    val current = nav.currentBackStackEntryAsState().value?.destination?.route
    val showTabs = current in tabs.map { it.first }
    var sheet by remember { mutableStateOf<Sheet?>(null) }

    // 시스템 설정(사용 정보 접근 등)에 다녀오면 권한 안내를 다시 계산
    val appVm: AppViewModel = hiltViewModel()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) appVm.onResume() }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // 알림 권한 (Android 13+). 거부해도 앱은 정상 동작하고, 설정에서 상태를 안내합니다 (단계 4).
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !Notifications.enabled(ctx)) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    Scaffold(
        containerColor = TC.Bg,
        bottomBar = {
            if (showTabs) NavigationBar(containerColor = TC.Card) {
                tabs.forEach { (route, label) ->
                    NavigationBarItem(
                        selected = current == route,
                        onClick = { nav.navigate(route) { launchSingleTop = true; popUpTo("home") } },
                        icon = {}, label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        alwaysShowLabel = true,
                    )
                }
            }
        },
    ) { pad ->
        NavHost(nav, startDestination = "home", modifier = Modifier.padding(pad)) {
            composable("home") {
                val vm: HomeViewModel = hiltViewModel()
                HomeScreen(
                    vm.state.collectAsStateWithLifecycle().value,
                    onCardClick = { nav.navigate("detail/$it") },
                    onDecide = { sheet = Sheet.Decide(it) },
                    onAdd = { nav.navigate("add") },
                    onTrash = { nav.navigate("trash") },
                    onSettings = { nav.navigate("settings") },
                    message = vm.message.collectAsStateWithLifecycle().value,
                    onMessageShown = vm::messageShown,
                )
            }
            composable("calc") { CalcScreen(hiltViewModel<CalcViewModel>()) }
            composable("stats") { StatsScreen(hiltViewModel<StatsViewModel>()) }
            composable("trash") { TrashScreen(hiltViewModel<TrashViewModel>(), onBack = { nav.popBackStack() }) }
            composable("settings") {
                SettingsScreen(hiltViewModel<SettingsViewModel>(), onBack = { nav.popBackStack() },
                    onDemo = { nav.navigate("demo") }, onDebug = { nav.navigate("debug") })
            }
            composable("demo") { DemoScreen(hiltViewModel<DemoViewModel>(), onBack = { nav.popBackStack() }) }
            composable("debug") { Column { TopBarBack("디버그 메뉴", { nav.popBackStack() }); Placeholder("디버그 메뉴") } }

            composable("add") { entry ->
                val vm: AddViewModel = hiltViewModel(entry)
                AddScreen(vm, onBack = { nav.popBackStack() }, onPickApps = { nav.navigate("apps") },
                    onSaved = { nav.popBackStack("home", inclusive = false) })
            }
            // F6-4 유지하기 → 수정: 값이 채워진 정기 구독 입력 화면
            composable("keepEdit/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: AddViewModel = hiltViewModel(entry)
                LaunchedEffect(id) { vm.prefillFromTrial(id) }
                AddScreen(vm, keepMode = true, onBack = { nav.popBackStack() }, onPickApps = { nav.navigate("apps") },
                    onSaved = { nav.popBackStack("home", inclusive = false) })
            }
            composable("apps") {
                // 바로 앞 화면(구독 추가 또는 유지 수정)과 같은 ViewModel을 공유해 선택 결과를 돌려줍니다.
                val parent = remember(it) { checkNotNull(nav.previousBackStackEntry) }
                AppsScreen(hiltViewModel<AddViewModel>(parent), onBack = { nav.popBackStack() })
            }
            composable("detail/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
                val vm: DetailViewModel = hiltViewModel()
                DetailScreen(vm.state.collectAsStateWithLifecycle().value,
                    onBack = { nav.popBackStack() },
                    onDelete = { vm.delete { nav.popBackStack("home", inclusive = false) } },
                    onDecide = { sheet = Sheet.Decide(it) },
                    onDayClick = { id, iso -> sheet = Sheet.Adjust(id, iso) },
                    onOpenSettings = { ctx.openUsageAccessSettings() })
            }
            // F6-5 해지 확인
            composable("cancel/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("id") ?: 0L
                val vm: DecideViewModel = hiltViewModel(entry)
                LaunchedEffect(id) { vm.load(id) }
                CancelScreen(vm.ui.collectAsStateWithLifecycle().value,
                    onMark = { vm.markCancelled { nav.popBackStack("home", inclusive = false) } },
                    onBack = { nav.popBackStack() })
            }
        }
    }

    // ---- 시트 ----
    when (val s = sheet) {
        is Sheet.Decide -> {
            val vm: DecideViewModel = hiltViewModel(key = "decide-${s.id}")
            LaunchedEffect(s.id) { vm.load(s.id) }
            vm.ui.collectAsStateWithLifecycle().value?.let { ui ->
                DecideSheet(ui,
                    onKeep = {
                        // 종료 후 구독료가 비어 있으면 확인 시트 대신 수정 화면으로 바로 이동
                        if (ui.goesToEdit) { sheet = null; nav.navigate("keepEdit/${s.id}") } else sheet = Sheet.KeepConfirm(s.id)
                    },
                    onCancel = { sheet = null; nav.navigate("cancel/${s.id}") },
                    onLater = { sheet = null })   // 나중에: 아무것도 바뀌지 않고 '결정 대기'로 남음
            }
        }
        is Sheet.KeepConfirm -> {
            val vm: DecideViewModel = hiltViewModel(key = "decide-${s.id}")
            vm.ui.collectAsStateWithLifecycle().value?.let { ui ->
                KeepConfirmSheet(ui,
                    onConfirm = { vm.confirmKeep { sheet = null; nav.popBackStack("home", inclusive = false) } },
                    onEdit = { sheet = null; nav.navigate("keepEdit/${s.id}") },
                    onBack = { sheet = Sheet.Decide(s.id) })
            }
        }
        is Sheet.Adjust -> {
            val vm: AdjustViewModel = hiltViewModel(key = "adjust-${s.id}-${s.iso}")
            LaunchedEffect(s.id, s.iso) { vm.load(s.id, s.iso) }
            AdjustSheet(vm) { sheet = null }
        }
        null -> {}
    }
}

@Composable
private fun Placeholder(text: String) =
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text, color = TC.Sub) }
