package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.SynapseDatabase
import com.example.data.local.SynapseRepository
import com.example.data.remote.DualEngineCollector
import com.example.model.ArbitrationFocus
import com.example.model.BrowserViewMode
import com.example.model.LiveStreamStatus
import com.example.model.MainNavTab
import com.example.model.SynthesizedResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SynapseBrowserViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val DEFAULT_PRIMARY_TEMPLATE = "https://gemini.google.com/search?q=%s"
        const val DEFAULT_SECONDARY_TEMPLATE = "https://copilot.microsoft.com/search?q=%s"
    }

    private val database = SynapseDatabase.getInstance(application)
    private val repository = SynapseRepository(database.searchSessionDao())
    private val collector = DualEngineCollector()

    val allSessions: StateFlow<List<SynthesizedResult>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedSessions: StateFlow<List<SynthesizedResult>> = repository.bookmarkedSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _primaryTemplate = MutableStateFlow(DEFAULT_PRIMARY_TEMPLATE)
    val primaryTemplate: StateFlow<String> = _primaryTemplate.asStateFlow()

    private val _secondaryTemplate = MutableStateFlow(DEFAULT_SECONDARY_TEMPLATE)
    val secondaryTemplate: StateFlow<String> = _secondaryTemplate.asStateFlow()

    private val _arbitrationFocus = MutableStateFlow(ArbitrationFocus.BALANCED_VALUE)
    val arbitrationFocus: StateFlow<ArbitrationFocus> = _arbitrationFocus.asStateFlow()

    private val _primaryWeightPct = MutableStateFlow(55)
    val primaryWeightPct: StateFlow<Int> = _primaryWeightPct.asStateFlow()

    private val _queryInput = MutableStateFlow("")
    val queryInput: StateFlow<String> = _queryInput.asStateFlow()

    private val _isSynthesizing = MutableStateFlow(false)
    val isSynthesizing: StateFlow<Boolean> = _isSynthesizing.asStateFlow()

    private val _currentResult = MutableStateFlow<SynthesizedResult?>(null)
    val currentResult: StateFlow<SynthesizedResult?> = _currentResult.asStateFlow()

    private val _liveStreamStatus = MutableStateFlow(LiveStreamStatus())
    val liveStreamStatus: StateFlow<LiveStreamStatus> = _liveStreamStatus.asStateFlow()

    private val _activeTab = MutableStateFlow(MainNavTab.SYNTHESIZE)
    val activeTab: StateFlow<MainNavTab> = _activeTab.asStateFlow()

    private val _browserViewMode = MutableStateFlow(BrowserViewMode.UNIFIED_BEST_VALUE)
    val browserViewMode: StateFlow<BrowserViewMode> = _browserViewMode.asStateFlow()

    private val _activeReaderUrl = MutableStateFlow("https://gemini.google.com/search?q=Solid-state+battery+EV+commercialization+timeline+2026-2028")
    val activeReaderUrl: StateFlow<String> = _activeReaderUrl.asStateFlow()

    private var latestPrimaryDomText: String = ""
    private var latestSecondaryDomText: String = ""
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            repository.seedInitialSessionsIfEmpty()
            val initialSeeds = SynapseRepository.buildSeedSyntheses()
            if (_currentResult.value == null && initialSeeds.isNotEmpty()) {
                val first = initialSeeds.first()
                _currentResult.value = first
                _queryInput.value = first.query
                _liveStreamStatus.update {
                    it.copy(
                        primaryUrl = first.primaryUrl,
                        secondaryUrl = first.secondaryUrl,
                        primaryProgress = 100,
                        secondaryProgress = 100,
                        primaryDomCharsExtracted = first.primaryRawSummary.length * 6,
                        secondaryDomCharsExtracted = first.secondaryRawSummary.length * 6,
                        primaryExtractedSnippet = first.primaryRawSummary,
                        secondaryExtractedSnippet = first.secondaryRawSummary,
                        stageMessage = "Synthesized Primary (Gemini) & Secondary (Copilot) into Best-Value Brief"
                    )
                }
            }
        }
    }

    fun onQueryInputChange(newQuery: String) {
        _queryInput.value = newQuery
    }

    fun selectMainTab(tab: MainNavTab) {
        _activeTab.value = tab
        if (tab == MainNavTab.DUAL_STREAMS) {
            _browserViewMode.value = BrowserViewMode.DUAL_LIVE_STREAMS
        } else if (tab == MainNavTab.SYNTHESIZE && _browserViewMode.value == BrowserViewMode.DUAL_LIVE_STREAMS) {
            _browserViewMode.value = BrowserViewMode.UNIFIED_BEST_VALUE
        }
    }

    fun setBrowserViewMode(mode: BrowserViewMode) {
        _browserViewMode.value = mode
        if (mode == BrowserViewMode.DUAL_LIVE_STREAMS) {
            _activeTab.value = MainNavTab.DUAL_STREAMS
        } else if (mode == BrowserViewMode.UNIFIED_BEST_VALUE) {
            _activeTab.value = MainNavTab.SYNTHESIZE
        }
    }

    fun executeDualSearch(
        rawQuery: String = _queryInput.value,
        focusOverride: ArbitrationFocus? = null
    ) {
        val cleanQuery = rawQuery.trim()
        if (cleanQuery.isEmpty()) return

        _queryInput.value = cleanQuery
        if (focusOverride != null) {
            _arbitrationFocus.value = focusOverride
        }

        val pUrl = collector.formatEngineUrl(_primaryTemplate.value, cleanQuery)
        val sUrl = collector.formatEngineUrl(_secondaryTemplate.value, cleanQuery)

        latestPrimaryDomText = ""
        latestSecondaryDomText = ""

        _liveStreamStatus.update {
            it.copy(
                primaryUrl = pUrl,
                secondaryUrl = sUrl,
                primaryLoading = true,
                secondaryLoading = true,
                primaryProgress = 15,
                secondaryProgress = 15,
                stageMessage = "Pulling Primary (Gemini) & Secondary (Copilot) simultaneously…"
            )
        }

        if (_activeTab.value != MainNavTab.DUAL_STREAMS) {
            _activeTab.value = MainNavTab.SYNTHESIZE
            _browserViewMode.value = BrowserViewMode.UNIFIED_BEST_VALUE
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSynthesizing.value = true
            try {
                val synthesized = collector.collectAndSynthesize(
                    query = cleanQuery,
                    primaryTemplate = _primaryTemplate.value,
                    secondaryTemplate = _secondaryTemplate.value,
                    arbitrationFocus = _arbitrationFocus.value,
                    primaryWeightPct = _primaryWeightPct.value,
                    livePrimaryDomText = latestPrimaryDomText,
                    liveSecondaryDomText = latestSecondaryDomText,
                    onStatusUpdate = { msg ->
                        _liveStreamStatus.update { state -> state.copy(stageMessage = msg) }
                    }
                )
                val saved = repository.saveResult(synthesized)
                _currentResult.value = saved
                _liveStreamStatus.update {
                    it.copy(
                        primaryLoading = false,
                        secondaryLoading = false,
                        primaryProgress = 100,
                        secondaryProgress = 100,
                        primaryDomCharsExtracted = it.primaryDomCharsExtracted.coerceAtLeast(saved.primaryRawSummary.length * 4),
                        secondaryDomCharsExtracted = it.secondaryDomCharsExtracted.coerceAtLeast(saved.secondaryRawSummary.length * 4),
                        primaryExtractedSnippet = saved.primaryRawSummary,
                        secondaryExtractedSnippet = saved.secondaryRawSummary,
                        stageMessage = "Distilled best valuable information from Gemini (${saved.primaryContributionPct}%) & Copilot (${saved.secondaryContributionPct}%)"
                    )
                }
            } finally {
                _isSynthesizing.value = false
            }
        }
    }

    fun onArbitrationFocusChanged(newFocus: ArbitrationFocus) {
        if (_arbitrationFocus.value == newFocus && !_isSynthesizing.value) return
        _arbitrationFocus.value = newFocus
        val activeQuery = _currentResult.value?.query ?: _queryInput.value
        if (activeQuery.isNotBlank()) {
            executeDualSearch(rawQuery = activeQuery, focusOverride = newFocus)
        }
    }

    fun onPrimaryWebViewProgress(progress: Int, loading: Boolean) {
        _liveStreamStatus.update {
            it.copy(
                primaryProgress = progress.coerceIn(0, 100),
                primaryLoading = loading
            )
        }
    }

    fun onSecondaryWebViewProgress(progress: Int, loading: Boolean) {
        _liveStreamStatus.update {
            it.copy(
                secondaryProgress = progress.coerceIn(0, 100),
                secondaryLoading = loading
            )
        }
    }

    fun onPrimaryWebViewDomCaptured(domText: String) {
        val cleaned = domText.trim()
        if (cleaned.length < 25) return
        latestPrimaryDomText = cleaned
        _liveStreamStatus.update {
            it.copy(
                primaryDomCharsExtracted = cleaned.length,
                primaryExtractedSnippet = cleaned.take(280)
            )
        }
    }

    fun onSecondaryWebViewDomCaptured(domText: String) {
        val cleaned = domText.trim()
        if (cleaned.length < 25) return
        latestSecondaryDomText = cleaned
        _liveStreamStatus.update {
            it.copy(
                secondaryDomCharsExtracted = cleaned.length,
                secondaryExtractedSnippet = cleaned.take(280)
            )
        }
    }

    fun reArbitrateWithLiveDom() {
        val query = _currentResult.value?.query ?: _queryInput.value.ifBlank { "AI search synthesis" }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isSynthesizing.value = true
            _liveStreamStatus.update {
                it.copy(stageMessage = "Re-arbitrating with live WebView DOM streams from Gemini & Copilot…")
            }
            try {
                val synthesized = collector.collectAndSynthesize(
                    query = query,
                    primaryTemplate = _primaryTemplate.value,
                    secondaryTemplate = _secondaryTemplate.value,
                    arbitrationFocus = _arbitrationFocus.value,
                    primaryWeightPct = _primaryWeightPct.value,
                    livePrimaryDomText = latestPrimaryDomText,
                    liveSecondaryDomText = latestSecondaryDomText,
                    onStatusUpdate = { msg ->
                        _liveStreamStatus.update { s -> s.copy(stageMessage = msg) }
                    }
                )
                val saved = repository.saveResult(synthesized)
                _currentResult.value = saved
                _activeTab.value = MainNavTab.SYNTHESIZE
                _browserViewMode.value = BrowserViewMode.UNIFIED_BEST_VALUE
            } finally {
                _isSynthesizing.value = false
            }
        }
    }

    fun selectSession(session: SynthesizedResult) {
        _currentResult.value = session
        _queryInput.value = session.query
        _arbitrationFocus.value = session.arbitrationFocus
        _liveStreamStatus.update {
            it.copy(
                primaryUrl = session.primaryUrl,
                secondaryUrl = session.secondaryUrl,
                primaryProgress = 100,
                secondaryProgress = 100,
                primaryExtractedSnippet = session.primaryRawSummary,
                secondaryExtractedSnippet = session.secondaryRawSummary,
                stageMessage = "Loaded saved dual-engine synthesis from Vault"
            )
        }
        _activeTab.value = MainNavTab.SYNTHESIZE
        _browserViewMode.value = BrowserViewMode.UNIFIED_BEST_VALUE
    }

    fun toggleBookmarkCurrent() {
        val current = _currentResult.value ?: return
        viewModelScope.launch {
            val updated = repository.toggleBookmark(current)
            _currentResult.value = updated
        }
    }

    fun toggleBookmarkSession(session: SynthesizedResult) {
        viewModelScope.launch {
            val updated = repository.toggleBookmark(session)
            if (_currentResult.value?.id == session.id) {
                _currentResult.value = updated
            }
        }
    }

    fun deleteSession(id: Long) {
        viewModelScope.launch {
            repository.deleteSession(id)
        }
    }

    fun clearUnbookmarkedHistory() {
        viewModelScope.launch {
            repository.clearHistoryKeepBookmarks()
        }
    }

    fun openUrlInReader(url: String) {
        _activeReaderUrl.value = url
        _browserViewMode.value = BrowserViewMode.WEB_READER
        _activeTab.value = MainNavTab.SYNTHESIZE
    }

    fun updateEngineConfig(
        primaryTemplate: String,
        secondaryTemplate: String,
        primaryWeight: Int,
        focus: ArbitrationFocus
    ) {
        _primaryTemplate.value = primaryTemplate.trim().ifBlank { DEFAULT_PRIMARY_TEMPLATE }
        _secondaryTemplate.value = secondaryTemplate.trim().ifBlank { DEFAULT_SECONDARY_TEMPLATE }
        _primaryWeightPct.value = primaryWeight.coerceIn(20, 80)
        _arbitrationFocus.value = focus
    }

    fun resetEngineDefaults() {
        _primaryTemplate.value = DEFAULT_PRIMARY_TEMPLATE
        _secondaryTemplate.value = DEFAULT_SECONDARY_TEMPLATE
        _primaryWeightPct.value = 55
        _arbitrationFocus.value = ArbitrationFocus.BALANCED_VALUE
    }
}
