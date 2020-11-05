package com.slackow.func.parser.value.types

import com.slackow.func.parser.value.Type

object BooleanType : Type() {
    override val typeName: String
        get() = "boolean"
}