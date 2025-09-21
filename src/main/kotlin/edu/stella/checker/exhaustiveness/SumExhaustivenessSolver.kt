package edu.stella.checker.exhaustiveness

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.StellaCompileException
import edu.stella.type.SumTy

internal class SumExhaustivenessSolver(ty: SumTy): ExhaustivenessSolver<SumTy>(ty) {
    override val isExhaustive: Boolean
        get() {
            val values = mutableListOf<Model<SumTy>>(
                SumModel(SumModel.SumSide.LEFT, of),
                SumModel(SumModel.SumSide.RIGHT, of)
            )
            for (m in models) {
                if (m is WildcardModel<SumTy>) {
                    return true
                }
                values.remove(m)
                if (values.isEmpty()) return true
            }
            return false
        }

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
        is stellaParser.PatternInlContext -> true
        is stellaParser.PatternInrContext -> true
        is stellaParser.PatternVarContext -> true
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }

    override fun plusAssign(pattern: stellaParser.PatternContext): Unit = when(pattern) {
        is stellaParser.PatternInlContext -> this += SumModel(SumModel.SumSide.LEFT, of)
        is stellaParser.PatternInrContext -> this += SumModel(SumModel.SumSide.RIGHT, of)
        is stellaParser.PatternVarContext -> this += WildcardModel<SumTy>(of)
        is stellaParser.ParenthesisedPatternContext -> this += pattern.pattern()
        else -> throw StellaCompileException("Unexpected pattern $pattern for sum matcher")
    }
}

private data class SumModel(val value: SumSide, private val ty: SumTy) : Model<SumTy>(ty) {
    enum class SumSide {
        LEFT, RIGHT
    }

    override val next: Model<SumTy>?
        get() = if (value == SumSide.LEFT) SumModel(SumSide.RIGHT, ty) else null
}