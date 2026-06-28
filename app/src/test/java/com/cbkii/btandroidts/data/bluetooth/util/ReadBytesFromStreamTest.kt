package com.cbkii.btandroidts.data.bluetooth.util

import com.cbkii.btandroidts.domain.settings.enums.BTTerminalDisplayMode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class ReadBytesFromStreamTest {

    @Test
    fun readResponseFromStream_textMode_returnsPlainText() {
        val inputStr = "Hello, Bluetooth"
        val inputStream = ByteArrayInputStream(inputStr.toByteArray(Charsets.UTF_8))

        val result = inputStream.readResponseFromStream(mode = BTTerminalDisplayMode.DISPLAY_MODE_TEXT)
        assertEquals(inputStr, result)
    }

    @Test
    fun readResponseFromStream_hexMode_returnsHexString() {
        val bytes = byteArrayOf(0x48, 0x65, 0x6c, 0x6c, 0x6f) // "Hello"
        val inputStream = ByteArrayInputStream(bytes)

        val result = inputStream.readResponseFromStream(mode = BTTerminalDisplayMode.DISPLAY_MODE_HEX)
        // Check exact expected hex formatting based on toHexString extension behavior
        val expected = bytes.toHexString()
        assertEquals(expected, result)
    }

    @Test
    fun readResponseFromStream_emptyStream_returnsEmptyString() {
        val inputStream = ByteArrayInputStream(ByteArray(0))
        val result = inputStream.readResponseFromStream()
        assertEquals("", result)
    }

    @Test
    fun readResponseFromStream_smallBufferMultipleChunks_readsCorrectly() {
        val inputStr = "A somewhat longer string that needs multiple reads to fit into a tiny buffer."
        val inputStream = ByteArrayInputStream(inputStr.toByteArray(Charsets.UTF_8))

        // Use a tiny 10-byte buffer to force multiple iterations
        val tinyBuffer = ByteArray(10)
        val result = inputStream.readResponseFromStream(buffer = tinyBuffer, mode = BTTerminalDisplayMode.DISPLAY_MODE_TEXT)
        assertEquals(inputStr, result)
    }

    @Test
    fun readResponseFromStream_stopsOnEofWithoutHanging() {
        val inputStream = ByteArrayInputStream("short".toByteArray())
        val result = inputStream.readResponseFromStream()
        assertEquals("short", result)
        // No hang occurred, test completes successfully
    }
}
