package ar.edu.uade.lensvest.presentation.ui.order

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.presentation.ui.components.formatUsd
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import ar.edu.uade.lensvest.ui.theme.LensVestTheme

/** 08 · Éxito: shown only after Berry confirmed the order (RNF06). */
@Composable
fun SuccessScreen(
    name: String,
    ticker: String,
    isSell: Boolean,
    amountUsd: Double,
    orderId: String?,
    onScanAnother: () -> Unit,
    onSeeInvestments: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().background(LensVestColors.Forest).safeDrawingPadding().padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(150.dp).background(LensVestColors.Cream.copy(alpha = 0.12f), CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(124.dp).background(LensVestColors.Cream.copy(alpha = 0.18f), CircleShape), contentAlignment = Alignment.Center) {
                Box(Modifier.size(96.dp).background(LensVestColors.Cream, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(painterResource(R.drawable.ic_check), null, tint = LensVestColors.Forest, modifier = Modifier.size(40.dp))
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Text(
            buildAnnotatedString {
                if (isSell) {
                    append("¡Listo!\nVendiste parte de\n")
                } else {
                    append("¡Listo!\nYa sos parte de\n")
                }
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append("$name.") }
            },
            style = MaterialTheme.typography.headlineLarge,
            color = LensVestColors.Cream,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "${if (isSell) "Vendiste" else "Compraste"} ${formatUsd(amountUsd)} de $ticker a través de Berry. Lo vas a ver en tu cuenta en unos minutos.",
            style = MaterialTheme.typography.bodyMedium,
            color = LensVestColors.Cream.copy(alpha = 0.85f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.background(LensVestColors.Cream.copy(alpha = 0.14f), RoundedCornerShape(999.dp)).padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_check), null, tint = LensVestColors.Cream, modifier = Modifier.size(14.dp))
            Text(
                (orderId?.let { "Orden #${it.take(8)} · " } ?: "Orden ") + "Enviada a Berry",
                style = MaterialTheme.typography.labelMedium,
                color = LensVestColors.Cream,
            )
        }
        Spacer(Modifier.weight(1.4f))
        Row(
            Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(18.dp)).background(LensVestColors.Cream).clickable(onClick = onScanAnother),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(R.drawable.ic_scan), null, tint = LensVestColors.Navy, modifier = Modifier.size(20.dp))
            Text("Escanear otro producto", style = MaterialTheme.typography.labelLarge, color = LensVestColors.Navy)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Ver mis inversiones",
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 16.sp),
            color = LensVestColors.Cream,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onSeeInvestments).padding(16.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 716)
@Composable
private fun SuccessPreview() {
    LensVestTheme { SuccessScreen("Coca-Cola", "KO", false, 5.0, "BR-20931", {}, {}) }
}
