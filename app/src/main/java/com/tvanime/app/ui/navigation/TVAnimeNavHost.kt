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
import com.tvanime.app.ui.screens.ExtractMediaScreen
import com.tvanime.app.ui.screens.PlayerScreen
import com.tvanime.app.ui.screens.SettingsScreen
import com.tvanime.app.ui.viewmodel.DetailViewModel
import com.tvanime.app.ui.viewmodel.ExtractMediaViewModel
import com.tvanime.app.ui.viewmodel.HomeViewModel
import com.tvanime.app.ui.viewmodel.SettingsViewModel
import androidx.activity.compose.BackHandler
import com.tvanime.app.domain.model.ContentItem

private fun parseHeadersFromRoute(decoded: String): Map<String, String> {
    val headersPart = decoded.substringAfter("&headers=", "")
    if (headersPart.isBlank()) return emptyMap()
    return headersPart.split(",").mapNotNull { pair ->
        val key = pair.substringBefore("=", "").trim()
        val value = pair.substringAfter("=", "").trim()
        if (key.isNotBlank() && value.isNotBlank()) key to value else null
    }.toMap()
}

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
                onOpenExtractor = { navController.navigate("extract") },
                onOpenSettings = { navController.navigate("settings") },
                onContentSelected = { item: ContentItem ->
                    navController.navigate("detail/${Uri.encode(item.id)}")
                }
            )
        }

        composable("extract") {
            val vm: ExtractMediaViewModel = hiltViewModel()
            val uiState by vm.uiState.collectAsState()

            BackHandler { navController.popBackStack() }

            ExtractMediaScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onUrlChanged = vm::updatePageUrl,
                onExtract = vm::extract,
                onPlayCandidate = { candidateUrl, headers ->
                    val headersParam = if (headers.isNotEmpty()) {
                        "&headers=" + Uri.encode(headers.entries.joinToString(",") { "${it.key}=${it.value}" })
                    } else ""
                    navController.navigate("player/${Uri.encode(candidateUrl)}$headersParam")
                },
                onSearchQueryChanged = vm::updateSearchQuery,
                onSiteSelected = vm::selectSite,
                onAutoAnalyze = vm::autoAnalyzePopularSites,
                onToggleSuggestions = vm::toggleSuggestions
            )
        }

        composable("settings") {
            val vm: SettingsViewModel = hiltViewModel()
            val uiState by vm.uiState.collectAsState()

            BackHandler { navController.popBackStack() }

            SettingsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onSelectDemo = { vm.selectSource(com.tvanime.app.data.settings.PlaylistSource.DEMO) },
                onSelectRemoteUrl = { vm.selectSource(com.tvanime.app.data.settings.PlaylistSource.REMOTE_URL) },
                onRemoteUrlChanged = vm::updateRemoteUrl,
                onRecurringSitesChanged = vm::updateRecurringSites,
                onSave = vm::save
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
            val rawArg = backStackEntry.arguments?.getString("videoUrl").orEmpty()
            val decoded = Uri.decode(rawArg)
            val headers = parseHeadersFromRoute(decoded)
            val videoUrl = decoded.substringBefore("&headers=")
            BackHandler { navController.popBackStack() }
            PlayerScreen(videoUrl = videoUrl, headers = headers)
        }
    }
}
