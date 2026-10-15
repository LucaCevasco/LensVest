package ar.edu.uade.lensvest.presentation.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.presentation.ui.components.IconCircleButton
import ar.edu.uade.lensvest.presentation.ui.components.LetterLogo
import ar.edu.uade.lensvest.presentation.ui.components.rememberComingSoon
import ar.edu.uade.lensvest.presentation.viewmodel.HomeUiState
import ar.edu.uade.lensvest.presentation.viewmodel.HomeViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import ar.edu.uade.lensvest.ui.theme.LensVestTheme
import java.util.Calendar

/** B1 · Inicio (todavía sin inversiones). */
@Composable
fun HomeScreen(
    onOpenLens: () -> Unit,
    onOpenAssistant: () -> Unit,
    onConnectBroker: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    val comingSoon = rememberComingSoon()
    HomeContent(
        uiState = uiState,
        onOpenLens = onOpenLens,
        onOpenAssistant = onOpenAssistant,
        onConnectBroker = onConnectBroker,
        onNotifications = { comingSoon("Notificaciones") },
    )
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onOpenLens: () -> Unit,
    onOpenAssistant: () -> Unit,
    onConnectBroker: () -> Unit,
    onNotifications: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(LensVestColors.Cream)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(greeting(), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = LensVestColors.Muted)
                Text(uiState.userName, style = MaterialTheme.typography.headlineMedium, color = LensVestColors.Navy)
            }
            IconCircleButton(R.drawable.ic_bell, "Notificaciones", onNotifications)
        }
        Spacer(Modifier.height(16.dp))
        EmptyPortfolioCard(onScan = onOpenLens)
        Spacer(Modifier.height(18.dp))
        Text("IDEAS PARA ARRANCAR", style = MaterialTheme.typography.labelSmall, color = LensVestColors.Muted)
        Spacer(Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Not in B1: without a broker the app can't show or buy anything, so offer it first.
            if (uiState.isBerryConnected == false) {
                IdeaRow("B", LensVestColors.Navy, "Conectá tu broker", "Vinculá Berry para ver y operar tus inversiones.", onConnectBroker)
            }
            IdeaRow("C", LensVestColors.Terracotta, "¿Tomás Coca-Cola?", "Apuntale a la lata y mirá cuánto vale la empresa.", onOpenLens)
            IdeaRow("A", LensVestColors.Forest, "¿Por dónde arranco?", "Preguntale al asistente, en criollo.", onOpenAssistant)
        }
        Spacer(Modifier.height(24.dp))
    }
}

// Calendar instead of java.time: minSdk 24 has no java.time without desugaring.
private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 6..12 -> "Buen día,"
    in 13..19 -> "Buenas tardes,"
    else -> "Buenas noches,"
}

@Composable
private fun EmptyPortfolioCard(onScan: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(LensVestColors.Navy, RoundedCornerShape(28.dp)).padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Image(painterResource(R.drawable.home_empty_illustration), null, Modifier.size(120.dp, 96.dp))
        Text(
            buildAnnotatedString {
                append("Todavía no empezaste\na ")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = LensVestColors.MintBright)) { append("invertir") }
            },
            style = MaterialTheme.typography.headlineSmall,
            color = Color(0xFFF5F0E3),
            textAlign = TextAlign.Center,
        )
        Text(
            "Escaneá un producto de tu marca favorita y arrancamos juntos. Es más fácil de lo que parece.",
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            color = LensVestColors.Cream.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(LensVestColors.Cream)
                .clickable(onClick = onScan).padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_scan), null, tint = LensVestColors.Navy, modifier = Modifier.size(18.dp))
            Text("Escanear mi primer producto", style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = LensVestColors.Navy)
        }
    }
}

@Composable
private fun IdeaRow(letter: String, color: Color, title: String, subtitle: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(LensVestColors.Paper).border(1.dp, LensVestColors.Line, shape)
            .clickable(onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LetterLogo(letter, color, size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = LensVestColors.Navy)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
        }
        Icon(painterResource(R.drawable.ic_chevron_right), null, tint = LensVestColors.Muted, modifier = Modifier.size(18.dp))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 716)
@Composable
private fun HomePreview() {
    LensVestTheme {
        HomeContent(HomeUiState(userName = "Martina", isBerryConnected = true), {}, {}, {}, {})
    }
}
