package com.cbkii.btandroidts.data.bluetooth.util

import com.cbkii.btandroidts.domain.settings.enums.BTTerminalDisplayMode
import java.io.InputStream

fun InputStream.readResponseFromStream(
	buffer: ByteArray = ByteArray(1024),
	mode: BTTerminalDisplayMode = BTTerminalDisplayMode.DISPLAY_MODE_TEXT
): String = buildString {
	var bytesRead: Int
	do {
		// read the bytes
		bytesRead = read(buffer)
		// if eof or nothing to read
		if (bytesRead <= 0) break
		val message = when (mode) {
			BTTerminalDisplayMode.DISPLAY_MODE_TEXT -> buffer.decodeToString(endIndex = bytesRead)
			BTTerminalDisplayMode.DISPLAY_MODE_HEX -> buffer.toHexString(endIndex = bytesRead)
		}
		append(message)
	} while (bytesRead == buffer.size || available() > 0)
}
