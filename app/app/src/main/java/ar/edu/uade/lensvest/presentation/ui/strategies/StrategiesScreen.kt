package ar.edu.uade.lensvest.presentation.ui.strategies

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.domain.model.OrderSide
import ar.edu.uade.lensvest.domain.model.Strategy
import ar.edu.uade.lensvest.domain.model.StrategyCondition
import ar.edu.uade.lensvest.presentation.ui.components.ErrorBlock
import ar.edu.uade.lensvest.presentation.ui.components.LoadingBlock
import ar.edu.uade.lensvest.presentation.ui.components.PaperCard
import ar.edu.uade.lensvest.presentation.ui.components.Pill
import ar.edu.uade.lensvest.presentation.ui.components.PrimaryButton
import ar.edu.uade.lensvest.presentation.viewmodel.StrategiesUiState
import ar.edu.uade.lensvest.presentation.viewmodel.StrategiesViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import ar.edu.uade.lensvest.ui.theme.LensVestTheme

private val PausedPill = Color(0xFFE5DDCB)

/** F1 · Estrategias: the rules the user activated from the assistant. */
@Composable
fun StrategiesScreen(onCreate: () -> Unit, viewModel: StrategiesViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    var toDelete by remember { mutableStateOf<Strategy?>(null) }

    StrategiesContent(
        uiState = uiState,
        onCreate = onCreate,
        onRetry = viewModel::load,
        onToggle = { viewModel.setActive(it, !it.isActive) },
        onDelete = { toDelete = it },
    )

    toDelete?.let { strategy ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("¿Borrar la estrategia?") },
            text = { Text("\"${strategy.title}\" deja de existir. Si solo querés frenarla un tiempo, pausala.") },
            confirmButton = {
                TextButton(onClick = { viewModel.delete(strategy); toDelete = null }) {
                    Text("Borrar", color = LensVestColors.Terracotta)
                }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Cancelar", color = LensVestColors.Navy) } },
            containerColor = LensVestColors.Paper,
        )
    }
}

@Composable
private fun StrategiesContent(
    uiState: StrategiesUiState,
    onCreate: () -> Unit,
    onRetry: () -> Unit,
    onToggle: (Strategy) -> Unit,
    onDelete: (Strategy) -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(LensVestColors.Cream).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Estrategias", style = MaterialTheme.typography.headlineLarge, color = LensVestColors.Navy, modifier = Modifier.weight(1f))
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(LensVestColors.Navy).clickable(onClick = onCreate),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_plus), "Nueva estrategia", tint = LensVestColors.Cream, modifier = Modifier.size(22.dp))
            }
        }
        Text(
            "Reglas que armaste con el asistente: \"si NVDA baja 10 %, comprá US$ 100\".",
            style = MaterialTheme.typography.bodyMedium,
            color = LensVestColors.Muted,
        )
        Spacer(Modifier.height(12.dp))
        if (uiState.strategies.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("${uiState.activeCount} ${if (uiState.activeCount == 1) "activa" else "activas"}", container = LensVestColors.Mint, content = LensVestColors.Forest)
                if (uiState.pausedCount > 0) {
                    Pill("${uiState.pausedCount} ${if (uiState.pausedCount == 1) "pausada" else "pausadas"}", container = PausedPill, content = LensVestColors.Muted)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        uiState.error?.let {
            ErrorBlock(it, onRetry = onRetry)
            Spacer(Modifier.height(14.dp))
        }
        when {
            uiState.isLoading && !uiState.hasLoaded -> LoadingBlock("Cargando tus estrategias…")
            uiState.hasLoaded && uiState.strategies.isEmpty() -> EmptyStrategies(onCreate)
            else -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                uiState.strategies.forEach { StrategyCard(it, onToggle = { onToggle(it) }, onDelete = { onDelete(it) }) }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun StrategyCard(strategy: Strategy, onToggle: () -> Unit, onDelete: () -> Unit) {
    val active = strategy.isActive
    PaperCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
            Box(
                Modifier.size(44.dp).background(if (active) LensVestColors.Mint else LensVestColors.Sand, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_repeat), null, tint = if (active) LensVestColors.Forest else LensVestColors.Muted, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    strategy.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp),
                    color = if (active) LensVestColors.Navy else LensVestColors.Navy.copy(alpha = 0.7f),
                )
                Text(strategy.summary(), style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
            }
            StatusPill(active)
        }
        HorizontalDivider(color = LensVestColors.Line)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_clock), null, tint = LensVestColors.Muted, modifier = Modifier.size(16.dp))
            Text(
                if (active) strategy.conditionText() else "Pausada desde ${strategy.updatedAt.toShortDate()}",
                style = MaterialTheme.typography.bodySmall,
                color = LensVestColors.Muted,
                modifier = Modifier.weight(1f),
            )
            RoundAction(
                icon = if (active) R.drawable.ic_pause else R.drawable.ic_play,
                description = if (active) "Pausar" else "Reanudar",
                container = LensVestColors.Sand,
                tint = LensVestColors.Navy,
                onClick = onToggle,
            )
            RoundAction(R.drawable.ic_trash, "Borrar", Color(0xFFF3D9CF), LensVestColors.Terracotta, onDelete)
        }
    }
}

@Composable
private fun StatusPill(active: Boolean) {
    val color = if (active) LensVestColors.Forest else LensVestColors.Muted
    Row(
        Modifier.background(if (active) LensVestColors.Mint else PausedPill, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Text(if (active) "Activa" else "Pausada", style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = color)
    }
}

@Composable
private fun RoundAction(icon: Int, description: String, container: Color, tint: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(container).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun EmptyStrategies(onCreate: () -> Unit) {
    PaperCard {
        Text("Todavía no tenés estrategias", style = MaterialTheme.typography.titleMedium, color = LensVestColors.Navy)
        Text(
            "Pedíselas al asistente en criollo, por ejemplo: \"si Coca-Cola baja 5 %, comprá US$ 10\". Te la mostramos antes de activarla.",
            style = MaterialTheme.typography.bodySmall,
            color = LensVestColors.Muted,
        )
        PrimaryButton("Crear con el asistente", onClick = onCreate, compact = true, containerColor = LensVestColors.Forest)
    }
}

/** "2026-09-12T…" → "12/09". */
private fun String.toShortDate(): String {
    val parts = take(10).split("-")
    return if (parts.size == 3) "${parts[2]}/${parts[1]}" else take(10)
}

@Preview(showBackground = true, widthDp = 360, heightDp = 716)
@Composable
private fun StrategiesPreview() {
    val base = Strategy(1, "NVDA", "NVIDIA", "Comprar NVDA si baja 10 %", StrategyCondition.DROP_PCT, 10.0, 181.99, OrderSide.BUY, 100.0, true, "2026-11-05T22:16:13Z")
    LensVestTheme {
        StrategiesContent(
            StrategiesUiState(
                strategies = listOf(base, base.copy(id = 2, title = "Vender KO si sube a US$ 110", condition = StrategyCondition.PRICE_ABOVE, threshold = 110.0, side = OrderSide.SELL, isActive = false)),
                hasLoaded = true,
            ),
            {}, {}, {}, {},
        )
    }
}
