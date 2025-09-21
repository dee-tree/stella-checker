package edu.stella.checker.exhaustiveness

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.checker.DiagNonExhaustiveMatching
import edu.stella.checker.DiagUnexpectedPatternForType
import edu.stella.core.DiagnosticsEngine
import edu.stella.core.StellaCompileException
import edu.stella.core.TypeManager
import edu.stella.core.quote
import edu.stella.type.BoolTy
import edu.stella.type.ListTy
import edu.stella.type.NatTy
import edu.stella.type.SumTy
import edu.stella.type.VariantTy

class ExhaustivenessChecker(
    private val matching: stellaParser.MatchContext,
    private val types: TypeManager,
    private val diag: DiagnosticsEngine
) {

    fun check(): Boolean {
        val matchingTy = types.getSynthesized(matching.expr()) ?: return true
        val solver = when (val ty = matchingTy) {
            is BoolTy -> BoolExhaustivenessSolver()
            is NatTy -> NatExhaustivenessSolver()
            is SumTy -> SumExhaustivenessSolver(ty)
            is VariantTy -> VariantExhaustivenessSolver(ty)
            is ListTy -> ListExhaustivenessSolver(ty)
            else -> throw StellaCompileException("Unexpected matching over type ${ty.toString().quote()}")
        }

        matching.cases.forEach { cs ->
            val pattern = cs.pattern()
            if (!solver.isValidPattern(pattern)) {
                diag.diag(DiagUnexpectedPatternForType(pattern, matchingTy))
            } else {
                solver += pattern
            }
        }

        val exhaustive = solver.isExhaustive

        if (!exhaustive) {
            diag.diag(DiagNonExhaustiveMatching(matching, matchingTy))
        }

        return exhaustive
    }
}