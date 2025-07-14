package io.github.peningtonj.recordcollection.ui.components.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun TextInputDialog(
    title: String,
    label: String,
    placeholder: String = "",
    initialValue: String = "",
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    confirmButtonText: String = "OK",
    dismissButtonText: String = "Cancel"
) {
    if (isVisible) {
        var textValue by remember { mutableStateOf(initialValue) }
        
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Column {
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        label = { Text(label) },
                        placeholder = { Text(placeholder) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirm(textValue)
                        onDismiss()
                    },
                    enabled = textValue.isNotBlank()
                ) {
                    Text(confirmButtonText)
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(dismissButtonText)
                }
            },
            shape = RoundedCornerShape(16.dp),
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true
            )
        )
    }
}