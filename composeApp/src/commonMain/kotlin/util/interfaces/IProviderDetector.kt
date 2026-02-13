package util.interfaces

import domain.model.ProviderInfo

interface IProviderDetector {
    suspend fun detectProvider(): Result<ProviderInfo>
} 