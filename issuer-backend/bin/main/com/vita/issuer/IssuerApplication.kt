package com.vita.issuer

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.vita"])
class IssuerApplication

fun main(args: Array<String>) {
    runApplication<IssuerApplication>(*args)
}
