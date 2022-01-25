package com.example.buybye.domain.common

enum class OrderType(val side: String) {
    LIMIT(""),  //  지정가 주문
    PRICE("bid"),  //  시장가 주문(매수)
    MARKET("ask");  //  시장가 주문(매도)

    companion object {

        fun of(value: String): OrderType {
            return valueOf(value.uppercase())
        }

    }
}
