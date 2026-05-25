package com.tvanime.app.domain.usecase

import com.tvanime.app.data.repository.HistoryRepository

/**
 * Recupera el historial de reproduccion.
 */
class GetHistoryUseCase(private val historyRepo: HistoryRepository) {
    operator fun invoke() = historyRepo.observeAll()
}
