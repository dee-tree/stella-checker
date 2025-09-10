package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import com.strumenta.antlrkotlin.parsers.generated.stellaParserBaseVisitor
import edu.stella.core.DiagnosticsEngine

class StellaTypeChecker(
    private val program: stellaParser.ProgramContext,
    private val diag: DiagnosticsEngine
) : stellaParserBaseVisitor<Unit>() {

    fun check() {
        program.accept(this)
    }

    override fun visitProgram(ctx: stellaParser.ProgramContext) {
        super.visitProgram(ctx)

        ctx.accept(MissingMainChecker(diag))
    }

    override fun defaultResult() = Unit

}