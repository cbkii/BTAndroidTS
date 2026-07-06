package com.cbkii.btandroidts.presentation.navigation.screens.bt_classic

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import com.cbkii.btandroidts.R
import com.cbkii.btandroidts.presentation.feature_connect.bt_profile.BluetoothProfileRoute
import com.cbkii.btandroidts.presentation.feature_connect.bt_profile.BluetoothProfileViewModel
import com.cbkii.btandroidts.presentation.navigation.UIEventsSideEffect
import com.cbkii.btandroidts.presentation.navigation.args.BluetoothDeviceArgs
import com.cbkii.btandroidts.presentation.navigation.config.RouteAnimation
import com.cbkii.btandroidts.presentation.navigation.config.Routes
import com.cbkii.btandroidts.presentation.util.LocalSharedTransitionVisibilityScopeProvider
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.BtProfileDestination
import com.ramcosta.composedestinations.generated.destinations.ClientRouteDestination
import com.ramcosta.composedestinations.generated.destinations.OppHistoryScreenDestination
import com.ramcosta.composedestinations.generated.destinations.PeripheralDetailScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.cbkii.btandroidts.presentation.feature_connect.bt_profile.ProfileNavigationEvent
import com.cbkii.btandroidts.presentation.navigation.args.PeripheralDetailArgs
import org.koin.androidx.compose.koinViewModel
import kotlin.uuid.toKotlinUuid

@Destination<RootGraph>(
	route = Routes.CLIENT_PROFILE_ROUTE,
	style = RouteAnimation::class,
	navArgs = BluetoothDeviceArgs::class
)
@Composable
fun AnimatedVisibilityScope.BTDeviceProfileScreen(
	navigator: DestinationsNavigator,
	args: BluetoothDeviceArgs,
) {

	val viewmodel = koinViewModel<BluetoothProfileViewModel>()
	val profile by viewmodel.profile.collectAsStateWithLifecycle()

	UIEventsSideEffect(
		events = { viewmodel.uiEvents },
		onPopBack = dropUnlessResumed { navigator.popBackStack() }
	)

	androidx.compose.runtime.LaunchedEffect(viewmodel.navEvents) {
		viewmodel.navEvents.collect { event ->
			when (event) {
				ProfileNavigationEvent.NavigateToHID -> {
					val pArgs = PeripheralDetailArgs(args.address, args.name ?: "Unknown")
					navigator.navigate(PeripheralDetailScreenDestination(pArgs)) {
						popUpTo(BtProfileDestination) { inclusive = true }
					}
				}
				ProfileNavigationEvent.NavigateToOPP -> {
					navigator.navigate(OppHistoryScreenDestination) {
						popUpTo(BtProfileDestination) { inclusive = true }
					}
				}
				is ProfileNavigationEvent.NavigateToRFCOMM -> {
					navigator.navigate(ClientRouteDestination(address = args.address, uuid = event.uuid.toKotlinUuid())) {
						popUpTo(BtProfileDestination) { inclusive = true }
					}
				}
				else -> {}
			}
		}
	}

	CompositionLocalProvider(LocalSharedTransitionVisibilityScopeProvider provides this) {
		BluetoothProfileRoute(
			address = args.address,
			state = profile,
			onEvent = viewmodel::onEvent,
			onConnect = { uuid ->
				viewmodel.onConnectSelected(uuid)
			},
			onTryAll = { viewmodel.onTryAllMethods() },
			navigation = {
				IconButton(onClick = dropUnlessResumed(block = navigator::popBackStack)) {
					Icon(
						imageVector = Icons.AutoMirrored.Default.ArrowBack,
						contentDescription = stringResource(id = R.string.back_arrow)
					)
				}
			}
		)
	}
}