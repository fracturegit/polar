package net.mcbrawls.fracture.polar.struct.mojang

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.codex.UuidCodecs
import java.util.UUID

data class PlayerNameResponse(
    val name: String,
    val id: UUID,
) {
    companion object {
        val CODEC: Codec<PlayerNameResponse> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("name").forGetter(PlayerNameResponse::name),
                UuidCodecs.CODEC.fieldOf("id").forGetter(PlayerNameResponse::id),
            ).apply(instance, ::PlayerNameResponse)
        }
    }
}
