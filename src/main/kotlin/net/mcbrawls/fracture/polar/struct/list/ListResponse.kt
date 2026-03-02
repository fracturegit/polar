package net.mcbrawls.fracture.polar.struct.list

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder

class ListResponse<T>(
    val items: List<T>,
    val pagination: Pagination,
) {
    companion object {
        fun <T> createCodec(elementCodec: Codec<T>): Codec<ListResponse<T>> {
            return RecordCodecBuilder.create { instance ->
                instance.group(
                    elementCodec.listOf().fieldOf("items").forGetter(ListResponse<T>::items),
                    Pagination.CODEC.fieldOf("pagination").forGetter(ListResponse<T>::pagination),
                ).apply(instance, ::ListResponse)
            }
        }
    }
}
