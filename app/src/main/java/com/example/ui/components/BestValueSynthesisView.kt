package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.ArbitrationFocus
import com.example.model.CollectedCitation
import com.example.model.EngineOrigin
import com.example.model.LiveStreamStatus
import com.example.model.StreamComparisonPoint
import com.example.model.SynthesizedResult
import com.example.model.ValuableTakeaway
import com.example.ui.theme.BestValueGold
import com.example.ui.theme.BestValueMint
import com.example.ui.theme.ConsensusViolet
import com.example.ui.theme.CopilotEmerald
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.ui.theme.ObsidianSurfaceVariant

@Composable
fun BestValueSynthesisView(
    result: SynthesizedResult?,
    liveStatus: LiveStreamStatus,
    isSynthesizing: Boolean,
    selectedFocus: ArbitrationFocus,
    isSpeaking: Boolean,
    onFocusSelected: (ArbitrationFocus) -> Unit,
    onOpenDualLiveStreams: () -> Unit,
    onOpenCitationUrl: (String) -> Unit,
    onFollowUpClicked: (String) -> Unit,
    onToggleBookmark: () -> Unit,
    onToggleSpeak: () -> Unit,
    onCopyBrief: () -> Unit,
    onShareBrief: () -> Unit,
    onReArbitrate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRawStreams by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("best_value_synthesis_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Hero Dual-Engine Fusion Telemetry Card
        item {
            DualStreamTelemetryHeroCard(
                liveStatus = liveStatus,
                result = result,
                isSynthesizing = isSynthesizing,
                onOpenDualLiveStreams = onOpenDualLiveStreams,
                onReArbitrate = onReArbitrate
            )
        }

        // 2. Arbitration Lens / Value Filter Bar
        item {
            ArbitrationFocusSelectorBar(
                selectedFocus = selectedFocus,
                onFocusSelected = onFocusSelected
            )
        }

        // 3. Active Synthesis Progress Banner (when querying both engines)
        if (isSynthesizing) {
            item {
                SynthesizingActiveBanner(stageMessage = liveStatus.stageMessage)
            }
        }

        if (result != null) {
            // 4. Distilled High-Value Intelligence Executive Brief Card
            item {
                ExecutiveBestValueCard(
                    result = result,
                    isSpeaking = isSpeaking,
                    onToggleBookmark = onToggleBookmark,
                    onToggleSpeak = onToggleSpeak,
                    onCopyBrief = onCopyBrief,
                    onShareBrief = onShareBrief
                )
            }

            // 5. Value Arbitration Verdict & Contribution Split
            item {
                ValueArbitrationVerdictCard(result = result)
            }

            // 6. Verified High-Value Takeaways Section Header
            item {
                SectionHeaderRow(
                    title = stringResource(R.string.section_key_takeaways),
                    subtitle = "Ranked by cross-engine signal density & actionability",
                    accentColor = BestValueGold
                )
            }

            // 7. Individual Takeaway Cards
            items(result.takeaways) { takeaway ->
                ValuableTakeawayCard(takeaway = takeaway)
            }

            // 8. Primary (Gemini) vs. Secondary (Copilot) Stream Delta Matrix
            item {
                StreamDeltaComparisonCard(
                    comparisonPoints = result.comparisonPoints,
                    primaryRawSummary = result.primaryRawSummary,
                    secondaryRawSummary = result.secondaryRawSummary,
                    showRawStreams = showRawStreams,
                    onToggleRawStreams = { showRawStreams = !showRawStreams },
                    onOpenDualLiveStreams = onOpenDualLiveStreams
                )
            }

            // 9. Collected Source Links & Citations
            item {
                SectionHeaderRow(
                    title = stringResource(R.string.section_citations),
                    subtitle = "Tap any source or live search endpoint to inspect in Browser Reader",
                    accentColor = GeminiCyan
                )
            }

            items(result.citations) { citation ->
                CitationSourceCard(
                    citation = citation,
                    onClick = { onOpenCitationUrl(citation.url) }
                )
            }

            // 10. Deep-Dive Follow-Up Queries
            item {
                FollowUpQueriesSection(
                    queries = result.followUpQueries,
                    onFollowUpClicked = onFollowUpClicked
                )
            }

            // 11. Security / Prototype Footnote
            item {
                Text(
                    text = stringResource(R.string.security_prototype_notice),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun DualStreamTelemetryHeroCard(
    liveStatus: LiveStreamStatus,
    result: SynthesizedResult?,
    isSynthesizing: Boolean,
    onOpenDualLiveStreams: () -> Unit,
    onReArbitrate: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurfaceElevated),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                brush = Brush.horizontalGradient(
                    listOf(GeminiCyan.copy(alpha = 0.6f), BestValueGold.copy(alpha = 0.5f), CopilotEmerald.copy(alpha = 0.6f))
                ),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_fusion_1791506530897),
                contentDescription = "Dual-engine data synthesis visualization",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
                    .clip(RoundedCornerShape(22.dp))
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ObsidianBackground.copy(alpha = 0.62f),
                                ObsidianSurfaceElevated.copy(alpha = 0.94f),
                                ObsidianSurfaceElevated
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = BestValueGold.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.border(1.dp, BestValueGold.copy(alpha = 0.45f), RoundedCornerShape(50))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Hub,
                                    contentDescription = null,
                                    tint = BestValueGold,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ZERO-SWITCH DUAL ENGINE FUSION",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BestValueGold,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Surface(
                            color = GeminiCyan.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clickable { onOpenDualLiveStreams() }
                                .testTag("inspect_dual_streams_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ViewColumn,
                                    contentDescription = "Open Dual Live Streams",
                                    tint = GeminiCyan,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Live Split View",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GeminiCyan,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Dual Engine Pills Row (Primary Gemini + Secondary Copilot)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EngineEndpointBadge(
                            label = "PRIMARY • GEMINI",
                            url = result?.primaryUrl ?: liveStatus.primaryUrl,
                            latencyMs = result?.primaryLatencyMs ?: 640L,
                            accentColor = GeminiCyan,
                            isLoading = isSynthesizing || liveStatus.primaryLoading,
                            modifier = Modifier.weight(1f)
                        )
                        EngineEndpointBadge(
                            label = "SECONDARY • COPILOT",
                            url = result?.secondaryUrl ?: liveStatus.secondaryUrl,
                            latencyMs = result?.secondaryLatencyMs ?: 710L,
                            accentColor = CopilotEmerald,
                            isLoading = isSynthesizing || liveStatus.secondaryLoading,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Status & Re-Arbitrate Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = liveStatus.stageMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Re-Arbitrate ↻",
                            style = MaterialTheme.typography.labelMedium,
                            color = BestValueGold,
                            modifier = Modifier
                                .clickable(enabled = !isSynthesizing) { onReArbitrate() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("rearbitrate_button")
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EngineEndpointBadge(
    label: String,
    url: String,
    latencyMs: Long,
    accentColor: Color,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ObsidianBackground.copy(alpha = 0.78f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(if (isLoading) BestValueGold else accentColor)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = accentColor,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${latencyMs}ms",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ArbitrationFocusSelectorBar(
    selectedFocus: ArbitrationFocus,
    onFocusSelected: (ArbitrationFocus) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "VALUE ARBITRATION LENS (HOW BOTH ENGINES ARE SCORED)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ArbitrationFocus.entries.forEach { focus ->
                val selected = focus == selectedFocus
                FilterChip(
                    selected = selected,
                    onClick = { onFocusSelected(focus) },
                    label = {
                        Text(
                            text = focus.title,
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    } else null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = GeminiCyan.copy(alpha = 0.22f),
                        selectedLabelColor = GeminiCyan,
                        selectedLeadingIconColor = GeminiCyan,
                        containerColor = ObsidianSurfaceElevated,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = GlassBorder,
                        selectedBorderColor = GeminiCyan
                    ),
                    modifier = Modifier.testTag("focus_chip_${focus.name.lowercase()}")
                )
            }
        }
    }
}

@Composable
private fun SynthesizingActiveBanner(stageMessage: String) {
    Surface(
        color = ObsidianSurfaceElevated,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GeminiCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                color = GeminiCyan,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Collecting & Arbitrating Dual Search Streams…",
                    style = MaterialTheme.typography.titleSmall,
                    color = GeminiCyan
                )
                Text(
                    text = stageMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExecutiveBestValueCard(
    result: SynthesizedResult,
    isSpeaking: Boolean,
    onToggleBookmark: () -> Unit,
    onToggleSpeak: () -> Unit,
    onCopyBrief: () -> Unit,
    onShareBrief: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = ObsidianSurface),
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(GeminiCyan, BestValueGold, CopilotEmerald)
                ),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = BestValueGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.section_best_value_brief),
                        style = MaterialTheme.typography.titleMedium,
                        color = BestValueGold
                    )
                }

                Surface(
                    color = BestValueMint.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.border(1.dp, BestValueMint.copy(alpha = 0.5f), RoundedCornerShape(50))
                ) {
                    Text(
                        text = "${result.overallConfidenceScore}% Value Confidence",
                        style = MaterialTheme.typography.labelMedium,
                        color = BestValueMint,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = result.query,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = result.executiveSummary,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.94f)
            )

            HorizontalDivider(color = GlassBorder.copy(alpha = 0.6f))

            // Action Toolbar: Voice TTS, Save Vault, Copy, Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onToggleSpeak,
                        modifier = Modifier.testTag("tts_speak_button")
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = stringResource(R.string.btn_speak),
                            tint = if (isSpeaking) BestValueGold else GeminiCyan
                        )
                    }
                    IconButton(
                        onClick = onToggleBookmark,
                        modifier = Modifier.testTag("bookmark_result_button")
                    ) {
                        Icon(
                            imageVector = if (result.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = stringResource(R.string.btn_bookmark),
                            tint = if (result.isBookmarked) BestValueGold else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onCopyBrief,
                        modifier = Modifier.testTag("copy_synthesis_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = stringResource(R.string.btn_copy),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onShareBrief,
                        modifier = Modifier.testTag("share_synthesis_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(R.string.btn_share),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Text(
                    text = "Lens: ${result.arbitrationFocus.title}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ValueArbitrationVerdictCard(result: SynthesizedResult) {
    Surface(
        color = ObsidianSurfaceElevated,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(18.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = ConsensusViolet,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.section_value_arbitration),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = result.valueArbitrationVerdict,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Contribution Split Bar
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Primary: Gemini (${result.primaryContributionPct}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = GeminiCyan
                    )
                    Text(
                        text = "Secondary: Copilot (${result.secondaryContributionPct}%)",
                        style = MaterialTheme.typography.labelMedium,
                        color = CopilotEmerald
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .weight(result.primaryContributionPct.coerceAtLeast(10).toFloat())
                            .fillMaxHeight()
                            .background(GeminiCyan)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Box(
                        modifier = Modifier
                            .weight(result.secondaryContributionPct.coerceAtLeast(10).toFloat())
                            .fillMaxHeight()
                            .background(CopilotEmerald)
                    )
                }
            }
        }
    }
}

@Composable
private fun ValuableTakeawayCard(takeaway: ValuableTakeaway) {
    val badgeColor = when (takeaway.origin) {
        EngineOrigin.PRIMARY_GEMINI -> GeminiCyan
        EngineOrigin.SECONDARY_COPILOT -> CopilotEmerald
        EngineOrigin.CONSENSUS_BOTH -> BestValueGold
    }

    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, badgeColor.copy(alpha = 0.38f), RoundedCornerShape(18.dp))
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
                Surface(
                    color = badgeColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = takeaway.origin.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Surface(
                    color = ObsidianSurfaceVariant,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Value Score: ${takeaway.valueScore}/100",
                        style = MaterialTheme.typography.labelSmall,
                        color = BestValueGold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Text(
                text = takeaway.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = takeaway.detail,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )

            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Why it's valuable: ${takeaway.whyValuable}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StreamDeltaComparisonCard(
    comparisonPoints: List<StreamComparisonPoint>,
    primaryRawSummary: String,
    secondaryRawSummary: String,
    showRawStreams: Boolean,
    onToggleRawStreams: () -> Unit,
    onOpenDualLiveStreams: () -> Unit
) {
    Surface(
        color = ObsidianSurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CompareArrows,
                        contentDescription = null,
                        tint = GeminiCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.section_stream_comparison),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            comparisonPoints.forEach { point ->
                Surface(
                    color = ObsidianSurface,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = point.dimension,
                            style = MaterialTheme.typography.labelLarge,
                            color = BestValueGold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(GeminiCyan.copy(alpha = 0.08f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "GEMINI PRIMARY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GeminiCyan,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = point.primaryGeminiPoint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CopilotEmerald.copy(alpha = 0.08f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "COPILOT SECONDARY",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = CopilotEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = point.secondaryCopilotPoint,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Expandable Raw Collected Stream Inspector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ObsidianSurfaceVariant.copy(alpha = 0.6f))
                    .clickable { onToggleRawStreams() }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("toggle_raw_streams_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showRawStreams) "Hide Raw Collected Stream Summaries" else "Inspect Raw Collected Stream Summaries (Gemini + Copilot)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (showRawStreams) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = GeminiCyan
                )
            }

            AnimatedVisibility(visible = showRawStreams) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, GeminiCyan.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "RAW PRIMARY EXTRACT (https://gemini.google.com/search?q=%s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = GeminiCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = primaryRawSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        color = ObsidianSurface,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, CopilotEmerald.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "RAW SECONDARY EXTRACT (https://copilot.microsoft.com/search?q=%s)",
                                style = MaterialTheme.typography.labelSmall,
                                color = CopilotEmerald
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = secondaryRawSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CitationSourceCard(
    citation: CollectedCitation,
    onClick: () -> Unit
) {
    val accent = when (citation.origin) {
        EngineOrigin.PRIMARY_GEMINI -> GeminiCyan
        EngineOrigin.SECONDARY_COPILOT -> CopilotEmerald
        EngineOrigin.CONSENSUS_BOTH -> BestValueGold
    }

    Surface(
        color = ObsidianSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = citation.origin.shortTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = citation.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = citation.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = citation.url,
                    style = MaterialTheme.typography.labelSmall,
                    color = accent.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open in Reader",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun FollowUpQueriesSection(
    queries: List<String>,
    onFollowUpClicked: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeaderRow(
            title = stringResource(R.string.section_followup),
            subtitle = "Tap any follow-up to simultaneously query Gemini & Copilot",
            accentColor = CopilotEmerald
        )
        queries.forEach { followUp ->
            Surface(
                color = ObsidianSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                    .clickable { onFollowUpClicked(followUp) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = BestValueGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = followUp,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Dual Search →",
                        style = MaterialTheme.typography.labelMedium,
                        color = GeminiCyan
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    subtitle: String,
    accentColor: Color
) {
    Column(modifier = Modifier.padding(top = 4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = accentColor
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
