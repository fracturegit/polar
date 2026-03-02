package net.mcbrawls.fracture.polar.struct.list

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

data class Pagination(
    val totalCount: Int,
    val maxPage: Int,
) {
    companion object {
        val CODEC: Codec<Pagination> = RecordCodecBuilder.create { instance ->
            instance.group(
                Codec.INT.fieldOf("total_count").forGetter(Pagination::totalCount),
                Codec.INT.fieldOf("max_page").forGetter(Pagination::maxPage),
            ).apply(instance, ::Pagination)
        }
    }
}
