package edu.stella.checker.exhaustiveness

import edu.stella.type.Ty
import edu.stella.type.UnitTy

internal sealed class Model<T : Ty>(val of: T) : Iterable<Model<T>>, Iterator<Model<T>> {
    open val next: Model<T>? = null

    override fun hasNext(): Boolean = next != null
    override fun next(): Model<T> = next!!
    override fun iterator(): Iterator<Model<T>> = this

    open fun normalized(): Model<T> = this
}

internal sealed class FiniteModel<T : Ty>(of: T) : Model<T>(of)

internal class WildcardModel<T : Ty>(of: T) : Model<T>(of) {
//    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = true
}

private object UnitModel : Model<UnitTy>(UnitTy()) {
    override fun equals(other: Any?): Boolean {
        if (other == null) return false
        return other is UnitModel
    }

//    override fun isValidPattern(pattern: stellaParser.PatternContext): Boolean = when (pattern) {
//        is stellaParser.PatternUnitContext -> true
//        is stellaParser.PatternVarContext -> true
//        is stellaParser.ParenthesisedPatternContext -> isValidPattern(pattern.pattern())
//        else -> false
//    }

    override fun hashCode(): Int = this::class.simpleName.hashCode()
}




