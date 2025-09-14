package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser

class NotFunctionApplicationChecker(
    private val delegate: SemaStage<Unit>
) : SemaASTVisitor<Unit>(), SemaStage<Unit> by delegate {
    override fun defaultResult() = Unit

    override fun visitApplication(ctx: stellaParser.ApplicationContext) {
        super.visitApplication(ctx)

        val ty = types[ctx.func!!]
        if (ty?.isFunction == true) return
        diag.diag(DiagNotAFunction(ctx, ty))
    }

    override fun visitFix(ctx: stellaParser.FixContext) {
        super.visitFix(ctx)

        val ty = types[ctx.expr()]
        if (ty?.isFunction == true) return
        diag.diag(DiagNotAFunction(ctx, ty))
    }
}