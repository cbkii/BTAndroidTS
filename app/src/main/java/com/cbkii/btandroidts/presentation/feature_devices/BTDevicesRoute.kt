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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MenuOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.saveable.rememberSaveable
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

	var sidebarCollapsed by rememberSaveable { mutableStateOf(false) }

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
		Row(
			modifier = Modifier
				.fillMaxSize()
				.padding(scPadding)
		) {
			Ts18ActionSidebar(
				collapsed = sidebarCollapsed,
				onCollapsedChange = { sidebarCollapsed = it },
				state = state,
				isScanning = isScanning,
				onAction = onDashboardAction,
				modifier = Modifier.fillMaxHeight()
			)

			Column(
				modifier = Modifier
					.weight(1f)
					.fillMaxHeight()
			) {
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
					}
					)
				}
			}
		}
	}
}

@Composable
fun Ts18ActionSidebar(
	collapsed: Boolean,
	onCollapsedChange: (Boolean) -> Unit,
	state: BTDevicesScreenState,
	isScanning: Boolean,
	onAction: (Ts18DashboardAction) -> Unit,
	modifier: Modifier = Modifier,
) {
	val scrollState = rememberScrollState()

	BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
		val maxAllowedWidth = maxWidth * 0.20f
		val actualWidth = if (collapsed) 80.dp else maxAllowedWidth.coerceAtMost(250.dp)

		NavigationRail(
			modifier = Modifier
				.widthIn(max = actualWidth)
				.fillMaxHeight(),
			header = {
				IconButton(onClick = { onCollapsedChange(!collapsed) }) {
					Icon(
						imageVector = if (collapsed) Icons.Default.Menu else Icons.Default.MenuOpen,
						contentDescription = stringResource(id = R.string.menu_option_more)
					)
				}
			}
		) {
			Column(
				modifier = Modifier
					.fillMaxHeight()
					.verticalScroll(scrollState)
			) {
				NavigationRailItem(
					icon = { Icon(Icons.Default.Phone, contentDescription = stringResource(R.string.ts18_dashboard_phone_auto)) },
					label = if (!collapsed) { { Text(stringResource(R.string.ts18_dashboard_phone_auto), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } } else null,
					selected = false,
					onClick = { onAction(Ts18DashboardAction.PHONE_AUTO) }
				)
				NavigationRailItem(
					icon = { Icon(Icons.Default.Devices, contentDescription = stringResource(R.string.ts18_dashboard_peripherals)) },
					label = if (!collapsed) { { Text(stringResource(R.string.ts18_dashboard_peripherals), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } } else null,
					selected = false,
					onClick = { onAction(Ts18DashboardAction.PERIPHERALS) }
				)
				NavigationRailItem(
					icon = { Icon(Icons.Default.Send, contentDescription = stringResource(R.string.ts18_dashboard_file_share)) },
					label = if (!collapsed) { { Text(stringResource(R.string.ts18_dashboard_file_share), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } } else null,
					selected = false,
					onClick = { onAction(Ts18DashboardAction.FILE_SHARING) }
				)
				NavigationRailItem(
					icon = { Icon(Icons.Default.Warning, contentDescription = stringResource(R.string.ts18_dashboard_supervision)) },
					label = if (!collapsed) { { Text(stringResource(R.string.ts18_dashboard_supervision), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } } else null,
					selected = false,
					onClick = { onAction(Ts18DashboardAction.SUPERVISION) }
				)
				NavigationRailItem(
					icon = { Icon(Icons.Default.BugReport, contentDescription = stringResource(R.string.ts18_dashboard_diagnostics)) },
					label = if (!collapsed) { { Text(stringResource(R.string.ts18_dashboard_diagnostics), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } } else null,
					selected = false,
					onClick = { onAction(Ts18DashboardAction.DIAGNOSTICS) }
				)
				NavigationRailItem(
					icon = { Icon(Icons.Default.Keyboard, contentDescription = stringResource(R.string.keyboard_test_title)) },
					label = if (!collapsed) { { Text(stringResource(R.string.keyboard_test_title), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } } else null,
					selected = false,
					onClick = { onAction(Ts18DashboardAction.KEYBOARD_TEST) }
				)
				NavigationRailItem(
					icon = { Icon(Icons.Default.Phone, contentDescription = stringResource(R.string.phone_keyboard_title)) },
					label = if (!collapsed) { { Text(stringResource(R.string.phone_keyboard_title), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } } else null,
					selected = false,
					onClick = { onAction(Ts18DashboardAction.PHONE_KEYBOARD_COMPAT) }
				)
				NavigationRailItem(
					icon = { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_route_title)) },
					label = if (!collapsed) { { Text(stringResource(R.string.settings_route_title), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } } else null,
					selected = false,
					onClick = { onAction(Ts18DashboardAction.SETTINGS) }
				)
				NavigationRailItem(
					icon = { Icon(Icons.Default.Info, contentDescription = stringResource(R.string.about_route_title)) },
					label = if (!collapsed) { { Text(stringResource(R.string.about_route_title), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } } else null,
					selected = false,
					onClick = { onAction(Ts18DashboardAction.ABOUT) }
				)
				NavigationRailItem(
					icon = { Icon(Icons.Default.Build, contentDescription = stringResource(R.string.bt_server_route)) },
					label = if (!collapsed) { { Text(stringResource(R.string.bt_server_route), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } } else null,
					selected = false,
					onClick = { onAction(Ts18DashboardAction.BT_SERVER) }
				)
				NavigationRailItem(
					icon = { Icon(Icons.Default.Bluetooth, contentDescription = stringResource(R.string.ble_server_title)) },
					label = if (!collapsed) { { Text(stringResource(R.string.ble_server_title), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) } } else null,
					selected = false,
					onClick = { onAction(Ts18DashboardAction.BLE_SERVER) }
				)
			}
		}
	}
}


enum class Ts18DashboardAction {
	PHONE_AUTO,
	PERIPHERALS,
	FILE_SHARING,
	SUPERVISION,
	DIAGNOSTICS,
	KEYBOARD_TEST,
	PHONE_KEYBOARD_COMPAT,
	SETTINGS,
	ABOUT,
	BT_SERVER,
	BLE_SERVER
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
		onSelectDevice = { },
		onSelectLeDevice = { }
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
		onSelectDevice = { },
		onSelectLeDevice = { }
	)
}
