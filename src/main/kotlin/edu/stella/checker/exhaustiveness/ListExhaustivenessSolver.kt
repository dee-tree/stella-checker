package edu.stella.checker.exhaustiveness

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.type.ListTy

internal class ListExhaustivenessSolver(ty: ListTy) : ExhaustivenessSolver<ListTy>(ty) {

    override fun plusAssign(pattern: stellaParser.PatternContext) {
        // TODO: implement
    }

    override val isExhaustive: Boolean
        get() = true // TODO: implement

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when(pattern) {
        is stellaParser.PatternListContext -> true
        is stellaParser.PatternConsContext -> true
        is stellaParser.PatternVarContext -> true
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }
}
