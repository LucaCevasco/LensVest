package ar.edu.uade.lensvest.presentation.ui.broker

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.presentation.ui.auth.ErrorText
import ar.edu.uade.lensvest.presentation.ui.components.GhostButton
import ar.edu.uade.lensvest.presentation.ui.components.IconCircleButton
import ar.edu.uade.lensvest.presentation.ui.components.LetterLogo
import ar.edu.uade.lensvest.presentation.ui.components.OnboardingTopBar
import ar.edu.uade.lensvest.presentation.ui.components.Pill
import ar.edu.uade.lensvest.presentation.ui.components.PrimaryButton
import ar.edu.uade.lensvest.presentation.ui.components.ScreenColumn
import ar.edu.uade.lensvest.presentation.viewmodel.BrokerViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors

/**
 * 02 · Conectar broker. Shown as step 3 of onboarding ([step] = 3) or from Home ([step] = null).
 * Berry's OAuth redirect comes back through [berryCallback].
 */
@Composable
fun ConnectBrokerScreen(
    step: Int?,
    berryCallback: BerryCallback?,
    onBerryCallbackHandled: () -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: BrokerViewModel = hiltViewModel(),
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
    LaunchedEffect(uiState.isBerryConnected) {
        if (uiState.isBerryConnected) onDone()
    }

    ScreenColumn(
        actions = {
            uiState.error?.let { ErrorText(it) }
            PrimaryButton("Conectar con Berry", onClick = viewModel::connectBerry, loading = uiState.isLoading)
            Spacer(Modifier.height(4.dp))
            GhostButton("Explorar sin conectar", onClick = onDone)
        },
    ) {
        if (step != null) {
            OnboardingTopBar(step = step, total = 3, onBack = onBack)
        } else {
            Box(Modifier.padding(vertical = 8.dp)) { IconCircleButton(R.drawable.ic_arrow_left, "Volver", onBack) }
        }
        Spacer(Modifier.height(20.dp))
        Text("Conectá tu broker", style = MaterialTheme.typography.headlineLarge, color = LensVestColors.Navy)
        Spacer(Modifier.height(10.dp))
        Text(
            "Lo usamos para ver tus acciones y comprar solo cuando vos lo confirmes. Nunca vemos tu contraseña.",
            style = MaterialTheme.typography.bodyMedium,
            color = LensVestColors.Muted,
        )
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            BrokerCard("Berry", "Disponible · Recomendado", LensVestColors.Navy, available = true)
            BrokerCard("Cocos Capital", "Muy pronto", LensVestColors.Terracotta, available = false)
            BrokerCard("InvertirOnline", "Muy pronto", LensVestColors.Sage, available = false)
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth().background(LensVestColors.Mint, RoundedCornerShape(16.dp)).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_shield_check), null, tint = LensVestColors.Forest, modifier = Modifier.size(20.dp))
            Text(
                "Conexión segura. Podés desconectarla cuando quieras.",
                style = MaterialTheme.typography.bodySmall,
                color = LensVestColors.Forest,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun BrokerCard(name: String, subtitle: String, color: Color, available: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = LensVestColors.Paper,
        border = if (available) BorderStroke(2.dp, LensVestColors.Forest) else BorderStroke(1.dp, LensVestColors.Line),
        shadowElevation = if (available) 6.dp else 0.dp,
        modifier = Modifier.fillMaxWidth().alpha(if (available) 1f else 0.6f),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LetterLogo(name, color, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp), color = LensVestColors.Navy)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 13.sp),
                    color = if (available) LensVestColors.Forest else LensVestColors.Muted,
                )
            }
            if (available) {
                Box(Modifier.size(24.dp).background(LensVestColors.Forest, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_check), "Seleccionado", tint = LensVestColors.Cream, modifier = Modifier.size(14.dp))
                }
            } else {
                Pill("Pronto")
            }
        }
    }
}
