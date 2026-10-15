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
import ar.edu.uade.lensvest.presentation.ui.common.ComingSoonScreen
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
@Serializable object AssistantRoute
@Serializable object ProfileRoute
@Serializable object LensRoute

private fun MainTab.route(): Any = when (this) {
    MainTab.HOME -> HomeRoute
    MainTab.STRATEGIES -> StrategiesRoute
    MainTab.ASSISTANT -> AssistantRoute
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
                    )
                }
                composable<StrategiesRoute> {
                    ComingSoonScreen("Estrategias", "Acá vas a ver y pausar las reglas que armes con el asistente, como \"si KO baja 10 %, comprá USD 50\".")
                }
                composable<AssistantRoute> { AssistantScreen() }
                composable<ProfileRoute> {
                    ComingSoonScreen("Perfil", "Tu nivel, el modo de la ficha (Simple o Pro) y los brokers conectados van a vivir acá.")
                }
                composable<LensRoute> { LensScreen() }
            }
        }
    }
}
