package net.mcbrawls.fracture.polar.event

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.codex.TimeCodecs
import net.mcbrawls.fracture.polar.struct.Checkout
import kotlin.time.Instant

data class CheckoutUpdatedEvent(
    val timestamp: Instant,
    val checkout: Checkout,
) : PolarEvent {
    companion object {
        val CODEC: Codec<CheckoutUpdatedEvent> = RecordCodecBuilder.create { instance ->
            instance.group(
                TimeCodecs.INSTANT.fieldOf("timestamp").forGetter(CheckoutUpdatedEvent::timestamp),
                Checkout.CODEC.fieldOf("data").forGetter(CheckoutUpdatedEvent::checkout),
            ).apply(instance, ::CheckoutUpdatedEvent)
        }
    }
}
