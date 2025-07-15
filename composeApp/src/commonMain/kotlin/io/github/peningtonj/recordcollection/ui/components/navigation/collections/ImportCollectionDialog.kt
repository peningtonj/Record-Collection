package io.github.peningtonj.recordcollection.ui.components.navigation.collections

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import io.github.aakira.napier.Napier
import io.github.peningtonj.recordcollection.ui.components.common.LoadingIndicator
import io.github.peningtonj.recordcollection.viewmodel.ImportUiState

@Composable
fun ImportCollectionDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onMakeCollection: (String) -> Unit,
    content: ImportUiState
) {
    var textValue by remember { mutableStateOf("") }

    if (isVisible) {
        Napier.d("Showing import collection dialog...")
        Napier.d("Loading: ${content.isLoading}")
        Napier.d("First Draft Album ${content.albumNames.firstOrNull()}")
        Napier.d("First Real Album ${content.albums.firstOrNull()?.album?.name}")
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                if (content.albumNames.isEmpty() && content.isLoading) {
                    Text(
                        text = "Parsing Article With OpenAI",
                        style = MaterialTheme.typography.headlineSmall
                    )
                } else {
                    Text(
                        text = "Draft Collection",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            },
            text = {
                if(content.isLoading && content.albums.isEmpty()) {
                    LoadingIndicator()
                } else if (content.error == null && content.albums.isNotEmpty()) {
                    Column {
                        if (content.albums.size == content.albumNames.size) {
                            OutlinedTextField(
                                value = textValue,
                                onValueChange = { textValue = it },
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
                            items(content.albums) {
                                if (it.album != null) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        AsyncImage(
                                            model = it.album.images.firstOrNull()?.url,
                                            contentDescription = "Album cover for ${it.album.name}",
                                            modifier = Modifier
                                                .width(50.dp)
                                                .aspectRatio(1f)
                                                .clip(RoundedCornerShape(4.dp)),
                                            contentScale = ContentScale.Fit
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = it.album.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth(0.75f)
                                            )

                                            Text(
                                                text = it.album.artists.firstOrNull()?.name ?: "Unknown Artist",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.fillMaxWidth(0.75f)
                                            )
                                        }
                                    }
                                } else {
                                    Text("Failed to search for ${it.query.album}")
                                }
                            }
                        }
                    }
                } else if (content.error == null && content.albumNames.isNotEmpty()){
                    Column {
                        Text("Importing collection including the following albums: ")
                        content.albumNames.take(5).map { album ->
                            Text("${album.album} by ${album.artist}")
                        }
                    }
                } else if (content.error != null) {
                    Column {
                        Text(content.error)
                        Text("The response looks like ${content.result}.")
                    }
                } else {
                    Text("No albums found to import.")
                }
            },
            confirmButton = {
                if (content.error == null && content.albums.isNotEmpty()) {
                    Button(
                        onClick = {
                            onMakeCollection(textValue)
                        },
                        enabled = textValue.isNotBlank() && (content.albums.size == content.albumNames.size)

                    ) {
                        Text("Create Collection")
                    }
                }
                else if (content.error == null && content.albumNames.isNotEmpty()) {
                    Button(
                        onClick = onSearch,
                    ) {
                        Text("Search Spotify For Albums")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            },            shape = RoundedCornerShape(16.dp),
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            ),
        )
    }

}