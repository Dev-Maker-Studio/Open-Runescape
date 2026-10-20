package com.darkan.bot

import kotlinx.serialization.json.*

internal fun text(value: String?) = JsonPrimitive(value)
internal fun number(value: Int) = JsonPrimitive(value)
internal fun list(items: Iterable<JsonElement>) = JsonArray(items.toList())
internal fun entry(vararg fields: Pair<String, JsonElement>) = JsonObject(mapOf(*fields))
