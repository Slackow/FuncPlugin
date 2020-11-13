package com.slackow.func.parser

import com.slackow.func.parser.FuncParser.*
import com.slackow.func.parser.value.ReturnValue
import com.slackow.func.parser.value.Value
import com.slackow.func.parser.value.values.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import java.lang.Integer.max
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

    override fun visitBoolAtom(ctx: BoolAtomContext) = BooleanValue(ctx.text!!.toBoolean())

    override fun visitNumAtom(ctx: NumAtomContext) = DoubleValue(ctx.text.toDouble())

    override fun visitThisAtom(ctx: ThisAtomContext) = memory["this"]

    override fun visitUndefinedAtom(ctx: UndefinedAtomContext): Value? = null

    override fun visitVarAtom(ctx: VarAtomContext) = memory[ctx.text]

    override fun visitStringAtom(ctx: StringAtomContext) = visit(ctx.string())

    override fun visitListAtom(ctx: ListAtomContext): Value? {
        return ListValue(ctx.exprList().expr().map { this.visit(it) }.toMutableList())
    }

    override fun visitObjectAtom(ctx: ObjectAtomContext): Value? {
        val result = ObjectValue()
        ctx.objectPart().forEach { result.properties[it.IDEN().text] = visit(it.expr()) }
        return result
    }

    override fun visitString(ctx: StringContext): Value? {
        return StringValue(ctx.stringPart().stream().map(stringVisitor::visit).collect(Collectors.joining()))
    }

    override fun visitLambdaAtom(ctx: LambdaAtomContext): Value? {
        return MethodValue { input ->
            enterScope()
            for ((i, node) in (ctx.idenList()?.IDEN() ?: listOf(ctx.IDEN())).withIndex()) {
                memory[node.text] = input[i]
            }
            val result = if (ctx.expr() != null) {
                visit(ctx.expr())
            } else {
                getValueFromBlock(ctx.block())
            }
            exitScope()
            return@MethodValue result
        }
    }

    override fun visitRunFunctionExpr(ctx: RunFunctionExprContext): Value? {
        val function = visit(ctx.expr())
        if (function is MethodValue) {
            return function.data(ctx.exprList().values())
        }
        TODO("Write Proper Exception")
    }

    private fun ExprListContext.values(): List<Value?> {
        return this.expr().map { visit(it) }
    }

    private fun getValueFromBlock(block: BlockContext): Value? {
        try {
            visit(block)
        } catch (returnValue: ReturnValue) {
            return ReturnValue.value
        }
        return null
    }


    override fun visitVarDefinition(ctx: VarDefinitionContext): Value? {
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

    override fun visitVarModification(ctx: VarModificationContext): Value? {
        val type = ctx.op.type
        val oldValue: Value?
        if (type != EQUAL) {
            when (val modifiableExpr = ctx.modifiableExpr()) {
                is ModifiableIdenContext -> {
                    oldValue = memory[modifiableExpr.IDEN().text]
                }
                is ModifiableObjectContext -> {
                    val main = this.visit(modifiableExpr.expr())
                    oldValue = if (main?.canChangeProperties == true) {
                        main.properties[modifiableExpr.IDEN().text]
                    } else {
                        TODO("Write Proper Exception")
                    }
                }
                is ModifiableArrayContext -> {
                    val main = this.visit(modifiableExpr.expr(0))
                    val key = this.visit(modifiableExpr.expr(1))
                    oldValue = if (main is ListValue && key is DoubleValue) {
                        main.data[key.intData]
                    } else if (key is StringValue) {
                        if (main?.canChangeProperties == true) {
                            main.properties[key.data]
                        } else {
                            TODO("Write Proper Exception")
                        }
                    } else {
                        TODO("Write Proper Exception")
                    }
                }
                else -> oldValue = null
            }
        } else {
            oldValue = null
        }
        val rightSide = this.visit(ctx.expr())
        val result: Value?
        when (type) {
            EQUAL -> {
                result = null
            }
            PLUSEQ -> {
                result = if (oldValue is DoubleValue && rightSide is DoubleValue)
                    oldValue + rightSide
                else if (oldValue is StringValue && rightSide is StringValue)
                    oldValue + rightSide
                else
                    TODO("Write Proper Exception")
            }
            MINUSEQ -> {
                if (oldValue is DoubleValue && rightSide is DoubleValue)
                    result = oldValue - rightSide
                else
                    TODO("Write Proper Exception")
            }
            MULTEQ -> {
                if (oldValue is DoubleValue && rightSide is DoubleValue)
                    result = oldValue * rightSide
                else
                    TODO("Write Proper Exception")
            }
            DIVEQ -> {
                if (oldValue is DoubleValue && rightSide is DoubleValue)
                    result = oldValue / rightSide
                else
                    TODO("Write Proper Exception")
            }
            PLUSPLUS -> {
                if (oldValue is DoubleValue)
                    result = oldValue.inc()
                else
                    TODO("Write Proper Exception")
            }
            MINUSMINUS -> {
                if (oldValue is DoubleValue)
                    result = oldValue.dec()
                else
                    TODO("Write Proper Exception")
            }
            else -> TODO("Write Proper Exception")
        }
        when (val modifiableExpr = ctx.modifiableExpr()) {
            is ModifiableIdenContext -> {
                memory[modifiableExpr.IDEN().text] = result
            }
            is ModifiableObjectContext -> {
                val main = this.visit(modifiableExpr.expr())
                if (main?.canChangeProperties == true) {
                    main.properties[modifiableExpr.IDEN().text] = result
                } else {
                    TODO("Write Proper Exception")
                }
            }
            is ModifiableArrayContext -> {
                val main = this.visit(modifiableExpr.expr(0))
                val key = this.visit(modifiableExpr.expr(1))
                if (main is ListValue && key is DoubleValue) {
                    main.data[key.intData] = result
                } else if (key is StringValue && main?.canChangeProperties == true) {
                    main.properties[key.data] = result
                } else {
                    TODO("Write Proper Exception")
                }
            }
            else -> TODO("Write Proper Exception")
        }
        return null

    }



    override fun visitBlock(ctx: BlockContext): Value? {
        enterScope()
        ctx.statement().forEach { this.visit(it) }
        if (ctx.endingLine() != null) {
            this.visit(ctx.endingLine())
        }
        exitScope()
        return null
    }

    override fun visitEndingLine(ctx: EndingLineContext): Value? {
        ReturnValue.value = when {
            ctx.RETURN() != null -> {
                this.visit(ctx.expr())
            }
            ctx.BREAK() != null -> {
                TODO("create BREAK")
            }
            ctx.CONTINUE() != null -> TODO("create CONTINUE")
            else -> {
                TODO("Write Proper Exception")
            }
        }
        exitScope()
        throw ReturnValue
    }

    override fun visitCommand(ctx: CommandContext): Value? {
        val command = ctx.commandPart().joinToString(separator = "") { stringVisitor.visit(it) }
        val current = namespaceStack.peek()

        if (ctx.OPEN_FUNCTION() != null) {
            val namespaceValue = visit(ctx.expr())
            if (namespaceValue is StringValue) {
                val namespace = processNamespace(namespaceValue.data)
                namespaceStack.add(namespace)
                this.visit(ctx.statBlock())
            } else {
                TODO("Write Proper Exception")
            }
        }
        return null
    }

    private fun processNamespace(namespace: String?): String {
        val current = namespaceStack.peek()
        var newNamespace = namespace
        if (newNamespace.isNullOrBlank()) {
            var num = 0
            do {
                newNamespace = current + "/fun" + num++
            } while (namespaceStack.contains(newNamespace))
        }
        if (newNamespace!![0] == '/') {
            newNamespace = current + newNamespace
        }
        if (!newNamespace.contains(":")) {
            newNamespace = current.substring(0, max(current.indexOf(':'), current.lastIndexOf('/'))) + newNamespace
        }

        if (!newNamespace.matches(Regex("[a-z_-][a-z\\d_-]*:[a-z_-][a-z\\d_-]*(?:/[a-z_-][a-z\\d_-]*)*")))
            TODO("Write Proper Exception")
        return newNamespace
    }

    private val stringVisitor = StringVisitor()

    private inner class StringVisitor : FuncParserBaseVisitor<String>() {

        override fun visitTextStringPart(ctx: TextStringPartContext): String = ctx.text

        override fun visitCommandText(ctx: CommandTextContext): String = ctx.text.replace("\\$", "$")

        override fun visitThisFunctionPart(ctx: ThisFunctionPartContext?): String = namespaceStack.peek()

        override fun visitCommandGoOnPart(ctx: CommandGoOnPartContext) = " "

        override fun visitCommandExprInterpPart(ctx: CommandExprInterpPartContext) =
                this@FuncVisitor.visit(ctx.expr()).toString()

        override fun visitFunctionReferencePart(ctx: FunctionReferencePartContext): String {
            return ctx.stringPart().joinToString("") { visit(it) }
        }

        override fun visitCommandIdInterpPart(ctx: CommandIdInterpPartContext) =
                memory[ctx.text.substring(1)].toString()

        override fun visitIdInterpPart(ctx: IdInterpPartContext) =
                memory[ctx.text.substring(1)].toString()

        override fun visitEscapeStringPart(ctx: EscapeStringPartContext): String {
            return when (val c = ctx.text[1]) {
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

        override fun visitExprInterpPart(ctx: ExprInterpPartContext) =
                this@FuncVisitor.visit(ctx.expr()).toString()
    }
}
