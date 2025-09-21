package edu.stella.checker.exhaustiveness

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.StellaCompileException
import edu.stella.type.NatTy

internal class NatExhaustivenessSolver() : ExhaustivenessSolver<NatTy>(NatTy()) {

    override fun plusAssign(pattern: stellaParser.PatternContext) = pattern.toNatModel()?.let { model ->
        this += model
    } ?: throw StellaCompileException("Unexpected pattern $pattern for Bool matcher")

    override val isExhaustive: Boolean
        get() {
            if (models.none { (it as NatModel).isRange }) return false
            val minCovered = models.filterIsInstance<NatModel>().filter { it.isRange }.minOf { it.value }
            if (minCovered == 0u) return true
            val picks = models.filterIsInstance<NatModel>().filter { !it.isRange }.map { it.value }

            for (n in 0u ..< minCovered) {
                if (n !in picks) return false
            }

            return true
        }

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when (pattern) {
        is stellaParser.PatternIntContext -> true
        is stellaParser.PatternSuccContext -> true
        is stellaParser.PatternVarContext -> true
        is stellaParser.PatternAscContext -> true
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }
}

private fun stellaParser.PatternContext.toNatModel(): Model<NatTy>? = when (this) {
    is stellaParser.PatternIntContext -> NatModel.of(n!!.text!!.toUInt())
    is stellaParser.PatternSuccContext -> when (val succ = pattern().toNatModel()) {
        is NatModel if !succ.isRange -> NatModel.of(succ.value + 1u)
        is NatModel if succ.isRange -> NatModel.range(succ.value + 1u)
        null -> null
        else -> throw IllegalStateException("Unexpected Nat model: $succ")
    }
    is stellaParser.PatternVarContext -> NatModel(0u, isRange = true)
    is stellaParser.ParenthesisedPatternContext -> this.pattern().toNatModel()
    is stellaParser.PatternAscContext -> this.pattern().toNatModel()
    else -> null
}

internal data class NatModel(val value: UInt, val isRange: Boolean) : Model<NatTy>(NatTy()) {

    override val next: Model<NatTy>?
        get() = null

    companion object {
        fun of(nat: UInt) = NatModel(nat, isRange = false)
        fun range(from: UInt) = NatModel(from, isRange = true)

    }
}