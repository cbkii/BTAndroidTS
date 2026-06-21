package com.cbkii.btandroidts.presentation.navigation.screens

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
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
				BTAppNavigationDrawer(
					modifier = Modifier.fillMaxWidth(.7f),
					onNavigateToFeedBackRoute = {
						navigator.navigate(InfoDestination)
					},
					onNavigateToSettingsRoute = {
						navigator.navigate(SettingsDestination)
					},
					onNavigateToClassicServer = {
						navigator.navigate(BtServerRouteDestination)
					},
					onNavigateToBLEServer = {
						navigator.navigate(BleServerRouteDestination)
					},
				)
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
