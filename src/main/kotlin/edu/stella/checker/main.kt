package edu.stella.checker

import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val sourceCode = generateSequence(::readlnOrNull).joinToString("\n")

    val compilerInstance = StellaCompiler(sourceCode)
    compilerInstance.compile()

    val errorCode = 0

    exitProcess(errorCode)
}