package ar.edu.uade.lensvest.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ar.edu.uade.lensvest.ui.theme.LensVestColors

/** The design's generic green can ("Producto (lata genérica)"), drawn with shapes. */
@Composable
fun ProductCan(width: Dp, modifier: Modifier = Modifier) {
    val scale = width.value / 84f
    val height = (154 * scale).dp
    Box(modifier.size(width, height)) {
        Box(
            Modifier
                .size(width, height)
                .shadow(12.dp, RoundedCornerShape((16.8f * scale).dp), ambientColor = LensVestColors.Navy, spotColor = LensVestColors.Navy)
                .background(
                    Brush.horizontalGradient(
                        0f to LensVestColors.Forest,
                        0.4f to Color(0xFF7DB58A),
                        1f to Color(0xFF24492F),
                    ),
                    RoundedCornerShape((16.8f * scale).dp),
                ),
        )
        Box(
            Modifier.offset(y = (53.9f * scale).dp).fillMaxWidth().height((46.2f * scale).dp)
                .alpha(0.9f).background(LensVestColors.Cream),
        )
        Box(
            Modifier.offset(x = (21 * scale).dp, y = (73.15f * scale).dp)
                .size((42 * scale).dp, (7.7f * scale).dp)
                .background(LensVestColors.Forest, RoundedCornerShape(4.dp)),
        )
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-5).dp)
                .width((63.84f * scale).dp).height(8.dp)
                .background(Color(0xFFCFC8B8), RoundedCornerShape(4.dp)),
        )
    }
}
