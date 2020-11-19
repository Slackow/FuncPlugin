package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value
import kotlin.math.round

class DoubleValue(val data: Double) : Value(DoubleType) {
    override fun toJsonValue() = toString()

    override fun toString(): String {
        val string = data.toString()
        return if (string.endsWith(".0")) string.substring(0, string.length-2) else string
    }

    operator fun plus(that: DoubleValue) = DoubleValue(this.data + that.data)

    operator fun minus(that: DoubleValue) = DoubleValue(this.data - that.data)

    operator fun times(that: DoubleValue) = DoubleValue(this.data * that.data)

    operator fun div(that: DoubleValue) = DoubleValue(this.data / that.data)

    operator fun rem(that: DoubleValue) = DoubleValue(this.data % that.data)

    operator fun compareTo(that: DoubleValue) = this.data.compareTo(that.data)

    operator fun inc() = DoubleValue(this.data + 1)

    operator fun dec() = DoubleValue(this.data - 1)

    val intData: Int
        get() {
            if (round(data) == data)
                return data.toInt()
            else
                TODO("Write Proper Exception")
        }

    object DoubleType : Type() {
        override val typeName: String
            get() = "double"
        override val canChangeProperties: Boolean
            get() = false
    }
}
