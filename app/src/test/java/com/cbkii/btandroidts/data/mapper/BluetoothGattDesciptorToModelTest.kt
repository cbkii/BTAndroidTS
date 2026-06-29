package com.cbkii.btandroidts.data.mapper

import com.cbkii.btandroidts.domain.bluetooth_le.models.BLEDescriptorModel
import com.cbkii.btandroidts.domain.bluetooth_le.util.BLEDescriptorValue
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BluetoothGattDesciptorToModelTest {

    private fun createModel(byteArray: ByteArray): BLEDescriptorModel {
        return BLEDescriptorModel(
            uuid = UUID.randomUUID(),
            permissions = persistentListOf(),
            byteArray = byteArray
        )
    }

    @Test
    fun descriptorValue_enableIndication_mapsToEnableIndication() {
        val model = createModel(byteArrayOf(0x02, 0x00))
        val result = model.descriptorValue
        assertEquals(BLEDescriptorValue.EnableIndication, result)
    }

    @Test
    fun descriptorValue_enableNotification_mapsToEnableNotification() {
        val model = createModel(byteArrayOf(0x01, 0x00))
        val result = model.descriptorValue
        assertEquals(BLEDescriptorValue.EnableNotification, result)
    }

    @Test
    fun descriptorValue_disableNotification_mapsToDisableNotifyOrIndication() {
        val model = createModel(byteArrayOf(0x00, 0x00))
        val result = model.descriptorValue
        assertEquals(BLEDescriptorValue.DisableNotifyOrIndication, result)
    }

    @Test
    fun descriptorValue_arbitraryBytes_mapsToReadableValue() {
        val arbitraryBytes = byteArrayOf(0x01, 0x02, 0x03)
        val model = createModel(arbitraryBytes)
        val result = model.descriptorValue

        assertTrue(result is BLEDescriptorValue.ReadableValue)
        val readableValue = result as BLEDescriptorValue.ReadableValue
        assertEquals(model.valueAsString, readableValue.string)
    }

    @Test
    fun descriptorValue_emptyByteArray_mapsToReadableValue() {
        val model = createModel(ByteArray(0))
        val result = model.descriptorValue

        assertTrue(result is BLEDescriptorValue.ReadableValue)
        val readableValue = result as BLEDescriptorValue.ReadableValue
        assertEquals(model.valueAsString, readableValue.string)
    }
}
