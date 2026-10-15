package ar.edu.uade.lensvest.presentation.ui.assistant

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.R
import ar.edu.uade.lensvest.domain.model.ChatMessage
import ar.edu.uade.lensvest.domain.model.ChatRole
import ar.edu.uade.lensvest.domain.model.PendingAction
import ar.edu.uade.lensvest.presentation.ui.components.LetterLogo
import ar.edu.uade.lensvest.presentation.ui.components.Pill
import ar.edu.uade.lensvest.presentation.ui.components.PrimaryButton
import ar.edu.uade.lensvest.presentation.ui.components.SecondaryButton
import ar.edu.uade.lensvest.presentation.viewmodel.AssistantUiState
import ar.edu.uade.lensvest.presentation.viewmodel.AssistantViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestColors
import ar.edu.uade.lensvest.ui.theme.LensVestTheme

private val suggestions = listOf("¿Cómo voy este mes?", "Comprá $5.000 de KO")

/** E1/E2 · Asistente: chat over the user's Berry account, with operations to confirm. */
@Composable
fun AssistantScreen(viewModel: AssistantViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    var voiceError by remember { mutableStateOf<String?>(null) }

    val voiceInput = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let(viewModel::send)
    }
    val startVoice = {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-AR")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "Decí lo que querés hacer")
        try {
            voiceError = null
            voiceInput.launch(intent)
        } catch (e: ActivityNotFoundException) {
            voiceError = "Este dispositivo no tiene reconocimiento de voz."
        }
    }

    AssistantContent(
        uiState = uiState.copy(error = uiState.error ?: voiceError),
        focusRequester = focusRequester,
        onSend = viewModel::send,
        onConfirm = viewModel::confirmPendingActions,
        onEdit = {
            viewModel.rejectPendingActions()
            focusRequester.requestFocus()
        },
        onVoice = startVoice,
    )
}

@Composable
private fun AssistantContent(
    uiState: AssistantUiState,
    focusRequester: FocusRequester,
    onSend: (String) -> Unit,
    onConfirm: () -> Unit,
    onEdit: () -> Unit,
    onVoice: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()
    val itemCount = uiState.messages.size + uiState.pendingActions.size
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(itemCount - 1)
    }
    val send = {
        onSend(draft)
        draft = ""
    }

    Column(
        Modifier.fillMaxSize().background(LensVestColors.Cream).statusBarsPadding().imePadding().padding(horizontal = 24.dp),
    ) {
        Header()
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(uiState.messages) { ChatBubble(it) }
            items(uiState.pendingActions) { action ->
                OperationCard(action, enabled = !uiState.isLoading, onConfirm = onConfirm, onEdit = onEdit)
            }
        }
        if (uiState.isLoading) {
            LinearProgressIndicator(
                color = LensVestColors.Forest,
                trackColor = LensVestColors.Line,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
        uiState.error?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Terracotta, modifier = Modifier.padding(vertical = 4.dp))
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            suggestions.forEach { suggestion ->
                Text(
                    suggestion,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                    color = LensVestColors.Navy,
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(LensVestColors.Paper)
                        .border(1.dp, LensVestColors.Line, RoundedCornerShape(999.dp))
                        .clickable(enabled = !uiState.isLoading) { onSend(suggestion) }
                        .padding(horizontal = 11.dp, vertical = 7.dp),
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        InputField(
            value = draft,
            onValueChange = { draft = it },
            canSend = draft.isNotBlank() && !uiState.isLoading,
            focusRequester = focusRequester,
            onSend = send,
            onVoice = onVoice,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun Header() {
    Row(Modifier.padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).background(LensVestColors.Forest, CircleShape), contentAlignment = Alignment.Center) {
            Icon(painterResource(R.drawable.ic_sparkles), null, tint = LensVestColors.Cream, modifier = Modifier.size(20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text("Asistente", style = MaterialTheme.typography.titleLarge, color = LensVestColors.Navy)
            Text("Operá y armá estrategias hablando", style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = LensVestColors.Muted)
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    // The corner on the speaker's side is the "tail".
    val shape = if (isUser) RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp) else RoundedCornerShape(4.dp, 18.dp, 18.dp, 18.dp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Text(
            message.text,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = if (isUser) LensVestColors.Cream else LensVestColors.Navy,
            modifier = Modifier
                .widthIn(max = 264.dp)
                .clip(shape)
                .background(if (isUser) LensVestColors.Navy else LensVestColors.Paper)
                .then(if (isUser) Modifier else Modifier.border(1.dp, LensVestColors.Line, shape))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

/** E2 "Vista previa operación": nothing reaches Berry until the user taps Confirmar (RNF01). */
@Composable
private fun OperationCard(action: PendingAction, enabled: Boolean, onConfirm: () -> Unit, onEdit: () -> Unit) {
    val summary = action.summary()
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(LensVestColors.Paper).border(2.dp, LensVestColors.Navy, shape).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Pill("OPERACIÓN DETECTADA", container = LensVestColors.Sand, content = LensVestColors.Navy, fontSize = 10)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            LetterLogo(summary.symbol ?: action.tool, LensVestColors.Terracotta, size = 36.dp)
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(summary.title, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp), color = LensVestColors.Navy)
                Text(summary.subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = LensVestColors.Muted)
            }
        }
        summary.rows.forEach { (label, value) ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = LensVestColors.Muted)
                Text(value, style = MaterialTheme.typography.labelMedium, color = LensVestColors.Navy)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton("Editar", onClick = onEdit, enabled = enabled, compact = true, modifier = Modifier.weight(1f))
            PrimaryButton("Confirmar", onClick = onConfirm, enabled = enabled, compact = true, containerColor = LensVestColors.Forest, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun InputField(
    value: String,
    onValueChange: (String) -> Unit,
    canSend: Boolean,
    focusRequester: FocusRequester,
    onSend: () -> Unit,
    onVoice: () -> Unit,
) {
    val shape = RoundedCornerShape(26.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(LensVestColors.Paper).border(1.dp, LensVestColors.Line, shape)
            .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text("Escribí lo que querés hacer…", style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp), color = LensVestColors.Muted)
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, color = LensVestColors.Navy),
                cursorBrush = SolidColor(LensVestColors.Forest),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
            )
        }
        RoundIcon(R.drawable.ic_mic, "Dictar", Color.Transparent, LensVestColors.Muted, onClick = onVoice)
        RoundIcon(R.drawable.ic_send, "Enviar", LensVestColors.Navy, LensVestColors.Cream, enabled = canSend, onClick = onSend)
    }
}

@Composable
private fun RoundIcon(icon: Int, description: String, container: Color, tint: Color, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(if (enabled) container else container.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), description, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 716)
@Composable
private fun AssistantPreview() {
    LensVestTheme {
        AssistantContent(
            uiState = AssistantUiState(
                messages = listOf(
                    ChatMessage(ChatRole.ASSISTANT, "¡Hola Martina! Contame qué querés hacer, como si le hablaras a un amigo."),
                    ChatMessage(ChatRole.USER, "Comprá $5.000 de Coca-Cola"),
                    ChatMessage(ChatRole.ASSISTANT, "Listo, esta es la operación. Revisala antes de seguir:"),
                ),
                pendingActions = listOf(PendingAction("sevt_1", "place_order", mapOf("symbol" to "KO", "side" to "buy", "amount" to "5000"))),
            ),
            focusRequester = remember { FocusRequester() },
            onSend = {}, onConfirm = {}, onEdit = {}, onVoice = {},
        )
    }
}
