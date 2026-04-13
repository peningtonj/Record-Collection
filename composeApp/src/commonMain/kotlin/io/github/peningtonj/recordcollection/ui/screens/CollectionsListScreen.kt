package io.github.peningtonj.recordcollection.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.navigation.LocalNavigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.ui.components.common.ScreenHeader
import io.github.peningtonj.recordcollection.ui.components.navigation.collections.CollectionsSection

@Composable
fun CollectionsListScreen() {
    val navigator = LocalNavigator.current ?: return

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Collections",
            icon = Icons.Default.Folder,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            CollectionsSection(
                currentScreen = Screen.Collections,
                navigator = navigator
            )
        }
    }
}

