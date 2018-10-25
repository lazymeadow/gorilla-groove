package com.example.groove

import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan


@SpringBootApplication
class GorillaGroove

fun main(args: Array<String>) {
    runApplication<GorillaGroove>(*args)
}
