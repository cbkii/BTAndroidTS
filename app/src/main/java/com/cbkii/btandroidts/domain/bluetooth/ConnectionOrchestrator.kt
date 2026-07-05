package com.cbkii.btandroidts.domain.bluetooth

import java.util.UUID

enum class ConnectionRouteStrategy {
    HID_HOST,
    OPP_FILE_TRANSFER,
    RFCOMM_TERMINAL,
    BLE_GATT,
    UNKNOWN
}

object ConnectionOrchestrator {
    fun determineStrategy(uuid: UUID): ConnectionRouteStrategy {
        val shortUuid = String.format("%04X", (uuid.mostSignificantBits ushr 32) and 0xFFFF)
        return when (shortUuid) {
            "1124", "112D" -> ConnectionRouteStrategy.HID_HOST
            "1105", "1106" -> ConnectionRouteStrategy.OPP_FILE_TRANSFER
            "1101" -> ConnectionRouteStrategy.RFCOMM_TERMINAL
            else -> ConnectionRouteStrategy.UNKNOWN
        }
    }

    fun getBestMethod(uuids: List<UUID>): UUID? {
        // Priority: HID > OPP > RFCOMM
        val mapped = uuids.map { it to determineStrategy(it) }.toMap()

        return mapped.entries.firstOrNull { it.value == ConnectionRouteStrategy.HID_HOST }?.key
            ?: mapped.entries.firstOrNull { it.value == ConnectionRouteStrategy.OPP_FILE_TRANSFER }?.key
            ?: mapped.entries.firstOrNull { it.value == ConnectionRouteStrategy.RFCOMM_TERMINAL }?.key
            ?: uuids.firstOrNull()
    }
}
