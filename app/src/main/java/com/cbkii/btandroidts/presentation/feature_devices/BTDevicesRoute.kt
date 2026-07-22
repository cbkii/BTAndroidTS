package com.cbkii.btandroidts.presentation.feature_devices

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.vector.ImageVector
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
				scrollBehavior = scrollBehaviour,
			)
		},
		snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
		modifier = modifier
			.nestedScroll(scrollBehaviour.nestedScrollConnection)
			.padding(top = dimensionResource(R.dimen.ts18_status_bar_height))
			.padding(end = dimensionResource(R.dimen.ts18_nav_bar_width)),
	) { scPadding ->
		BTDevicesContent(
			isBTActive = isBTActive,
			isScanning = isScanning,
			hasBtPermission = hasBtPermission,
			onBTPermissionChanged = { isGranted ->
				hasBtPermission = isGranted
				onEvent(BTDevicesScreenEvents.OnBTPermissionChanged(isGranted))
			},
			hasLocationPermission = hasLocationPermission,
			onLocationPermissionChanged = { isGranted ->
				hasLocationPermission = isGranted
				onEvent(BTDevicesScreenEvents.OnLocationPermissionChanged(isGranted))
			},
			onLeLocationPermissionChanged = { isAccepted ->
				hasLocationPermission = isAccepted
			},
			showLocationBlock = showLocationBlock,
			sidebarCollapsed = sidebarCollapsed,
			onSidebarCollapsedChange = { sidebarCollapsed = it },
			state = state,
			initialTab = initialTab,
			onCurrentTabChanged = { type ->
				currentTab = type
				onEvent(BTDevicesScreenEvents.OnStopAnyRunningScan)
			},
			onSelectDevice = onSelectDevice,
			onSelectLeDevice = onSelectLeDevice,
			onDashboardAction = onDashboardAction,
			scPadding = scPadding
		)
	}
}

@Composable
private fun BTDevicesContent(
	isBTActive: Boolean,
	isScanning: Boolean,
	hasBtPermission: Boolean,
	onBTPermissionChanged: (Boolean) -> Unit,
	hasLocationPermission: Boolean,
	onLocationPermissionChanged: (Boolean) -> Unit,
	onLeLocationPermissionChanged: (Boolean) -> Unit,
	showLocationBlock: Boolean,
	sidebarCollapsed: Boolean,
	onSidebarCollapsedChange: (Boolean) -> Unit,
	state: BTDevicesScreenState,
	initialTab: BluetoothTypes,
	onCurrentTabChanged: (BluetoothTypes) -> Unit,
	onSelectDevice: (BluetoothDeviceModel) -> Unit,
	onSelectLeDevice: (BluetoothLEDeviceModel) -> Unit,
	onDashboardAction: (Ts18DashboardAction) -> Unit,
	scPadding: PaddingValues,
) {
	Row(
		modifier = Modifier
			.fillMaxSize()
			.padding(scPadding)
	) {
		Ts18ActionSidebar(
			collapsed = sidebarCollapsed,
			onCollapsedChange = onSidebarCollapsedChange,
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
				onBTPermissionChanged = onBTPermissionChanged,
				modifier = Modifier
					.fillMaxWidth()
					.weight(1f)
			) {
				BTDevicesTabsLayout(
					isScanning = isScanning,
					initialTab = initialTab,
					onCurrentTabChanged = onCurrentTabChanged,
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
							onLocationPermsAccept = onLocationPermissionChanged,
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
							onLocationPermissionChanged = onLeLocationPermissionChanged,
							modifier = Modifier.fillMaxSize(),
						)
					}
				)
			}
		}
	}
}


@Suppress("DEPRECATION")
@Composable
fun Ts18ActionSidebar(
	collapsed: Boolean,
	onCollapsedChange: (Boolean) -> Unit,
	onAction: (Ts18DashboardAction) -> Unit,
	modifier: Modifier = Modifier,
) {
	val scrollState = rememberScrollState()

	// Calculate responsive width
	val textMeasurer = rememberTextMeasurer()
	val density = LocalDensity.current

	val labels = listOf(
		R.string.ts18_dashboard_phone_auto,
		R.string.ts18_dashboard_peripherals,
		R.string.ts18_dashboard_file_share,
		R.string.ts18_dashboard_supervision,
		R.string.ts18_dashboard_diagnostics,
		R.string.keyboard_test_title,
		R.string.phone_keyboard_title,
		R.string.settings_route_title,
		R.string.about_route_title,
		R.string.bt_server_route,
		R.string.ble_server_title
	).map { stringResource(id = it) }

	val maxLabelWidthDp = remember(labels, density) {
		with(density) {
			labels.maxOfOrNull { textMeasurer.measure(it).size.width }?.toDp() ?: 0.dp
		}
	}

	val iconWidth = 24.dp
	val horizontalPadding = 32.dp // 16dp start + 16dp end
	val iconTextSpacing = 16.dp
	val expandedRequiredWidth = maxLabelWidthDp + iconWidth + horizontalPadding + iconTextSpacing

	androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxHeight()) {
		val collapsedWidth = 80.dp
		val expandedWidth = expandedRequiredWidth
			.coerceAtLeast(150.dp)
			.coerceAtMost(350.dp)

		val actualWidth = if (collapsed) collapsedWidth else expandedWidth

		Surface(
			modifier = Modifier
				.width(actualWidth)
				.fillMaxHeight(),
			color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp
		) {
			Column(modifier = Modifier.fillMaxSize()) {
				// Header
				Row(
					modifier = Modifier
						.fillMaxWidth()
						.padding(top = 12.dp, bottom = 12.dp),
					horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start
				) {
					IconButton(
						onClick = { onCollapsedChange(!collapsed) },
						modifier = if (!collapsed) Modifier.padding(start = 16.dp) else Modifier
					) {
						Icon(
							imageVector = if (collapsed) Icons.Default.Menu else Icons.Filled.MenuOpen,
							contentDescription = stringResource(
								id = if (collapsed) R.string.navigation_expand else R.string.navigation_collapse
							)
						)
					}
				}

				Column(
					modifier = Modifier
						.fillMaxWidth()
						.weight(1f)
						.verticalScroll(scrollState)
				) {
					SidebarItem(collapsed, Icons.Default.Phone, R.string.ts18_dashboard_phone_auto) { onAction(Ts18DashboardAction.PHONE_AUTO) }
					SidebarItem(collapsed, Icons.Default.Devices, R.string.ts18_dashboard_peripherals) { onAction(Ts18DashboardAction.PERIPHERALS) }
					SidebarItem(collapsed, Icons.Filled.Send, R.string.ts18_dashboard_file_share) { onAction(Ts18DashboardAction.FILE_SHARING) }
					SidebarItem(collapsed, Icons.Default.Warning, R.string.ts18_dashboard_supervision) { onAction(Ts18DashboardAction.SUPERVISION) }
					SidebarItem(collapsed, Icons.Default.BugReport, R.string.ts18_dashboard_diagnostics) { onAction(Ts18DashboardAction.DIAGNOSTICS) }
					SidebarItem(collapsed, Icons.Default.Keyboard, R.string.keyboard_test_title) { onAction(Ts18DashboardAction.KEYBOARD_TEST) }
					SidebarItem(collapsed, Icons.Default.Phone, R.string.phone_keyboard_title) { onAction(Ts18DashboardAction.PHONE_KEYBOARD_COMPAT) }
					SidebarItem(collapsed, Icons.Default.Settings, R.string.settings_route_title) { onAction(Ts18DashboardAction.SETTINGS) }
					SidebarItem(collapsed, Icons.Default.Info, R.string.about_route_title) { onAction(Ts18DashboardAction.ABOUT) }
					SidebarItem(collapsed, Icons.Default.Build, R.string.bt_server_route) { onAction(Ts18DashboardAction.BT_SERVER) }
					SidebarItem(collapsed, Icons.Default.Bluetooth, R.string.ble_server_title) { onAction(Ts18DashboardAction.BLE_SERVER) }
				}
			}
		}
	}
}

@Composable
private fun SidebarItem(
	collapsed: Boolean,
	icon: ImageVector,
	labelRes: Int,
	onClick: () -> Unit,
) {
	Row(
		modifier = Modifier
			.fillMaxWidth()
			.clickable(onClick = onClick)
			.padding(vertical = 16.dp, horizontal = if (collapsed) 0.dp else 16.dp),
		verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
		horizontalArrangement = if (collapsed) Arrangement.Center else Arrangement.Start
	) {
		Icon(
			imageVector = icon,
			contentDescription = stringResource(labelRes),
			modifier = Modifier.size(24.dp)
		)
		if (!collapsed) {
			Spacer(modifier = Modifier.width(16.dp))
			Text(
				text = stringResource(labelRes),
				modifier = Modifier.fillMaxWidth(),
				textAlign = androidx.compose.ui.text.style.TextAlign.Start,
				style = androidx.compose.material3.MaterialTheme.typography.labelLarge
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
	KEYBOARD_TEST,
	PHONE_KEYBOARD_COMPAT,
	SETTINGS,
	ABOUT,
	BT_SERVER,
	BLE_SERVER
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

@Preview(showBackground = true, widthDp = 1225, heightDp = 720)
@Composable
private fun BTDeviceRouteNarrowerPreview(
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
