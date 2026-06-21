package com.cbkii.btandroidts.data.datastore

import android.content.Context
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import com.cbkii.btandroidts.domain.peripheral.BluetoothAddress
import com.cbkii.btandroidts.domain.peripheral.PeripheralPolicy
import com.cbkii.btandroidts.domain.peripheral.PeripheralPolicyStore
import com.cbkii.btandroidts.domain.peripheral.PeripheralRetryState
import com.cbkii.btandroidts.domain.peripheral.ProtectedPeripheralRecord
import com.cbkii.btandroidts.domain.peripheral.ReconnectPolicy
import com.cbkii.btandroidts.domain.peripheral.SavedPeripheralRecord
import com.google.protobuf.InvalidProtocolBufferException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.InputStream
import java.io.OutputStream

class PeripheralPolicyDataStore(
	private val context: Context,
) : PeripheralPolicyStore {

	override val policy: Flow<PeripheralPolicy>
		get() = context.peripheralPolicyDataStore.data.map(PeripheralPolicyPreferences::toDomain)

	override suspend fun currentPolicy(): PeripheralPolicy = policy.first()

	override suspend fun setSupervisionEnabled(enabled: Boolean) {
		context.peripheralPolicyDataStore.updateData { prefs ->
			prefs.toBuilder().setSupervisionEnabled(enabled).build()
		}
	}

	override suspend fun setSafeModeEnabled(enabled: Boolean) {
		context.peripheralPolicyDataStore.updateData { prefs ->
			prefs.toBuilder().setSafeModeEnabled(enabled).build()
		}
	}

	override suspend fun savePeripheral(device: SavedPeripheralRecord) {
		context.peripheralPolicyDataStore.updateData { prefs ->
			val records = prefs.savedPeripheralsList
				.filterNot { it.address == device.address.value } + device.toProto()
			prefs.toBuilder()
				.clearSavedPeripherals()
				.addAllSavedPeripherals(records)
				.build()
		}
	}

	override suspend fun removeSavedPeripheral(address: BluetoothAddress) {
		context.peripheralPolicyDataStore.updateData { prefs ->
			prefs.toBuilder()
				.clearSavedPeripherals()
				.addAllSavedPeripherals(prefs.savedPeripheralsList.filterNot { it.address == address.value })
				.clearRetryStates()
				.addAllRetryStates(prefs.retryStatesList.filterNot { it.address == address.value })
				.build()
		}
	}

	override suspend fun protectDevice(device: ProtectedPeripheralRecord) {
		context.peripheralPolicyDataStore.updateData { prefs ->
			val records = prefs.protectedDevicesList
				.filterNot { it.address == device.address.value } + device.toProto()
			prefs.toBuilder()
				.clearProtectedDevices()
				.addAllProtectedDevices(records)
				.build()
		}
	}

	override suspend fun removeProtectedDevice(address: BluetoothAddress) {
		context.peripheralPolicyDataStore.updateData { prefs ->
			prefs.toBuilder()
				.clearProtectedDevices()
				.addAllProtectedDevices(prefs.protectedDevicesList.filterNot { it.address == address.value })
				.build()
		}
	}

	override suspend fun setRetryState(address: BluetoothAddress, retryState: PeripheralRetryState?) {
		context.peripheralPolicyDataStore.updateData { prefs ->
			val builder = prefs.toBuilder()
				.clearRetryStates()
				.addAllRetryStates(prefs.retryStatesList.filterNot { it.address == address.value })
			if (retryState != null) builder.addRetryStates(retryState.toProto(address))
			builder.build()
		}
	}

	override suspend fun recordResult(address: BluetoothAddress, result: String, atMillis: Long) {
		context.peripheralPolicyDataStore.updateData { prefs ->
			val records = prefs.savedPeripheralsList.map { saved ->
				if (saved.address != address.value) saved
				else saved.toBuilder()
					.setLastResult(result)
					.setLastResultAtMillis(atMillis)
					.build()
			}
			prefs.toBuilder()
				.clearSavedPeripherals()
				.addAllSavedPeripherals(records)
				.build()
		}
	}
}

private fun PeripheralPolicyPreferences.toDomain(): PeripheralPolicy =
	PeripheralPolicy(
		supervisionEnabled = supervisionEnabled,
		safeModeEnabled = safeModeEnabled,
		savedPeripherals = savedPeripheralsList.mapNotNull(SavedPeripheralPreference::toDomainOrNull),
		protectedDevices = protectedDevicesList.mapNotNull(ProtectedDevicePreference::toDomainOrNull),
		retryStates = retryStatesList.mapNotNull { retry ->
			val address = BluetoothAddress.parse(retry.address) ?: return@mapNotNull null
			address to PeripheralRetryState(
				attempt = retry.attempt,
				nextAttemptAtMillis = retry.nextAttemptAtMillis,
				lastError = retry.lastError.takeIf(String::isNotBlank),
			)
		}.toMap(),
	)

private fun SavedPeripheralPreference.toDomainOrNull(): SavedPeripheralRecord? {
	val parsedAddress = BluetoothAddress.parse(address) ?: return null
	return SavedPeripheralRecord(
		address = parsedAddress,
		displayName = displayName,
		policy = reconnectPolicy.toDomain(),
		savedAtMillis = savedAtMillis,
		lastResult = lastResult.takeIf(String::isNotBlank),
		lastResultAtMillis = lastResultAtMillis.takeIf { it > 0L },
		expertOverride = expertOverride,
		protectionReason = protectionReason.takeIf(String::isNotBlank),
	)
}

private fun ProtectedDevicePreference.toDomainOrNull(): ProtectedPeripheralRecord? {
	val parsedAddress = BluetoothAddress.parse(address) ?: return null
	return ProtectedPeripheralRecord(
		address = parsedAddress,
		displayName = displayName,
		reason = reason,
		expertOverride = expertOverride,
		updatedAtMillis = updatedAtMillis,
	)
}

private fun ReconnectPolicyPreference.toDomain(): ReconnectPolicy =
	ReconnectPolicy(
		maxAttempts = if (maxAttempts == 0) 3 else maxAttempts,
		initialDelayMillis = if (initialDelayMillis == 0L) 5_000L else initialDelayMillis,
		maxDelayMillis = if (maxDelayMillis == 0L) 60_000L else maxDelayMillis,
	)

private fun SavedPeripheralRecord.toProto(): SavedPeripheralPreference =
	SavedPeripheralPreference.newBuilder()
		.setAddress(address.value)
		.setDisplayName(displayName)
		.setReconnectPolicy(policy.toProto())
		.setSavedAtMillis(savedAtMillis)
		.setLastResult(lastResult.orEmpty())
		.setLastResultAtMillis(lastResultAtMillis ?: 0L)
		.setExpertOverride(expertOverride)
		.setProtectionReason(protectionReason.orEmpty())
		.build()

private fun ProtectedPeripheralRecord.toProto(): ProtectedDevicePreference =
	ProtectedDevicePreference.newBuilder()
		.setAddress(address.value)
		.setDisplayName(displayName)
		.setReason(reason)
		.setExpertOverride(expertOverride)
		.setUpdatedAtMillis(updatedAtMillis)
		.build()

private fun ReconnectPolicy.toProto(): ReconnectPolicyPreference =
	ReconnectPolicyPreference.newBuilder()
		.setMaxAttempts(maxAttempts)
		.setInitialDelayMillis(initialDelayMillis)
		.setMaxDelayMillis(maxDelayMillis)
		.build()

private fun PeripheralRetryState.toProto(address: BluetoothAddress): RetryStatePreference =
	RetryStatePreference.newBuilder()
		.setAddress(address.value)
		.setAttempt(attempt)
		.setNextAttemptAtMillis(nextAttemptAtMillis)
		.setLastError(lastError.orEmpty())
		.build()

private val Context.peripheralPolicyDataStore: DataStore<PeripheralPolicyPreferences> by dataStore(
	fileName = DatastoreConstants.PERIPHERAL_POLICY_FILE_NAME,
	serializer = object : Serializer<PeripheralPolicyPreferences> {
		override val defaultValue: PeripheralPolicyPreferences =
			PeripheralPolicyPreferences.newBuilder()
				.setSafeModeEnabled(true)
				.build()

		override suspend fun readFrom(input: InputStream): PeripheralPolicyPreferences {
			try {
				return PeripheralPolicyPreferences.parseFrom(input)
			} catch (exception: InvalidProtocolBufferException) {
				throw CorruptionException("Cannot read peripheral policy proto", exception)
			}
		}

		override suspend fun writeTo(t: PeripheralPolicyPreferences, output: OutputStream) =
			t.writeTo(output)
	}
)
