package ar.edu.uade.lensvest.presentation.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.domain.model.OrderSide
import ar.edu.uade.lensvest.domain.model.PreparedOrder
import ar.edu.uade.lensvest.presentation.ui.components.IconCircleButton
import ar.edu.uade.lensvest.presentation.ui.components.PrimaryButton
import ar.edu.uade.lensvest.presentation.ui.components.ScreenColumn
import ar.edu.uade.lensvest.presentation.ui.components.formatQuantity
import ar.edu.uade.lensvest.presentation.ui.components.formatUsd
import ar.edu.uade.lensvest.presentation.viewmodel.AmountViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import ar.edu.uade.lensvest.ui.theme.fraunces

private val presets = listOf(1, 5, 10)

/** 06 · Monto: how much to buy or sell, typed on the app's own keypad. */
@Composable
fun AmountScreen(
    onBack: () -> Unit,
    onClose: () -> Unit,
    onPrepared: (PreparedOrder) -> Unit,
    viewModel: AmountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(uiState.prepared) {
        uiState.prepared?.let {
            viewModel.onReviewOpened()
            onPrepared(it)
        }
    }
    val selling = viewModel.side == OrderSide.SELL
    val amount = uiState.amount

    ScreenColumn(
        actions = {
            uiState.error?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Terracotta, modifier = Modifier.padding(bottom = 10.dp))
            }
            if (uiState.isPreparing) {
                Text(
                    "Preparando la orden en Berry…",
                    style = MaterialTheme.typography.bodySmall,
                    color = LensVestColors.Muted,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    textAlign = TextAlign.Center,
                )
            }
            PrimaryButton(
                if (selling) "Revisar venta" else "Revisar compra",
                onClick = viewModel::review,
                enabled = (amount ?: 0.0) > 0,
                loading = uiState.isPreparing,
                showArrow = true,
            )
        },
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(R.drawable.ic_arrow_left, "Volver", onBack)
            Text(
                "${if (selling) "Vender" else "Comprar"} · ${viewModel.ticker}",
                style = MaterialTheme.typography.labelLarge,
                color = LensVestColors.Navy,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            IconCircleButton(R.drawable.ic_close, "Cerrar", onClose)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            if (selling) "¿Cuánto querés\nvender?" else "¿Cuánto querés\ninvertir?",
            style = MaterialTheme.typography.headlineLarge,
            color = LensVestColors.Navy,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = LensVestColors.Muted)) { append("US$") }
                append(uiState.text.ifEmpty { "0" })
            },
            style = MaterialTheme.typography.displayLarge.copy(fontFamily = fraunces(64f), fontSize = 64.sp),
            color = LensVestColors.Navy,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        val shares = viewModel.priceUsd?.takeIf { it > 0 }?.let { (amount ?: 0.0) / it }
        shares?.let {
            Text(
                "Son aprox. ${formatQuantity(it)} acciones de ${viewModel.name}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = LensVestColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
            presets.forEach { preset ->
                Chip("US$ $preset", selected = uiState.text == preset.toString()) { viewModel.preset(preset) }
            }
            Chip("Otro", selected = false, onClick = viewModel::clear)
        }
        viewModel.availableUsd?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                buildAnnotatedString {
                    append("Tenés ")
                    withStyle(SpanStyle(color = LensVestColors.Forest)) { append(formatUsd(it)) }
                    append(if (selling) " en ${viewModel.ticker}" else " disponibles en Berry")
                },
                style = MaterialTheme.typography.bodySmall,
                color = LensVestColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Spacer(Modifier.height(20.dp))
        Keypad(onKey = viewModel::type, onBackspace = viewModel::backspace)
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(999.dp)
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) LensVestColors.Cream else LensVestColors.Navy,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) LensVestColors.Navy else LensVestColors.Paper)
            .border(1.dp, if (selected) LensVestColors.Navy else LensVestColors.Line, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    )
}

/** 3×4 keypad: digits, decimal comma and backspace. */
@Composable
private fun Keypad(onKey: (Char) -> Unit, onBackspace: () -> Unit) {
    val rows = listOf("123", "456", "789", ",0<")
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { key ->
                    Box(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp))
                            .clickable { if (key == '<') onBackspace() else onKey(key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (key == '<') {
                            Icon(painterResource(R.drawable.ic_backspace), "Borrar", tint = LensVestColors.Navy, modifier = Modifier.size(24.dp))
                        } else {
                            Text(key.toString(), style = MaterialTheme.typography.headlineSmall.copy(fontFamily = MaterialTheme.typography.bodyLarge.fontFamily), color = LensVestColors.Navy)
                        }
                    }
                }
            }
        }
    }
}

