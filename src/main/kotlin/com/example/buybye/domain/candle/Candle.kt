package com.example.buybye.domain.candle

import java.time.LocalDateTime

data class Candle(
    val market: String,//마켓명
    val candle_date_time_utc: LocalDateTime,//캔들 기준 시각(UTC 기준)
    val candle_date_time_kst: LocalDateTime,//캔들 기준 시각(KST 기준)
    val opening_price: Double,//시가
    val high_price: Double,//고가
    val low_price: Double,//저가
    val trade_price: Double,//종가
    val timestamp: Long,//마지막 틱이 저장된 시각
    val candle_acc_trade_price: Double, // 누적 거래 금액
    val candle_acc_trade_volume: Double,// 누적 거래량
    val prev_closing_price: Double,// 전일 종가(UTC 0시 기준)
    val change_price: Double,//전일 종가 대비 변화 금액
    val change_rate: Double,//전일 종가 대비 변화량
    val converted_trade_price: Double?,//종가 환산 화폐 단위로 환산된 가격(요청에 convertingPriceUnit 파라미터 없을 시 해당 필드 포함되지 않음.)
) {

}
