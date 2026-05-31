package com.tvanime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tvanime.app.ui.theme.FocusBg
import com.tvanime.app.ui.theme.FocusCyan
import com.tvanime.app.ui.theme.FocusGlow

data class SiteSuggestion(
    val name: String,
    val url: String,
    val category: String,
    val icon: String = "🌐"
)

val POPULAR_SITES = listOf(
    SiteSuggestion("AnimeFLV", "https://www3.animeflv.net", "Anime", "🎌"),
    SiteSuggestion("JKAnime", "https://jkanime.net", "Anime", "🎌"),
    SiteSuggestion("AnimeFenix", "https://www.animefenix.tv", "Anime", "🎌"),
    SiteSuggestion("MonosChinos", "https://monoschinos2.com", "Anime", "🎌"),
    SiteSuggestion("TioAnime", "https://tioanime.com", "Anime", "🎌"),
    SiteSuggestion("Cuevana3", "https://cuevana3.ch", "Películas", "🎬"),
    SiteSuggestion("PelisPlus", "https://pelisplus.me", "Películas", "🎬"),
    SiteSuggestion("Pelisflix", "https://pelisflix.media", "Películas", "🎬"),
    SiteSuggestion("SeriesFLV", "https://seriesflv.net", "Series", "📺"),
    SiteSuggestion("DoramasMP4", "https://doramasmmp4.com", "Doramas", "🎭"),
    SiteSuggestion("DoramasFlix", "https://doramaflix.com", "Doramas", "🎭"),
    SiteSuggestion("HentaiLA", "https://hentaila.com", "Hentai", "🔞"),
    SiteSuggestion("Archive.org", "https://archive.org", "Archivos", "📚"),
    SiteSuggestion("YouTube", "https://youtube.com", "Videos", "▶️")
)

@Composable
fun WebSearchSuggestions(
    query: String,
    onSiteSelected: (SiteSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredSites = remember(query) {
        if (query.isBlank()) POPULAR_SITES
        else POPULAR_SITES.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true) ||
            it.url.contains(query, ignoreCase = true)
        }
    }

    val groupedSites = remember(filteredSites) { filteredSites.groupBy { it.category } }

    LazyColumn(
        modifier = modifier.fillMaxWidth().heightIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        groupedSites.forEach { (category, sites) ->
            item {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }
            items(sites) { site ->
                SiteSuggestionCard(site = site, onClick = { onSiteSelected(site) })
            }
        }
    }
}

@Composable
private fun SiteSuggestionCard(site: SiteSuggestion, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick)
            .focusable().onFocusChanged { focused = it.isFocused }
            .scale(if (focused) 1.03f else 1f)
            .border(if (focused) 4.dp else 0.dp,
                if (focused) Brush.linearGradient(listOf(FocusCyan, FocusGlow)) else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                RoundedCornerShape(14.dp))
            .background(if (focused) FocusBg else Color.Transparent, RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (focused) 1f else 0.6f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = site.icon, style = MaterialTheme.typography.headlineMedium)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = site.name, style = MaterialTheme.typography.titleMedium,
                    color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(text = site.url, style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f))
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = if (focused) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.5f)
            )
        }
    }
}