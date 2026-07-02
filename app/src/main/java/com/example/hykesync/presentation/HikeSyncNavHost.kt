package com.example.hykesync.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

@Composable
fun HikeSyncNavHost(viewModel: AppViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = HikeSyncRoute.Dashboard.route,
    ) {
        composable(HikeSyncRoute.Dashboard.route) {
            AppDashboardScreen(
                viewModel = viewModel,
                selectedTab = MainTab.Dashboard,
                onNavigateTo = { tab -> navController.navigateToMainTab(tab) },
            )
        }

        composable(HikeSyncRoute.History.route) {
            RouteHistoryScreen(
                routeHistory = uiState.routeHistory,
                onOpenRouteDetail = { sessionId ->
                    navController.navigate(HikeSyncRoute.routeDetail(sessionId))
                },
                selectedTab = MainTab.History,
                onNavigateTo = { tab -> navController.navigateToMainTab(tab) },
            )
        }

        composable(HikeSyncRoute.Devices.route) {
            LinkedDevicesScreen(
                uiState = uiState,
                selectedTab = MainTab.Devices,
                onNavigateTo = { tab -> navController.navigateToMainTab(tab) },
            )
        }

        composable(
            route = HikeSyncRoute.RouteDetail.route,
            arguments = listOf(navArgument(HikeSyncRoute.ARG_SESSION_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong(HikeSyncRoute.ARG_SESSION_ID) ?: 0L
            LaunchedEffect(sessionId) {
                viewModel.loadRouteDetail(sessionId)
            }

            val routeDetail = uiState.selectedRouteDetail
            when {
                uiState.isRouteDetailLoading -> LoadingRouteScreen()
                routeDetail != null && routeDetail.sessionId == sessionId -> {
                    RouteDetailScreen(
                        routeDetail = routeDetail,
                        onBack = {
                            viewModel.clearRouteDetail()
                            navController.popBackStack()
                        },
                        onOpenMetrics = {
                            navController.navigate(HikeSyncRoute.hikingMetrics(sessionId))
                        },
                    )
                }
                uiState.routeDetailError != null -> EmptyStateScreen(
                    title = "No se pudo abrir la ruta",
                    message = uiState.routeDetailError ?: "Sin detalles disponibles",
                    onBack = {
                        viewModel.clearRouteDetail()
                        navController.popBackStack()
                    },
                )
                else -> LoadingRouteScreen()
            }
        }

        composable(
            route = HikeSyncRoute.HikingMetrics.route,
            arguments = listOf(navArgument(HikeSyncRoute.ARG_SESSION_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getLong(HikeSyncRoute.ARG_SESSION_ID) ?: 0L
            LaunchedEffect(sessionId) {
                if (uiState.selectedRouteDetail?.sessionId != sessionId) {
                    viewModel.loadRouteDetail(sessionId)
                }
            }

            val routeDetail = uiState.selectedRouteDetail
            when {
                uiState.isRouteDetailLoading -> LoadingRouteScreen()
                routeDetail != null && routeDetail.sessionId == sessionId -> {
                    HikingMetricsScreen(
                        routeDetail = routeDetail,
                        onBack = { navController.popBackStack() },
                    )
                }
                uiState.routeDetailError != null -> EmptyStateScreen(
                    title = "Metricas no disponibles",
                    message = uiState.routeDetailError ?: "No se pudieron cargar las metricas",
                    onBack = {
                        viewModel.clearRouteDetail()
                        navController.popBackStack()
                    },
                )
                else -> LoadingRouteScreen()
            }
        }
    }
}

private fun androidx.navigation.NavHostController.navigateToMainTab(tab: MainTab) {
    val target = when (tab) {
        MainTab.Dashboard -> HikeSyncRoute.Dashboard.route
        MainTab.History -> HikeSyncRoute.History.route
        MainTab.Devices -> HikeSyncRoute.Devices.route
    }

    navigate(target) {
        popUpTo(graph.startDestinationId) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}

private sealed class HikeSyncRoute(val route: String) {
    data object Dashboard : HikeSyncRoute("dashboard")
    data object History : HikeSyncRoute("history")
    data object Devices : HikeSyncRoute("devices")
    data object RouteDetail : HikeSyncRoute("route_detail/{sessionId}")
    data object HikingMetrics : HikeSyncRoute("hiking_metrics/{sessionId}")

    companion object {
        const val ARG_SESSION_ID = "sessionId"
        fun routeDetail(sessionId: Long): String = "route_detail/$sessionId"
        fun hikingMetrics(sessionId: Long): String = "hiking_metrics/$sessionId"
    }
}
