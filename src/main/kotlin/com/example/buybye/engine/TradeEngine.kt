package com.example.buybye.engine

class TradeEngine {

    fun calculateTargetPrice(openingPrice: Double, highPrice: Double, lowPrice: Double): Double {
        return openingPrice.plus((highPrice.minus(lowPrice)).times(0.5))
    }
}