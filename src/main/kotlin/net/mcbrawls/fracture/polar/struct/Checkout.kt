package net.mcbrawls.fracture.polar.struct

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import net.mcbrawls.codex.UuidCodecs
import java.util.Optional
import java.util.UUID

data class Checkout(
    /**
     * The checkout status.
     */
    val status: Status,

    /**
     * The checkout URL.
     */
    val url: String,

    /**
     * The internal customer id from Polar.
     */
    val customerId: Optional<UUID>,

    /**
     * The external customer id registered with Polar.
     */
    val externalCustomerId: Optional<String>,

    /**
     * A key-value map of the custom fields provided.
     */
    val customFieldData: Map<String, String>,

    /**
     * The final amount of money spent.
     */
    val totalAmount: Int,

    /**
     * A string representation of the currency used.
     */
    val currency: String,

    /**
     * The purchased product.
     */
    val product: Product,

    /**
     * The available selection of products.
     */
    val products: List<Product>,
) {
    enum class Status(val id: String) {
        /**
         * The checkout session was opened.
         */
        OPEN("open"),

        /**
         * The checkout session was expired and is no more accessible.
         */
        EXPIRED("expired"),

        /**
         * The user on the checkout session clicked Pay. This is not indicative of the payment's success status.
         */
        CONFIRMED("confirmed"),

        /**
         * The checkout definitely failed for technical reasons and cannot be retried.
         * In most cases, this state is never reached.
         */
        FAILED("failed"),

        /**
         * The payment on the checkout was performed successfully.
         */
        SUCCEEDED("succeeded");

        companion object {
            val byId: Map<String, Status> = entries.associateBy(Status::id)
            val CODEC: Codec<Status> = Codec.STRING.xmap(byId::get, Status::id)
        }
    }

    data class Product(
        val id: UUID,
        val name: String,
    ) {
        companion object {
            val CODEC: Codec<Product> = RecordCodecBuilder.create { instance ->
                instance.group(
                    UuidCodecs.CODEC.fieldOf("id").forGetter(Product::id),
                    Codec.STRING.fieldOf("name").forGetter(Product::name),
                ).apply(instance, ::Product)
            }
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
                Product.CODEC.fieldOf("product").forGetter(Checkout::product),
                Product.CODEC.listOf().fieldOf("products").forGetter(Checkout::products),
            ).apply(instance, ::Checkout)
        }
    }
}
