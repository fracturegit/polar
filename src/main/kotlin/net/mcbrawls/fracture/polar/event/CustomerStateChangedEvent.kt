package net.mcbrawls.fracture.polar.event

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.codex.TimeCodecs
import net.mcbrawls.fracture.polar.struct.customer.Customer
import kotlin.time.Instant

data class CustomerStateChangedEvent(
    val timestamp: Instant,
    val data: Customer,
) : PolarEvent {
    companion object {
        val CODEC: Codec<CustomerStateChangedEvent> = RecordCodecBuilder.create { instance ->
            instance.group(
                TimeCodecs.INSTANT.fieldOf("timestamp").forGetter(CustomerStateChangedEvent::timestamp),
                Customer.CODEC.fieldOf("data").forGetter(CustomerStateChangedEvent::data),
            ).apply(instance, ::CustomerStateChangedEvent)
        }
    }
}
