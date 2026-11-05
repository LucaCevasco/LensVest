package ar.edu.uade.lensvest.presentation.ui.history

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.domain.model.AssetSearchResult
import ar.edu.uade.lensvest.presentation.ui.components.AssetRow
import ar.edu.uade.lensvest.presentation.ui.components.ErrorBlock
import ar.edu.uade.lensvest.presentation.ui.components.IconCircleButton
import ar.edu.uade.lensvest.presentation.ui.components.LoadingBlock
import ar.edu.uade.lensvest.presentation.ui.components.PaperCard
import ar.edu.uade.lensvest.presentation.ui.components.ScreenColumn
import ar.edu.uade.lensvest.presentation.viewmodel.SearchViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors

/** D3 · Buscar a mano. */
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onOpenAsset: (AssetSearchResult) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    ScreenColumn {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconCircleButton(R.drawable.ic_arrow_left, "Volver", onBack)
        }
        Spacer(Modifier.height(8.dp))
        Text("Buscar a mano", style = MaterialTheme.typography.headlineLarge, color = LensVestColors.Navy)
        Spacer(Modifier.height(6.dp))
        Text(
            "Escribí la marca o la empresa, por ejemplo \"Oreo\" o \"Mondelez\".",
            style = MaterialTheme.typography.bodyMedium,
            color = LensVestColors.Muted,
        )
        Spacer(Modifier.height(16.dp))
        val shape = RoundedCornerShape(16.dp)
        Row(
            Modifier.fillMaxWidth().background(LensVestColors.Paper, shape).border(1.dp, LensVestColors.Line, shape).padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_search), null, tint = LensVestColors.Muted, modifier = Modifier.size(20.dp))
            Box(Modifier.weight(1f)) {
                if (uiState.query.isEmpty()) {
                    Text("Marca o empresa", style = MaterialTheme.typography.bodyMedium, color = LensVestColors.Muted)
                }
                BasicTextField(
                    value = uiState.query,
                    onValueChange = viewModel::onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = LensVestColors.Navy),
                    cursorBrush = SolidColor(LensVestColors.Forest),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.submit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        when {
            uiState.isLoading -> LoadingBlock("Buscando \"${uiState.searched}\" en Berry…")
            uiState.error != null -> ErrorBlock(uiState.error!!, onRetry = viewModel::submit)
            uiState.searched != null && uiState.results.isEmpty() -> Text(
                "No encontramos nada para \"${uiState.searched}\". Probá con el nombre de la empresa.",
                style = MaterialTheme.typography.bodyMedium,
                color = LensVestColors.Muted,
            )
            uiState.results.isNotEmpty() -> PaperCard {
                uiState.results.forEachIndexed { index, result ->
                    if (index > 0) HorizontalDivider(color = LensVestColors.Line)
                    AssetRow(
                        title = result.name,
                        subtitle = result.ticker,
                        value = "",
                        changePct = null,
                        onClick = { onOpenAsset(result) },
                        logoKey = result.ticker,
                    )
                }
            }
        }
    }
}
