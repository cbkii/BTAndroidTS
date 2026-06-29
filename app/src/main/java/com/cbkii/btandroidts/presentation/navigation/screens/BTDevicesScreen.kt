package com.cbkii.btandroidts.presentation.navigation.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
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
import com.ramcosta.composedestinations.generated.destinations.KeyboardTestDestination
import com.ramcosta.composedestinations.generated.destinations.SettingsDestination
import com.ramcosta.composedestinations.generated.destinations.PhoneKeyboardDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator

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

	CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
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
					Ts18DashboardAction.KEYBOARD_TEST -> navigator.navigate(KeyboardTestDestination)
					Ts18DashboardAction.PHONE_KEYBOARD_COMPAT -> navigator.navigate(PhoneKeyboardDestination)
					Ts18DashboardAction.SETTINGS -> navigator.navigate(SettingsDestination)
					Ts18DashboardAction.ABOUT -> navigator.navigate(InfoDestination)
					Ts18DashboardAction.BT_SERVER -> navigator.navigate(BtServerRouteDestination)
					Ts18DashboardAction.BLE_SERVER -> navigator.navigate(BleServerRouteDestination)
				}
			},
			navigation = {},
		)
	}
}
