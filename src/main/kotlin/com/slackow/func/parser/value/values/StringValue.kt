package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value

class StringValue(val data: String) : Value(StringType) {
    override fun toString() = data
    override fun toJsonValue() = "\"${data.replace(Regex("([\\\\\"])"), "\\$1")}\""
    object StringType : Type() {
        override val typeName: String
            get() = "string"
    }
}
