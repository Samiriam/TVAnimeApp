package com.tvanime.app.ui.navigation

import android.net.Uri
import android.util.Base64
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
import com.tvanime.app.ui.screens.WebViewBrowserScreen
import com.tvanime.app.ui.screens.PlayerScreen
import com.tvanime.app.ui.screens.SettingsScreen
import com.tvanime.app.ui.viewmodel.CrawlerViewModel
import com.tvanime.app.ui.viewmodel.DetailViewModel
import com.tvanime.app.ui.viewmodel.HomeViewModel
import com.tvanime.app.ui.viewmodel.SettingsViewModel
import androidx.activity.compose.BackHandler
import com.tvanime.app.domain.model.ContentItem
import org.json.JSONObject

private fun encodeHeaders(headers: Map<String, String>): String {
    if (headers.isEmpty()) return ""
    val json = JSONObject()
    headers.forEach { (key, value) -> json.put(key, value) }
    return Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP)
}

private fun decodeHeaders(encoded: String?): Map<String, String> {
    if (encoded.isNullOrBlank()) return emptyMap()
    return runCatching {
        val json = JSONObject(String(Base64.decode(encoded, Base64.URL_SAFE), Charsets.UTF_8))
        json.keys().asSequence().associateWith { json.optString(it) }
            .filterValues { it.isNotBlank() }
    }.getOrDefault(emptyMap())
}

@Composable
fun TVAnimeNavHost(
    navController: NavHostController = androidx.navigation.compose.rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = "home"
    ) {
        composable("home") {
            val vm: HomeViewModel = hiltViewModel()
            val uiState by vm.uiState.collectAsState()

            HomeScreen(
                catalog = uiState.catalog,
                onOpenBrowser = { navController.navigate("browser") },
                onOpenSettings = { navController.navigate("settings") },
                onContentSelected = { item: ContentItem ->
                    navController.navigate("detail/${Uri.encode(item.id)}")
                }
            )
        }

        composable("browser") {
            BackHandler { navController.popBackStack() }

            WebViewBrowserScreen(
                initialUrl = null,
                onBack = { navController.popBackStack() },
                onPlayVideo = { videoUrl, headers ->
                    val headersParam = encodeHeaders(headers).takeIf { it.isNotBlank() }
                        ?.let { "?headers=${Uri.encode(it)}" }
                        .orEmpty()
                    navController.navigate("player/${Uri.encode(videoUrl)}$headersParam")
                }
            )
        }

        composable("settings") {
            val settingsVm: SettingsViewModel = hiltViewModel()
            val crawlerVm: CrawlerViewModel = hiltViewModel()
            val uiState by settingsVm.uiState.collectAsState()
            val crawlerState by crawlerVm.uiState.collectAsState()

            BackHandler { navController.popBackStack() }

            SettingsScreen(
                uiState = uiState,
                crawlerState = crawlerState,
                onBack = { navController.popBackStack() },
                onSelectDemo = { settingsVm.selectSource(com.tvanime.app.data.settings.PlaylistSource.DEMO) },
                onSelectRemoteUrl = { settingsVm.selectSource(com.tvanime.app.data.settings.PlaylistSource.REMOTE_URL) },
                onRemoteUrlChanged = settingsVm::updateRemoteUrl,
                onSave = settingsVm::save,
                onToggleCategory = crawlerVm::toggleCategory
            )
        }

        composable(
            route = "detail/{contentId}",
            arguments = listOf(navArgument("contentId") { type = NavType.StringType })
        ) {
            val vm: DetailViewModel = hiltViewModel()
            val uiState by vm.uiState.collectAsState()

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

        composable(
            route = "player/{videoUrl}?headers={headers}",
            arguments = listOf(
                navArgument("videoUrl") { type = NavType.StringType },
                navArgument("headers") { type = NavType.StringType; nullable = true; defaultValue = null }
            )
        ) { backStackEntry ->
            val rawArg = backStackEntry.arguments?.getString("videoUrl").orEmpty()
            val decoded = Uri.decode(rawArg)
            val headers = decodeHeaders(backStackEntry.arguments?.getString("headers"))
            val videoUrl = decoded
            BackHandler { navController.popBackStack() }
            PlayerScreen(videoUrl = videoUrl, headers = headers)
        }
    }
}
