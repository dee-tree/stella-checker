package edu.stella.core

import com.strumenta.antlrkotlin.parsers.generated.stellaLexer
import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.checker.StellaTypeChecker
import edu.stella.checker.SymbolCollector
import org.antlr.v4.kotlinruntime.CommonTokenStream
import org.antlr.v4.kotlinruntime.StringCharStream
import java.io.PrintStream

class StellaCompiler(private val sourceCode: String, diagOutput: PrintStream = System.out) {
    private val lexer = stellaLexer(StringCharStream(sourceCode, SOURCE_CODE_NAME))
    private val tokenStream = CommonTokenStream(lexer)
    private val parser = stellaParser(tokenStream)

    val diagEngine = DiagnosticsEngine(tokenStream, diagOutput)
    val typeManager = TypeManager(diagEngine)

    fun compile() {
        val ast = parser.program()
        val symbols = ast.accept(SymbolCollector())

        StellaTypeChecker(ast, symbols, typeManager, diagEngine).check()
    }

    companion object {
        private const val SOURCE_CODE_NAME = "program.st"
    }
}