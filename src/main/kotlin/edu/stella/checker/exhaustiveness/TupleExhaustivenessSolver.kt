package edu.stella.checker.exhaustiveness

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.type.TupleTy
import edu.stella.type.asTy

internal class TupleExhaustivenessSolver(ty: TupleTy) : ExhaustivenessSolver<TupleTy>(ty) {
    override val isExhaustive: Boolean
        get() {
            return true
        }

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
        is stellaParser.PatternTupleContext -> true
        is stellaParser.PatternVarContext -> true
        is stellaParser.PatternAscContext -> of same pattern.stellatype().asTy && isValidPattern(pattern.pattern())
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }

    override fun plusAssign(pattern: stellaParser.PatternContext): Unit = Unit
}

private data class TupleModel(val values: List<Model<*>>, private val ty: TupleTy) : Model<TupleTy>(ty) {
    override val next: Model<TupleTy>?
        get() {
            val nextValues = mutableListOf<Model<*>>()
            for (v in values) {
                nextValues.add(v)
            }

            for (i in nextValues.indices) {
                nextValues[i].next?.let {
                    nextValues[i] = it
                    break
                }
                if (i == nextValues.indices.last) return null
            }
            return TupleModel(nextValues, ty)
        }
}