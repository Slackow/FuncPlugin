package com.slackow.func.parser

import com.slackow.func.parser.FuncParser.*
import com.slackow.func.parser.value.ReturnValue
import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value
import com.slackow.func.parser.value.values.*
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.tree.ParseTree
import java.lang.Integer.max
import java.util.*
import java.util.stream.Collectors
import kotlin.math.pow

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
        val parent = memory.parent ?: TODO("Write Proper Exception")
        memory = parent
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

    override fun visitBoolAtom(ctx: BoolAtomContext) = BooleanValue.of(ctx.text!!.toBoolean())

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

    override fun visitDefineFunctionStatement(ctx: DefineFunctionStatementContext): Value? {
        val list = ctx.idenList().IDEN().map { it.text }
        memory[ctx.IDEN().text] = methodValue(list, ctx.block())
        return null
    }

    override fun visitTypeOfExpr(ctx: TypeOfExprContext): Value? {
        return visit(ctx.expr())?.type ?: Type.TypeType
    }

    override fun visitDefineInstanceFunctionStatement(ctx: DefineInstanceFunctionStatementContext): Value? {
        val type = visit(ctx.expr())
        if (type is Type) {
            if (type.canChangeProperties) {
                val key = ctx.IDEN().text
                if (type.properties[key] == null) {
                    type.properties[key] = methodValue(ctx.idenList().IDEN().map { it.text }, ctx.block())
                } else {
                    TODO("Write Proper Exception")
                }
            } else {
                TODO("Write Proper Exception")
            }
        } else {
            TODO("Write Proper Exception")
        }
        return null
    }

    override fun visitLambdaAtom(ctx: LambdaAtomContext): Value? {
        val list = (ctx.idenList()?.IDEN() ?: listOf(ctx.IDEN())).map { it.text }
        return methodValue(list, ctx.block(), ctx.expr())
    }

    private fun methodValue(idens: List<String>, block: BlockContext? = null, expr: ExprContext? = null): MethodValue {
        return MethodValue { input ->
            enterScope()
            if (input.size != idens.size) TODO("Throw Proper Exception")
            for ((i, parameter) in idens.withIndex()) {
                memory[parameter] = input[i]
            }
            val result = if (expr != null) {
                visit(expr)
            } else {
                getValueFromBlock(block!!)
            }
            exitScope()
            result
        }
    }

    override fun visitRunFunctionExpr(ctx: RunFunctionExprContext): Value? {
        val function = visit(ctx.expr())
        if (function is MethodValue) {
            return function(ctx.exprList().values())
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

    override fun visitNotExpr(ctx: NotExprContext): Value? {
        val value = visit(ctx.expr())
        return BooleanValue.of(!((value as? BooleanValue)?.data ?: TODO("Write Proper Exception")))
    }

    override fun visitAndExpr(ctx: AndExprContext): Value? {
        return BooleanValue.of(
                (visit(ctx.left) as? BooleanValue)?.data ?: TODO("Write proper exception")
                        && (visit(ctx.right) as? BooleanValue)?.data ?: TODO("Write proper exception"))
    }

    override fun visitOrExpr(ctx: OrExprContext): Value? {
        return BooleanValue.of(
                (visit(ctx.left) as? BooleanValue)?.data ?: TODO("Write proper exception")
                        || (visit(ctx.right) as? BooleanValue)?.data ?: TODO("Write proper exception")
        )
    }

    override fun visitCommand(ctx: CommandContext): Value? {
        val command = ctx.commandPart().joinToString(separator = "") { stringVisitor.visit(it) }
        val current = namespaceStack.peek()

        if (ctx.OPEN_FUNCTION() != null) {
            val namespaceValue = visit(ctx.expr())
            if (namespaceValue is StringValue) {
                val namespace = processNamespace(namespaceValue.data)
                namespaceStack.add(namespace)
                this.visit(ctx.block())
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

    override fun visitPowExpr(ctx: PowExprContext): Value? {
        val left = visit(ctx.left)
        val right = visit(ctx.right)
        if (left is DoubleValue && right is DoubleValue) {
            return DoubleValue(left.data.pow(right.data))
        }
        TODO("Write Proper Exception")
    }

    override fun visitMultExpr(ctx: MultExprContext): Value? {
        val left = visit(ctx.left)
        val right = visit(ctx.right)
        if (left is DoubleValue && right is DoubleValue)
            return when (ctx.op.type) {
                MULT -> left * right
                DIV -> left / right
                MOD -> left % right
                else -> TODO("Write Proper Exception")
            }
        TODO("Write Proper Exception")
    }

    override fun visitSubExpr(ctx: SubExprContext): Value? {
        val start = visit(ctx.start)
        val end = visit(ctx.end)
        if (start is DoubleValue && end is DoubleValue) {
            val inc = visit(ctx.inc) ?: DoubleValue(if (start.data <= end.data) 1.0 else -1.0)



            val main = visit(ctx.main)
            if (main is ListValue) {

            }
        } else {

        }


        TODO("Write Proper Exception")
    }

    override fun visitAddExpr(ctx: AddExprContext): Value? {
        val left = visit(ctx.left)
        val right = visit(ctx.right)
        return when (ctx.op.type) {
            PLUS -> {
                if (left is DoubleValue && right is DoubleValue) {
                    left + right
                } else if (left is StringValue && right is StringValue) {
                    left + right
                } else {
                    TODO("Write proper exception")
                }
            }
            MINUS -> {
                if (left is DoubleValue && right is DoubleValue) {
                    left - right
                } else {
                    TODO("Write proper exception")
                }
            }
            else -> TODO("Write proper exception")
        }
    }

    override fun visitNegationExpr(ctx: NegationExprContext): Value? {
        return DoubleValue(-((visit(ctx.expr()) as? DoubleValue)?.data ?: TODO("Write Proper Exception")))
    }

    override fun visitElvisExpr(ctx: ElvisExprContext): Value? {
        return visit(ctx.left) ?: visit(ctx.right)
    }

    override fun visitIsExpr(ctx: IsExprContext): Value {
        return BooleanValue.of(visit(ctx.left) === visit(ctx.right))
    }

    override fun visitRelationalExpr(ctx: RelationalExprContext): Value? {
        val left = visit(ctx.left)
        val right = visit(ctx.right)
        if (left is DoubleValue && right is DoubleValue) {
            return when (ctx.op.type) {
                LT -> BooleanValue.of(left.data < right.data)
                GT -> BooleanValue.of(left.data > right.data)
                LE -> BooleanValue.of(left.data <= right.data)
                GE -> BooleanValue.of(left.data >= right.data)
                else -> TODO("Write Proper Exception")
            }
        }
        TODO("Write Proper Exception")
    }

    override fun visitEqualityExpr(ctx: EqualityExprContext): Value? {
        return BooleanValue.of((visit(ctx.left) == visit(ctx.right)) == (ctx.op.type == EQ))
    }

    override fun visitGetItemExpr(ctx: GetItemExprContext): Value? {
        val main = visit(ctx.main)
        val key = visit(ctx.key)
        if (main is ListValue && key is DoubleValue) {
            return main.data[key.intData]
        } else if (main != null && key is StringValue) {
            return main.properties[key.data]
        }
        TODO("Write Proper Exception")
    }

    override fun visitGetObjectExpr(ctx: GetObjectExprContext): Value? {
        val main = visit(ctx.main) ?: TODO("Write Proper Exception")
        return main.properties[ctx.key.text]
    }

    override fun visitTernaryExpr(ctx: TernaryExprContext): Value? {
        val condition = (visit(ctx.condition) as? BooleanValue)?.data ?: TODO("Write Proper Exception")
        return visit(if (condition) ctx.left else ctx.right)
    }

    override fun visitFunctionCallLine(ctx: FunctionCallLineContext): Value? {
        val function = visit(ctx.expr())
        if (function is MethodValue) {
            function(ctx.exprList().values())
        } else {
            TODO("Write Proper Exception")
        }
        return null
    }

    override fun visitAssertLine(ctx: AssertLineContext): Value? {
        if (!(visit(ctx.expr()) as? BooleanValue ?: TODO("Write Proper Exception")).data) {
            throw AssertionError()
        }
        return null
    }

    override fun visitIfStatement(ctx: IfStatementContext): Value? {
        val condition = visit(ctx.exprBlock().expr())
        if (condition is BooleanValue) {
            if (condition.data) {
                visit(ctx.exprBlock().statBlock())
            } else if (ctx.ELSE() != null) {
                visit(ctx.statBlock())
            }
        } else {
            TODO("Write Proper Exception")
        }
        return null
    }

    override fun visitForLoop(ctx: ForLoopContext): Value? {
        visit(ctx.first)
        while ((visit(ctx.condition) as? BooleanValue ?: TODO("Write Proper Exception")).data){
            visit(ctx.statBlock())
        }
        visit(ctx.last)
        return null
    }

    override fun visitNullAtom(ctx: NullAtomContext?): Value? {
        TODO("Implement Null Values")
    }


    override fun visitWhileLoop(ctx: WhileLoopContext): Value? {
        while ((visit(ctx.exprBlock().expr()) as? BooleanValue ?: TODO("Write Proper Exception")).data) {
            visit(ctx.exprBlock().statBlock())
        }
        return null
    }

    override fun visitDoWhileLoop(ctx: DoWhileLoopContext): Value? {
        do {
            visit(ctx.statBlock())
        } while ((visit(ctx.expr()) as? BooleanValue ?: TODO("Write Proper Exception")).data)
        return null
    }

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
