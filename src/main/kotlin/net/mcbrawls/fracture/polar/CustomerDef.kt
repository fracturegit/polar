package net.mcbrawls.fracture.polar

import java.util.UUID

sealed interface CustomerDef {
    val urlComponent: String

    data class Polar(val uuid: UUID) : CustomerDef {
        override val urlComponent: String = "$uuid"
    }

    data class External(val uuid: UUID) : CustomerDef {
        override val urlComponent: String = "external/$uuid"
    }
}
