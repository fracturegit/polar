package net.mcbrawls.fracture.polar.webhook

import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps

data class IncomingHandler<T>(
    val codec: Codec<T>,
    val handler: suspend (T) -> Unit,
) {
    @Throws(JsonParseException::class)
    suspend fun handle(json: JsonObject) {
        val event = codec.parse(JsonOps.INSTANCE, json).getOrThrow(::JsonParseException)
        handler.invoke(event)
    }
}
