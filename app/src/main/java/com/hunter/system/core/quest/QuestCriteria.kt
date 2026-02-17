package com.hunter.system.core.quest

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for quest completion criteria.
 *
 * Currently only Steps is supported, but the interface allows adding Distance, Pushups,
 * WorkoutDuration, etc. in the future. Each criteria type has its own tracker implementation.
 */

// ─── Criteria Types ───────────────────────────────────────────────
sealed interface QuestCriteriaType {
  val displayName: String
  val goal: Int
  val unit: String

  data class Steps(override val goal: Int = 10_000) : QuestCriteriaType {
    override val displayName = "STEPS"
    override val unit = "steps"
  }

  // Future criteria types:
  // data class Distance(override val goal: Int = 5_000) : QuestCriteriaType {
  //     override val displayName = "DISTANCE"
  //     override val unit = "meters"
  // }
  // data class Pushups(override val goal: Int = 50) : QuestCriteriaType { ... }
  // data class ActiveMinutes(override val goal: Int = 30) : QuestCriteriaType { ... }
}

// ─── Progress Tracking ────────────────────────────────────────────
data class CriteriaProgress(
        val current: Int = 0,
        val goal: Int = 10_000,
        val criteriaType: QuestCriteriaType = QuestCriteriaType.Steps()
) {
  /** 0–100 percentage */
  val percentage: Float
    get() = ((current.toFloat() / goal) * 100f).coerceIn(0f, 100f)
  val isComplete: Boolean
    get() = current >= goal
  val remaining: Int
    get() = (goal - current).coerceAtLeast(0)
}

// ─── Tracker Interface ────────────────────────────────────────────
/**
 * Interface for tracking progress toward a quest criteria. Implementations handle sensor
 * registration, data reading, and cleanup.
 */
interface QuestCriteriaTracker {
  /** The type of criteria being tracked */
  val criteriaType: QuestCriteriaType

  /** Observable progress state */
  val progress: StateFlow<CriteriaProgress>

  /** Whether the tracker is currently active */
  val isTracking: Boolean

  /**
   * Start tracking. Records a baseline and begins monitoring.
   * @param goal Override goal (uses criteria default if null)
   */
  fun start(goal: Int? = null)

  /** Stop tracking and release resources */
  fun stop()

  /** Add simulated progress for testing (debug builds only) */
  fun addDebugSteps(count: Int) {}
}
