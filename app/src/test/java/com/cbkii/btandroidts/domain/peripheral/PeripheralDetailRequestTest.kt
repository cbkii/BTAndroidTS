package com.cbkii.btandroidts.domain.peripheral

import com.cbkii.btandroidts.presentation.feature_devices.detail.PeripheralDetailRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeripheralDetailRequestTest {
    @Test
    fun validArgsParseAddressAndName() {
        val request = PeripheralDetailRequest.fromRaw("00:11:22:33:44:55", "Keyboard")
        assertEquals(BluetoothAddress.requireValid("00:11:22:33:44:55"), request.address)
        assertEquals("Keyboard", request.fallbackName)
    }

    @Test
    fun invalidAddressIsHandledDeterministically() {
        val request = PeripheralDetailRequest.fromRaw("not-a-mac", "Keyboard")
        assertNull(request.address)
        assertEquals("Keyboard", request.fallbackName)
    }

    @Test
    fun blankNameIsNotUsedAsFallback() {
        val request = PeripheralDetailRequest.fromRaw("00:11:22:33:44:55", " ")
        assertEquals(BluetoothAddress.requireValid("00:11:22:33:44:55"), request.address)
        assertNull(request.fallbackName)
    }
}
