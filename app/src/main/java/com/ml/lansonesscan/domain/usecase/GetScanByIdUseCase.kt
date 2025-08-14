package com.ml.lansonesscan.domain.usecase

import com.ml.lansonesscan.domain.model.ScanResult
import com.ml.lansonesscan.domain.repository.ScanRepository

/**
 * Use case for retrieving a single scan result by its ID
 */
class GetScanByIdUseCase(
    private val scanRepository: ScanRepository
) {
    /**
     * Retrieves a scan result by its unique identifier
     * @param scanId The ID of the scan to retrieve
     * @return Result containing the scan result or null if not found, or an error
     */
    suspend operator fun invoke(scanId: String): Result<ScanResult?> {
        return try {
            if (scanId.isBlank()) {
                return Result.failure(IllegalArgumentException("Scan ID cannot be blank"))
            }
            val result = scanRepository.getScanById(scanId)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
