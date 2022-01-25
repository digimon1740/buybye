package com.example.buybye.domain.engine

import com.example.buybye.auth.AuthTokenGenerator
import com.example.buybye.domain.candle.Candle
import com.example.buybye.domain.common.MarketUnit
import com.example.buybye.domain.common.OrderType
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody

@Component
class TradeEngine(
    private val authTokenGenerator: AuthTokenGenerator,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    fun calculateTargetPrice(tradingPrice: Double, highPrice: Double, lowPrice: Double): Double {
        return tradingPrice.plus((highPrice.minus(lowPrice)).times(0.5))
    }

    suspend fun buy(market: MarketUnit = MarketUnit.`KRW-BTC`, price: Double) {
        val params = mapOf(
            "market" to market.name,
            "side" to "bid",
            "price" to price,
            "ord_type" to OrderType.PRICE.name.lowercase()
        )
        order(params)
    }

    suspend fun sell(market: MarketUnit = MarketUnit.`KRW-BTC`, volume: Double): Map<String, Any> {
        val params = mapOf(
            "market" to market.name,
            "side" to "ask",
            "volume" to volume,
            "ord_type" to OrderType.MARKET.name.lowercase()
        )
        return order(params)
    }


    suspend fun order(params: Map<String, Any>): Map<String, Any> {
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

    // 매수 목표가 조회
    suspend fun getTargetPrice(): Double {
        // 목표가 계산하기
        val candles = getCandles(periodUnit = "days", count = 2)
        val yesterdayCandle = candles.minByOrNull(Candle::timestamp)!!

        return calculateTargetPrice(
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

    suspend fun getMyBalance(currency: String = "KRW"): Double {
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
        return balance.toDouble()
    }

    private suspend fun getCandles(
        periodUnit: String = "minutes",
        minuteUnit: String = "1",
        market: String = "KRW-BTC",
        count: Int = 200,
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
}