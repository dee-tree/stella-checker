package edu.stella

import edu.stella.core.DiagnosticsEngine
import edu.stella.core.StellaCompiler
import java.io.File
import java.io.OutputStream.nullOutputStream
import java.io.PrintStream
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isEmpty()) {
        val sourceCode = generateSequence(::readlnOrNull).joinToString("\n")
        val compilerInstance = StellaCompiler(sourceCode)
        compilerInstance.compile()
        exitProcess(getErrorCode(compilerInstance.diagEngine))
    }

    File(args[0]).walk().forEach { f ->
        if (f.isFile) {
            print(f.name)
            val sourceCode = f.readText()
            val compilerInstance = StellaCompiler(sourceCode, PrintStream(nullOutputStream()))
            compilerInstance.compile()
            listOfNotNull<String>(
                if (compilerInstance.diagEngine.hasError) "Error" else "Ok",
                "code ${getErrorCode(compilerInstance.diagEngine)}",
                compilerInstance.diagEngine.errorCode?.toString(),
            ).joinToString(" | ").also { println(" | $it") }
        }
    }
}

private fun getErrorCode(diag: DiagnosticsEngine) = if (diag.hasError) 1 else 0