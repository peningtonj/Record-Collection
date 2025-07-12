package io.github.peningtonj.recordcollection.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Temp(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick
    ) {
        Icon(
            imageVector = Icons.Default.ImportExport,
            contentDescription = "Import",
        )
    }
}