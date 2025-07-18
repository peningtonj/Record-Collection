package io.github.peningtonj.recordcollection.ui.components.navigation.collections

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.peningtonj.recordcollection.ui.components.common.LoadingIndicator
import io.github.peningtonj.recordcollection.viewmodel.UiState
import io.github.peningtonj.recordcollection.viewmodel.ArticleImportData
import io.github.peningtonj.recordcollection.viewmodel.AlbumLookUpResult

@Composable
fun ImportCollectionDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onMakeCollection: (String) -> Unit,
    uiState: UiState,
    data: ArticleImportData?
) {
    var textValue by remember { mutableStateOf("") }

    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ImportDialogTitle(uiState) },
        text = {
            ImportDialogContent(
                uiState = uiState,
                data = data,
                textValue = textValue,
                onTextChange = { textValue = it }
            )
        },
        confirmButton = {
            ImportDialogConfirmButton(
                data = data,
                textValue = textValue,
                onSearch = onSearch,
                onMakeCollection = onMakeCollection
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp),
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        ),
    )
}

@Composable
private fun ImportDialogTitle(uiState: UiState) {
    Text(
        text = if (uiState is UiState.Loading) "Processing with OpenAI" else "Draft Collection",
        style = MaterialTheme.typography.headlineSmall
    )
}

@Composable
private fun ImportDialogContent(
    uiState: UiState,
    data: ArticleImportData?,
    textValue: String,
    onTextChange: (String) -> Unit
) {
    when {
        uiState is UiState.Loading && (data?.lookupResults?.isEmpty() != false) -> LoadingIndicator()
        uiState is UiState.Error -> Column {
            Text(uiState.message)
            Text("The response looks like ${data?.openAiResponse}.")
        }
        data?.lookupResults?.isNotEmpty() == true -> {
            Column {
                if (data.lookupResults.size == data.albumNames.size) {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = onTextChange,
                        label = { Text("Collection Name:") },
                        placeholder = { Text("Enter Collection Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(data.lookupResults) {
                        if (it.album != null) AlbumRow(it)
                        else Text("Failed to search for ${it.query.album}")
                    }
                }
            }
        }
        data?.albumNames?.isNotEmpty() == true -> Column {
            Text("Importing collection including the following albums:")
            data.albumNames.take(5).forEach {
                Text("${it.album} by ${it.artist}")
            }
        }
        else -> Text("No albums found to import.")
    }
}

@Composable
private fun ImportDialogConfirmButton(
    data: ArticleImportData?,
    textValue: String,
    onSearch: () -> Unit,
    onMakeCollection: (String) -> Unit
) {
    when {
        data?.lookupResults?.isNotEmpty() == true && data.lookupResults.size == data.albumNames.size -> {
            Button(
                onClick = { onMakeCollection(textValue) },
                enabled = textValue.isNotBlank()
            ) {
                Text("Create Collection")
            }
        }
        data?.albumNames?.isNotEmpty() == true -> {
            Button(onClick = onSearch) {
                Text("Search Spotify For Albums")
            }
        }
    }
}

@Composable
private fun AlbumRow(it: AlbumLookUpResult) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        AsyncImage(
            model = it.album?.images?.firstOrNull()?.url,
            contentDescription = "Album cover for ${it.album?.name}",
            modifier = Modifier
                .width(50.dp)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = it.album?.name ?: "Unknown Album",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.75f)
            )
            Text(
                text = it.album?.artists?.firstOrNull()?.name ?: "Unknown Artist",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        }
    }
}
