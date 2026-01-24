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
class PolarWebhookEngine(private val secret: String, polarApiKey: String, polarApiUrl: String) {
    private val logger: Logger = LoggerFactory.getLogger(PolarWebhookEngine::class.java)
    private val gson: Gson = Gson()

    val api = PolarAPI(polarApiKey, polarApiUrl)

    private val webhookRequestHandler = WebhookRequestHandler(api)

    lateinit var server: EmbeddedServer<*, *>
        private set

    @OptIn(DelicateCoroutinesApi::class)
    fun initialize(port: Int) {
        logger.info("Starting polar webhook server on port $port")

        server = embeddedServer(Netty, port) {
            routing {
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
        val fullSignature = headers["Webhook-Signature"] ?: return false

        val keyBytes = secret.toByteArray(Charsets.UTF_8)

        val signedPayload = "$id.$timestamp.${String(body, Charsets.UTF_8)}"

        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(keyBytes, "HmacSHA256"))
        val computed = mac.doFinal(signedPayload.toByteArray(Charsets.UTF_8))

        val computedBase64 = Base64.getEncoder().encodeToString(computed)
        val signature = fullSignature.substringAfter("v1,")

        return constantTimeEquals(computedBase64, signature)
    }

    fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var result = 0
        for (i in a.indices) {
            result = result or (a[i].code xor b[i].code)
        }
        return result == 0
    }
}
