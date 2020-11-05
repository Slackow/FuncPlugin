package com.slackow.func.parser

import com.slackow.func.parser.value.Value
import com.slackow.func.parser.value.values.BooleanValue
import java.util.*

class FuncVisitor : FuncParserBaseVisitor<Value?>() {
    private var memory: Scope<Value> = Scope()
    private val namespaceStack = Stack<String>()
    private fun enterScope() {
        memory = memory.newChild
    }

    private fun exitScope() {
        memory = memory.parent!!
    }

    override fun visitBoolAtom(ctx: FuncParser.BoolAtomContext): Value? {
        return BooleanValue(java.lang.Boolean.valueOf(ctx.text))
    }
}