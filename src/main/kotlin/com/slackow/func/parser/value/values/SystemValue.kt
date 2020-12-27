package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.values.MethodValue.MethodType.verifyLength
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors

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

        properties["read"] = MethodValue { args ->
            verifyLength(args, 1)
            val value = args[0]
            if (value is StringValue) {
                return@MethodValue StringValue(Files.lines(Paths.get("/" + value.data)).collect(Collectors.joining("\n")))
            }
            throw RuntimeException("input was not a string")
        }

        properties["write"] = MethodValue { args ->
            verifyLength(args, 2)
            val file = args[0]
            val value = args[1]
            if (file is StringValue && value is StringValue) {
                Files.write(Paths.get(""), value.data.split("\n"))
            } else {
                throw RuntimeException("inputs were not strings")
            }
            null
        }
    }
}