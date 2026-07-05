package com.cbkii.btandroidts.domain.bluetooth

import com.cbkii.btandroidts.R
import java.util.Locale
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
        val shortUuid = String.format(Locale.ROOT, "%04X", (uuid.mostSignificantBits ushr 32) and 0xFFFF)
        return when (shortUuid) {
            "1124" -> ConnectionRouteStrategy.HID_HOST
            "1105", "1106" -> ConnectionRouteStrategy.OPP_FILE_TRANSFER
            "1101" -> ConnectionRouteStrategy.RFCOMM_TERMINAL
            else -> ConnectionRouteStrategy.UNKNOWN
        }
    }

    fun getHumanReadableNameRes(uuid: UUID): Int {
        if (uuid.toString() == "3fe6c764-029f-48f0-a2d0-a43d9b1df5c8") {
            return R.string.profile_btandroidts_internal
        }
        val shortUuid = String.format(Locale.ROOT, "%04X", (uuid.mostSignificantBits ushr 32) and 0xFFFF)
        return when (shortUuid) {
            "1101" -> R.string.bl_connect_profile_server_uuid_text
            "1124" -> R.string.profile_hid
            "1105", "1106" -> R.string.profile_opp
            "110A" -> R.string.profile_a2dp_source
            "110B" -> R.string.profile_a2dp_sink
            "111E" -> R.string.profile_hfp
            "112F" -> R.string.profile_pbap
            "112D" -> R.string.profile_sim_access
            else -> R.string.profile_unknown
        }
    }

    fun getBestMethod(uuids: List<UUID>): UUID? {
        val mapped = uuids.associateWith { determineStrategy(it) }

        return mapped.entries.firstOrNull { it.value == ConnectionRouteStrategy.HID_HOST }?.key
            ?: mapped.entries.firstOrNull { it.value == ConnectionRouteStrategy.OPP_FILE_TRANSFER }?.key
            ?: mapped.entries.firstOrNull { it.value == ConnectionRouteStrategy.RFCOMM_TERMINAL }?.key
    }
}
