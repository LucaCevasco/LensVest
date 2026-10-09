package ar.edu.uade.lensvest.presentation.ui.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ar.edu.uade.lensvest.domain.model.ChatMessage
import ar.edu.uade.lensvest.domain.model.ChatRole
import ar.edu.uade.lensvest.domain.model.PendingAction
import ar.edu.uade.lensvest.presentation.viewmodel.AssistantUiState
import ar.edu.uade.lensvest.presentation.viewmodel.AssistantViewModel
import ar.edu.uade.lensvest.ui.theme.LensVestTheme

@Composable
fun AssistantScreen(viewModel: AssistantViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AssistantContent(
        uiState = uiState,
        onSend = viewModel::send,
        onConfirm = viewModel::confirmPendingActions,
        onReject = viewModel::rejectPendingActions,
    )
}

@Composable
private fun AssistantContent(
    uiState: AssistantUiState,
    onSend: (String) -> Unit,
    onConfirm: () -> Unit,
    onReject: () -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) listState.animateScrollToItem(uiState.messages.lastIndex)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (uiState.messages.isEmpty()) {
                item { Text("Preguntá por tu portfolio o pedí una operación, por ejemplo: \"Comprá USD 50 de KO\".") }
            }
            items(uiState.messages) { MessageBubble(it) }
        }

        uiState.pendingActions.forEach { action ->
            PendingActionCard(action, enabled = !uiState.isLoading, onConfirm = onConfirm, onReject = onReject)
        }
        if (uiState.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
        uiState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                placeholder = { Text("Preguntale a LensVest…") },
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = {
                    onSend(draft)
                    draft = ""
                },
                enabled = draft.isNotBlank() && !uiState.isLoading,
            ) { Text("Enviar") }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.USER
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            ),
            modifier = Modifier.widthIn(max = 300.dp),
        ) {
            Text(message.text, modifier = Modifier.padding(12.dp))
        }
    }
}

/** Review step before an order reaches Berry: nothing is sent until the user confirms. */
@Composable
private fun PendingActionCard(action: PendingAction, enabled: Boolean, onConfirm: () -> Unit, onReject: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(action.title(), style = MaterialTheme.typography.titleMedium)
            action.parameters.forEach { (name, value) -> Text("$name: $value") }
            Text("Broker: Berry", style = MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = onReject, enabled = enabled) { Text("Rechazar") }
                Button(onClick = onConfirm, enabled = enabled) { Text("Confirmar") }
            }
        }
    }
}

private fun PendingAction.title() = when (tool) {
    "place_order" -> "Revisá la orden"
    "cancel_order" -> "Revisá la cancelación"
    else -> "Confirmá: $tool"
}

@Preview(showBackground = true)
@Composable
private fun AssistantContentPreview() {
    LensVestTheme {
        AssistantContent(
            uiState = AssistantUiState(
                messages = listOf(
                    ChatMessage(ChatRole.USER, "Comprá USD 50 de KO"),
                    ChatMessage(ChatRole.ASSISTANT, "Preparé la orden, revisala y confirmala."),
                ),
                pendingActions = listOf(
                    PendingAction("sevt_1", "place_order", mapOf("symbol" to "KO", "side" to "buy", "amount" to "50")),
                ),
            ),
            onSend = {},
            onConfirm = {},
            onReject = {},
        )
    }
}
