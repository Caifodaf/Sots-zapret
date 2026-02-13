package domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class MachineInfo(
    @SerialName("MachineGuid")
    val machineGuid: String,
    @SerialName("UserName")
    val userName: String
) 