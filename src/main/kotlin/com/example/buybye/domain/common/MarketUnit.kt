package com.example.buybye.domain.common

enum class MarketUnit {

    `KRW-BTC`;

    companion object {

        fun of(value: String): MarketUnit {
            return valueOf(value.uppercase())
        }
    }
}