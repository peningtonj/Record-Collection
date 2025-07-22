package io.github.peningtonj.recordcollection.ui.components.filter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import kotlinx.datetime.Clock
import kotlinx.datetime.toLocalDateTime

@Composable
fun ReleaseYearFilter(
    onFilterChange: (Int, Int, String?) -> Unit, // Add optional label parameter
    startYear: Int
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "Filter by year",
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Release Year")
        }

        if (expanded) {
            Popup(
                alignment = Alignment.TopEnd,
                offset = IntOffset(x = -80, y = 48),
                onDismissRequest = { expanded = false }
            ) {
                Surface(
                    modifier = Modifier
                        .width(380.dp)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    ReleaseYearWidget(
                        onYearSelected = { start, end, label ->
                            onFilterChange(start, end, label)
                            expanded = false
                        },
                        startYear = startYear
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReleaseYearWidget(
    onYearSelected: (Int, Int, String?) -> Unit, // Add optional label parameter
    startYear: Int
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Filter by Release Year",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Range slider for years
        var yearRange by remember { mutableStateOf(startYear.toFloat() ..2025f) }
        var selectedLabel by remember { mutableStateOf<String?>(null) }

        Text("${yearRange.start.toInt()} - ${yearRange.endInclusive.toInt()}")

        RangeSlider(
            value = yearRange,
            onValueChange = { 
                yearRange = it
                selectedLabel = null // Clear label when manually adjusting slider
            },
            valueRange = startYear.toFloat()..Clock.System.now().toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).year.toFloat(),
            modifier = Modifier
                .padding(vertical = 8.dp)
                .height(32.dp)
        )

        QuickSelectButtons(
            onClick = { range, label ->
                yearRange = range
                selectedLabel = label
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = { /* Clear filter */ },
                modifier = Modifier.scale(0.8f)
            ) {
                Text("Clear")
            }

            Button(
                onClick = {
                    onYearSelected(yearRange.start.toInt(), yearRange.endInclusive.toInt(), selectedLabel)
                },
                modifier = Modifier.scale(0.8f)
            ) {
                Text("Apply")
            }
        }
    }
}

@Composable
fun QuickSelectButtons(
    onClick: (ClosedFloatingPointRange<Float>, String) -> Unit
) {
    val currentYear = 2025
    val presets = listOf(
        "This Year" to (currentYear.toFloat()..currentYear.toFloat()),
        "This Decade" to (2020f..currentYear.toFloat()),
        "Last 5 Years" to ((currentYear - 5f)..currentYear.toFloat()),
    )

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        presets.forEach { (label, range) ->
            AssistChip(
                onClick = { onClick(range, label) },
                label = { Text(label, fontSize = 12.sp) }
            )
        }
    }
}