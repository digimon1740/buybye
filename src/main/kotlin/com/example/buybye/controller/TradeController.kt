package com.example.buybye.controller

import com.example.buybye.auth.AuthTokenGenerator
import com.example.buybye.domain.candle.Candle
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@RestController
class TradeController(
    private val authTokenGenerator: AuthTokenGenerator,
    private val objectMapper: ObjectMapper,
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


    @GetMapping("/v1/candles")
    suspend fun getCandles(
        @RequestParam(defaultValue = "minutes") periodUnit: String,
        @RequestParam(defaultValue = "1") minuteUnit: String,
        @RequestParam(defaultValue = "KRW-BTC") market: String,
        @RequestParam(defaultValue = "200") count: Int,
    ): List<Candle> {
        val jwt = authTokenGenerator.generateJwt()
        val url = if (periodUnit == "minutes") {
            "https://api.upbit.com/v1/candles/${periodUnit}/${minuteUnit}"
        } else "https://api.upbit.com/v1/candles/${periodUnit}"

        val baseUrl = url + "?market=${market}&count=${count}"
        val response = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeaders {
                it[HttpHeaders.AUTHORIZATION] = "Bearer $jwt"
                it[HttpHeaders.CONTENT_TYPE] = "application/json"
            }
            .build()
            .get()
            .retrieve()
            .awaitBody<List<Candle>>()
        return response
    }

    // 매수 목표가 조회

    // 이동 평균 조회
    @GetMapping("/v1/ma")
    suspend fun getMovingAverage(
        @RequestParam(defaultValue = "days") periodUnit: String,
        @RequestParam(defaultValue = "KRW-BTC") market: String,
        @RequestParam(defaultValue = "5") count: Int,
    ): Map<String, Any> {
        val jwt = authTokenGenerator.generateJwt()
        val baseUrl = "https://api.upbit.com/v1/candles/${periodUnit}?market=${market}&count=${count}"
        val response = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeaders {
                it[HttpHeaders.AUTHORIZATION] = "Bearer $jwt"
                it[HttpHeaders.CONTENT_TYPE] = "application/json"
            }
            .build()
            .get()
            .retrieve()
            .awaitBody<List<Candle>>()

        val avg = response.sumOf { it.trade_price } / count

        return mapOf("avg of 5" to avg)
    }

}