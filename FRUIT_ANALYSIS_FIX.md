# Fruit Analysis Validation Fix: "Disease name cannot be null" Error

## Problem
The fruit analysis was failing with:
```
Analysis failed: Disease name cannot be null or blank when disease is detected
```

This occurred when the AI detected a disease but returned `diseaseName: null` in the JSON response, violating the `ScanResult` validation rule.

## Root Cause
The AI model sometimes:
1. Detects a disease (`diseaseDetected: true`)
2. But fails to provide a disease name (`diseaseName: null`)

This created an invalid state since the `ScanResult` constructor requires a non-null disease name whenever `diseaseDetected` is true:

```kotlin
if (diseaseDetected) {
    require(!diseaseName.isNullOrBlank()) { 
        "Disease name cannot be null or blank when disease is detected" 
    }
}
```

## Solution

### 1. Improved AI Prompts
Updated both fruit and leaf analysis prompts with explicit rules:

```
CRITICAL RULES:
- If diseaseDetected is true, diseaseName MUST be provided (never null or empty)
- Use specific disease names: Anthracnose, Fruit Rot, Bacterial Soft Rot, Blight, etc.
- If unsure of exact disease, use "Unknown Disease" instead of null
- confidenceLevel should be between 0.0 and 1.0
```

This guides the AI to always provide a disease name when one is detected.

### 2. Fallback Logic in Response Parser
Added defensive parsing in `parseJsonResponse()` to handle edge cases:

```kotlin
// If disease is detected but no name provided, use a default description
val finalDiseaseName = if (jsonResponse.diseaseDetected && jsonResponse.diseaseName.isNullOrBlank()) {
    "Unidentified disease detected"
} else {
    jsonResponse.diseaseName
}

// If disease is detected but we have no name even after default, mark as not detected
val finalDiseaseDetected = if (finalDiseaseName.isNullOrBlank()) {
    false
} else {
    jsonResponse.diseaseDetected
}
```

This ensures:
- If disease is detected but no name is provided, we use "Unidentified disease detected" as a placeholder
- If somehow the disease name is still blank, we mark `diseaseDetected = false` to avoid validation errors
- Severity is only set to "low|medium|high" when a disease is actually detected

## Files Modified
- `app/src/main/java/com/ml/lansonesscan/data/remote/service/AnalysisService.kt`
  - Updated `FRUIT_ANALYSIS_PROMPT` with CRITICAL RULES
  - Updated `LEAF_ANALYSIS_PROMPT` with CRITICAL RULES
  - Enhanced `parseJsonResponse()` with fallback logic

## User Experience
- Users no longer see validation errors for diseases without names
- If AI can't identify a specific disease but detects abnormalities, it returns "Unidentified disease detected"
- If no disease name can be determined, the app correctly marks no disease detected (healthy fruit/leaves)

## Defense in Depth
Two-layer protection:
1. **Prevention**: AI prompts explicitly require disease names
2. **Recovery**: Fallback logic handles API responses that don't follow rules

This ensures robustness even if the AI occasionally ignores prompt instructions.
