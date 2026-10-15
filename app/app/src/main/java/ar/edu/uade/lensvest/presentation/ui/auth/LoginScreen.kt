package ar.edu.uade.lensvest.presentation.ui.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.presentation.ui.components.BrandMark
import ar.edu.uade.lensvest.presentation.ui.components.LensVestTextField
import ar.edu.uade.lensvest.presentation.ui.components.PrimaryButton
import ar.edu.uade.lensvest.presentation.ui.components.ScreenColumn
import ar.edu.uade.lensvest.presentation.ui.components.SecondaryButton
import ar.edu.uade.lensvest.presentation.ui.components.rememberComingSoon
import ar.edu.uade.lensvest.presentation.viewmodel.AuthViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors

/** A1 · Iniciar sesión. */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    onCreateAccount: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val comingSoon = rememberComingSoon()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onLoggedIn()
    }

    ScreenColumn(
        actions = {
            uiState.error?.let { ErrorText(it) }
            PrimaryButton(
                "Entrar",
                onClick = { viewModel.login(email, password) },
                enabled = email.isNotBlank() && password.isNotBlank(),
                loading = uiState.isLoading,
                showArrow = true,
            )
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(Modifier.weight(1f), color = LensVestColors.Line)
                Text("o", style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
                HorizontalDivider(Modifier.weight(1f), color = LensVestColors.Line)
            }
            Spacer(Modifier.height(14.dp))
            SecondaryButton("Continuar con Google", onClick = { comingSoon("Entrar con Google") })
            Spacer(Modifier.height(16.dp))
            Text(
                buildAnnotatedString {
                    append("¿No tenés cuenta? ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = LensVestColors.Forest)) { append("Creá una") }
                },
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = LensVestColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onCreateAccount),
            )
        },
    ) {
        Spacer(Modifier.height(8.dp))
        BrandMark()
        Spacer(Modifier.height(40.dp))
        Text(
            buildAnnotatedString {
                append("Hola de\n")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = LensVestColors.Forest)) { append("nuevo.") }
            },
            style = MaterialTheme.typography.displayLarge,
            color = LensVestColors.Navy,
        )
        Spacer(Modifier.height(10.dp))
        Text("Entrá para ver cómo van tus inversiones.", style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 15.sp), color = LensVestColors.Muted)
        Spacer(Modifier.height(28.dp))
        LensVestTextField("Email", email, { email = it }, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(14.dp))
        LensVestTextField("Contraseña", password, { password = it }, isPassword = true)
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Text(
                "¿Te olvidaste la contraseña?",
                style = MaterialTheme.typography.labelMedium,
                color = LensVestColors.Forest,
                modifier = Modifier.clickable { comingSoon("Recuperar contraseña") },
            )
        }
    }
}

@Composable
internal fun ErrorText(message: String) {
    Text(
        message,
        style = MaterialTheme.typography.bodySmall,
        color = LensVestColors.Terracotta,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    )
}
