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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import ar.edu.uade.lensvest.domain.model.Portfolio
import ar.edu.uade.lensvest.domain.model.Position
import ar.edu.uade.lensvest.presentation.ui.components.AssetRow
import ar.edu.uade.lensvest.presentation.ui.components.ErrorBlock
import ar.edu.uade.lensvest.presentation.ui.components.PaperCard
import ar.edu.uade.lensvest.presentation.ui.components.Pill
import ar.edu.uade.lensvest.presentation.ui.components.SectionHeader
import ar.edu.uade.lensvest.presentation.ui.components.Sparkline
import ar.edu.uade.lensvest.presentation.ui.components.formatPct
import ar.edu.uade.lensvest.presentation.ui.components.formatQuantity
import ar.edu.uade.lensvest.presentation.ui.components.formatUsd
import ar.edu.uade.lensvest.presentation.ui.strategies.summary
import ar.edu.uade.lensvest.presentation.ui.components.IconCircleButton
import ar.edu.uade.lensvest.presentation.ui.components.LetterLogo
import ar.edu.uade.lensvest.presentation.ui.components.rememberComingSoon
import ar.edu.uade.lensvest.presentation.viewmodel.HomeUiState
import ar.edu.uade.lensvest.presentation.viewmodel.HomeViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import ar.edu.uade.lensvest.ui.theme.LensVestTheme
import java.util.Calendar

/** B1 · Inicio (todavía sin inversiones) and B2 · Home (con inversiones). */
@Composable
fun HomeScreen(
    onOpenLens: () -> Unit,
    onOpenAssistant: () -> Unit,
    onConnectBroker: () -> Unit,
    onOpenStrategies: () -> Unit,
    onOpenAsset: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    val comingSoon = rememberComingSoon()
    HomeContent(
        uiState = uiState,
        actions = HomeActions(
            onOpenLens = onOpenLens,
            onOpenAssistant = onOpenAssistant,
            onConnectBroker = onConnectBroker,
            onOpenStrategies = onOpenStrategies,
            onOpenAsset = onOpenAsset,
            onRetry = viewModel::retryPortfolio,
            onNotifications = { comingSoon("Notificaciones") },
        ),
    )
}

private class HomeActions(
    val onOpenLens: () -> Unit,
    val onOpenAssistant: () -> Unit,
    val onConnectBroker: () -> Unit,
    val onOpenStrategies: () -> Unit,
    val onOpenAsset: (String) -> Unit,
    val onRetry: () -> Unit,
    val onNotifications: () -> Unit,
)

@Composable
private fun HomeContent(uiState: HomeUiState, actions: HomeActions) {
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
            IconCircleButton(R.drawable.ic_bell, "Notificaciones", actions.onNotifications)
        }
        Spacer(Modifier.height(16.dp))
        val portfolio = uiState.portfolio
        when {
            portfolio?.hasInvestments == true -> {
                InvestedHome(portfolio, uiState, actions)
                return@Column
            }
            portfolio == null && uiState.isPortfolioLoading -> {
                PortfolioLoadingCard()
                Spacer(Modifier.height(18.dp))
            }
            portfolio == null && uiState.portfolioError != null -> {
                ErrorBlock(uiState.portfolioError, onRetry = actions.onRetry)
                Spacer(Modifier.height(18.dp))
            }
            else -> {
                EmptyPortfolioCard(onScan = actions.onOpenLens)
                Spacer(Modifier.height(18.dp))
            }
        }
        IdeasToStart(uiState, actions)
    }
}

@Composable
private fun IdeasToStart(uiState: HomeUiState, actions: HomeActions) {
    val onOpenLens = actions.onOpenLens
    val onOpenAssistant = actions.onOpenAssistant
    val onConnectBroker = actions.onConnectBroker
    Column {
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

/** B2: portfolio card, shortcuts, active strategies and positions. */
@Composable
private fun InvestedHome(portfolio: Portfolio, uiState: HomeUiState, actions: HomeActions) {
    PortfolioCard(portfolio, isRefreshing = uiState.isPortfolioLoading)
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ShortcutCard(R.drawable.ic_scan, LensVestColors.Navy, "Lens Mode", "Escaneá", actions.onOpenLens, Modifier.weight(1f))
        ShortcutCard(R.drawable.ic_sparkles, LensVestColors.Forest, "Asistente", "Preguntá", actions.onOpenAssistant, Modifier.weight(1f))
    }
    uiState.activeStrategies.firstOrNull()?.let { first ->
        Spacer(Modifier.height(22.dp))
        SectionHeader("Estrategias activas", action = "Ver todas", onAction = actions.onOpenStrategies)
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(LensVestColors.Mint)
                .clickable(onClick = actions.onOpenStrategies).padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_repeat), null, tint = LensVestColors.Forest, modifier = Modifier.size(22.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(first.title, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = LensVestColors.Navy)
                Text(first.summary(), style = MaterialTheme.typography.bodySmall, color = LensVestColors.Forest)
            }
            val more = uiState.activeStrategies.size - 1
            if (more > 0) Pill("+$more más", container = LensVestColors.Paper, content = LensVestColors.Navy, fontSize = 11)
        }
    }
    Spacer(Modifier.height(22.dp))
    SectionHeader("Tus posiciones")
    Spacer(Modifier.height(10.dp))
    PaperCard {
        portfolio.positions.forEachIndexed { index, position ->
            if (index > 0) HorizontalDivider(color = LensVestColors.Line)
            AssetRow(
                title = position.name,
                subtitle = "${position.ticker} · ${formatQuantity(position.quantity)} acciones",
                value = formatUsd(position.valueUsd),
                changePct = position.changePct,
                onClick = { actions.onOpenAsset(position.ticker) },
                logoKey = position.ticker,
            )
        }
    }
    Spacer(Modifier.height(24.dp))
}

@Composable
private fun PortfolioCard(portfolio: Portfolio, isRefreshing: Boolean) {
    Column(
        Modifier.fillMaxWidth().background(LensVestColors.Navy, RoundedCornerShape(28.dp)).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("TU PORTFOLIO EN BERRY", style = MaterialTheme.typography.labelSmall, color = LensVestColors.Cream.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
            if (isRefreshing) {
                CircularProgressIndicator(color = LensVestColors.Cream, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            } else {
                Pill("1M", container = LensVestColors.Cream, content = LensVestColors.Navy, fontSize = 11)
            }
        }
        Text(formatUsd(portfolio.totalValueUsd), style = MaterialTheme.typography.displayLarge, color = LensVestColors.Cream)
        portfolio.changePct?.let { pct ->
            val usd = portfolio.changeUsd?.let { (if (it >= 0) "+" else "−") + formatUsd(kotlin.math.abs(it)) + " · " }.orEmpty()
            Row(
                Modifier.background(LensVestColors.Sage, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(if (pct >= 0) R.drawable.ic_trending_up else R.drawable.ic_trending_down), null, tint = LensVestColors.Cream, modifier = Modifier.size(12.dp))
                Text("$usd${formatPct(pct)} este mes", style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = LensVestColors.Cream)
            }
        }
        if (portfolio.history.size >= 2) {
            Sparkline(portfolio.history, LensVestColors.MintBright, Modifier.fillMaxWidth().height(64.dp), fill = Color.White.copy(alpha = 0.08f))
        }
        AllocationBar(portfolio)
        portfolio.cashUsd?.let {
            Text("Disponible para invertir: ${formatUsd(it)}", style = MaterialTheme.typography.bodySmall, color = LensVestColors.Cream.copy(alpha = 0.7f))
        }
    }
}

/** How the portfolio splits between positions and cash. */
@Composable
private fun AllocationBar(portfolio: Portfolio) {
    val colors = listOf(LensVestColors.MintBright, LensVestColors.Sand, LensVestColors.Terracotta, LensVestColors.Sage)
    val parts = portfolio.positions.map { it.valueUsd } + listOfNotNull(portfolio.cashUsd)
    val total = parts.sum().takeIf { it > 0 } ?: return
    Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        parts.forEachIndexed { index, value ->
            if (value > 0) {
                Box(
                    Modifier.weight((value / total).toFloat().coerceAtLeast(0.02f)).fillMaxHeight()
                        .background(colors[index % colors.size], RoundedCornerShape(3.dp)),
                )
            }
        }
    }
}

@Composable
private fun PortfolioLoadingCard() {
    Column(
        Modifier.fillMaxWidth().background(LensVestColors.Navy, RoundedCornerShape(28.dp)).padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("TU PORTFOLIO EN BERRY", style = MaterialTheme.typography.labelSmall, color = LensVestColors.Cream.copy(alpha = 0.7f))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = LensVestColors.MintBright, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
            Text("Consultando tu cuenta de Berry…", style = MaterialTheme.typography.bodyMedium, color = LensVestColors.Cream)
        }
    }
}

@Composable
private fun ShortcutCard(icon: Int, color: Color, title: String, subtitle: String, onClick: () -> Unit, modifier: Modifier) {
    val shape = RoundedCornerShape(20.dp)
    Row(
        modifier.clip(shape).background(LensVestColors.Paper).border(1.dp, LensVestColors.Line, shape).clickable(onClick = onClick).padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(color, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), null, tint = LensVestColors.Cream, modifier = Modifier.size(20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = LensVestColors.Navy)
            Text(subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = LensVestColors.Muted)
        }
    }
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

private val previewActions = HomeActions({}, {}, {}, {}, {}, {}, {})

@Preview(showBackground = true, widthDp = 360, heightDp = 716)
@Composable
private fun HomePreview() {
    LensVestTheme {
        HomeContent(HomeUiState(userName = "Martina", isBerryConnected = true), previewActions)
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun InvestedHomePreview() {
    val portfolio = Portfolio(
        totalValueUsd = 112.48, cashUsd = 25.0, changeUsd = 6.23, changePct = 5.9,
        history = listOf(100.0, 102.0, 101.0, 104.0, 106.0, 105.0, 109.0, 112.48),
        positions = listOf(
            Position("KO", "Coca-Cola", 0.38, 36.9, 8.4),
            Position("PG", "P&G", 0.31, 50.96, 6.1),
        ),
    )
    LensVestTheme {
        HomeContent(HomeUiState(userName = "Martina", isBerryConnected = true, portfolio = portfolio), previewActions)
    }
}
