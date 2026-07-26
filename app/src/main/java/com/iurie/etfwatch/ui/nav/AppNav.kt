package com.iurie.etfwatch.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import com.iurie.etfwatch.ui.detail.DetailScreen
import com.iurie.etfwatch.ui.hamilton.HamiltonScreen
import com.iurie.etfwatch.ui.home.HomeScreen
import com.iurie.etfwatch.ui.leveraged.LeveragedScreen
import com.iurie.etfwatch.ui.settings.SettingsScreen
import com.iurie.etfwatch.ui.watchlist.WatchlistScreen
import androidx.compose.material3.MaterialTheme

sealed class TopRoute(val route: String, val label: String, val icon: ImageVector) {
    data object Home : TopRoute("home", "Home", Icons.Filled.Home)
    data object Watchlist : TopRoute("watchlist", "Watchlist", Icons.Filled.Star)
    data object Hamilton : TopRoute("hamilton", "Hamilton", Icons.Filled.MonetizationOn)
    data object Leveraged : TopRoute("leveraged", "Leveraged", Icons.Filled.BarChart)
    data object Settings : TopRoute("settings", "Settings", Icons.Filled.Settings)
}

private val TopRoutes = listOf(
    TopRoute.Home,
    TopRoute.Watchlist,
    TopRoute.Hamilton,
    TopRoute.Leveraged,
    TopRoute.Settings,
)

@Composable
fun AppRoot() {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    fun navigateToTab(route: String) {
        nav.navigate(route) {
            popUpTo(nav.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in TopRoutes.map { it.route }) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp,
                ) {
                    TopRoutes.forEach { item ->
                        NavigationBarItem(
                            selected = backStack?.destination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = { navigateToTab(item.route) },
                            icon = { Icon(item.icon, item.label) },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            ),
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = TopRoute.Home.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(TopRoute.Home.route) {
                HomeScreen(
                    onNavigate = { navigateToTab(it) },
                    onOpenDetail = { nav.navigate("detail/$it") },
                )
            }
            composable(TopRoute.Watchlist.route) { WatchlistScreen(onOpenDetail = { nav.navigate("detail/$it") }) }
            composable(TopRoute.Hamilton.route) { HamiltonScreen(onOpenDetail = { nav.navigate("detail/$it") }) }
            composable(TopRoute.Leveraged.route) { LeveragedScreen(onOpenDetail = { nav.navigate("detail/$it") }) }
            composable(TopRoute.Settings.route) { SettingsScreen() }
            composable(
                route = "detail/{ticker}",
                deepLinks = listOf(navDeepLink { uriPattern = "etfwatch://detail/{ticker}" }),
            ) { entry ->
                val ticker = entry.arguments?.getString("ticker").orEmpty()
                DetailScreen(ticker = ticker, onBack = { nav.popBackStack() })
            }
        }
    }
}
