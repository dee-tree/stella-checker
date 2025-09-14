package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser

class UndefinedVariableChecker(
    private val delegate: SemaStage<Unit>
) : SemaASTVisitor<Unit>(), SemaStage<Unit> by delegate {
    override fun defaultResult() = Unit

    override fun visitVar(ctx: stellaParser.VarContext) {
        if (!symbols.contains(ctx.name!!.text!!, ctx)) {
            diag.diag(DiagUndefinedVariable(ctx, ctx.getParent() ?: ctx))
        }
        super.visitVar(ctx)
    }
}