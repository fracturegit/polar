package net.mcbrawls.fracture.polar.struct.customer

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.codex.UuidCodecs
import net.mcbrawls.fracture.polar.struct.BenefitGrant
import java.util.Optional
import java.util.UUID

data class CustomerState(
    val id: UUID,
    val externalId: Optional<String>,
    val grantedBenefits: List<BenefitGrant>,
) {
    companion object {
        val CODEC: Codec<CustomerState> = RecordCodecBuilder.create { instance ->
            instance.group(
                UuidCodecs.CODEC.fieldOf("id").forGetter(CustomerState::id),
                Codec.STRING.optionalFieldOf("external_id").forGetter(CustomerState::externalId),
                BenefitGrant.CODEC.listOf().fieldOf("granted_benefits").forGetter(CustomerState::grantedBenefits),
            ).apply(instance, ::CustomerState)
        }
    }
}
