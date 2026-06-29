package com.cbkii.btandroidts.data.bluetooth.util

import com.cbkii.btandroidts.domain.settings.enums.BTTerminalDisplayMode
import java.io.InputStream

fun InputStream.readResponseFromStream(
	buffer: ByteArray = ByteArray(1024),
	mode: BTTerminalDisplayMode = BTTerminalDisplayMode.DISPLAY_MODE_TEXT
): String = buildString {
	while (true) {
		// Read one blocking chunk, then drain only bytes already reported as available.
		// Do not use buffer fullness as a continuation signal; on a live Bluetooth
		// socket that can issue a second blocking read after an exact-sized message.
		val bytesRead = read(buffer)
		if (bytesRead <= 0) break

		val message = when (mode) {
			BTTerminalDisplayMode.DISPLAY_MODE_TEXT -> buffer.decodeToString(endIndex = bytesRead)
			BTTerminalDisplayMode.DISPLAY_MODE_HEX -> buffer.toHexString(endIndex = bytesRead)
		}
		append(message)

		if (available() <= 0) break
	}
}
