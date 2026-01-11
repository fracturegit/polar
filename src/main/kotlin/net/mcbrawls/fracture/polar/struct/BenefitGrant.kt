package net.mcbrawls.fracture.polar.struct

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.codex.UuidCodecs
import java.util.UUID

data class BenefitGrant(
    val benefitId: UUID,
) {
    companion object {
        val CODEC: Codec<BenefitGrant> = RecordCodecBuilder.create { instance ->
            instance.group(
                UuidCodecs.CODEC.fieldOf("benefit_id").forGetter(BenefitGrant::benefitId),
            ).apply(instance, ::BenefitGrant)
        }
    }
}
