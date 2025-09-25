package edu.stella.checker.exhaustiveness

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.StellaCompileException
import edu.stella.type.UnitTy
import edu.stella.type.asTy

internal class UnitExhaustivenessSolver() : ExhaustivenessSolver<UnitTy>(UnitTy()) {
    override val isExhaustive: Boolean
        get() {
            return models.isNotEmpty()
        }

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
        is stellaParser.PatternUnitContext -> true
        is stellaParser.PatternVarContext -> true
        is stellaParser.PatternAscContext -> of same pattern.stellatype().asTy && isValidPattern(pattern.pattern())
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }

    override fun plusAssign(pattern: stellaParser.PatternContext): Unit = when(pattern) {
        is stellaParser.PatternUnitContext -> this += UnitModel
        is stellaParser.PatternVarContext -> this += UnitModel
        is stellaParser.PatternAscContext -> this += UnitModel
        is stellaParser.ParenthesisedPatternContext -> this += pattern.pattern()
        else -> throw StellaCompileException("Unexpected pattern $pattern for Bool matcher")
    }
}

internal object UnitModel : Model<UnitTy>(UnitTy()) {
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        return other is UnitModel
    }

    override fun hashCode(): Int = this::class.simpleName.hashCode()
}