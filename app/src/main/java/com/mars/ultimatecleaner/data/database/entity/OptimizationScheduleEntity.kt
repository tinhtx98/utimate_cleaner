package com.mars.ultimatecleaner.data.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.mars.ultimatecleaner.domain.model.OptimizationSchedule
import com.mars.ultimatecleaner.domain.model.OptimizationType
import com.mars.ultimatecleaner.domain.model.ScheduleFrequency

@Entity(
    tableName = "optimization_schedules",
    indices = [
        Index(value = ["isEnabled"]),
        Index(value = ["nextRun"]),
        Index(value = ["optimizationType"])
    ]
)
data class OptimizationScheduleEntity(
    @PrimaryKey
    val id: String,
    val optimizationType: String,
    val frequency: String,
    val timeOfDay: Int,
    val isEnabled: Boolean,
    val lastRun: Long?,
    val nextRun: Long?,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toDomainModel(): OptimizationSchedule {
        return OptimizationSchedule(
            id = id,
            type = OptimizationType.valueOf(optimizationType),
            frequency = ScheduleFrequency.valueOf(frequency),
            timeOfDay = timeOfDay,
            isEnabled = isEnabled,
            lastRun = lastRun,
            nextRun = nextRun
        )
    }
}

fun OptimizationSchedule.toEntity(): OptimizationScheduleEntity {
    return OptimizationScheduleEntity(
        id = id,
        optimizationType = type.name,
        frequency = frequency.name,
        timeOfDay = timeOfDay,
        isEnabled = isEnabled,
        lastRun = lastRun,
        nextRun = nextRun,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}