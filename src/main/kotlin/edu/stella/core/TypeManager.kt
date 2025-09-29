package edu.stella.core

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.checker.DiagMissingRecordField
import edu.stella.checker.DiagUnexpectedLambda
import edu.stella.checker.DiagUnexpectedList
import edu.stella.checker.DiagUnexpectedParameterType
import edu.stella.checker.DiagUnexpectedRecord
import edu.stella.checker.DiagUnexpectedSubtypeForExpr
import edu.stella.checker.DiagUnexpectedTuple
import edu.stella.checker.DiagUnexpectedTupleLength
import edu.stella.checker.DiagUnexpectedTypeForExpr
import edu.stella.checker.DiagUnexpectedVariant
import edu.stella.checker.DiagUnexpectedVariantLabel
import edu.stella.checker.ExtensionChecker
import edu.stella.type.AnyTy
import edu.stella.type.BoolTy
import edu.stella.type.BotTy
import edu.stella.type.FunTy
import edu.stella.type.ListTy
import edu.stella.type.NatTy
import edu.stella.type.RecordTy
import edu.stella.type.RefTy
import edu.stella.type.SumTy
import edu.stella.type.TopTy
import edu.stella.type.TupleTy
import edu.stella.type.Ty
import edu.stella.type.UnitTy
import edu.stella.type.VariantTy
import edu.stella.utils.MapAst
import org.antlr.v4.kotlinruntime.RuleContext
import org.antlr.v4.kotlinruntime.tree.ParseTree

class TypeManager(private val diagEngine: DiagnosticsEngine, private val extensions: ExtensionChecker) {
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

    fun expect(node: ParseTree, ty: Ty, deep: Boolean = true, contravariant: Boolean = false) {
        val (subtype, supertype) = if (contravariant) (expectation[node] to ty) else (ty to expectation[node])
        if (node in expectation && (subtype?.subtypeOf(supertype) != true))
            diagEngine.diag(DiagUnexpectedTypeForExpr(node as RuleContext, ty, expectation[node]!!))
        expectation[node] = ty
        unchecked[node] = ty

        if (node is stellaParser.ParenthesisedExprContext) expectation[node.expr()] = ty
        if (node is stellaParser.TerminatingSemicolonContext) expectation[node.expr()] = ty

        check(node, deep, contravariant)
    }

    fun check(node: ParseTree, deep: Boolean = true, contravariant: Boolean = false) {
        val knownTy = this.getSynthesized(node)
        val expectedTy = getExpectation(node) ?: return // No expectations => any is possible

        val (subtype, supertype) = if (contravariant) (expectedTy to knownTy) else (knownTy to expectedTy)

        if (knownTy == null || (deep && subtype?.subtypeOf(supertype) == true) || (!deep && subtype?.same(supertype, false) == true)) {
            unchecked.remove(node)
            return
        }

        TypeMismatchReporter(node).report(expectedTy, knownTy)
    }

    inner class TypeMismatchReporter(private val node: ParseTree) {
        fun report(expected: Ty, actual: Ty) {
            visit(expected, actual, topLevel = true)?.let {
                diagEngine.diag(it)
            }
        }

        private fun visit(expected: Ty, actual: Ty, topLevel: Boolean = false): Diag? {
            if (actual is AnyTy || expected is AnyTy) return null
            if (expected is TopTy) return null

            if (topLevel) {
                val diag = when {
                    node is stellaParser.ListContext && !expected.isList -> DiagUnexpectedList(node, expected)
                    node is stellaParser.ConsListContext && !expected.isList -> DiagUnexpectedList(node, expected)
                    node is stellaParser.ParamDeclContext -> DiagUnexpectedParameterType(node, expected)
                    node is stellaParser.TupleContext && !expected.isTuple -> DiagUnexpectedTuple(node, expected)
                    node is stellaParser.RecordContext && !expected.isRecord -> DiagUnexpectedRecord(node, expected)
                    node is stellaParser.VariantContext && !expected.isVariant -> DiagUnexpectedVariant(node, expected)
                    node is stellaParser.AbstractionContext && !expected.isFunction -> DiagUnexpectedLambda(
                        node,
                        expected
                    )

                    else -> null
                }

                diag?.let {
                    return it
                }
            }

            return when (expected) {
                is UnitTy -> visitUnit(expected, actual)
                is BoolTy -> visitBool(expected, actual)
                is NatTy -> visitNat(expected, actual)
                is FunTy -> visitFun(expected, actual)
                is TupleTy -> visitTuple(expected, actual)
                is RecordTy -> visitRecord(expected, actual)
                is SumTy -> visitSum(expected, actual)
                is VariantTy -> visitVariant(expected, actual)
                is ListTy -> visitList(expected, actual)
                is RefTy -> visitRef(expected, actual)
                is BotTy if actual !is BotTy -> typeMismatch(expected, actual)
                else -> null
            }
        }

        private fun visitUnit(expected: UnitTy, actual: Ty): Diag? {
            if (actual !is UnitTy) return typeMismatch(expected, actual)
            return null
        }

        private fun visitBool(expected: BoolTy, actual: Ty): Diag? {
            if (actual !is BoolTy) return typeMismatch(expected, actual)
            return null
        }

        private fun visitNat(expected: NatTy, actual: Ty): Diag? {
            if (actual !is NatTy) return typeMismatch(expected, actual)
            return null
        }

        private fun visitFun(expected: FunTy, actual: Ty): Diag? {
            if (actual !is FunTy) return typeMismatch(expected, actual)

            visit(expected.ret, actual.ret,)?.let { return it }
            if (expected.params.size != actual.params.size) return typeMismatch(expected, actual)

            expected.params.zip(actual.params).forEach { (actual, expected) ->
                // parameters are inverted since they are in contravariant relation
                visit(expected, actual)?.let { return it }
            }
            return null
        }

        private fun visitTuple(expected: TupleTy, actual: Ty): Diag? {
            if (actual !is TupleTy) return typeMismatch(expected, actual)
            if (expected.components.size != actual.components.size) return DiagUnexpectedTupleLength(node as stellaParser.ExprContext, expected, actual)

            expected.components.zip(actual.components).forEach { (expected, actual) ->
                visit(expected, actual)?.let { return it }
            }
            return null
        }

        private fun visitRecord(expected: RecordTy, actual: Ty): Diag? {
            if (actual !is RecordTy) return typeMismatch(expected, actual)

            expected.labels.forEach { label ->
                if (label !in actual.labels) return DiagMissingRecordField(node as stellaParser.ExprContext, label, expected)
            }

            if (expected.components.size != actual.components.size && !Ty.withStructuralSubtyping) return typeMismatch(expected, actual)
            if (actual.components.size < expected.components.size && Ty.withStructuralSubtyping) return typeMismatch(expected, actual)

            if (Ty.withStructuralSubtyping) {
                expected.components.forEach { (n, t) ->
                    visit(t, actual.getComponentTyOrNull(n)!!)?.let { return it }
                }
            } else {
                expected.components.zip(actual.components).forEach { (expC, actualC) ->
                    val (expN, expT) = expC
                    val (actualN, actualT) = actualC
                    if (expN != actualN) return typeMismatch(expected, actual)
                    visit(expT, actualT)?.let { return it }
                }
            }
            return null
        }

        private fun visitSum(expected: SumTy, actual: Ty): Diag? {
            if (actual !is SumTy) return typeMismatch(expected, actual)

            visit(expected.left, actual.left)?.let { return it }
            visit(expected.right, actual.right)?.let { return it }
            return null
        }

        private fun visitVariant(expected: VariantTy, actual: Ty): Diag? {
            if (actual !is VariantTy) return typeMismatch(expected, actual)

            if (actual.components.size != expected.components.size && !Ty.withStructuralSubtyping) return typeMismatch(expected, actual)
            if (actual.components.size > expected.components.size && Ty.withStructuralSubtyping) return typeMismatch(expected, actual)

            if (Ty.withStructuralSubtyping) {
                actual.components.forEach { (n, t) ->
                    if (n !in expected.tags) return DiagUnexpectedVariantLabel(node as stellaParser.ExprContext, n, expected)
                    if (actual.of(n) == null && expected.of(n) != null) return typeMismatch(expected, actual)

                    actual.of(n)?.let { actOfN ->
                        expected.of(n)?.let { expOfN ->
                            visit(expOfN, actOfN)?.let { return it }
                        }
                    }
                }
            } else {
                actual.components.zip(expected.components).forEach { (actC, expC) ->
                    val (actN, actT) = actC
                    val (expN, expT) = expC
                    if (actN != expN) return typeMismatch(expected, actual)

                    if ((actT == null) xor (expT == null) == true) return typeMismatch(expected, actual)
                    actT?.let {
                        visit(expT!!, actT)?.let { return it }
                    }
                }
            }
            return null
        }

        private fun visitList(expected: ListTy, actual: Ty): Diag? {
            if (actual !is ListTy) return typeMismatch(expected, actual)
            return visit(expected.of, actual.of)
        }

        private fun visitRef(expected: RefTy, actual: Ty): Diag? {
            if (actual !is RefTy) return typeMismatch(expected, actual)
            return visit(expected.of, actual.of)
        }

        private fun typeMismatch(expected: Ty, actual: Ty): Diag {
            val diag = when {
                extensions.isStructuralSubtypingEnabled -> DiagUnexpectedSubtypeForExpr(
                    node as RuleContext,
                    expected,
                    actual
                )
                else -> DiagUnexpectedTypeForExpr(node as RuleContext, expected, actual)
            }

            return diag
        }
    }
}