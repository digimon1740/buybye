package com.example.buybye.controller

import com.example.buybye.utils.SlackNotifier
import org.slf4j.LoggerFactory
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController("/_")
class SystemController(
    private val slackNotifier: SlackNotifier,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/health")
    fun health() = "OK"

    @EventListener
    fun handleContextClosedEvent(event: ContextClosedEvent) {
        slackNotifier.notifyBlock("<!channel> 시스템이 비정상적으로 종료되었습니다.")
    }
}