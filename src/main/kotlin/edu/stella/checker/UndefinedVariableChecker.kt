package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import com.strumenta.antlrkotlin.parsers.generated.stellaParserBaseVisitor
import edu.stella.core.DiagnosticsEngine

class UndefinedVariableChecker(
    private val symbols: SymbolTable,
    private val diag: DiagnosticsEngine
) : stellaParserBaseVisitor<Unit>() {
    override fun defaultResult() = Unit

    override fun visitVar(ctx: stellaParser.VarContext) {
        if (!symbols.contains(ctx.name!!.text!!, ctx)) {
            diag.diag(DiagUndefinedVariable(ctx, ctx.getParent() ?: ctx))
        }
        super.visitVar(ctx)
    }
}