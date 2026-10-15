package ar.edu.uade.lensvest.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import ar.edu.uade.lensvest.ui.theme.fraunces

/** Logo: scan icon in a navy circle + "InvestLens". */
@Composable
fun BrandMark(modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).background(LensVestColors.Navy, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_scan), null, tint = LensVestColors.Cream, modifier = Modifier.size(18.dp))
        }
        Text("InvestLens", style = MaterialTheme.typography.titleMedium, color = LensVestColors.Navy)
    }
}

/** 44dp round paper button with an outline (back, bell…). */
@Composable
fun IconCircleButton(icon: Int, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(LensVestColors.Paper)
            .border(1.dp, LensVestColors.Line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription, tint = LensVestColors.Navy, modifier = Modifier.size(20.dp))
    }
}

/** Company or broker mark: a colored circle with its initial in Fraunces. */
@Composable
fun LetterLogo(letter: String, color: Color, size: Dp = 40.dp, modifier: Modifier = Modifier) {
    val fontSize = size.value * 0.45f
    Box(modifier.size(size).background(color, CircleShape), contentAlignment = Alignment.Center) {
        Text(
            letter.take(1).uppercase(),
            color = LensVestColors.Cream,
            style = TextStyle(fontFamily = fraunces(fontSize), fontWeight = FontWeight.SemiBold, fontSize = fontSize.sp),
        )
    }
}

/** "Paso n de total" with a segmented progress bar. */
@Composable
fun StepIndicator(step: Int, total: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { i ->
            Box(
                Modifier.width(24.dp).height(4.dp)
                    .background(if (i < step) LensVestColors.Navy else LensVestColors.Line, RoundedCornerShape(2.dp)),
            )
        }
        Text("Paso $step de $total", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium), color = LensVestColors.Muted)
    }
}

/** Small rounded label, e.g. "Pronto" or "OPERACIÓN DETECTADA". */
@Composable
fun Pill(
    text: String,
    modifier: Modifier = Modifier,
    container: Color = LensVestColors.Sand,
    content: Color = LensVestColors.Muted,
    fontSize: Int = 12,
) {
    Text(
        text,
        color = content,
        style = MaterialTheme.typography.labelMedium.copy(fontSize = fontSize.sp),
        modifier = modifier
            .background(container, RoundedCornerShape(999.dp))
            .padding(horizontal = if (fontSize <= 11) 8.dp else 10.dp, vertical = if (fontSize <= 11) 3.dp else 6.dp),
    )
}

/** Back button (when there is somewhere to go back to) + step indicator for onboarding steps. */
@Composable
fun OnboardingTopBar(step: Int, total: Int, onBack: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) IconCircleButton(R.drawable.ic_arrow_left, "Volver", onBack) else Box(Modifier.size(44.dp))
        Box(Modifier.weight(1f))
        StepIndicator(step, total)
    }
}
