package edu.stella.checker.exhaustiveness

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.type.Ty

internal sealed class Model<T : Ty>(val of: T) : Iterable<Model<T>>, Iterator<Model<T>> {
    open val next: Model<T>? = null

    override fun hasNext(): Boolean = next != null
    override fun next(): Model<T> = next!!
    override fun iterator(): Iterator<Model<T>> = this
}

internal sealed class FiniteModel<T : Ty>(of: T) : Model<T>(of)

internal class WildcardModel<T : Ty>(of: T) : Model<T>(of) {
//    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = true
}

internal class WildcardExhaustivenessSolver<T : Ty>(of: T) : ExhaustivenessSolver<T>(of) {
    override fun plusAssign(pattern: stellaParser.PatternContext) {
        this += WildcardModel<T>(of)
    }

    override val isExhaustive: Boolean
        get() = models.isNotEmpty()

    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when (pattern) {
        is stellaParser.PatternVarContext -> true
        is stellaParser.PatternAscContext -> isValidPattern(pattern.pattern())
        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
        else -> false
    }
}





