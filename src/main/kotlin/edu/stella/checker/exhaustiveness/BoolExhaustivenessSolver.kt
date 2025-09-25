package edu.stella.checker.exhaustiveness

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.StellaCompileException
import edu.stella.type.BoolTy
import edu.stella.type.asTy

internal class BoolExhaustivenessSolver() : ExhaustivenessSolver<BoolTy>(BoolTy()) {
    override val isExhaustive: Boolean
        get() {
            val values = mutableListOf<Model<BoolTy>>(BoolModel.False, BoolModel.True)
            for (m in models) {
                if (m is WildcardModel<BoolTy>) {
                    return true
                }
                values.remove(m)
                if (values.isEmpty()) return true
            }
            return false
        }

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
        is stellaParser.PatternTrueContext -> true
        is stellaParser.PatternFalseContext -> true
        is stellaParser.PatternVarContext -> true
        is stellaParser.PatternAscContext -> of same pattern.stellatype().asTy && isValidPattern(pattern.pattern())
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }

    override fun plusAssign(pattern: stellaParser.PatternContext): Unit = when(pattern) {
        is stellaParser.PatternTrueContext -> this += BoolModel.True
        is stellaParser.PatternFalseContext -> this += BoolModel.False
        is stellaParser.PatternVarContext -> this += WildcardModel<BoolTy>(of)
        is stellaParser.PatternAscContext -> this += pattern.pattern()
        is stellaParser.ParenthesisedPatternContext -> this += pattern.pattern()
        else -> throw StellaCompileException("Unexpected pattern $pattern for Bool matcher")
    }
}

internal data class BoolModel(val value: Boolean) : FiniteModel<BoolTy>(BoolTy()) {
    override val next: Model<BoolTy>?
        get() = if (!value) True else null

    companion object {
        val True = BoolModel(true)
        val False = BoolModel(false)
    }
}