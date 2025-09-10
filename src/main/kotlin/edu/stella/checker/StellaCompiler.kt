package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaLexer
import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.StringCharStream

class StellaCompiler(private val sourceCode: String) {
    private val lexer = stellaLexer(StringCharStream(sourceCode, SOURCE_CODE_NAME))
    private val tokenStream = CommonTokenStream(lexer)
    val diagEngine = DiagnosticsEngine(tokenStream)
    private val parser = stellaParser(tokenStream)

    fun compile() {
        val ast = parser.program()

        StellaTypeChecker(ast, diagEngine).check()
    }

    companion object {
        private const val SOURCE_CODE_NAME = "program.st"
    }
}