package com.cbkii.btandroidts.domain.settings.models

import com.cbkii.btandroidts.domain.settings.enums.BTTerminalCharSet
import com.cbkii.btandroidts.domain.settings.enums.BTTerminalDisplayMode
import com.cbkii.btandroidts.domain.settings.enums.BTTerminalNewLineChar

data class BTSettingsModel(
	val btTerminalCharSet: BTTerminalCharSet = BTTerminalCharSet.CHAR_SET_UTF_8,
	val showTimeStamp: Boolean = false,
	val displayMode: BTTerminalDisplayMode = BTTerminalDisplayMode.DISPLAY_MODE_TEXT,
	val newLineCharReceive: BTTerminalNewLineChar = BTTerminalNewLineChar.NEW_LINE_LF,
	val newLineCharSend: BTTerminalNewLineChar = BTTerminalNewLineChar.NEW_LINE_LF,
	val autoScrollEnabled: Boolean = false,
	val localEchoEnabled: Boolean = false,
	val clearInputOnSend: Boolean = false,
	val keepScreenOnWhenConnected: Boolean = false,
)
