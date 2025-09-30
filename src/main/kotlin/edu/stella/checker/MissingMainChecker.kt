package edu.stella.checker

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import com.strumenta.antlrkotlin.parsers.generated.stellaParserBaseVisitor
import edu.stella.core.DiagnosticsEngine
import org.antlr.v4.kotlinruntime.tree.RuleNode

class MissingMainChecker(private val diag: DiagnosticsEngine) : stellaParserBaseVisitor<Unit>() {
    override fun defaultResult() = Unit

    private var hasMain = false

    override fun visitProgram(ctx: stellaParser.ProgramContext) {
        super.visitProgram(ctx)

        if (!hasMain)
            diag.diag(DiagMissingMain(ctx))
    }

    override fun shouldVisitNextChild(node: RuleNode, currentResult: Unit): Boolean {
        return !hasMain
    }

    override fun visitDeclFun(ctx: stellaParser.DeclFunContext) {
        super.visitDeclFun(ctx)

        if (ctx.name?.text == "main") {
            hasMain = true

            if (ctx.paramDecls.size != 1)
                diag.diag(DiagIncorrectArityOfMain(ctx))
        }
    }
}