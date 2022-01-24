package com.example.buybye.controller

import com.example.buybye.auth.AuthTokenGenerator
import com.example.buybye.domain.candle.Candle
import com.example.buybye.domain.common.MarketUnit
import com.example.buybye.domain.engine.TradeEngine
import com.example.buybye.utils.SlackNotifier
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
    private val slackNotifier: SlackNotifier,
) {

    private val log = LoggerFactory.getLogger(javaClass)

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
    suspend fun getTargetPrice(): Double {
        // 목표가 계산하기
        val candles = getCandles(periodUnit = "days", count = 2)
        val yesterdayCandle = candles.minByOrNull(Candle::timestamp)!!

        val engine = TradeEngine()
        return engine.calculateTargetPrice(
            tradingPrice = yesterdayCandle.trade_price.toDouble(),
            highPrice = yesterdayCandle.high_price.toDouble(),
            lowPrice = yesterdayCandle.low_price.toDouble()
        )
    }

    suspend fun getCurrentPrice(market: MarketUnit = MarketUnit.`KRW-BTC`): Double {
        val jwt = authTokenGenerator.generateJwt()
        val baseUrl = "https://api.upbit.com/v1/ticker?markets=${market.name}"
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
        return responseMap["trade_price"]!!.toDouble()
    }

    suspend fun getMyBalance(currency: String = "KRW"): Long {
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
            .awaitBody<List<Map<String, Any>>>()

        val balance = response
            .first { it["currency"] == currency }["balance"] as String
        return balance.toDouble().toLong()
    }

    //@Scheduled(cron = "0 0 0 * * *")
    @GetMapping("/trade")
    suspend fun trade() = runCatching {
        val market = MarketUnit.`KRW-BTC`
        val currentPrice = getCurrentPrice(market)
        val targetPrice = getTargetPrice()

        val myBalance = getMyBalance()
        if (targetPrice < currentPrice) {
            if (myBalance > 5000) {
                val priceToBuy = myBalance.toDouble() * 0.9995
                order(market, priceToBuy)
                slackNotifier.notify("체결완료 매수금액 : $priceToBuy, 목표가 : $targetPrice, 현재가 : $currentPrice")
            }
        }
        mapOf(
            "current" to currentPrice,
            "targetPrice" to targetPrice,
            "balance" to myBalance,
        )
    }.onFailure {
        log.error(it.message)
        slackNotifier.notify("에러 발생 ${it.localizedMessage}")
    }.getOrNull()

    suspend fun order(market: MarketUnit = MarketUnit.`KRW-BTC`, price: Double): Map<String, Any> {
        val params = mapOf(
            "market" to market.name,
            "side" to "bid",
            "price" to price,
            "ord_type" to "price"
        )
        val queryElements = params.map {
            "${it.key}=${it.value}"
        }
        val queryString = queryElements.toTypedArray().joinToString("&")
        val jwt = authTokenGenerator.generateJwtWithQueryString(queryString)
        return WebClient.builder()
            .baseUrl("https://api.upbit.com/v1/orders")
            .defaultHeaders {
                it[HttpHeaders.AUTHORIZATION] = "Bearer $jwt"
                it[HttpHeaders.CONTENT_TYPE] = "application/json"
            }
            .build()
            .post()
            .bodyValue(params)
            .retrieve()
            .awaitBody()
    }

}