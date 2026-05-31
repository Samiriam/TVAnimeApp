package com.tvanime.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebSearchSuggestions(
    query: String,
    onSiteSelected: (SiteSuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredSites = remember(query) {
        if (query.isBlank()) {
            POPULAR_SITES
        } else {
            POPULAR_SITES.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.category.contains(query, ignoreCase = true) ||
                it.url.contains(query, ignoreCase = true)
            }
        }
    }

    val groupedSites = remember(filteredSites) {
        filteredSites.groupBy { it.category }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedSites.forEach { (category, sites) ->
            item {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(sites) { site ->
                SiteSuggestionCard(
                    site = site,
                    onClick = { onSiteSelected(site) }
                )
            }
        }
    }
}

@Composable
private fun SiteSuggestionCard(
    site: SiteSuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = site.icon,
                style = MaterialTheme.typography.headlineMedium
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = site.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = site.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}
