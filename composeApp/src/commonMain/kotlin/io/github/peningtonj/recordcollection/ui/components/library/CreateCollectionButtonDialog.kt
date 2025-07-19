package io.github.peningtonj.recordcollection.ui.components.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp

@Composable
fun CreateCollectionButton(
    onCreateCollection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }

    AssistChip(
        onClick = { showDialog = true },
        modifier = modifier,
        label = { Text("Create Collection From Filtered Albums") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create Collection From Filter"
            )
        }
    )
    
    if (showDialog) {
        CreateCollectionDialog(
            onDismiss = { showDialog = false },
            onConfirm = { name ->
                onCreateCollection(name)
                showDialog = false
            }
        )
    }
}

@Composable
private fun CreateCollectionDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var collectionName by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    
    fun handleConfirm() {
        val trimmedName = collectionName.trim()
        if (trimmedName.isNotEmpty()) {
            onConfirm(trimmedName)
        } else {
            isError = true
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create Collection")
        },
        text = {
            Column {
                Text(
                    text = "Enter a name for your new collection:",
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = collectionName,
                    onValueChange = { 
                        collectionName = it
                        isError = false
                    },
                    label = { Text("Collection Name") },
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Collection name cannot be empty") }
                    } else null,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            keyboardController?.hide()
                            handleConfirm()
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { handleConfirm() }
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancel")
            }
        }
    )
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}