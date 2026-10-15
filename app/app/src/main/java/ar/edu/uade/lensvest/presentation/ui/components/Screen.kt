package ar.edu.uade.lensvest.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import kotlinx.coroutines.launch

/**
 * Cream screen with the design's 24dp margins: the content scrolls and the
 * actions stay pinned at the bottom, above the keyboard.
 */
@Composable
fun ScreenColumn(
    actions: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(LensVestColors.Cream)
            // safeDrawing already includes the keyboard, so the actions sit right above it.
            .safeDrawingPadding()
            .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
    ) {
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), content = content)
        actions()
    }
}

val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> { error("No SnackbarHostState provided") }

/** Feedback for designed actions that aren't built yet, so a tap never does nothing silently. */
@Composable
fun rememberComingSoon(): (String) -> Unit {
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    return { what -> scope.launch { snackbar.showSnackbar("$what: próximamente") } }
}
