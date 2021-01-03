package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

object MathValue : Type() {
    override val typeName: String
        get() = "Math"
    override val canChangeProperties: Boolean
        get() = false

    init {
        properties["sin"] = MethodValue { args ->
            MethodValue.MethodType.verifyLength(args, 1)
            val x = args[0]
            if (x is DoubleValue) {
                DoubleValue(sin(x.data))
            } else {
                throw RuntimeException()
            }
        }
        properties["cos"] = MethodValue { args ->
            MethodValue.MethodType.verifyLength(args, 1)
            val x = args[0]
            if (x is DoubleValue) {
                DoubleValue(cos(x.data))
            } else {
                throw RuntimeException()
            }
        }

        properties["arcsin"] = MethodValue { args ->
            MethodValue.MethodType.verifyLength(args, 1)
            val x = args[0]
            if (x is DoubleValue) {
                DoubleValue(asin(x.data))
            } else {
                throw RuntimeException()
            }
        }

        properties["arccos"] = MethodValue { args ->
            MethodValue.MethodType.verifyLength(args, 1)
            val x = args[0]
            if (x is DoubleValue) {
                DoubleValue(acos(x.data))
            } else {
                throw RuntimeException()
            }
        }
    }
}