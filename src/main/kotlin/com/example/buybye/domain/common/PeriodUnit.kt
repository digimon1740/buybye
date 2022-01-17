package com.example.buybye.domain.common

enum class PeriodUnit(unit: String) {

    MINUTES("minutes"),
    DAYS("days"),
    WEEKS("weeks"),
    MONTH("months");

    companion object {

        fun of(unit: String): PeriodUnit {
            return valueOf(unit.uppercase())
        }
    }
}