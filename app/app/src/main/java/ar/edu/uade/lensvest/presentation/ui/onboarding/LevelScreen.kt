package ar.edu.uade.lensvest.presentation.ui.onboarding

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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.domain.model.InvestorLevel
import ar.edu.uade.lensvest.presentation.ui.components.OnboardingTopBar
import ar.edu.uade.lensvest.presentation.ui.components.Pill
import ar.edu.uade.lensvest.presentation.ui.components.PrimaryButton
import ar.edu.uade.lensvest.presentation.ui.components.ScreenColumn
import ar.edu.uade.lensvest.presentation.viewmodel.LevelViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors

private data class LevelOption(
    val level: InvestorLevel,
    val icon: Int,
    val title: String,
    val description: String,
    val view: String,
)

private val options = listOf(
    LevelOption(InvestorLevel.BEGINNER, R.drawable.ic_sparkles, "Recién arranco", "Te explicamos todo en criollo, paso a paso.", "Vista Simple"),
    LevelOption(InvestorLevel.INTERMEDIATE, R.drawable.ic_trending_up, "Sé un poco", "Conozco lo básico: acciones, CEDEARs, comisiones.", "Vista Simple + datos"),
    LevelOption(InvestorLevel.ADVANCED, R.drawable.ic_bar_chart, "Ya invierto", "Quiero datos avanzados: P/E, dividendos, gráficos.", "Vista Pro"),
)

/** A3 · Tu nivel (paso 2 de 3). */
@Composable
fun LevelScreen(
    onContinue: () -> Unit,
    viewModel: LevelViewModel = hiltViewModel(),
) {
    val selected by viewModel.selected.collectAsStateWithLifecycle()
    ScreenColumn(
        actions = {
            PrimaryButton("Continuar", onClick = { viewModel.confirm(); onContinue() }, showArrow = true)
        },
    ) {
        // The account already exists at this point, so there's no going back to the form.
        OnboardingTopBar(step = 2, total = 3, onBack = null)
        Spacer(Modifier.height(20.dp))
        Text("¿Cuánto sabés\nde invertir?", style = MaterialTheme.typography.headlineLarge, color = LensVestColors.Navy)
        Spacer(Modifier.height(8.dp))
        Text(
            "Así adaptamos la app a vos. Lo podés cambiar cuando quieras desde tu perfil.",
            style = MaterialTheme.typography.bodyMedium,
            color = LensVestColors.Muted,
        )
        Spacer(Modifier.height(24.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { option ->
                LevelCard(option, isSelected = option.level == selected, onSelect = { viewModel.select(option.level) })
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun LevelCard(option: LevelOption, isSelected: Boolean, onSelect: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = LensVestColors.Paper,
        border = if (isSelected) BorderStroke(2.dp, LensVestColors.Forest) else BorderStroke(1.dp, LensVestColors.Line),
        shadowElevation = if (isSelected) 6.dp else 0.dp,
        modifier = Modifier.fillMaxWidth().selectable(selected = isSelected, role = Role.RadioButton, onClick = onSelect),
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                Modifier.size(44.dp).background(if (isSelected) LensVestColors.Forest else LensVestColors.Sand, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(option.icon),
                    null,
                    tint = if (isSelected) LensVestColors.Cream else LensVestColors.Navy,
                    modifier = Modifier.size(22.dp),
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(option.title, style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp), color = LensVestColors.Navy)
                Text(option.description, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
                Pill(
                    option.view,
                    container = if (isSelected) LensVestColors.Mint else LensVestColors.Sand,
                    content = if (isSelected) LensVestColors.Forest else LensVestColors.Muted,
                    fontSize = 11,
                )
            }
        }
    }
}
