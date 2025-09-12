package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import com.strumenta.antlrkotlin.parsers.generated.stellaParserBaseVisitor
import edu.stella.core.DiagnosticsEngine

class StellaTypeChecker(
    private val program: stellaParser.ProgramContext,
    private val symbols: SymbolTable,
    private val diag: DiagnosticsEngine
) : stellaParserBaseVisitor<Unit>() {

    fun check() {
        program.accept(this)
    }

    override fun visitProgram(ctx: stellaParser.ProgramContext) {
        super.visitProgram(ctx)

        ctx.accept(MissingMainChecker(diag))
        ctx.accept(UndefinedVariableChecker(symbols, diag))
        ctx.accept(NotFunctionApplicationChecker(symbols, diag))
        ctx.accept(NotATupleChecker(symbols, diag))
        ctx.accept(NotARecordChecker(symbols, diag))
        ctx.accept(NotAListChecker(symbols, diag))
        ctx.accept(UnexpectedLambdaChecker(symbols, diag))
    }

    override fun defaultResult() = Unit

}