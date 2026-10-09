package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.BrowserViewMode
import com.example.model.MainNavTab
import com.example.model.SynthesizedResult
import com.example.ui.components.BackgroundDualWebViewCollector
import com.example.ui.components.BestValueSynthesisView
import com.example.ui.components.DualLiveStreamsScreen
import com.example.ui.components.EngineConfigurationScreen
import com.example.ui.components.SavedVaultAndHistoryScreen
import com.example.ui.components.SingleWebReaderScreen
import com.example.ui.theme.BestValueGold
import com.example.ui.theme.CopilotEmerald
import com.example.ui.theme.GeminiCyan
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBackground
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceElevated
import com.example.viewmodel.SynapseBrowserViewModel
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) {
                SynapseBrowserApp()
            }
        }
    }
}

@Composable
fun SynapseBrowserApp(
    viewModel: SynapseBrowserViewModel = viewModel()
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val queryInput by viewModel.queryInput.collectAsStateWithLifecycle()
    val isSynthesizing by viewModel.isSynthesizing.collectAsStateWithLifecycle()
    val currentResult by viewModel.currentResult.collectAsStateWithLifecycle()
    val liveStatus by viewModel.liveStreamStatus.collectAsStateWithLifecycle()
    val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
    val browserViewMode by viewModel.browserViewMode.collectAsStateWithLifecycle()
    val activeReaderUrl by viewModel.activeReaderUrl.collectAsStateWithLifecycle()
    val arbitrationFocus by viewModel.arbitrationFocus.collectAsStateWithLifecycle()
    val primaryTemplate by viewModel.primaryTemplate.collectAsStateWithLifecycle()
    val secondaryTemplate by viewModel.secondaryTemplate.collectAsStateWithLifecycle()
    val primaryWeightPct by viewModel.primaryWeightPct.collectAsStateWithLifecycle()
    val allSessions by viewModel.allSessions.collectAsStateWithLifecycle()
    val bookmarkedSessions by viewModel.bookmarkedSessions.collectAsStateWithLifecycle()

    // Native TextToSpeech for listening to synthesized briefs
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsEngine by remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                instance?.language = Locale.US
            }
        }
        ttsEngine = instance
        onDispose {
            instance?.stop()
            instance?.shutdown()
        }
    }

    // Back navigation handler for sub-modes and secondary tabs
    BackHandler(
        enabled = activeTab != MainNavTab.SYNTHESIZE || browserViewMode != BrowserViewMode.UNIFIED_BEST_VALUE
    ) {
        if (browserViewMode == BrowserViewMode.WEB_READER || browserViewMode == BrowserViewMode.DUAL_LIVE_STREAMS) {
            viewModel.setBrowserViewMode(BrowserViewMode.UNIFIED_BEST_VALUE)
            viewModel.selectMainTab(MainNavTab.SYNTHESIZE)
        } else {
            viewModel.selectMainTab(MainNavTab.SYNTHESIZE)
        }
    }

    // Keep background dual WebView streams active when not in the split Dual Live screen
    if (activeTab != MainNavTab.DUAL_STREAMS && browserViewMode != BrowserViewMode.DUAL_LIVE_STREAMS) {
        BackgroundDualWebViewCollector(
            primaryUrl = liveStatus.primaryUrl,
            secondaryUrl = liveStatus.secondaryUrl,
            onPrimaryProgress = viewModel::onPrimaryWebViewProgress,
            onSecondaryProgress = viewModel::onSecondaryWebViewProgress,
            onPrimaryDomCaptured = viewModel::onPrimaryWebViewDomCaptured,
            onSecondaryDomCaptured = viewModel::onSecondaryWebViewDomCaptured
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        val useNavRail = maxWidth >= 720.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            containerColor = ObsidianBackground,
            bottomBar = {
                if (!useNavRail) {
                    SynapseBottomNavigationBar(
                        activeTab = activeTab,
                        savedCount = bookmarkedSessions.size,
                        onSelectTab = viewModel::selectMainTab
                    )
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    SynapseNavigationRail(
                        activeTab = activeTab,
                        savedCount = bookmarkedSessions.size,
                        onSelectTab = viewModel::selectMainTab
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    // Persistent Dual-Engine Omnibox Header
                    SynapseOmniboxCommandBar(
                        queryInput = queryInput,
                        isSynthesizing = isSynthesizing,
                        browserViewMode = browserViewMode,
                        onQueryChange = viewModel::onQueryInputChange,
                        onSearchTriggered = {
                            focusManager.clearFocus()
                            viewModel.executeDualSearch(queryInput)
                        },
                        onQuickTopicSelected = { topic ->
                            focusManager.clearFocus()
                            viewModel.executeDualSearch(topic)
                        },
                        onModeSelected = { mode ->
                            viewModel.setBrowserViewMode(mode)
                        }
                    )

                    // Main Content Area
                    Box(modifier = Modifier.weight(1f)) {
                        when (activeTab) {
                            MainNavTab.SYNTHESIZE -> {
                                when (browserViewMode) {
                                    BrowserViewMode.UNIFIED_BEST_VALUE -> {
                                        BestValueSynthesisView(
                                            result = currentResult,
                                            liveStatus = liveStatus,
                                            isSynthesizing = isSynthesizing,
                                            selectedFocus = arbitrationFocus,
                                            isSpeaking = isSpeaking,
                                            onFocusSelected = viewModel::onArbitrationFocusChanged,
                                            onOpenDualLiveStreams = {
                                                viewModel.selectMainTab(MainNavTab.DUAL_STREAMS)
                                            },
                                            onOpenCitationUrl = viewModel::openUrlInReader,
                                            onFollowUpClicked = { followUp ->
                                                focusManager.clearFocus()
                                                viewModel.executeDualSearch(followUp)
                                            },
                                            onToggleBookmark = viewModel::toggleBookmarkCurrent,
                                            onToggleSpeak = {
                                                val res = currentResult ?: return@BestValueSynthesisView
                                                if (isSpeaking) {
                                                    ttsEngine?.stop()
                                                    isSpeaking = false
                                                } else {
                                                    val speechText = buildString {
                                                        append("Synthesized best value brief for ${res.query}. ")
                                                        append(res.executiveSummary)
                                                        res.takeaways.take(3).forEachIndexed { idx, t ->
                                                            append(" Key takeaway ${idx + 1}: ${t.title}. ${t.detail}")
                                                        }
                                                    }
                                                    ttsEngine?.speak(
                                                        speechText,
                                                        TextToSpeech.QUEUE_FLUSH,
                                                        null,
                                                        "synapse_brief"
                                                    )
                                                    isSpeaking = true
                                                }
                                            },
                                            onCopyBrief = {
                                                currentResult?.let { copySynthesisToClipboard(context, it) }
                                            },
                                            onShareBrief = {
                                                currentResult?.let { shareSynthesisBrief(context, it) }
                                            },
                                            onReArbitrate = viewModel::reArbitrateWithLiveDom
                                        )
                                    }

                                    BrowserViewMode.DUAL_LIVE_STREAMS -> {
                                        DualLiveStreamsScreen(
                                            liveStatus = liveStatus,
                                            isSynthesizing = isSynthesizing,
                                            onPrimaryProgress = viewModel::onPrimaryWebViewProgress,
                                            onSecondaryProgress = viewModel::onSecondaryWebViewProgress,
                                            onPrimaryDomCaptured = viewModel::onPrimaryWebViewDomCaptured,
                                            onSecondaryDomCaptured = viewModel::onSecondaryWebViewDomCaptured,
                                            onReArbitrateLiveDom = viewModel::reArbitrateWithLiveDom,
                                            onReturnToSynthesis = {
                                                viewModel.setBrowserViewMode(BrowserViewMode.UNIFIED_BEST_VALUE)
                                            }
                                        )
                                    }

                                    BrowserViewMode.WEB_READER -> {
                                        SingleWebReaderScreen(
                                            url = activeReaderUrl,
                                            onCloseReader = {
                                                viewModel.setBrowserViewMode(BrowserViewMode.UNIFIED_BEST_VALUE)
                                            },
                                            onDistillUrlAsQuery = { readerUrl ->
                                                viewModel.executeDualSearch("Key valuable insights from $readerUrl")
                                            }
                                        )
                                    }
                                }
                            }

                            MainNavTab.DUAL_STREAMS -> {
                                DualLiveStreamsScreen(
                                    liveStatus = liveStatus,
                                    isSynthesizing = isSynthesizing,
                                    onPrimaryProgress = viewModel::onPrimaryWebViewProgress,
                                    onSecondaryProgress = viewModel::onSecondaryWebViewProgress,
                                    onPrimaryDomCaptured = viewModel::onPrimaryWebViewDomCaptured,
                                    onSecondaryDomCaptured = viewModel::onSecondaryWebViewDomCaptured,
                                    onReArbitrateLiveDom = viewModel::reArbitrateWithLiveDom,
                                    onReturnToSynthesis = {
                                        viewModel.selectMainTab(MainNavTab.SYNTHESIZE)
                                    }
                                )
                            }

                            MainNavTab.SAVED_VAULT -> {
                                SavedVaultAndHistoryScreen(
                                    allSessions = allSessions,
                                    bookmarkedSessions = bookmarkedSessions,
                                    onSelectSession = viewModel::selectSession,
                                    onToggleBookmark = viewModel::toggleBookmarkSession,
                                    onDeleteSession = viewModel::deleteSession,
                                    onClearHistory = viewModel::clearUnbookmarkedHistory
                                )
                            }

                            MainNavTab.ENGINE_CONFIG -> {
                                EngineConfigurationScreen(
                                    primaryTemplate = primaryTemplate,
                                    secondaryTemplate = secondaryTemplate,
                                    primaryWeightPct = primaryWeightPct,
                                    defaultFocus = arbitrationFocus,
                                    onSaveConfig = viewModel::updateEngineConfig,
                                    onResetDefaults = viewModel::resetEngineDefaults
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SynapseOmniboxCommandBar(
    queryInput: String,
    isSynthesizing: Boolean,
    browserViewMode: BrowserViewMode,
    onQueryChange: (String) -> Unit,
    onSearchTriggered: () -> Unit,
    onQuickTopicSelected: (String) -> Unit,
    onModeSelected: (BrowserViewMode) -> Unit
) {
    val quickPrompts = remember {
        listOf(
            "Solid-state battery EV commercialization timeline 2026-2028",
            "Kotlin Multiplatform vs Flutter performance and native UI sharing in 2026",
            "On-device LLM quantization GGUF vs AWQ memory bandwidth",
            "Zero-trust passkeys vs hardware FIDO2 security keys enterprise rollout",
            "CRISPR base editing vs prime editing clinical milestones"
        )
    }

    Surface(
        color = ObsidianSurface,
        modifier = Modifier
            .fillMaxWidth()
            .border(width = 0.5.dp, color = GlassBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Top Identity Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(GeminiCyan, CopilotEmerald))
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYNAPSE DUAL-ENGINE BROWSER",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Mode Switcher Pills (Unified Best Value vs Dual Live Split)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModeTogglePill(
                        label = stringResource(R.string.mode_best_value),
                        selected = browserViewMode == BrowserViewMode.UNIFIED_BEST_VALUE,
                        accentColor = BestValueGold,
                        onClick = { onModeSelected(BrowserViewMode.UNIFIED_BEST_VALUE) },
                        testTag = "mode_pill_best_value"
                    )
                    ModeTogglePill(
                        label = stringResource(R.string.mode_dual_live),
                        selected = browserViewMode == BrowserViewMode.DUAL_LIVE_STREAMS,
                        accentColor = GeminiCyan,
                        onClick = { onModeSelected(BrowserViewMode.DUAL_LIVE_STREAMS) },
                        testTag = "mode_pill_dual_live"
                    )
                }
            }

            // Unified Omnibox Input + Fuse Action Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = queryInput,
                    onValueChange = onQueryChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.omnibox_placeholder),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search Icon",
                            tint = GeminiCyan
                        )
                    },
                    trailingIcon = {
                        if (queryInput.isNotEmpty()) {
                            IconButton(
                                onClick = { onQueryChange("") },
                                modifier = Modifier.testTag("clear_omnibox_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear Query",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearchTriggered() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = ObsidianSurfaceElevated,
                        unfocusedContainerColor = ObsidianSurfaceElevated,
                        focusedBorderColor = GeminiCyan,
                        unfocusedBorderColor = GlassBorder
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("omnibox_search_input")
                )

                Button(
                    onClick = onSearchTriggered,
                    enabled = !isSynthesizing && queryInput.isNotBlank(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GeminiCyan,
                        contentColor = Color(0xFF001E30)
                    ),
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("omnibox_search_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Fuse",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Quick Topic Discovery Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickPrompts.forEachIndexed { index, prompt ->
                    Surface(
                        color = ObsidianSurfaceElevated,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .border(1.dp, GlassBorder, RoundedCornerShape(50))
                            .clickable { onQuickTopicSelected(prompt) }
                            .testTag("quick_prompt_$index")
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeTogglePill(
    label: String,
    selected: Boolean,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        color = if (selected) accentColor.copy(alpha = 0.2f) else ObsidianSurfaceElevated,
        shape = RoundedCornerShape(50),
        modifier = Modifier
            .border(
                width = 1.dp,
                color = if (selected) accentColor else GlassBorder,
                shape = RoundedCornerShape(50)
            )
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun SynapseBottomNavigationBar(
    activeTab: MainNavTab,
    savedCount: Int,
    onSelectTab: (MainNavTab) -> Unit
) {
    NavigationBar(
        containerColor = ObsidianSurface,
        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
    ) {
        NavigationBarItem(
            selected = activeTab == MainNavTab.SYNTHESIZE,
            onClick = { onSelectTab(MainNavTab.SYNTHESIZE) },
            icon = {
                Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = "Synthesize")
            },
            label = { Text(stringResource(R.string.nav_discover)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GeminiCyan,
                selectedTextColor = GeminiCyan,
                indicatorColor = GeminiCyan.copy(alpha = 0.16f)
            ),
            modifier = Modifier.testTag("nav_tab_synthesize")
        )
        NavigationBarItem(
            selected = activeTab == MainNavTab.DUAL_STREAMS,
            onClick = { onSelectTab(MainNavTab.DUAL_STREAMS) },
            icon = {
                Icon(imageVector = Icons.Default.ViewColumn, contentDescription = "Dual Live Streams")
            },
            label = { Text(stringResource(R.string.nav_streams)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = CopilotEmerald,
                selectedTextColor = CopilotEmerald,
                indicatorColor = CopilotEmerald.copy(alpha = 0.16f)
            ),
            modifier = Modifier.testTag("nav_tab_streams")
        )
        NavigationBarItem(
            selected = activeTab == MainNavTab.SAVED_VAULT,
            onClick = { onSelectTab(MainNavTab.SAVED_VAULT) },
            icon = {
                Icon(imageVector = Icons.Default.Bookmark, contentDescription = "Saved Vault")
            },
            label = {
                Text(
                    if (savedCount > 0) "${stringResource(R.string.nav_vault)} ($savedCount)"
                    else stringResource(R.string.nav_vault)
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = BestValueGold,
                selectedTextColor = BestValueGold,
                indicatorColor = BestValueGold.copy(alpha = 0.16f)
            ),
            modifier = Modifier.testTag("nav_tab_vault")
        )
        NavigationBarItem(
            selected = activeTab == MainNavTab.ENGINE_CONFIG,
            onClick = { onSelectTab(MainNavTab.ENGINE_CONFIG) },
            icon = {
                Icon(imageVector = Icons.Default.Tune, contentDescription = "Engines")
            },
            label = { Text(stringResource(R.string.nav_engines)) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = GeminiCyan,
                selectedTextColor = GeminiCyan,
                indicatorColor = GeminiCyan.copy(alpha = 0.16f)
            ),
            modifier = Modifier.testTag("nav_tab_engines")
        )
    }
}

@Composable
private fun SynapseNavigationRail(
    activeTab: MainNavTab,
    savedCount: Int,
    onSelectTab: (MainNavTab) -> Unit
) {
    NavigationRail(
        containerColor = ObsidianSurface,
        modifier = Modifier.fillMaxHeight()
    ) {
        NavigationRailItem(
            selected = activeTab == MainNavTab.SYNTHESIZE,
            onClick = { onSelectTab(MainNavTab.SYNTHESIZE) },
            icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "Synthesize") },
            label = { Text(stringResource(R.string.nav_discover)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = GeminiCyan,
                selectedTextColor = GeminiCyan,
                indicatorColor = GeminiCyan.copy(alpha = 0.16f)
            )
        )
        NavigationRailItem(
            selected = activeTab == MainNavTab.DUAL_STREAMS,
            onClick = { onSelectTab(MainNavTab.DUAL_STREAMS) },
            icon = { Icon(Icons.Default.ViewColumn, contentDescription = "Dual Live") },
            label = { Text(stringResource(R.string.nav_streams)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = CopilotEmerald,
                selectedTextColor = CopilotEmerald,
                indicatorColor = CopilotEmerald.copy(alpha = 0.16f)
            )
        )
        NavigationRailItem(
            selected = activeTab == MainNavTab.SAVED_VAULT,
            onClick = { onSelectTab(MainNavTab.SAVED_VAULT) },
            icon = { Icon(Icons.Default.Bookmark, contentDescription = "Vault") },
            label = {
                Text(
                    if (savedCount > 0) "Vault ($savedCount)"
                    else stringResource(R.string.nav_vault)
                )
            },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = BestValueGold,
                selectedTextColor = BestValueGold,
                indicatorColor = BestValueGold.copy(alpha = 0.16f)
            )
        )
        NavigationRailItem(
            selected = activeTab == MainNavTab.ENGINE_CONFIG,
            onClick = { onSelectTab(MainNavTab.ENGINE_CONFIG) },
            icon = { Icon(Icons.Default.Tune, contentDescription = "Engines") },
            label = { Text(stringResource(R.string.nav_engines)) },
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = GeminiCyan,
                selectedTextColor = GeminiCyan,
                indicatorColor = GeminiCyan.copy(alpha = 0.16f)
            )
        )
    }
}

private fun formatBriefAsMarkdown(result: SynthesizedResult): String {
    return buildString {
        appendLine("# ${result.query}")
        appendLine("Synthesized by Synapse Browser (${result.overallConfidenceScore}% Confidence)")
        appendLine("- Primary Engine: ${result.primaryUrl} (${result.primaryContributionPct}%)")
        appendLine("- Secondary Engine: ${result.secondaryUrl} (${result.secondaryContributionPct}%)")
        appendLine()
        appendLine("## Distilled Best-Value Summary")
        appendLine(result.executiveSummary)
        appendLine()
        appendLine("## Value Arbitration Verdict")
        appendLine(result.valueArbitrationVerdict)
        appendLine()
        appendLine("## Verified High-Value Takeaways")
        result.takeaways.forEach { t ->
            appendLine("- **[${t.origin.shortTag} • ${t.valueScore}/100] ${t.title}**: ${t.detail}")
        }
    }
}

private fun copySynthesisToClipboard(context: Context, result: SynthesizedResult) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Synapse Synthesis Brief", formatBriefAsMarkdown(result))
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Synthesized brief copied to clipboard", Toast.LENGTH_SHORT).show()
}

private fun shareSynthesisBrief(context: Context, result: SynthesizedResult) {
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Synapse Dual-Search: ${result.query}")
        putExtra(Intent.EXTRA_TEXT, formatBriefAsMarkdown(result))
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Synthesized Intelligence"))
}
