package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value

class NoValue private constructor(val name: String) : Value(NoType) {

    companion object {
        val BREAK = NoValue("break")
        val CONTINUE = NoValue("continue")
    }

    var iden = ""

    override val properties: MutableMap<String, Value?>
        get() = TODO("You shouldn't have access to this value")

    object NoType : Type() {
        override val typeName: String
            get() = "None"
        override val canChangeProperties: Boolean
            get() = false

    }
}