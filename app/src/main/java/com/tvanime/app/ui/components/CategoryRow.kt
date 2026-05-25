package com.tvanime.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tvanime.app.domain.model.ContentItem

@Composable
fun CategoryRow(
    title: String,
    items: List<ContentItem>,
    onItemSelected: (ContentItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 32.dp, top = 20.dp, bottom = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(items) { item ->
                ContentCard(item = item, onClick = { onItemSelected(item) })
            }
        }
    }
}

@Composable
fun DetailScreen(
    title: String,
    description: String,
    backdropUrl: String,
    genres: List<String>,
    year: Int,
    rating: Float,
    onPlayClick: () -> Unit
) {
    // TODO: implementar diseño de detalle con backdrop y sinopsis
    androidx.compose.material3.Text(text = "Detalle: $title — $year")
}
