package com.tvanime.app.domain.usecase

import com.tvanime.app.data.repository.ExtractionRepository

class ExtractMediaFromPageUseCase(
    private val extractionRepository: ExtractionRepository
) {
    suspend operator fun invoke(pageUrl: String) = extractionRepository.extractFromPage(pageUrl)
}
