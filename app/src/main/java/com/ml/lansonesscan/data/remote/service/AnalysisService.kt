package com.ml.lansonesscan.data.remote.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializable
import com.ml.lansonesscan.data.cache.AnalysisCache
import com.ml.lansonesscan.data.remote.api.GeminiSdkClient
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.LansonesVariety
import com.ml.lansonesscan.util.ImagePreprocessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for analyzing lansones images using Gemini SDK (fast approach)
 * Based on TomatoScan implementation with caching for consistent results
 * Includes caching to return same results for identical images
 */
import com.ml.lansonesscan.data.cache.PreprocessedBitmapCache

class AnalysisService(
    private val sdkClient: GeminiSdkClient,
    private val json: Json = Json { ignoreUnknownKeys = true },
    private val cache: AnalysisCache = AnalysisCache(),
    private val preprocessedBitmapCache: PreprocessedBitmapCache = PreprocessedBitmapCache()
) {
    
    companion object {
        private const val TAG = "AnalysisService"
        
        // Performance tracking
        private var totalAnalyses = 0
        private var cacheHits = 0
        
        fun getPerformanceStats(): String {
            val hitRate = if (totalAnalyses > 0) (cacheHits * 100.0 / totalAnalyses) else 0.0
            return "Analyses: $totalAnalyses, Cache hits: $cacheHits (${String.format("%.1f", hitRate)}%)"
        }
        
        // Initial detection prompt to determine if the image contains lansones
        private val DETECTION_PROMPT = """
            You are an expert botanist specializing in lansones (Lansium domesticum). 
            Analyze the provided image and determine if it contains lansones fruit or leaves.
            
            Lansones characteristics:
            - Fruit: Small, round, yellow-brown tropical fruits that grow in clusters
            - Leaves: Compound pinnate leaves with 5-7 leaflets (this is a key identifying feature)
            
            IMPORTANT: Respond ONLY with a valid JSON object in this exact format, no other text:
            {
                "isLansones": boolean,
                "itemType": "lansones_fruit|lansones_leaves|other",
                "confidence": float,
                "description": "string"
            }
            
            Key identification points:
            - If you see a compound leaf with 5-7 leaflets, it is definitely lansones leaves
            - If you see small, round, yellow-brown fruits in clusters, it is lansones fruit
            - If you're unsure or it doesn't match these characteristics, classify as "other"
            
            Be conservative in your classification - only classify as lansones if you are confident.
        """.trimIndent()
        
        // Fruit analysis prompt optimized for lansones fruit disease detection
        private val FRUIT_ANALYSIS_PROMPT = """
            Expert agricultural pathologist specializing in lansones fruit diseases.
            Analyze the provided image of lansones fruit for diseases, ripeness, and defects.

            IMPORTANT: Respond ONLY with a valid JSON object in this exact format, no other text:
            {
                "diseaseDetected": boolean,
                "diseaseName": "string",
                "confidenceLevel": float,
                "symptoms": ["string"],
                "recommendations": ["string"],
                "severity": "low|medium|high|none",
                "ripenessLevel": "unripe|ripe|overripe|unknown"
            }

            CRITICAL RULES - MUST FOLLOW:
            1. If diseaseDetected is true, diseaseName MUST be a non-empty string (NEVER null, NEVER empty string "")
            2. If diseaseDetected is false, diseaseName should be "None" or "Healthy"
            3. Use specific disease names when possible: Anthracnose, Fruit Rot, Bacterial Soft Rot, Blight, etc.
            4. If unsure of exact disease but disease is present, use "Unidentified Disease" or "Unknown Fungal Infection"
            5. confidenceLevel must be between 0.0 and 1.0
            6. NEVER return null for diseaseName field

            Focus on lansones-specific diseases like Anthracnose, fruit rot, and bacterial soft rot.
            Provide formal, professional recommendations in complete sentences.
            If no disease is detected, provide handling and storage recommendations.
        """.trimIndent()
        
        // Leaf analysis prompt optimized for lansones leaf diseases
        private val LEAF_ANALYSIS_PROMPT = """
            Expert agricultural pathologist specializing in lansones leaf diseases.
            Analyze the provided image of lansones leaves for diseases, pests, and nutrient deficiencies.
            Lansones have compound pinnate leaves with 5-7 leaflets.

            IMPORTANT: Respond ONLY with a valid JSON object in this exact format, no other text:
            {
                "diseaseDetected": boolean,
                "diseaseName": "string",
                "confidenceLevel": float,
                "symptoms": ["string"],
                "recommendations": ["string"],
                "severity": "low|medium|high|none",
                "leafHealthStatus": "healthy|stressed|diseased|severely_damaged"
            }

            CRITICAL RULES - MUST FOLLOW:
            1. If diseaseDetected is true, diseaseName MUST be a non-empty string (NEVER null, NEVER empty string "")
            2. If diseaseDetected is false, diseaseName should be "None" or "Healthy"
            3. Use specific disease/condition names: Cercospora Leaf Spot, Anthracnose, Scale Insects, Chlorosis, etc.
            4. If unsure of exact disease but disease is present, use "Unidentified Disease" or "Unknown Leaf Condition"
            5. confidenceLevel must be between 0.0 and 1.0
            6. NEVER return null for diseaseName field

            Focus on lansones-specific issues like:
            - Cercospora Leaf Spot: Brown spots with yellow halos
            - Anthracnose: Dark, sunken lesions on leaves
            - Scale insects: Small, brown, immobile insects on leaf surfaces
            - Nutrient deficiencies: Yellowing (chlorosis), stunted growth
            - Environmental stress: Wilting, browning edges

            Provide formal, professional recommendations in complete sentences.
            If no disease is detected, provide preventive care recommendations.
        """.trimIndent()

        // Variety detection prompt for identifying lansones varieties
        private val VARIETY_DETECTION_PROMPT = """
            Expert in lansones varieties and botany.
            Analyze the provided image to identify the lansones variety.

            IMPORTANT: Respond ONLY with a valid JSON object in this exact format, no other text:
            {
                "variety": "longkong|duku|paete|jolo|unknown",
                "confidenceLevel": float,
                "characteristics": ["string"],
                "description": "string"
            }

            Focus on distinguishing characteristics like fruit size, shape, skin texture, and color.
            Known varieties: Longkong, Duku, Paete, Jolo.
            If unsure, respond with "unknown" for the variety.
        """.trimIndent()

        // Neutral analysis prompt for non-lansones items
        private val NEUTRAL_ANALYSIS_PROMPT = """
            Analyze the image and provide factual, objective observations.

            IMPORTANT: Respond ONLY with a valid JSON object in this exact format, no other text:
            {
                "observations": ["string"],
                "measurements": ["string"],
                "characteristics": ["string"]
            }

            Do not include subjective judgments, health assessments, or recommendations.
        """.trimIndent()
    }
    
    /**
     * Analyzes an image, first detecting if it contains lansones, then applying appropriate analysis
     * Uses caching to return same results for identical images
     */
    suspend fun analyzeImage(
        imageUri: Uri,
        getImageBytes: suspend (Uri) -> ByteArray,
        getMimeType: (Uri) -> String
    ): Result<AnalysisResult> {
        return withContext(Dispatchers.IO) {
            try {
                // Get image data
                val imageBytes = getImageBytes(imageUri)
                val mimeType = getMimeType(imageUri)

                // Validate image data
                if (imageBytes.isEmpty()) {
                    return@withContext Result.failure(
                        AnalysisException("Image data is empty")
                    )
                }

                // Generate hash for the image to check cache
                val imageHash = cache.generateImageHash(imageBytes)

                // Check if we have a cached result for this image
                val cachedResult = cache.get(imageHash)
                if (cachedResult != null) {
                    totalAnalyses++
                    cacheHits++
                    Log.d(TAG, "✓ Cache HIT - Returning cached result (${getPerformanceStats()})")
                    Log.d(TAG, "Image hash: ${imageHash.take(16)}...")
                    return@withContext Result.success(cachedResult)
                }

                totalAnalyses++
                Log.d(TAG, "✗ Cache MISS - Performing new analysis (${getPerformanceStats()})")
                Log.d(TAG, "Image hash: ${imageHash.take(16)}...")

                // Preprocess the image
                val preprocessedBitmap = preprocessedBitmapCache.get(imageHash) ?: run {
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        ?: return@withContext Result.failure(AnalysisException("Failed to decode image"))
                    val processed = ImagePreprocessor.preprocessForAnalysis(bitmap)
                    preprocessedBitmapCache.put(imageHash, processed)
                    processed
                }

                // First, detect if the image contains lansones
                val detectionResult = detectLansonesInImage(preprocessedBitmap)
                if (detectionResult.isFailure) {
                    return@withContext detectionResult.map {
                        // Fallback to neutral analysis if detection fails
                        createNeutralAnalysisResult("Detection failed, providing neutral analysis")
                    }
                }

                val detection = detectionResult.getOrNull()!!

                // Determine the actual analysis type based on detection
                val actualAnalysisType = if (detection.isLansones) {
                    when (detection.itemType) {
                        "lansones_fruit" -> AnalysisType.FRUIT
                        "lansones_leaves" -> AnalysisType.LEAVES
                        else -> AnalysisType.NON_LANSONES
                    }
                } else {
                    AnalysisType.NON_LANSONES
                }

                // Perform appropriate analysis
                val analysisResult = when (actualAnalysisType) {
                    AnalysisType.FRUIT -> performLansonesAnalysis(preprocessedBitmap, FRUIT_ANALYSIS_PROMPT, actualAnalysisType, imageBytes, mimeType)
                    AnalysisType.LEAVES -> performLansonesAnalysis(preprocessedBitmap, LEAF_ANALYSIS_PROMPT, actualAnalysisType, imageBytes, mimeType)
                    AnalysisType.NON_LANSONES -> performNeutralAnalysis(preprocessedBitmap, detection.description)
                }

                Result.success(analysisResult)

            } catch (e: Exception) {
                Result.failure(
                    AnalysisException(
                        "Unexpected error during analysis: ${e.message}",
                        e
                    )
                )
            }
        }
    }

    /**
     * Detects if the image contains lansones fruit or leaves
     */
    private suspend fun detectLansonesInImage(
        preprocessedBitmap: Bitmap
    ): Result<DetectionResult> {
        return try {
            // Use SDK client - much faster!
            val apiResult = sdkClient.analyzeImage(preprocessedBitmap, DETECTION_PROMPT)
            
            if (apiResult.isFailure) {
                return Result.failure(
                    AnalysisException("Detection API call failed: ${apiResult.exceptionOrNull()?.message}")
                )
            }

            val responseText = apiResult.getOrNull()!!
            val detection = parseDetectionResponse(responseText)
            Result.success(detection)
        } catch (e: Exception) {
            Result.failure(AnalysisException("Detection failed: ${e.message}", e))
        }
    }

    /**
     * Performs lansones-specific analysis with automatic variety detection
     */
    private suspend fun performLansonesAnalysis(
        preprocessedBitmap: Bitmap,
        prompt: String,
        analysisType: AnalysisType,
        imageBytes: ByteArray,
        mimeType: String
    ): AnalysisResult {
        // Use SDK client - much faster!
        val apiResult = sdkClient.analyzeImage(preprocessedBitmap, prompt)
        if (apiResult.isFailure) {
            throw AnalysisException("Lansones analysis API call failed: ${apiResult.exceptionOrNull()?.message}")
        }

        val responseText = apiResult.getOrNull()!!
        val mainAnalysisResult = parseResponseText(responseText, analysisType)
        
        // Then, automatically detect the variety for lansones fruit
        var varietyResult: VarietyAnalysisResult? = null
        if (analysisType == AnalysisType.FRUIT) {
            varietyResult = detectLansonesVariety(imageBytes, mimeType)
        }

        return AnalysisResult(
            diseaseDetected = mainAnalysisResult.diseaseDetected,
            diseaseName = mainAnalysisResult.diseaseName,
            confidenceLevel = mainAnalysisResult.confidenceLevel,
            affectedPart = mainAnalysisResult.affectedPart,
            symptoms = mainAnalysisResult.symptoms,
            recommendations = mainAnalysisResult.recommendations,
            severity = mainAnalysisResult.severity,
            rawResponse = mainAnalysisResult.rawResponse,
            detectedAnalysisType = mainAnalysisResult.detectedAnalysisType,
            varietyResult = varietyResult
        )
    }

    /**
     * Automatically detects the variety of lansones fruit
     */
    private suspend fun detectLansonesVariety(
        imageBytes: ByteArray,
        mimeType: String
    ): VarietyAnalysisResult? {
        return try {
            // Convert bytes to bitmap for SDK
            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ?: return null

            // Preprocess image for optimal analysis
            val preprocessedBitmap = ImagePreprocessor.preprocessForAnalysis(bitmap)

            // Use SDK client
            val apiResult = sdkClient.analyzeImage(preprocessedBitmap, VARIETY_DETECTION_PROMPT)
            if (apiResult.isFailure) {
                // Don't fail the entire analysis if variety detection fails
                return null
            }

            val responseText = apiResult.getOrNull()!!

            // Extract JSON from response text if it's embedded in markdown or other text
            val jsonText = extractJsonFromText(responseText)
            
            val varietyResponse = json.decodeFromString<VarietyAnalysisResponse>(jsonText)
            
            VarietyAnalysisResult(
                variety = varietyResponse.variety?.let { LansonesVariety.fromString(it) } ?: LansonesVariety.UNKNOWN,
                confidenceLevel = varietyResponse.confidenceLevel,
                characteristics = varietyResponse.characteristics ?: emptyList(),
                description = varietyResponse.description ?: ""
            )
        } catch (e: Exception) {
            // Don't fail the entire analysis if variety detection fails
            null
        }
    }

    /**
     * Performs neutral analysis for non-lansones items
     */
    private suspend fun performNeutralAnalysis(
        preprocessedBitmap: Bitmap,
        itemDescription: String
    ): AnalysisResult {
        // Use SDK client
        val apiResult = sdkClient.analyzeImage(preprocessedBitmap, NEUTRAL_ANALYSIS_PROMPT)
        if (apiResult.isFailure) {
            // Fallback to basic neutral result if API fails
            return createNeutralAnalysisResult(itemDescription)
        }

        val responseText = apiResult.getOrNull()!!
        return parseNeutralResponseText(responseText, itemDescription)
    }

    /**
     * Creates a basic neutral analysis result
     */
    private fun createNeutralAnalysisResult(description: String): AnalysisResult {
        return AnalysisResult(
            diseaseDetected = false,
            diseaseName = null,
            confidenceLevel = 1.0f,
            affectedPart = "general",
            symptoms = emptyList(),
            recommendations = emptyList(),
            severity = "none",
            rawResponse = "Neutral analysis: $description",
            detectedAnalysisType = AnalysisType.NON_LANSONES
        )
    }

    /**
     * Parses response text directly from SDK (new fast approach)
     */
    private fun parseResponseText(
        responseText: String,
        analysisType: AnalysisType
    ): AnalysisResult {
        return try {
            // Try to parse as JSON first
            parseJsonResponse(responseText, analysisType)
        } catch (e: Exception) {
            // Fallback to text-based parsing
            parseTextResponse(responseText, analysisType)
        }
    }
    
    /**
     * Parses Gemini API response into structured analysis result (legacy OkHttp approach)
     */
    @Deprecated("Use parseResponseText with SDK client instead")
    private fun parseGeminiResponse(
        response: com.ml.lansonesscan.data.remote.dto.GeminiResponse,
        analysisType: AnalysisType
    ): AnalysisResult {
        if (response.candidates.isEmpty()) {
            throw AnalysisException("No analysis candidates returned from API")
        }
        
        val candidate = response.candidates.first()
        if (candidate.content.parts.isEmpty()) {
            throw AnalysisException("No content parts in API response")
        }
        
        val responseText = candidate.content.parts.first().text
        return parseResponseText(responseText, analysisType)
    }
    
    /**
     * Parses JSON-formatted response from Gemini API
     */
    private fun parseJsonResponse(responseText: String, analysisType: AnalysisType): AnalysisResult {
        // Extract JSON from response text if it's embedded in markdown or other text
        val jsonText = extractJsonFromText(responseText)
        
        return try {
            val jsonResponse = json.decodeFromString<JsonAnalysisResponse>(jsonText)
            
            // CRITICAL: Ensure disease name is never null/blank when disease is detected
            // This prevents validation errors in ScanResult
            val finalDiseaseDetected: Boolean
            val finalDiseaseName: String?
            
            if (jsonResponse.diseaseDetected) {
                // Disease detected - ensure we have a valid disease name
                if (jsonResponse.diseaseName.isNullOrBlank()) {
                    // AI detected disease but didn't provide name - use fallback
                    finalDiseaseDetected = true
                    finalDiseaseName = "Unidentified Disease"
                    Log.w(TAG, "Disease detected but no name provided by AI. Using fallback: 'Unidentified Disease'")
                } else {
                    // AI provided both detection and name - use as is
                    finalDiseaseDetected = true
                    finalDiseaseName = jsonResponse.diseaseName
                }
            } else {
                // No disease detected - ensure disease name is null
                finalDiseaseDetected = false
                finalDiseaseName = null
            }
            
            AnalysisResult(
                diseaseDetected = finalDiseaseDetected,
                diseaseName = finalDiseaseName,
                confidenceLevel = jsonResponse.confidenceLevel,
                affectedPart = jsonResponse.affectedPart ?: analysisType.name.lowercase(),
                symptoms = jsonResponse.symptoms ?: emptyList(),
                recommendations = jsonResponse.recommendations ?: emptyList(),
                severity = if (finalDiseaseDetected) (jsonResponse.severity ?: "medium") else "none",
                rawResponse = responseText,
                detectedAnalysisType = analysisType
            )
        } catch (e: SerializationException) {
            // If JSON parsing fails, throw exception to trigger fallback
            throw AnalysisException("Failed to parse JSON response: ${e.message}", e)
        }
    }
    
    /**
     * Extracts JSON content from text that might contain markdown or other formatting
     */
    private fun extractJsonFromText(text: String): String {
        // Look for JSON block in markdown code blocks
        val jsonBlockRegex = "```(?:json)?\\s*\\n?([\\s\\S]*?)```".toRegex()
        val jsonMatch = jsonBlockRegex.find(text)
        
        if (jsonMatch != null) {
            return jsonMatch.groupValues[1].trim()
        }
        
        // Look for JSON object starting with { and ending with }
        val jsonObjectRegex = "\\{[\\s\\S]*\\}".toRegex()
        val objectMatch = jsonObjectRegex.find(text)
        
        if (objectMatch != null) {
            return objectMatch.value
        }
        
        // If no JSON found, return original text to trigger fallback parsing
        throw AnalysisException("No JSON content found in response")
    }
    
    /**
     * Parses text-based response from Gemini API using pattern matching
     */
    private fun parseTextResponse(responseText: String, analysisType: AnalysisType): AnalysisResult {
        val text = responseText.lowercase()
        
        // Detect disease presence
        val diseaseDetected = detectDiseaseInText(text)
        
        // Extract disease name
        val diseaseName = extractDiseaseNameFromText(responseText)
        
        // Estimate confidence level
        val confidenceLevel = estimateConfidenceFromText(text)
        
        // Extract symptoms
        val symptoms = extractSymptomsFromText(responseText)
        
        // Extract recommendations
        val recommendations = extractRecommendationsFromText(responseText)
        
        // Determine severity
        val severity = determineSeverityFromText(text)
        
        return AnalysisResult(
            diseaseDetected = diseaseDetected,
            diseaseName = diseaseName,
            confidenceLevel = confidenceLevel,
            affectedPart = analysisType.name.lowercase(),
            symptoms = symptoms,
            recommendations = recommendations,
            severity = severity,
            rawResponse = responseText,
            detectedAnalysisType = analysisType
        )
    }
    
    /**
     * Detects if disease is mentioned in the response text
     */
    private fun detectDiseaseInText(text: String): Boolean {
        val lowerText = text.lowercase()
        
        // First check for explicit healthy indicators in context
        val healthyPhrases = listOf(
            "appears healthy", "looks healthy", "is healthy", "seems healthy",
            "no disease", "no visible disease", "no signs of disease",
            "no symptoms", "no visible symptoms", "healthy condition"
        )
        
        // Only return false if we find a clear statement that the plant is healthy
        if (healthyPhrases.any { phrase -> lowerText.contains(phrase) }) {
            return false
        }
        
        // Then check for disease indicators
        val diseaseKeywords = listOf(
            "anthracnose", "rot", "blight", "spot disease", "mildew",
            "infected", "diseased", "unhealthy", "damaged",
            "fungal growth", "bacterial infection", "pathogen",
            "signs of disease", "disease symptoms", "infection"
        )
        
        return diseaseKeywords.any { keyword ->
            lowerText.contains(keyword)
        }
    }
    
    /**
     * Extracts disease name from response text
     */
    private fun extractDiseaseNameFromText(text: String): String? {
        val diseasePatterns = listOf(
            "anthracnose",
            "fruit rot",
            "bacterial soft rot",
            "leaf spot",
            "powdery mildew",
            "downy mildew",
            "bacterial leaf blight",
            "cercospora",
            "phyllosticta",
            "septoria",
            "oidium",
            "scale insects",
            "mealybugs",
            "spider mites",
            "thrips"
        )
        
        return diseasePatterns.find { disease ->
            text.lowercase().contains(disease)
        }?.replaceFirstChar { it.uppercase() }
    }
    
    /**
     * Estimates confidence level from text indicators
     */
    private fun estimateConfidenceFromText(text: String): Float {
        return when {
            text.contains("clearly") || text.contains("definitely") || text.contains("obvious") -> 0.9f
            text.contains("likely") || text.contains("appears") || text.contains("seems") -> 0.7f
            text.contains("possibly") || text.contains("might") || text.contains("could") -> 0.5f
            text.contains("uncertain") || text.contains("unclear") || text.contains("difficult") -> 0.3f
            else -> 0.6f // Default moderate confidence
        }
    }
    
    /**
     * Extracts symptoms from response text
     */
    private fun extractSymptomsFromText(text: String): List<String> {
        val symptoms = mutableListOf<String>()
        
        val symptomKeywords = mapOf(
            "black spots" to listOf("black spot", "dark spot", "black patch"),
            "brown patches" to listOf("brown patch", "brown area", "browning"),
            "yellowing" to listOf("yellow", "yellowing", "chlorosis"),
            "wilting" to listOf("wilt", "wilting", "drooping"),
            "fungal growth" to listOf("fungal", "mold", "mildew"),
            "bacterial infection" to listOf("bacterial", "soft rot", "oozing"),
            "powdery coating" to listOf("powdery", "white coating"),
            "webbing" to listOf("webbing", "spider web"),
            "sticky residue" to listOf("sticky", "honeydew"),
            "small insects" to listOf("insects", "bugs", "pests"),
            "necrotic areas" to listOf("necrotic", "dead tissue"),
            "distorted growth" to listOf("distorted", "deformed")
        )
        
        val lowerText = text.lowercase()
        symptomKeywords.forEach { (symptom, keywords) ->
            if (keywords.any { keyword -> lowerText.contains(keyword) }) {
                symptoms.add(symptom)
            }
        }
        
        return symptoms.ifEmpty { listOf("General disease symptoms observed") }
    }
    
    /**
     * Extracts recommendations from response text
     */
    private fun extractRecommendationsFromText(text: String): List<String> {
        val recommendations = mutableListOf<String>()
        
        val lowerText = text.lowercase()
        
        // Common recommendations based on keywords
        when {
            lowerText.contains("fungicide") -> recommendations.add("Apply appropriate fungicide treatment")
            lowerText.contains("remove") -> recommendations.add("Remove affected parts to prevent spread")
            lowerText.contains("spray") -> recommendations.add("Apply recommended spray treatment")
            lowerText.contains("drainage") -> recommendations.add("Improve drainage around the plant")
            lowerText.contains("ventilation") -> recommendations.add("Ensure proper air circulation")
            lowerText.contains("pruning") -> recommendations.add("Prune affected leaves to improve air circulation")
            lowerText.contains("fertilizer") || lowerText.contains("nutrient") -> recommendations.add("Apply appropriate fertilizer to address nutrient deficiencies")
            lowerText.contains("watering") -> recommendations.add("Adjust watering practices to prevent waterlogging")
        }
        
        // Default recommendations if none found
        if (recommendations.isEmpty()) {
            recommendations.addAll(listOf(
                "Monitor plant health regularly",
                "Maintain proper growing conditions",
                "Consult with agricultural extension services if symptoms persist"
            ))
        }
        
        // Add leaf-specific general recommendations
        if (lowerText.contains("leaf") || lowerText.contains("foliage")) {
            recommendations.addAll(listOf(
                "Ensure proper spacing between plants for air circulation",
                "Water at the base of the plant to keep foliage dry",
                "Apply mulch to maintain soil moisture and temperature"
            ))
        }
        
        return recommendations
    }

    /**
     * Determines severity level from text indicators
     */
    private fun determineSeverityFromText(text: String): String {
        return when {
            text.contains("severe") || text.contains("extensive") || text.contains("widespread") -> "high"
            text.contains("moderate") || text.contains("noticeable") || text.contains("significant") -> "medium"
            text.contains("mild") || text.contains("slight") || text.contains("minor") -> "low"
            text.contains("healthy") || text.contains("no disease") || text.contains("normal") -> "none"
            else -> "medium" // Default to medium if unclear
        }
    }

    /**
     * Parses detection response from Gemini API
     */
    private fun parseDetectionResponse(responseText: String): DetectionResult {
        return try {
            val jsonText = extractJsonFromText(responseText)
            json.decodeFromString<DetectionResult>(jsonText)
        } catch (e: Exception) {
            // Fallback to text-based detection for cases where JSON parsing fails
            // This is a more robust approach that analyzes the content of the response
            val lowerText = responseText.lowercase()
            
            // Check if the AI determined it's lansones based on the response content
            val isLansones = lowerText.contains("lansones") || 
                           lowerText.contains("lansium") || 
                           lowerText.contains("is lansones") ||
                           lowerText.contains("contains lansones") ||
                           lowerText.contains("lansones fruit") ||
                           lowerText.contains("lansones leaves") ||
                           lowerText.contains("lansones leaf")
            
            // Determine if it's leaves or fruit based on keywords in the AI's response
            val itemType = when {
                !isLansones -> "other"
                lowerText.contains("leaf") || lowerText.contains("leaves") || 
                lowerText.contains("foliage") || lowerText.contains("leaflet") ||
                lowerText.contains("compound") || lowerText.contains("pinnate") ||
                lowerText.contains("5 leaflets") || lowerText.contains("7 leaflets") -> "lansones_leaves"
                lowerText.contains("fruit") || lowerText.contains("berry") || 
                lowerText.contains("cluster") -> "lansones_fruit"
                else -> "other" // If we can't determine, classify as other
            }
            
            DetectionResult(
                isLansones = isLansones && itemType != "other",
                itemType = itemType,
                confidence = if (isLansones && itemType != "other") 0.7f else 0.3f,
                description = "Fallback detection based on response content"
            )
        }
    }

    /**
     * Parses neutral analysis response text (new SDK approach)
     */
    private fun parseNeutralResponseText(responseText: String, itemDescription: String): AnalysisResult {
        return try {
            val jsonText = extractJsonFromText(responseText)
            val neutralResponse = json.decodeFromString<NeutralAnalysisResponse>(jsonText)

            AnalysisResult(
                diseaseDetected = false,
                diseaseName = null,
                confidenceLevel = 1.0f,
                affectedPart = "general",
                symptoms = neutralResponse.observations ?: emptyList(),
                recommendations = emptyList(), // No recommendations for non-lansones items
                severity = "none",
                rawResponse = responseText,
                detectedAnalysisType = AnalysisType.NON_LANSONES
            )
        } catch (e: Exception) {
            createNeutralAnalysisResult(itemDescription)
        }
    }
    
    /**
     * Parses neutral analysis response for non-lansones items (legacy)
     */
    @Deprecated("Use parseNeutralResponseText with SDK client instead")
    private fun parseNeutralResponse(response: com.ml.lansonesscan.data.remote.dto.GeminiResponse, itemDescription: String): AnalysisResult {
        val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: return createNeutralAnalysisResult(itemDescription)
        return parseNeutralResponseText(responseText, itemDescription)
    }
}

/**
 * Data class representing the result of image analysis
 */
@Serializable
data class AnalysisResult(
    val diseaseDetected: Boolean,
    val diseaseName: String?,
    val confidenceLevel: Float,
    val affectedPart: String,
    val symptoms: List<String>,
    val recommendations: List<String>,
    val severity: String,
    val rawResponse: String,
    val detectedAnalysisType: AnalysisType = AnalysisType.FRUIT, // Default for backward compatibility
    val varietyResult: VarietyAnalysisResult? = null
)

/**
 * Data class for variety analysis results
 */
@Serializable
data class VarietyAnalysisResult(
    val variety: LansonesVariety,
    val confidenceLevel: Float,
    val characteristics: List<String>,
    val description: String
)

/**
 * Data class for parsing JSON responses from Gemini API
 */
@Serializable
data class JsonAnalysisResponse(
    val diseaseDetected: Boolean,
    val diseaseName: String?,
    val confidenceLevel: Float,
    val affectedPart: String?,
    val symptoms: List<String>?,
    val recommendations: List<String>?,
    val severity: String?,
    val ripenessLevel: String? = null, // For fruit analysis
    val leafHealthStatus: String? = null // For leaf analysis
)

/**
 * Data class for variety analysis responses
 */
@Serializable
data class VarietyAnalysisResponse(
    val variety: String?,
    val confidenceLevel: Float,
    val characteristics: List<String>?,
    val description: String?
)

/**
 * Data class for detection results
 */
@Serializable
data class DetectionResult(
    val isLansones: Boolean,
    val itemType: String,
    val confidence: Float,
    val description: String
)

/**
 * Data class for neutral analysis responses
 */
@Serializable
data class NeutralAnalysisResponse(
    val diseaseDetected: Boolean = false,
    val diseaseName: String? = null,
    val confidenceLevel: Float = 1.0f,
    val affectedPart: String = "general",
    val symptoms: List<String>? = null,
    val recommendations: List<String>? = null,
    val severity: String = "none",
    val observations: List<String>? = null,
    val measurements: List<String>? = null,
    val characteristics: List<String>? = null
)

/**
 * Exception thrown during analysis operations
 */
class AnalysisException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)