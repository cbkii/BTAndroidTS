package com.cbkii.btandroidts.domain.bluetooth_le.models

import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEServicesTypes
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import java.util.UUID

data class BLEServiceModel(
	val serviceId: Int,
	val serviceUUID: UUID,
	val serviceType: BLEServicesTypes = BLEServicesTypes.UNKNOWN,
	val characteristics: PersistentList<BLECharacteristicsModel> = persistentListOf(),
	val probableName: String? = null,
) {
	val characteristicsCount: Int
		get() = characteristics.size
}