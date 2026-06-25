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
import com.tvanime.app.ui.screens.WebViewBrowserScreen
import com.tvanime.app.ui.screens.PlayerScreen
import androidx.activity.compose.BackHandler
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
    navController: NavHostController = androidx.navigation.compose.rememberNavController(),
    permissionsGranted: Boolean = true
) {
    NavHost(
        navController = navController,
        startDestination = "browser"
    ) {
        composable("browser") {
            WebViewBrowserScreen(
                initialUrl = null,
                onBack = { },
                onPlayVideo = { videoUrl, headers ->
                    val headersParam = encodeHeaders(headers).takeIf { it.isNotBlank() }
                        ?.let { "?headers=${Uri.encode(it)}" }
                        .orEmpty()
                    navController.navigate("player/${Uri.encode(videoUrl)}$headersParam")
                }
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
