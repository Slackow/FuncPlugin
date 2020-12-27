package com.slackow.func.parser

import java.nio.file.Path

class Datapack(private val name: String, private val path: Path) {
    private val files = FuncVisitor(this)
    private val output: MutableMap<String, String> = HashMap()

    fun writeOutput() {

    }

}