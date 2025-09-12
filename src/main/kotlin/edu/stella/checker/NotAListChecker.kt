package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import com.strumenta.antlrkotlin.parsers.generated.stellaParserBaseVisitor
import edu.stella.core.DiagnosticsEngine

class NotAListChecker(
    private val symbols: SymbolTable,
    private val diag: DiagnosticsEngine
) : stellaParserBaseVisitor<Unit>() {
    override fun defaultResult() = Unit

    private fun isList(fn: stellaParser.ExprContext): Boolean = when (fn) {
        is stellaParser.ListContext -> true
        is stellaParser.ParenthesisedExprContext -> isList(fn.expr())
        is stellaParser.ConsListContext -> true
        is stellaParser.VarContext if !symbols.contains(fn.name!!.text!!, fn) -> false
        is stellaParser.VarContext -> when (val decl = symbols.get(fn.name!!.text!!, fn)) {
            is stellaParser.ParamDeclContext -> decl.paramType!! is stellaParser.TypeListContext
            is stellaParser.LetContext -> isList(decl.body!!)
            else -> false
        }
        else -> false
    }


    override fun visitHead(ctx: stellaParser.HeadContext) {
        super.visitHead(ctx)
        if (!isList(ctx.expr())) diag.diag(DiagNotAList(ctx.expr()))
    }

    override fun visitTail(ctx: stellaParser.TailContext) {
        super.visitTail(ctx)
        if (!isList(ctx.expr())) diag.diag(DiagNotAList(ctx.expr()))
    }

    override fun visitIsEmpty(ctx: stellaParser.IsEmptyContext) {
        super.visitIsEmpty(ctx)
        if (!isList(ctx.expr())) diag.diag(DiagNotAList(ctx.expr()))
    }

}