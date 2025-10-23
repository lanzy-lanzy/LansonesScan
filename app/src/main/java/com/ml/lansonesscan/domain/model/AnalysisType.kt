package com.ml.lansonesscan.domain.model

/**
 * Enum representing the type of analysis being performed on lansones
 */
enum class AnalysisType {
    FRUIT,
    LEAVES,
    NON_LANSONES;

    /**
     * Returns a human-readable display name for the analysis type
     */
    fun getDisplayName(): String {
        return when (this) {
            FRUIT -> "Fruit Analysis"
            LEAVES -> "Leaf Analysis"
            NON_LANSONES -> "General Analysis"
        }
    }

    /**
     * Returns a description of what this analysis type focuses on
     */
    fun getDescription(): String {
        return when (this) {
            FRUIT -> "Analyzes fruit surface for diseases, ripeness, and quality issues"
            LEAVES -> "Analyzes leaves for diseases, pest damage, nutrient deficiencies, and environmental stress"
            NON_LANSONES -> "Provides factual analysis of non-lansones items"
        }
    }

    companion object {
        /**
         * Safely converts a string to AnalysisType, returns null if invalid
         */
        fun fromString(value: String?): AnalysisType? {
            return try {
                value?.let { valueOf(it.uppercase()) }
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}