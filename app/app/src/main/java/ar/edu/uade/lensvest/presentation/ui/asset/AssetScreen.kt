package ar.edu.uade.lensvest.presentation.ui.asset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.domain.model.AssetDetail
import ar.edu.uade.lensvest.domain.model.Holding
import ar.edu.uade.lensvest.domain.model.OrderSide
import ar.edu.uade.lensvest.domain.model.ProFigures
import ar.edu.uade.lensvest.domain.model.SheetMode
import ar.edu.uade.lensvest.presentation.ui.components.ChangePill
import ar.edu.uade.lensvest.presentation.ui.components.ErrorBlock
import ar.edu.uade.lensvest.presentation.ui.components.IconCircleButton
import ar.edu.uade.lensvest.presentation.ui.components.LetterLogo
import ar.edu.uade.lensvest.presentation.ui.components.LoadingBlock
import ar.edu.uade.lensvest.presentation.ui.components.PaperCard
import ar.edu.uade.lensvest.presentation.ui.components.Pill
import ar.edu.uade.lensvest.presentation.ui.components.PrimaryButton
import ar.edu.uade.lensvest.presentation.ui.components.ScreenColumn
import ar.edu.uade.lensvest.presentation.ui.components.SecondaryButton
import ar.edu.uade.lensvest.presentation.ui.components.SegmentedToggle
import ar.edu.uade.lensvest.presentation.ui.components.Sparkline
import ar.edu.uade.lensvest.presentation.ui.components.formatQuantity
import ar.edu.uade.lensvest.presentation.ui.components.formatUsd
import ar.edu.uade.lensvest.presentation.ui.components.logoColor
import ar.edu.uade.lensvest.presentation.viewmodel.AssetContent
import ar.edu.uade.lensvest.presentation.viewmodel.AssetUiState
import ar.edu.uade.lensvest.presentation.viewmodel.AssetViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import ar.edu.uade.lensvest.ui.theme.LensVestTheme

/** What the sheet hands to the amount screen when the user buys or sells. */
data class TradeRequest(val ticker: String, val name: String, val side: OrderSide, val priceUsd: Double, val availableUsd: Double?)

/** 05 · Ficha de la empresa, 05b with the user's position, 05c in Pro mode. */
@Composable
fun AssetScreen(
    onBack: () -> Unit,
    onTrade: (TradeRequest) -> Unit,
    viewModel: AssetViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AssetContent(
        uiState = uiState,
        onBack = onBack,
        onMode = viewModel::setMode,
        onFavorite = viewModel::toggleFavorite,
        onRetry = viewModel::load,
        onTrade = onTrade,
    )
}

@Composable
private fun AssetContent(
    uiState: AssetUiState,
    onBack: () -> Unit,
    onMode: (SheetMode) -> Unit,
    onFavorite: () -> Unit,
    onRetry: () -> Unit,
    onTrade: (TradeRequest) -> Unit,
) {
    val content = uiState.content
    val asset = content.asset
    ScreenColumn(
        actions = { if (asset != null) TradeButtons(asset, onTrade) },
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(R.drawable.ic_arrow_left, "Volver", onBack)
            Spacer(Modifier.weight(1f))
            if (content.canFavorite) {
                IconCircleButton(
                    if (content.favorite) R.drawable.ic_star_filled else R.drawable.ic_star,
                    if (content.favorite) "Quitar de favoritos" else "Agregar a favoritos",
                    onFavorite,
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        SegmentedToggle(
            options = listOf("Simple", "✦ Pro · más datos"),
            selected = if (uiState.mode == SheetMode.PRO) 1 else 0,
            onSelect = { onMode(if (it == 1) SheetMode.PRO else SheetMode.SIMPLE) },
        )
        Spacer(Modifier.height(18.dp))
        Title(content)
        Spacer(Modifier.height(18.dp))
        when {
            asset != null -> AssetBody(asset, uiState)
            content.error != null -> ErrorBlock(content.error, onRetry)
            else -> LoadingBlock("Buscando ${content.ticker} en Berry…")
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Title(content: AssetContent) {
    val name = content.asset?.name
    val title = content.brand ?: name ?: content.ticker
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        LetterLogo(title, if (content.brand != null) LensVestColors.Forest else logoColor(content.ticker), size = 56.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = LensVestColors.Navy)
            // "Sprite es de The Coca-Cola Company": only when the brand isn't the company's own name.
            if (name != null && content.brand != null && !name.contains(content.brand, ignoreCase = true)) {
                Text(
                    buildAnnotatedString {
                        append("es de ")
                        withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = LensVestColors.Navy)) { append(name) }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = LensVestColors.Muted,
                )
            }
        }
    }
}

@Composable
private fun AssetBody(asset: AssetDetail, uiState: AssetUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        PriceCard(asset)
        if (uiState.mode == SheetMode.PRO) {
            ProFiguresCard(asset)
        }
        asset.position?.let { PositionCard(it) }
        if (uiState.mode == SheetMode.PRO || uiState.plainExplanations) {
            AboutCard(asset)
        }
        val note = asset.position?.avgPriceUsd?.let { "Las compraste a ${formatUsd(it)} cada una, en promedio." }
            ?: "Todavía no tenés acciones de ${asset.name}".takeIf { asset.position == null }
        note?.let {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_info), null, tint = LensVestColors.Muted, modifier = Modifier.size(16.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
            }
        }
    }
}

@Composable
private fun PriceCard(asset: AssetDetail) {
    PaperCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("PRECIO DE LA ACCIÓN", style = MaterialTheme.typography.labelSmall, color = LensVestColors.Muted, modifier = Modifier.weight(1f))
            Pill("${asset.ticker} · CEDEAR", container = LensVestColors.Sand, content = LensVestColors.Navy, fontSize = 11)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(formatUsd(asset.priceUsd), style = MaterialTheme.typography.displayLarge, color = LensVestColors.Navy)
            asset.changePct?.let { ChangePill(it, suffix = " hoy") }
        }
        Sparkline(asset.history, LensVestColors.Forest, Modifier.fillMaxWidth().height(64.dp))
        Text("Último mes · datos de Berry", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = LensVestColors.Muted)
    }
}

/** 05b · Tu posición. */
@Composable
private fun PositionCard(position: Holding) {
    Column(
        Modifier.fillMaxWidth().background(LensVestColors.Mint, RoundedCornerShape(24.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_briefcase), null, tint = LensVestColors.Forest, modifier = Modifier.size(16.dp))
            Text("TU POSICIÓN", style = MaterialTheme.typography.labelSmall, color = LensVestColors.Forest)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("${formatQuantity(position.quantity)} acciones", style = MaterialTheme.typography.titleLarge, color = LensVestColors.Navy)
                Text("Hoy valen ${formatUsd(position.valueUsd)}", style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                position.gainPct?.let { ChangePill(it) }
                position.gainUsd?.let {
                    Text(
                        (if (it >= 0) "+" else "−") + formatUsd(kotlin.math.abs(it)) + if (it >= 0) " ganados" else " perdidos",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (it >= 0) LensVestColors.Forest else LensVestColors.Terracotta,
                    )
                }
            }
        }
    }
}

/** "En criollo · otras marcas": what the company does, in plain words. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutCard(asset: AssetDetail) {
    Column(
        Modifier.fillMaxWidth().background(LensVestColors.Sand.copy(alpha = 0.7f), RoundedCornerShape(24.dp)).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(painterResource(R.drawable.ic_sparkles), null, tint = LensVestColors.Forest, modifier = Modifier.size(16.dp))
            Text("En criollo · otras marcas", style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp), color = LensVestColors.Forest)
        }
        if (asset.about.isNotBlank()) {
            Text(asset.about, style = MaterialTheme.typography.bodyMedium, color = LensVestColors.Navy)
        }
        if (asset.brands.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                asset.brands.forEach {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                        color = LensVestColors.Navy,
                        modifier = Modifier.background(LensVestColors.Paper, RoundedCornerShape(999.dp)).padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
    }
}

/** 05c · Pro: today's session, the month's range and the company. Only what Berry reports is shown. */
@Composable
private fun ProFiguresCard(asset: AssetDetail) {
    val pro = asset.pro
    val session = listOf(
        "Apertura" to pro.open?.let(::formatUsd),
        "Máximo / mínimo del día" to if (pro.dayHigh != null && pro.dayLow != null) "${formatUsd(pro.dayHigh)} / ${formatUsd(pro.dayLow)}" else null,
        "Cierre anterior" to pro.previousClose?.let(::formatUsd),
        "Rango del mes" to asset.history.takeIf { it.size >= 2 }?.let { "${formatUsd(it.min())} – ${formatUsd(it.max())}" },
        "Volumen" to pro.volume?.toLongOrNull()?.let { formatQuantity(it.toDouble()) },
    )
    val company = listOf(
        "CEO" to pro.ceo,
        "Fundada" to pro.founded,
        "Empleados" to pro.employees?.toLongOrNull()?.let { formatQuantity(it.toDouble()) },
        "Sede" to pro.headquarters,
    )
    listOf("HOY" to session, "LA EMPRESA" to company).forEach { (title, rows) ->
        val known = rows.filter { it.second != null }
        if (known.isNotEmpty()) {
            PaperCard {
                Text(title, style = MaterialTheme.typography.labelSmall, color = LensVestColors.Muted)
                known.forEach { (label, value) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(label, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted, modifier = Modifier.weight(1f))
                        Text(value.orEmpty(), style = MaterialTheme.typography.labelMedium, color = LensVestColors.Navy)
                    }
                }
            }
        }
    }
}

@Composable
private fun TradeButtons(asset: AssetDetail, onTrade: (TradeRequest) -> Unit) {
    val buy = TradeRequest(asset.ticker, asset.name, OrderSide.BUY, asset.priceUsd, asset.cashUsd)
    val position = asset.position
    if (position == null) {
        PrimaryButton("Comprar", onClick = { onTrade(buy) }, containerColor = LensVestColors.Forest)
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(
                "Vender",
                onClick = { onTrade(buy.copy(side = OrderSide.SELL, availableUsd = position.valueUsd)) },
                modifier = Modifier.weight(1f),
            )
            PrimaryButton("Comprar más", onClick = { onTrade(buy) }, containerColor = LensVestColors.Forest, modifier = Modifier.weight(1f))
        }
    }
}

private val previewAsset = AssetDetail(
    ticker = "KO", name = "The Coca-Cola Company", priceUsd = 95.6, changePct = 1.2,
    history = listOf(86.7, 87.0, 88.1, 90.7, 92.7, 93.9, 92.6, 94.6, 95.8, 95.6),
    about = "Es la empresa de bebidas sin alcohol más grande del mundo: vende en más de 200 países.",
    brands = listOf("Coca-Cola", "Sprite", "Fanta", "Powerade"),
    position = Holding(0.38, 36.9, 2.86, 8.4, 87.0), cashUsd = 48.2,
    pro = ProFigures(95.8, 96.1, 95.2, 95.8, "12345678", "James Quincey", "1892", "69700", "Atlanta, Georgia"),
)

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun AssetPreview() {
    LensVestTheme {
        AssetContent(
            AssetUiState(AssetContent("KO", "Sprite", previewAsset, canFavorite = true), SheetMode.SIMPLE, true),
            {}, {}, {}, {}, {},
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun AssetProPreview() {
    LensVestTheme {
        AssetContent(AssetUiState(AssetContent("KO", null, previewAsset.copy(position = null)), SheetMode.PRO, true), {}, {}, {}, {}, {})
    }
}
