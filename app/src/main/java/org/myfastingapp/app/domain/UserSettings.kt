package org.myfastingapp.app.domain

data class UserSettings(
    val defaultPlanId: String = FastPlans.DEFAULT_ID,
    val customFastingMinutes: Int = FastPlans.DEFAULT_CUSTOM_MINUTES,
    val remindersEnabled: Boolean = false,
    val reminderLeadMinutes: Int = 15,
    val weightUnit: WeightUnit = WeightUnit.LB,
    val targetWeightKg: Double? = null,
) {
    val defaultPlan: FastPlan
        get() = FastPlans.resolve(defaultPlanId, customFastingMinutes)
}

enum class WeightUnit(val storageValue: String, val label: String) {
    LB("lb", "lb"),
    KG("kg", "kg");

    companion object {
        fun fromStorage(value: String?): WeightUnit {
            return entries.firstOrNull { it.storageValue == value } ?: LB
        }
    }
}
