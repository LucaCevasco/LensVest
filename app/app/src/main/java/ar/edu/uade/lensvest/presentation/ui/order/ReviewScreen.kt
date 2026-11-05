package ar.edu.uade.lensvest.presentation.ui.order

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.domain.model.OrderResult
import ar.edu.uade.lensvest.presentation.ui.components.GhostButton
import ar.edu.uade.lensvest.presentation.ui.components.IconCircleButton
import ar.edu.uade.lensvest.presentation.ui.components.LetterLogo
import ar.edu.uade.lensvest.presentation.ui.components.PaperCard
import ar.edu.uade.lensvest.presentation.ui.components.PrimaryButton
import ar.edu.uade.lensvest.presentation.ui.components.ScreenColumn
import ar.edu.uade.lensvest.presentation.ui.components.formatQuantity
import ar.edu.uade.lensvest.presentation.ui.components.formatUsd
import ar.edu.uade.lensvest.presentation.ui.components.logoColor
import ar.edu.uade.lensvest.presentation.viewmodel.ReviewViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors

/** 07 · Revisión de la orden: instrument, operation, amount, broker and estimate before anything is sent (RNF01). */
@Composable
fun ReviewScreen(
    onBack: () -> Unit,
    onExecuted: (OrderResult) -> Unit,
    viewModel: ReviewViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.result) { uiState.result?.let(onExecuted) }
    val leave = {
        viewModel.discard()
        onBack()
    }
    BackHandler(onBack = leave)
    val verb = if (viewModel.isSell) "venta" else "compra"

    ScreenColumn(
        actions = {
            uiState.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Terracotta, modifier = Modifier.padding(bottom = 10.dp))
            }
            if (uiState.isSettled) {
                PrimaryButton("Volver", onClick = onBack)
            } else {
                PrimaryButton(
                    "Confirmar $verb",
                    onClick = viewModel::confirm,
                    loading = uiState.isSending,
                    containerColor = LensVestColors.Forest,
                )
                Spacer(Modifier.height(4.dp))
                GhostButton("Cancelar", onClick = leave)
            }
        },
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(R.drawable.ic_arrow_left, "Volver", leave)
            Text(
                "Revisión",
                style = MaterialTheme.typography.labelLarge,
                color = LensVestColors.Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.size(44.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Revisá tu $verb", style = MaterialTheme.typography.headlineLarge, color = LensVestColors.Navy)
        Spacer(Modifier.height(6.dp))
        Text(
            "Todavía no se mandó nada. La orden llega a Berry recién cuando confirmes.",
            style = MaterialTheme.typography.bodyMedium,
            color = LensVestColors.Muted,
        )
        Spacer(Modifier.height(20.dp))
        PaperCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                LetterLogo(viewModel.name, logoColor(viewModel.ticker), size = 44.dp)
                androidx.compose.foundation.layout.Column {
                    Text(viewModel.name, style = MaterialTheme.typography.labelLarge, color = LensVestColors.Navy)
                    Text("${viewModel.ticker} · CEDEAR", style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
                }
            }
            HorizontalDivider(color = LensVestColors.Line)
            val amount = if (viewModel.amountUnit == "USD") formatUsd(viewModel.amount) else "${formatQuantity(viewModel.amount)} acciones"
            ReviewRow("Operación", if (viewModel.isSell) "Venta" else "Compra")
            ReviewRow("Monto", amount)
            viewModel.estimatedShares?.let { ReviewRow("Acciones estimadas", "≈ ${formatQuantity(it)}") }
            viewModel.priceUsd?.let { ReviewRow("Precio de referencia", formatUsd(it)) }
            ReviewRow("Tipo de orden", "A mercado")
            ReviewRow("Broker", "Berry")
        }
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth().background(LensVestColors.Mint, RoundedCornerShape(16.dp)).padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_shield_check), null, tint = LensVestColors.Forest, modifier = Modifier.size(18.dp))
            Text(
                "A mercado: se ejecuta al precio del momento, que puede variar un poco del de referencia.",
                style = MaterialTheme.typography.bodySmall,
                color = LensVestColors.Forest,
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = LensVestColors.Muted)
        Text(value, style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp), color = LensVestColors.Navy)
    }
}
