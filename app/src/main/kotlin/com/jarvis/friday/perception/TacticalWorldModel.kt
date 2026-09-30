package com.jarvis.friday.perception

import com.jarvis.friday.core.TacticalActor
import com.jarvis.friday.core.TacticalWorld
import com.jarvis.friday.core.VitalSnapshot

interface TacticalSensor {
    val sensorId: String
    suspend fun sample(): SensorSample
}

sealed interface SensorSample {
    data class Actors(val values: List<TacticalActor>) : SensorSample
    data class Hazards(val values: List<String>) : SensorSample
    data class Vitals(val values: VitalSnapshot) : SensorSample
}

class TacticalWorldModel {
    fun fuse(samples: List<SensorSample>): TacticalWorld {
        val actors = samples.filterIsInstance<SensorSample.Actors>().flatMap { it.values }.distinctBy { it.id }
        val hazards = samples.filterIsInstance<SensorSample.Hazards>().flatMap { it.values }.distinct()
        val confidence = if (samples.isEmpty()) 0.0 else samples.sumOf {
            when (it) {
                is SensorSample.Actors -> if (it.values.isNotEmpty()) 1.0 else 0.0
                is SensorSample.Hazards -> if (it.values.isNotEmpty()) 1.0 else 0.0
                is SensorSample.Vitals -> it.values.confidence
            }
        } / samples.size.toDouble()
        return TacticalWorld(actors = actors, hazards = hazards, confidence = confidence.coerceIn(0.0, 1.0), sourceCount = samples.size)
    }
}
