package net.mcbrawls.fracture.polar.webhook

import io.github.rybalkinsd.kohttp.dsl.httpPost
import io.github.rybalkinsd.kohttp.ext.url
import okhttp3.Response
import org.apache.commons.text.StringEscapeUtils

class DiscordWebhookHandler(url: String) {
    private val webhookUrl = "https://discord.com/api/webhooks/$url"

    fun send(content: String, profile: Profile, callback: (Response) -> Unit = { }) {
        httpPost {
            url(webhookUrl)

            body {
                json {
                    "content" to StringEscapeUtils.escapeJson(content)
                    "username" to profile.username

                    profile.avatarUrl?.let { avatarUrl ->
                        "avatar_url" to avatarUrl
                    }
                }
            }
        }.use(callback)
    }

    data class Profile(
        val username: String,
        val avatarUrl: String? = null,
    )
}
