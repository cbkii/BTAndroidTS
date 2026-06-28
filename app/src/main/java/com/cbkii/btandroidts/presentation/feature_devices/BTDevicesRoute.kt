package com.cbkii.btandroidts.presentation.feature_devices

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
import com.cbkii.btandroidts.R
import com.cbkii.btandroidts.data.utils.hasBTScanPermission
import com.cbkii.btandroidts.data.utils.hasLocationPermission
import com.cbkii.btandroidts.domain.bluetooth.models.BluetoothDeviceModel
import com.cbkii.btandroidts.domain.bluetooth_le.models.BluetoothLEDeviceModel
import com.cbkii.btandroidts.domain.peripheral.CapabilityStatus
import com.cbkii.btandroidts.domain.peripheral.PeripheralFeature
import com.cbkii.btandroidts.presentation.feature_devices.composables.BTDeviceRouteTopBar
import com.cbkii.btandroidts.presentation.feature_devices.composables.BTDevicesTabsLayout
import com.cbkii.btandroidts.presentation.feature_devices.composables.BluetoothDevicesList
import com.cbkii.btandroidts.presentation.feature_devices.composables.BluetoothLeDeviceList
import com.cbkii.btandroidts.presentation.feature_devices.composables.DevicesScreenModeContainer
import com.cbkii.btandroidts.presentation.feature_devices.state.BTDevicesScreenEvents
import com.cbkii.btandroidts.presentation.feature_devices.state.BTDevicesScreenState
import com.cbkii.btandroidts.presentation.util.BluetoothTypes
import com.cbkii.btandroidts.presentation.util.LocalSnackBarProvider
import com.cbkii.btandroidts.presentation.util.PreviewFakes
import com.cbkii.btandroidts.ui.theme.BTAndroidTSTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BTDevicesRoute(
	isBTActive: Boolean,
	isScanning: Boolean,
	state: BTDevicesScreenState,
	onEvent: (BTDevicesScreenEvents) -> Unit,
	modifier: Modifier = Modifier,
	initialTab: BluetoothTypes = BluetoothTypes.CLASSIC,
	onSelectDevice: (BluetoothDeviceModel) -> Unit = {},
	onSelectLeDevice: (BluetoothLEDeviceModel) -> Unit = {},
	onDashboardAction: (Ts18DashboardAction) -> Unit = {},
	navigation: @Composable () -> Unit = {},
) {
	val context = LocalContext.current
	val snackBarHostState = LocalSnackBarProvider.current

	var currentTab by remember { mutableStateOf(initialTab) }

	var hasBtPermission by remember(context) {
		mutableStateOf(context.hasBTScanPermission)
	}

	var hasLocationPermission by remember(context) {
		mutableStateOf(context.hasLocationPermission)
	}

	val showLocationBlock by remember(hasLocationPermission) {
		derivedStateOf { Build.VERSION.SDK_INT < Build.VERSION_CODES.S && !hasLocationPermission }
	}

	val showScanButton by remember(hasBtPermission, hasLocationPermission, isBTActive) {
		derivedStateOf {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) hasBtPermission && isBTActive
			else hasLocationPermission && isBTActive
		}
	}

	val scrollBehaviour = TopAppBarDefaults.enterAlwaysScrollBehavior()

	Scaffold(
		topBar = {
			BTDeviceRouteTopBar(
				isScanning = isScanning,
				canShowScanOption = showScanButton,
				currentTab = currentTab,
				hasLocationPermission = hasLocationPermission,
				startClassicScan = { onEvent(BTDevicesScreenEvents.StartScan) },
				stopClassicScan = { onEvent(BTDevicesScreenEvents.StopScan) },
				startBLEScan = { onEvent(BTDevicesScreenEvents.StartLEDeviceScan) },
				stopBLEScan = { onEvent(BTDevicesScreenEvents.StopLEDevicesScan) },
				navigation = navigation,
				scrollBehavior = scrollBehaviour,
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
		modifier = modifier
			.nestedScroll(scrollBehaviour.nestedScrollConnection)
			.padding(top = dimensionResource(R.dimen.ts18_status_bar_height))
			.padding(end = dimensionResource(R.dimen.ts18_nav_bar_width)),
	) { scPadding ->
		Column(
			verticalArrangement = Arrangement.spacedBy(dimensionResource(R.dimen.sc_padding)),
			modifier = Modifier
				.fillMaxSize()
				.padding(scPadding)
		) {
			Ts18DashboardHeader(
				state = state,
				isScanning = isScanning,
				onAction = onDashboardAction,
				modifier = Modifier
					.fillMaxWidth()
					.padding(horizontal = dimensionResource(R.dimen.sc_padding))
			)
			DevicesScreenModeContainer(
				isActive = isBTActive,
				hasPermission = hasBtPermission,
				onBTPermissionChanged = { isGranted ->
					hasBtPermission = isGranted
					onEvent(BTDevicesScreenEvents.OnBTPermissionChanged(isGranted))
				},
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
			) {
				BTDevicesTabsLayout(
					isScanning = isScanning,
					initialTab = initialTab,
					onCurrentTabChanged = { type ->
						currentTab = type
						onEvent(BTDevicesScreenEvents.OnStopAnyRunningScan)
					},
					modifier = Modifier.fillMaxSize(),
					classicTabContent = {
						BluetoothDevicesList(
							isScanning = isScanning,
							pairedDevices = state.pairedDevices,
							availableDevices = state.availableDevices,
							isPairedDevicesReady = state.isPairedDevicesLoaded,
							showLocationPlaceholder = showLocationBlock,
							onSelectDevice = onSelectDevice,
							contentPadding = PaddingValues(all = dimensionResource(R.dimen.sc_padding)),
							onLocationPermsAccept = { isGranted ->
								hasLocationPermission = isGranted
								onEvent(BTDevicesScreenEvents.OnLocationPermissionChanged(isGranted))
							},
							modifier = Modifier.fillMaxSize(),
						)
					},
					leTabContent = {
						BluetoothLeDeviceList(
							isScanning = isScanning,
							hasLocationPermission = hasLocationPermission,
							leDevices = state.leDevices,
							onDeviceSelect = onSelectLeDevice,
							contentPadding = PaddingValues(all = dimensionResource(R.dimen.sc_padding)),
							onLocationPermissionChanged = { isAccepted ->
								hasLocationPermission = isAccepted
							},
							modifier = Modifier.fillMaxSize(),
						)
					},
				)
			}
		}
	}
}

@Composable
private fun Ts18DashboardHeader(
	state: BTDevicesScreenState,
	isScanning: Boolean,
	onAction: (Ts18DashboardAction) -> Unit,
	modifier: Modifier = Modifier,
) {
	val hidStatus = state.capabilities.firstOrNull { it.feature == PeripheralFeature.HID_HOST }?.status
	val oppStatus = state.capabilities.firstOrNull { it.feature == PeripheralFeature.OPP_SHARE }?.status
	val protectedCount = state.inventoryDevices.count { it.isProtected }

	ElevatedCard(modifier = modifier) {
		Column(
			verticalArrangement = Arrangement.spacedBy(8.dp),
			modifier = Modifier.padding(12.dp)
		) {
			Text(
				text = stringResource(R.string.ts18_dashboard_title),
				style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
			)
			Row(
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				modifier = Modifier.fillMaxWidth()
			) {
				DashboardButton(
					label = stringResource(R.string.ts18_dashboard_phone_auto),
					detail = stringResource(R.string.ts18_dashboard_vendor_owned),
					onClick = { onAction(Ts18DashboardAction.PHONE_AUTO) },
					modifier = Modifier.weight(1f)
				)
				DashboardButton(
					label = stringResource(R.string.ts18_dashboard_peripherals),
					detail = stringResource(
						R.string.ts18_dashboard_inventory_count,
						state.inventoryDevices.size,
						protectedCount
					),
					onClick = { onAction(Ts18DashboardAction.PERIPHERALS) },
					modifier = Modifier.weight(1f)
				)
				DashboardButton(
					label = stringResource(R.string.ts18_dashboard_file_share),
					detail = oppStatus.toDashboardText(),
					onClick = { onAction(Ts18DashboardAction.FILE_SHARING) },
					modifier = Modifier.weight(1f)
				)
			}
			Row(
				horizontalArrangement = Arrangement.spacedBy(8.dp),
				modifier = Modifier.fillMaxWidth()
			) {
				DashboardButton(
					label = stringResource(R.string.keyboard_test_title),
					detail = stringResource(R.string.keyboard_test_desc),
					onClick = { onAction(Ts18DashboardAction.KEYBOARD_TEST) },
					modifier = Modifier.weight(1f)
				)
				DashboardButton(
					label = stringResource(R.string.ts18_dashboard_diagnostics),
					detail = stringResource(R.string.ts18_dashboard_local_export),
					onClick = { onAction(Ts18DashboardAction.DIAGNOSTICS) },
					modifier = Modifier.weight(1f)
				)
				DashboardButton(
					label = stringResource(R.string.ts18_dashboard_advanced),
					detail = stringResource(R.string.ts18_dashboard_rfcomm_gatt),
					onClick = { onAction(Ts18DashboardAction.ADVANCED_TOOLS) },
					modifier = Modifier.weight(1f)
				)
			}
		}
	}
}

@Composable
private fun DashboardButton(
	label: String,
	detail: String,
	onClick: () -> Unit,
	modifier: Modifier = Modifier,
) {
	FilledTonalButton(
		onClick = onClick,
		modifier = modifier.heightIn(min = dimensionResource(R.dimen.dashboard_card_min_height)),
		shape = androidx.compose.material3.MaterialTheme.shapes.medium,
		contentPadding = PaddingValues(16.dp)
	) {
		Column(
			modifier = Modifier.fillMaxWidth(),
			verticalArrangement = Arrangement.Center
		) {
			Text(
				text = label,
				style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
				maxLines = 1,
				overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
			)
			Text(
				text = detail,
				style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
				maxLines = 2,
				overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
			)
		}
	}
}

enum class Ts18DashboardAction {
	PHONE_AUTO,
	PERIPHERALS,
	FILE_SHARING,
	SUPERVISION,
	DIAGNOSTICS,
	ADVANCED_TOOLS,
	KEYBOARD_TEST,
	PHONE_KEYBOARD_COMPAT,
}

@Composable
private fun CapabilityStatus?.toDashboardText(): String = when (this) {
	CapabilityStatus.AVAILABLE -> stringResource(R.string.ts18_dashboard_available)
	CapabilityStatus.REQUIRES_PRIVILEGE -> stringResource(R.string.ts18_dashboard_requires_privilege)
	CapabilityStatus.REQUIRES_ROOT -> stringResource(R.string.ts18_dashboard_requires_root)
	CapabilityStatus.REQUIRES_DEVICE_VALIDATION -> stringResource(R.string.ts18_dashboard_requires_validation)
	CapabilityStatus.FAILED -> stringResource(R.string.ts18_dashboard_failed)
	CapabilityStatus.UNAVAILABLE,
	null -> stringResource(R.string.ts18_dashboard_unavailable)
}

private class BTDeviceClassicalScreenStateParams :
	CollectionPreviewParameterProvider<BTDevicesScreenState>(
		listOf(
			PreviewFakes.FAKE_DEVICE_STATE_WITH_PAIRED_AND_AVAILABLE_DEVICES,
			PreviewFakes.FAKE_DEVICE_STATE_WITH_PAIRED_DEVICE,
			PreviewFakes.FAKE_DEVICE_STATE_WITH_NO_DEVICE,
		)
	)

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
private fun BTDeviceRouteWithClassicDevicesPreview(
	@PreviewParameter(BTDeviceClassicalScreenStateParams::class)
	state: BTDevicesScreenState
) = BTAndroidTSTheme {
	BTDevicesRoute(
		isBTActive = true,
		isScanning = false,
		state = state,
		onEvent = {},
		onSelectDevice = { }
	)
}

class BTDevicesLEScreenStateParams
	: CollectionPreviewParameterProvider<BTDevicesScreenState>(
	listOf(
		PreviewFakes.FAKE_DEVICE_STATE_WITH_NO_DEVICE,
		PreviewFakes.FAKE_DEVICE_STATE_WITH_SOME_BLE_DEVICES,
	)
)

@Preview(showBackground = true, widthDp = 1280, heightDp = 720)
@Composable
private fun BTDeviceRouteWithLEDevicesPreview(
	@PreviewParameter(BTDevicesLEScreenStateParams::class)
	state: BTDevicesScreenState
) = BTAndroidTSTheme {
	BTDevicesRoute(
		isBTActive = true,
		isScanning = false,
		state = state,
		onEvent = {},
		onSelectDevice = { }
	)
}
