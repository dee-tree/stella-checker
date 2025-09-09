package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaLexer
import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import org.antlr.v4.kotlinruntime.BufferedTokenStream
import org.antlr.v4.kotlinruntime.StringCharStream

class StellaCompiler(private val sourceCode: String) {
    private val lexer = stellaLexer(StringCharStream(sourceCode, SOURCE_CODE_NAME))
    private val parser = stellaParser(BufferedTokenStream(lexer))

    fun compile() {
        val ast = parser.program()
    }

    companion object {
        private const val SOURCE_CODE_NAME = "program.st"
    }
}