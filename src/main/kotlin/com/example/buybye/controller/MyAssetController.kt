package com.example.buybye.controller

import com.example.buybye.auth.AuthTokenGenerator
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@RestController
class MyAssetController(
    private val authTokenGenerator: AuthTokenGenerator,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/v1/assets")
    suspend fun getMyAsset(): String {
        val jwt = authTokenGenerator.generateJwt()
        val response = WebClient.builder()
            .baseUrl("https://api.upbit.com/v1/accounts")
            .defaultHeaders {
                it[HttpHeaders.AUTHORIZATION] = "Bearer $jwt"
                it[HttpHeaders.CONTENT_TYPE] = "application/json"
            }
            .build()
            .get()
            .retrieve()
            .awaitBody<String>()
        return response

    }

}