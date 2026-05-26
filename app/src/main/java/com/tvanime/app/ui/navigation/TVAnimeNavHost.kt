package com.tvanime.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.tvanime.app.ui.screens.HomeScreen
import com.tvanime.app.ui.screens.DetailScreen
import com.tvanime.app.ui.screens.PlayerScreen
import com.tvanime.app.ui.viewmodel.DetailViewModel
import com.tvanime.app.ui.viewmodel.HomeViewModel
import androidx.activity.compose.BackHandler
import com.tvanime.app.domain.model.ContentItem

/**
 * NavHost principal de la app Android TV.
 * Rutas:
 *  - home               → catálogo principal
 *  - detail/{contentId} → detalle / botón reproducir
 *  - player/{videoUrl}  → pantalla de reproducción Media3
 */
@Composable
fun TVAnimeNavHost(
    navController: NavHostController = androidx.navigation.compose.rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        // ── HOME ──────────────────────────────────────────────────────────
        composable("home") {
            val vm: HomeViewModel = hiltViewModel()
            val uiState by vm.uiState.collectAsState()

            HomeScreen(
                catalog = uiState.catalog,
                onContentSelected = { item: ContentItem ->
                    navController.navigate("detail/${Uri.encode(item.id)}")
                }
            )
        }

        // ── DETAIL ────────────────────────────────────────────────────────
        composable(
            route = "detail/{contentId}",
            arguments = listOf(navArgument("contentId") { type = NavType.StringType })
        ) {
            val vm: DetailViewModel = hiltViewModel()
            val uiState by vm.uiState.collectAsState()

            // Botón atrás por hardware / control remoto
            BackHandler { navController.popBackStack() }

            DetailScreen(
                contentItem = (uiState as? com.tvanime.app.ui.viewmodel.DetailUiState.Ready)?.item,
                isFavorite = (uiState as? com.tvanime.app.ui.viewmodel.DetailUiState.Ready)?.isFavorite ?: false,
                onPlayClick = {
                    val videoUrl = (uiState as? com.tvanime.app.ui.viewmodel.DetailUiState.Ready)
                        ?.item?.videoUrl
                    if (!videoUrl.isNullOrBlank()) {
                        navController.navigate("player/${Uri.encode(videoUrl)}")
                    }
                },
                onToggleFavorite = {
                    val currentFav = (uiState as? com.tvanime.app.ui.viewmodel.DetailUiState.Ready)
                        ?.isFavorite ?: false
                    vm.onToggleFavorite(currentFav)
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ── PLAYER ────────────────────────────────────────────────────────
        composable("player/{videoUrl}") { backStackEntry ->
            val videoUrl = Uri.decode(backStackEntry.arguments?.getString("videoUrl").orEmpty())
            BackHandler { navController.popBackStack() }
            PlayerScreen(videoUrl = videoUrl)
        }
    }
}
