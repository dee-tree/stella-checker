package edu.stella.core

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.checker.DiagUnexpectedLambda
import edu.stella.checker.DiagUnexpectedList
import edu.stella.checker.DiagUnexpectedParameterType
import edu.stella.checker.DiagUnexpectedRecord
import edu.stella.checker.DiagUnexpectedTuple
import edu.stella.checker.DiagUnexpectedTypeForExpr
import edu.stella.checker.DiagUnexpectedVariant
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

    operator fun get(node: ParseTree): Ty? = getExpectation(node) ?: getSynthesized(node)

    fun getSynthesized(node: ParseTree): Ty? = getTy(node, context)

    fun getExpectation(node: ParseTree): Ty? = getTy(node, expectation)

    fun expect(node: ParseTree, ty: Ty) {
        if (node in expectation && !(ty same expectation[node]))
            diagEngine.diag(DiagUnexpectedTypeForExpr(node as RuleContext, ty, expectation[node]!!))
        expectation[node] = ty
        unchecked[node] = ty

        if (node is stellaParser.ParenthesisedExprContext) expectation[node.expr()] = ty
        if (node is stellaParser.TerminatingSemicolonContext) expectation[node.expr()] = ty

        check(node)
    }

    fun check(node: ParseTree, deep: Boolean = true) {
        val knownTy = this.getSynthesized(node)
        val expectedTy = getExpectation(node) ?: return // No expectations => any is possible
//            ?: throw StellaCompileException("Cannot check types without expectation for node ${node.text.quote()}")
        if (expectedTy.same(knownTy, deep) || knownTy == null) {
            unchecked.remove(node)
            return
        }

        val diag = when {
            node is stellaParser.ListContext && !expectedTy.isList -> DiagUnexpectedList(node, expectedTy)
            node is stellaParser.ConsListContext && !expectedTy.isList -> DiagUnexpectedList(node, expectedTy)
            node is stellaParser.ParamDeclContext -> DiagUnexpectedParameterType(node, expectedTy)
            node is stellaParser.TupleContext && !expectedTy.isTuple -> DiagUnexpectedTuple(node, expectedTy)
            node is stellaParser.RecordContext && !expectedTy.isRecord -> DiagUnexpectedRecord(node, expectedTy)
            node is stellaParser.VariantContext && !expectedTy.isVariant -> DiagUnexpectedVariant(node, expectedTy)
            node is stellaParser.AbstractionContext && !expectedTy.isFunction -> DiagUnexpectedLambda(node, expectedTy)
            else -> DiagUnexpectedTypeForExpr(node as RuleContext, expectedTy, this.getSynthesized(node))
        }
        diagEngine.diag(diag)
    }

    fun checkRemaining() {
        for ((node, _) in expectation) {
//            check(node)
        }
    }

}