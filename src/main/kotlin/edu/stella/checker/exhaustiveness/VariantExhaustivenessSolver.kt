package edu.stella.checker.exhaustiveness

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.core.StellaCompileException
import edu.stella.type.VariantTy

internal class VariantExhaustivenessSolver(ty: VariantTy) : ExhaustivenessSolver<VariantTy>(ty) {
    override val isExhaustive: Boolean
        get() {
            val values = of.tags.mapTo(mutableListOf()) { VariantModel(it, of) }
            for (m in models) {
                if (m is WildcardModel<VariantTy>) return true
                values.remove(m)
                if (values.isEmpty()) return true
            }
            return false
        }

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
        is stellaParser.PatternVariantContext -> pattern.label!!.text!! in of.tags
        is stellaParser.PatternVarContext -> true
        is stellaParser.PatternAscContext -> true
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }

    override fun plusAssign(pattern: stellaParser.PatternContext): Unit = when(pattern) {
        is stellaParser.PatternVariantContext -> this += VariantModel(pattern.label!!.text!!, of)
        is stellaParser.PatternVarContext -> this += WildcardModel<VariantTy>(of)
        is stellaParser.ParenthesisedPatternContext -> this += pattern.pattern()
        else -> throw StellaCompileException("Unexpected pattern $pattern for sum matcher")
    }
}

private data class VariantModel(val label: String, val ty: VariantTy) : Model<VariantTy>(ty) {
    // assume that variant pattern contains only "variable" as a tag value (as declared in description in task doc)
    override val next: Model<VariantTy>?
        get() {
            val tags = ty.tags
            val nextIdx = ty.tags.indexOf(label) + 1
            if (nextIdx > tags.lastIndex) return null
            return VariantModel(ty.tags[nextIdx], /*value,*/ ty)
        }
}