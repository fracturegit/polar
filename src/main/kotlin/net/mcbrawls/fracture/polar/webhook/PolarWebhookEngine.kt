package net.mcbrawls.fracture.polar.webhook

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.http.Headers
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.mcbrawls.fracture.polar.PolarAPI
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/*
    - webhook to handle purchase event
           - THIS contains information regarding the modified purchase (state_changed) and other info
           - CHECK webhook for customer metadata minecraft account uuid
           - IF no uuid present, fetch UUID from provided username & PATCH to polar metadata
                - IF username cannot be parsed to a uuid, panic and notify an admin, who can contact the customer or refund

                UPDATE: POLAR has custom external id field
 */
class PolarWebhookEngine(
    private val secret: String,
    organizationId: UUID,
    apiKey: String,
    apiUrl: String
) {
    private val logger: Logger = LoggerFactory.getLogger(PolarWebhookEngine::class.java)
    private val gson: Gson = Gson()

    val api = PolarAPI(apiKey, apiUrl, organizationId)

    private val webhookRequestHandler = WebhookRequestHandler(api)

    lateinit var server: EmbeddedServer<*, *>
        private set

    @OptIn(DelicateCoroutinesApi::class)
    fun initialize(port: Int) {
        logger.info("Starting polar webhook server on port $port")

        server = embeddedServer(Netty, port) {
            routing {
                get("/") {
                    call.respond(HttpStatusCode.OK)
                }

                post("/polar") { handleRequest() }
            }
        }

        GlobalScope.launch {
            server.start(wait = true)
        }
    }

    private suspend fun RoutingContext.handleRequest() {
        val request = call.request
        val body = call.receiveChannel().toByteArray()
        val bodyText = body.decodeToString()

        try {
            if (!verifyWebhook(body, request.headers)) {
                call.respond(HttpStatusCode.Unauthorized, "Invalid signature")
                return
            }

            call.respond(HttpStatusCode.OK, "Request handled")

            logger.info("Handling Polar request")

            try {
                val json = gson.fromJson(bodyText, JsonObject::class.java)
                val type = json["type"]?.asString

                webhookRequestHandler.handle(type, json)
            } catch (exception: IllegalStateException) {
                logger.error("Failure handling request: {}", exception.message)
            }
        } catch (throwable: Throwable) {
            logger.error("Invalid request from Polar", throwable)
        }
    }

    fun verifyWebhook(body: ByteArray, headers: Headers): Boolean {
        val id = headers["Webhook-Id"] ?: return false
        val timestamp = headers["Webhook-Timestamp"] ?: return false
        val signature = headers["Webhook-Signature"] ?: return false
        return verifyWebhook(secret, body, id, timestamp, signature)
    }

    companion object {
        fun verifyWebhook(secret: String, body: ByteArray, id: String, timestamp: String, fullSignature: String): Boolean {
            val signedPayload = "$id.$timestamp.${String(body, Charsets.UTF_8)}"
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val computedBase64 = Base64.getEncoder().encodeToString(mac.doFinal(signedPayload.toByteArray(Charsets.UTF_8)))
            val signature = fullSignature.substringAfter("v1,")
            if (computedBase64.length != signature.length) return false
            var result = 0
            for (i in computedBase64.indices) result = result or (computedBase64[i].code xor signature[i].code)
            return result == 0
        }
    }
}
