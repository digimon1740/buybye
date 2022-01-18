package com.example.buybye.domain.engine

import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.ZoneId

class TradeEngine {

    private val log = LoggerFactory.getLogger(javaClass)

    fun calculateTargetPrice(openingPrice: Double, highPrice: Double, lowPrice: Double): Double {
        return openingPrice.plus((highPrice.minus(lowPrice)).times(0.5))
    }

    fun getCurrentTargetPrice(openingPrice: Double, highPrice: Double, lowPrice: Double) {
        val timezone = ZoneId.of("Asia/Seoul")
        var now = LocalDateTime.now()
        var mid = LocalDateTime.of(now.year, now.month, now.dayOfMonth, 0, 0, 0).plusDays(1)
        var targetPrice = calculateTargetPrice(openingPrice, highPrice, lowPrice)

        while (true) {
            now = LocalDateTime.now()
            val nowAsMilli = now.atZone(timezone).toInstant().toEpochMilli()
            val midAsMilli = mid.atZone(timezone).toInstant().toEpochMilli()
            val midPlus10AsMilli = mid.plusSeconds(10).atZone(timezone).toInstant().toEpochMilli()

            if (midAsMilli < nowAsMilli || nowAsMilli < midPlus10AsMilli) {
                targetPrice = calculateTargetPrice(openingPrice, highPrice, lowPrice)
                mid = LocalDateTime.of(now.year, now.month, now.dayOfMonth, 0, 0, 0).plusDays(1)
                log.info("refresh targetPrice now : {} targetPrice : {} nextMid : {}", now, targetPrice, mid)
            }
        }
    }
}