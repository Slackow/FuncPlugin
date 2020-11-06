package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value

class DoubleValue(private val double: Double) : Value(DoubleType) {
    override fun toJsonValue() = toString()

    override fun toString(): String {
        val string = double.toString()
        return if (string.endsWith(".0")) string.substring(0, string.length-2) else string
    }
    object DoubleType : Type() {
        override val typeName: String
            get() = "double"
    }
}
