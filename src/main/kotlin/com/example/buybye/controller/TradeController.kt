package com.example.buybye.controller

import com.example.buybye.auth.AuthTokenGenerator
import com.example.buybye.domain.candle.Candle
import com.example.buybye.domain.engine.TradeEngine
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import java.time.LocalDateTime

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
        @RequestParam(defaultValue = "1") minuteUnit: String = "1",
        @RequestParam(defaultValue = "KRW-BTC") market: String = "KRW-BTC",
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

        val avg = response.sumOf { it.trade_price.toDouble() } / count

        return mapOf("avg of 5" to avg)
    }

    // 매수 목표가 조회
    @GetMapping("/v1/target-price")
    suspend fun getTargetPrice(
        @RequestParam(defaultValue = "KRW-BTC") market: String,
    ): Map<String, Any?> {
        val jwt = authTokenGenerator.generateJwt()
        val baseUrl = "https://api.upbit.com/v1/ticker?markets=${market}"
        val response = WebClient.builder()
            .baseUrl(baseUrl)
            .defaultHeaders {
                it[HttpHeaders.AUTHORIZATION] = "Bearer $jwt"
                it[HttpHeaders.CONTENT_TYPE] = "application/json"
            }
            .build()
            .get()
            .retrieve()
            .awaitBody<List<Map<String, String>>>()

        val responseMap = response.first()
        // 현재 종가
        // TODO 반복 주기 필요
        val currentTradePrice = responseMap["trade_price"]

        // 목표가 계산하기
        val candles = getCandles(periodUnit = "days", count = 2)
        val yesterdayCandle = candles.minByOrNull(Candle::timestamp)!!

        val now = LocalDateTime.now()
        val mid = LocalDateTime.of(now.year, now.month, now.dayOfMonth, 0, 0, 0).plusDays(1)
        val engine = TradeEngine()
        val targetPrice = engine.calculateTargetPrice(
            openingPrice = yesterdayCandle.opening_price.toDouble(),
            highPrice = yesterdayCandle.high_price.toDouble(),
            lowPrice = yesterdayCandle.low_price.toDouble()
        )
        return mapOf(
            "currentTradePrice" to currentTradePrice,
            "targetPrice" to targetPrice,
            "now" to now,
            "mid" to mid
        )
    }

}