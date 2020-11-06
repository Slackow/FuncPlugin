package com.slackow.func.parser

import com.slackow.func.parser.FuncLexer.*
import com.slackow.func.parser.value.Value
import com.slackow.func.parser.value.values.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import java.util.*
import java.util.stream.Collectors

class FuncVisitor : FuncParserBaseVisitor<Value?>() {
    var memory: Scope<Value?> = Scope()
    private val namespaceStack = Stack<String>()

    init {
        namespaceStack.add("")
    }

    private fun enterScope() {
        memory = memory.newChild
    }

    private fun exitScope() {
        memory = memory.parent!!
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val text = "var a = 1;var b = true;"
            val charStream = CharStreams.fromString(text)
            val funcLexer = FuncLexer(charStream)
            val tokens = CommonTokenStream(funcLexer)
            val funcParser = FuncParser(tokens)
            val visitor = FuncVisitor()
            visitor.visit(funcParser.program())
        }
    }

    override fun visit(tree: ParseTree?): Value? {
        if (tree == null) return null
        return super.visit(tree)
    }

    override fun visitBoolAtom(ctx: FuncParser.BoolAtomContext) = BooleanValue(ctx.text!!.toBoolean())

    override fun visitNumAtom(ctx: FuncParser.NumAtomContext) = DoubleValue(ctx.text.toDouble())

    override fun visitThisAtom(ctx: FuncParser.ThisAtomContext) = memory["this"]

    override fun visitUndefinedAtom(ctx: FuncParser.UndefinedAtomContext?): Value? = null

    override fun visitVarAtom(ctx: FuncParser.VarAtomContext) = memory[ctx.text]

    override fun visitStringAtom(ctx: FuncParser.StringAtomContext) = visit(ctx.string())

    override fun visitString(ctx: FuncParser.StringContext): Value? {
        return StringValue(ctx.stringPart().stream().map(stringVisitor::visit).collect(Collectors.joining()))
    }

    override fun visitLambdaAtom(ctx: FuncParser.LambdaAtomContext): Value? {
        return MethodValue { input ->
            enterScope()
            for ((i, node) in (ctx.idenList()?.IDEN() ?: listOf(ctx.IDEN())).withIndex()) {
                memory[node.text] = input[i]
            }
            val result = visit(ctx.expr() ?: ctx.block())
            exitScope()
            return@MethodValue result
        }
    }



    override fun visitVarDefinition(ctx: FuncParser.VarDefinitionContext): Value? {
        val idenList = ctx.idenList().IDEN()
        val value = this.visit(ctx.expr())
        if (idenList.size == 1) {
            val varName = idenList[0].text
            memory.setDirect(varName, value)
            println("stored $value to $varName")
        } else {
            if (value is ListValue) {
                for ((i, iden) in idenList.withIndex()) {
                    val list = value.data
                    memory.setDirect(iden.text, list[i])
                }
            } else {
                throw RuntimeException() //TODO make a proper exception
            }
        }
        return null
    }

    override fun visitVarModification(ctx: FuncParser.VarModificationContext): Value? {
        val type = ctx.op.type
        val oldValue: Value?
        if (type != EQUAL) {
            val modifiableExpr = ctx.modifiableExpr()
            val main = this.visit(modifiableExpr.expr(0))
            if (main == null) {
                oldValue = memory[modifiableExpr.IDEN().text]
            } else {
                TODO("Need to implement object and array setting!")
            }
        }
        //TODO FIX UP!
        when (type) {
            EQUAL -> {
            }
            PLUSEQ -> {

            }
            MINUSEQ -> {

            }
            MULTEQ -> {

            }
            DIVEQ -> {

            }
            PLUSPLUS -> {

            }
            MINUSMINUS -> {

            }
        }
        return null
    }

    private val stringVisitor = StringVisitor()
    private inner class StringVisitor : FuncParserBaseVisitor<String>() {


        override fun visitTextStringPart(ctx: FuncParser.TextStringPartContext): String {
            return ctx.text
        }

        override fun visitEscapeStringPart(ctx: FuncParser.EscapeStringPartContext): String {
            val c = ctx.text[1]
            return when (c) {
                'r' -> {
                    "\r"
                }
                'n' -> {
                    "\n"
                }
                'u' -> {
                    ctx.text.substring(2).toInt(16).toChar().toString()
                }
                else -> {
                    c.toString()
                }
            }
        }

        override fun visitExprInterpPart(ctx: FuncParser.ExprInterpPartContext): String {
            return this@FuncVisitor.visit(ctx.expr()).toString()
        }
    }
}
