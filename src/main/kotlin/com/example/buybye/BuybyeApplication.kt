package com.example.buybye

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class BuybyeApplication

fun main(args: Array<String>) {
    runApplication<BuybyeApplication>(*args)
}
