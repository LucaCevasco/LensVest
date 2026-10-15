package ar.edu.uade.lensvest.presentation.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.ui.theme.LensVestColors

/** Label above a 52dp paper box, as in the design's "Campo/…" components. */
@Composable
fun LensVestTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = LensVestColors.Navy)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = LensVestColors.Navy, lineHeight = MaterialTheme.typography.bodyMedium.fontSize),
            cursorBrush = SolidColor(LensVestColors.Forest),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (isPassword) KeyboardType.Password else keyboardType,
                // Emails and passwords must arrive exactly as typed.
                capitalization = if (keyboardType == KeyboardType.Text && !isPassword) KeyboardCapitalization.Words else KeyboardCapitalization.None,
                autoCorrectEnabled = keyboardType == KeyboardType.Text && !isPassword,
            ),
            visualTransformation = if (isPassword && !visible) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { inner ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .background(LensVestColors.Paper, shape)
                        .border(1.dp, LensVestColors.Line, shape)
                        .padding(start = 16.dp, end = if (isPassword) 4.dp else 16.dp),
                ) {
                    Box(Modifier.weight(1f)) { inner() }
                    if (isPassword) {
                        IconButton(onClick = { visible = !visible }) {
                            Icon(
                                painterResource(R.drawable.ic_eye),
                                contentDescription = if (visible) "Ocultar contraseña" else "Mostrar contraseña",
                                tint = LensVestColors.Muted,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            },
        )
    }
}
