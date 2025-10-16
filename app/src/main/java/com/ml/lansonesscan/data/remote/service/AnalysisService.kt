package com.ml.lansonesscan.data.remote.service

import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.ml.lansonesscan.data.remote.api.GeminiApiClient
import com.ml.lansonesscan.data.remote.api.GeminiRequestBuilder
import com.ml.lansonesscan.data.remote.dto.GeminiResponse
import com.ml.lansonesscan.domain.model.AnalysisType
import com.ml.lansonesscan.domain.model.LansonesVariety
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Service for analyzing lansones images using Gemini API with specialized prompts
 */
class AnalysisService(
    private val apiClient: GeminiApiClient,
    private val requestBuilder: GeminiRequestBuilder,
    private val gson: Gson = Gson()
) {
    
    companion object {
        private const val TAG = "AnalysisService"
        
        // Fruit analysis prompt optimized for lansones fruit disease detection
        private val FRUIT_ANALYSIS_PROMPT = """
            You are an expert agricultural pathologist specializing in lansones (Lansium domesticum) fruit diseases. 
            Analyze this lansones fruit image and provide a detailed assessment.
            
            Focus on identifying:
            1. Disease symptoms (black spots, brown patches, fungal growth, bacterial infections)
            2. ripeness level and quality indicators
            3. Physical damage or defects
            4. Overall fruit health status
            
            Provide your response in the following JSON format:
            {
                "diseaseDetected": boolean,
                "diseaseName": "string or null",
                "confidenceLevel": float (0.0 to 1.0),
                "affectedPart": "fruit",
                "symptoms": ["list of observed symptoms"],
                "recommendations": ["list of actionable recommendations"],
                "severity": "low|medium|high|none",
                "ripenessLevel": "unripe|ripe|overripe|unknown"
            }
            
            Be specific about lansones-related diseases such as:
            - Anthracnose (Colletotrichum gloeosporioides)
            - Fruit rot diseases
            - Bacterial soft rot
            - Post-harvest decay
            
            If no disease is detected, still provide recommendations for proper handling and storage.
        """.trimIndent()
        
        // Leaf analysis prompt optimized for lansones leaf diseases
        private val LEAF_ANALYSIS_PROMPT = """
            You are an expert agricultural pathologist specializing in lansones (Lansium domesticum) leaf diseases and plant health.
            Analyze this lansones leaf image and provide a detailed assessment.

            Focus on identifying:
            1. Leaf diseases (leaf spots, blights, fungal infections, bacterial diseases)
            2. Pest damage (insect feeding, mite damage, scale insects)
            3. Nutrient deficiencies (yellowing patterns, chlorosis, necrosis)
            4. Environmental stress indicators
            5. Overall plant health status

            Provide your response in the following JSON format:
            {
                "diseaseDetected": boolean,
                "diseaseName": "string or null",
                "confidenceLevel": float (0.0 to 1.0),
                "affectedPart": "leaves",
                "symptoms": ["list of observed symptoms"],
                "recommendations": ["list of actionable recommendations"],
                "severity": "low|medium|high|none",
                "leafHealthStatus": "healthy|stressed|diseased|severely_damaged"
            }

            Be specific about lansones-related leaf issues such as:
            - Leaf spot diseases (Cercospora, Phyllosticta)
            - Powdery mildew
            - Bacterial leaf blight
            - Scale insect infestations
            - Nutrient deficiency symptoms (nitrogen, potassium, magnesium)

            If no disease is detected, still provide recommendations for preventive care and optimal growing conditions.
        """.trimIndent()

        // Variety detection prompt for identifying lansones varieties
        private val VARIETY_DETECTION_PROMPT = """
            You are an expert in lansones (Lansium domesticum) varieties and botany.
            Analyze this lansones fruit image and identify the specific variety.

            Focus on identifying distinguishing characteristics such as:
            1. Size and shape of the fruit
            2. Texture and color of the skin
            3. Arrangement and appearance of the scales
            4. Flesh characteristics (color, texture, juiciness)
            5. Seed size and number

            Known varieties of lansones include:
            - Longkong: Sweet, juicy variety with thick, rough skin and prominent scales
            - Duku: Sweet and slightly tangy variety with thin, smooth skin
            - Paete: Small to medium-sized fruit with very sweet, translucent flesh
            - Jolo: Large fruit with thick, rough skin and sweet, aromatic flesh

            Provide your response in the following JSON format:
            {
                "variety": "longkong|duku|paete|jolo|unknown",
                "confidenceLevel": float (0.0 to 1.0),
                "characteristics": ["list of observed distinguishing features"],
                "description": "brief description of why this variety was identified"
            }

            If you cannot confidently identify the variety, respond with "unknown" for the variety field.
            Be accurate in your identification and only classify with high confidence.
        """.trimIndent()

        // Initial detection prompt to determine if the image contains lansones
        private val DETECTION_PROMPT = """
            Analyze this image to determine what type of item is shown.

            First, determine if this image contains lansones (Lansium domesticum) fruit or leaves:
            - Lansones fruits are small, round, yellow-brown tropical fruits that grow in clusters
            - Lansones leaves are compound, oval-shaped with prominent veins

            Provide your response in the following JSON format:
            {
                "isLansones": boolean,
                "itemType": "lansones_fruit|lansones_leaves|other",
                "confidence": float (0.0 to 1.0),
                "description": "brief factual description of what is visible in the image"
            }

            Be accurate in your identification. Only classify as lansones if you are confident it matches the botanical characteristics.
        """.trimIndent()

        // Neutral analysis prompt for non-lansones items
        private val NEUTRAL_ANALYSIS_PROMPT = """
            Analyze this image and provide only factual, objective observations without any recommendations, evaluations, or assessments.

            Focus on providing:
            1. Technical specifications and measurable properties
            2. Observable characteristics and features
            3. Quantitative metrics where applicable
            4. Factual descriptions of what is detected

            Provide your response in the following JSON format:
            {
                "diseaseDetected": false,
                "diseaseName": null,
                "confidenceLevel": 1.0,
                "affectedPart": "general",
                "symptoms": [],
                "recommendations": [],
                "severity": "none",
                "observations": ["list of factual observations"],
                "measurements": ["list of measurable properties"],
                "characteristics": ["list of observable features"]
            }

            IMPORTANT: Do not include any subjective judgments, health assessments, safety evaluations, or recommendations.
            Only report factual data and observable characteristics.
        """.trimIndent()
    }
    
    /**
     * Analyzes an image, first detecting if it contains lansones, then applying appropriate analysis
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

                // First, detect if the image contains lansones
                val detectionResult = detectLansonesInImage(imageBytes, mimeType)
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
                    AnalysisType.FRUIT -> performLansonesAnalysis(imageBytes, mimeType, FRUIT_ANALYSIS_PROMPT, actualAnalysisType)
                    AnalysisType.LEAVES -> performLansonesAnalysis(imageBytes, mimeType, LEAF_ANALYSIS_PROMPT, actualAnalysisType)
                    AnalysisType.NON_LANSONES -> performNeutralAnalysis(imageBytes, mimeType, detection.description)
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
        imageBytes: ByteArray,
        mimeType: String
    ): Result<DetectionResult> {
        return try {
            val request = requestBuilder.createImageAnalysisRequest(
                imageBytes = imageBytes,
                mimeType = mimeType,
                prompt = DETECTION_PROMPT
            )

            val apiResult = apiClient.analyzeImage(request)
            if (apiResult.isFailure) {
                return Result.failure(
                    AnalysisException("Detection API call failed: ${apiResult.exceptionOrNull()?.message}")
                )
            }

            val response = apiResult.getOrNull()!!
            val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return Result.failure(AnalysisException("Empty detection response"))

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
        imageBytes: ByteArray,
        mimeType: String,
        prompt: String,
        analysisType: AnalysisType
    ): AnalysisResult {
        // First, perform the main analysis (disease detection)
        val request = requestBuilder.createImageAnalysisRequest(
            imageBytes = imageBytes,
            mimeType = mimeType,
            prompt = prompt
        )

        val apiResult = apiClient.analyzeImage(request)
        if (apiResult.isFailure) {
            throw AnalysisException("Lansones analysis API call failed: ${apiResult.exceptionOrNull()?.message}")
        }

        val response = apiResult.getOrNull()!!
        val mainAnalysisResult = parseGeminiResponse(response, analysisType)
        
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
            val request = requestBuilder.createImageAnalysisRequest(
                imageBytes = imageBytes,
                mimeType = mimeType,
                prompt = VARIETY_DETECTION_PROMPT
            )

            val apiResult = apiClient.analyzeImage(request)
            if (apiResult.isFailure) {
                // Don't fail the entire analysis if variety detection fails
                return null
            }

            val response = apiResult.getOrNull()!!
            val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return null

            // Extract JSON from response text if it's embedded in markdown or other text
            val jsonText = extractJsonFromText(responseText)
            
            val varietyResponse = gson.fromJson(jsonText, VarietyAnalysisResponse::class.java)
            
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
        imageBytes: ByteArray,
        mimeType: String,
        itemDescription: String
    ): AnalysisResult {
        val request = requestBuilder.createImageAnalysisRequest(
            imageBytes = imageBytes,
            mimeType = mimeType,
            prompt = NEUTRAL_ANALYSIS_PROMPT
        )

        val apiResult = apiClient.analyzeImage(request)
        if (apiResult.isFailure) {
            // Fallback to basic neutral result if API fails
            return createNeutralAnalysisResult(itemDescription)
        }

        val response = apiResult.getOrNull()!!
        return parseNeutralResponse(response, itemDescription)
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
     * Parses Gemini API response into structured analysis result
     */
    private fun parseGeminiResponse(
        response: GeminiResponse,
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
        
        return try {
            // Try to parse as JSON first
            parseJsonResponse(responseText, analysisType)
        } catch (e: Exception) {
            // Fallback to text-based parsing
            parseTextResponse(responseText, analysisType)
        }
    }
    
    /**
     * Parses JSON-formatted response from Gemini API
     */
    private fun parseJsonResponse(responseText: String, analysisType: AnalysisType): AnalysisResult {
        // Extract JSON from response text if it's embedded in markdown or other text
        val jsonText = extractJsonFromText(responseText)
        
        return try {
            val jsonResponse = gson.fromJson(jsonText, JsonAnalysisResponse::class.java)
            
            AnalysisResult(
                diseaseDetected = jsonResponse.diseaseDetected,
                diseaseName = jsonResponse.diseaseName,
                confidenceLevel = jsonResponse.confidenceLevel,
                affectedPart = jsonResponse.affectedPart ?: analysisType.name.lowercase(),
                symptoms = jsonResponse.symptoms ?: emptyList(),
                recommendations = jsonResponse.recommendations ?: emptyList(),
                severity = jsonResponse.severity ?: "medium",
                rawResponse = responseText,
                detectedAnalysisType = analysisType
            )
        } catch (e: JsonSyntaxException) {
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
            "bacterial leaf blight",
            "cercospora",
            "phyllosticta"
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
            "bacterial infection" to listOf("bacterial", "soft rot", "oozing")
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
        }
        
        // Default recommendations if none found
        if (recommendations.isEmpty()) {
            recommendations.addAll(listOf(
                "Monitor plant health regularly",
                "Maintain proper growing conditions",
                "Consult with agricultural extension services if symptoms persist"
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
            gson.fromJson(jsonText, DetectionResult::class.java)
        } catch (e: Exception) {
            // Fallback to text-based detection
            val isLansones = responseText.lowercase().let { text ->
                text.contains("lansones") || text.contains("lansium domesticum")
            }
            DetectionResult(
                isLansones = isLansones,
                itemType = if (isLansones) "lansones_fruit" else "other",
                confidence = 0.5f,
                description = "Item detected in image"
            )
        }
    }

    /**
     * Parses neutral analysis response for non-lansones items
     */
    private fun parseNeutralResponse(response: GeminiResponse, itemDescription: String): AnalysisResult {
        val responseText = response.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: return createNeutralAnalysisResult(itemDescription)

        return try {
            val jsonText = extractJsonFromText(responseText)
            val neutralResponse = gson.fromJson(jsonText, NeutralAnalysisResponse::class.java)

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
}

/**
 * Data class representing the result of image analysis
 */
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
data class VarietyAnalysisResult(
    val variety: LansonesVariety,
    val confidenceLevel: Float,
    val characteristics: List<String>,
    val description: String
)

/**
 * Data class for parsing JSON responses from Gemini API
 */
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
data class VarietyAnalysisResponse(
    val variety: String?,
    val confidenceLevel: Float,
    val characteristics: List<String>?,
    val description: String?
)

/**
 * Data class for detection results
 */
data class DetectionResult(
    val isLansones: Boolean,
    val itemType: String,
    val confidence: Float,
    val description: String
)

/**
 * Data class for neutral analysis responses
 */
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