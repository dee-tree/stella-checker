package edu.stella.core

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.checker.DiagUnexpectedTypeForExpr
import edu.stella.type.BadTy
import edu.stella.type.Ty
import edu.stella.utils.MapAst
import org.antlr.v4.kotlinruntime.RuleContext
import org.antlr.v4.kotlinruntime.tree.ParseTree

class TypeManager(private val diagEngine: DiagnosticsEngine) {
    private val context = MapAst<Ty>()
    private val expectation = MapAst<Ty>()

    private val unchecked = MapAst<Ty>()

    fun learn(node: ParseTree, ty: Ty, override: Boolean = false) {
        if (node in context && !override) throw StellaCompileException("Ty of ${node.text} is already known")
        context[node] = ty
        if (node is stellaParser.ParenthesisedExprContext) context[node.expr()] = ty
        if (node is stellaParser.TerminatingSemicolonContext) context[node.expr()] = ty
    }

    private fun getTy(node: ParseTree, storage: Map<ParseTree, Ty>): Ty? {
        if (node in storage) return storage[node]

        return when (node) {
            is stellaParser.ParenthesisedExprContext -> getTy(node.expr(), storage)
            is stellaParser.TypeAscContext -> getTy(node.expr(), storage)
            is stellaParser.TerminatingSemicolonContext -> getTy(node.expr(), storage)
            else -> null
        }
    }

    operator fun get(node: ParseTree): Ty? = getTy(node, context)

    fun getExpectation(node: ParseTree): Ty? = getTy(node, expectation)

    fun expect(node: ParseTree, ty: Ty) {
        if (node in expectation)
            diagEngine.diag(DiagUnexpectedTypeForExpr(node as RuleContext, ty, expectation[node]!!))
//            throw StellaCompileException("Ty of ${node.text} is already expected as ${expectation[node]}}")
        expectation[node] = ty
        unchecked[node] = ty

        if (node is stellaParser.ParenthesisedExprContext) expectation[node.expr()] = ty
        if (node is stellaParser.TerminatingSemicolonContext) expectation[node.expr()] = ty
    }

    fun check(node: ParseTree, deep: Boolean = true, diag: ((expected: Ty, actual: Ty?) -> Diag)? = null) {
        val knownTy = this[node]
        val expectedTy = getExpectation(node) ?: return // No expectations => any is possible
//            ?: throw StellaCompileException("Cannot check types without expectation for node ${node.text.quote()}")
        if (expectedTy.same(knownTy, deep) || knownTy == null) {
            unchecked.remove(node)
            return
        }

        diagEngine.diag(diag?.invoke(expectedTy, knownTy) ?: DiagUnexpectedTypeForExpr(node as RuleContext, expectedTy, knownTy))
    }

    fun checkRemaining() {
        for ((node, ty) in unchecked) {
            if (ty !is BadTy && this[node] != null &&
                !(ty same this[node])) diagEngine.diag(DiagUnexpectedTypeForExpr(node as RuleContext, ty, this[node]))
        }
    }

}