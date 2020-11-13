package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value

class ListValue(val data: MutableList<Value?>) : Value(ListType) {
    override fun toJsonValue() = "[${data.joinToString(",") { it?.toJsonValue() ?: "undefined" }}]"

    override fun toString() = data.toString()
    object ListType : Type() {
        override val typeName: String
            get() = "list"
        override val canChangeProperties: Boolean
            get() = false
    }
}
