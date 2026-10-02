package ar.edu.uade.lensvest.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ar.edu.uade.lensvest.presentation.ui.auth.AuthScreen
import ar.edu.uade.lensvest.presentation.ui.home.BerryCallback
import ar.edu.uade.lensvest.presentation.ui.home.HomeScreen
import kotlinx.serialization.Serializable

@Serializable
object AuthRoute

@Serializable
object HomeRoute

@Composable
fun AppNavigation(
    berryCallback: BerryCallback?,
    onBerryCallbackHandled: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AuthRoute, modifier = modifier) {
        composable<AuthRoute> {
            AuthScreen(
                onLoggedIn = {
                    navController.navigate(HomeRoute) {
                        popUpTo<AuthRoute> { inclusive = true }
                    }
                },
            )
        }
        composable<HomeRoute> {
            // A Berry redirect that arrives before login waits until Home is shown.
            HomeScreen(
                berryCallback = berryCallback,
                onBerryCallbackHandled = onBerryCallbackHandled,
            )
        }
    }
}
