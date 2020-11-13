package com.slackow.func.parser.value


abstract class Value(open val type: Type?) {
    val properties: MutableMap<String, Value?> = HashMap()
    open fun toJsonValue(): String = throw UnsupportedOperationException()

    open val canChangeProperties: Boolean
        get() {
            val currentType = type
            return currentType != null && currentType.canChangeProperties
        }
}