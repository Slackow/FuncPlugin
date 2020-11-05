package com.slackow.func.parser.value

import java.util.*

abstract class Value(open val type: Type) {
    private val properties: Map<String, Value> = HashMap()
    open fun toJsonValue(): String {
        throw UnsupportedOperationException()
    }
}