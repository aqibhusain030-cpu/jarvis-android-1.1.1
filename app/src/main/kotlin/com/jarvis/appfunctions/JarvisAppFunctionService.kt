package com.jarvis.appfunctions

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.jarvis.BuildConfig
import com.jarvis.autonomy.Goal
import com.jarvis.autonomy.GoalStore
import com.jarvis.config.ConfigRepository
import com.jarvis.memory.MemoryStore
import com.jarvis.security.KillSwitch
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android-system integration surface for safe, read-only JARVIS capabilities.
 *
 * Mutating and high-risk capabilities remain behind JARVIS SecurityPolicy,
 * PermissionGate, confirmation, and unlock controls and are not exposed here.
 */
@RequiresApi(Build.VERSION_CODES.S)
@AndroidEntryPoint
@AppFunctionServiceEntryPoint(
    serviceName = "JarvisAppFunctionService",
    appFunctionXmlFileName = "jarvis_app_function_service",
)
abstract class BaseJarvisAppFunctionService : AppFunctionService() {

    @Inject internal lateinit var config: ConfigRepository
    @Inject internal lateinit var goalStore: GoalStore
    @Inject internal lateinit var memory: MemoryStore
    @Inject internal lateinit var killSwitch: KillSwitch

    /**
     * Read the current JARVIS runtime and safety state.
     *
     * @return Non-sensitive runtime status including version, core state, and kill-switch state.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getJarvisStatus(): JarvisStatus = withContext(Dispatchers.IO) {
        JarvisStatus(
            versionName = BuildConfig.VERSION_NAME,
            coreEnabled = config.current.value.coreEnabled,
            killSwitchActive = killSwitch.isKilled.value,
            localAiOnlyByDefault = !config.current.value.cloudFallbackEnabled,
        )
    }

    /**
     * List the user's JARVIS goals as compact summaries.
     *
     * @param limit Maximum number of goals to return. Must be between 1 and 50.
     * @return Compact goal summaries without plan steps, evidence, or tool arguments.
     * @throws androidx.appfunctions.AppFunctionInvalidArgumentException if limit is outside 1..50.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listGoals(limit: Int = 20): GoalList = withContext(Dispatchers.IO) {
        requireValidLimit(limit)
        memory.open()
        GoalList(goalStore.all().take(limit).map(::toSummary))
    }

    /**
     * Get one JARVIS goal as a compact summary.
     *
     * @param goalId Stable JARVIS goal identifier.
     * @return Compact summary of the requested goal.
     * @throws androidx.appfunctions.AppFunctionInvalidArgumentException if goalId is blank.
     * @throws androidx.appfunctions.AppFunctionElementNotFoundException if the goal does not exist.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getGoal(goalId: String): GoalSummary = withContext(Dispatchers.IO) {
        if (goalId.isBlank() || goalId.length > MAX_ID_LENGTH) {
            throw AppFunctionInvalidArgumentException(
                "goalId must be non-blank and at most $MAX_ID_LENGTH characters"
            )
        }
        memory.open()
        val goal = goalStore.get(goalId)
            ?: throw AppFunctionElementNotFoundException(
                "No JARVIS goal exists for the requested id"
            )
        toSummary(goal)
    }

    private fun requireValidLimit(limit: Int) {
        if (limit !in MIN_LIMIT..MAX_LIMIT) {
            throw AppFunctionInvalidArgumentException("limit must be between $MIN_LIMIT and $MAX_LIMIT")
        }
    }

    private fun toSummary(goal: Goal) = GoalSummary(
        id = goal.id,
        title = goal.title.take(MAX_TITLE_LENGTH),
        status = goal.status.name,
        priority = goal.priority,
        deadlineEpochMillis = goal.deadline,
    )

    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class JarvisStatus(
        /** Current JARVIS application version. */
        val versionName: String,
        /** Whether the main JARVIS core is enabled by configuration. */
        val coreEnabled: Boolean,
        /** Whether the emergency kill switch is currently active. */
        val killSwitchActive: Boolean,
        /** Whether cloud fallback is disabled by default. */
        val localAiOnlyByDefault: Boolean,
    )

    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class GoalList(
        /** Goal summaries ordered using JARVIS's existing goal priority policy. */
        val goals: List<GoalSummary>,
    )

    @AppFunctionSerializable(isDescribedByKDoc = true)
    data class GoalSummary(
        /** Stable identifier of the goal. */
        val id: String,
        /** User-provided goal title. */
        val title: String,
        /** Current goal lifecycle state. */
        val status: String,
        /** JARVIS priority value for the goal. */
        val priority: Int,
        /** Deadline as Unix epoch milliseconds, or null when no deadline is set. */
        val deadlineEpochMillis: Long?,
    )

    private companion object {
        const val MIN_LIMIT = 1
        const val MAX_LIMIT = 50
        const val MAX_ID_LENGTH = 128
        const val MAX_TITLE_LENGTH = 200
    }
}
