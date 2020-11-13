package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value

class BooleanValue(private val data: Boolean) : Value(BooleanType) {
    override fun toJsonValue() = toString()

    override fun toString() = data.toString()
    object BooleanType : Type() {
        override val typeName: String
            get() = "boolean"
        override val canChangeProperties: Boolean
            get() = false
    }
}
