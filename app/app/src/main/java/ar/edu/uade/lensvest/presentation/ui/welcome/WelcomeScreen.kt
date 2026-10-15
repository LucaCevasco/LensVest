package ar.edu.uade.lensvest.presentation.ui.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.presentation.ui.components.BrandMark
import ar.edu.uade.lensvest.presentation.ui.components.GhostButton
import ar.edu.uade.lensvest.presentation.ui.components.PrimaryButton
import ar.edu.uade.lensvest.presentation.ui.components.ProductCan
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import ar.edu.uade.lensvest.ui.theme.LensVestTheme

@Composable
fun WelcomeScreen(onStart: () -> Unit, onLogin: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LensVestColors.Cream)
            .safeDrawingPadding()
            .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 28.dp),
    ) {
        BrandMark()
        Spacer(Modifier.height(20.dp))
        WelcomeIllustration()
        Spacer(Modifier.height(32.dp))
        Text(
            buildAnnotatedString {
                append("Invertí en lo\nque ")
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = LensVestColors.Forest)) { append("ves.") }
            },
            style = MaterialTheme.typography.displayLarge,
            color = LensVestColors.Navy,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Apuntá la cámara a cualquier producto y descubrí qué empresa está detrás, cuánto vale y cómo invertir. En segundos.",
            style = MaterialTheme.typography.bodyLarge,
            color = LensVestColors.Muted,
        )
        Spacer(Modifier.weight(1f))
        PrimaryButton("Empezar", onClick = onStart, showArrow = true)
        Spacer(Modifier.height(4.dp))
        GhostButton("Ya tengo cuenta", onClick = onLogin)
    }
}

/** Can inside a scan frame, with the brand → company and ticker chips. */
@Composable
private fun WelcomeIllustration() {
    Box(
        Modifier.fillMaxWidth().height(300.dp).background(LensVestColors.Sand, RoundedCornerShape(28.dp)),
        contentAlignment = Alignment.Center,
    ) {
        // Fixed 312x300 composition from the design, centered in the available width.
        Box(Modifier.size(312.dp, 300.dp)) {
            Image(painterResource(R.drawable.welcome_ellipse), null, Modifier.offset(36.dp, 30.dp).size(240.dp))
            Image(painterResource(R.drawable.welcome_scan_frame), null, Modifier.offset(86.dp, 48.dp).size(140.dp, 210.dp))
            ProductCan(84.dp, Modifier.offset(114.dp, 76.dp))
            Chip(Modifier.offset(18.dp, 238.dp)) {
                Text("Sprite", style = MaterialTheme.typography.labelMedium, color = LensVestColors.Navy)
                Icon(painterResource(R.drawable.ic_arrow_right), null, tint = LensVestColors.Navy, modifier = Modifier.size(14.dp))
                Text("Coca-Cola", style = MaterialTheme.typography.labelMedium, color = LensVestColors.Forest)
            }
            Chip(Modifier.offset(186.dp, 28.dp)) {
                Icon(painterResource(R.drawable.ic_trending_up), null, tint = LensVestColors.Forest, modifier = Modifier.size(14.dp))
                Text("KO  +1,2%", style = MaterialTheme.typography.labelMedium, color = LensVestColors.Forest)
            }
        }
    }
}

@Composable
private fun Chip(modifier: Modifier, content: @Composable () -> Unit) {
    Row(
        modifier = modifier
            .shadow(6.dp, RoundedCornerShape(999.dp), ambientColor = LensVestColors.Navy, spotColor = LensVestColors.Navy)
            .background(Color.White, RoundedCornerShape(999.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) { content() }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun WelcomePreview() {
    LensVestTheme { WelcomeScreen(onStart = {}, onLogin = {}) }
}
