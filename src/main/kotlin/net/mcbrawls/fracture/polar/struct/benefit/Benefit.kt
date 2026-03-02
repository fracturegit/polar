package net.mcbrawls.fracture.polar.struct.benefit

import com.mojang.serialization.Codec
import com.mojang.serialization.Dynamic
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.codex.UuidCodecs
import net.mcbrawls.fracture.polar.struct.list.ListResponse
import java.util.UUID

data class Benefit(
    val type: String,
    val id: UUID,
    val description: String,
    val metadata: Map<String, Dynamic<*>>,
) {
    companion object {
        val CODEC: Codec<Benefit> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.STRING.fieldOf("type").forGetter(Benefit::type),
                UuidCodecs.CODEC.fieldOf("id").forGetter(Benefit::id),
                Codec.STRING.fieldOf("description").forGetter(Benefit::description),
                Codec.unboundedMap(Codec.STRING, Codec.PASSTHROUGH).optionalFieldOf("metadata", emptyMap()).forGetter(Benefit::metadata),
            ).apply(instance, ::Benefit)
        }

        val LIST_RESPONSE_CODEC: Codec<ListResponse<Benefit>> = ListResponse.createCodec(CODEC)
    }
}
