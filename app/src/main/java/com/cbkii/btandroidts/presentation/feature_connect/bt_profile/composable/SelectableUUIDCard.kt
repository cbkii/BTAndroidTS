package com.cbkii.btandroidts.presentation.feature_connect.bt_profile.composable

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.cbkii.btandroidts.R
import java.util.UUID

@Composable
fun SelectableUUIDCard(
	uuid: UUID,
	onSelect: () -> Unit,
	modifier: Modifier = Modifier,
	isSelected: Boolean = false,
	uniqueName: String? = null,
	shape: Shape = MaterialTheme.shapes.medium,
	fontFamily: FontFamily = FontFamily.Monospace,
) {

	val uuidName by remember(uuid, uniqueName) {
		derivedStateOf {
			val shortUuid = String.format("%04X", (uuid.mostSignificantBits ushr 32) and 0xFFFF)
			val type = when (shortUuid) {
				"1101" -> "Serial terminal (RFCOMM/SPP)"
				"1124", "112D" -> "Keyboard / mouse / controller (HID)"
				"1105", "1106" -> "File transfer (OPP)"
				"110A" -> "Audio Source (A2DP)"
				"110B" -> "Audio Sink (A2DP)"
				"111E" -> "Handsfree (HFP)"
				"112F" -> "Phonebook Access (PBAP)"
				"0000" -> if (uuid.toString() == "3fe6c764-029f-48f0-a2d0-a43d9b1df5c8") "BTAndroidTS Internal" else "Custom/Unknown"
				else -> "Custom/Unknown"
			}
			uniqueName ?: "$type\n$uuid"
		}
	}

	Row(
		modifier = modifier
			.padding(horizontal = dimensionResource(id = R.dimen.lazy_colum_content_padding))
			.clip(shape = shape)
			.clickable(onClick = onSelect, role = Role.Checkbox),
		horizontalArrangement = Arrangement.spacedBy(4.dp),
		verticalAlignment = Alignment.CenterVertically
	) {
		RadioButton(
			selected = isSelected,
			onClick = onSelect,
			colors = RadioButtonDefaults
				.colors(selectedColor = MaterialTheme.colorScheme.secondary)
		)
		Text(
			text = uuidName,
			style = MaterialTheme.typography.labelLarge,
			fontFamily = fontFamily,
			color = MaterialTheme.colorScheme.onSurface,
		)
	}
}