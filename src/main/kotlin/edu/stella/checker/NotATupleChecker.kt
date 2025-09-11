package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import com.strumenta.antlrkotlin.parsers.generated.stellaParserBaseVisitor
import edu.stella.core.DiagnosticsEngine

class NotATupleChecker(
    private val symbols: SymbolTable,
    private val diag: DiagnosticsEngine
) : stellaParserBaseVisitor<Unit>() {
    override fun defaultResult() = Unit

    private fun isTuple(fn: stellaParser.ExprContext): Boolean = when (fn) {
        is stellaParser.TupleContext -> true
        is stellaParser.VarContext if !symbols.contains(fn.name!!.text!!, fn) -> false
        is stellaParser.VarContext -> when (val decl = symbols.get(fn.name!!.text!!, fn)) {
            is stellaParser.ParamDeclContext -> decl.paramType!! is stellaParser.TypeTupleContext
            is stellaParser.DeclFunContext -> false
            is stellaParser.BindingContext -> false
            is stellaParser.LetContext -> isTuple(decl.body!!)
            else -> false
        }
        else -> false
    }

    override fun visitDotTuple(ctx: stellaParser.DotTupleContext) {
        super.visitDotTuple(ctx)

        if (!isTuple(ctx.expr())) diag.diag(DiagNotATuple(ctx.expr()))
    }
}