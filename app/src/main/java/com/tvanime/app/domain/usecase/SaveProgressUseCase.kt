package com.tvanime.app.domain.usecase

import com.tvanime.app.data.repository.HistoryRepository

/**
 * Guarda el progreso de reproducción.
 */
class SaveProgressUseCase(private val historyRepo: HistoryRepository) {
    suspend operator fun invoke(contentId: String, positionMs: Long, durationMs: Long) =
        historyRepo.saveProgress(contentId, positionMs, durationMs)
}
