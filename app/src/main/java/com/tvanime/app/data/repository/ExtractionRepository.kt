package com.tvanime.app.data.repository

import com.tvanime.app.domain.model.ExtractionResult

interface ExtractionRepository {
    suspend fun extractFromPage(pageUrl: String): ExtractionResult
}
