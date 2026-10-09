package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class EngineOrigin(val displayName: String, val shortTag: String) {
    PRIMARY_GEMINI("Primary: Gemini Search", "GEMINI"),
    SECONDARY_COPILOT("Secondary: Copilot Search", "COPILOT"),
    CONSENSUS_BOTH("Dual Consensus (Both)", "CONSENSUS")
}

@Serializable
enum class ArbitrationFocus(
    val title: String,
    val subtitle: String,
    val promptDirective: String
) {
    BALANCED_VALUE(
        title = "Highest Overall Value",
        subtitle = "Balances factual consensus, depth, and immediate utility",
        promptDirective = "Prioritize the most actionable, accurate, and high-signal facts verified across both streams."
    ),
    TECHNICAL_DEPTH(
        title = "Technical & Architectural Depth",
        subtitle = "Prioritizes specs, benchmarks, code patterns, and root causes",
        promptDirective = "Prioritize deep technical details, specifications, architectural tradeoffs, and quantitative metrics."
    ),
    CONSENSUS_VERIFIED(
        title = "Strict Cross-Verified Consensus",
        subtitle = "Highlights claims corroborated by both Gemini and Copilot streams",
        promptDirective = "Prioritize facts where Primary Gemini and Secondary Copilot agree, explicitly flagging any divergence."
    ),
    ACTIONABLE_BRIEF(
        title = "Executive Action Brief",
        subtitle = "Distills bottom-line decisions and step-by-step execution",
        promptDirective = "Distill into an ultra-concise executive summary and immediate step-by-step actions."
    ),
    DECISION_MATRIX(
        title = "Pros, Cons & Tradeoff Matrix",
        subtitle = "Compares competing options and surfaces hidden risks",
        promptDirective = "Structure the valuable information around comparative strengths, limitations, and decision criteria."
    )
}

@Serializable
data class ValuableTakeaway(
    val title: String,
    val detail: String,
    val origin: EngineOrigin,
    val valueScore: Int,
    val whyValuable: String
)

@Serializable
data class StreamComparisonPoint(
    val dimension: String,
    val primaryGeminiPoint: String,
    val secondaryCopilotPoint: String,
    val winner: EngineOrigin
)

@Serializable
data class CollectedCitation(
    val title: String,
    val url: String,
    val snippet: String,
    val origin: EngineOrigin,
    val relevanceScore: Int
)

@Serializable
data class SynthesizedResult(
    val id: Long = 0L,
    val query: String,
    val primaryUrl: String,
    val secondaryUrl: String,
    val arbitrationFocus: ArbitrationFocus = ArbitrationFocus.BALANCED_VALUE,
    val executiveSummary: String,
    val valueArbitrationVerdict: String,
    val overallConfidenceScore: Int,
    val primaryContributionPct: Int,
    val secondaryContributionPct: Int,
    val primaryLatencyMs: Long,
    val secondaryLatencyMs: Long,
    val primaryRawSummary: String,
    val secondaryRawSummary: String,
    val takeaways: List<ValuableTakeaway>,
    val comparisonPoints: List<StreamComparisonPoint>,
    val citations: List<CollectedCitation>,
    val followUpQueries: List<String>,
    val isBookmarked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class LiveStreamStatus(
    val primaryUrl: String = "https://gemini.google.com/search?q=",
    val secondaryUrl: String = "https://copilot.microsoft.com/search?q=",
    val primaryLoading: Boolean = false,
    val secondaryLoading: Boolean = false,
    val primaryProgress: Int = 0,
    val secondaryProgress: Int = 0,
    val primaryDomCharsExtracted: Int = 0,
    val secondaryDomCharsExtracted: Int = 0,
    val primaryExtractedSnippet: String = "",
    val secondaryExtractedSnippet: String = "",
    val stageMessage: String = "Ready to query Primary (Gemini) & Secondary (Copilot) simultaneously"
)

enum class BrowserViewMode {
    UNIFIED_BEST_VALUE,
    DUAL_LIVE_STREAMS,
    WEB_READER
}

enum class MainNavTab {
    SYNTHESIZE,
    DUAL_STREAMS,
    SAVED_VAULT,
    ENGINE_CONFIG
}
