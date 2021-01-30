package com.slackow.func.parser

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class Datapack(private val name: String) {
    private lateinit var packMcMeta: JsonObject
    val output: MutableMap<String, MutableMap<String, String>> = HashMap()

    fun collect(inputPath: Path) {
        packMcMeta = JsonParser.parseReader(
            Files.newBufferedReader(inputPath.resolve("pack.mcmeta"))
        ).asJsonObject["func"].asJsonObject
        val data = inputPath.resolve("data")
        data.toFile().listFiles()?.forEach { subDir ->
            if (subDir.isDirectory) {
                val sources = subDir.resolve("sources")
                fun recursiveLook(dir: File) {
                    dir.listFiles()?.forEach {
                        if (it.isDirectory) {
                            recursiveLook(it)
                            return
                        }
                        if (it.exists()) {
                            val extension = it.extension
                            if (extension == "mcfscript") {
                                println(it)
                                val fromPath = CharStreams.fromPath(it.toPath())
                                val funcLexer = FuncLexer(fromPath)
                                val tokenStream = CommonTokenStream(funcLexer)
                                val funcParser = FuncParser(tokenStream)
                                val functionID =
                                    it.toRelativeString(subDir.parentFile)
                                        .replace('\\', '/')
                                        .replace("/sources/", ":")
                                        .run { substring(0, this.length - 10) }
                                FuncVisitor(this,
                                    functionID).visit(funcParser.program())
                            } else if (extension == "mcflib") {
                                println("World's not ready yet")
                            }
                        } else {
                            throw Exception("WHAT: $it")
                        }
                    }
                }
                recursiveLook(sources)
            }
        }
    }

    private fun JsonObject.isPropertyTrue(key: String): Boolean {
        if (!this.has(key)) return false
        val boolProperty = this[key]
        return boolProperty.isJsonPrimitive && boolProperty.asJsonPrimitive.isBoolean && boolProperty.asBoolean
    }

    fun writeOutput(outputPath: Path) {
        //Good Luck Reading this lol
        output.forEach { (type, files) ->
            files.forEach { (id, content) ->
                val colonIndex = id.indexOf(':')
                val namespace = id.substring(0, colonIndex)
                val path = id.substring(colonIndex + 1)
                Files.write(outputPath.resolve(Paths.get(this.name,
                    "data",
                    namespace,
                    type,
                    "$path.${if (type == "functions") "mcfunction" else "json"}"))
                    .apply { Files.createDirectories(parent) },

                    content.lines())
            }
        }
        Files.write(outputPath.resolve(Paths.get(name, "pack.mcmeta")), """
                {
                    "pack": {
                        "pack_format": 6,
                        "description": "test"
                    }
                }
            """.trimIndent().lines())
    }

}