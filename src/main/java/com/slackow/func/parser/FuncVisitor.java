package com.slackow.func.parser;


import com.slackow.func.parser.value.Value;

import java.util.Stack;

public class FuncVisitor extends FuncParserBaseVisitor<Value> {
    private Scope<Value> memory = new Scope<>();
    private final Stack<String> namespaceStack = new Stack<>();

    private void enterScope() {
        memory = memory.getNewChild();
    }

    private void exitScope() {
        memory = memory.getParent();
    }
}
