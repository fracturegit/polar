package net.mcbrawls.fracture.polar.struct

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.codex.UuidCodecs
import java.util.Optional
import java.util.UUID

data class Checkout(
    val status: Status,
    val url: String,
    val customerId: Optional<UUID>,
    val externalCustomerId: Optional<String>,
    val customFieldData: Map<String, String>,
    val totalAmount: Int,
    val currency: String,
) {
    enum class Status(val id: String) {
        OPEN("open"),
        EXPIRED("expired"),
        CONFIRMED("confirmed"),
        FAILED("failed"),
        SUCCEEDED("succeeded");

        companion object {
            val byId: Map<String, Status> = entries.associateBy(Status::id)
            val CODEC: Codec<Status> = Codec.STRING.xmap(byId::get, Status::id)
        }
    }

    companion object {
        val CODEC: Codec<Checkout> = RecordCodecBuilder.create { instance ->
            instance.group(
                Status.CODEC.fieldOf("status").forGetter(Checkout::status),
                Codec.STRING.fieldOf("url").forGetter(Checkout::url),
                UuidCodecs.CODEC.optionalFieldOf("customer_id").forGetter(Checkout::customerId),
                Codec.STRING.optionalFieldOf("external_customer_id").forGetter(Checkout::externalCustomerId),
                Codec.unboundedMap(Codec.STRING, Codec.STRING).orElseGet(::emptyMap).fieldOf("custom_field_data").forGetter(Checkout::customFieldData),
                Codec.INT.fieldOf("total_amount").forGetter(Checkout::totalAmount),
                Codec.STRING.fieldOf("currency").forGetter(Checkout::currency),
            ).apply(instance, ::Checkout)
        }
    }
}
