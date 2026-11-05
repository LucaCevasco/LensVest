package ar.edu.uade.lensvest.presentation.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

private val esAr = Locale.forLanguageTag("es-AR")

/** "US$ 1.234,56": Berry operates in dollars. */
fun formatUsd(value: Double, decimals: Int = 2): String =
    "US$ " + NumberFormat.getNumberInstance(esAr).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }.format(value)

/** "+1,2 %" / "−0,4 %". */
fun formatPct(value: Double): String {
    val number = NumberFormat.getNumberInstance(esAr).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }.format(abs(value))
    return (if (value < 0) "−" else "+") + number + " %"
}

/** Up to 6 decimals without trailing zeros, e.g. fractional shares. */
fun formatQuantity(value: Double): String =
    NumberFormat.getNumberInstance(esAr).apply { maximumFractionDigits = 6 }.format(value)

/** Green pill for gains, terracotta for losses, with the matching arrow. */
@Composable
fun ChangePill(changePct: Double, modifier: Modifier = Modifier, suffix: String = "", onDark: Boolean = false) {
    val up = changePct >= 0
    val container = when {
        onDark -> LensVestColors.Sage
        up -> LensVestColors.Mint
        else -> Color(0xFFF3D9CF)
    }
    val content = when {
        onDark -> LensVestColors.Cream
        up -> LensVestColors.Forest
        else -> LensVestColors.Terracotta
    }
    Row(
        modifier.background(container, RoundedCornerShape(999.dp)).padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painterResource(if (up) R.drawable.ic_trending_up else R.drawable.ic_trending_down),
            null,
            tint = content,
            modifier = Modifier.size(12.dp),
        )
        Text(formatPct(changePct) + suffix, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp), color = content)
    }
}

/** A price line with a soft fill under it and a dot on the last point. */
@Composable
fun Sparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier, fill: Color = color.copy(alpha = 0.15f)) {
    if (values.size < 2) return
    Canvas(modifier) {
        val min = values.min()
        val span = (values.max() - min).takeIf { it > 0 } ?: 1.0
        val points = values.mapIndexed { i, v ->
            Offset(
                x = size.width * i / (values.size - 1),
                // 8% margin so the line and the dot don't touch the edges.
                y = (size.height * (0.92 - 0.84 * (v - min) / span)).toFloat(),
            )
        }
        val line = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(area, fill)
        drawPath(line, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawCircle(color, radius = 3.5.dp.toPx(), center = points.last())
    }
}

/** Two-option switch, e.g. "Simple | Pro" or "Historial | Favoritos". */
@Composable
fun SegmentedToggle(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(LensVestColors.Sand, RoundedCornerShape(16.dp)).padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Text(
                label,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = if (isSelected) LensVestColors.Navy else LensVestColors.Muted,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isSelected) LensVestColors.Paper else Color.Transparent)
                    .clickable { onSelect(index) }
                    .padding(vertical = 10.dp),
            )
        }
    }
}

/** Paper card with the design's outline. */
@Composable
fun PaperCard(
    modifier: Modifier = Modifier,
    color: Color = LensVestColors.Paper,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(color).border(1.dp, LensVestColors.Line, shape).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Spinner with a line about what's loading: Berry data takes a few seconds. */
@Composable
fun LoadingBlock(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CircularProgressIndicator(color = LensVestColors.Forest, trackColor = LensVestColors.Line, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted, textAlign = TextAlign.Center)
    }
}

/** Recoverable error with a retry (RNF03). */
@Composable
fun ErrorBlock(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    PaperCard(modifier) {
        Text("Algo salió mal", style = MaterialTheme.typography.titleMedium, color = LensVestColors.Navy)
        Text(message, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
        PrimaryButton("Reintentar", onClick = onRetry, compact = true)
    }
}

/** Section title with an optional action on the right, e.g. "Tus posiciones · Ver todo". */
@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: () -> Unit = {}) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = LensVestColors.Navy, modifier = Modifier.weight(1f))
        action?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                color = LensVestColors.Forest,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(4.dp),
            )
        }
    }
}

/** Initial-letter logos cycle through the palette so a list doesn't look all the same. */
fun logoColor(key: String): Color {
    val palette = listOf(LensVestColors.Terracotta, LensVestColors.Navy, LensVestColors.Forest, Color(0xFFC98E3A), LensVestColors.Sage)
    return palette[abs(key.hashCode()) % palette.size]
}

/** Row of an asset in a list: logo, name, subtitle, value and change. */
@Composable
fun AssetRow(
    title: String,
    subtitle: String,
    value: String?,
    changePct: Double?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    logoKey: String = title,
) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LetterLogo(title, logoColor(logoKey), size = 40.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = LensVestColors.Navy)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (value != null) {
                Text(value, style = MaterialTheme.typography.labelLarge, color = LensVestColors.Navy)
            } else {
                Box(Modifier.size(width = 64.dp, height = 16.dp).background(LensVestColors.Sand, RoundedCornerShape(4.dp)))
            }
            changePct?.let { ChangePill(it) }
        }
    }
}
