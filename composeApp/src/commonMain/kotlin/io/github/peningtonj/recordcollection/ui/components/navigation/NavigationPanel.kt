package io.github.peningtonj.recordcollection.ui.components.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import io.github.peningtonj.recordcollection.db.domain.AlbumCollection
import io.github.peningtonj.recordcollection.navigation.Navigator
import io.github.peningtonj.recordcollection.navigation.Screen
import io.github.peningtonj.recordcollection.ui.collections.CollectionsViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionDetailViewModel
import io.github.peningtonj.recordcollection.viewmodel.rememberCollectionsViewModel

@Composable
fun NavigationPanel(
    navigator: Navigator,
    currentScreen: Screen,
    collectionsViewModel: CollectionsViewModel = rememberCollectionsViewModel(),
    modifier: Modifier = Modifier
) {
    val collectionsUiState by collectionsViewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(240.dp)
            .background(colorScheme.surface)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Navigation",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Main navigation items
        NavigationItem(
            title = "Records",
            icon = Icons.Default.LibraryMusic,
            isSelected = currentScreen is Screen.Library,
            onClick = { navigator.navigateTo(Screen.Library) }
        )

        NavigationItem(
            title = "Profile",
            icon = Icons.Default.Person,
            isSelected = currentScreen is Screen.Profile,
            onClick = { navigator.navigateTo(Screen.Profile) }
        )

        // Collections section
        Spacer(modifier = Modifier.height(24.dp))
        
        HorizontalDivider(
            color = colorScheme.outline.copy(alpha = 0.3f),
            thickness = 1.dp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Collections",
            style = MaterialTheme.typography.titleMedium,
            color = colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Collections list
        if (collectionsUiState.isLoading) {
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else if (collectionsUiState.collections.isEmpty()) {
            Text(
                text = "No collections yet",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        } else {
            collectionsUiState.collections.forEach { collection ->
                CollectionItem(
                    collection = collection,
                    isSelected = currentScreen is Screen.Collection &&
                               currentScreen.collectionName == collection.name,
                    onClick = { 
                        navigator.navigateTo(Screen.Collection(collection.name))
                    }
                )
            }
        }
    }
}

@Composable
private fun NavigationItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                colorScheme.primaryContainer
            } else {
                colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isSelected) {
                    colorScheme.onPrimaryContainer
                } else {
                    colorScheme.onSurface
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isSelected) {
                    colorScheme.onPrimaryContainer
                } else {
                    colorScheme.onSurface
                }
            )
        }
    }
}