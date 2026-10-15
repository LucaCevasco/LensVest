package ar.edu.uade.lensvest.presentation.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.ui.theme.LensVestColors

private val ButtonShape = RoundedCornerShape(18.dp)
private val CompactShape = RoundedCornerShape(16.dp)

/** 16sp for full-size buttons, 15sp for the compact 44dp ones inside cards. */
@Composable
private fun buttonText(compact: Boolean) =
    MaterialTheme.typography.labelLarge.let { if (compact) it.copy(fontSize = 15.sp) else it }

/** Filled navy call to action (56dp), optionally with a trailing arrow. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    showArrow: Boolean = false,
    containerColor: Color = LensVestColors.Navy,
    compact: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        shape = if (compact) CompactShape else ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = LensVestColors.Cream,
            disabledContainerColor = containerColor.copy(alpha = 0.45f),
            disabledContentColor = LensVestColors.Cream,
        ),
        modifier = modifier.fillMaxWidth().height(if (compact) 44.dp else 56.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(color = LensVestColors.Cream, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text, style = buttonText(compact))
                if (showArrow) Icon(painterResource(R.drawable.ic_arrow_right), null, Modifier.size(20.dp))
            }
        }
    }
}

/** Paper button with a 1.5dp outline, e.g. "Continuar con Google". */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = if (compact) CompactShape else ButtonShape,
        border = BorderStroke(1.5.dp, LensVestColors.Line),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = LensVestColors.Paper,
            contentColor = LensVestColors.Navy,
        ),
        modifier = modifier.fillMaxWidth().height(if (compact) 44.dp else 56.dp),
    ) {
        Text(text, style = buttonText(compact))
    }
}

/** Text-only button, e.g. "Ya tengo cuenta". */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        onClick = onClick,
        shape = ButtonShape,
        colors = ButtonDefaults.textButtonColors(contentColor = LensVestColors.Navy),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
