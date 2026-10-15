package ar.edu.uade.lensvest.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.ui.theme.LensVestColors

enum class MainTab(val label: String, val icon: Int) {
    HOME("Inicio", R.drawable.ic_home),
    STRATEGIES("Estrategias", R.drawable.ic_repeat),
    ASSISTANT("Asistente", R.drawable.ic_sparkles),
    PROFILE("Perfil", R.drawable.ic_user),
}

/** Bottom navigation with the raised Lens (scan) button in the middle. */
@Composable
fun LensVestBottomBar(selected: MainTab?, onSelect: (MainTab) -> Unit, onLens: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(LensVestColors.Paper).navigationBarsPadding()) {
        HorizontalDivider(color = LensVestColors.Line)
        Row(
            modifier = Modifier.fillMaxWidth().height(84.dp).padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(MainTab.HOME, selected, onSelect)
            TabItem(MainTab.STRATEGIES, selected, onSelect)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .shadow(18.dp, CircleShape, ambientColor = LensVestColors.Navy, spotColor = LensVestColors.Navy)
                    .clip(CircleShape)
                    .background(LensVestColors.Navy)
                    .clickable(role = Role.Button, onClickLabel = "Abrir Lens Mode", onClick = onLens),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_scan), "Lens Mode", tint = LensVestColors.Cream, modifier = Modifier.size(26.dp))
            }
            TabItem(MainTab.ASSISTANT, selected, onSelect)
            TabItem(MainTab.PROFILE, selected, onSelect)
        }
    }
}

@Composable
private fun TabItem(tab: MainTab, selected: MainTab?, onSelect: (MainTab) -> Unit) {
    val isSelected = tab == selected
    val color = if (isSelected) LensVestColors.Navy else LensVestColors.Muted
    Column(
        modifier = Modifier
            .width(58.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Tab,
            ) { onSelect(tab) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(painterResource(tab.icon), null, tint = color, modifier = Modifier.size(22.dp))
        Text(
            tab.label,
            color = color,
            maxLines = 1,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        )
    }
}
