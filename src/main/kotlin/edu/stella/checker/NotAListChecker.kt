package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser

class NotAListChecker(
    delegate: SemaStage<Unit>
) : SemaASTVisitor<Unit>(), SemaStage<Unit> by delegate {
    override fun defaultResult() = Unit

    override fun visitHead(ctx: stellaParser.HeadContext) {
        super.visitHead(ctx)
        val ty = types.getSynthesized(ctx.expr())
        if (ty?.isList != false) return
        diag.diag(DiagNotAList(ctx.expr(), ty))
    }

    override fun visitTail(ctx: stellaParser.TailContext) {
        super.visitTail(ctx)
        val ty = types.getSynthesized(ctx.expr())
        if (ty?.isList != false) return
        diag.diag(DiagNotAList(ctx.expr(), ty))
    }

    override fun visitIsEmpty(ctx: stellaParser.IsEmptyContext) {
        super.visitIsEmpty(ctx)
        val ty = types.getSynthesized(ctx.expr())
        if (ty?.isList != false) return
        diag.diag(DiagNotAList(ctx.expr(), ty))
    }

}