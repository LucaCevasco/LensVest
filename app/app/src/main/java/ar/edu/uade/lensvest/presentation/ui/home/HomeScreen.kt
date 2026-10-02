package ar.edu.uade.lensvest.presentation.ui.home

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.presentation.viewmodel.HomeUiState
import ar.edu.uade.lensvest.presentation.viewmodel.HomeViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestTheme

@Composable
fun HomeScreen(
    berryCallback: BerryCallback?,
    onBerryCallbackHandled: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(berryCallback) {
        berryCallback?.let {
            viewModel.onBerryCallback(it)
            onBerryCallbackHandled()
        }
    }

    LaunchedEffect(uiState.authorizeUrl) {
        uiState.authorizeUrl?.let {
            context.startActivity(Intent(Intent.ACTION_VIEW, it.toUri()))
            viewModel.onAuthorizeUrlOpened()
        }
    }

    HomeContent(
        uiState = uiState,
        onConnectBerry = viewModel::connectBerry,
        onRetry = viewModel::refreshStatus,
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onConnectBerry: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (uiState.isBerryConnected) {
            Text("Berry conectado", style = MaterialTheme.typography.titleLarge)
            uiState.tools.forEach { Text("• $it") }
        } else {
            Button(onClick = onConnectBerry, enabled = !uiState.isLoading) {
                Text("Conectar con Berry")
            }
        }

        if (uiState.isLoading) CircularProgressIndicator()
        uiState.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onRetry, enabled = !uiState.isLoading) { Text("Reintentar") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentConnectedPreview() {
    LensVestTheme {
        HomeContent(
            uiState = HomeUiState(isBerryConnected = true, tools = listOf("get_portfolio", "place_order")),
            onConnectBerry = {},
            onRetry = {},
        )
    }
}
