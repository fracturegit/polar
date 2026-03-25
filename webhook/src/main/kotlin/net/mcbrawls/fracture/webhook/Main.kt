package net.mcbrawls.fracture.webhook

import com.google.gson.Gson
import com.google.gson.JsonObject
import io.ktor.http.HttpStatusCode
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.mcbrawls.fracture.polar.webhook.PolarWebhookEngine
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object Main {
    private val logger = LoggerFactory.getLogger(Main::class.java)
    private val gson = Gson()
    private val httpClient = HttpClient.newHttpClient()

    @JvmStatic
    fun main(args: Array<String>) {
        val secret = System.getenv("POLAR_SECRET") ?: error("No POLAR_SECRET env var")
        val port = System.getenv("PORT_WEBHOOK")?.toInt() ?: 8090
        val forwardUrl = System.getenv("POLAR_FORWARD_URL")

        logger.info("Starting Polar webhook relay on port $port")
        if (forwardUrl != null) {
            logger.info("Forwarding verified events to: $forwardUrl")
        }

        embeddedServer(Netty, port) {
            routing {
                post("/polar") {
                    val body = call.receiveChannel().toByteArray()
                    val headers = call.request.headers

                    val webhookId = headers["Webhook-Id"]
                    val timestamp = headers["Webhook-Timestamp"]
                    val signature = headers["Webhook-Signature"]

                    if (webhookId == null || timestamp == null || signature == null) {
                        call.respond(HttpStatusCode.BadRequest)
                        return@post
                    }

                    if (!PolarWebhookEngine.verifyWebhook(secret, body, webhookId, timestamp, signature)) {
                        logger.warn("Rejected webhook $webhookId: invalid signature")
                        call.respond(HttpStatusCode.Unauthorized)
                        return@post
                    }

                    // Respond 200 immediately so Polar never disables the webhook endpoint
                    call.respond(HttpStatusCode.OK)

                    if (forwardUrl != null) {
                        val eventType = runCatching {
                            gson.fromJson(body.decodeToString(), JsonObject::class.java)["type"]?.asString
                        }.getOrNull() ?: "unknown"

                        withContext(Dispatchers.IO) {
                            forwardToServer(forwardUrl, body, webhookId, timestamp, signature, eventType)
                        }
                    }
                }
            }
        }.start(wait = true)
    }

    private fun forwardToServer(url: String, body: ByteArray, webhookId: String, timestamp: String, signature: String, eventType: String) {
        runCatching {
            val request = HttpRequest.newBuilder(URI(url))
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .header("Content-Type", "application/json")
                .header("Webhook-Id", webhookId)
                .header("Webhook-Timestamp", timestamp)
                .header("Webhook-Signature", signature)
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            logger.info("Forwarded $eventType ($webhookId) -> ${response.statusCode()}")
        }.onFailure { error ->
            logger.debug("Game server offline, dropping $eventType ($webhookId)", error)
        }
    }

}
