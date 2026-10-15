package ar.edu.uade.lensvest.presentation.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.presentation.ui.components.LensVestTextField
import ar.edu.uade.lensvest.presentation.ui.components.OnboardingTopBar
import ar.edu.uade.lensvest.presentation.ui.components.PrimaryButton
import ar.edu.uade.lensvest.presentation.ui.components.ScreenColumn
import ar.edu.uade.lensvest.presentation.ui.components.SecondaryButton
import ar.edu.uade.lensvest.presentation.ui.components.rememberComingSoon
import ar.edu.uade.lensvest.presentation.viewmodel.AuthViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors

/** A2 · Crear cuenta (paso 1 de 3). */
@Composable
fun RegisterScreen(
    onRegistered: () -> Unit,
    onBack: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var acceptedTerms by rememberSaveable { mutableStateOf(false) }
    val comingSoon = rememberComingSoon()

    LaunchedEffect(uiState.isLoggedIn) {
        if (uiState.isLoggedIn) onRegistered()
    }

    ScreenColumn(
        actions = {
            uiState.error?.let { ErrorText(it) }
            PrimaryButton(
                "Crear cuenta",
                onClick = { viewModel.register(name, email, password) },
                enabled = acceptedTerms && name.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                loading = uiState.isLoading,
                showArrow = true,
            )
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Continuar con Google", onClick = { comingSoon("Registrarse con Google") })
        },
    ) {
        OnboardingTopBar(step = 1, total = 3, onBack = onBack)
        Spacer(Modifier.height(20.dp))
        Text("Creá tu cuenta", style = MaterialTheme.typography.headlineLarge, color = LensVestColors.Navy)
        Spacer(Modifier.height(8.dp))
        Text(
            "Tarda menos de un minuto. Después conectamos tu broker.",
            style = MaterialTheme.typography.bodyMedium,
            color = LensVestColors.Muted,
        )
        Spacer(Modifier.height(24.dp))
        LensVestTextField("Nombre", name, { name = it })
        Spacer(Modifier.height(14.dp))
        LensVestTextField("Email", email, { email = it }, keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(14.dp))
        LensVestTextField("Contraseña", password, { password = it }, isPassword = true)
        if (password.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            PasswordStrength(password)
        }
        Spacer(Modifier.height(20.dp))
        TermsCheckbox(acceptedTerms, onToggle = { acceptedTerms = !acceptedTerms })
    }
}

/** 0-3: long enough, has a digit, mixes case or symbols. */
internal fun passwordScore(password: String): Int = listOf(
    password.length >= 8,
    password.any(Char::isDigit),
    password.any(Char::isUpperCase) || password.any { !it.isLetterOrDigit() },
).count { it }

@Composable
private fun PasswordStrength(password: String) {
    val score = passwordScore(password)
    val (label, color) = when (score) {
        3 -> "Segura" to LensVestColors.Forest
        2 -> "Media" to LensVestColors.Sage
        else -> "Débil" to LensVestColors.Terracotta
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            Box(
                Modifier.weight(1f).height(4.dp)
                    .background(if (i < score) color else LensVestColors.Line, RoundedCornerShape(2.dp)),
            )
        }
        Text(label, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = color)
    }
}

@Composable
private fun TermsCheckbox(checked: Boolean, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(6.dp)
    Row(
        modifier = Modifier.clickable(role = Role.Checkbox, onClick = onToggle),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(22.dp)
                .background(if (checked) LensVestColors.Forest else LensVestColors.Paper, shape)
                .border(1.5.dp, if (checked) LensVestColors.Forest else LensVestColors.Line, shape),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(painterResource(R.drawable.ic_check), null, tint = LensVestColors.Cream, modifier = Modifier.size(14.dp))
        }
        Text(
            buildAnnotatedString {
                append("Acepto los ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = LensVestColors.Forest)) { append("términos y condiciones") }
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = LensVestColors.Navy,
        )
    }
}
