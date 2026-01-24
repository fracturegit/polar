package net.mcbrawls.fracture.polar.webhook

import com.google.gson.JsonObject
import net.mcbrawls.fracture.polar.CustomFields
import net.mcbrawls.fracture.polar.MinecraftUsernameHelper
import net.mcbrawls.fracture.polar.PolarAPI
import net.mcbrawls.fracture.polar.event.CheckoutUpdatedEvent
import net.mcbrawls.fracture.polar.event.CustomerStateChangedEvent
import net.mcbrawls.fracture.polar.event.InvalidPolarProfileEvent
import net.mcbrawls.fracture.polar.event.PolarEvents
import net.mcbrawls.fracture.polar.struct.Checkout
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.jvm.optionals.getOrNull

class WebhookRequestHandler(
    private val api: PolarAPI,
) {
    private val logger: Logger = LoggerFactory.getLogger(WebhookRequestHandler::class.java)

    private val types: Map<String, IncomingHandler<*>> = buildMap {
        this["checkout.updated"] = IncomingHandler(CheckoutUpdatedEvent.CODEC, ::handleCheckoutUpdated)
        this["customer.state_changed"] = IncomingHandler(CustomerStateChangedEvent.CODEC, ::handleCustomerStateChanged)
    }

    suspend fun handle(type: String?, json: JsonObject) {
        val handler = types[type] ?: return
        handler.handle(json) { throw it }
    }

    fun handleCheckoutUpdated(event: CheckoutUpdatedEvent) {
        logger.debug("Checkout updated: {}", event)

        val checkout = event.checkout
        if (checkout.status == Checkout.Status.SUCCEEDED) {
            patchExternalId(checkout, event)
        }

        PolarEvents.emit(CheckoutUpdatedEvent::class, event)
    }

    private fun patchExternalId(checkout: Checkout, event: CheckoutUpdatedEvent) {
        // check for customer id
        val customerId = checkout.customerId.getOrNull() ?: return // when does that happen??

        // get customer state
        val customerState = api.getCustomerState(customerId)

        // check for existing customer id
        if (customerState.externalId.isPresent) return

        logger.info("No external id found: $customerId. Patching.")

        // get minecraft username
        val customData = checkout.customFieldData
        val minecraftUsername = customData[CustomFields.MINECRAFT_USERNAME]
            ?: error("No minecraft username provided")
        if (!MinecraftUsernameHelper.validateMinecraftUsername(minecraftUsername)) {
            PolarEvents.emit(
                InvalidPolarProfileEvent::class,
                InvalidPolarProfileEvent(event, InvalidPolarProfileEvent.Reason.INVALID_USERNAME)
            )
            error("Invalid Minecraft username")
        }

        // get minecraft uuid from username
        val minecraftUuid = MinecraftUsernameHelper.getPlayerUuid(minecraftUsername)
        if (minecraftUuid == null) {
            PolarEvents.emit(
                InvalidPolarProfileEvent::class,
                InvalidPolarProfileEvent(event, InvalidPolarProfileEvent.Reason.PROFILE_NOT_EXIST)
            )
            error("Username does not match to a profile")
        }

        // patch
        logger.info("Paired $customerId given username $minecraftUsername to $minecraftUuid.")

        runCatching {
            api.patchExternalId(customerId, minecraftUuid)
        }.onFailure {
            PolarEvents.emit(
                InvalidPolarProfileEvent::class,
                InvalidPolarProfileEvent(event, InvalidPolarProfileEvent.Reason.USERNAME_REGISTERED)
            )
        }
    }

    fun handleCustomerStateChanged(event: CustomerStateChangedEvent) {
        logger.debug("Customer state changed: {}", event)
        PolarEvents.emit(CustomerStateChangedEvent::class, event)
    }
}
