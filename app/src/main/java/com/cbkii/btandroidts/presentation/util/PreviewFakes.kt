package com.cbkii.btandroidts.presentation.util

import com.cbkii.btandroidts.domain.bluetooth.enums.BluetoothDeviceType
import com.cbkii.btandroidts.domain.bluetooth.enums.BluetoothMode
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEConnectionState
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEPermission
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEPropertyTypes
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEServicesTypes
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEWriteTypes
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLECharacteristicsModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEDescriptorModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEServiceModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BluetoothLEDeviceModel
import com.cbkii.btandroidts.domain.settings.enums.BLEScanPeriodTimmings
import com.cbkii.btandroidts.domain.settings.enums.BLESettingsScanMode
import com.cbkii.btandroidts.domain.settings.enums.BLESettingsSupportedLayer
import com.cbkii.btandroidts.domain.settings.models.BLESettingsModel
import com.cbkii.btandroidts.domain.settings.models.BTSettingsModel
import com.cbkii.btandroidts.presentation.feature_connect.bt_profile.state.BTProfileScreenState
import com.cbkii.btandroidts.presentation.feature_devices.state.BTDevicesScreenState
import com.cbkii.btandroidts.presentation.feature_le_connect.state.BLEDeviceProfileState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import java.util.UUID


object PreviewFakes {

	val ANDROID_NAME_AS_BYTEARRAY = "Android".encodeToByteArray()

	val FAKE_DEVICE_MODEL = BluetoothDeviceModel(
		name = "Android HeadSet",
		address = "00:11:22:33:FF:EE",
		mode = BluetoothMode.BLUETOOTH_DEVICE_CLASSIC,
		type = BluetoothDeviceType.AUDIO_VIDEO
	)

	val FAKE_BLE_DEVICE_MODEL = BluetoothLEDeviceModel(
		deviceName = "Android Headset", deviceModel = FAKE_DEVICE_MODEL
	)


	val FAKE_DEVICE_STATE_WITH_NO_DEVICE = BTDevicesScreenState()

	val FAKE_DEVICE_STATE_WITH_PAIRED_DEVICE = BTDevicesScreenState(
		pairedDevices = List(3) { FAKE_DEVICE_MODEL }.toPersistentList()
	)

	val FAKE_DEVICE_STATE_WITH_PAIRED_AND_AVAILABLE_DEVICES = BTDevicesScreenState(
		pairedDevices = List(3) { FAKE_DEVICE_MODEL }.toPersistentList(),
		availableDevices = List(2) { FAKE_DEVICE_MODEL }.toPersistentList()
	)

	val FAKE_DEVICE_STATE_WITH_SOME_BLE_DEVICES = BTDevicesScreenState(
		leDevices = List(2) { FAKE_BLE_DEVICE_MODEL }.toPersistentList()
	)

	val FAKE_BLE_SERVICE = BLEServiceModel(
		serviceId = 1,
		serviceUUID = UUID.fromString("10297702-35bd-4fda-a904-1e693390e08a"),
		serviceType = BLEServicesTypes.PRIMARY
	)

	val FAKE_BLE_PROFILE_STATE = BLEDeviceProfileState(
		device = FAKE_DEVICE_MODEL,
		connectionState = BLEConnectionState.CONNECTED,
		signalStrength = 100,
		services = List(4) { FAKE_BLE_SERVICE }.toImmutableList()
	)

	val FAKE_BLE_PROFILE_STATE_CONNECTING = BLEDeviceProfileState(
		device = FAKE_DEVICE_MODEL,
		connectionState = BLEConnectionState.CONNECTING,
		signalStrength = 100,
	)

	val FAKE_BLE_DESCRIPTOR_MODEL = BLEDescriptorModel(
		uuid = UUID.fromString("10297702-35bd-4fda-a904-1e693390e08a"),
		permissions = persistentListOf(BLEPermission.PERMISSION_WRITE),
	)

	val FAKE_BLE_DESCRIPTOR_MODEL_WITH_VALUE = BLEDescriptorModel(
		uuid = UUID.fromString("10297702-35bd-4fda-a904-1e693390e08a"),
		permissions = persistentListOf(BLEPermission.PERMISSION_READ),
		byteArray = byteArrayOf(0x4B, 0x6F, 0x6C, 0x74, 0x69, 0x6E)
	)

	val FAKE_BLE_DESCRIPTOR_WITH_ENABLE_NOTIFICATION_VALUE = BLEDescriptorModel(
		uuid = UUID.fromString("10297702-35bd-4fda-a904-1e693390e08a"),
		permissions = persistentListOf(BLEPermission.PERMISSION_READ),
		byteArray = byteArrayOf(0x00, 0x00)
	)

	val FAKE_BLE_CHARACTERISTIC_MODEL = BLECharacteristicsModel(
		instanceId = 1,
		uuid = UUID.fromString("10297702-35bd-4fda-a904-1e693390e08a"),
		permission = BLEPermission.PERMISSION_WRITE,
		writeType = BLEWriteTypes.TYPE_UNKNOWN,
		properties = persistentListOf(
			BLEPropertyTypes.PROPERTY_WRITE,
			BLEPropertyTypes.PROPERTY_READ,
			BLEPropertyTypes.PROPERTY_INDICATE,
			BLEPropertyTypes.PROPERTY_NOTIFY

		),
		descriptors = listOf(FAKE_BLE_DESCRIPTOR_MODEL).toPersistentList(),
		probableName = "Compose"
	)

	val FAKE_BLE_CHARACTERISTIC_MODEL_WITH_DATA = BLECharacteristicsModel(
		instanceId = 1,
		uuid = UUID.fromString("10297702-35bd-4fda-a904-1e693390e08a"),
		permission = BLEPermission.PERMISSION_WRITE,
		writeType = BLEWriteTypes.TYPE_UNKNOWN,
		properties = persistentListOf(
			BLEPropertyTypes.PROPERTY_WRITE,
			BLEPropertyTypes.PROPERTY_READ,
			BLEPropertyTypes.PROPERTY_INDICATE,

			),
		descriptors = listOf(
			FAKE_BLE_DESCRIPTOR_MODEL,
			FAKE_BLE_DESCRIPTOR_MODEL_WITH_VALUE
		).toPersistentList(),
		byteArray = ANDROID_NAME_AS_BYTEARRAY,
		probableName = "Compose"
	)

	val FAKE_SERVICE_WITH_CHARACTERISTICS = BLEServiceModel(
		serviceId = 1,
		serviceUUID = UUID.fromString("10297702-35bd-4fda-a904-1e693390e08a"),
		serviceType = BLEServicesTypes.SECONDARY,
		characteristics = persistentListOf(
			FAKE_BLE_CHARACTERISTIC_MODEL,
			FAKE_BLE_CHARACTERISTIC_MODEL_WITH_DATA
		)
	)

	val FAKE_UUID_LIST = List(10) {
		UUID.fromString("10297702-35bd-4fda-a904-1e693390e08a")
	}

	val FAKE_BT_DEVICE_PROFILE = BTProfileScreenState(
		isDiscovering = false,
		deviceUUIDS = FAKE_UUID_LIST.toImmutableList()
	)

	val FAKE_BLE_SETTINGS = BLESettingsModel(
		scanPeriod = BLEScanPeriodTimmings.FIVE_MINUTES,
		supportedLayer = BLESettingsSupportedLayer.LONG_RANGE,
		isLegacyOnly = true
	)

	val FAKE_BLE_SETTINGS_2 = BLESettingsModel(
		scanPeriod = BLEScanPeriodTimmings.TWELVE_SECONDS,
		supportedLayer = BLESettingsSupportedLayer.ALL,
		scanMode = BLESettingsScanMode.LOW_POWER,
		isLegacyOnly = false
	)

	val FAKE_BT_SETTINGS = BTSettingsModel()
}