package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import com.strumenta.antlrkotlin.parsers.generated.stellaParserBaseVisitor
import edu.stella.core.DiagnosticsEngine

class UnexpectedLambdaChecker(
    private val symbols: SymbolTable,
    private val diag: DiagnosticsEngine
) : stellaParserBaseVisitor<Unit>() {
    override fun defaultResult() = Unit

    override fun visitDeclFun(ctx: stellaParser.DeclFunContext) {
        if (ctx.returnType !is stellaParser.TypeFunContext && ctx.returnExpr is stellaParser.AbstractionContext) {
            diag.diag(DiagUnexpectedLambda(ctx.returnExpr!!))
        }

        super.visitDeclFun(ctx)
    }

    override fun visitDeclFunGeneric(ctx: stellaParser.DeclFunGenericContext) {
        if (ctx.returnType !is stellaParser.TypeFunContext && ctx.returnExpr is stellaParser.AbstractionContext) {
            diag.diag(DiagUnexpectedLambda(ctx.returnExpr!!))
        }

        super.visitDeclFunGeneric(ctx)
    }
}