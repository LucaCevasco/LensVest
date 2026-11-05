package ar.edu.uade.lensvest.presentation.ui.profile

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import ar.edu.uade.lensvest.domain.model.InvestorLevel
import ar.edu.uade.lensvest.domain.model.SheetMode
import ar.edu.uade.lensvest.presentation.ui.components.LetterLogo
import ar.edu.uade.lensvest.presentation.ui.components.Pill
import ar.edu.uade.lensvest.presentation.ui.components.rememberComingSoon
import ar.edu.uade.lensvest.presentation.viewmodel.ProfileAccount
import ar.edu.uade.lensvest.presentation.viewmodel.ProfileUiState
import ar.edu.uade.lensvest.presentation.viewmodel.ProfileViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import ar.edu.uade.lensvest.ui.theme.LensVestTheme

fun InvestorLevel.label() = when (this) {
    InvestorLevel.BEGINNER -> "Recién arranco"
    InvestorLevel.INTERMEDIATE -> "Sé un poco"
    InvestorLevel.ADVANCED -> "Ya invierto"
}

/** B3 · Perfil: personalization, the connected broker and logout. */
@Composable
fun ProfileScreen(
    onChangeLevel: () -> Unit,
    onManageBroker: () -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    val comingSoon = rememberComingSoon()
    ProfileContent(
        uiState = uiState,
        onMode = viewModel::setMode,
        onPlainExplanations = viewModel::setPlainExplanations,
        onChangeLevel = onChangeLevel,
        onManageBroker = onManageBroker,
        onComingSoon = comingSoon,
        onLogout = {
            viewModel.logout()
            onLoggedOut()
        },
    )
}

@Composable
private fun ProfileContent(
    uiState: ProfileUiState,
    onMode: (SheetMode) -> Unit,
    onPlainExplanations: (Boolean) -> Unit,
    onChangeLevel: () -> Unit,
    onManageBroker: () -> Unit,
    onComingSoon: (String) -> Unit,
    onLogout: () -> Unit,
) {
    val account = uiState.account
    Column(
        Modifier.fillMaxSize().background(LensVestColors.Cream).statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
    ) {
        Text("Perfil", style = MaterialTheme.typography.headlineLarge, color = LensVestColors.Navy, modifier = Modifier.padding(vertical = 12.dp))
        Group {
            Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                LetterLogo(account.name.ifEmpty { "?" }, LensVestColors.Navy, size = 64.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(account.name, style = MaterialTheme.typography.labelLarge.copy(fontSize = 18.sp), color = LensVestColors.Navy)
                    if (account.email.isNotEmpty()) {
                        Text(account.email, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
                    }
                    Pill("Nivel: ${account.level.label()}", container = LensVestColors.Mint, content = LensVestColors.Forest, fontSize = 11)
                }
            }
        }
        SectionLabel("PERSONALIZACIÓN")
        Group {
            SettingRow(R.drawable.ic_layers, "Vista") {
                ModeToggle(uiState.mode, onMode)
            }
            Divider()
            SettingRow(R.drawable.ic_trending_up, "Tu nivel", onClick = onChangeLevel) {
                Text(account.level.label(), style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = LensVestColors.Muted)
                Chevron()
            }
            Divider()
            SettingRow(R.drawable.ic_message, "Explicaciones en criollo") {
                Switch(
                    checked = uiState.plainExplanations,
                    onCheckedChange = onPlainExplanations,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = LensVestColors.Forest,
                        checkedThumbColor = LensVestColors.Cream,
                        uncheckedTrackColor = LensVestColors.Sand,
                        uncheckedBorderColor = LensVestColors.Line,
                    ),
                )
            }
        }
        SectionLabel("CUENTA")
        Group {
            SettingRow(R.drawable.ic_link, "Brokers conectados", onClick = onManageBroker) {
                BrokerStatus(account.isBerryConnected)
            }
            Divider()
            SettingRow(R.drawable.ic_shield_check, "Seguridad y huella", onClick = { onComingSoon("Seguridad y huella") }) { Chevron() }
            Divider()
            SettingRow(R.drawable.ic_help, "Ayuda", onClick = { onComingSoon("Ayuda") }) { Chevron() }
            Divider()
            SettingRow(R.drawable.ic_logout, "Cerrar sesión", onClick = onLogout, danger = true) {}
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BrokerStatus(connected: Boolean?) {
    when (connected) {
        true -> Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).background(LensVestColors.Sage, CircleShape))
            Text("Berry", style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp), color = LensVestColors.Navy)
        }
        false -> Text("Conectar Berry", style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp), color = LensVestColors.Forest)
        null -> Text("…", style = MaterialTheme.typography.labelMedium, color = LensVestColors.Muted)
    }
}

@Composable
private fun ModeToggle(mode: SheetMode, onMode: (SheetMode) -> Unit) {
    Row(Modifier.background(LensVestColors.Sand, RoundedCornerShape(12.dp)).padding(3.dp)) {
        listOf(SheetMode.SIMPLE to "Simple", SheetMode.PRO to "Pro").forEach { (value, label) ->
            val selected = value == mode
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                color = if (selected) LensVestColors.Navy else LensVestColors.Muted,
                modifier = Modifier.clip(RoundedCornerShape(9.dp)).background(if (selected) LensVestColors.Paper else Color.Transparent)
                    .clickable { onMode(value) }.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun Group(content: @Composable () -> Unit) {
    val shape = RoundedCornerShape(24.dp)
    Column(Modifier.fillMaxWidth().clip(shape).background(LensVestColors.Paper).border(1.dp, LensVestColors.Line, shape)) { content() }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = LensVestColors.Muted, modifier = Modifier.padding(top = 22.dp, bottom = 8.dp))
}

@Composable
private fun Divider() = HorizontalDivider(color = LensVestColors.Line, modifier = Modifier.padding(horizontal = 16.dp))

@Composable
private fun Chevron() = Icon(painterResource(R.drawable.ic_chevron_right), null, tint = LensVestColors.Muted, modifier = Modifier.size(18.dp))

@Composable
private fun SettingRow(
    icon: Int,
    title: String,
    onClick: (() -> Unit)? = null,
    danger: Boolean = false,
    trailing: @Composable () -> Unit,
) {
    val tint = if (danger) LensVestColors.Terracotta else LensVestColors.Navy
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).background(if (danger) Color(0xFFF3D9CF) else LensVestColors.Sand, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, color = tint, modifier = Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) { trailing() }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun ProfilePreview() {
    LensVestTheme {
        ProfileContent(
            ProfileUiState(ProfileAccount("Martina López", "martina@mail.com", InvestorLevel.BEGINNER, true), SheetMode.SIMPLE, true),
            {}, {}, {}, {}, {}, {},
        )
    }
}
