package io.github.peningtonj.recordcollection.ui.components.tag

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp


@Composable
fun AddTagDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var tagKey by remember { mutableStateOf("") }
    var tagValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Tag") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tagKey,
                    onValueChange = { tagKey = it },
                    label = { Text("Tag Key") },
                    placeholder = { Text("e.g., genre, mood, style") }
                )
                OutlinedTextField(
                    value = tagValue,
                    onValueChange = { tagValue = it },
                    label = { Text("Tag Value") },
                    placeholder = { Text("e.g., rock, energetic, indie") }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tagKey, tagValue) },
                enabled = tagKey.isNotBlank() && tagValue.isNotBlank()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
