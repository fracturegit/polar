package net.mcbrawls.fracture.polar

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.mojang.serialization.JsonOps
import net.mcbrawls.codex.decodeQuick
import net.mcbrawls.fracture.polar.struct.mojang.PlayerNameResponse
import java.net.URI
import java.util.UUID

object MinecraftProfiles {
    private val gson = Gson()
    private val usernameRegex: Regex = "^[a-zA-Z0-9_]{3,16}$".toRegex()

    fun validateMinecraftUsername(username: String): Boolean {
        val regex = usernameRegex
        return regex.matches(username)
    }

    fun getProfile(query: Query): PlayerNameResponse? {
        return runCatching {
            val uri = URI("https://api.mojang.com/minecraft/profile/lookup/${query.suffix}")
            val text = uri.toURL().readText()
            val json = gson.fromJson(text, JsonObject::class.java)
            PlayerNameResponse.CODEC.decodeQuick(JsonOps.INSTANCE, json)
        }.getOrNull()
    }

    fun getUuid(username: String): UUID? {
        return getProfile(Query.ByName(username))?.id
    }

    fun getUsername(uuid: UUID): String? {
        return getProfile(Query.ByUuid(uuid))?.name
    }

    sealed class Query(val suffix: String) {
        class ByName(username: String) : Query("name/$username")
        class ByUuid(uuid: UUID) : Query(uuid.toString())
    }
}
