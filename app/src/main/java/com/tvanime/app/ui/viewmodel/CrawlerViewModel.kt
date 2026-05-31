package com.tvanime.app.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tvanime.app.data.local.dao.CrawlCategoryDao
import com.tvanime.app.data.local.entity.CrawlCategoryEntity
import com.tvanime.app.domain.model.CategoryConfig
import com.tvanime.app.worker.CrawlWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CrawlerUiState(
    val categories: List<CrawlCategoryEntity> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class CrawlerViewModel @Inject constructor(
    private val crawlCategoryDao: CrawlCategoryDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(CrawlerUiState())
    val uiState: StateFlow<CrawlerUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val defaultCats = CategoryConfig.DEFAULT.map { config ->
                CrawlCategoryEntity(
                    category = config.category,
                    enabled = true,
                    sites = "[]",
                    lastCrawledAt = 0L,
                    crawlIntervalHours = 6
                )
            }

            crawlCategoryDao.observeAll().collect { cats ->
                val allCats = if (cats.isEmpty()) {
                    defaultCats
                } else {
                    val existingCats = cats.associateBy { it.category }
                    defaultCats.map { d -> existingCats[d.category] ?: d }
                }

                _uiState.value = _uiState.value.copy(
                    categories = allCats,
                    isLoading = false
                )
            }
        }
    }

    fun toggleCategory(category: String, enabled: Boolean) {
        viewModelScope.launch {
            val existing = _uiState.value.categories.find { it.category == category }
            if (existing != null) {
                crawlCategoryDao.setEnabled(category, enabled)
            } else {
                crawlCategoryDao.insert(
                    CrawlCategoryEntity(
                        category = category,
                        enabled = enabled,
                        sites = "[]",
                        lastCrawledAt = 0L,
                        crawlIntervalHours = 6
                    )
                )
            }

            val anyEnabled = _uiState.value.categories
                .filter { it.category != category }
                .any { it.enabled } || enabled

            if (anyEnabled) {
                CrawlWorker.schedule(context)
            } else {
                CrawlWorker.cancel(context)
            }
        }
    }
}