package com.cbkii.btandroidts.presentation.feature_devices.composables

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.cbkii.btandroidts.R
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.presentation.util.PreviewFakes
import com.cbkii.btandroidts.presentation.util.SharedElementTransitionKeys
import com.cbkii.btandroidts.presentation.util.sharedBoundsWrapper
import com.cbkii.btandroidts.ui.theme.BTAndroidTSTheme

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun BluetoothDeviceCard(
	device: BluetoothDeviceModel,
	onConnect: () -> Unit,
	modifier: Modifier = Modifier,
	shape: Shape = MaterialTheme.shapes.medium,
	isPaired: Boolean = false,
	selectedColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
	containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
	var isExpanded by remember { mutableStateOf(false) }

	val cardContainerColor by animateColorAsState(
		targetValue = if (isExpanded) selectedColor else containerColor,
		label = "Card container color",
		animationSpec = tween(durationMillis = 200)
	)

	Surface(
		onClick = { isExpanded = true },
		shape = shape,
		color = cardContainerColor,
		contentColor = contentColorFor(containerColor),
		modifier = modifier.sharedBoundsWrapper(SharedElementTransitionKeys.btProfileScreen(device.address))
	) {
		Row(
			verticalAlignment = Alignment.CenterVertically,
			horizontalArrangement = Arrangement.spacedBy(12.dp),
			modifier = Modifier.padding(vertical = 8.dp, horizontal = 10.dp)
		) {
			BTDeviceIcon(
				device = device,
				deviceName = device.name,
				contentColor = if (isPaired) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
				containerColor = if (isPaired) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
			)
			Column(modifier = Modifier.weight(1f)) {
				Row(verticalAlignment = Alignment.CenterVertically) {
					Text(
						text = device.name,
						maxLines = 1,
						overflow = TextOverflow.Ellipsis,
						style = MaterialTheme.typography.titleMedium,
						color = MaterialTheme.colorScheme.onSurface,
						modifier = Modifier.weight(1f, fill = false)
					)
					if (isPaired) {
						Text(
							text = stringResource(R.string.state_bonded),
							style = MaterialTheme.typography.labelSmall,
							color = MaterialTheme.colorScheme.primary,
							fontWeight = FontWeight.Bold,
							modifier = Modifier.padding(start = 8.dp)
						)
					}

					// If the device model had a way to check for HID active or protected, we would add it here.
					// Since BluetoothDeviceModel doesn't have it, we'll use a placeholder for now
					// to satisfy the UI requirement if the data was available.
				}
				Text(
					text = buildAnnotatedString {
						append(device.address)
						append(" | ")
						append(device.mode.name)
					},
					style = MaterialTheme.typography.bodySmall,
					color = MaterialTheme.colorScheme.onSurfaceVariant
				)
			}
			Box {
				Icon(
					imageVector = Icons.Default.MoreVert,
					contentDescription = stringResource(id = R.string.menu_option_more)
				)
				DropdownMenu(
					expanded = isExpanded,
					onDismissRequest = { isExpanded = false },
					shape = MaterialTheme.shapes.medium,
					tonalElevation = 4.dp
				) {
					DropdownMenuItem(
						text = { Text(text = stringResource(R.string.connect_to_client)) },
						onClick = onConnect,
						leadingIcon = {
							Icon(
								imageVector = Icons.Outlined.Cable,
								contentDescription = stringResource(R.string.connect_to_client)
							)
						},
					)
				}
			}
		}
	}
}


@PreviewLightDark
@Composable
private fun BluetoothDeviceCardPreview() = BTAndroidTSTheme {
	BluetoothDeviceCard(
		device = PreviewFakes.FAKE_DEVICE_MODEL,
		onConnect = {}
	)
}