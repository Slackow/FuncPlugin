package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value

class MethodValue(val data: (List<Value>) -> Value?) : Value(MethodType) {

    object MethodType : Type() {
        override val typeName: String
            get() = "method"
    }
}