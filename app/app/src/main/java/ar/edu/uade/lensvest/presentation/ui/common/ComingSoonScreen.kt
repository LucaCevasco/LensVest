package ar.edu.uade.lensvest.presentation.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.lensvest.ui.theme.LensVestColors

/** Placeholder for tabs whose design (F1 Estrategias, B3 Perfil) is built in the next phase. */
@Composable
fun ComingSoonScreen(title: String, description: String) {
    Column(
        Modifier.fillMaxSize().background(LensVestColors.Cream).statusBarsPadding().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, color = LensVestColors.Navy)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = LensVestColors.Muted)
    }
}
