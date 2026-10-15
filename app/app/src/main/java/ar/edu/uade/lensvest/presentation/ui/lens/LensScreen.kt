package ar.edu.uade.lensvest.presentation.ui.lens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.presentation.ui.components.LocalSnackbarHostState
import ar.edu.uade.lensvest.presentation.ui.components.PrimaryButton
import ar.edu.uade.lensvest.presentation.ui.components.rememberComingSoon
import ar.edu.uade.lensvest.presentation.viewmodel.LensViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private val Glass = Color.White.copy(alpha = 0.14f)
private val CreamText = LensVestColors.Cream

/** 03 · Lens Mode: live camera with the scan frame and controls on top. */
@Composable
fun LensScreen(viewModel: LensViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val comingSoon = rememberComingSoon()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var askedBefore by remember { mutableStateOf(false) }
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
        askedBefore = true
    }
    val activity = context as? Activity
    val allowCamera = {
        // After repeated denials Android stops showing the dialog: send the user to the app's settings.
        if (askedBefore && activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == false) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
            )
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }
    LaunchedEffect(Unit) {
        if (!hasPermission) requestPermission.launch(Manifest.permission.CAMERA)
    }
    LaunchedEffect(hasPermission, lifecycleOwner) {
        if (hasPermission) viewModel.bindToCamera(context.applicationContext, lifecycleOwner)
    }
    LightStatusBarIcons()

    Box(Modifier.fillMaxSize().background(LensVestColors.CameraNight)) {
        uiState.surfaceRequest?.let { CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize()) }

        if (!hasPermission) {
            PermissionPrompt(onAllow = allowCamera)
        } else {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.lens_scan_frame), null, Modifier.size(200.dp, 262.dp))
                Spacer(Modifier.height(20.dp))
                Text(
                    "Centrá el producto en el recuadro",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                    color = CreamText.copy(alpha = 0.8f),
                )
            }
        }

        // Darkens the bottom so the controls stay legible over any scene.
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(240.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, LensVestColors.CameraNight.copy(alpha = 0.9f)))),
        )

        Row(
            Modifier.statusBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (uiState.hasFlash) {
                GlassButton(R.drawable.ic_zap, if (uiState.isTorchOn) "Apagar linterna" else "Prender linterna", onClick = viewModel::toggleTorch)
            } else {
                Spacer(Modifier.size(44.dp))
            }
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Row(
                    Modifier.clip(RoundedCornerShape(999.dp)).background(Glass).padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_scan), null, tint = CreamText, modifier = Modifier.size(16.dp))
                    Text("Apuntá a una marca", style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp), color = CreamText)
                }
            }
            GlassButton(R.drawable.ic_info, "Cómo funciona") {
                scope.launch {
                    snackbar.showSnackbar("Apuntá a un producto de una marca conocida, como Coca-Cola, Apple, Nike o McDonald's.")
                }
            }
        }

        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 40.dp).width(296.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LabeledGlassButton(R.drawable.ic_clock, "Historial") { comingSoon("Historial de escaneos") }
            Image(
                painterResource(R.drawable.lens_capture_button),
                contentDescription = "Reconocer marca",
                modifier = Modifier.size(84.dp).alpha(if (hasPermission) 1f else 0.4f).clip(CircleShape)
                    .clickable(enabled = hasPermission) { comingSoon("Reconocer la marca") },
            )
            LabeledGlassButton(R.drawable.ic_search, "Buscar") { comingSoon("Buscar a mano") }
        }
    }
}

/** The camera is dark, so the status bar icons go light while Lens is open. */
@Composable
private fun LightStatusBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { previous?.let { controller.isAppearanceLightStatusBars = it } }
    }
}

@Composable
private fun PermissionPrompt(onAllow: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Necesitamos la cámara",
            style = MaterialTheme.typography.headlineSmall,
            color = CreamText,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Con la cámara reconocemos la marca del producto que tenés adelante. Las imágenes no se guardan.",
            style = MaterialTheme.typography.bodyMedium,
            color = CreamText.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        PrimaryButton("Permitir cámara", onClick = onAllow, containerColor = LensVestColors.Forest)
    }
}

@Composable
private fun GlassButton(icon: Int, description: String, size: Int = 44, onClick: () -> Unit) {
    Box(
        Modifier.size(size.dp).clip(CircleShape).background(Glass).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, tint = CreamText, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun LabeledGlassButton(icon: Int, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        GlassButton(icon, label, size = 52, onClick = onClick)
        Text(
            label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
            color = CreamText.copy(alpha = 0.85f),
        )
    }
}
