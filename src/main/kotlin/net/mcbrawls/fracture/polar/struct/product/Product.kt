package net.mcbrawls.fracture.polar.struct.product

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.codex.UuidCodecs
import net.mcbrawls.fracture.polar.struct.list.ListResponse
import java.util.UUID

data class Product(
    val id: UUID,
    val name: String,
) {
    override fun toString(): String {
        return name
    }

    companion object {
        val CODEC: Codec<Product> = RecordCodecBuilder.create { instance ->
            instance.group(
                UuidCodecs.CODEC.fieldOf("id").forGetter(Product::id),
                Codec.STRING.fieldOf("name").forGetter(Product::name),
            ).apply(instance, ::Product)
        }

        val LIST_RESPONSE_CODEC: Codec<ListResponse<Product>> = ListResponse.createCodec(CODEC)
    }
}
