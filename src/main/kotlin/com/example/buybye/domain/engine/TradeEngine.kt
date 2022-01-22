package com.example.buybye.domain.engine

import org.slf4j.LoggerFactory

class TradeEngine {

    private val log = LoggerFactory.getLogger(javaClass)

    fun calculateTargetPrice(tradingPrice: Double, highPrice: Double, lowPrice: Double): Double {
        return tradingPrice.plus((highPrice.minus(lowPrice)).times(0.5))
    }
}