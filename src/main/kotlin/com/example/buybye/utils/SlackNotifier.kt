package com.example.buybye.utils

import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class SlackNotifier(
    @Value("\${slack.noti-url}") private val slackUrl: String,
    @Value("\${slack.errornoti-url}") private val errorSlackUrl: String,
) {

    suspend fun notify(msg: String, url: String = slackUrl) {
        WebClient.builder()
            .baseUrl(url)
            .build()
            .post()
            .bodyValue(mapOf("text" to msg))
            .retrieve()
            .awaitBody<String>()
    }

    fun notifyBlock(msg: String) {
        runBlocking {
            notify(msg)
        }
    }

    suspend fun error(msg: String) {
        notify(msg, errorSlackUrl)
    }
}