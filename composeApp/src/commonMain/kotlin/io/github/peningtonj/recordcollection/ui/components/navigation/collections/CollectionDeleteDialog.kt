package io.github.peningtonj.recordcollection.ui.components.navigation.collections

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection

@Composable
fun CollectionDeleteDialog(
    collection: AlbumCollection,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Delete Collection") },
            text = { 
                Text("Are you sure you want to delete \"${collection.name}\"? This action cannot be undone.") 
            },
            confirmButton = {
                Button(
                    onClick = onConfirm,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}