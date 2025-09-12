package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import com.strumenta.antlrkotlin.parsers.generated.stellaParserBaseVisitor
import edu.stella.core.DiagnosticsEngine

class NotFunctionApplicationChecker(
    private val symbols: SymbolTable,
    private val diag: DiagnosticsEngine
) : stellaParserBaseVisitor<Unit>() {
    override fun defaultResult() = Unit

    private fun isFunction(fn: stellaParser.ExprContext): Boolean = when (fn) {
        is stellaParser.AbstractionContext -> true
        is stellaParser.ParenthesisedExprContext -> isFunction(fn.expr())
        is stellaParser.TypeApplicationContext -> isFunction(fn.expr())
        is stellaParser.VarContext if !symbols.contains(fn.name!!.text!!, fn) -> false
        is stellaParser.VarContext -> when (val decl = symbols.get(fn.name!!.text!!, fn)) {
            is stellaParser.ParamDeclContext -> decl.paramType!! is stellaParser.TypeFunContext
            is stellaParser.DeclFunContext -> true
            is stellaParser.DeclFunGenericContext -> true
            is stellaParser.BindingContext -> false
            is stellaParser.LetContext -> isFunction(decl.body!!)
            else -> false
        }
        else -> false
    }

    override fun visitApplication(ctx: stellaParser.ApplicationContext) {
        super.visitApplication(ctx)

        if (!isFunction(ctx.func!!)) diag.diag(DiagNotAFunction(ctx))
    }

    override fun visitFix(ctx: stellaParser.FixContext) {
        super.visitFix(ctx)

        if (!isFunction(ctx.expr())) diag.diag(DiagNotAFunction(ctx))
    }
}