package com.cbkii.btandroidts.domain.peripheral

import org.junit.Assert.*
import org.junit.Test

class BluetoothAddressTest {

    @Test
    fun parse_validLowercase_normalizesToUppercase() {
        val address = BluetoothAddress.parse("00:11:22:33:44:55")
        assertNotNull(address)
        assertEquals("00:11:22:33:44:55", address?.value)
    }

    @Test
    fun parse_validUppercase_staysValid() {
        val address = BluetoothAddress.parse("AA:BB:CC:DD:EE:FF")
        assertNotNull(address)
        assertEquals("AA:BB:CC:DD:EE:FF", address?.value)
    }

    @Test
    fun parse_validMixedCase_normalizesToUppercase() {
        val address = BluetoothAddress.parse("aa:BB:cc:DD:ee:FF")
        assertNotNull(address)
        assertEquals("AA:BB:CC:DD:EE:FF", address?.value)
    }

    @Test
    fun parse_leadingTrailingWhitespace_isTrimmed() {
        val address = BluetoothAddress.parse("  00:11:22:33:44:55  ")
        assertNotNull(address)
        assertEquals("00:11:22:33:44:55", address?.value)
    }

    @Test
    fun parse_invalidSeparators_fails() {
        assertNull(BluetoothAddress.parse("00-11-22-33-44-55"))
        assertNull(BluetoothAddress.parse("001122334455"))
    }

    @Test
    fun parse_tooFewOctets_fails() {
        assertNull(BluetoothAddress.parse("00:11:22:33:44"))
    }

    @Test
    fun parse_tooManyOctets_fails() {
        assertNull(BluetoothAddress.parse("00:11:22:33:44:55:66"))
    }

    @Test
    fun parse_nonHexCharacters_fails() {
        assertNull(BluetoothAddress.parse("00:11:22:33:44:GG"))
    }

    @Test
    fun parse_emptyString_fails() {
        assertNull(BluetoothAddress.parse(""))
        assertNull(BluetoothAddress.parse("   "))
    }

    @Test
    fun requireValid_validInput_returnsValue() {
        val address = BluetoothAddress.requireValid("00:11:22:33:44:55")
        assertEquals("00:11:22:33:44:55", address.value)
    }

    @Test(expected = IllegalStateException::class)
    fun requireValid_invalidInput_throwsException() {
        BluetoothAddress.requireValid("invalid")
    }
}
