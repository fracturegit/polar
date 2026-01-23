package net.mcbrawls.fracture.polar

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.mojang.serialization.Codec
import com.mojang.serialization.JsonOps
import io.github.rybalkinsd.kohttp.dsl.context.HttpContext
import io.github.rybalkinsd.kohttp.dsl.httpGet
import io.github.rybalkinsd.kohttp.dsl.httpPatch
import io.github.rybalkinsd.kohttp.dsl.httpPost
import io.github.rybalkinsd.kohttp.ext.url
import net.mcbrawls.fracture.polar.struct.Checkout
import net.mcbrawls.fracture.polar.struct.customer.Customer
import net.mcbrawls.fracture.polar.struct.customer.CustomerState
import okhttp3.Response
import java.net.URI
import java.net.URL
import java.util.UUID

class PolarAPI(private val secret: String, private val rootUrl: String) {
    fun patchExternalId(customerUuid: UUID, playerId: UUID): Customer {
        return result(Customer.CODEC, httpPatch {
            url(createUrl("customers/$customerUuid"))

            authorize()

            body {
                json {
                    "external_id" to playerId.toString()
                }
            }
        })
    }

    fun getCustomerId(playerId: UUID): UUID {
        val customer = result(Customer.CODEC, httpGet {
            url(createUrl("customers/external/$playerId"))
            authorize()
        })

        return customer.id
    }

    fun getCustomerState(id: UUID): CustomerState {
        return result(CustomerState.CODEC, httpGet {
            url(createUrl("customers/$id/state"))
            authorize()
        })
    }

    fun createCheckoutSession(username: String?, products: Set<UUID>): Checkout {
        return result(Checkout.CODEC, httpPost {
            url(createUrl("checkouts/"))
            authorize()

            body {
                json {
                    "products" to products.toList()

                    username?.let { username ->
                        "custom_field_data" to json {
                            CustomFields.MINECRAFT_USERNAME to username
                        }
                    }
                }
            }
        })
    }

    private fun <T> result(codec: Codec<T>, response: Response): T {
        return response.use { response ->
            val code = response.code()
            if (code !in 200..299) error("Invalid response: $response")

            val bodyObj = response.body() ?: error("No body")

            val body = bodyObj.string()
            val json = gson.fromJson(body, JsonObject::class.java)
            codec.parse(JsonOps.INSTANCE, json).getOrThrow(::JsonParseException)
        }
    }

    private fun HttpContext.authorize() {
        header {
            "Authorization" to "Bearer $secret"
        }
    }

    private fun createUrl(str: String): URL {
        return URI("$rootUrl/$str").toURL()
    }

    companion object {
        private val gson: Gson = Gson()
    }
}
