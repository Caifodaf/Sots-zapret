package domain

import data.service.ServiceManagerImpl

interface ServiceManager {
    suspend fun checkServiceStatus(serviceName: String): Result<ServiceManagerImpl.ServiceStatusRequest>
    suspend fun serviceCreateStart(selectedProfileFileName: String)
    suspend fun serviceShutdown(serviceName: String)
    suspend fun startOrRecreateService(profileName: String)
    suspend fun restartService(profileName: String)
    suspend fun isAnyServiceRunning(serviceNames: List<String>): Boolean
}