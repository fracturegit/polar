package net.mcbrawls.fracture.polar.struct.customer

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.codex.UuidCodecs
import java.util.Optional
import java.util.UUID

data class Customer(
    val id: UUID,
    val externalId: Optional<String>,
) {
    companion object {
        val CODEC: Codec<Customer> = RecordCodecBuilder.create { instance ->
            instance.group(
                UuidCodecs.CODEC.fieldOf("id").forGetter(Customer::id),
                Codec.STRING.optionalFieldOf("external_id").forGetter(Customer::externalId),
            ).apply(instance, ::Customer)
        }
    }
}
