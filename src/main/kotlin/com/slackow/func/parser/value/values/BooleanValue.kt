package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value

class BooleanValue private constructor(val data: Boolean) : Value(BooleanType) {
    override fun toJsonValue() = toString()

    override fun toString() = data.toString()

    companion object {
        val TRUE = BooleanValue(true)
        val FALSE = BooleanValue(false)
        fun of(boolean: Boolean): BooleanValue {
            return if (boolean) TRUE else FALSE
        }
    }

    object BooleanType : Type() {
        override val typeName: String
            get() = "boolean"
        override val canChangeProperties: Boolean
            get() = false
    }
}
