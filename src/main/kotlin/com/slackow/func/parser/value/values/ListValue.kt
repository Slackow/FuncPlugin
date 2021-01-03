package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value

class ListValue(val data: MutableList<Value?>) : Value(ListType) {

    constructor() : this(mutableListOf())

    override fun toJsonValue() = "[${data.joinToString(",") { it?.toJsonValue() ?: "undefined" }}]"

    override fun toString() = data.toString()
    operator fun get(i: Int) = data[i]

    operator fun set(i: Int, value: Value?) = data.set(i, value)


    object ListType : Type() {
        override val typeName: String
            get() = "List"
        override val canChangeProperties: Boolean
            get() = false
    }
}
