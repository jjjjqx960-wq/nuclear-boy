package com.nuclearboy.api.deepseek

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private val providerModelListJson = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

internal fun parseProviderModelIds(body: String): List<String> {
    if (body.isBlank()) return emptyList()
    val root = runCatching { providerModelListJson.parseToJsonElement(body) }.getOrNull()
        ?: return emptyList()
    val items = root.providerModelItems()
    return items
        .mapNotNull { it.providerModelId() }
        .map(::sanitizeProviderModelName)
        .filter { it.isNotBlank() }
        .distinct()
}

private fun JsonElement.providerModelItems(): List<JsonElement> =
    when (this) {
        is JsonArray -> this.toList()
        is JsonObject -> {
            val container = this["data"] ?: this["models"] ?: this["items"]
            when (container) {
                is JsonArray -> container.toList()
                is JsonObject -> container.values.toList()
                else -> emptyList()
            }
        }
        else -> emptyList()
    }

private fun JsonElement.providerModelId(): String? =
    when (this) {
        is JsonPrimitive -> jsonPrimitive.contentOrNull
        is JsonObject -> {
            val fields = listOf("id", "name", "model")
            fields.firstNotNullOfOrNull { key ->
                (this[key] as? JsonPrimitive)?.jsonPrimitive?.contentOrNull
            }
        }
        else -> null
    }
