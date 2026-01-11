package net.mcbrawls.fracture.polar.event

class InvalidPolarProfileEvent(
    val checkoutEvent: CheckoutUpdatedEvent,
    val reason: Reason,
) : PolarEvent {
    enum class Reason {
        INVALID_USERNAME,
        PROFILE_NOT_EXIST,
    }
}
