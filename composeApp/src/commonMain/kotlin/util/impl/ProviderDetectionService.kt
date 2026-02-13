package util.impl

import domain.model.ProviderInfo
import util.interfaces.IProviderDetector

class ProviderDetectionService(
    private val detector: IProviderDetector
) {
    suspend fun getProviderInfo(): Result<ProviderInfo> = detector.detectProvider()
} 