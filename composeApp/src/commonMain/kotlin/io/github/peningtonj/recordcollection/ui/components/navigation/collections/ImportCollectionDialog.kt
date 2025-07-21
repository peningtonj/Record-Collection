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
import io.github.peningtonj.recordcollection.service.AlbumNameAndArtist
import io.github.peningtonj.recordcollection.ui.components.common.LoadingIndicator
import io.github.peningtonj.recordcollection.viewmodel.UiState
import io.github.peningtonj.recordcollection.viewmodel.AlbumLookUpResult
@Composable
fun ImportCollectionDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onMakeCollection: (String, Boolean) -> Unit,
    initialValue: String = "",
    uiState: UiState,
) {
    var textValue by remember { mutableStateOf(initialValue) }
    var addToLibrary by remember { mutableStateOf(false) }

    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { ImportDialogTitle(uiState) },
        text = {
            ImportDialogContent(
                uiState = uiState,
                textValue = textValue,
                onTextChange = { textValue = it },
                addToLibrary = addToLibrary,
                onSwitchChange = { addToLibrary = !addToLibrary }
            )
        },
        confirmButton = {
            ImportDialogConfirmButton(
                uiState = uiState,
                textValue = textValue,
                onSearch = onSearch,
                onMakeCollection =  { collectionName ->
                    onMakeCollection(collectionName, addToLibrary)
                }
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
        text = when (uiState) {
            is UiState.Loading -> "Processing..."
            else -> "Draft Collection"
        },
        style = MaterialTheme.typography.headlineSmall
    )
}

@Composable
private fun ImportDialogContent(
    uiState: UiState,
    textValue: String,
    onTextChange: (String) -> Unit,
    addToLibrary: Boolean,
    onSwitchChange: (Boolean) -> Unit
) {

    when (uiState) {
        is UiState.Loading -> LoadingIndicator()

        is UiState.Error -> Column {
            Text("Error: ${uiState.message}")
        }

        is UiState.AlbumsList -> {
            Column {
                Text("Found album names:")
                uiState.albumNames.take(5).forEach {
                    Text("${it.album} by ${it.artist}")
                }
            }
        }

        is UiState.Searching -> {
            Column {
                if (uiState.albums.size == uiState.albumNames.size) {
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
                    items(uiState.albums) {
                        if (it.album != null) AlbumRow(it)
                        else Text("Failed to search for ${it.query.album}")
                    }
                }
            }
        }

        is UiState.ReadyToImport -> {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = onTextChange,
                        label = { Text("Collection Name:") },
                        placeholder = { Text("Enter Collection Name") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        addToLibrary,
                        onCheckedChange = onSwitchChange,
                    )
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(uiState.albums) {
                        AlbumRow(AlbumLookUpResult(query = AlbumNameAndArtist(
                            it.name,
                            it.artists.firstOrNull()?.name.orEmpty()
                        ), album = it))
                    }
                }
            }
        }

        UiState.Idle -> {
            Text("No albums found to import.")
        }
    }
}

@Composable
private fun ImportDialogConfirmButton(
    uiState: UiState,
    textValue: String,
    onSearch: () -> Unit,
    onMakeCollection: (String) -> Unit
) {
    when (uiState) {
        is UiState.Searching -> {
            if (uiState.albumNames.isNotEmpty() && uiState.albums.size < uiState.albumNames.size) {
                Button(onClick = onSearch) {
                    Text("Search Spotify For Albums")
                }
            } else if (uiState.albums.size == uiState.albumNames.size) {
                Button(
                    onClick = { onMakeCollection(textValue) },
                    enabled = textValue.isNotBlank()
                ) {
                    Text("Create Collection")
                }
            }
        }

        is UiState.ReadyToImport -> {
            Button(
                onClick = { onMakeCollection(textValue) },
                enabled = textValue.isNotBlank()
            ) {
                Text("Create Collection")
            }
        }

        is UiState.AlbumsList -> {
            Button(onClick = onSearch) {
                Text("Search Spotify For Albums")
            }
        }

        else -> {}
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
