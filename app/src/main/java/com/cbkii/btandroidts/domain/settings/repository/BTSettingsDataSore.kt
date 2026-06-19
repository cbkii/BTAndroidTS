package com.cbkii.btandroidts.domain.settings.repository

import com.cbkii.btandroidts.domain.settings.enums.BTTerminalCharSet
import com.cbkii.btandroidts.domain.settings.enums.BTTerminalDisplayMode
import com.cbkii.btandroidts.domain.settings.enums.BTTerminalNewLineChar
import com.cbkii.btandroidts.domain.settings.models.BTSettingsModel
import kotlinx.coroutines.flow.Flow

interface BTSettingsDataSore {

	val settingsFlow: Flow<BTSettingsModel>

	suspend fun getSettings(): BTSettingsModel

	suspend fun onCharsetChange(charSet: BTTerminalCharSet)

	suspend fun onShowTimestampChange(isChange: Boolean)

	suspend fun onDisplayModeChange(mode: BTTerminalDisplayMode)

	suspend fun onNewLineCharChangeForReceive(newLineChar: BTTerminalNewLineChar)

	suspend fun onNewLineCharChangeForSend(newLineChar: BTTerminalNewLineChar)

	suspend fun onLocalEchoValueChange(isLocalEcho: Boolean)

	suspend fun onClearInputOnSendValueChange(canClear: Boolean)

	suspend fun onKeepScreenOnConnectedValueChange(isKeepScreenOn: Boolean)

	suspend fun onAutoScrollValueChange(isEnabled: Boolean)

}