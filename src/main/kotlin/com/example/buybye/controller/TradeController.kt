package com.example.buybye.controller

import com.example.buybye.domain.common.MarketUnit
import com.example.buybye.domain.engine.TradeEngine
import com.example.buybye.utils.SlackNotifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class TradeController(
    private val slackNotifier: SlackNotifier,
    private val tradeEngine: TradeEngine,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/error")
    suspend fun error() {
        slackNotifier.error("에러 발생 !!")
    }

    @GetMapping("/sell")
    suspend fun sellManually() = sell()

    @GetMapping("/buy")
    suspend fun buyManually() = buy()

    @Scheduled(cron = "0 0 0 * * *")
    fun sellScheduled() = CoroutineScope(Dispatchers.IO).launch {
        sell()
    }

    @Scheduled(cron = "* * * * * *")
    fun buyScheduled() = CoroutineScope(Dispatchers.IO).launch {
        val now = LocalDateTime.now()
        val startDateTime = LocalDateTime.of(now.year, now.month, now.dayOfMonth, 7, 0, 0)
        if (now.isAfter(startDateTime)) {
            buy()
        }
    }

    suspend fun sell() = runCatching {
        val market = MarketUnit.`KRW-BTC`

        val currentPrice = tradeEngine.getCurrentPrice(market)
        val targetPrice = tradeEngine.getTargetPrice()
        val btc = tradeEngine.getMyBalance("BTC")
        if (btc > 0.00008) {
            val btcToSell = btc * 0.9995
            tradeEngine.sell(market, btcToSell)
            slackNotifier.notify("<!channel> 매도완료 매도수량 : ${btcToSell.toLong()}, 현재가 : ${currentPrice.toLong()}")
        }

        mapOf(
            "current" to currentPrice,
            "targetPrice" to targetPrice,
            "balance" to btc,
        )
    }.onFailure {
        log.error(it.message)
        slackNotifier.error("에러 발생 ${it.localizedMessage}")
    }.getOrNull()

    suspend fun buy() = runCatching {
        val market = MarketUnit.`KRW-BTC`

        val currentPrice = tradeEngine.getCurrentPrice(market)
        val targetPrice = tradeEngine.getTargetPrice()
        val myBalance = tradeEngine.getMyBalance()

        if (targetPrice < currentPrice) {
            if (myBalance > 5000) {
                val priceToBuy = myBalance * 0.9995
                tradeEngine.buy(market, priceToBuy)
                slackNotifier.notify(
                    "<!channel> 체결완료 매수금액 : ${priceToBuy.toLong()}, 목표가 : ${targetPrice.toLong()}, 현재가 : ${
                        currentPrice
                            .toLong()
                    }"
                )
            }
        }

        mapOf(
            "current" to currentPrice,
            "targetPrice" to targetPrice,
            "balance" to myBalance,
        )
    }.onFailure {
        log.error(it.message)
        slackNotifier.error("에러 발생 ${it.localizedMessage}")
    }.getOrNull()

    @GetMapping("/target")
    suspend fun getTargetAndCurrentPrice(@RequestParam(defaultValue = "KRW-BTC") marketStr: String): Map<String, Any> {
        val market = MarketUnit.of(marketStr)
        val currentPrice = tradeEngine.getCurrentPrice(market)
        val targetPrice = tradeEngine.getTargetPrice()

        return mapOf(
            "market" to market,
            "currentPrice" to currentPrice,
            "targetPrice" to targetPrice
        )
    }

}