package com.cbkii.btandroidts.data.bluetooth.util

import com.cbkii.btandroidts.domain.settings.enums.BTTerminalNewLineChar
import org.junit.Assert.assertEquals
import org.junit.Test

class ReplaceLineSeperatorTest {

    private val platformSeparator = System.lineSeparator()

    @Test
    fun replaceLineSeparatorTo_eachSupportedValue() {
        val input = "line1${platformSeparator}line2${platformSeparator}line3"

        val toCR = input.replaceLineSeparatorTo(BTTerminalNewLineChar.NEW_LINE_CR)
        assertEquals("line1\rline2\rline3", toCR)

        val toLF = input.replaceLineSeparatorTo(BTTerminalNewLineChar.NEW_LINE_LF)
        assertEquals("line1\nline2\nline3", toLF)

        val toCRLF = input.replaceLineSeparatorTo(BTTerminalNewLineChar.NEW_LINE_CR_LF)
        assertEquals("line1\r\nline2\r\nline3", toCRLF)

        val toNone = input.replaceLineSeparatorTo(BTTerminalNewLineChar.NEW_LINE_NONE)
        assertEquals("line1line2line3", toNone)
    }

    @Test
    fun replaceLineSeparatorFrom_CR_backToPlatform() {
        val input = "line1\rline2"
        val expected = "line1${platformSeparator}line2"
        val result = input.replaceLineSeparatorFrom(BTTerminalNewLineChar.NEW_LINE_CR)
        assertEquals(expected, result)
    }

    @Test
    fun replaceLineSeparatorFrom_LF_backToPlatform() {
        val input = "line1\nline2"
        val expected = "line1${platformSeparator}line2"
        val result = input.replaceLineSeparatorFrom(BTTerminalNewLineChar.NEW_LINE_LF)
        assertEquals(expected, result)
    }

    @Test
    fun replaceLineSeparatorFrom_CRLF_backToPlatform() {
        val input = "line1\r\nline2"
        val expected = "line1${platformSeparator}line2"
        val result = input.replaceLineSeparatorFrom(BTTerminalNewLineChar.NEW_LINE_CR_LF)
        assertEquals(expected, result)
    }

    @Test
    fun replaceLineSeparatorFrom_none_backToPlatformUnchanged() {
        val input = "line1line2"
        val result = input.replaceLineSeparatorFrom(BTTerminalNewLineChar.NEW_LINE_NONE)
        assertEquals(input, result)
    }

    @Test
    fun replaceLineSeparator_stringWithoutSeparators_remainsUnchanged() {
        val input = "single line string"
        assertEquals(input, input.replaceLineSeparatorTo(BTTerminalNewLineChar.NEW_LINE_CR))
        assertEquals(input, input.replaceLineSeparatorFrom(BTTerminalNewLineChar.NEW_LINE_CR))
    }

    @Test
    fun replaceLineSeparator_emptyString_remainsEmpty() {
        val input = ""
        assertEquals(input, input.replaceLineSeparatorTo(BTTerminalNewLineChar.NEW_LINE_CR))
        assertEquals(input, input.replaceLineSeparatorFrom(BTTerminalNewLineChar.NEW_LINE_CR))
    }
}
