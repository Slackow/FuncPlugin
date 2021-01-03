package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.values.MethodValue.MethodType.verifyLength

object SystemValue : Type() {
    override val typeName: String
        get() = "System"
    override val canChangeProperties: Boolean
        get() = false

    init {
        properties["println"] = MethodValue { args ->
            verifyLength(args, 1)
            println(args[0])
            null
        }

        properties["print"] = MethodValue { args ->
            verifyLength(args, 1)
            print(args[0])
            null
        }
    }
}