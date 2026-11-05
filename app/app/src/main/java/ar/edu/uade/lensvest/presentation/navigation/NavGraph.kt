package ar.edu.uade.lensvest.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ar.edu.uade.lensvest.presentation.ui.assistant.AssistantScreen
import ar.edu.uade.lensvest.presentation.ui.auth.LoginScreen
import ar.edu.uade.lensvest.presentation.ui.auth.RegisterScreen
import ar.edu.uade.lensvest.presentation.ui.broker.BerryCallback
import ar.edu.uade.lensvest.presentation.ui.broker.ConnectBrokerScreen
import ar.edu.uade.lensvest.domain.model.OrderSide
import ar.edu.uade.lensvest.presentation.ui.asset.AssetScreen
import ar.edu.uade.lensvest.presentation.ui.history.HistoryScreen
import ar.edu.uade.lensvest.presentation.ui.history.SearchScreen
import ar.edu.uade.lensvest.presentation.ui.order.AmountScreen
import ar.edu.uade.lensvest.presentation.ui.order.ReviewScreen
import ar.edu.uade.lensvest.presentation.ui.order.SuccessScreen
import ar.edu.uade.lensvest.presentation.ui.profile.ProfileScreen
import ar.edu.uade.lensvest.presentation.ui.strategies.StrategiesScreen
import ar.edu.uade.lensvest.presentation.ui.components.LensVestBottomBar
import ar.edu.uade.lensvest.presentation.ui.components.LocalSnackbarHostState
import ar.edu.uade.lensvest.presentation.ui.components.MainTab
import ar.edu.uade.lensvest.presentation.ui.home.HomeScreen
import ar.edu.uade.lensvest.presentation.ui.lens.LensScreen
import ar.edu.uade.lensvest.presentation.ui.onboarding.LevelScreen
import ar.edu.uade.lensvest.presentation.ui.welcome.WelcomeScreen
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import kotlinx.serialization.Serializable

@Serializable object WelcomeRoute
@Serializable object LoginRoute
@Serializable object RegisterRoute
@Serializable object LevelRoute
@Serializable data class ConnectBrokerRoute(val fromOnboarding: Boolean)
@Serializable object HomeRoute
@Serializable object StrategiesRoute
/** [prefill] is sent as the user's first message, e.g. from Lens Mode's "Invertir en KO". */
@Serializable data class AssistantRoute(val prefill: String? = null)
@Serializable object ProfileRoute
@Serializable object ProfileLevelRoute
@Serializable object LensRoute
@Serializable object HistoryRoute
@Serializable object SearchRoute
/** [brand] and [scanId] are set when the sheet opens from a scan, e.g. "Sprite" for KO. */
@Serializable data class AssetRoute(val ticker: String, val brand: String? = null, val scanId: Long = 0, val favorite: Boolean = false)
// Amounts travel as strings: they're money, and the route only carries them to the next screen.
@Serializable data class AmountRoute(val ticker: String, val name: String, val side: String, val priceUsd: String, val availableUsd: String? = null)
@Serializable data class ReviewRoute(val ticker: String, val name: String, val side: String, val amount: String, val amountUnit: String, val priceUsd: String?)
@Serializable data class SuccessRoute(val name: String, val ticker: String, val side: String, val amount: String, val orderId: String?)

private fun OrderSide.wire() = if (this == OrderSide.SELL) "sell" else "buy"

private fun MainTab.route(): Any = when (this) {
    MainTab.HOME -> HomeRoute
    MainTab.STRATEGIES -> StrategiesRoute
    MainTab.ASSISTANT -> AssistantRoute()
    MainTab.PROFILE -> ProfileRoute
}

private fun NavDestination?.currentTab(): MainTab? = MainTab.entries.firstOrNull { tab ->
    this?.hasRoute(tab.route()::class) == true
}

/** Home is the root once the user is in the app (enterApp clears onboarding), so tabs pop back to it. */
private fun NavHostController.openTab(tab: MainTab) = navigate(tab.route()) {
    popUpTo<HomeRoute> { saveState = true }
    launchSingleTop = true
    restoreState = true
}

/** Leaves onboarding for the main app; back from Home exits instead of returning to the forms. */
private fun NavHostController.enterApp() = navigate(HomeRoute) {
    popUpTo(0) { inclusive = true }
}

@Composable
fun AppNavigation(
    berryCallback: BerryCallback?,
    onBerryCallbackHandled: () -> Unit,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination.currentTab()

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            containerColor = LensVestColors.Cream,
            // Each screen handles its own insets: Lens draws behind the status bar.
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState, Modifier.navigationBarsPadding()) },
            bottomBar = {
                if (currentTab != null) {
                    LensVestBottomBar(
                        selected = currentTab,
                        onSelect = navController::openTab,
                        onLens = { navController.navigate(LensRoute) },
                    )
                }
            },
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = WelcomeRoute,
                modifier = Modifier.padding(padding),
            ) {
                composable<WelcomeRoute> {
                    WelcomeScreen(
                        onStart = { navController.navigate(RegisterRoute) },
                        onLogin = { navController.navigate(LoginRoute) },
                    )
                }
                composable<LoginRoute> {
                    LoginScreen(
                        onLoggedIn = navController::enterApp,
                        onCreateAccount = { navController.navigate(RegisterRoute) },
                    )
                }
                composable<RegisterRoute> {
                    RegisterScreen(
                        onRegistered = {
                            navController.navigate(LevelRoute) { popUpTo(0) { inclusive = true } }
                        },
                        onBack = navController::popBackStack,
                    )
                }
                composable<LevelRoute> {
                    LevelScreen(onContinue = { navController.navigate(ConnectBrokerRoute(fromOnboarding = true)) })
                }
                composable<ProfileLevelRoute> {
                    LevelScreen(onContinue = navController::popBackStack, onBack = navController::popBackStack)
                }
                composable<ConnectBrokerRoute> { entry ->
                    val fromOnboarding = entry.toRoute<ConnectBrokerRoute>().fromOnboarding
                    ConnectBrokerScreen(
                        step = if (fromOnboarding) 3 else null,
                        berryCallback = berryCallback,
                        onBerryCallbackHandled = onBerryCallbackHandled,
                        onDone = { if (fromOnboarding) navController.enterApp() else navController.popBackStack() },
                        onBack = navController::popBackStack,
                    )
                }
                composable<HomeRoute> {
                    HomeScreen(
                        onOpenLens = { navController.navigate(LensRoute) },
                        onOpenAssistant = { navController.openTab(MainTab.ASSISTANT) },
                        onConnectBroker = { navController.navigate(ConnectBrokerRoute(fromOnboarding = false)) },
                        onOpenStrategies = { navController.openTab(MainTab.STRATEGIES) },
                        onOpenAsset = { navController.navigate(AssetRoute(it)) },
                    )
                }
                composable<StrategiesRoute> {
                    StrategiesScreen(onCreate = { navController.openTab(MainTab.ASSISTANT) })
                }
                composable<AssistantRoute> { AssistantScreen() }
                composable<ProfileRoute> {
                    ProfileScreen(
                        onChangeLevel = { navController.navigate(ProfileLevelRoute) },
                        onManageBroker = { navController.navigate(ConnectBrokerRoute(fromOnboarding = false)) },
                        onLoggedOut = { navController.navigate(WelcomeRoute) { popUpTo(0) { inclusive = true } } },
                    )
                }
                composable<LensRoute> {
                    LensScreen(
                        onOpenAsset = { ticker, brand, scanId -> navController.navigate(AssetRoute(ticker, brand, scanId ?: 0)) },
                        onOpenHistory = { navController.navigate(HistoryRoute) },
                        onSearch = { navController.navigate(SearchRoute) },
                    )
                }
                composable<HistoryRoute> {
                    HistoryScreen(
                        onBack = navController::popBackStack,
                        onOpenScan = { scan ->
                            scan.ticker?.let { navController.navigate(AssetRoute(it, scan.brand, scan.id, scan.favorite)) }
                        },
                        onSearch = { navController.navigate(SearchRoute) },
                        onScan = { navController.navigate(LensRoute) { popUpTo<LensRoute> { inclusive = true } } },
                    )
                }
                composable<SearchRoute> {
                    SearchScreen(
                        onBack = navController::popBackStack,
                        onOpenAsset = { navController.navigate(AssetRoute(it.ticker)) },
                    )
                }
                composable<AssetRoute> {
                    AssetScreen(
                        onBack = navController::popBackStack,
                        onTrade = { trade ->
                            navController.navigate(
                                AmountRoute(
                                    ticker = trade.ticker,
                                    name = trade.name,
                                    side = trade.side.wire(),
                                    priceUsd = trade.priceUsd.toString(),
                                    availableUsd = trade.availableUsd?.toString(),
                                ),
                            )
                        },
                    )
                }
                composable<AmountRoute> { entry ->
                    val route = entry.toRoute<AmountRoute>()
                    AmountScreen(
                        onBack = navController::popBackStack,
                        onClose = navController::popBackStack,
                        onPrepared = { order ->
                            navController.navigate(
                                ReviewRoute(
                                    ticker = order.ticker,
                                    name = route.name,
                                    side = order.side.wire(),
                                    amount = order.amount.toString(),
                                    amountUnit = order.amountUnit,
                                    priceUsd = route.priceUsd,
                                ),
                            )
                        },
                    )
                }
                composable<ReviewRoute> { entry ->
                    val route = entry.toRoute<ReviewRoute>()
                    ReviewScreen(
                        onBack = navController::popBackStack,
                        onExecuted = { result ->
                            navController.navigate(SuccessRoute(route.name, route.ticker, route.side, route.amount, result.orderId)) {
                                // Back from the success screen returns to the sheet, not to a sent order.
                                popUpTo<AmountRoute> { inclusive = true }
                            }
                        },
                    )
                }
                composable<SuccessRoute> { entry ->
                    val route = entry.toRoute<SuccessRoute>()
                    SuccessScreen(
                        name = route.name,
                        ticker = route.ticker,
                        isSell = route.side == "sell",
                        amountUsd = route.amount.toDoubleOrNull() ?: 0.0,
                        orderId = route.orderId,
                        onScanAnother = { navController.navigate(LensRoute) { popUpTo<HomeRoute>() } },
                        onSeeInvestments = { navController.navigate(HomeRoute) { popUpTo<HomeRoute> { inclusive = true } } },
                    )
                }
            }
        }
    }
}
