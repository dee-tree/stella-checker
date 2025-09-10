package edu.stella

import edu.stella.core.StellaCompiler
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val sourceCode = generateSequence(::readlnOrNull).joinToString("\n")

    val compilerInstance = StellaCompiler(sourceCode)
    compilerInstance.compile()

    val errorCode = if (compilerInstance.diagEngine.hasError) -2 else 0

    exitProcess(errorCode)
}