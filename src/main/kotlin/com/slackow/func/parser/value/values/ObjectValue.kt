package com.slackow.func.parser.value.values

import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value

class ObjectValue : Value(ObjectType) {

    private object ObjectType : Type() {
        override val typeName: String
            get() = "Object"
        override val canChangeProperties: Boolean
            get() = true

    }
}