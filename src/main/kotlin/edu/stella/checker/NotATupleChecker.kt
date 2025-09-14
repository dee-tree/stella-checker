package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser

class NotATupleChecker(
    delegate: SemaStage<Unit>
) : SemaASTVisitor<Unit>(), SemaStage<Unit> by delegate {
    override fun defaultResult() = Unit

    override fun visitDotTuple(ctx: stellaParser.DotTupleContext) {
        super.visitDotTuple(ctx)

        val ty = types[ctx.expr()]
        if (ty?.isTuple == true) return
        diag.diag(DiagNotATuple(ctx.expr(), ty))
    }
}