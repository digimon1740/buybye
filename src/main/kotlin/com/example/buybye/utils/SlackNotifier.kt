package com.example.buybye.utils

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class SlackNotifier(
    @Value("\${slack.noti-url}") private val slackUrl: String,
) {

    suspend fun notify(msg: String) {
        WebClient.builder()
            .baseUrl(slackUrl)
            .build()
            .post()
            .bodyValue(mapOf("text" to msg))
            .retrieve()
            .awaitBody<String>()
    }
}