package edu.stella.core

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.checker.DiagUnexpectedTypeForExpr
import edu.stella.type.Ty
import edu.stella.utils.MapAst
import org.antlr.v4.kotlinruntime.RuleContext
import org.antlr.v4.kotlinruntime.tree.ParseTree

class TypeManager(private val diagEngine: DiagnosticsEngine) {
    private val context = MapAst<Ty>()
    private val expectation = MapAst<Ty>()

    fun learn(node: ParseTree, ty: Ty) {
        if (node in context) throw StellaCompileException("Ty of ${node.text} is already known")
        context[node] = ty
    }

    private fun getTy(node: ParseTree, storage: Map<ParseTree, Ty>): Ty? {
        if (node in storage) return storage[node]

        return when (node) {
            is stellaParser.ParenthesisedExprContext -> getTy(node.expr(), storage)
            else -> null
        }
    }

    operator fun get(node: ParseTree): Ty? = getTy(node, context)

    fun getExpectation(node: ParseTree): Ty? = getTy(node, expectation)

    fun expect(node: ParseTree, ty: Ty) {
        if (node in expectation)
            throw StellaCompileException("Ty of ${node.text} is already expected as ${expectation[node]}}")
        expectation[node] = ty
    }

    fun check(node: ParseTree, deep: Boolean = true, diag: (() -> Diag)? = null) {
        val knownTy = this[node]
        val expectedTy = getExpectation(node) ?: return // No expectations => any is possible
//            ?: throw StellaCompileException("Cannot check types without expectation for node ${node.text.quote()}")
        if (expectedTy.same(knownTy, deep)) return

        diagEngine.diag(diag?.invoke() ?: DiagUnexpectedTypeForExpr(node as RuleContext, expectedTy, knownTy))
    }

}