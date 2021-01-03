package com.slackow.func.parser

class Datapack(private val name: String) {
    private val files = FuncVisitor(this)
    private val output: MutableMap<String, String> = HashMap()

    fun writeOutput() {
        
    }

}