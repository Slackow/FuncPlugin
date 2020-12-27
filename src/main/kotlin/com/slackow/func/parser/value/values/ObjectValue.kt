package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value
import java.util.stream.Collectors

class ObjectValue : Value(ObjectType) {

    override fun toString(): String {
        return toJsonValue()
    }

    override fun toJsonValue(): String {
        return "{${properties.entries.stream().map { entry -> "\"${entry.key}\":${entry.value?.toJsonValue()}"}
            .collect(Collectors.joining(", "))}}"
    }

    private object ObjectType : Type() {
        override val typeName: String
            get() = "Object"
        override val canChangeProperties: Boolean
            get() = true

    }
}