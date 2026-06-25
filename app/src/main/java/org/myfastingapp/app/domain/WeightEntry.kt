package org.myfastingapp.app.domain

data class WeightEntry(
    val id: Long,
    val weightKg: Double,
    val recordedEpochMillis: Long,
    val createdEpochMillis: Long,
)

fun Double.kgToLb(): Double = this * 2.2046226218

fun Double.lbToKg(): Double = this / 2.2046226218
