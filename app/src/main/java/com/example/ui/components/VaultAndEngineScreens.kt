package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.ArbitrationFocus
import com.example.model.SynthesizedResult
import com.example.ui.theme.BestValueGold
import com.example.ui.theme.CopilotEmerald
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SavedVaultAndHistoryScreen(
    allSessions: List<SynthesizedResult>,
    bookmarkedSessions: List<SynthesizedResult>,
    onSelectSession: (SynthesizedResult) -> Unit,
    onToggleBookmark: (SynthesizedResult) -> Unit,
    onDeleteSession: (Long) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOnlyBookmarked by remember { mutableStateOf(false) }
    val displayedList = if (showOnlyBookmarked) bookmarkedSessions else allSessions

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !showOnlyBookmarked,
                    onClick = { showOnlyBookmarked = false },
                    label = { Text("All Syntheses (${allSessions.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GeminiCyan.copy(alpha = 0.2f),
                        selectedLabelColor = GeminiCyan
                    ),
                    modifier = Modifier.testTag("vault_filter_all")
                )
                FilterChip(
                    selected = showOnlyBookmarked,
                    onClick = { showOnlyBookmarked = true },
                    label = { Text("Saved Vault (${bookmarkedSessions.size})") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BestValueGold.copy(alpha = 0.2f),
                        selectedLabelColor = BestValueGold
                    ),
                    modifier = Modifier.testTag("vault_filter_saved")
                )
            }

            if (!showOnlyBookmarked && allSessions.isNotEmpty()) {
                Text(
                    text = "Clear Unpinned",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable { onClearHistory() }
                        .padding(6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (displayedList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkBorder,
                        contentDescription = null,
                        tint = GeminiCyan,
                        modifier = Modifier.size(44.dp)
                    )
                    Text(
                        text = if (showOnlyBookmarked) "No Saved Syntheses Yet" else "No Dual-Search History Yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Every dual search across Gemini & Copilot is automatically synthesized and can be pinned here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(displayedList, key = { it.id }) { session ->
                    VaultSessionItemCard(
                        session = session,
                        onSelect = { onSelectSession(session) },
                        onToggleBookmark = { onToggleBookmark(session) },
                        onDelete = { onDeleteSession(session.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun VaultSessionItemCard(
    session: SynthesizedResult,
    onSelect: () -> Unit,
    onToggleBookmark: () -> Unit,
    onDelete: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

    Surface(
        color = ObsidianSurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
            .clickable { onSelect() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = GeminiCyan.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Gemini ${session.primaryContributionPct}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = GeminiCyan,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Surface(
                        color = CopilotEmerald.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Copilot ${session.secondaryContributionPct}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = CopilotEmerald,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                    Text(
                        text = "• ${dateFormat.format(Date(session.timestamp))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row {
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (session.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Pin to Vault",
                            tint = if (session.isBookmarked) BestValueGold else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Session",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = session.query,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = session.executiveSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun EngineConfigurationScreen(
    primaryTemplate: String,
    secondaryTemplate: String,
    primaryWeightPct: Int,
    defaultFocus: ArbitrationFocus,
    onSaveConfig: (String, String, Int, ArbitrationFocus) -> Unit,
    onResetDefaults: () -> Unit,
    modifier: Modifier = Modifier
) {
    var primaryInput by remember(primaryTemplate) { mutableStateOf(primaryTemplate) }
    var secondaryInput by remember(secondaryTemplate) { mutableStateOf(secondaryTemplate) }
    var weightSlider by remember(primaryWeightPct) { mutableFloatStateOf(primaryWeightPct.toFloat()) }
    var selectedFocus by remember(defaultFocus) { mutableStateOf(defaultFocus) }
    var savedBannerVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            color = ObsidianSurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = GeminiCyan
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Dual-Engine Pipeline Endpoints",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Use %s as the query placeholder. Both URLs are queried in parallel without switching tabs.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = primaryInput,
                    onValueChange = {
                        primaryInput = it
                        savedBannerVisible = false
                    },
                    label = { Text("Primary Engine URL Template (Gemini Search)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GeminiCyan,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = GeminiCyan
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("primary_engine_url_input")
                )

                OutlinedTextField(
                    value = secondaryInput,
                    onValueChange = {
                        secondaryInput = it
                        savedBannerVisible = false
                    },
                    label = { Text("Secondary Engine URL Template (Copilot Search)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CopilotEmerald,
                        unfocusedBorderColor = GlassBorder,
                        focusedLabelColor = CopilotEmerald
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("secondary_engine_url_input")
                )

                // Weighting Slider
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    val pInt = weightSlider.toInt()
                    val sInt = 100 - pInt
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Primary (Gemini) Weight: $pInt%",
                            style = MaterialTheme.typography.labelLarge,
                            color = GeminiCyan
                        )
                        Text(
                            text = "Secondary (Copilot) Weight: $sInt%",
                            style = MaterialTheme.typography.labelLarge,
                            color = CopilotEmerald
                        )
                    }
                    Slider(
                        value = weightSlider,
                        onValueChange = {
                            weightSlider = it
                            savedBannerVisible = false
                        },
                        valueRange = 20f..80f,
                        colors = SliderDefaults.colors(
                            thumbColor = BestValueGold,
                            activeTrackColor = GeminiCyan,
                            inactiveTrackColor = CopilotEmerald
                        )
                    )
                    Text(
                        text = "Controls how the Value Arbitrator prioritizes depth vs. cross-web breadth when synthesizing answers.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Default Arbitration Focus
                Text(
                    text = "Default Value Arbitration Mode",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                ArbitrationFocus.entries.forEach { focus ->
                    val isSelected = focus == selectedFocus
                    Surface(
                        color = if (isSelected) GeminiCyan.copy(alpha = 0.14f) else ObsidianSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isSelected) GeminiCyan else GlassBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedFocus = focus
                                savedBannerVisible = false
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = focus.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (isSelected) GeminiCyan else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = focus.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (savedBannerVisible) {
                    Text(
                        text = "✓ Dual-engine configuration saved and active for all searches.",
                        style = MaterialTheme.typography.labelLarge,
                        color = CopilotEmerald
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onResetDefaults()
                            primaryInput = "https://gemini.google.com/search?q=%s"
                            secondaryInput = "https://copilot.microsoft.com/search?q=%s"
                            weightSlider = 55f
                            selectedFocus = ArbitrationFocus.BALANCED_VALUE
                            savedBannerVisible = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reset_engines_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Defaults")
                    }

                    Button(
                        onClick = {
                            onSaveConfig(
                                primaryInput,
                                secondaryInput,
                                weightSlider.toInt(),
                                selectedFocus
                            )
                            savedBannerVisible = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GeminiCyan,
                            contentColor = Color(0xFF001E30)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_engines_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save Pipeline")
                    }
                }
            }
        }
    }
}
