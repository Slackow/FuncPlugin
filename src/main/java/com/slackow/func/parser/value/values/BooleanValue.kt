package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value
import com.slackow.func.parser.value.types.BooleanType

class BooleanValue(val value: Boolean?) : Value(BooleanType) {

    override fun toJsonValue(): String {
        return toString()
    }
}