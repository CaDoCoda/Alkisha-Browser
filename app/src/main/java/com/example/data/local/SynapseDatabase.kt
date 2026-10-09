package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.model.ArbitrationFocus
import com.example.model.CollectedCitation
import com.example.model.EngineOrigin
import com.example.model.StreamComparisonPoint
import com.example.model.SynthesizedResult
import com.example.model.ValuableTakeaway
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "search_sessions")
data class SearchSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val query: String,
    val primaryUrl: String,
    val secondaryUrl: String,
    val payloadJson: String,
    val isBookmarked: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface SearchSessionDao {
    @Query("SELECT * FROM search_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SearchSessionEntity>>

    @Query("SELECT * FROM search_sessions WHERE isBookmarked = 1 ORDER BY timestamp DESC")
    fun getBookmarkedSessions(): Flow<List<SearchSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SearchSessionEntity): Long

    @Query("UPDATE search_sessions SET isBookmarked = :bookmarked WHERE id = :id")
    suspend fun updateBookmark(id: Long, bookmarked: Boolean)

    @Query("DELETE FROM search_sessions WHERE id = :id")
    suspend fun deleteSessionById(id: Long)

    @Query("DELETE FROM search_sessions WHERE isBookmarked = 0")
    suspend fun clearNonBookmarkedHistory()

    @Query("SELECT COUNT(*) FROM search_sessions")
    suspend fun countSessions(): Int
}

@Database(entities = [SearchSessionEntity::class], version = 1, exportSchema = false)
abstract class SynapseDatabase : RoomDatabase() {
    abstract fun searchSessionDao(): SearchSessionDao

    companion object {
        @Volatile
        private var INSTANCE: SynapseDatabase? = null

        fun getInstance(context: Context): SynapseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SynapseDatabase::class.java,
                    "synapse_dual_browser.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class SynapseRepository(private val dao: SearchSessionDao) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val allSessions: Flow<List<SynthesizedResult>> = dao.getAllSessions().map { list ->
        list.mapNotNull { entity ->
            runCatching {
                json.decodeFromString<SynthesizedResult>(entity.payloadJson).copy(
                    id = entity.id,
                    isBookmarked = entity.isBookmarked,
                    timestamp = entity.timestamp
                )
            }.getOrNull()
        }
    }

    val bookmarkedSessions: Flow<List<SynthesizedResult>> = dao.getBookmarkedSessions().map { list ->
        list.mapNotNull { entity ->
            runCatching {
                json.decodeFromString<SynthesizedResult>(entity.payloadJson).copy(
                    id = entity.id,
                    isBookmarked = entity.isBookmarked,
                    timestamp = entity.timestamp
                )
            }.getOrNull()
        }
    }

    suspend fun saveResult(result: SynthesizedResult): SynthesizedResult {
        val entity = SearchSessionEntity(
            id = if (result.id > 0L) result.id else 0L,
            query = result.query,
            primaryUrl = result.primaryUrl,
            secondaryUrl = result.secondaryUrl,
            payloadJson = json.encodeToString(result),
            isBookmarked = result.isBookmarked,
            timestamp = result.timestamp
        )
        val newId = dao.insertSession(entity)
        val saved = result.copy(id = newId)
        dao.insertSession(entity.copy(id = newId, payloadJson = json.encodeToString(saved)))
        return saved
    }

    suspend fun toggleBookmark(result: SynthesizedResult): SynthesizedResult {
        val updated = result.copy(isBookmarked = !result.isBookmarked)
        if (updated.id > 0L) {
            dao.updateBookmark(updated.id, updated.isBookmarked)
        } else {
            return saveResult(updated)
        }
        return updated
    }

    suspend fun deleteSession(id: Long) {
        dao.deleteSessionById(id)
    }

    suspend fun clearHistoryKeepBookmarks() {
        dao.clearNonBookmarkedHistory()
    }

    suspend fun seedInitialSessionsIfEmpty() {
        if (dao.countSessions() > 0) return
        val seeds = buildSeedSyntheses()
        seeds.forEach { saveResult(it) }
    }

    companion object {
        fun buildSeedSyntheses(): List<SynthesizedResult> {
            val now = System.currentTimeMillis()
            return listOf(
                SynthesizedResult(
                    query = "Solid-state battery EV commercialization timeline 2026-2028",
                    primaryUrl = "https://gemini.google.com/search?q=Solid-state+battery+EV+commercialization+timeline+2026-2028",
                    secondaryUrl = "https://copilot.microsoft.com/search?q=Solid-state+battery+EV+commercialization+timeline+2026-2028",
                    arbitrationFocus = ArbitrationFocus.BALANCED_VALUE,
                    executiveSummary = "Both Gemini Search and Copilot Search confirm that semi-solid-state cells (350–400 Wh/kg) are already entering flagship EV production in 2026, while true all-solid-state sulfide batteries (450–500 Wh/kg, 10–80% charge in 9 minutes) are slated for low-volume pilot fleets in 2027 and mass-market scaling between 2028 and 2030. Primary (Gemini) provided superior technical metrics on dendrite suppression and stack pressure, while Secondary (Copilot) surfaced granular OEM supply-chain partnerships across Toyota-Idemitsu, QuantumScape-VW PowerCo, and CATL.",
                    valueArbitrationVerdict = "Selected 5 high-signal findings by combining Gemini's cell-chemistry energy density data (56% weight) with Copilot's OEM pilot production schedules and cost-per-kWh forecasts (44% weight), eliminating duplicate marketing announcements.",
                    overallConfidenceScore = 96,
                    primaryContributionPct = 56,
                    secondaryContributionPct = 44,
                    primaryLatencyMs = 680L,
                    secondaryLatencyMs = 740L,
                    primaryRawSummary = "Gemini Search extracted electro-chemical benchmarks: sulfide-based solid electrolytes achieve >10 mS/cm ionic conductivity, enabling 9-minute fast charging and 1,000+ cycle longevity when paired with silicon-anodes or anodeless lithium-metal architectures.",
                    secondaryRawSummary = "Copilot Search aggregated commercial manufacturing milestones: Toyota & Idemitsu pilot line ramping in 2027, QuantumScape QSE-5 B-sample vehicle integration with PowerCo, and NIO/IM Motors deploying 150 kWh semi-solid packs today.",
                    takeaways = listOf(
                        ValuableTakeaway(
                            title = "2026 Reality: Semi-Solid State First, True All-Solid in 2027–2028",
                            detail = "Commercial vehicles delivering today use semi-solid gel/oxide electrolytes (~360 Wh/kg). True dry all-solid-state cells remain in B-sample automotive validation due to interface pressure requirements.",
                            origin = EngineOrigin.CONSENSUS_BOTH,
                            valueScore = 98,
                            whyValuable = "Both engines independently corroborated the distinction between semi-solid marketing claims and true all-solid-state production."
                        ),
                        ValuableTakeaway(
                            title = "Sulfide Electrolytes Lead Fast-Charge Performance (10–80% in 9 Mins)",
                            detail = "Sulfide chemistries outperform polymer and oxide variants in room-temperature ionic conductivity, allowing 4C–5C charging without lithium plating.",
                            origin = EngineOrigin.PRIMARY_GEMINI,
                            valueScore = 94,
                            whyValuable = "Primary (Gemini Search) provided specific electrochemical conductivity and C-rate data absent from general news summaries."
                        ),
                        ValuableTakeaway(
                            title = "OEM Pilot Roadmap: QuantumScape QSE-5, Toyota-Idemitsu & CATL Condensed",
                            detail = "VW PowerCo's industrialization agreement covers up to 40–80 GWh/year of QSE-5 technology once B-sample vehicle testing concludes, while Toyota targets 1,000+ km range packs for 2027–2028 launch.",
                            origin = EngineOrigin.SECONDARY_COPILOT,
                            valueScore = 93,
                            whyValuable = "Secondary (Copilot Search) pinpointed concrete GWh licensing agreements and factory ramp timelines."
                        ),
                        ValuableTakeaway(
                            title = "Manufacturing Cost Parity Requires $85–$100/kWh Pack Threshold",
                            detail = "Dry-electrode roll-to-roll processing and stack pressure reduction (<5 MPa) are the two remaining engineering bottlenecks before solid-state undercuts premium NMC lithium-ion.",
                            origin = EngineOrigin.CONSENSUS_BOTH,
                            valueScore = 91,
                            whyValuable = "Synthesized economic threshold from both streams to give a clear buyer/investor decision metric."
                        )
                    ),
                    comparisonPoints = listOf(
                        StreamComparisonPoint(
                            dimension = "Electrochemical Specs & Energy Density",
                            primaryGeminiPoint = "Detailed Wh/kg (450–500 Wh/kg), MPa stack pressure, and sulfide vs. oxide ionic conductivity.",
                            secondaryCopilotPoint = "Focused on pack-level range estimates (600–750 miles) and charging times.",
                            winner = EngineOrigin.PRIMARY_GEMINI
                        ),
                        StreamComparisonPoint(
                            dimension = "Supply Chain & OEM Licensing Deals",
                            primaryGeminiPoint = "Listed major automakers pursuing solid-state R&D.",
                            secondaryCopilotPoint = "Mapped specific joint ventures (Toyota-Idemitsu, VW-QuantumScape, Mercedes-Factorial).",
                            winner = EngineOrigin.SECONDARY_COPILOT
                        ),
                        StreamComparisonPoint(
                            dimension = "Commercial Timeline Realism",
                            primaryGeminiPoint = "Separated 2026 semi-solid deployments from 2028 mass-scale all-solid rollout.",
                            secondaryCopilotPoint = "Confirmed 2027–2028 low-volume halo EV launches before 2030 mass adoption.",
                            winner = EngineOrigin.CONSENSUS_BOTH
                        )
                    ),
                    citations = listOf(
                        CollectedCitation(
                            title = "Gemini Search: Solid-State Battery Chemistry & Commercialization",
                            url = "https://gemini.google.com/search?q=Solid-state+battery+EV+commercialization+timeline+2026-2028",
                            snippet = "Primary synthesis stream analyzing sulfide/oxide electrolyte breakthroughs and 2026-2028 automotive qualification.",
                            origin = EngineOrigin.PRIMARY_GEMINI,
                            relevanceScore = 98
                        ),
                        CollectedCitation(
                            title = "Copilot Search: EV Solid-State OEM Production & Supply Chain",
                            url = "https://copilot.microsoft.com/search?q=Solid-state+battery+EV+commercialization+timeline+2026-2028",
                            snippet = "Secondary synthesis stream tracking global gigafactory pilot lines, B-sample deliveries, and OEM partnerships.",
                            origin = EngineOrigin.SECONDARY_COPILOT,
                            relevanceScore = 96
                        ),
                        CollectedCitation(
                            title = "U.S. DOE Vehicle Technologies Office — Next-Gen Battery Targets",
                            url = "https://www.energy.gov/eere/vehicles/batteries",
                            snippet = "Benchmark targets for lithium-metal solid-state cell cycle life, energy density, and $/kWh pack cost.",
                            origin = EngineOrigin.CONSENSUS_BOTH,
                            relevanceScore = 92
                        )
                    ),
                    followUpQueries = listOf(
                        "Sulfide vs oxide solid-state battery safety and moisture sensitivity",
                        "QuantumScape QSE-5 vs Toyota Idemitsu pilot line specs",
                        "How sodium-ion batteries impact low-cost EV market vs solid-state"
                    ),
                    isBookmarked = true,
                    timestamp = now - 3600_000L
                ),
                SynthesizedResult(
                    query = "Kotlin Multiplatform vs Flutter performance and native UI sharing in 2026",
                    primaryUrl = "https://gemini.google.com/search?q=Kotlin+Multiplatform+vs+Flutter+performance+and+native+UI+sharing+in+2026",
                    secondaryUrl = "https://copilot.microsoft.com/search?q=Kotlin+Multiplatform+vs+Flutter+performance+and+native+UI+sharing+in+2026",
                    arbitrationFocus = ArbitrationFocus.TECHNICAL_DEPTH,
                    executiveSummary = "Synthesizing Primary (Gemini) and Secondary (Copilot) reveals a clear architectural split: Kotlin Multiplatform (KMP) with Compose Multiplatform is the premier choice when you need incremental adoption, zero-overhead native platform APIs, and shared Kotlin business logic with optional 100% shared Compose UI (via Skia on iOS/Web). Flutter (powered by the Impeller rendering engine) excels when strict pixel-identical custom brand UI across iOS/Android/Desktop and rapid hot-reload widget velocity are the primary goals.",
                    valueArbitrationVerdict = "Merged Gemini's deep Kotlin 2.2+ compiler & Compose Multiplatform iOS memory management benchmarks (58% contribution) with Copilot's enterprise adoption survey and Impeller shader pre-compilation metrics (42% contribution).",
                    overallConfidenceScore = 95,
                    primaryContributionPct = 58,
                    secondaryContributionPct = 42,
                    primaryLatencyMs = 610L,
                    secondaryLatencyMs = 705L,
                    primaryRawSummary = "Gemini highlighted KMP's native Kotlin/Native compilation to LLVM bitcode on iOS, direct Swift interop without bridge serialization overhead, and Jetpack Room/DataStore official KMP support.",
                    secondaryRawSummary = "Copilot emphasized Flutter's Impeller engine eliminating shader compilation jank on both Metal (iOS) and Vulkan (Android), plus its vast pub.dev widget ecosystem.",
                    takeaways = listOf(
                        ValuableTakeaway(
                            title = "Native Interop Overhead: KMP Compiles Directly to Native Binaries",
                            detail = "KMP compiles Kotlin code directly to JVM bytecode on Android and native LLVM binaries on iOS, calling platform APIs without an asynchronous message-channel serialization bridge.",
                            origin = EngineOrigin.PRIMARY_GEMINI,
                            valueScore = 97,
                            whyValuable = "Identifies the exact architectural reason KMP integrates seamlessly into existing native Android/iOS codebases."
                        ),
                        ValuableTakeaway(
                            title = "Rendering Pipeline: Impeller (Flutter) vs Compose Multiplatform (Skia)",
                            detail = "Flutter's Impeller pre-compiles a smaller, predictable set of shaders at build time for 120Hz frame stability, while Compose Multiplatform leverages Skia on iOS and native Jetpack Compose on Android.",
                            origin = EngineOrigin.CONSENSUS_BOTH,
                            valueScore = 95,
                            whyValuable = "Both engines agreed on the rendering engine mechanics and 120Hz frame-pacing implications."
                        ),
                        ValuableTakeaway(
                            title = "Incremental Adoption Strategy Favors KMP for Existing Teams",
                            detail = "Teams can share just the data/repository/domain layer (Room KMP, Ktor, Coroutines) while keeping SwiftUI on iOS and Jetpack Compose on Android—something Flutter's add-to-app model makes heavier.",
                            origin = EngineOrigin.SECONDARY_COPILOT,
                            valueScore = 92,
                            whyValuable = "Copilot surfaced practical engineering migration patterns for brownfield enterprise apps."
                        )
                    ),
                    comparisonPoints = listOf(
                        StreamComparisonPoint(
                            dimension = "Kotlin/Jetpack Ecosystem Alignment",
                            primaryGeminiPoint = "Detailed official Google Jetpack libraries supporting KMP (Room, ViewModel, DataStore, Lifecycle).",
                            secondaryCopilotPoint = "Noted Google's official backing of KMP for business logic sharing.",
                            winner = EngineOrigin.PRIMARY_GEMINI
                        ),
                        StreamComparisonPoint(
                            dimension = "Cross-Platform Desktop & Web Maturity",
                            primaryGeminiPoint = "Covered Compose for Web (Wasm) and Desktop JVM.",
                            secondaryCopilotPoint = "Provided side-by-side Windows/macOS/Linux production deployment comparisons.",
                            winner = EngineOrigin.SECONDARY_COPILOT
                        )
                    ),
                    citations = listOf(
                        CollectedCitation(
                            title = "Gemini Search: Kotlin Multiplatform & Jetpack Architecture",
                            url = "https://gemini.google.com/search?q=Kotlin+Multiplatform+vs+Flutter+performance+and+native+UI+sharing+in+2026",
                            snippet = "Primary stream analysis of Kotlin/Native LLVM compilation, Compose Multiplatform, and official Jetpack KMP libraries.",
                            origin = EngineOrigin.PRIMARY_GEMINI,
                            relevanceScore = 97
                        ),
                        CollectedCitation(
                            title = "Copilot Search: Cross-Platform Framework Benchmarks",
                            url = "https://copilot.microsoft.com/search?q=Kotlin+Multiplatform+vs+Flutter+performance+and+native+UI+sharing+in+2026",
                            snippet = "Secondary stream synthesis of Impeller Vulkan/Metal rendering vs Compose Multiplatform in enterprise apps.",
                            origin = EngineOrigin.SECONDARY_COPILOT,
                            relevanceScore = 95
                        )
                    ),
                    followUpQueries = listOf(
                        "Jetpack Room KMP setup with SQLite bundled driver",
                        "SwiftUI interoperability with Kotlin StateFlow in KMP",
                        "Compose Multiplatform iOS 120Hz ProMotion performance tuning"
                    ),
                    isBookmarked = false,
                    timestamp = now - 7200_000L
                )
            )
        }
    }
}
