package com.slackow.func.parser.value

abstract class Type : Value(TypeType) {
    abstract val typeName: String

    override fun toString() = "[$typeName]"

    private object TypeType : Type() {
        override val typeName: String
            get() = "type"
    }
}