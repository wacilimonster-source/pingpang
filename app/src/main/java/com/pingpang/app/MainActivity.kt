package com.pingpang.app

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pingpang.app.ui.home.HomeScreen
import com.pingpang.app.ui.mine.MineScreen
import com.pingpang.app.ui.mine.SettingsScreen
import com.pingpang.app.ui.opponent.OpponentDetailScreen
import com.pingpang.app.ui.opponent.OpponentListScreen
import com.pingpang.app.ui.plan.CheckinScreen
import com.pingpang.app.ui.plan.HistoryScreen
import com.pingpang.app.ui.plan.PlanDetailScreen
import com.pingpang.app.ui.plan.PlanEditScreen
import com.pingpang.app.ui.plan.PlanScreen
import com.pingpang.app.ui.plan.SessionDetailScreen
import com.pingpang.app.ui.skill.SkillListScreen
import com.pingpang.app.ui.theme.PingPangTheme
import com.pingpang.app.ui.video.CompareScreen
import com.pingpang.app.ui.video.RecordScreen
import com.pingpang.app.ui.video.VideoDetailScreen
import com.pingpang.app.ui.video.VideoLibScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PingPangTheme {
                PingPangAppRoot()
            }
        }
    }
}

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

private val navItems = listOf(
    NavItem("home", "首页", Icons.Filled.Home),
    NavItem("plan", "计划", Icons.AutoMirrored.Filled.ListAlt),
    NavItem("video", "视频", Icons.Filled.VideoLibrary),
    NavItem("mine", "我的", Icons.Filled.Person),
)

@Composable
fun PingPangAppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                navItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") {
                HomeScreen(
                    onCheckin = { navController.navigate("checkin/-1") },
                    onRecord = { navController.navigate("record") },
                    onCompare = { navController.navigate("compare") },
                    onHistory = { navController.navigate("history") },
                )
            }
            composable("plan") {
                PlanScreen(
                    onNew = { navController.navigate("plan_edit/-1") },
                    onOpen = { id -> navController.navigate("plan_detail/$id") },
                )
            }
            composable("video") {
                VideoLibScreen(
                    onOpenVideo = { id -> navController.navigate("video_detail/$id") },
                    onRecord = { navController.navigate("record") },
                    onCompare = { navController.navigate("compare") },
                )
            }
            composable("mine") {
                MineScreen(
                    onOpenSettings = { navController.navigate("settings") },
                    onOpenSkills = { navController.navigate("skills") },
                    onOpenOpponents = { navController.navigate("opponents") },
                )
            }

            composable(
                "plan_edit/{stageId}",
                arguments = listOf(navArgument("stageId") { type = NavType.LongType }),
            ) { entry ->
                PlanEditScreen(
                    stageId = entry.arguments?.getLong("stageId") ?: -1,
                    onDone = { navController.popBackStack() },
                )
            }
            composable(
                "plan_detail/{stageId}",
                arguments = listOf(navArgument("stageId") { type = NavType.LongType }),
            ) { entry ->
                val stageId = entry.arguments?.getLong("stageId") ?: -1L
                PlanDetailScreen(
                    stageId = stageId,
                    onEdit = { navController.navigate("plan_edit/$stageId") },
                    onCheckin = { planId, content ->
                        navController.navigate(
                            "checkin/$planId?prefill=${Uri.encode(content)}"
                        )
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                "checkin/{planId}?prefill={prefill}&sessionId={sessionId}",
                arguments = listOf(
                    navArgument("planId") { type = NavType.LongType },
                    navArgument("prefill") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("sessionId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) { entry ->
                CheckinScreen(
                    planId = entry.arguments?.getLong("planId") ?: -1L,
                    prefillContent = entry.arguments?.getString("prefill") ?: "",
                    editSessionId = entry.arguments?.getLong("sessionId") ?: -1L,
                    onDone = { navController.popBackStack() },
                )
            }
            composable(
                "session_detail/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
            ) { entry ->
                SessionDetailScreen(
                    sessionId = entry.arguments?.getLong("sessionId") ?: -1L,
                    onBack = { navController.popBackStack() },
                    onEdit = { id ->
                        navController.navigate("checkin/0?sessionId=$id")
                    },
                )
            }
            composable("history") {
                HistoryScreen(
                    onBack = { navController.popBackStack() },
                    onOpen = { id -> navController.navigate("session_detail/$id") },
                )
            }
            composable("record") {
                RecordScreen(
                    onBack = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() },
                )
            }
            composable(
                "video_detail/{videoId}",
                arguments = listOf(navArgument("videoId") { type = NavType.LongType }),
            ) { entry ->
                VideoDetailScreen(
                    videoId = entry.arguments?.getLong("videoId") ?: -1L,
                    onBack = { navController.popBackStack() },
                    onAddToCompare = { id ->
                        navController.navigate("compare?ids=$id")
                    },
                )
            }
            composable(
                "compare?ids={ids}",
                arguments = listOf(navArgument("ids") {
                    type = NavType.StringType
                    defaultValue = ""
                }),
            ) { entry ->
                val ids = (entry.arguments?.getString("ids") ?: "")
                    .split(",")
                    .mapNotNull { it.toLongOrNull() }
                    .filter { it > 0 }
                CompareScreen(
                    initialIds = ids,
                    onBack = { navController.popBackStack() },
                )
            }
            composable("settings") {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable("skills") {
                SkillListScreen(onBack = { navController.popBackStack() })
            }
            composable("opponents") {
                OpponentListScreen(
                    onBack = { navController.popBackStack() },
                    onOpen = { id -> navController.navigate("opponent_detail/$id") },
                )
            }
            composable(
                "opponent_detail/{opponentId}",
                arguments = listOf(navArgument("opponentId") { type = NavType.LongType }),
            ) { entry ->
                OpponentDetailScreen(
                    opponentId = entry.arguments?.getLong("opponentId") ?: -1L,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
