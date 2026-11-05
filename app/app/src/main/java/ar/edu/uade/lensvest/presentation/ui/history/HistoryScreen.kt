package ar.edu.uade.lensvest.presentation.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.domain.model.Scan
import ar.edu.uade.lensvest.presentation.ui.components.AssetRow
import ar.edu.uade.lensvest.presentation.ui.components.ErrorBlock
import ar.edu.uade.lensvest.presentation.ui.components.IconCircleButton
import ar.edu.uade.lensvest.presentation.ui.components.LoadingBlock
import ar.edu.uade.lensvest.presentation.ui.components.PaperCard
import ar.edu.uade.lensvest.presentation.ui.components.SegmentedToggle
import ar.edu.uade.lensvest.presentation.ui.components.formatUsd
import ar.edu.uade.lensvest.presentation.viewmodel.HistoryUiState
import ar.edu.uade.lensvest.presentation.viewmodel.HistoryViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/** 09 · Tus escaneos: the brands the user confirmed, newest first, and their favorites. */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenScan: (Scan) -> Unit,
    onSearch: () -> Unit,
    onScan: () -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    Box(Modifier.fillMaxSize().background(LensVestColors.Cream)) {
        HistoryContent(uiState, onBack, onOpenScan, onSearch, viewModel::showFavorites, viewModel::toggleFavorite, viewModel::load)
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 24.dp)
                .clip(RoundedCornerShape(999.dp)).background(LensVestColors.Navy).clickable(onClick = onScan)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_scan), null, tint = LensVestColors.Cream, modifier = Modifier.size(20.dp))
            Text("Escanear", style = MaterialTheme.typography.labelLarge, color = LensVestColors.Cream)
        }
    }
}

@Composable
private fun HistoryContent(
    uiState: HistoryUiState,
    onBack: () -> Unit,
    onOpenScan: (Scan) -> Unit,
    onSearch: () -> Unit,
    onShowFavorites: (Boolean) -> Unit,
    onToggleFavorite: (Scan) -> Unit,
    onRetry: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(R.drawable.ic_arrow_left, "Volver", onBack)
            Spacer(Modifier.weight(1f))
            IconCircleButton(R.drawable.ic_search, "Buscar a mano", onSearch)
        }
        Spacer(Modifier.height(8.dp))
        Text("Tus escaneos", style = MaterialTheme.typography.headlineLarge, color = LensVestColors.Navy)
        Spacer(Modifier.height(16.dp))
        SegmentedToggle(
            options = listOf("Historial", "Favoritos · ${uiState.favoriteCount}"),
            selected = if (uiState.showFavorites) 1 else 0,
            onSelect = { onShowFavorites(it == 1) },
        )
        Spacer(Modifier.height(18.dp))
        when {
            uiState.error != null && uiState.scans.isEmpty() -> ErrorBlock(uiState.error, onRetry)
            uiState.isLoading && uiState.scans.isEmpty() -> LoadingBlock("Cargando tus escaneos…")
            uiState.visible.isEmpty() -> Text(
                if (uiState.showFavorites) "Marcá una empresa con la estrella en su ficha y aparece acá."
                else "Todavía no escaneaste nada. Apuntale a un producto con Lens Mode.",
                style = MaterialTheme.typography.bodyMedium,
                color = LensVestColors.Muted,
            )
            else -> uiState.visible.groupBy { it.createdAt.dayLabel() }.forEach { (day, scans) ->
                Text(day, style = MaterialTheme.typography.labelSmall, color = LensVestColors.Muted)
                Spacer(Modifier.height(8.dp))
                PaperCard {
                    scans.forEachIndexed { index, scan ->
                        if (index > 0) HorizontalDivider(color = LensVestColors.Line)
                        val quote = scan.ticker?.let { uiState.quotes[it] }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            AssetRow(
                                title = scan.brand,
                                subtitle = listOfNotNull(scan.company, scan.ticker).joinToString(" · ").ifEmpty { "No cotiza" },
                                value = quote?.let { formatUsd(it.priceUsd) } ?: if (scan.ticker == null) "" else null,
                                changePct = quote?.changePct,
                                onClick = { onOpenScan(scan) },
                                modifier = Modifier.weight(1f),
                                logoKey = scan.ticker ?: scan.brand,
                            )
                            Icon(
                                painterResource(if (scan.favorite) R.drawable.ic_star_filled else R.drawable.ic_star),
                                if (scan.favorite) "Quitar de favoritos" else "Agregar a favoritos",
                                tint = if (scan.favorite) LensVestColors.Forest else LensVestColors.Line,
                                modifier = Modifier.padding(start = 8.dp).size(36.dp).clip(RoundedCornerShape(10.dp))
                                    .clickable { onToggleFavorite(scan) }.padding(8.dp),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
        Spacer(Modifier.height(96.dp))
    }
}

/** "HOY", "AYER" or "12 SEP" for an ISO timestamp from the backend (UTC). */
private fun String.dayLabel(): String {
    val parsed = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(take(19))
    }.getOrNull() ?: return take(10)
    val day = Calendar.getInstance().apply { time = parsed }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    fun Calendar.sameDay(other: Calendar) = get(Calendar.YEAR) == other.get(Calendar.YEAR) && get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    return when {
        day.sameDay(today) -> "HOY"
        day.sameDay(yesterday) -> "AYER"
        else -> SimpleDateFormat("d MMM", Locale.forLanguageTag("es-AR")).format(parsed).uppercase()
    }
}
