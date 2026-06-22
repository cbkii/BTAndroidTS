package com.cbkii.btandroidts.presentation.navigation.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.cbkii.btandroidts.R
import com.cbkii.btandroidts.presentation.composables.BTAppNavigationDrawer
import com.cbkii.btandroidts.presentation.feature_devices.BTDeviceViewmodel
import com.cbkii.btandroidts.presentation.feature_devices.BTDevicesRoute
import com.cbkii.btandroidts.presentation.feature_devices.Ts18DashboardAction
import com.cbkii.btandroidts.presentation.feature_devices.state.BTDevicesScreenEvents
import com.cbkii.btandroidts.presentation.navigation.UIEventsSideEffect
import com.cbkii.btandroidts.presentation.navigation.args.toArgs
import com.cbkii.btandroidts.presentation.navigation.config.RouteAnimation
import com.cbkii.btandroidts.presentation.navigation.config.Routes
import com.cbkii.btandroidts.presentation.util.LocalSharedTransitionVisibilityScopeProvider
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.BleClientRouteDestination
import com.ramcosta.composedestinations.generated.destinations.BleServerRouteDestination
import com.ramcosta.composedestinations.generated.destinations.BtProfileDestination
import com.ramcosta.composedestinations.generated.destinations.BtServerRouteDestination
import com.ramcosta.composedestinations.generated.destinations.InfoDestination
import com.ramcosta.composedestinations.generated.destinations.KeyboardTestScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@Destination<RootGraph>(
	start = true,
	route = Routes.DEVICES_ROUTE,
	style = RouteAnimation::class
)
@Composable
fun AnimatedVisibilityScope.BTDevicesScreen(
	navigator: DestinationsNavigator
) {
	val viewModel = koinViewModel<BTDeviceViewmodel>()

	val state by viewModel.screenState.collectAsStateWithLifecycle()
	val isBTActive by viewModel.isBTActive.collectAsStateWithLifecycle()
	val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

	UIEventsSideEffect(
		events = { viewModel.uiEvents },
		onPopBack = dropUnlessResumed { navigator.popBackStack() }
	)

	val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
	val scope = rememberCoroutineScope()


	CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
		ModalNavigationDrawer(
			drawerState = drawerState,
			gesturesEnabled = true,
			drawerContent = {
				Column(
					modifier = Modifier
						.fillMaxWidth(.7f)
						.background(androidx.compose.material3.MaterialTheme.colorScheme.surface)
						.padding(dimensionResource(R.dimen.sc_padding))
				) {
					Text(
						text = stringResource(R.string.app_name),
						style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
						modifier = Modifier.padding(bottom = 16.dp)
					)

					NavigationDrawerItem(
						label = { Text(stringResource(R.string.settings_route_title)) },
						selected = false,
						onClick = { navigator.navigate(SettingsDestination) },
						modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
					)
					NavigationDrawerItem(
						label = { Text(stringResource(R.string.about_route_title)) },
						selected = false,
						onClick = { navigator.navigate(InfoDestination) },
						modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
					)

					HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

					Text(
						text = stringResource(R.string.ts18_dashboard_advanced),
						style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
						modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
					)

					NavigationDrawerItem(
						icon = { Icon(Icons.Default.Build, null) },
						label = { Text(stringResource(R.string.bt_server_route)) },
						selected = false,
						onClick = { navigator.navigate(BtServerRouteDestination) },
						modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
					)
					NavigationDrawerItem(
						icon = { Icon(Icons.Default.Build, null) },
						label = { Text(stringResource(R.string.ble_server_title)) },
						selected = false,
						onClick = { navigator.navigate(BleServerRouteDestination) },
						modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
					)
				}
			},
		) {
			BTDevicesRoute(
				state = state,
				isBTActive = isBTActive,
				isScanning = isScanning,
				onEvent = viewModel::onEvents,
				onSelectDevice = { device ->
					val args = device.toArgs()
					navigator.navigate(BtProfileDestination(args))
				},
				onSelectLeDevice = { device ->
					val args = device.toArgs()
					navigator.navigate(BleClientRouteDestination(args))
				},
				onDashboardAction = { action ->
					when (action) {
						Ts18DashboardAction.PHONE_AUTO -> viewModel.onEvents(BTDevicesScreenEvents.OpenTopwayBluetooth)
						Ts18DashboardAction.PERIPHERALS -> viewModel.onEvents(BTDevicesScreenEvents.ShowPeripheralManager)
						Ts18DashboardAction.FILE_SHARING -> viewModel.onEvents(BTDevicesScreenEvents.ShowFileSharing)
						Ts18DashboardAction.SUPERVISION -> viewModel.onEvents(BTDevicesScreenEvents.ManualSupervisorRetry)
						Ts18DashboardAction.DIAGNOSTICS -> viewModel.onEvents(BTDevicesScreenEvents.ExportDiagnostics)
						Ts18DashboardAction.ADVANCED_TOOLS -> viewModel.onEvents(BTDevicesScreenEvents.ShowAdvancedTools)
						Ts18DashboardAction.KEYBOARD_TEST -> navigator.navigate(KeyboardTestScreenDestination)
					}
				},
				navigation = {
					IconButton(
						onClick = { scope.launch { drawerState.open() } },
					) {
						Icon(
							imageVector = Icons.Default.Menu,
							contentDescription = stringResource(id = R.string.menu_option_more)
						)
					}
				},
			)
		}
	}
}
