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
import net.mcbrawls.fracture.polar.struct.benefit.Benefit
import net.mcbrawls.fracture.polar.struct.customer.Customer
import net.mcbrawls.fracture.polar.struct.customer.CustomerState
import net.mcbrawls.fracture.polar.struct.list.ListResponse
import net.mcbrawls.fracture.polar.struct.product.Product
import okhttp3.Response
import java.net.URI
import java.net.URL
import java.util.UUID

class PolarAPI(
    private val secret: String,
    private val rootUrl: String,
    private val organizationId: UUID,
) {
    fun patchExternalIdByCustomerId(id: UUID, externalId: UUID): Customer {
        return result(Customer.CODEC, httpPatch {
            url(createUrl("customers/$id"))

            authorize()

            body {
                json {
                    "external_id" to externalId.toString()
                }
            }
        })
    }

    fun getCustomerIdByExternalId(externalId: UUID): UUID {
        val customer = result(Customer.CODEC, httpGet {
            url(createUrl("customers/external/$externalId"))
            authorize()
        })

        return customer.id
    }

    fun getCustomerState(def: CustomerDef): CustomerState {
        val urlComponent = def.urlComponent
        return result(CustomerState.CODEC, httpGet {
            url(createUrl("customers/$urlComponent/state"))
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

    fun getProducts(): List<Product> {
        return getListResponse("products", Product.LIST_RESPONSE_CODEC)
    }

    fun getBenefits(): List<Benefit> {
        return getListResponse("benefits", Benefit.LIST_RESPONSE_CODEC)
    }

    private fun <T> getListResponse(url: String, listResponseCodec: Codec<ListResponse<T>>): List<T> {
        val all = mutableListOf<T>()
        var page = 1

        while (true) {
            val response = getListResponsePage(url, listResponseCodec, page)
            all.addAll(response.items)
            if (page >= response.pagination.maxPage) break
            page++
        }

        return all
    }

    private fun <T> getListResponsePage(url: String, listResponseCodec: Codec<ListResponse<T>>, page: Int): ListResponse<T> {
        return result(listResponseCodec, httpGet {
            url(createUrl(url))
            authorize()

            param {
                "organization_id" to organizationId.toString()
                "page" to page
                "limit" to 100
            }
        })
    }

    private fun <T> result(codec: Codec<T>, response: Response): T {
        return response.use { response ->
            val code = response.code()
            if (code !in 200..299) error("Invalid response: $response, ${response.body()?.string()}")

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
