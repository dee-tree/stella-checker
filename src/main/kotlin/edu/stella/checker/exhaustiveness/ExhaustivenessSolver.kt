package edu.stella.checker.exhaustiveness

import com.strumenta.antlrkotlin.parsers.generated.stellaParser
import edu.stella.type.Ty

internal abstract class ExhaustivenessSolver<T : Ty>(val of: T) {
    protected val models = mutableListOf<Model<T>>()

    operator fun plusAssign(m: Model<T>) {
        models.add(m)
    }

    abstract operator fun plusAssign(pattern: stellaParser.PatternContext)

    abstract val isExhaustive: Boolean

    abstract fun isValidPattern(pattern: stellaParser.PatternContext): Boolean
}
