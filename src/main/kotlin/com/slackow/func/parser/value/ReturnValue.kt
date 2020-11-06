package com.slackow.func.parser.value

class ReturnValue : RuntimeException() {
    var value: Value? = null
        set(value) {
            field = value
            throw this
        }
}