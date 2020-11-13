package com.slackow.func.parser.value

abstract class Type : Value(TypeType) {
    abstract val typeName: String
    abstract override val canChangeProperties: Boolean

    override fun toString() = "[$typeName]"

    private object TypeType : Type() {
        override val typeName: String
            get() = "type"
        override val canChangeProperties: Boolean
            get() = true
    }
}