package com.ml.lansonesscan.data.remote.dto

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Custom Gson serializer for RequestPart sealed class
 */
class RequestPartSerializer : JsonSerializer<RequestPart> {
    override fun serialize(
        src: RequestPart,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        return when (src) {
            is RequestPart.TextPart -> {
                JsonObject().apply {
                    addProperty("text", src.text)
                }
            }
            is RequestPart.InlineDataPart -> {
                JsonObject().apply {
                    add("inlineData", context.serialize(src.inlineData))
                }
            }
        }
    }
}

/**
 * Custom Gson deserializer for RequestPart sealed class
 */
class RequestPartDeserializer : JsonDeserializer<RequestPart> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): RequestPart {
        val jsonObject = json.asJsonObject
        
        return when {
            jsonObject.has("text") -> {
                RequestPart.TextPart(jsonObject.get("text").asString)
            }
            jsonObject.has("inlineData") -> {
                val inlineData = context.deserialize<InlineData>(
                    jsonObject.get("inlineData"),
                    InlineData::class.java
                )
                RequestPart.InlineDataPart(inlineData)
            }
            else -> throw JsonParseException("Unknown RequestPart type")
        }
    }
}