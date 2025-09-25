package edu.stella.checker.exhaustiveness

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.checker.DiagNonExhaustiveMatching
import edu.stella.checker.DiagUnexpectedPatternForType
import edu.stella.core.DiagnosticsEngine
import edu.stella.core.TypeManager

class ExhaustivenessChecker(
    private val matching: stellaParser.MatchContext,
    private val types: TypeManager,
    private val diag: DiagnosticsEngine
) {
    fun check(): Boolean {
        val matchingTy = types.getSynthesized(matching.expr()) ?: return true
        val solver = matchingTy.patternChecker()

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