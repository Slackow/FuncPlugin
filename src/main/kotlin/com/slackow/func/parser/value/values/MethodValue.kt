package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value

class MethodValue(val data: (List<Value?>) -> Value?) : Value(MethodType) {

    operator fun invoke(args: List<Value?>) = data(args)

    object MethodType : Type() {
        fun verifyLength(list: List<Any?>, size: Int) {
            if (list.size != size) {
                throw RuntimeException("inputs are not the same size as argument length for $this")
            }
        }

        override val typeName: String
            get() = "Method"
        override val canChangeProperties: Boolean
            get() = false
    }
}