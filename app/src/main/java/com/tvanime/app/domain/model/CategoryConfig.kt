package com.tvanime.app.domain.model

data class SiteConfig(
    val name: String,
    val baseUrl: String,
    val category: String,
    val listSelector: String = ".item, .movie, .video, article",
    val titleSelector: String = "h3, .title, .name, a[title]",
    val thumbnailSelector: String = "img[data-src], img[src]",
    val detailUrlSelector: String = "a[href]",
    val yearSelector: String = ".year, .date, span",
    val ratingSelector: String = ".rating, .score"
)

data class CategoryConfig(
    val category: String,
    val label: String,
    val icon: String,
    val mediaType: String,
    val sites: List<SiteConfig> = emptyList()
) {
    companion object {
        val DEFAULT = listOf(
            CategoryConfig("anime", "Anime", "star", "anime"),
            CategoryConfig("movies", "Peliculas", "movie", "movie"),
            CategoryConfig("series", "Series", "tv", "series"),
            CategoryConfig("documentaries", "Documentales", "menu_book", "documentary")
        )
    }
}