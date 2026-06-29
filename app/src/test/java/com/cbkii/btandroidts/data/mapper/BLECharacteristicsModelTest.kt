package com.cbkii.btandroidts.data.mapper

import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEPermission
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEPropertyTypes
import com.cbkii.btandroidts.domain.bluetooth_le.enums.BLEWriteTypes
import com.cbkii.btandroidts.domain.bluetooth_le.models.BLECharacteristicsModel
import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class BLECharacteristicsModelTest {

    private fun createModel(
        properties: List<BLEPropertyTypes> = emptyList(),
        writeType: BLEWriteTypes = BLEWriteTypes.TYPE_DEFAULT
    ) = BLECharacteristicsModel(
        instanceId = 1,
        uuid = UUID.randomUUID(),
        permission = BLEPermission.PERMISSION_UNKNOWN,
        properties = kotlinx.collections.immutable.persistentListOf(*properties.toTypedArray()),
        writeType = writeType,
        descriptors = persistentListOf(),
        probableName = null,
        byteArray = ByteArray(0)
    )

    @Test
    fun emptyProperties_allCapabilityChecksFalse() {
        val model = createModel()
        assertFalse(model.canIndicate)
        assertFalse(model.canNotify)
        assertFalse(model.canRead)
        assertFalse(model.canWrite)
    }

    @Test
    fun propertyIndicate_canIndicateTrueOnly() {
        val model = createModel(properties = listOf(BLEPropertyTypes.PROPERTY_INDICATE))
        assertTrue(model.canIndicate)
        assertFalse(model.canNotify)
        assertFalse(model.canRead)
        assertFalse(model.canWrite)
    }

    @Test
    fun propertyNotify_canNotifyTrueOnly() {
        val model = createModel(properties = listOf(BLEPropertyTypes.PROPERTY_NOTIFY))
        assertFalse(model.canIndicate)
        assertTrue(model.canNotify)
        assertFalse(model.canRead)
        assertFalse(model.canWrite)
    }

    @Test
    fun propertyRead_canReadTrueOnly() {
        val model = createModel(properties = listOf(BLEPropertyTypes.PROPERTY_READ))
        assertFalse(model.canIndicate)
        assertFalse(model.canNotify)
        assertTrue(model.canRead)
        assertFalse(model.canWrite)
    }

    @Test
    fun propertyWriteWithDefaultType_canWriteTrue() {
        val model = createModel(
            properties = listOf(BLEPropertyTypes.PROPERTY_WRITE),
            writeType = BLEWriteTypes.TYPE_DEFAULT
        )
        assertTrue(model.canWrite)
    }

    @Test
    fun propertyWriteNoResponseWithTypeNoResponse_canWriteTrue() {
        val model = createModel(
            properties = listOf(BLEPropertyTypes.PROPERTY_WRITE_NO_RESPONSE),
            writeType = BLEWriteTypes.TYPE_NO_RESPONSE
        )
        assertTrue(model.canWrite)
    }

    @Test
    fun writePropertyWithUnsupportedWriteType_canWriteFalse() {
        val model = createModel(
            properties = listOf(BLEPropertyTypes.PROPERTY_WRITE),
            writeType = BLEWriteTypes.TYPE_SIGNED
        )
        assertFalse(model.canWrite)
    }

    @Test
    fun combinedProperties_behaveCorrectly() {
        val model = createModel(
            properties = listOf(
                BLEPropertyTypes.PROPERTY_READ,
                BLEPropertyTypes.PROPERTY_WRITE,
                BLEPropertyTypes.PROPERTY_NOTIFY
            ),
            writeType = BLEWriteTypes.TYPE_DEFAULT
        )
        assertFalse(model.canIndicate)
        assertTrue(model.canNotify)
        assertTrue(model.canRead)
        assertTrue(model.canWrite)
    }
}
