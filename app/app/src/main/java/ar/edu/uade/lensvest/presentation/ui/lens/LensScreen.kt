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
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import ar.edu.uade.lensvest.domain.model.RecognizedBrand
import ar.edu.uade.lensvest.presentation.ui.components.LetterLogo
import ar.edu.uade.lensvest.presentation.ui.components.Pill
import ar.edu.uade.lensvest.presentation.ui.components.PrimaryButton
import ar.edu.uade.lensvest.presentation.ui.components.SecondaryButton
import ar.edu.uade.lensvest.presentation.ui.components.ChangePill
import ar.edu.uade.lensvest.presentation.ui.components.formatUsd
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import ar.edu.uade.lensvest.presentation.viewmodel.LensViewModel
import ar.edu.uade.lensvest.presentation.viewmodel.ScanState
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private val Glass = Color.White.copy(alpha = 0.14f)
private val CreamText = LensVestColors.Cream

/**
 * 03 · Lens Mode: live camera; the capture button asks the AI which brand is in
 * front of it (04), the user confirms it (D1) and sees its price (D2).
 */
@Composable
fun LensScreen(
    onOpenAsset: (ticker: String, brand: String, scanId: Long?) -> Unit,
    onOpenHistory: () -> Unit,
    onSearch: () -> Unit,
    viewModel: LensViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
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

    val scan = uiState.scan
    Box(Modifier.fillMaxSize().background(LensVestColors.CameraNight)) {
        uiState.surfaceRequest?.let { CameraXViewfinder(surfaceRequest = it, modifier = Modifier.fillMaxSize()) }

        if (!hasPermission) {
            PermissionPrompt(onAllow = allowCamera)
        } else {
            ScanFrame(scan, Modifier.align(Alignment.Center))
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
                    Icon(painterResource(if (scan is ScanState.Recognizing) R.drawable.ic_sparkles else R.drawable.ic_scan), null, tint = CreamText, modifier = Modifier.size(16.dp))
                    Text(scan.tip(), style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp), color = CreamText)
                }
            }
            GlassButton(R.drawable.ic_info, "Cómo funciona") {
                scope.launch {
                    snackbar.showSnackbar("Apuntá a un producto de una marca conocida, como Coca-Cola, Apple, Nike o McDonald's, y tocá el botón.")
                }
            }
        }

        val bottom = Modifier.align(Alignment.BottomCenter).navigationBarsPadding()
        when (scan) {
            is ScanState.Recognized -> ConfirmBrandSheet(
                brand = scan.brand,
                onConfirm = viewModel::confirmBrand,
                onReject = viewModel::scanAgain,
                onSearch = onSearch,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            is ScanState.Confirmed -> Column(
                bottom.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                BrandCard(
                    scan = scan,
                    onOpenAsset = { ticker -> onOpenAsset(ticker, scan.brand.brand, scan.scanId) },
                    onRetryQuote = viewModel::retryQuote,
                )
                Row(
                    Modifier.clip(RoundedCornerShape(999.dp)).clickable(onClick = viewModel::scanAgain).padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(R.drawable.ic_scan), null, tint = CreamText, modifier = Modifier.size(16.dp))
                    Text("Escanear otro producto", style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp), color = CreamText)
                }
            }
            ScanState.NotFound -> MessageCard(
                title = "No reconocimos una marca",
                message = "Acercate al logo, mejorá la luz y probá de nuevo.",
                onRetry = viewModel::scanAgain,
                modifier = bottom.padding(16.dp),
            )
            is ScanState.Failed -> MessageCard(
                title = "No pudimos analizar la foto",
                message = scan.message,
                onRetry = viewModel::scanAgain,
                modifier = bottom.padding(16.dp),
            )
            // 04 · Reconociendo
            ScanState.Recognizing -> Row(
                bottom.padding(16.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(LensVestColors.Paper).padding(18.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(color = LensVestColors.Forest, trackColor = LensVestColors.Mint, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Reconociendo la marca…", style = MaterialTheme.typography.labelLarge, color = LensVestColors.Navy)
                    Text("Esto tarda unos segundos", style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
                }
            }
            ScanState.Scanning -> Row(
                bottom.padding(bottom = 40.dp).width(296.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LabeledGlassButton(R.drawable.ic_clock, "Historial", onClick = onOpenHistory)
                val canCapture = hasPermission && scan == ScanState.Scanning
                Image(
                    painterResource(R.drawable.lens_capture_button),
                    contentDescription = "Reconocer marca",
                    modifier = Modifier.size(84.dp).alpha(if (canCapture) 1f else 0.4f).clip(CircleShape)
                        .clickable(enabled = canCapture, onClick = viewModel::recognize),
                )
                LabeledGlassButton(R.drawable.ic_search, "Buscar", onClick = onSearch)
            }
        }
    }
}

private fun ScanState.tip() = when (this) {
    ScanState.Recognizing -> "Analizando la imagen"
    is ScanState.Recognized, is ScanState.Confirmed -> "Encontramos algo"
    else -> "Apuntá a una marca"
}

/** The scan frame; it pulses while the photo is being analyzed. */
@Composable
private fun ScanFrame(scan: ScanState, modifier: Modifier) {
    val pulse = rememberInfiniteTransition(label = "scan")
    val alpha by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "frameAlpha",
    )
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Image(
            painterResource(R.drawable.lens_scan_frame),
            null,
            Modifier.size(200.dp, 262.dp).alpha(if (scan == ScanState.Recognizing) alpha else 1f),
        )
        Spacer(Modifier.height(20.dp))
        if (scan == ScanState.Scanning || scan == ScanState.Recognizing) {
            Text(
                if (scan == ScanState.Recognizing) "Mirando la marca…" else "Centrá el producto en el recuadro",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
                color = CreamText.copy(alpha = 0.8f),
            )
        }
    }
}

/** D1 · Confirmar marca: the AI's guess, so a wrong brand never leads to a wrong company. */
@Composable
private fun ConfirmBrandSheet(
    brand: RecognizedBrand,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)).background(LensVestColors.Cream)
            .navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.size(36.dp, 4.dp).background(LensVestColors.Line, RoundedCornerShape(2.dp)))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            LetterLogo(brand.brand, LensVestColors.Forest, size = 56.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("¿ES ESTA MARCA?", style = MaterialTheme.typography.labelSmall, color = LensVestColors.Muted)
                Text("¿Es ${brand.brand}?", style = MaterialTheme.typography.headlineSmall, color = LensVestColors.Navy)
                brand.company?.let {
                    Text(
                        buildAnnotatedString {
                            append("de ")
                            withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = LensVestColors.Navy)) { append(it) }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = LensVestColors.Muted,
                    )
                }
            }
        }
        PrimaryButton("Sí, es esa", onClick = onConfirm, containerColor = LensVestColors.Forest, leadingIcon = R.drawable.ic_check)
        SecondaryButton("No, es otra", onClick = onReject)
        Text(
            buildAnnotatedString {
                append("¿No aparece? ")
                withStyle(SpanStyle(fontWeight = FontWeight.SemiBold, color = LensVestColors.Forest)) { append("Buscar a mano") }
            },
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp),
            color = LensVestColors.Muted,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onSearch).padding(6.dp),
        )
    }
}

/** D2 · Marca confirmada: the company behind the brand and its price, over the camera. */
@Composable
private fun BrandCard(scan: ScanState.Confirmed, onOpenAsset: (String) -> Unit, onRetryQuote: () -> Unit) {
    val brand = scan.brand
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(LensVestColors.Paper).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            LetterLogo(brand.brand, LensVestColors.Forest, size = 44.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(brand.brand, style = MaterialTheme.typography.labelLarge.copy(fontSize = 17.sp), color = LensVestColors.Navy)
                brand.company?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted) }
            }
            brand.ticker?.let { Pill("$it · CEDEAR", container = LensVestColors.Sand, content = LensVestColors.Navy, fontSize = 11) }
        }
        val ticker = brand.ticker
        when {
            ticker == null -> Text(
                "${brand.company ?: "La empresa"} no cotiza en bolsa, así que no se puede invertir en ella.",
                style = MaterialTheme.typography.bodySmall,
                color = LensVestColors.Navy,
            )
            scan.quote != null -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatUsd(scan.quote.priceUsd), style = MaterialTheme.typography.headlineMedium, color = LensVestColors.Navy)
                scan.quote.changePct?.let { ChangePill(it, suffix = " hoy") }
            }
            scan.quoteError != null -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(scan.quoteError, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Terracotta, modifier = Modifier.weight(1f))
                Text(
                    "Reintentar",
                    style = MaterialTheme.typography.labelMedium,
                    color = LensVestColors.Forest,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onRetryQuote).padding(8.dp),
                )
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(color = LensVestColors.Forest, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                Text("Buscando el precio en Berry…", style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
            }
        }
        if (ticker != null) {
            PrimaryButton("Ver ficha de la empresa", onClick = { onOpenAsset(ticker) }, showArrow = true, compact = true)
        }
    }
}

@Composable
private fun MessageCard(title: String, message: String, onRetry: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(LensVestColors.Paper).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = LensVestColors.Navy)
        Text(message, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
        Spacer(Modifier.height(4.dp))
        PrimaryButton("Probar de nuevo", onClick = onRetry, compact = true)
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
