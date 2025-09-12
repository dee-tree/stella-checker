package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import com.strumenta.antlrkotlin.parsers.generated.stellaParserBaseVisitor
import edu.stella.core.DiagnosticsEngine

class NotARecordChecker(
    private val symbols: SymbolTable,
    private val diag: DiagnosticsEngine
) : stellaParserBaseVisitor<Unit>() {
    override fun defaultResult() = Unit

    private fun isRecord(fn: stellaParser.ExprContext): Boolean = when (fn) {
        is stellaParser.RecordContext -> true
        is stellaParser.VarContext if !symbols.contains(fn.name!!.text!!, fn) -> false
        is stellaParser.VarContext -> when (val decl = symbols.get(fn.name!!.text!!, fn)) {
            is stellaParser.ParamDeclContext -> decl.paramType!! is stellaParser.TypeRecordContext
            is stellaParser.LetContext -> isRecord(decl.body!!)
            else -> false
        }
        else -> false
    }

    override fun visitDotRecord(ctx: stellaParser.DotRecordContext) {
        super.visitDotRecord(ctx)

        if (!isRecord(ctx.expr())) diag.diag(DiagNotARecord(ctx.expr()))
    }
}