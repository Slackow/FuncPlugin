package com.slackow.func.parser

import com.slackow.func.parser.FuncParser.*
import com.slackow.func.parser.value.ReturnValue
import com.slackow.func.parser.value.Type
import com.slackow.func.parser.value.Value
import com.slackow.func.parser.value.values.*
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.tree.ParseTree
import java.lang.Integer.max
import java.nio.file.Paths
import java.util.*
import java.util.stream.Collectors
import kotlin.collections.HashMap
import kotlin.math.pow

class FuncVisitor(private val parent: Datapack, location: String) : FuncParserBaseVisitor<Value?>() {
    var memory: Scope<Value?> = Scope()
    private val namespaceStack = Stack<String>()

    init {
        namespaceStack.add(location)
    }

    private fun enterScope() {
        memory = memory.newChild
    }

    private fun exitScope() {
        memory = memory.parent ?: error("exited scope when there was no parent?")
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
           // val d = "$"
//            val text = """
//                |#import mw:raycast/deeper/Functions as rc;
//                |#import Test;
//                |#import MW:a;
//                |var a = 1;
//                |var a = {
//                |   b: "Dream Cheated",
//                |   "rolf a": 1232409879
//                |};
//                |var c = a.b;
//                |var d = a["rolf a"];
//                |/execute as @a run ${d}function "mc:test" {
//                |   /this belongs in mc:test
//                |   gen function "mc:wow" {
//                |       /belongs in mw:wow
//                |   }
//                |   /aftermath (mc:test)
//                |}
//                |/aftermath (root)
//                |
//                |""".trimMargin()
            val parent = Datapack("TestDatapack")
            parent.collect(Paths.get("src/testfiles/TestDatapack"))
            parent.writeOutput(Paths.get("src/testfiles/output"))
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

    override fun visitListAtom(ctx: ListAtomContext): Value {
        return ListValue(ctx.exprList().expr().map { this.visit(it) }.toMutableList())
    }

    override fun visitObjectAtom(ctx: ObjectAtomContext): Value {
        val result = ObjectValue()
        ctx.objectPart()
            .forEach { result.properties[it.IDEN()?.text ?: stringVisitor.visit(it.string())] = visit(it.expr()) }
        return result
    }

    override fun visitString(ctx: StringContext): Value {
        return StringValue(ctx.stringPart().stream().map(stringVisitor::visit).collect(Collectors.joining()))
    }

    override fun visitDefineFunctionStatement(ctx: DefineFunctionStatementContext): Value? {
        val list = ctx.idenList().IDEN().map { it.text }
        memory[ctx.IDEN().text] = methodValue(list, ctx.block())
        return null
    }

    override fun visitTypeOfExpr(ctx: TypeOfExprContext): Value {
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
                    error(ctx, "Property Already Defined for " + type.typeName)
                }
            } else {
                error(ctx, "Type cannot change properties")
            }
        } else {
            error(ctx, "Class not found: $type")
        }
        return null
    }

    override fun visitLambdaAtom(ctx: LambdaAtomContext): Value {
        val list = (ctx.idenList()?.IDEN() ?: listOf(ctx.IDEN())).map { it.text }
        return methodValue(list, ctx.block(), ctx.expr())
    }

    private fun methodValue(idens: List<String>, block: BlockContext? = null, expr: ExprContext? = null): MethodValue {
        return MethodValue { input ->
            enterScope()
            if (input.size != idens.size) {
                throw RuntimeException("inputs are not the same size as argument length for $this")
            }
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
        error(ctx, "Not a function: $function")
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

    override fun visitImportLine(ctx: ImportLineContext): Value? {
        val name = (ctx.alais ?: ctx.importMeat().name).text
        //TODO("yeah fix this area")
        val loc = if (ctx.importMeat().namespace != null)
            "${ctx.importMeat().text.replace(":", "/sources/")}.mcfslib"
        else "${namespaceStack[0].apply { substring(0, indexOf(':')) }}/sources/${ctx.importMeat().text}.mcfslib"
        println("imported $loc as $name")
        memory[name] = null
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
                error(ctx, "excepted list: $value")
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
                        error(ctx, "value is not modifiable: $main")
                    }
                }
                is ModifiableArrayContext -> {
                    val main = this.visit(modifiableExpr.expr(0))
                    val key = this.visit(modifiableExpr.expr(1))
                    oldValue = if (main is ListValue && key is DoubleValue) {
                        main[key.intData]
                    } else if (key is StringValue) {
                        if (main?.canChangeProperties == true) {
                            main.properties[key.data]
                        } else {
                            error(ctx, "value cannot change properties: $main")
                        }
                    } else {
                        error(ctx, "key is not correct type or main is not correct type: ${typeName(main)}[${
                            typeName(
                                key
                            )
                        }]")
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
                    error(ctx, "values must be strings or numbers")
            }
            MINUSEQ -> {
                if (oldValue is DoubleValue && rightSide is DoubleValue)
                    result = oldValue - rightSide
                else
                    error(ctx, "values must be numbers")
            }
            MULTEQ -> {
                if (oldValue is DoubleValue && rightSide is DoubleValue)
                    result = oldValue * rightSide
                else
                    error(ctx, "values must be numbers")
            }
            DIVEQ -> {
                if (oldValue is DoubleValue && rightSide is DoubleValue)
                    result = oldValue / rightSide
                else
                    error(ctx, "values must be numbers")
            }
            PLUSPLUS -> {
                if (oldValue is DoubleValue)
                    result = oldValue.inc()
                else
                    error(ctx, "value must be a number")
            }
            MINUSMINUS -> {
                if (oldValue is DoubleValue)
                    result = oldValue.dec()
                else
                    error(ctx, "value must be a number")
            }
            else -> error(ctx, "Unknown Operator: ${ctx.op.text}")
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
                    error(ctx, "object cannot change properties $main")
                }
            }
            is ModifiableArrayContext -> {
                val main = this.visit(modifiableExpr.expr(0))
                val key = this.visit(modifiableExpr.expr(1))
                if (main is ListValue && key is DoubleValue) {
                    main[key.intData] = result
                } else if (key is StringValue && main?.canChangeProperties == true) {
                    main.properties[key.data] = result
                } else {
                    error(ctx, "bad type match: ${typeName(main)}[${typeName(key)}]")
                }
            }
            else -> error(ctx, "invalid modifiable expression")
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
            ctx.BREAK() != null -> NoValue.BREAK
            ctx.CONTINUE() != null -> NoValue.CONTINUE
            else -> {
                error(ctx, "invalid ending line")
            }
        }
        exitScope()
        throw ReturnValue
    }

    override fun visitNotExpr(ctx: NotExprContext): Value {
        val value = visit(ctx.expr())
        return BooleanValue.of(!((value as? BooleanValue)?.data ?: error(ctx, "not a boolean: $value")))
    }

    override fun visitAndExpr(ctx: AndExprContext): Value {
        return BooleanValue.of(
            (visit(ctx.left) as? BooleanValue)?.data ?: error(ctx, "not a boolean")
                    && (visit(ctx.right) as? BooleanValue)?.data ?: error(ctx, "not a boolean"))
    }

    override fun visitOrExpr(ctx: OrExprContext): Value {
        return BooleanValue.of(
            (visit(ctx.left) as? BooleanValue)?.data ?: error(ctx, "not a boolean")
                    || (visit(ctx.right) as? BooleanValue)?.data ?: error(ctx, "not a boolean")
        )
    }

    override fun visitCommand(ctx: CommandContext): Value? {
        var command = ctx.commandPart().joinToString("") { stringVisitor.visit(it) }
        val current = namespaceStack.peek()

        if (ctx.OPEN_FUNCTION() != null) {
            val namespaceValue = visit(ctx.expr())
            if (namespaceValue is StringValue) {
                val namespace = processNamespace(namespaceValue.data, ctx)
                namespaceStack.add(namespace)
                this.visit(ctx.block())
                namespaceStack.pop()
                command += "function $namespace"
            } else {
                error(ctx, "functionName is not a string: $namespaceValue")
            }
        }
        val functions = parent.output.computeIfAbsent("functions") { HashMap() }
        val currentFunc = "${functions[current] ?: "#Generated by https://www.github.com/Slackow/FuncPlugin"}\n$command"

        functions[current] = currentFunc
        return null
    }

    override fun visitGenMCfunctionStatement(ctx: GenMCfunctionStatementContext): Value? {
        val loc = visit(ctx.exprBlock().expr())
        if (loc is StringValue) {
            val namespace = processNamespace(loc.data, ctx)
            namespaceStack.add(namespace)
            visit(ctx.exprBlock().statBlock())
            namespaceStack.pop()
        }
        return null
    }

    private fun processNamespace(namespace: String?, ctx: ParserRuleContext): String {
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
            error(ctx, "")
        return newNamespace
    }

    private fun error(ctx: ParserRuleContext, msg: String = ""): Nothing {
        throw RuntimeException("$msg line: ${ctx.start.line}")
    }

    private val stringVisitor = StringVisitor()

    override fun visitPowExpr(ctx: PowExprContext): Value {
        val left = visit(ctx.left)
        val right = visit(ctx.right)
        if (left is DoubleValue && right is DoubleValue) {
            return DoubleValue(left.data.pow(right.data))
        }
        error(ctx, "bad type match: ${typeName(left)} ^ ${typeName(right)}")
    }

    override fun visitMultExpr(ctx: MultExprContext): Value {
        val left = visit(ctx.left)
        val right = visit(ctx.right)
        if (left is DoubleValue && right is DoubleValue)
            return when (ctx.op.type) {
                MULT -> left * right
                DIV -> left / right
                MOD -> left % right
                else -> error(ctx, "unknown operator: ${ctx.op.text}")
            }
        error(ctx, "bad type match: ${typeName(left)} ${ctx.op.text} ${typeName(right)}")
    }

    override fun visitSubExpr(ctx: SubExprContext): Value {
        val start = visit(ctx.start) ?: DoubleValue(0.0)
        val end = visit(ctx.end) ?: DoubleValue(1.0)
        if (start is DoubleValue && end is DoubleValue) {
            val inc = visit(ctx.inc) ?: DoubleValue(if (start.data <= end.data) 1.0 else -1.0)

            if (inc !is DoubleValue) {
                error(ctx, "increment needs to be number, not ${typeName(inc)}")
            }
            when (val main = visit(ctx.main)) {
                is ListValue -> {
                    when (inc.intData) {
                        1 -> {
                            return ListValue(main.data.subList(start.intData, end.intData))
                        }
                        -1 -> {
                            return ListValue(main.data.subList(end.intData, start.intData).asReversed())
                        }
                        else -> {
                            val result = ListValue()
                            var i = start.intData
                            while (i < end.intData) {
                                result[i] = main[i]
                                i += inc.intData
                            }
                            return result
                        }
                    }
                }
                is StringValue -> {
                    when (inc.intData) {
                        1 -> {
                            return StringValue(main.data.substring(start.intData, end.intData))
                        }
                        -1 -> {
                            return StringValue(main.data.substring(end.intData, start.intData).reversed())
                        }
                        else -> {
                            var i = start.intData
                            val result = StringBuilder()
                            while (i < end.intData) {
                                result[i] = main.data[i]
                                i += inc.intData
                            }
                            return StringValue(result.toString())
                        }
                    }
                }
                else -> {
                    error(ctx, "bad type : ${typeName(main)}")
                }
            }
        } else {
            error(ctx, "bad type match: [${typeName(start)}:${typeName(end)}]")
        }
    }

    private fun typeName(main: Value?) = main?.type?.typeName

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
                    error(ctx, "bad type match: ${typeName(left)} + ${typeName(right)}")
                }
            }
            MINUS -> {
                if (left is DoubleValue && right is DoubleValue) {
                    left - right
                } else {
                    error(ctx, "bad type match: ${typeName(left)} - ${typeName(right)}")
                }
            }
            else -> error(ctx, "Unknown operator: ${ctx.op.text}")
        }
    }

    override fun visitNegationExpr(ctx: NegationExprContext): Value {
        val main = visit(ctx.expr())
        return DoubleValue(-((main as? DoubleValue)?.data ?: error(ctx, "bad type match: -${typeName(main)}")))
    }

    override fun visitElvisExpr(ctx: ElvisExprContext): Value? {
        return visit(ctx.left) ?: visit(ctx.right)
    }

    override fun visitIsExpr(ctx: IsExprContext): Value {
        return BooleanValue.of(visit(ctx.left) === visit(ctx.right))
    }

    override fun visitRelationalExpr(ctx: RelationalExprContext): Value {
        val left = visit(ctx.left)
        val right = visit(ctx.right)
        if (left is DoubleValue && right is DoubleValue) {
            return when (ctx.op.type) {
                LT -> BooleanValue.of(left.data < right.data)
                GT -> BooleanValue.of(left.data > right.data)
                LE -> BooleanValue.of(left.data <= right.data)
                GE -> BooleanValue.of(left.data >= right.data)
                else -> error(ctx, "Unknown Operator: ${ctx.op.text}")
            }
        }
        error(ctx, "bad type match: ${typeName(left)} ${ctx.op.text} ${typeName(right)}")
    }

    override fun visitEqualityExpr(ctx: EqualityExprContext): Value {
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
        error(ctx, "bad type match: ${typeName(main)}[${typeName(key)}]")
    }

    override fun visitGetObjectExpr(ctx: GetObjectExprContext): Value? {
        val main = visit(ctx.main) ?: error(ctx, "cannot get properties from null")
        return main.properties[ctx.key.text]
    }

    override fun visitTernaryExpr(ctx: TernaryExprContext): Value? {
        val conditionValue = visit(ctx.condition)
        val condition = (conditionValue as? BooleanValue)?.data ?: error(ctx,
            "bad type match: ${typeName(conditionValue)} ? any : any")
        return visit(if (condition) ctx.left else ctx.right)
    }

    override fun visitFunctionCallLine(ctx: FunctionCallLineContext): Value? {
        val function = visit(ctx.expr())
        if (function is MethodValue) {
            function(ctx.exprList().values())
        } else {
            error(ctx, "bad type match ${typeName(function)}(parameters)")
        }
        return null
    }

    override fun visitAssertLine(ctx: AssertLineContext): Value? {
        val value = visit(ctx.expr())
        if (!(value as? BooleanValue ?: error(ctx, "bad type: $value")).data) {
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
            error(ctx, "bad type match: if (${typeName(condition)}) block")
        }
        return null
    }

    override fun visitForLoop(ctx: ForLoopContext): Value? {
        visit(ctx.first)
        while ((visit(ctx.condition) as? BooleanValue ?: error(ctx, "condition is not a boolean")).data) {
            visit(ctx.statBlock())
        }
        visit(ctx.last)
        return null
    }


    override fun visitWhileLoop(ctx: WhileLoopContext): Value? {
        while ((visit(ctx.exprBlock().expr()) as? BooleanValue ?: error(ctx, "condition is not a boolean")).data) {
            visit(ctx.exprBlock().statBlock())
        }
        return null
    }

    override fun visitDoWhileLoop(ctx: DoWhileLoopContext): Value? {
        do {
            visit(ctx.statBlock())
        } while ((visit(ctx.expr()) as? BooleanValue ?: error(ctx, "condition is not a boolean")).data)
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

        override fun visitString(ctx: StringContext): String {
            return ctx.stringPart().joinToString("") { visit(it) }
        }

        override fun visitExprInterpPart(ctx: ExprInterpPartContext) =
            this@FuncVisitor.visit(ctx.expr()).toString()
    }
}
