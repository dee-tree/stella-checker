package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser

class NotARecordChecker(
    delegate: SemaStage<Unit>
) : SemaASTVisitor<Unit>(), SemaStage<Unit> by delegate {
    override fun defaultResult() = Unit

    override fun visitDotRecord(ctx: stellaParser.DotRecordContext) {
        super.visitDotRecord(ctx)

        val ty = types[ctx.expr()]
        if (ty?.isRecord == true) return

        diag.diag(DiagNotARecord(ctx.expr(), ty))
    }
}